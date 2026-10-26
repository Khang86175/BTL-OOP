package com.example.movie.service;

import com.example.movie.model.*;
import com.example.movie.repository.*;
import com.example.movie.service.payment.PaymentResult;
import com.example.movie.service.payment.PaymentStrategy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class PaymentService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final TicketRepository ticketRepository;
    private final Map<String, PaymentStrategy> strategies;

    public PaymentService(BookingRepository bookingRepository,
                          PaymentRepository paymentRepository,
                          BookingSeatRepository bookingSeatRepository,
                          ShowtimeSeatRepository showtimeSeatRepository,
                          TicketRepository ticketRepository,
                          Map<String, PaymentStrategy> strategies) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.showtimeSeatRepository = showtimeSeatRepository;
        this.ticketRepository = ticketRepository;
        this.strategies = strategies;
    }

    @Transactional
    public PaymentResult process(Long userId, Long bookingId, String method, boolean demoSuccess) {
        Booking booking = bookingRepository.findForUpdate(bookingId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt vé."));
        if (booking.getStatus() == BookingStatus.CONFIRMED
                || booking.getPaymentStatus() == PaymentStatus.SUCCESS) {
            throw new IllegalStateException("Đơn đặt vé này đã thanh toán thành công, không thể thanh toán lần hai.");
        }
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("Đơn đặt vé không còn trong thời gian thanh toán.");
        }
        LocalDateTime now = LocalDateTime.now(Movie.CINEMA_ZONE);
        if (booking.getHoldExpiresAt() == null || !booking.getHoldExpiresAt().isAfter(now)) {
            releaseExpiredBooking(booking);
            throw new IllegalStateException("Thời gian giữ ghế đã hết. Vui lòng đặt vé lại.");
        }
        validateMethod(method);

        BigDecimal amount = booking.getTotalAmount();
        Payment payment = new Payment(booking, method, amount, PaymentStatus.PENDING,
                null, now);
        payment = paymentRepository.saveAndFlush(payment);

        PaymentResult result = demoSuccess
                ? strategies.get(method).pay(amount)
                : new PaymentResult(false, "MOCK-FAILED-" + payment.getId());
        if (!result.successful()) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setTransactionRef(result.transactionRef());
            paymentRepository.save(payment);
            return result;
        }
        if (payment.getAmount() == null || amount == null
                || payment.getAmount().compareTo(amount) != 0) {
            throw new IllegalStateException("Số tiền thanh toán không khớp tổng tiền vé.");
        }

        List<BookingSeat> bookingSeats = bookingSeatRepository.findByBookingId(bookingId);
        List<Long> seatIds = bookingSeats.stream().map(s -> s.getSeat().getId()).toList();
        List<ShowtimeSeat> heldSeats = showtimeSeatRepository
                .findByShowtimeIdAndSeatIdIn(booking.getShowtime().getId(), seatIds);
        if (bookingSeats.isEmpty() || heldSeats.size() != seatIds.size()
                || heldSeats.stream().anyMatch(s -> s.getStatus() != SeatStatus.BOOKED)) {
            throw new IllegalStateException("Ghế giữ cho đơn này không còn khả dụng.");
        }
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(now);
        payment.setTransactionRef(result.transactionRef());
        paymentRepository.save(payment);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentStatus(PaymentStatus.SUCCESS);
        bookingRepository.save(booking);
        issueTickets(booking, bookingSeats);
        return result;
    }

    private void validateMethod(String method) {
        if (method == null || !strategies.containsKey(method)
                || !java.util.Arrays.stream(PaymentMethod.values()).anyMatch(m -> m.name().equals(method))) {
            throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ.");
        }
    }

    public List<Payment> getUserPayments(Long userId) {
        return paymentRepository.findByBookingUserIdOrderByCreatedAtDesc(userId);
    }

    private void issueTickets(Booking booking, List<BookingSeat> bookingSeats) {
        if (ticketRepository.existsByBookingId(booking.getId())) {
            return;
        }
        bookingSeats.forEach(seat -> ticketRepository.save(
                new Ticket(booking, booking.getShowtime(), seat.getSeat())));
    }

    private void releaseExpiredBooking(Booking booking) {
        bookingSeatRepository.findByBookingId(booking.getId()).forEach(bookingSeat ->
                showtimeSeatRepository.findByShowtimeIdAndSeatIdIn(
                        bookingSeat.getShowtime().getId(), List.of(bookingSeat.getSeat().getId()))
                        .forEach(seat -> {
                            if (seat.getStatus() == SeatStatus.BOOKED) {
                                seat.setStatus(SeatStatus.AVAILABLE);
                                showtimeSeatRepository.save(seat);
                            }
                        }));
        paymentRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId()).stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.PENDING)
                .forEach(payment -> {
                    payment.setStatus(PaymentStatus.FAILED);
                    paymentRepository.save(payment);
                });
        booking.setStatus(BookingStatus.EXPIRED);
        booking.setPaymentStatus(PaymentStatus.FAILED);
        bookingRepository.save(booking);
    }
}
