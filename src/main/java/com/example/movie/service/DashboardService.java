package com.example.movie.service;

import com.example.movie.dto.MovieRevenue;
import com.example.movie.dto.RevenueBreakdown;
import com.example.movie.model.BookingStatus;
import com.example.movie.model.PaymentStatus;
import com.example.movie.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DashboardService {
    private final PaymentRepository payments;

    public DashboardService(PaymentRepository payments) {
        this.payments = payments;
    }

    @Transactional(readOnly = true)
    public BigDecimal monthlyRevenue(LocalDateTime from, LocalDateTime to) {
        ScheduleAccess.admin();
        return payments.sumSuccessfulBetween(PaymentStatus.SUCCESS, BookingStatus.CONFIRMED, from, to);
    }

    @Transactional(readOnly = true)
    public List<MovieRevenue> topMovies(LocalDateTime from, LocalDateTime to) {
        ScheduleAccess.admin();
        return payments.revenueByMovie(PaymentStatus.SUCCESS, BookingStatus.CONFIRMED, from, to)
                .stream()
                .map(row -> new MovieRevenue((String) row[0], (BigDecimal) row[1]))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RevenueBreakdown> topTheaters(LocalDateTime from, LocalDateTime to) {
        ScheduleAccess.admin();
        return payments.revenueByTheater(PaymentStatus.SUCCESS, BookingStatus.CONFIRMED, from, to)
                .stream()
                .map(row -> new RevenueBreakdown((String) row[0], (BigDecimal) row[1]))
                .toList();
    }
}
