package com.example.movie.service;

import com.example.movie.dto.CategoryForm;
import com.example.movie.model.Category;
import com.example.movie.repository.CategoryRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
@Validated
@Transactional(readOnly = true)
public class CategoryService {
    private final CategoryRepository repository;
    public CategoryService(CategoryRepository repository) { this.repository = repository; }

    public List<Category> findAll() { return repository.findAll(Sort.by("name")); }
    public Category getById(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thể loại."));
    }

    @Transactional
    public Category create(@Valid CategoryForm form) { return save(new Category(), form); }

    @Transactional
    public Category update(Long id, @Valid CategoryForm form) { return save(getById(id), form); }

    private Category save(Category category, CategoryForm form) {
        String name = form.getName().strip().replaceAll("\\s+", " ");
        repository.findByNameIgnoreCase(name).filter(existing -> !existing.getId().equals(category.getId()))
                .ifPresent(existing -> { throw new CatalogValidationException("name", "Thể loại này đã tồn tại."); });
        category.setName(name);
        return repository.saveAndFlush(category);
    }
}
