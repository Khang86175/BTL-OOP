package com.example.movie.dto;

import com.example.movie.model.AgeRating;
import com.example.movie.model.Movie;
import com.example.movie.validation.HttpUrl;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class MovieForm {
    @NotBlank(message = "Vui lòng nhập tên phim.")
    @Size(max = 200, message = "Tên phim tối đa 200 ký tự.")
    private String title;
    @NotBlank(message = "Vui lòng nhập đạo diễn.")
    @Size(max = 150, message = "Tên đạo diễn tối đa 150 ký tự.")
    private String director;
    @NotNull(message = "Vui lòng nhập thời lượng.")
    @Min(value = 1, message = "Thời lượng ít nhất 1 phút.")
    @Max(value = 600, message = "Thời lượng tối đa 600 phút.")
    private Integer durationMinutes;
    @NotNull(message = "Vui lòng nhập ngày khởi chiếu.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate releaseDate;
    @NotNull(message = "Vui lòng chọn giới hạn tuổi.")
    private AgeRating ageRating = AgeRating.P;
    @NotBlank(message = "Vui lòng nhập nội dung phim.")
    @Size(max = 10000, message = "Nội dung tối đa 10.000 ký tự.")
    private String synopsis;
    @HttpUrl
    @Size(max = 2048, message = "URL tối đa 2.048 ký tự.")
    private String posterUrl;
    @HttpUrl
    @Size(max = 2048, message = "URL tối đa 2.048 ký tự.")
    private String trailerUrl;
    private boolean visible = true;
    @NotEmpty(message = "Vui lòng chọn ít nhất một thể loại.")
    private Set<@NotNull @Positive Long> categoryIds = new LinkedHashSet<>();

    public static MovieForm from(Movie movie) {
        MovieForm form = new MovieForm();
        form.title = movie.getTitle();
        form.director = movie.getDirector();
        form.durationMinutes = movie.getDurationMinutes();
        form.releaseDate = movie.getReleaseDate();
        form.ageRating = movie.getAgeRating();
        form.synopsis = movie.getSynopsis();
        form.posterUrl = movie.getPosterUrl();
        form.trailerUrl = movie.getTrailerUrl();
        form.visible = movie.isVisible();
        form.categoryIds = movie.getCategories().stream().map(c -> c.getId())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return form;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDirector() { return director; }
    public void setDirector(String director) { this.director = director; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public LocalDate getReleaseDate() { return releaseDate; }
    public void setReleaseDate(LocalDate releaseDate) { this.releaseDate = releaseDate; }
    public AgeRating getAgeRating() { return ageRating; }
    public void setAgeRating(AgeRating ageRating) { this.ageRating = ageRating; }
    public String getSynopsis() { return synopsis; }
    public void setSynopsis(String synopsis) { this.synopsis = synopsis; }
    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }
    public String getTrailerUrl() { return trailerUrl; }
    public void setTrailerUrl(String trailerUrl) { this.trailerUrl = trailerUrl; }
    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
    public Set<Long> getCategoryIds() { return categoryIds; }
    public void setCategoryIds(Set<Long> categoryIds) { this.categoryIds = categoryIds; }
}
