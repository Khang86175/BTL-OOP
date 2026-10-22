package com.example.movie.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;
import com.example.movie.model.ScreeningStatus;
import com.example.movie.service.MovieService;

@Controller
public class HomeController {
    private final MovieService movies;

    public HomeController(MovieService movies) { this.movies = movies; }

    @GetMapping("/")
    public String homePage(Model model) {
        model.addAttribute("movies", movies.findVisible(null, null, ScreeningStatus.NOW_SHOWING, "releaseDateDesc")
                .stream().limit(6).toList());
        return "index";
    }
}
