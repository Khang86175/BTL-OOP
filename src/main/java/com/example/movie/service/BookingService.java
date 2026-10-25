package com.example.movie.service;

import com.example.movie.dto.BookingRequest;
import com.example.movie.model.*;
import com.example.movie.repository.*;
import com.example.movie.service.pricing.SeatPriceCalculator;
import com.example.movie.service.pricing.SeatPriceCalculatorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingService {

    @Autowired
    private ShowtimeSeatRepository showtimeSeatRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingSeatRepository bookingSeatRepository;

    @Autowired
    private ShowtimeRepository showtimeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SeatPriceCalculatorFactory priceCalculatorFactory;

    public List<ShowtimeSeat> getSeatsByShowtime(Long showtimeId) {
        return showtimeSeatRepository.findByShowtimeId(showtimeId);
    }

    public Booking getBookingById(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt vé ID: " + bookingId));
    }

    public List<Booking> getBookingsByUser(Long userId) {
        return bookingRepository.findByUserId(userId);
    }

    @Transactional
    public Booking createBooking(BookingRequest request) {
        if (request.getSeatIds() == null || request.getSeatIds().isEmpty()) {
            throw new IllegalArgumentException("Danh sách ghế chọn không được để trống!");
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại ID: " + request.getUserId()));

        Showtime showtime = showtimeRepository.findById(request.getShowtimeId())
                .orElseThrow(() -> new IllegalArgumentException("Suất chiếu không tồn tại ID: " + request.getShowtimeId()));

        if (!showtime.isBookableAt(LocalDateTime.now())) {
            throw new IllegalStateException("Suất chiếu này hiện không thể đặt vé!");
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setShowtime(showtime);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setCreatedAt(LocalDateTime.now());
        booking.setTotalAmount(BigDecimal.ZERO);
        booking = bookingRepository.save(booking);

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Long seatId : request.getSeatIds()) {
            ShowtimeSeat showtimeSeat = showtimeSeatRepository.findByShowtimeIdAndSeatId(request.getShowtimeId(), seatId)
                    .orElseThrow(() -> new IllegalArgumentException("Ghế ID " + seatId + " không thuộc suất chiếu này!"));

            if (showtimeSeat.getStatus() != SeatStatus.AVAILABLE) {
                throw new IllegalStateException("Ghế " + showtimeSeat.getSeat().getSeatNumber() + " đã có người đặt hoặc đang giữ chỗ!");
            }

            showtimeSeat.setStatus(SeatStatus.BOOKED);
            showtimeSeatRepository.save(showtimeSeat);

            SeatPriceCalculator calculator = priceCalculatorFactory.getCalculator(showtimeSeat.getSeat().getSeatType());
            BigDecimal seatPrice = calculator.calculate(showtime.getBasePrice());

            totalAmount = totalAmount.add(seatPrice);

            BookingSeat bookingSeat = new BookingSeat(booking, showtimeSeat.getSeat(), seatPrice);
            bookingSeatRepository.save(bookingSeat);
        }

        booking.setTotalAmount(totalAmount);
        return bookingRepository.save(booking);
    }
}