> Giá Showtime hiện dùng hai trường normalPrice/vipPrice, không dùng giá cơ bản + phụ phí. Xem showtime-prices-review.md để nâng cấp QA trước khi chạy JAR mới.

# Đợt B — Chạy và tích hợp giao diện Người 3

## Chạy demo an toàn

Không khởi động profile mặc định để thử UI: profile đó dùng MySQL.
Sau khi build thành công, cách ngắn nhất tại thư mục dự án:

```powershell
java -jar target/movie-0.0.1-SNAPSHOT.jar --spring.profiles.active=demo --showtime.seed.enabled=true
```

Mở http://localhost:8080. Không cần sửa cấu hình hoặc chạy SQL.

Chạy bằng Maven 3.9.9 với hai tham số:

`-Dspring-boot.run.profiles=demo`
`-Dspring-boot.run.arguments=--showtime.seed.enabled=true`

Ví dụ PowerShell (dùng đúng đường dẫn Maven trên máy):

```powershell
& 'C:/Users/Admin/.m2/wrapper/dists/apache-maven-3.9.9/8e74001100ff70d6af083c5511fcc5ec49282d7017cde82c3698eee8fdf86698/bin/mvn.cmd' '-Dmaven.repo.local=C:/Users/Admin/.m2/repository' spring-boot:run '-Dspring-boot.run.profiles=demo' '-Dspring-boot.run.arguments=--showtime.seed.enabled=true'
```

Profile demo mặc định dùng H2 trong bộ nhớ. Không đặt biến môi trường ghi đè datasource sang MySQL khi demo.
Seed mới yêu cầu cả profile demo và showtime.seed.enabled=true; mặc định tắt.
Seed chạy khi ApplicationReadyEvent, sau seed phim; chọn MovieService, không dùng ID phim cố định.
Seed không sửa phim/tài khoản. Nếu đã có rạp mẫu thì bỏ qua, không ghi đè chỉnh sửa Admin.
Không có phim hiển thị thì bỏ qua. Demo H2 mất dữ liệu khi dừng ứng dụng.

Đăng nhập admin@cineticket.test / DemoAdmin123! rồi truy cập:

- /admin/theaters: thêm/sửa/bật/tắt rạp, chuyển sang phòng.
- /admin/rooms: lọc rạp, thêm/sửa/bật/tắt phòng, mở sơ đồ ghế.
- /admin/seats?roomId=ID_THỰC: thêm ghế và bấm ghế để sửa, giữ ID.
- /admin/seats/batch?roomId=ID_THỰC: tạo nhiều hàng/khoảng ghế và xem trước.
- /admin/showtimes: thêm/sửa/đóng/hủy lịch.
- /showtimes: chọn ngày có suất từ danh sách Admin, lọc phim/rạp, mở chi tiết.

Đăng ký/đăng nhập Customer để kiểm tra trang lịch và thử URL Admin (403).
Khách chưa đăng nhập xem được GET /showtimes và /showtimes/{id} khi suất hợp lệ.
Các trang Admin vẫn yêu cầu đăng nhập ADMIN; Customer bị từ chối (403).
Các form dùng POST + token CSRF. Không có Booking/Payment hoặc thao tác xóa cứng.
Giờ kết thúc không có trong form. Đóng/hủy dùng thao tác riêng, kể cả lịch đã qua hoặc phim đã bị ẩn.
Lịch đã hủy không được chuyển sang CLOSED bằng thao tác nhanh; phải sửa lịch để kiểm tra xung đột.

## Tích hợp dùng chung đã áp dụng trong Đợt B.1

1. SecurityConfig.java: sau matcher /admin/** và trước anyRequest().authenticated(), đã thêm:

```java
.requestMatchers(org.springframework.http.HttpMethod.GET, "/showtimes", "/showtimes/**").permitAll()
```

Chỉ mở GET trang lịch; giữ nguyên /admin/** và CSRF. POST/PUT/DELETE không được mở công khai.
Test dùng SecurityConfig thật, kiểm tra anonymous xem lịch và vẫn không quản trị được.

2. movies/detail.html: đã thay dòng chờ cập nhật bằng nút:

```html
<a th:href="@{/showtimes(movieId=${movie.id})}">Xem lịch chiếu</a>
```

3. fragments/navbar.html: đã thêm liên kết /showtimes cho khách; /admin/theaters,
/admin/rooms, /admin/showtimes chỉ hiển thị khi isAdmin, giữ menu cũ.

Không cần sửa application.properties, pom.xml hoặc seed catalog. Navbar riêng của Người 3
nằm trong fragments/schedule.html, xuất hiện trên các trang mới.

## Quản lý ghế hiện tại

Xem seat-designer.md. Đổi số hàng/số ghế để xem sơ đồ ngay và nhấn Tạo/Cập nhật để lưu trực tiếp. Cho phép tăng/giảm kích thước, xóa riêng/toàn bộ nếu không có tham chiếu, giữ NORMAL/VIP và active/inactive của vị trí còn lại. Không còn endpoint B.2/batch, lối đi, số bắt đầu, chọn nhiều ghế hoặc di chuyển ID sang vị trí khác. Không có chức năng bản nháp riêng.
Các API Booking giữ nguyên. TC07 về trùng rạp vẫn được báo cáo riêng tại TC07-theater-duplicates.md; chưa áp đặt quy tắc mới.
