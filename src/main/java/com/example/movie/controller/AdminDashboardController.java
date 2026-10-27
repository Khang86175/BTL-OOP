package com.example.movie.controller;

import com.example.movie.model.Movie;
import com.example.movie.service.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.YearMonth;
import java.time.ZoneId;

@Controller
@RequestMapping("/admin/dashboard")
public class AdminDashboardController {
    private static final ZoneId CINEMA_ZONE = Movie.CINEMA_ZONE;
    private final DashboardService dashboard;

    public AdminDashboardController(DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping
    public String dashboard(@RequestParam(required = false) Integer year,
                            @RequestParam(required = false) Integer month,
                            Model model) {
        YearMonth selected = resolveMonth(year, month);
        var from = selected.atDay(1).atStartOfDay();
        var to = selected.plusMonths(1).atDay(1).atStartOfDay();
        model.addAttribute("selectedYear", selected.getYear());
        model.addAttribute("selectedMonth", selected.getMonthValue());
        model.addAttribute("totalRevenue", dashboard.monthlyRevenue(from, to));
        model.addAttribute("topMovies", dashboard.topMovies(from, to));
        model.addAttribute("topTheaters", dashboard.topTheaters(from, to));
        return "admin/dashboard";
    }

    private YearMonth resolveMonth(Integer year, Integer month) {
        YearMonth current = YearMonth.now(CINEMA_ZONE);
        if (year == null || month == null || month < 1 || month > 12 || year < 2000 || year > 2100) {
            return current;
        }
        return YearMonth.of(year, month);
    }
}
