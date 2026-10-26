package com.example.movie.service;

import com.example.movie.dto.BookingRequest;
import com.example.movie.dto.BookingResponse;
import com.example.movie.model.*;
import com.example.movie.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public BookingService(BookingRepository bookingRepository,
                          BookingSeatRepository bookingSeatRepository,
                          ShowtimeSeatRepository showtimeSeatRepository,
                          ShowtimeRepository showtimeRepository,
                          SeatRepository seatRepository,
                          UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.showtimeSeatRepository = showtimeSeatRepository;
        this.showtimeRepository = showtimeRepository;
        this.seatRepository = seatRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public List<ShowtimeSeat> getOrCreateShowtimeSeats(Long showtimeId) {
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
        Showtime showtime = findShowtime(request.getShowtimeId());
        List<ShowtimeSeat> selectedSeats = validateAndGetSelectedSeats(request);

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<String> seatNames = new ArrayList<>();
        for (ShowtimeSeat seat : selectedSeats) {
            totalAmount = totalAmount.add(showtime.priceFor(seat.getSeat().getSeatType()));
            seat.setStatus(SeatStatus.BOOKED);
            seatNames.add(seat.getSeat().getRowLabel() + seat.getSeat().getSeatNumber());
        }
        showtimeSeatRepository.saveAll(selectedSeats);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user."));
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setShowtime(showtime);
        booking.setTotalAmount(totalAmount);
        // Keep the original booking status for compatibility with existing MySQL ENUM values.
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentStatus(PaymentStatus.PENDING);
        booking.setCreatedAt(LocalDateTime.now(Movie.CINEMA_ZONE));
        Booking savedBooking = bookingRepository.save(booking);

        for (ShowtimeSeat selectedSeat : selectedSeats) {
            bookingSeatRepository.save(new BookingSeat(savedBooking, selectedSeat.getSeat(), showtime,
                    showtime.priceFor(selectedSeat.getSeat().getSeatType())));
        }

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
        Booking booking = bookingRepository.findByIdAndUserId(bookingId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt vé."));
        if (booking.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException("Đơn đặt vé này đã được xử lý.");
        }
        booking.setPaymentStatus(PaymentStatus.PAID);
        return bookingRepository.save(booking);
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
