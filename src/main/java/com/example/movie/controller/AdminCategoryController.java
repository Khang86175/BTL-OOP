package com.example.movie.controller;

import com.example.movie.dto.CategoryForm;
import com.example.movie.service.*;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/categories")
public class AdminCategoryController {
    private final CategoryService categories;
    public AdminCategoryController(CategoryService categories) { this.categories = categories; }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("categoryForm", new CategoryForm());
        return prepareForm(null, model);
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        CategoryForm form = new CategoryForm();
        form.setName(categories.getById(id).getName());
        model.addAttribute("categoryForm", form);
        return prepareForm(id, model);
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("categoryForm") CategoryForm form, BindingResult errors,
                         Model model, RedirectAttributes redirect) {
        return save(null, form, errors, model, redirect);
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("categoryForm") CategoryForm form,
                         BindingResult errors, Model model, RedirectAttributes redirect) {
        categories.getById(id);
        return save(id, form, errors, model, redirect);
    }

    private String save(Long id, CategoryForm form, BindingResult errors, Model model, RedirectAttributes redirect) {
        if (errors.hasErrors()) return prepareForm(id, model);
        try {
            if (id == null) categories.create(form); else categories.update(id, form);
        } catch (CatalogValidationException ex) {
            errors.rejectValue(ex.getField(), "catalog.invalid", ex.getMessage());
            return prepareForm(id, model);
        } catch (DataIntegrityViolationException ex) {
            errors.rejectValue("name", "catalog.duplicate", "Thể loại này đã tồn tại.");
            return prepareForm(id, model);
        }
        redirect.addFlashAttribute("success", id == null ? "Đã thêm thể loại." : "Đã cập nhật thể loại.");
        return "redirect:/admin/categories";
    }

    private String prepareForm(Long id, Model model) {
        model.addAttribute("categoryId", id);
        model.addAttribute("categories", categories.findAll());
        return "admin/categories/list";
    }
}
