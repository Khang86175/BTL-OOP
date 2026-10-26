package com.example.movie.controller;

import com.example.movie.dto.BookingRequest;
import com.example.movie.dto.BookingPreview;
import com.example.movie.dto.BookingResponse;
import com.example.movie.model.User;
import com.example.movie.model.Booking;
import com.example.movie.repository.UserRepository;
import com.example.movie.service.BookingService;
import com.example.movie.service.PaymentQrService;
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
    private final PaymentQrService paymentQrService;

    public BookingViewController(BookingService bookingService, UserRepository userRepository,
                                 PaymentQrService paymentQrService) {
        this.bookingService = bookingService;
        this.userRepository = userRepository;
        this.paymentQrService = paymentQrService;
    }

    // ==========================================
    // 1. Mở trang chọn ghế (Khi khách bấm vào 1 suất chiếu)
    // ==========================================
    @GetMapping("/showtimes/{showtimeId}")
    public String showSeatSelection(@PathVariable Long showtimeId, Model model) {
        // Lấy danh sách ghế từ Service và đẩy lên giao diện (View)
        model.addAttribute("showtimeId", showtimeId);
        model.addAttribute("seats", bookingService.getOrCreateShowtimeSeats(showtimeId));
        return "booking/seat-selection"; // Trỏ tới file seat-selection.html
    }

    @GetMapping("/confirmation")
    public String showConfirmation(Model model) {
        if (!model.containsAttribute("preview")) {
            return "redirect:/showtimes";
        }
        return "booking/confirmation";
    }

    @PostMapping("/payment")
    public String createPayment(@RequestParam("showtimeId") Long showtimeId,
                                @RequestParam("seatIds") List<Long> seatIds,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        try {
            User user = currentUser(principal);
            BookingRequest request = new BookingRequest();
            request.setShowtimeId(showtimeId);
            request.setSeatIds(seatIds);
            BookingResponse booking = bookingService.createBooking(user.getId(), request);
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
        model.addAttribute("paymentQrUrl", paymentQrService.createQrUrl(booking.getId(), booking.getTotalAmount()));
        model.addAttribute("paymentQrConfigured", paymentQrService.isConfigured());
        return "booking/payment";
    }

    @PostMapping("/payment/{bookingId}/complete")
    public String completePayment(@PathVariable Long bookingId, Principal principal,
                                  RedirectAttributes redirectAttributes) {
        try {
            bookingService.markAsPaid(currentUser(principal).getId(), bookingId);
            redirectAttributes.addFlashAttribute("success", "Thanh toán thành công.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/bookings/history";
    }

    @GetMapping("/history")
    public String showHistory(Principal principal, Model model) {
        model.addAttribute("bookings", bookingService.getUserBookings(currentUser(principal).getId()));
        return "booking/history";
    }

    // ==========================================
    // 2. Xử lý khi khách bấm nút "Xác nhận đặt vé"
    // ==========================================
    @PostMapping
    public String processBooking(@RequestParam("showtimeId") Long showtimeId,
                                 @RequestParam(value = "seatIds", required = false) List<Long> seatIds,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {

        // BƯỚC 1: Kiểm tra xem khách đã tích chọn ghế nào chưa
        if (seatIds == null || seatIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng chọn ít nhất 1 ghế!");
            return "redirect:/bookings/showtimes/" + showtimeId;
        }

        try {
            User currentUser = currentUser(principal);

            // BƯỚC 3: Đóng gói dữ liệu và gọi Service Đặt vé
            BookingRequest request = new BookingRequest();
            request.setShowtimeId(showtimeId);
            request.setSeatIds(seatIds);

            BookingPreview preview = bookingService.previewBooking(currentUser.getId(), request);
            redirectAttributes.addFlashAttribute("preview", preview);
            return "redirect:/bookings/confirmation";

        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/bookings/showtimes/" + showtimeId;
    }

    private User currentUser(Principal principal) {
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy tài khoản đang đăng nhập."));
    }
}
