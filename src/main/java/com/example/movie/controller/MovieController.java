package com.example.movie.controller;

import com.example.movie.model.ScreeningStatus;
import com.example.movie.service.CategoryService;
import com.example.movie.service.MovieService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/movies")
public class MovieController {
    private final MovieService movies;
    private final CategoryService categories;

    public MovieController(MovieService movies, CategoryService categories) {
        this.movies = movies;
        this.categories = categories;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String q,
                       @RequestParam(required = false) Long categoryId,
                       @RequestParam(required = false) ScreeningStatus status,
                       @RequestParam(defaultValue = "releaseDateDesc") String sort, Model model) {
        model.addAttribute("movies", movies.findVisible(q, categoryId, status, sort));
        model.addAttribute("categories", categories.findAll());
        model.addAttribute("statuses", ScreeningStatus.values());
        model.addAttribute("q", q);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("status", status);
        model.addAttribute("sort", sort);
        return "movies/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("movie", movies.getVisibleMovie(id));
        return "movies/detail";
    }
}
