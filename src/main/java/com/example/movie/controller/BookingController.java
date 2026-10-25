package com.example.movie.controller;

import com.example.movie.dto.BookingRequest;
import com.example.movie.dto.BookingResponse;
import com.example.movie.model.ShowtimeSeat;
import com.example.movie.repository.UserRepository;
import com.example.movie.service.BookingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
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

    // Resolve the account from the authenticated session, never from a client-supplied user ID.
    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            @RequestBody BookingRequest request, Principal principal) {
        var user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        BookingResponse response = bookingService.createBooking(user.getId(), request);
        return ResponseEntity.ok(response);
    }
}