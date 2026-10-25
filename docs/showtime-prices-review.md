# Showtime: hai mức giá NORMAL/VIP — 25/10/2026

Nhánh fix/seat-aisle-layout. Thay đổi trước đợt này được sao lưu tại target/showtime-prices-backup-20261025-202714. Không stage/commit/push; không truy cập hoặc thay đổi MySQL QA/movie_booking.

## Hành vi hiện tại

- Tạo/sửa Showtime bắt buộc normalPrice và vipPrice riêng, BigDecimal, lớn hơn 0, tối đa 10 chữ số nguyên/2 chữ số thập phân. Không làm tròn dữ liệu không hợp lệ; không áp đặt VIP >= NORMAL.
- HTML có hai input required, min=0.01, step=0.01, max=9999999999.99. Backend NotNull/DecimalMin/Digits kiểm tra độc lập, kể cả khi bỏ qua frontend hoặc gửi basePrice cũ.
- Danh sách Admin/khách và chi tiết khách hiển thị hai giá, giữ đủ 2 chữ số thập phân. Mỗi suất có giá riêng; không có phụ phí cố định toàn hệ thống.
- Showtime.priceFor(SeatType.NORMAL/VIP) trả đúng giá đã cấu hình; từ chối COUPLE/loại không hỗ trợ hoặc suất thiếu giá. Không tự cộng 20.000.
- Không đổi kiểm tra lịch, trạng thái, thời lượng phim, Room lock hoặc quyền/CSRF. Theater/Room/Seat và các API đọc Booking vẫn được kiểm thử.

## Schema và dữ liệu cũ

- Thêm normal_price và vip_price kiểu DECIMAL(12,2), tạm nullable để giữ mọi dòng cũ trong giai đoạn chuyển tiếp. CHECK chỉ chấp nhận NULL hoặc giá > 0. Service/form mới luôn yêu cầu đủ hai giá.
- Giữ base_price, getters/setters tương thích và thành phần basePrice của ShowtimeSummary. Đây là dữ liệu legacy, KHÔNG phải nguồn tính giá mới.
- Lúc tạo mới, base_price được điền bằng normalPrice để đáp ứng cột NOT NULL cũ. Lúc sửa, không ghi đè base_price đã có. Không đổi ID Movie/Room/Seat/Showtime/Booking.
- Không tự suy ra VIP từ giá cũ, không tự backfill hoặc thay đổi trạng thái lịch. Lịch cũ thiếu giá vẫn hiện trong Admin; khách/Booking tạm không chọn được cho đến khi có đủ hai giá dương. Ngay cả base_price cũ bằng 0 cũng không được coi là giá mới hợp lệ.
- Admin chọn rõ hai giá. Quy tắc không sửa lịch quá khứ vẫn giữ: lịch lịch sử cần kế hoạch SQL backfill được nhóm duyệt, không đổi giờ cũ chỉ để sửa giá.
- Phương án được đề xuất: NORMAL dùng base_price dương sau khi nhóm duyệt; VIP chọn theo từng suất. Nếu nhóm muốn cả hai bằng giá cũ hoặc áp dụng một công thức chuyển tiếp, phải thống nhất riêng; code không tự áp dụng.
- Chỉ sau khi tất cả môi trường/row đã được chuyển đổi mới đưa hai cột sang NOT NULL và đổi JPA nullable=false trong đợt sau. Chưa xóa base_price trước khi mọi Booking caller chuyển xong.

Schema tạo mới: database/theater-showtime.sql. Migration QA riêng: database/showtime-prices-migration.sql. Không chạy schema tổng vào QA vì file tổng còn USE movie_booking; script migration riêng chỉ định rõ cineticketp3qa.
Script Phase A có ALTER và không tự kiểm soát chạy lại: kiểm tra hai cột/constraint trước, chỉ chạy khi đều chưa có. Nếu đã có một phần hoặc đã để Hibernate tạo, dừng và đối chiếu cấu trúc trước.

MySQL 8 hỗ trợ CHECK thực thi từ 8.0.16: [tài liệu chính thức](https://dev.mysql.com/blog-archive/mysql-8-0-16-introducing-check-constraint/). ALTER TABLE có implicit commit; không coi transaction/ROLLBACK là cơ chế hoàn tác DDL: [tài liệu MySQL](https://dev.mysql.com/doc/refman/8.0/en/implicit-commit.html). Vì vậy phải sao lưu và duyệt trước khi thực hiện trên QA. Chưa chạy DDL/DML nào trên MySQL trong đợt này.

## Bàn giao Người 4 — chưa sửa Booking

Đã đọc bản tham chiếu Git có sẵn origin/feature/member-4-booking-clean, không khẳng định đó là bản remote mới nhất. Nhánh hiện tại chưa có Booking để chạy end-to-end.

| File/thành phần Người 4 | Việc cần cập nhật |
|---|---|
| service/BookingService.java, createBooking | Đổi calculator.calculate(showtime.getBasePrice()) sang giá theo NORMAL/VIP của Showtime trong transaction. Không lấy giá client hoặc trường basePrice legacy. |
| service/pricing/SeatPriceCalculator.java | Nếu giữ calculator, đổi hợp đồng calculate(BigDecimal basePrice) để nhận đủ Showtime/hai giá; truyền một basePrice không thể xác định VIP độc lập. |
| service/pricing/RegularSeatCalculator.java | Trả normalPrice, không lấy legacy basePrice. |
| service/pricing/VipSeatCalculator.java | Trả vipPrice, bỏ cộng cố định 20.000. |
| service/pricing/SeatPriceCalculatorFactory.java | Chỉ NORMAL/VIP; loại không hỗ trợ phải từ chối, không fallback âm thầm sang giá thường. |
| service/pricing/CoupleSeatCalculator.java | Không còn dùng cho nghiệp vụ mới; bỏ đăng ký/phụ thuộc trong đợt Người 4. Enum lịch sử vẫn tạm giữ để đọc dữ liệu. |
| model/ShowtimeSeat.java, trường price | Kiểm tra nơi khởi tạo/hiển thị/cache giá nếu đang dùng; không dùng giá cache cũ làm giá xác nhận một cách vô điều kiện. Chốt chính sách giữ giá lúc giữ ghế với nhóm. |
| model/BookingSeat.java, priceAtBooking | Đã có DECIMAL(12,2); tiếp tục lưu giá thực tế đã chọn tại thời điểm xác nhận. Không tính lại hoặc UPDATE hóa đơn lịch sử khi Admin sửa Showtime. |
| Booking.totalAmount, trang hóa đơn và test giá | Cộng các priceAtBooking đã chốt; hiển thị giá lịch sử, không join giá hiện tại rồi tính lại. Cập nhật expected giá trong test, gồm VIP thấp hơn NORMAL. |
| ShowtimeSummary/API consumer | Đọc normalPrice/vipPrice mới. Ba chữ ký findOpenShowtimes/getBookableShowtime/findActiveSeats không đổi; basePrice chỉ giữ tương thích để biên dịch/chuyển đổi, không bảo đảm tính đúng giá mới. |

Thay đổi tối thiểu gợi ý trong vòng lặp xác nhận, KHÔNG áp dụng vào nhánh Người 4:

```java
BigDecimal seatPrice = showtime.priceFor(showtimeSeat.getSeat().getSeatType());
totalAmount = totalAmount.add(seatPrice);
BookingSeat bookingSeat = new BookingSeat(booking, showtimeSeat.getSeat(), seatPrice);
```

Phải tiếp tục kiểm tra lịch còn bookable, ghế đúng phòng/active/NORMAL hoặc VIP, trạng thái giữ/đặt và khóa đồng thời. Việc kiểm tra active/loại vẫn thiếu trong bản tham chiếu Booking đã đọc, như các báo cáo Seat trước đó.
**Chưa thể coi Booking đã dùng đúng hai mức giá cho đến khi Người 4 chuyển đổi và kiểm thử.** Getter basePrice còn tồn tại không có nghĩa công thức Booking cũ còn đúng.

## Kết quả tự động

| Bộ kiểm tra | Kết quả |
|---|---|
| Java | 173/173 PASS, 0 failure/error/skipped |
| JavaScript Seat hiện tại | 19/19 PASS |
| Edge headless Seat + Showtime | 34/34 PASS |
| Maven 3.9.9 clean package | BUILD SUCCESS |
| Git whitespace | Sạch |

Test mới gồm hai giá, sửa từng giá, reload chính xác BigDecimal, giá khác nhau cho cùng phim, thiếu/0/âm/ký tự/precision, VIP rẻ hơn NORMAL, lịch cũ chưa cấu hình và overlap, lịch cũ thiếu giá vẫn chiếm phòng, base_price=0 cũ vẫn giữ được và hủy lịch được. Test snapshot lịch sử dùng bảng fixture H2 có FK, chứng minh Service Admin không ghi vào bản ghi giá đã chốt; không thay thế test Booking thực tế của Người 4.

Build/test trên H2 riêng tại target/showtime-prices-verification; đối chiếu source với workspace. Không khởi động ứng dụng trên MySQL. JAR mới:

`target/showtime-prices-verification/target/movie-0.0.1-SNAPSHOT.jar`

JAR các thư mục seat-... trước đó vẫn là phiên bản cũ, không tự ghi đè.

## MySQL QA: các bước tự kiểm thử trước commit/merge

1. Dừng ứng dụng QA đang ghi, sao lưu riêng cineticketp3qa ngoài repository; dùng tài khoản QA giới hạn, không root. Chưa chạy ứng dụng mới khi thiếu cột.
2. Trong Workbench kiểm tra DATABASE()/CURRENT_USER()/VERSION(), SHOW CREATE TABLE cineticketp3qa.showtimes và preflight ở script migration. Export toàn bộ ID, base_price, thời gian, trạng thái trước nâng cấp; ghi nhận các Booking và price_at_booking thực tế theo schema của Người 4.
3. Chỉ sau khi duyệt, tự chạy Phase A của database/showtime-prices-migration.sql. Giữ các row/base_price/ID như trước; các giá mới NULL. Không chạy lại ALTER nếu Hibernate/các lần thử trước đã tạo cột.
4. Khởi động JAR mới bằng cấu hình QA tường minh cổng 18084, profile mysqlqa, cả hai seed=false và ddl-auto=validate. Hướng dẫn truyền password bằng Get-Credential có trong mysql8-isolated-check.md. Không dùng demo/test hoặc URL database gốc. validate không xác nhận đầy đủ CHECK/FK, vẫn phải kiểm tra SHOW CREATE TABLE.
5. Trước khi sửa: lịch cũ vẫn trong Admin, giá Chưa cập nhật, ID/base_price/trạng thái giữ nguyên; không được đặt khi thiếu giá. Chọn một lịch tương lai, nhập rõ hai giá được nhóm duyệt và lưu.
6. Tạo lịch với NORMAL=80000, VIP=100000. Đổi NORMAL=85000, giữ VIP; sau đó VIP=70000, giữ NORMAL. Reload và khởi động lại QA vẫn đúng. Lịch khác cùng phim có thể có giá khác; giữ quy tắc không trùng phòng/giờ.
7. Thử thiếu một giá, 0, âm, chữ, quá 2 chữ số thập phân hoặc vượt DECIMAL: frontend/backend phải từ chối, DB không đổi. Gửi basePrice cũ không thay thế được hai trường bắt buộc.
8. Sau khi Người 4 cập nhật, đặt NORMAL/VIP và kiểm tra tiền bằng giá tương ứng. Đổi hai giá Admin rồi mở hóa đơn cũ: price_at_booking/totalAmount và ID Booking/Seat/Showtime phải giữ nguyên. Không tự sửa dữ liệu Booking cũ để làm test xanh.
9. Kiểm tra lại Seat 40→48→60→40, xóa có tham chiếu bị chặn, Theater/Room, lịch xung đột và CSRF/Admin/khách. Chưa nên merge như thay đổi Booking hoàn chỉnh trước khi thực hiện các bước này.

SQL chỉ đọc đối chiếu sau Phase A:

```sql
SELECT id,movie_id,room_id,start_time,end_time,status,base_price,normal_price,vip_price
FROM cineticketp3qa.showtimes ORDER BY id;
SELECT id FROM cineticketp3qa.showtimes
WHERE normal_price IS NULL OR vip_price IS NULL OR normal_price<=0 OR vip_price<=0;
SHOW CREATE TABLE cineticketp3qa.showtimes;
```

## File thuộc riêng đợt hai giá

Java: model/Showtime.java; dto/ShowtimeForm.java, ShowtimeSummary.java; service/ShowtimeService.java; controller/AdminShowtimeController.java; config/ShowtimeSeedData.java.
HTML: admin/showtimes/form.html, list.html; showtimes/list.html, detail.html.
SQL: database/theater-showtime.sql và database/showtime-prices-migration.sql (mới).
Test: ScheduleDomainTest.java, ShowtimeServiceIntegrationTest.java, ShowtimeWebIntegrationTest.java, ShowtimePriceIntegrationTest.java (mới), scripts/seat-designer-browser.ps1.
Tài liệu: showtime-booking-contract.md, mysql8-isolated-check.md, showtime-ui-integration.md và báo cáo này (mới).

Không sửa pom.xml/application.properties/SecurityConfig hoặc nghiệp vụ Người 1/2. Các thay đổi Seat chưa commit từ trước được giữ nguyên.

## Danh sách thay đổi tích lũy so với HEAD

- M	database/theater-showtime.sql
- M	docs/mysql8-isolated-check.md
- M	docs/seat-designer.md
- M	docs/showtime-booking-contract.md
- M	docs/showtime-ui-integration.md
- M	src/main/java/com/example/movie/config/ShowtimeSeedData.java
- M	src/main/java/com/example/movie/controller/AdminSeatController.java
- M	src/main/java/com/example/movie/controller/AdminShowtimeController.java
- M	src/main/java/com/example/movie/controller/ScheduleWebAdvice.java
- D	src/main/java/com/example/movie/dto/SeatBatchForm.java
- D	src/main/java/com/example/movie/dto/SeatBulkUpdateForm.java
- D	src/main/java/com/example/movie/dto/SeatLayoutForm.java
- D	src/main/java/com/example/movie/dto/SeatLayoutResult.java
- D	src/main/java/com/example/movie/dto/SeatLayoutSeatForm.java
- D	src/main/java/com/example/movie/dto/SeatLayoutState.java
- D	src/main/java/com/example/movie/dto/SeatRowForm.java
- M	src/main/java/com/example/movie/dto/ShowtimeForm.java
- M	src/main/java/com/example/movie/dto/ShowtimeSummary.java
- M	src/main/java/com/example/movie/model/SeatType.java
- M	src/main/java/com/example/movie/model/Showtime.java
- M	src/main/java/com/example/movie/repository/SeatRepository.java
- M	src/main/java/com/example/movie/service/SeatService.java
- M	src/main/java/com/example/movie/service/ShowtimeService.java
- M	src/main/resources/static/css/schedule.css
- M	src/main/resources/static/js/seat-designer-model.js
- M	src/main/resources/static/js/seat-designer.js
- D	src/main/resources/static/js/seat-layout.js
- D	src/main/resources/templates/admin/seats/batch.html
- M	src/main/resources/templates/admin/seats/form.html
- M	src/main/resources/templates/admin/seats/list.html
- M	src/main/resources/templates/admin/showtimes/form.html
- M	src/main/resources/templates/admin/showtimes/list.html
- M	src/main/resources/templates/showtimes/detail.html
- M	src/main/resources/templates/showtimes/list.html
- M	src/test/java/com/example/movie/ScheduleDomainTest.java
- D	src/test/java/com/example/movie/SeatBatchIntegrationTest.java
- M	src/test/java/com/example/movie/SeatDesignerIntegrationTest.java
- M	src/test/java/com/example/movie/ShowtimeServiceIntegrationTest.java
- M	src/test/java/com/example/movie/ShowtimeWebIntegrationTest.java
- M	src/test/java/com/example/movie/TheaterRoomSeatIntegrationTest.java
- M	src/test/resources/seat-designer-model.test.js
- M	src/test/scripts/seat-designer-browser.ps1
- A (chưa theo dõi) database/showtime-prices-migration.sql
- A (chưa theo dõi) docs/seat-direct-save-review.md
- A (chưa theo dõi) docs/seat-simplification-review.md
- A (chưa theo dõi) docs/seat-types-migration.md
- A (chưa theo dõi) docs/showtime-prices-review.md
- A (chưa theo dõi) src/main/java/com/example/movie/dto/SeatLayoutCreateForm.java
- A (chưa theo dõi) src/main/java/com/example/movie/service/SeatReferenceGuard.java
- A (chưa theo dõi) src/test/java/com/example/movie/SeatDeletionSchemaSafetyTest.java
- A (chưa theo dõi) src/test/java/com/example/movie/SeatLayoutCreationIntegrationTest.java
- A (chưa theo dõi) src/test/java/com/example/movie/SeatLayoutResizeIntegrationTest.java
- A (chưa theo dõi) src/test/java/com/example/movie/ShowtimePriceIntegrationTest.java
