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

## Đợt B.2 — Tạo ghế hàng loạt và sơ đồ

Lưu ý B.3: trang /admin/seats đã thay bằng trình thiết kế trực tiếp trên sơ đồ.
Hướng dẫn hiện tại: seat-designer.md. Các endpoint nhập khoảng B.2 phía dưới vẫn giữ để tương thích,
nhưng không còn là trải nghiệm chính.

1. Chạy lại JAR mới bằng profile demo theo hướng dẫn trên. Vào Phòng và ghế, chọn phòng,
   rồi bấm Tạo ghế theo hàng.
2. Nhập hàng D, từ 1 đến 12, loại Thường, hoạt động. Bấm Thêm hàng / khoảng để nhập
   E1–12 VIP và F1–6 Ghế đôi (nếu chưa có các vị trí này).
3. Bấm Xem trước sơ đồ. Màn hình ở trên; ghế mới mang nhãn Mới, ghế cũ vẫn có trong sơ đồ.
   Xem trước không ghi dữ liệu. Sửa bất kỳ dòng nào sẽ ẩn bản xem trước cũ.
4. Bấm Tạo toàn bộ ghế. Toàn bộ khoảng được kiểm tra lại trong một transaction,
   khóa Room chung với thao tác tạo/sửa từng ghế. Một vị trí trùng làm cả lần tạo bị từ chối;
   không bỏ qua, không cập nhật hoặc tạo lại ghế cũ.
5. Muốn có lối đi ở vị trí 6 của hàng G, nhập G1–5 và G7–12. Số 6 để trống thực sự.
   Nếu G6 đã tồn tại thì thao tác này KHÔNG xóa G6.
6. Tại sơ đồ, tích các ghế, bấm tên hàng để chọn/bỏ chọn cả hàng, hoặc chọn tất cả.
   Chọn loại mới và/hoặc trạng thái mới rồi Áp dụng. Lựa chọn Giữ nguyên không đổi thuộc tính đó.
   Nhấn tên ghế vẫn mở trang sửa riêng.
7. ID/phòng/vị trí ghế không đổi khi cập nhật nhiều ghế; ghế đôi chỉ là SeatType.COUPLE,
   không tự nhân đôi số vé hoặc tính giá. Không có thao tác xóa.

Giới hạn mỗi lần tạo: tối đa 30 khoảng, số ghế 1–200, tổng tối đa 1.000 ghế;
mỗi lần cập nhật tối đa 1.000 ID duy nhất, cùng phòng. Không yêu cầu đổi schema hoặc API Booking.
Xem trước không giữ chỗ; nếu người khác tạo ghế sau đó, thao tác lưu sẽ kiểm tra lại và báo trùng.
JavaScript chỉ hỗ trợ thêm dòng, chọn ghế và đếm lựa chọn; server giữ toàn bộ validation/quyền/CSRF.

TC07 về trùng rạp được báo cáo riêng tại TC07-theater-duplicates.md; chưa áp đặt quy tắc mới.
