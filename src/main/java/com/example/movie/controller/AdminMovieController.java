package com.example.movie.controller;

import com.example.movie.dto.MovieForm;
import com.example.movie.model.*;
import com.example.movie.service.*;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/movies")
public class AdminMovieController {
    private final MovieService movies;
    private final CategoryService categories;

    public AdminMovieController(MovieService movies, CategoryService categories) {
        this.movies = movies;
        this.categories = categories;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String q,
                       @RequestParam(required = false) Long categoryId,
                       @RequestParam(required = false) ScreeningStatus status,
                       @RequestParam(defaultValue = "releaseDateDesc") String sort, Model model) {
        model.addAttribute("movies", movies.findForAdmin(q, categoryId, status, sort));
        model.addAttribute("categories", categories.findAll());
        model.addAttribute("statuses", ScreeningStatus.values());
        model.addAttribute("q", q);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("status", status);
        model.addAttribute("sort", sort);
        return "admin/movies/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("movieForm", new MovieForm());
        return prepareForm(null, model);
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("movieForm") MovieForm form, BindingResult errors,
                         Model model, RedirectAttributes redirect) {
        if (errors.hasErrors()) return prepareForm(null, model);
        try {
            movies.create(form);
        } catch (CatalogValidationException ex) {
            errors.rejectValue(ex.getField(), "catalog.invalid", ex.getMessage());
            return prepareForm(null, model);
        }
        redirect.addFlashAttribute("success", "Đã thêm phim.");
        return "redirect:/admin/movies";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("movieForm", MovieForm.from(movies.getById(id)));
        return prepareForm(id, model);
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("movieForm") MovieForm form,
                         BindingResult errors, Model model, RedirectAttributes redirect) {
        movies.getById(id);
        if (errors.hasErrors()) return prepareForm(id, model);
        try {
            movies.update(id, form);
        } catch (CatalogValidationException ex) {
            errors.rejectValue(ex.getField(), "catalog.invalid", ex.getMessage());
            return prepareForm(id, model);
        }
        redirect.addFlashAttribute("success", "Đã cập nhật phim.");
        return "redirect:/admin/movies";
    }

    @PostMapping("/{id}/visibility")
    public String visibility(@PathVariable Long id, @RequestParam boolean visible, RedirectAttributes redirect) {
        movies.setVisibility(id, visible);
        redirect.addFlashAttribute("success", visible ? "Đã hiển thị phim." : "Đã ẩn phim khỏi trang khách.");
        return "redirect:/admin/movies";
    }

    private String prepareForm(Long id, Model model) {
        model.addAttribute("movieId", id);
        model.addAttribute("categories", categories.findAll());
        model.addAttribute("ageRatings", AgeRating.values());
        return "admin/movies/form";
    }
}
