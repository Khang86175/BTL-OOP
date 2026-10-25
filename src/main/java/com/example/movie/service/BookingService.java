package com.example.movie.service;

import com.example.movie.dto.BookingRequest;
import com.example.movie.dto.BookingResponse;
import com.example.movie.model.*;
import com.example.movie.repository.*;
import com.example.movie.service.pricing.SeatPriceCalculator;
import com.example.movie.service.pricing.SeatPriceCalculatorFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;
    private final SeatPriceCalculatorFactory calculatorFactory;

    public BookingService(BookingRepository bookingRepository,
                          ShowtimeSeatRepository showtimeSeatRepository,
                          ShowtimeRepository showtimeRepository,
                          SeatRepository seatRepository,
                          UserRepository userRepository,
                          SeatPriceCalculatorFactory calculatorFactory) {
        this.bookingRepository = bookingRepository;
        this.showtimeSeatRepository = showtimeSeatRepository;
        this.showtimeRepository = showtimeRepository;
        this.seatRepository = seatRepository;
        this.userRepository = userRepository;
        this.calculatorFactory = calculatorFactory;
    }

    // 1. Tự sinh ghế nếu suất chiếu chưa có dữ liệu ghế
    @Transactional
    public List<ShowtimeSeat> getOrCreateShowtimeSeats(Long showtimeId) {
        List<ShowtimeSeat> seats = showtimeSeatRepository.findByShowtimeId(showtimeId);
        if (!seats.isEmpty()) {
            return seats;
        }

        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy suất chiếu với ID: " + showtimeId));

        // Lấy tất cả ghế và lọc theo ID của phòng chiếu
        Long targetRoomId = showtime.getRoom().getId();
        List<Seat> roomSeats = seatRepository.findAll().stream()
                .filter(seat -> seat.getRoom() != null && seat.getRoom().getId().equals(targetRoomId))
                .toList();

        List<ShowtimeSeat> generatedSeats = new ArrayList<>();
        for (Seat seat : roomSeats) {
            ShowtimeSeat ss = new ShowtimeSeat();
            ss.setShowtime(showtime);
            ss.setSeat(seat);
            ss.setStatus(SeatStatus.AVAILABLE);
            generatedSeats.add(ss);
        }

        return showtimeSeatRepository.saveAll(generatedSeats);
    }

    // 2. Tạo booking, dùng getter getSeatIds(), getShowtimeId() chuẩn xác
    @Transactional
    public BookingResponse createBooking(Long currentUserId, BookingRequest request) {
        if (request.getSeatIds() == null || request.getSeatIds().isEmpty() || request.getSeatIds().size() > 8) {
            throw new IllegalArgumentException("Số lượng ghế đặt trong một lượt phải từ 1 đến 8 ghế");
        }

        // Đảm bảo ghế của suất chiếu đã được khởi tạo
        getOrCreateShowtimeSeats(request.getShowtimeId());

        // Khóa các dòng ghế cần đặt trong database
        List<ShowtimeSeat> selectedSeats = showtimeSeatRepository
                .findSeatsForBookingWithLock(request.getShowtimeId(), request.getSeatIds());

        if (selectedSeats.size() != request.getSeatIds().size()) {
            throw new IllegalArgumentException("Một số ghế không tồn tại hoặc không thuộc suất chiếu này");
        }

        // Kiểm tra ghế còn trống không
        for (ShowtimeSeat s : selectedSeats) {
            if (s.getStatus() != SeatStatus.AVAILABLE) {
                throw new IllegalStateException("Ghế đã có người giữ hoặc đặt");
            }
        }

        // Tính tiền đa hình và chuyển trạng thái ghế sang HELD
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (ShowtimeSeat s : selectedSeats) {
            SeatPriceCalculator calculator = calculatorFactory.getCalculator(s.getSeat().getSeatType());
            totalAmount = totalAmount.add(calculator.calculate(s.getShowtime().getBasePrice()));
            s.setStatus(SeatStatus.HELD);
        }
        showtimeSeatRepository.saveAll(selectedSeats);

        // Lưu đơn đặt vé với trạng thái PENDING_PAYMENT
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin tài khoản người dùng"));

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setShowtime(selectedSeats.get(0).getShowtime());
        booking.setTotalAmount(totalAmount);
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        booking.setCreatedAt(LocalDateTime.now());

        Booking saved = bookingRepository.save(booking);

        // Trả về DTO an toàn
        List<String> seatLabels = selectedSeats.stream()
                .map(s -> "Ghế #" + s.getSeat().getId())
                .toList();

        return new BookingResponse(
                saved.getId(),
                saved.getShowtime().getId(),
                user.getId().toString(),
                saved.getTotalAmount(),
                saved.getStatus().name(),
                saved.getCreatedAt(),
                seatLabels
        );
    }
}