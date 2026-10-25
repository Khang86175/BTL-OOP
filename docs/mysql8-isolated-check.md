> Bản hai giá Showtime cần kiểm tra/nâng cấp hai cột mới trước khi chạy. Làm theo showtime-prices-review.md và database/showtime-prices-migration.sql; dùng ddl-auto=validate sau khi schema đã đầy đủ. Không chạy lệnh update cũ để tự nâng cấp khi chưa duyệt.

# Kiểm thử MySQL 8 trên database riêng — CHƯA THỰC THI

Không dùng database movie_booking hoặc tài khoản root để chạy ứng dụng thử nghiệm.
Các lệnh dưới đây là hướng dẫn để người quản lý DB tự chạy sau khi kiểm tra.

## 1. Tạo database và tài khoản chỉ có quyền trên database QA

Dùng MySQL 8 của máy thử. Ví dụ tên database/tài khoản mới là cineticketp3qa.
Nếu tên này đã tồn tại, chọn tên QA mới và thay nhất quán trong mọi bước; không xóa database cũ.

```sql
CREATE DATABASE cineticketp3qa
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'cineticketp3qa'@'127.0.0.1'
  IDENTIFIED BY '<THAY_BANG_MAT_KHAU_QA_RIENG>';
GRANT ALL PRIVILEGES ON cineticketp3qa.*
  TO 'cineticketp3qa'@'127.0.0.1';
SHOW GRANTS FOR 'cineticketp3qa'@'127.0.0.1';
```

Tên database trên không có ký tự wildcard trong GRANT. Chỉ cấp quyền cho đúng database QA,
không cấp quyền movie_booking.* hoặc quyền toàn server. Dùng kết nối TCP 127.0.0.1 đúng với Host của tài khoản.
Mật khẩu trên là placeholder, không phải mật khẩu thật và không lưu mật khẩu đã chọn vào Git.

## 2. Chạy JAR trên cổng riêng, cấu hình tường minh

Mở terminal PowerShell mới. Dùng JAR đã build thành công trong báo cáo cuối; không cần dừng demo cổng 8080.
Profile mysqlqa không cần file cấu hình mới: kế thừa application.properties và nhận các giá trị CLI dưới đây.
KHÔNG bật profile demo/test vì các profile đó dùng H2 và create-drop.

```powershell
$qaCredential = Get-Credential -UserName 'cineticketp3qa' -Message 'Nhap mat khau MySQL QA'
$env:SPRING_DATASOURCE_PASSWORD = $qaCredential.GetNetworkCredential().Password
try {
  java -jar target/showtime-prices-verification/target/movie-0.0.1-SNAPSHOT.jar --spring.profiles.active=mysqlqa --server.port=18084 '--spring.datasource.url=jdbc:mysql://127.0.0.1:3306/cineticketp3qa?useSSL=false&serverTimezone=UTC' --spring.datasource.username=cineticketp3qa --spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver --spring.jpa.hibernate.ddl-auto=validate --catalog.seed.enabled=false --showtime.seed.enabled=false
} finally {
  Remove-Item Env:SPRING_DATASOURCE_PASSWORD -ErrorAction SilentlyContinue
}
```

Lệnh dùng JAR mới sau tinh giản Seat. Nếu build lại ở nơi khác, thay đúng đường dẫn JAR vừa kiểm chứng. Với QA đã đủ schema, ưu tiên ddl-auto=validate như checklist seat-simplification-review.md.
CLI ghi đè URL/username/profile cũ. Tài khoản QA không có quyền database gốc nên cấu hình nhầm
sẽ bị từ chối thay vì ghi vào movie_booking. Các dòng useSSL=false chỉ dành cho kiểm thử localhost.

Lệnh hiện tại dùng validate, chỉ áp dụng sau khi đã chuẩn bị đủ schema QA và hai cột giá mới. Nếu chọn Hibernate update trong quy trình khác, nó có thể thay đổi toàn bộ schema Entity, không chỉ giá. Lệnh trên tắt mọi seed;
không sao chép phim hoặc tài khoản từ database thật. Người 3 không seed sơ đồ trên profile này.

## 3. Xác nhận database đích và tạo Admin QA

Kết nối bằng chính tài khoản QA:

```sql
USE cineticketp3qa;
SELECT DATABASE(), CURRENT_USER(), VERSION(), @@transaction_isolation;
SHOW TABLES;
SHOW CREATE TABLE rooms;
SHOW CREATE TABLE seats;
SHOW CREATE TABLE showtimes;
```

Đăng ký một tài khoản mới tại http://localhost:18084/register, ví dụ admin-qa@cineticket.test.
Chỉ sau khi kiểm tra SELECT DATABASE() là cineticketp3qa, cấp role trên bảng QA đã chỉ rõ schema:

```sql
UPDATE cineticketp3qa.users SET role='ADMIN', enabled=b'1'
WHERE email='admin-qa@cineticket.test';
```

Đăng xuất/đăng nhập lại. Không có Admin mặc định được tạo ngoài profile demo.

## 4. Kiểm thử chức năng và dữ liệu thật trên QA

- Tạo rạp/phòng trống, kiểm tra tên phòng trùng trong cùng rạp và khác rạp.
- Phòng trống tạo 5 hàng × 8 ghế: đúng A1–E8, 40 ghế NORMAL/active, ID duy nhất.
- Tăng 6×8, 6×10 rồi giảm 5×8 sau xác nhận; giữ ID/loại/active vị trí còn lại. Có FK tham chiếu ghế cần bỏ thì cả thao tác bị từ chối. Thử xóa riêng/toàn bộ khi không có tham chiếu.
- Nhấn A1 đổi VIP rồi NORMAL; tắt A2, tải lại vẫn inactive, chủ động bật lại; kiểm tra ID/vị trí giữ nguyên.
- Không có bản nháp riêng, lối đi, số bắt đầu hoặc chọn nhiều ghế. Với phòng cũ có A11/A12, số không đều, ghế inactive: chỉ xem không đổi dữ liệu.
- Kiểm kê COUPLE và tham chiếu Booking theo seat-types-migration.md; không tự chạy migration. Ghế inactive/loại cũ không xuất hiện trong findActiveSeats.
- Code không còn sử dụng seat_row_layouts. Nếu bảng này từng được tạo, để nguyên; không DROP hoặc chuyển đổi dữ liệu.
- Lịch chồng lấn cùng phòng bị từ chối; nối tiếp đúng endTime được phép; khác phòng được cùng giờ.
- CLOSED vẫn chiếm phòng; CANCELLED không chiếm phòng. Lịch quá khứ, phim ẩn,
  phòng/rạp tắt, giá âm hoặc trước ngày phát hành bị từ chối.
- Gửi hai yêu cầu tạo lịch cùng phòng/giờ gần như đồng thời: chỉ một lịch được tạo.
  Thử đồng thời lưu sơ đồ, cập nhật một ghế và batch; không tạo vị trí trùng.
- Xác nhận DATETIME(6), giờ qua nửa đêm, BigDecimal/DECIMAL(12,2), Unicode và CHECK/FK/unique.
  Collation utf8mb4_0900_ai_ci có thể so sánh không phân biệt dấu, khác kiểm tra ignore-case ở Java/H2;
  cần ghi nhận kết quả với tên phòng/hàng có dấu.
- Khách xem GET /showtimes; Customer không vào Admin; POST không CSRF bị 403.
- Dừng/khởi động lại riêng ứng dụng QA: dữ liệu MySQL QA phải còn, ID không đổi.

Đây mới là kiểm thử thủ công MySQL. Các Java test hiện tại tự chọn H2;
đổi biến môi trường rồi chạy mvn test không biến chúng thành test MySQL.
Muốn tự động hóa các trường hợp MySQL cần test/profile riêng chỉ trỏ QA, không sửa profile H2 đang có.

## 5. Kiểm chứng script SQL riêng

KHÔNG import nguyên database/movie-booking.sql hoặc database/theater-showtime.sql vào phiên thử:
chúng có CREATE DATABASE/USE movie_booking; tham số mysql -D cineticketp3qa không vô hiệu hóa các lệnh USE trong file.

Lệnh validate ở bước 2 không tạo bảng. Với QA hiện có, chuẩn bị schema/hai cột giá theo showtime-prices-review.md trước. Để kiểm chứng schema trên QA mới,
dùng thêm một database QA rỗng và tài khoản giới hạn tương tự; tạo bản sao script chỉ trong thư mục thử,
thay/bỏ mọi CREATE DATABASE/USE trỏ movie_booking, kiểm tra bằng mắt rồi mới thực thi.
Tạo các bảng nền users/movies/categories/movie_category của QA trước bốn bảng Người 3.
Sau đó chạy ứng dụng QA với ddl-auto=validate để kiểm tra mapping tương ứng.
validate không thay thế việc kiểm tra FK/unique/CHECK/index bằng SHOW CREATE TABLE và ca thử dữ liệu.
Không chạy DROP/TRUNCATE/DELETE để chuẩn bị lại dữ liệu; chọn database QA mới.
