package com.example.movie.service;

import com.example.movie.dto.MovieForm;
import com.example.movie.model.*;
import com.example.movie.repository.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

@Service
@Validated
@Transactional(readOnly = true)
public class MovieService {
    private final MovieRepository movies;
    private final CategoryRepository categories;

    public MovieService(MovieRepository movies, CategoryRepository categories) {
        this.movies = movies;
        this.categories = categories;
    }

    public List<Movie> findVisible(String query, Long categoryId, ScreeningStatus status, String sort) {
        return search(query, categoryId, status, sort, false);
    }

    public List<Movie> findForAdmin(String query, Long categoryId, ScreeningStatus status, String sort) {
        return search(query, categoryId, status, sort, true);
    }

    private List<Movie> search(String query, Long categoryId, ScreeningStatus status, String sort, boolean includeHidden) {
        LocalDate today = LocalDate.now(Movie.CINEMA_ZONE);
        Specification<Movie> spec = (root, criteria, cb) -> cb.conjunction();
        if (!includeHidden) spec = spec.and((root, criteria, cb) -> cb.isTrue(root.get("visible")));
        if (query != null && !query.isBlank()) {
            String escaped = query.strip().toLowerCase(Locale.ROOT)
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            spec = spec.and((root, criteria, cb) -> cb.like(cb.lower(root.get("title")), "%" + escaped + "%", '\\'));
        }
        if (categoryId != null) {
            spec = spec.and((root, criteria, cb) -> {
                criteria.distinct(true);
                return cb.equal(root.join("categories").get("id"), categoryId);
            });
        }
        if (status == ScreeningStatus.NOW_SHOWING)
            spec = spec.and((root, criteria, cb) -> cb.lessThanOrEqualTo(root.get("releaseDate"), today));
        if (status == ScreeningStatus.COMING_SOON)
            spec = spec.and((root, criteria, cb) -> cb.greaterThan(root.get("releaseDate"), today));
        Sort ordering = switch (sort == null ? "" : sort) {
            case "title" -> Sort.by("title").ascending();
            case "releaseDateAsc" -> Sort.by("releaseDate").ascending();
            default -> Sort.by("releaseDate").descending();
        };
        return movies.findAll(spec, ordering.and(Sort.by("id")));
    }

    public Movie getById(Long id) {
        return movies.findById(id).orElseThrow(MovieService::notFound);
    }

    /** Điểm bàn giao cho module lịch chiếu: gồm ID, thời lượng và thể loại đã tải sẵn. */
    public Movie getVisibleMovie(Long id) {
        Movie movie = getById(id);
        if (!movie.isVisible()) throw notFound();
        return movie;
    }

    @Transactional
    public Movie create(@Valid MovieForm form) { return save(new Movie(), form); }

    @Transactional
    public Movie update(Long id, @Valid MovieForm form) { return save(getById(id), form); }

    private Movie save(Movie movie, MovieForm form) {
        List<Category> selected = categories.findAllById(form.getCategoryIds());
        if (selected.size() != form.getCategoryIds().size())
            throw new CatalogValidationException("categoryIds", "Có thể loại không tồn tại. Vui lòng chọn lại.");
        movie.setTitle(form.getTitle().strip());
        movie.setDirector(form.getDirector().strip());
        movie.setDurationMinutes(form.getDurationMinutes());
        movie.setReleaseDate(form.getReleaseDate());
        movie.setAgeRating(form.getAgeRating());
        movie.setSynopsis(form.getSynopsis().strip());
        movie.setPosterUrl(trimUrl(form.getPosterUrl()));
        movie.setTrailerUrl(trimUrl(form.getTrailerUrl()));
        movie.setVisible(form.isVisible());
        movie.setCategories(new LinkedHashSet<>(selected));
        return movies.save(movie);
    }

    /** Chỉ ẩn/hiện, không xóa bản ghi để bảo toàn tham chiếu từ lịch chiếu và booking. */
    @Transactional
    public void setVisibility(Long id, boolean visible) { getById(id).setVisible(visible); }

    private static String trimUrl(String url) { return url == null || url.isBlank() ? null : url.strip(); }
    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phim.");
    }
}
