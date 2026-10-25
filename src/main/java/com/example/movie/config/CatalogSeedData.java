package com.example.movie.config;

import com.example.movie.model.*;
import com.example.movie.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;

@Component
@ConditionalOnProperty(name = "catalog.seed.enabled", havingValue = "true")
public class CatalogSeedData implements CommandLineRunner {
    private final MovieRepository movies;
    private final CategoryRepository categories;
    public CatalogSeedData(MovieRepository movies, CategoryRepository categories) {
        this.movies = movies;
        this.categories = categories;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (movies.count() > 0) return;
        Category adventure = category("Phiêu lưu");
        Category family = category("Gia đình");
        Category drama = category("Tâm lý");
        Category animation = category("Hoạt hình");
        Category scifi = category("Khoa học viễn tưởng");
        sample("Hành trình qua miền gió", "An Nguyễn", 112, -14, AgeRating.P,
                "Một nhóm bạn trở về miền biển để tìm lại chiếc thuyền của tuổi thơ. Hành trình đưa họ qua những làng chài, những câu chuyện cũ và lựa chọn cho tương lai.", true, adventure, family);
        sample("Thành phố sau cơn mưa", "Minh Trần", 105, -7, AgeRating.T13,
                "Một kiến trúc sư trẻ nhận nhiệm vụ phục hồi rạp chiếu cũ. Giữa thành phố đang đổi thay, cô tìm được những người cùng muốn giữ lại ký ức của khu phố.", true, drama);
        sample("Khu rừng đom đóm", "Hà Lê", 94, 0, AgeRating.P,
                "Chú cáo nhỏ và những người bạn lần theo ánh đom đóm để đưa mùa xuân trở lại khu rừng. Một chuyến phiêu lưu về tình bạn, lòng dũng cảm và sự sẻ chia.", true, animation, adventure, family);
        sample("Tín hiệu từ ngày mai", "Lâm Vũ", 128, 14, AgeRating.T13,
                "Một trạm quan sát nhận được tín hiệu đến từ chính mình trong tương lai. Nhóm nghiên cứu phải tìm lời giải trước khi cơn bão vũ trụ đi qua Trái Đất.", true, scifi, adventure);
        sample("Mùa hè bên hiên nhà", "Thảo Phạm", 101, 30, AgeRating.P,
                "Kỳ nghỉ ở nhà bà đưa hai chị em vào một cuộc thi làm phim của xóm nhỏ. Những điều bình dị trở thành chất liệu cho câu chuyện đầu tiên của họ.", true, family, drama);
        sample("Bản nháp mùa đông", "An Nguyễn", 110, -3, AgeRating.T16,
                "Phim mẫu đang ẩn, dùng để kiểm tra quyền hiển thị và thao tác quản trị.", false, drama);
    }

    private Category category(String name) {
        return categories.findByNameIgnoreCase(name).orElseGet(() -> categories.save(new Category(name)));
    }

    private void sample(String title, String director, int minutes, int days, AgeRating rating,
                        String synopsis, boolean visible, Category... genres) {
        Movie movie = new Movie();
        movie.setTitle(title);
        movie.setDirector(director);
        movie.setDurationMinutes(minutes);
        movie.setReleaseDate(LocalDate.now(Movie.CINEMA_ZONE).plusDays(days));
        movie.setAgeRating(rating);
        movie.setSynopsis(synopsis);
        movie.setVisible(visible);
        movie.setCategories(new LinkedHashSet<>(List.of(genres)));
        movies.save(movie);
    }
}
