package com.example.movie.controller;

import com.example.movie.dto.BookingRequest;
import com.example.movie.model.Booking;
import com.example.movie.model.ShowtimeSeat;
import com.example.movie.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    // 1. API lấy sơ đồ ghế theo suất chiếu
    @GetMapping("/showtimes/{showtimeId}/seats")
    public ResponseEntity<List<ShowtimeSeat>> getSeatsByShowtime(@PathVariable Long showtimeId) {
        return ResponseEntity.ok(bookingService.getSeatsByShowtime(showtimeId));
    }

    // 2. API thực hiện đặt vé
    @PostMapping
    public ResponseEntity<Booking> createBooking(@RequestBody BookingRequest request) {
        Booking createdBooking = bookingService.createBooking(request);
        return ResponseEntity.ok(createdBooking);
    }

    // 3. API xem chi tiết một hóa đơn
    @GetMapping("/{bookingId}")
    public ResponseEntity<Booking> getBookingDetails(@PathVariable Long bookingId) {
        return ResponseEntity.ok(bookingService.getBookingById(bookingId));
    }

    // 4. API lấy lịch sử đặt vé của một người dùng
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Booking>> getUserBookings(@PathVariable Long userId) {
        return ResponseEntity.ok(bookingService.getBookingsByUser(userId));
    }
}