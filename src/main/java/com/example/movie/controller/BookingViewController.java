package com.example.movie.controller;

import com.example.movie.dto.BookingPreview;
import com.example.movie.dto.BookingRequest;
import com.example.movie.dto.BookingResponse;
import com.example.movie.model.Booking;
import com.example.movie.model.User;
import com.example.movie.repository.UserRepository;
import com.example.movie.repository.TicketRepository;
import com.example.movie.service.BookingService;
import com.example.movie.service.PaymentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/bookings")
public class BookingViewController {

    private final BookingService bookingService;
    private final UserRepository userRepository;
    private final PaymentService paymentService;
    private final TicketRepository ticketRepository;

    public BookingViewController(BookingService bookingService, UserRepository userRepository,
                                 PaymentService paymentService, TicketRepository ticketRepository) {
        this.bookingService = bookingService;
        this.userRepository = userRepository;
        this.paymentService = paymentService;
        this.ticketRepository = ticketRepository;
    }

    @GetMapping("/showtimes/{showtimeId}")
    public String showSeatSelection(@PathVariable Long showtimeId, Model model) {
        model.addAttribute("showtimeId", showtimeId);
        model.addAttribute("seats", bookingService.getOrCreateShowtimeSeats(showtimeId));
        return "booking/seat-selection";
    }

    @GetMapping("/confirmation")
    public String showConfirmation(Model model) {
        if (!model.containsAttribute("preview")) {
            return "redirect:/showtimes";
        }
        return "booking/confirmation";
    }

    @PostMapping
    public String processBooking(@RequestParam("showtimeId") Long showtimeId,
                                 @RequestParam(value = "seatIds", required = false) List<Long> seatIds,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        if (seatIds == null || seatIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng chọn ít nhất 1 ghế!");
            return "redirect:/bookings/showtimes/" + showtimeId;
        }
        try {
            User user = currentUser(principal);
            BookingRequest request = new BookingRequest();
            request.setShowtimeId(showtimeId);
            request.setSeatIds(seatIds);
            BookingPreview preview = bookingService.previewBooking(user.getId(), request);
            redirectAttributes.addFlashAttribute("preview", preview);
            return "redirect:/bookings/confirmation";
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/bookings/showtimes/" + showtimeId;
        }
    }

    @PostMapping("/payment")
    public String createBooking(@RequestParam("showtimeId") Long showtimeId,
                                @RequestParam("seatIds") List<Long> seatIds,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        try {
            BookingRequest request = new BookingRequest();
            request.setShowtimeId(showtimeId);
            request.setSeatIds(seatIds);
            BookingResponse booking = bookingService.createBooking(currentUser(principal).getId(), request);
            return "redirect:/bookings/payment/" + booking.getBookingId();
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/bookings/showtimes/" + showtimeId;
        }
    }

    @GetMapping("/payment/{bookingId}")
    public String showPayment(@PathVariable Long bookingId, Principal principal, Model model) {
        Booking booking = bookingService.getUserBooking(currentUser(principal).getId(), bookingId);
        model.addAttribute("booking", booking);
        model.addAttribute("paymentMethods", List.of("CARD", "WALLET"));
        model.addAttribute("selectedMethod", "CARD");
        return "booking/payment";
    }

    @PostMapping("/payment/{bookingId}/process")
    public String processPayment(@PathVariable Long bookingId,
                                 @RequestParam String method,
                                 @RequestParam(defaultValue = "SUCCESS") String outcome,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        try {
            var result = paymentService.process(currentUser(principal).getId(), bookingId,
                    method, "SUCCESS".equalsIgnoreCase(outcome));
            if (result.successful()) {
                redirectAttributes.addFlashAttribute("success", "Thanh toán giả lập thành công.");
                return "redirect:/bookings/history";
            }
            redirectAttributes.addFlashAttribute("error", "Thanh toán giả lập thất bại. Bạn có thể thử lại.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/bookings/payment/" + bookingId;
    }

    @GetMapping("/history")
    public String showHistory(Principal principal, Model model) {
        Long userId = currentUser(principal).getId();
        model.addAttribute("bookings", bookingService.getUserBookings(userId));
        model.addAttribute("payments", paymentService.getUserPayments(userId));
        return "booking/history";
    }

    @GetMapping("/tickets/{bookingId}")
    public String showTickets(@PathVariable Long bookingId, Principal principal, Model model) {
        Booking booking = bookingService.getUserBooking(currentUser(principal).getId(), bookingId);
        model.addAttribute("booking", booking);
        model.addAttribute("tickets", ticketRepository.findByBookingIdOrderByIdAsc(bookingId));
        return "booking/tickets";
    }

    private User currentUser(Principal principal) {
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy tài khoản đang đăng nhập."));
    }
}
