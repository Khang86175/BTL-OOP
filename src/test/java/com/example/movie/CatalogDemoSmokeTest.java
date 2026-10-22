package com.example.movie;

import com.example.movie.model.Movie;
import com.example.movie.repository.MovieRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Không dùng transaction ở test: kiểm tra template vẫn tải được thể loại khi open-in-view=false. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class CatalogDemoSmokeTest {
    @Autowired MockMvc mvc;
    @Autowired MovieRepository movies;

    @Test
    void demoBootsSeedsAndRendersPublicPagesWithoutOpenInView() throws Exception {
        assertThat(movies.count()).isEqualTo(6);
        mvc.perform(get("/")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Hành trình qua miền gió")));
        mvc.perform(get("/movies")).andExpect(status().isOk())
                .andExpect(content().string(containsString("5 phim phù hợp")));
        var movie = movies.findAll().stream().filter(Movie::isVisible).findFirst().orElseThrow();
        mvc.perform(get("/movies/{id}", movie.getId())).andExpect(status().isOk());
        mvc.perform(get("/images/poster-placeholder.svg")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPagesRenderWithoutOpenInView() throws Exception {
        mvc.perform(get("/admin/movies")).andExpect(status().isOk());
        mvc.perform(get("/admin/movies/new")).andExpect(status().isOk());
        var movie = movies.findAll().getFirst();
        mvc.perform(get("/admin/movies/{id}/edit", movie.getId())).andExpect(status().isOk());
        mvc.perform(get("/admin/categories")).andExpect(status().isOk());
    }
}
