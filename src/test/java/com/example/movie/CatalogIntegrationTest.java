package com.example.movie;

import com.example.movie.dto.*;
import com.example.movie.model.*;
import com.example.movie.repository.*;
import com.example.movie.service.*;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CatalogIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired MovieService movies;
    @Autowired CategoryService categories;
    @Autowired MovieRepository movieRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwords;
    Category family, adventure;
    Movie current, upcoming, hidden;

    @BeforeEach
    void fixtures() {
        family = categoryRepository.save(new Category("Gia đình"));
        adventure = categoryRepository.save(new Category("Phiêu lưu"));
        current = movies.create(form("Alpha hôm nay", 0, true, family.getId(), adventure.getId()));
        upcoming = movies.create(form("Beta ngày mai", 1, true, adventure.getId()));
        hidden = movies.create(form("Hidden bí mật", -1, false, family.getId()));
    }

    @Test
    void guestCanBrowseRealHomeListAndDetail() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Alpha hôm nay")))
                .andExpect(content().string(not(containsString("Hidden bí mật"))))
                .andExpect(content().string(not(containsString("Beta ngày mai"))));
        mvc.perform(get("/movies")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Beta ngày mai")))
                .andExpect(content().string(not(containsString("Hidden bí mật"))));
        mvc.perform(get("/movies/{id}", current.getId())).andExpect(status().isOk())
                .andExpect(content().string(containsString("Gia đình")))
                .andExpect(content().string(containsString("Phiêu lưu")))
                .andExpect(content().string(containsString("110 phút")));
    }

    @Test
    void hiddenAndMissingMoviesReturn404() throws Exception {
        mvc.perform(get("/movies/{id}", hidden.getId())).andExpect(status().isNotFound());
        mvc.perform(get("/movies/999999")).andExpect(status().isNotFound());
    }

    @Test
    void searchCombinesTitleGenreAndStatusWithoutDuplicateRows() throws Exception {
        assertThat(movies.findVisible("  ALPHA  ", family.getId(), ScreeningStatus.NOW_SHOWING, "title"))
                .extracting(Movie::getId).containsExactly(current.getId());
        assertThat(movies.findVisible("Beta", family.getId(), null, "title")).isEmpty();
        assertThat(movies.findVisible("", adventure.getId(), null, "title"))
                .extracting(Movie::getId).containsExactly(current.getId(), upcoming.getId());
        assertThat(movies.findVisible("", family.getId(), null, "title").getFirst().getCategories())
                .extracting(Category::getName).containsExactlyInAnyOrder("Gia đình", "Phiêu lưu");
        mvc.perform(get("/movies").param("q", "alpha").param("categoryId", family.getId().toString())
                        .param("status", "NOW_SHOWING").param("sort", "title"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("1 phim phù hợp")))
                .andExpect(content().string(not(containsString("Beta ngày mai"))));
    }

    @Test
    void statusBoundaryIncludesTodayAsNowShowing() {
        assertThat(current.getScreeningStatus()).isEqualTo(ScreeningStatus.NOW_SHOWING);
        assertThat(upcoming.getScreeningStatus()).isEqualTo(ScreeningStatus.COMING_SOON);
        assertThat(movies.findVisible(null, null, ScreeningStatus.NOW_SHOWING, "title"))
                .extracting(Movie::getId).containsExactly(current.getId());
        assertThat(movies.findVisible(null, null, ScreeningStatus.COMING_SOON, "title"))
                .extracting(Movie::getId).containsExactly(upcoming.getId());
    }

    @Test
    void supportedSortsAndUnknownSortAreSafe() {
        assertThat(movies.findVisible(null, null, null, "releaseDateAsc"))
                .extracting(Movie::getId).containsExactly(current.getId(), upcoming.getId());
        assertThat(movies.findVisible(null, null, null, "releaseDateDesc"))
                .extracting(Movie::getId).containsExactly(upcoming.getId(), current.getId());
        assertThat(movies.findVisible(null, null, null, "title"))
                .extracting(Movie::getId).containsExactly(current.getId(), upcoming.getId());
        assertThat(movies.findVisible(null, null, null, "invalid.column"))
                .extracting(Movie::getId).containsExactly(upcoming.getId(), current.getId());
    }

    @Test
    void sqlWildcardsAreTreatedAsLiteralTitleCharacters() {
        Movie literal = movies.create(form("100%_đặc biệt", -3, true, family.getId()));
        assertThat(movies.findVisible("%_", null, null, "title"))
                .extracting(Movie::getId).containsExactly(literal.getId());
    }

    @Test
    void emptyResultsRenderHelpfulMessage() throws Exception {
        mvc.perform(get("/movies").param("q", "không có phim này"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Chưa tìm thấy phim phù hợp")));
        mvc.perform(get("/movies").param("categoryId", "999999"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("0 phim phù hợp")));
    }

    @Test
    void invalidFilterTypesReturn400() throws Exception {
        mvc.perform(get("/movies").param("status", "INVALID")).andExpect(status().isBadRequest());
        mvc.perform(get("/movies").param("categoryId", "abc")).andExpect(status().isBadRequest());
    }

    @Test
    void anonymousAdminAccessRedirectsToLogin() throws Exception {
        mvc.perform(get("/admin/movies")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        mvc.perform(get("/admin/categories")).andExpect(status().is3xxRedirection());
        mvc.perform(validPost("/admin/movies")).andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerCannotReadOrMutateAdminResources() throws Exception {
        mvc.perform(get("/admin/movies")).andExpect(status().isForbidden());
        mvc.perform(get("/admin/movies/new")).andExpect(status().isForbidden());
        mvc.perform(get("/admin/movies/{id}/edit", current.getId())).andExpect(status().isForbidden());
        mvc.perform(validPost("/admin/movies")).andExpect(status().isForbidden());
        mvc.perform(validPost("/admin/movies/" + current.getId())).andExpect(status().isForbidden());
        mvc.perform(post("/admin/movies/{id}/visibility", current.getId()).with(csrf()).param("visible", "false"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/admin/categories")).andExpect(status().isForbidden());
        mvc.perform(post("/admin/categories").with(csrf()).param("name", "Hài"))
                .andExpect(status().isForbidden());
        assertThat(movieRepository.count()).isEqualTo(3);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanRenderAllFormsIncludingHiddenMovies() throws Exception {
        mvc.perform(get("/admin/movies")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Hidden bí mật")));
        mvc.perform(get("/admin/movies/new")).andExpect(status().isOk())
                .andExpect(content().string(containsString("_csrf")));
        mvc.perform(get("/admin/movies/{id}/edit", hidden.getId())).andExpect(status().isOk())
                .andExpect(model().attribute("movieForm", hasProperty("title", equalTo("Hidden bí mật"))))
                .andExpect(content().string(containsString("checked=\"checked\"")));
        mvc.perform(get("/admin/categories")).andExpect(status().isOk());
        mvc.perform(get("/admin/categories/{id}/edit", family.getId())).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void creationPersistsMultipleCategoriesAndTrimsFields() throws Exception {
        mvc.perform(validPost("/admin/movies", "title", "  Phim mới  ")
                        .param("categoryIds", adventure.getId().toString()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/movies"));
        Movie saved = movies.findForAdmin("Phim mới", null, null, "title").getFirst();
        assertThat(saved.getTitle()).isEqualTo("Phim mới");
        assertThat(saved.getCategories()).extracting(Category::getId)
                .containsExactlyInAnyOrder(family.getId(), adventure.getId());
        assertThat(saved.getPosterUrl()).isNull();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void invalidMovieFormDoesNotCreateMovie() throws Exception {
        mvc.perform(validPost("/admin/movies", "durationMinutes", "-5"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("movieForm", "durationMinutes"));
        mvc.perform(validPost("/admin/movies", "title", "   "))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("movieForm", "title"));
        mvc.perform(validPost("/admin/movies", "releaseDate", "2026-02-31"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("movieForm", "releaseDate"));
        assertThat(movieRepository.count()).isEqualTo(3);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rejectsMissingAndUnknownGenres() throws Exception {
        mvc.perform(validPost("/admin/movies").param("categoryIds", "999999"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("movieForm", "categoryIds"));
        // Một yêu cầu riêng không gửi categoryIds, tương ứng bỏ chọn mọi checkbox.
        mvc.perform(post("/admin/movies").with(csrf()).param("title", "Mới").param("director", "An")
                        .param("durationMinutes", "90").param("releaseDate", "2026-10-22")
                        .param("ageRating", "P").param("synopsis", "Nội dung"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("movieForm", "categoryIds"));
        assertThat(movieRepository.count()).isEqualTo(3);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rejectsScriptAndMalformedUrls() throws Exception {
        for (String url : new String[]{"javascript:alert(1)", "https://", "file:///etc/passwd", "https://user:pass@example.com/a"}) {
            mvc.perform(validPost("/admin/movies").param("posterUrl", url))
                    .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("movieForm", "posterUrl"));
        }
        mvc.perform(validPost("/admin/movies").param("trailerUrl", "https://example.com/trailer"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateReplacesGenresAndPreservesIdAndCreationTime() throws Exception {
        var createdAt = current.getCreatedAt();
        mvc.perform(validPost("/admin/movies/" + current.getId(), "title", "Đã sửa"))
                .andExpect(status().is3xxRedirection());
        Movie edited = movies.getById(current.getId());
        assertThat(edited.getTitle()).isEqualTo("Đã sửa");
        assertThat(edited.getCreatedAt()).isEqualTo(createdAt);
        assertThat(edited.getCategories()).extracting(Category::getId).containsExactly(family.getId());
        assertThat(categoryRepository.count()).isEqualTo(2);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void invalidEditLeavesExistingMovieUntouched() throws Exception {
        mvc.perform(validPost("/admin/movies/" + current.getId(), "durationMinutes", "0"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("movieForm", "durationMinutes"));
        assertThat(movies.getById(current.getId()).getTitle()).isEqualTo("Alpha hôm nay");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void hideAndRestoreKeepMovieAndSharedCategories() throws Exception {
        mvc.perform(post("/admin/movies/{id}/visibility", current.getId()).with(csrf()).param("visible", "false"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/movies/{id}", current.getId())).andExpect(status().isNotFound());
        assertThat(movieRepository.count()).isEqualTo(3);
        assertThat(movies.getById(current.getId()).getCategories()).hasSize(2);
        assertThat(categoryRepository.count()).isEqualTo(2);
        mvc.perform(post("/admin/movies/{id}/visibility", current.getId()).with(csrf()).param("visible", "true"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/movies/{id}", current.getId())).andExpect(status().isOk());
        mvc.perform(post("/admin/movies/{id}/delete", current.getId()).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void allMutationsRequireCsrfToken() throws Exception {
        mvc.perform(post("/admin/movies/{id}/visibility", current.getId()).param("visible", "false"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/admin/categories").param("name", "Hài")).andExpect(status().isForbidden());
        assertThat(movies.getById(current.getId()).isVisible()).isTrue();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void categoryNamesAreNormalizedAndUniqueIgnoringCase() throws Exception {
        mvc.perform(post("/admin/categories").with(csrf()).param("name", "  Hài   kịch  "))
                .andExpect(status().is3xxRedirection());
        assertThat(categoryRepository.findByNameIgnoreCase("Hài kịch")).isPresent();
        mvc.perform(post("/admin/categories").with(csrf()).param("name", "  HÀI KỊCH  "))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("categoryForm", "name"));
        mvc.perform(post("/admin/categories").with(csrf()).param("name", "  "))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("categoryForm", "name"));
        assertThat(categoryRepository.count()).isEqualTo(3);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void categoryRenameUpdatesSharedMoviesAndRejectsDuplicateName() throws Exception {
        mvc.perform(post("/admin/categories/{id}", family.getId()).with(csrf()).param("name", "Gia đình mới"))
                .andExpect(status().is3xxRedirection());
        assertThat(movies.getById(current.getId()).getCategories()).extracting(Category::getName)
                .contains("Gia đình mới");
        assertThat(movies.getById(hidden.getId()).getCategories()).extracting(Category::getName)
                .contains("Gia đình mới");
        mvc.perform(post("/admin/categories/{id}", family.getId()).with(csrf()).param("name", "PHIÊU LƯU"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("categoryForm", "name"));
    }

    @Test
    void serviceAlsoValidatesInputForOtherModules() {
        MovieForm bad = form("Sai", 0, true, family.getId());
        bad.setDurationMinutes(-1);
        assertThatThrownBy(() -> movies.create(bad)).isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> movies.getVisibleMovie(hidden.getId())).isInstanceOf(ResponseStatusException.class);
        assertThat(movies.getVisibleMovie(current.getId()).getDurationMinutes()).isEqualTo(110);
    }

    @Test
    void userContentIsEscapedInRenderedHtml() throws Exception {
        Movie escaped = movies.create(form("<script>alert(1)</script>", 0, true, family.getId()));
        mvc.perform(get("/movies/{id}", escaped.getId())).andExpect(status().isOk())
                .andExpect(content().string(not(containsString("<script>alert(1)</script>"))))
                .andExpect(content().string(containsString("&lt;script&gt;alert(1)&lt;/script&gt;")));
    }

    @Test
    void realAdminCanLogInAndReachCatalogManagement() throws Exception {
        User admin = new User();
        admin.setFullName("Admin Test");
        admin.setEmail("admin@test.local");
        admin.setPasswordHash(passwords.encode("Testing123!"));
        admin.setRole(Role.ADMIN);
        userRepository.saveAndFlush(admin);
        var result = mvc.perform(post("/login").with(csrf()).param("username", "admin@test.local")
                        .param("password", "Testing123!"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/"))
                .andReturn();
        var session = (MockHttpSession) result.getRequest().getSession(false);
        mvc.perform(get("/admin/movies").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Quản lý thể loại")));
    }

    private MovieForm form(String title, int days, boolean visible, Long... categoryIds) {
        MovieForm form = new MovieForm();
        form.setTitle(title);
        form.setDirector("Đạo diễn mẫu");
        form.setDurationMinutes(110);
        form.setReleaseDate(LocalDate.now(Movie.CINEMA_ZONE).plusDays(days));
        form.setAgeRating(AgeRating.P);
        form.setSynopsis("Nội dung phim dùng kiểm thử.");
        form.setVisible(visible);
        form.setCategoryIds(Set.of(categoryIds));
        return form;
    }

    private MockHttpServletRequestBuilder validPost(String url, String... overrides) {
        var parameters = new org.springframework.util.LinkedMultiValueMap<String, String>();
        parameters.set("title", "Phim mới");
        parameters.set("director", "Đạo diễn");
        parameters.set("durationMinutes", "100");
        parameters.set("releaseDate", LocalDate.now(Movie.CINEMA_ZONE).toString());
        parameters.set("ageRating", "P");
        parameters.set("synopsis", "Nội dung mới.");
        parameters.set("visible", "true");
        parameters.set("categoryIds", family.getId().toString());
        for (int i = 0; i < overrides.length; i += 2) parameters.set(overrides[i], overrides[i + 1]);
        return post(url).with(csrf()).params(parameters);
    }
}
