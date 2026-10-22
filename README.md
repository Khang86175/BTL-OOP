# CineTicket — Website đặt vé xem phim

Ứng dụng Java 21, Spring Boot 4.1.1, Spring Security, Thymeleaf, Spring Data JPA và MySQL.
Phần đã triển khai: xác thực cơ bản của Người 1 và **phim/thể loại của Người 2**.

## Chạy nhanh module phim, không cần MySQL

Cần JDK 21 và kết nối Internet cho lần tải dependency đầu tiên. Maven Wrapper đi kèm nên không bắt buộc cài Maven riêng.
Chạy lệnh tại thư mục có `pom.xml`:

```sh
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

Windows PowerShell dùng `./mvnw.cmd` thay cho `./mvnw`. Nếu đã có Maven, cũng có thể dùng `mvn`.
Mở <http://localhost:8080>.

- Admin demo: `admin@cineticket.test` / `DemoAdmin123!`.
- Có 6 phim hư cấu: 3 đang chiếu, 2 sắp chiếu, 1 đang ẩn; 5 thể loại.
- Có thể đăng ký một Customer để kiểm tra quyền.
- Profile `demo` dùng H2 **trong bộ nhớ**: mọi dữ liệu mất khi tắt ứng dụng. Không dùng profile này cho dữ liệu cần lưu lâu dài.
- Tài khoản Admin demo chỉ được tạo khi bật profile `demo`.

Nếu máy báo không tìm thấy Java, cài JDK 21 và kiểm tra `java -version` / `JAVA_HOME` trước khi chạy.

## Chạy với MySQL của nhóm

1. Cài JDK 21 và MySQL 8; tạo database hỗ trợ Unicode:

   ```sql
   CREATE DATABASE IF NOT EXISTS movie_booking
       CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
   ```

2. Dùng chung `src/main/resources/application.properties`. Có thể ghi đè thông tin kết nối bằng biến môi trường để không sửa mật khẩu trong Git:

   ```sh
   export SPRING_DATASOURCE_URL='jdbc:mysql://localhost:3306/movie_booking?useSSL=false&serverTimezone=UTC'
   export SPRING_DATASOURCE_USERNAME='root'
   export SPRING_DATASOURCE_PASSWORD='mat-khau-db-cua-ban'
   ./mvnw spring-boot:run
   ```

3. `ddl-auto=update` hiện có sẽ tạo thêm `movies`, `categories`, `movie_category`. Nếu nhóm muốn tạo schema thủ công, dùng `database/catalog-schema.sql` trong database trên; script không tự chạy và không xóa dữ liệu.
4. Muốn nạp phim mẫu vào MySQL lần đầu, đặt `CATALOG_SEED_ENABLED=true` trước khi chạy. Seeder chỉ chạy khi bảng `movies` rỗng; không ghi đè phim đã có. Sau khi nạp có thể tắt biến này.
5. Dùng tài khoản Admin của Người 1. Không có Admin mặc định được tự tạo ở chế độ MySQL. Có thể đăng ký một tài khoản rồi người quản lý DB cấp quyền cho đúng email:

   ```sql
   UPDATE users SET role = 'ADMIN', enabled = b'1'
   WHERE email = 'email-admin-cua-ban@example.com';
   ```

   Đăng xuất và đăng nhập lại sau khi đổi role.

File `da1.sql` cũ chỉ chứa users; không cần import để chạy module phim. Nếu đã import, chú ý nó có cả `enable`/`enabled` và `create_at`/`created_at`; Java dùng **enabled** và **created_at**. Tài khoản Admin cũ có `enabled = NULL`, cần đặt `enabled = b'1'` trước khi đăng nhập. Không chạy lại dump lên DB đang có dữ liệu cần giữ vì dump có `DROP TABLE users`.

## Chức năng và đường dẫn của Người 2

| Đường dẫn | Quyền | Nội dung |
|---|---|---|
| `/` | Mọi người | Trang chủ lấy tối đa 6 phim đang chiếu thật từ DB |
| `/movies` | Mọi người | Danh sách, tìm tên, lọc thể loại/trạng thái, sắp xếp |
| `/movies/{id}` | Mọi người | Chi tiết phim đang hiển thị, thể loại, poster, trailer |
| `/admin/movies` | ADMIN | Quản lý cả phim đang hiện và đang ẩn |
| `/admin/movies/new` | ADMIN | Form thêm phim |
| `/admin/movies/{id}/edit` | ADMIN | Form sửa phim |
| `POST /admin/movies` | ADMIN + CSRF | Tạo phim |
| `POST /admin/movies/{id}` | ADMIN + CSRF | Cập nhật phim |
| `POST /admin/movies/{id}/visibility` | ADMIN + CSRF | Đặt `visible=true/false` |
| `/admin/categories` | ADMIN | Danh sách và form thêm thể loại |
| `/admin/categories/{id}/edit` | ADMIN | Form đổi tên thể loại |
| `POST /admin/categories` | ADMIN + CSRF | Thêm thể loại |
| `POST /admin/categories/{id}` | ADMIN + CSRF | Đổi tên thể loại |

Ví dụ bộ lọc: `/movies?q=Hành&categoryId=1&status=NOW_SHOWING&sort=title`.
`status`: trống, `NOW_SHOWING`, `COMING_SOON`.
`sort`: `releaseDateDesc` (mặc định), `releaseDateAsc`, `title`; giá trị khác trở về mặc định.
Các bộ lọc kết hợp theo AND. Tìm theo tên không phân biệt hoa/thường; `%`/`_` được tìm như ký tự thường.

### Quy tắc nghiệp vụ

- Quan hệ phim–thể loại là nhiều–nhiều; mỗi phim cần ít nhất 1 thể loại hợp lệ.
- Bắt buộc tên phim, đạo diễn, ngày khởi chiếu, giới hạn tuổi, nội dung. Thời lượng từ 1 đến 600 phút.
- Các mức tuổi: `P`, `K`, `T13`, `T16`, `T18`.
- Ngày khởi chiếu **sau hôm nay** → sắp chiếu; **hôm nay trở về trước** → đang chiếu, theo múi giờ `Asia/Ho_Chi_Minh`. Chưa có ngày kết thúc chiếu; Admin ẩn phim khi cần ngừng giới thiệu.
- `visible` độc lập với trạng thái theo ngày. Phim ẩn không xuất hiện trong tìm kiếm/trang chủ và trả 404 khi khách nhập URL chi tiết trực tiếp.
- Chỉ ẩn/hiện, không cung cấp thao tác xóa cứng phim; giữ ID và dữ liệu để lịch chiếu/booking tham chiếu sau này.
- Tên thể loại được bỏ khoảng trắng đầu/cuối, gộp khoảng trắng thừa và kiểm tra trùng không phân biệt hoa/thường. Đổi tên áp dụng cho tất cả phim liên quan. Không cung cấp thao tác xóa thể loại đang dùng.
- Poster/trailer là URL `http`/`https` tùy chọn; không nhận `javascript:`/`file:`. Poster trống hoặc không tải được dùng SVG mặc định. Trailer mở tab mới.
- Dữ liệu được kiểm tra cả ở Controller và Service; nội dung người dùng được Thymeleaf escape khi hiển thị.

## Bàn giao cho Người 3 — lịch chiếu

Inject `MovieService` qua constructor và gọi:

```java
Movie movie = movieService.getVisibleMovie(movieId);
Long id = movie.getId();
Integer durationMinutes = movie.getDurationMinutes();
```

`getVisibleMovie` trả 404 nếu ID không tồn tại hoặc phim bị ẩn. Đối tượng trả về có các thể loại được tải sẵn.
Người 3 dùng `durationMinutes` để tính giờ kết thúc và **kiểm tra lại trạng thái phim ngay trong transaction tạo/cập nhật lịch**.
Để lấy danh sách cho form chọn phim:

```java
List<Movie> choices = movieService.findVisible(null, null, null, "title");
```

Các API khác:

- `findVisible(query, categoryId, status, sort)` cho trang khách hoặc module khác.
- `findForAdmin(...)` / `getById(id)` có thể trả phim ẩn, dành cho luồng quản trị.
- `create(MovieForm)`, `update(id, MovieForm)`, `setVisibility(id, visible)` dành cho quản trị; request phải đi qua `/admin/**` được Spring Security bảo vệ.

Cấu trúc mã: `model` → `repository` → `service` → `controller` → `templates`; DTO form không bind trực tiếp entity.

## Kiểm thử

```sh
./mvnw test
./mvnw package
```

26 bài kiểm thử tích hợp ở `CatalogIntegrationTest` và `CatalogDemoSmokeTest`:

- Trang chủ/danh sách/chi tiết lấy dữ liệu thật; phim ẩn và ID không tồn tại.
- Tìm tên kết hợp thể loại/trạng thái, không lặp phim hay mất thể loại khi join, các kiểu sắp xếp, ngày khởi chiếu đúng hôm nay, tìm `%`/`_`, không có kết quả.
- Validation trường bắt buộc/thời lượng/ngày/URL/thể loại, cập nhật và nhiều thể loại, ẩn rồi hiện lại vẫn giữ dữ liệu.
- Tên thể loại trùng, chuẩn hóa và đổi tên dùng chung.
- Khách chưa đăng nhập và Customer không có quyền Admin; kiểm tra CSRF và đăng nhập Admin thật bằng BCrypt.
- Escape nội dung HTML; các trang vẫn render khi tắt Open Session in View.
- Profile demo khởi động và nạp đúng dữ liệu mẫu.

Test tự động dùng H2 ở chế độ tương thích MySQL; vẫn cần chạy luồng demo trên MySQL của nhóm trước khi tích hợp cuối.

### Luồng kiểm tra thủ công

1. Mở `/movies` khi chưa đăng nhập: xem phim, tìm tên, lọc thể loại, chuyển đang/sắp chiếu, sắp xếp và thử từ khóa không có kết quả.
2. Mở chi tiết: kiểm tra ngày, thời lượng, thể loại, mô tả; thêm URL poster/trailer hợp lệ để kiểm tra liên kết.
3. Đăng nhập Admin: thêm thể loại, thêm phim với nhiều thể loại, sửa phim; thử dữ liệu sai và xác nhận thông báo lỗi.
4. Ẩn phim: kiểm tra Admin vẫn thấy, khách không thấy và URL chi tiết trả 404; hiện lại phim.
5. Đổi tên thể loại: kiểm tra các phim liên quan dùng tên mới.
6. Đăng ký/đăng nhập Customer rồi nhập trực tiếp `/admin/movies` và `/admin/categories`: phải bị từ chối.

Phòng chiếu, lịch chiếu, đặt vé, thanh toán và thống kê thuộc Người 3–5, chưa triển khai. Giao diện Bootstrap và font tải từ CDN nên cần Internet để hiển thị đầy đủ kiểu dáng.
