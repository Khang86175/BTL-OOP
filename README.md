# CineTicket — Website đặt vé xem phim

Ứng dụng Java 21, Spring Boot 4.1.1, Spring Security, Thymeleaf, Spring Data JPA và MySQL.
Phần đã triển khai: xác thực cơ bản của Người 1 và **phim/thể loại của Người 2**.

## Chạy ứng dụng với MySQL

1. Cài JDK 21 và MySQL 8; tạo database hỗ trợ Unicode:

   ```sql
   CREATE DATABASE IF NOT EXISTS movie_booking
       CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
   ```

2. Có thể đặt biến môi trường để cấu hình hoặc ghi đè thông tin kết nối MySQL. Trên PowerShell, đặt các biến trong terminal trước khi chạy; không commit mật khẩu thật vào Git:

   ```powershell
   $env:SPRING_DATASOURCE_URL = 'jdbc:mysql://localhost:3306/movie_booking?useSSL=false&serverTimezone=UTC'
   $env:SPRING_DATASOURCE_USERNAME = 'root'
   $env:SPRING_DATASOURCE_PASSWORD = 'mat-khau-db-cua-ban'
   .\mvnw.cmd spring-boot:run
   ```

   Bash/macOS/Linux có thể dùng `export SPRING_DATASOURCE_PASSWORD='mat-khau-db-cua-ban'` và `./mvnw spring-boot:run`.

3. `ddl-auto=update` sẽ cập nhật schema khi ứng dụng chạy. Nếu muốn tạo bảng thủ công trước, chạy `database/movie-booking.sql`; file tạo bảng `users`, `movies`, `categories`, `movie_category`, không xóa bảng và không chứa tài khoản/dữ liệu cá nhân.
4. Muốn nạp phim mẫu vào MySQL lần đầu, đặt `CATALOG_SEED_ENABLED=true` trước khi chạy. Seeder chỉ chạy khi bảng `movies` rỗng; không ghi đè phim đã có. Sau khi nạp có thể tắt biến này.
5. Dùng tài khoản Admin của Người 1. Không có Admin mặc định được tự tạo ở chế độ MySQL. Có thể đăng ký một tài khoản rồi người quản lý DB cấp quyền cho đúng email:

   ```sql
   UPDATE users SET role = 'ADMIN', enabled = b'1'
   WHERE email = 'email-admin-cua-ban@example.com';
   ```

   Đăng xuất và đăng nhập lại sau khi đổi role.

Nếu database cũ được tạo từ dump trước đây, có thể còn các cột dư `enable`/`create_at`; Java sử dụng `enabled`/`created_at`. File schema hợp nhất không xóa hoặc tự di chuyển các cột cũ. Hãy sao lưu DB trước khi thay đổi schema hiện có.
