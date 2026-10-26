package com.example.movie.service;

import com.example.movie.dto.BookingRequest;
import com.example.movie.dto.BookingResponse;
import com.example.movie.model.*;
import com.example.movie.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.example.movie.dto.BookingPreview;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final long holdMinutes;

    public BookingService(BookingRepository bookingRepository,
                          BookingSeatRepository bookingSeatRepository,
                          ShowtimeSeatRepository showtimeSeatRepository,
                          ShowtimeRepository showtimeRepository,
                          SeatRepository seatRepository,
                          UserRepository userRepository,
                          PaymentRepository paymentRepository,
                          @Value("${payment.hold-minutes:15}") long holdMinutes) {
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.showtimeSeatRepository = showtimeSeatRepository;
        this.showtimeRepository = showtimeRepository;
        this.seatRepository = seatRepository;
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
        this.holdMinutes = holdMinutes;
    }

    @Transactional
    public List<ShowtimeSeat> getOrCreateShowtimeSeats(Long showtimeId) {
        expirePendingBookings();
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy suất chiếu."));
        if (!showtime.isBookableAt(LocalDateTime.now(Movie.CINEMA_ZONE))) {
            throw new IllegalArgumentException("Suất chiếu hiện không mở bán.");
        }

        List<Seat> roomSeats = seatRepository.findByRoomIdAndActiveTrueOrderByRowLabelAscSeatNumberAsc(
                        showtime.getRoom().getId()).stream()
                .filter(seat -> seat.getSeatType() != null && seat.getSeatType().isSupported())
                .toList();
        List<ShowtimeSeat> existingSeats = showtimeSeatRepository.findByShowtimeId(showtimeId);
        Set<Long> existingSeatIds = new HashSet<>();
        for (ShowtimeSeat existingSeat : existingSeats) {
            existingSeatIds.add(existingSeat.getSeat().getId());
        }

        List<ShowtimeSeat> newShowtimeSeats = new ArrayList<>();
        for (Seat seat : roomSeats) {
            if (existingSeatIds.contains(seat.getId())) {
                continue;
            }
            ShowtimeSeat showtimeSeat = new ShowtimeSeat();
            showtimeSeat.setShowtime(showtime);
            showtimeSeat.setSeat(seat);
            showtimeSeat.setStatus(SeatStatus.AVAILABLE);
            newShowtimeSeats.add(showtimeSeat);
        }
        if (!newShowtimeSeats.isEmpty()) {
            showtimeSeatRepository.saveAll(newShowtimeSeats);
        }

        Set<Long> activeSeatIds = new HashSet<>();
        for (Seat seat : roomSeats) {
            activeSeatIds.add(seat.getId());
        }
        return showtimeSeatRepository.findByShowtimeId(showtimeId).stream()
                .filter(showtimeSeat -> activeSeatIds.contains(showtimeSeat.getSeat().getId()))
                .toList();
    }

    @Transactional
    public BookingPreview previewBooking(Long userId, BookingRequest request) {
        List<ShowtimeSeat> selectedSeats = validateAndGetSelectedSeats(request);
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<String> seatNames = new ArrayList<>();
        for (ShowtimeSeat seat : selectedSeats) {
            totalAmount = totalAmount.add(findShowtime(request.getShowtimeId()).priceFor(seat.getSeat().getSeatType()));
            seatNames.add(seat.getSeat().getRowLabel() + seat.getSeat().getSeatNumber());
        }
        return new BookingPreview(request.getShowtimeId(), new ArrayList<>(request.getSeatIds()), seatNames, totalAmount);
    }

    @Transactional
    public BookingResponse createBooking(Long userId, BookingRequest request) {
        expirePendingBookings();
        Showtime showtime = findShowtime(request.getShowtimeId());
        List<ShowtimeSeat> selectedSeats = validateAndGetSelectedSeats(request);

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<String> seatNames = new ArrayList<>();
        for (ShowtimeSeat seat : selectedSeats) {
            totalAmount = totalAmount.add(showtime.priceFor(seat.getSeat().getSeatType()));
            seatNames.add(seat.getSeat().getRowLabel() + seat.getSeat().getSeatNumber());
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user."));
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setShowtime(showtime);
        booking.setTotalAmount(totalAmount);
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        booking.setPaymentStatus(PaymentStatus.PENDING);
        LocalDateTime now = LocalDateTime.now(Movie.CINEMA_ZONE);
        booking.setCreatedAt(now);
        booking.setHoldExpiresAt(now.plusMinutes(holdMinutes));
        Booking savedBooking = bookingRepository.save(booking);

        for (ShowtimeSeat selectedSeat : selectedSeats) {
            // BOOKED is the existing inventory representation for a temporary hold.
            selectedSeat.setStatus(SeatStatus.BOOKED);
            bookingSeatRepository.save(new BookingSeat(savedBooking, selectedSeat.getSeat(), showtime,
                    showtime.priceFor(selectedSeat.getSeat().getSeatType())));
        }
        showtimeSeatRepository.saveAll(selectedSeats);
        return new BookingResponse(
                savedBooking.getId(),
                showtime.getId(),
                user.getId().toString(),
                totalAmount,
                savedBooking.getStatus().name(),
                savedBooking.getCreatedAt(),
                seatNames
        );
    }

    @Transactional
    public Booking markAsPaid(Long userId, Long bookingId) {
        expirePendingBookings();
        Booking booking = bookingRepository.findByIdAndUserId(bookingId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt vé."));
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT
                || booking.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException("Đơn đặt vé này đã được xử lý.");
        }
        LocalDateTime now = LocalDateTime.now(Movie.CINEMA_ZONE);
        if (booking.getHoldExpiresAt() != null && !booking.getHoldExpiresAt().isAfter(now)) {
            expireBooking(booking);
            throw new IllegalStateException("Thời gian giữ ghế đã hết. Vui lòng đặt vé lại.");
        }
        Payment payment = paymentRepository
                .findFirstByBookingIdAndStatusOrderByCreatedAtDesc(bookingId, PaymentStatus.PENDING)
                .orElseThrow(() -> new IllegalStateException("Không còn giao dịch thanh toán đang chờ."));
        if (payment.getAmount() == null || booking.getTotalAmount() == null
                || payment.getAmount().compareTo(booking.getTotalAmount()) != 0) {
            throw new IllegalStateException("Số tiền thanh toán không khớp tổng tiền vé.");
        }

        List<BookingSeat> bookingSeats = bookingSeatRepository.findByBookingId(bookingId);
        if (bookingSeats.isEmpty()) {
            throw new IllegalStateException("Đơn đặt vé không có ghế.");
        }
        List<Long> seatIds = bookingSeats.stream()
                .map(bookingSeat -> bookingSeat.getSeat().getId())
                .toList();
        List<ShowtimeSeat> selectedSeats = showtimeSeatRepository
                .findByShowtimeIdAndSeatIdIn(booking.getShowtime().getId(), seatIds);
        if (selectedSeats.size() != seatIds.size()
                || selectedSeats.stream().anyMatch(seat -> seat.getStatus() != SeatStatus.BOOKED)) {
            throw new IllegalStateException("Ghế giữ cho đơn này không còn khả dụng. Vui lòng đặt vé lại.");
        }
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(now);
        payment.setTransactionRef("QR-" + payment.getId());
        paymentRepository.save(payment);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentStatus(PaymentStatus.SUCCESS);
        return bookingRepository.save(booking);
    }

    @Transactional
    public Payment createPaymentAttempt(Long userId, Long bookingId) {
        expirePendingBookings();
        Booking booking = getPendingBooking(userId, bookingId);
        paymentRepository.findFirstByBookingIdAndStatusOrderByCreatedAtDesc(booking.getId(), PaymentStatus.PENDING)
                .ifPresent(current -> {
                    current.setStatus(PaymentStatus.FAILED);
                    paymentRepository.save(current);
                });
        Payment payment = new Payment(booking, "QR", booking.getTotalAmount(), PaymentStatus.PENDING,
                null, LocalDateTime.now(Movie.CINEMA_ZONE));
        return paymentRepository.save(payment);
    }

    @Transactional
    public void failLatestPayment(Long userId, Long bookingId) {
        expirePendingBookings();
        Booking booking = getPendingBooking(userId, bookingId);
        paymentRepository.findFirstByBookingIdAndStatusOrderByCreatedAtDesc(booking.getId(), PaymentStatus.PENDING)
                .ifPresent(payment -> {
                    payment.setStatus(PaymentStatus.FAILED);
                    paymentRepository.save(payment);
                });
    }

    @Transactional
    @Scheduled(fixedDelayString = "${payment.expiry-check-ms:60000}")
    public void expirePendingBookings() {
        LocalDateTime now = LocalDateTime.now(Movie.CINEMA_ZONE);
        bookingRepository.findByStatusAndHoldExpiresAtBefore(BookingStatus.PENDING_PAYMENT, now)
                .forEach(this::expireBooking);
    }

    private Booking getPendingBooking(Long userId, Long bookingId) {
        Booking booking = bookingRepository.findByIdAndUserId(bookingId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt vé."));
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT
                || booking.getHoldExpiresAt() == null
                || !booking.getHoldExpiresAt().isAfter(LocalDateTime.now(Movie.CINEMA_ZONE))) {
            if (booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
                expireBooking(booking);
            }
            throw new IllegalStateException("Đơn đặt vé không còn trong thời gian giữ ghế.");
        }
        return booking;
    }

    private void expireBooking(Booking booking) {
        bookingSeatRepository.findByBookingId(booking.getId()).forEach(bookingSeat ->
                showtimeSeatRepository.findByShowtimeIdAndSeatIdIn(
                        bookingSeat.getShowtime().getId(), List.of(bookingSeat.getSeat().getId()))
                        .forEach(showtimeSeat -> {
                            if (showtimeSeat.getStatus() == SeatStatus.BOOKED) {
                                showtimeSeat.setStatus(SeatStatus.AVAILABLE);
                                showtimeSeatRepository.save(showtimeSeat);
                            }
                        }));
        paymentRepository.findFirstByBookingIdAndStatusOrderByCreatedAtDesc(booking.getId(), PaymentStatus.PENDING)
                .ifPresent(payment -> {
                    payment.setStatus(PaymentStatus.FAILED);
                    paymentRepository.save(payment);
                });
        booking.setStatus(BookingStatus.EXPIRED);
        booking.setPaymentStatus(PaymentStatus.FAILED);
        bookingRepository.save(booking);
    }

    @Transactional(readOnly = true)
    public Booking getUserBooking(Long userId, Long bookingId) {
        return bookingRepository.findByIdAndUserId(bookingId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt vé."));
    }

    @Transactional(readOnly = true)
    public List<Booking> getUserBookings(Long userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    private Showtime findShowtime(Long showtimeId) {
        return showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy suất chiếu."));
    }

    private List<ShowtimeSeat> validateAndGetSelectedSeats(BookingRequest request) {
        if (request.getShowtimeId() == null || request.getSeatIds() == null || request.getSeatIds().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất 1 ghế!");
        }
        if (request.getSeatIds().contains(null)
                || request.getSeatIds().stream().distinct().count() != request.getSeatIds().size()) {
            throw new IllegalArgumentException("Danh sách ghế không hợp lệ.");
        }
        Showtime showtime = findShowtime(request.getShowtimeId());
        if (!showtime.isBookableAt(LocalDateTime.now(Movie.CINEMA_ZONE))) {
            throw new IllegalArgumentException("Suất chiếu hiện không mở bán.");
        }
        List<ShowtimeSeat> selectedSeats = showtimeSeatRepository
                .findByShowtimeIdAndSeatIdIn(request.getShowtimeId(), request.getSeatIds());
        if (selectedSeats.size() != request.getSeatIds().size()) {
            throw new IllegalArgumentException("Có ghế không thuộc suất chiếu này. Vui lòng tải lại sơ đồ ghế.");
        }
        for (ShowtimeSeat seat : selectedSeats) {
            if (seat.getStatus() != SeatStatus.AVAILABLE) {
                throw new IllegalStateException("Ghế đã có người đặt, vui lòng chọn ghế khác!");
            }
        }
        return selectedSeats;
    }
}
