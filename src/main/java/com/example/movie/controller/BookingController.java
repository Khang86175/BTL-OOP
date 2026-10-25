package com.example.movie.controller;

import com.example.movie.dto.BookingRequest;
import com.example.movie.dto.BookingResponse;
import com.example.movie.model.ShowtimeSeat;
import com.example.movie.model.User;
import com.example.movie.repository.UserRepository;
import com.example.movie.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final UserRepository userRepository;

    public BookingController(BookingService bookingService, UserRepository userRepository) {
        this.bookingService = bookingService;
        this.userRepository = userRepository;
    }

    // Lấy sơ đồ ghế (tự sinh nếu rỗng)
    @GetMapping("/showtimes/{showtimeId}/seats")
    public ResponseEntity<List<ShowtimeSeat>> getSeats(@PathVariable Long showtimeId) {
        return ResponseEntity.ok(bookingService.getOrCreateShowtimeSeats(showtimeId));
    }

    // Đặt vé: Lấy User qua session, che password bằng BookingResponse
    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            @RequestBody BookingRequest request) {

        if (request.getUserId() == null) {
            return ResponseEntity.badRequest().build();
        }

        BookingResponse response = bookingService.createBooking(request.getUserId(), request);
        return ResponseEntity.ok(response);
    }
}