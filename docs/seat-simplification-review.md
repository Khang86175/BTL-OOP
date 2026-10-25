> Báo cáo lịch sử của phiên bản tạo sơ đồ một lần. Yêu cầu mới cho phép cập nhật trực tiếp và xóa an toàn: xem seat-direct-save-review.md.

# Tinh giản Seat — kiểm tra cuối ngày 25/10/2026

Nhánh: fix/seat-aisle-layout. Chưa stage/commit/push/tạo PR. Chưa truy cập MySQL QA hoặc database gốc.

## Kết quả

- Java: 129/129 PASS, 0 failure/error/skipped. JavaScript: 13/13 PASS. Edge headless: 19/19 PASS. Maven 3.9.9 clean package: BUILD SUCCESS. git diff --check sạch.
- Test được cập nhật theo phạm vi mới: bỏ test batch/lối đi đã ngừng hỗ trợ, thay bằng kiểm thử tạo một lần, từ chối tạo lại, vị trí cố định và sửa từng ghế. Không có test đang lỗi bị bỏ qua. Test Theater/Room/Showtime, lịch xung đột, quyền, CSRF, khách và Movie/Auth vẫn được chạy.
- Build trên bản sao trong target/seat-simple-verification; JAR: target/seat-simple-verification/target/movie-0.0.1-SNAPSHOT.jar. Source được đối chiếu hash với workspace, không dùng JAR cũ ở target gốc.
- Các thay đổi trước khi tinh giản được sao lưu tại target/seat-simplify-backup-20261025-183901, gồm file mới và working-tree.patch. Thư mục target được Git ignore. docs/final-review-person3.md giữ nguyên, chưa được theo dõi.

## Giữ và bỏ

Giữ Theater/Room/Showtime và các quy tắc hiện tại, lọc lịch khách, quyền Admin/CSRF. Seat tạo lần đầu theo số hàng/số ghế, tên A/B/C… và số từ 1, NORMAL/active, tối đa 1.000 ghế; sửa riêng NORMAL/VIP, active/inactive; đọc lại không ghi dữ liệu. Room lock + transaction ngăn tạo hai sơ đồ đồng thời và bảo đảm rollback khi lỗi.

Bỏ chọn COUPLE, cấu hình khoảng trống/lối đi, số bắt đầu, chọn/sửa nhiều ghế, thiết lập hàng, thêm/bớt hàng/ghế sau lưu, endpoint tạo lẻ/batch/designer và cơ chế lưu toàn sơ đồ. POST tạo lại bị từ chối ở Service kể cả mọi ghế đã inactive. Không còn chấp nhận đổi ID/phòng/hàng/số của ghế đã lưu.

## Database và dữ liệu cũ

- Không cần thay schema hiện tại. Không sửa database/theater-showtime.sql hoặc database/movie-booking.sql.
- Đã gỡ file mới chưa commit chỉ phục vụ chức năng bị hủy: SeatRowLayout.java, SeatRowLayoutRepository.java, SeatLayoutRowForm.java, database/seat-row-layout.sql và docs/seat-aisle-fix.md. Bản sao vẫn nằm trong thư mục sao lưu.
- Nếu seat_row_layouts từng được tạo, để nguyên bảng/dữ liệu/FK. Code mới không map, đọc, ghi hoặc tự drop bảng đó. Hình hiển thị chỉ dựa trên các Seat thực tế; không còn tái dựng khoảng trống từ metadata cũ. Ghế đã lưu inactive vẫn inactive, không biến mất hoặc tự bật.
- A11/A12, hàng/số không đều và mọi ID Seat cũ giữ nguyên. Không có DELETE Seat hoặc tạo lại phòng đã có Seat.
- COUPLE giữ @Deprecated để đọc tương thích. Không cho chọn/gán mới; khi chủ động sửa ghế cũ phải chọn NORMAL/VIP. Migration đề xuất COUPLE → NORMAL chỉ thay seat_type, giữ ID/vị trí/trạng thái và giá Booking lịch sử. Chưa chạy. Xem seat-types-migration.md.

## Booking — giới hạn và việc Người 4 cần làm

Giữ nguyên SeatSummary, SeatForm và các API findByRoom(roomId), findOpenShowtimes(...), getBookableShowtime(id), findActiveSeats(id). findByRoom trả toàn bộ ghế vật lý để hiển thị; findActiveSeats chỉ trả active NORMAL/VIP. Ghế active không đồng nghĩa còn vé. Trạng thái giữ/đặt và tính giá cuối thuộc Người 4; việc đổi loại không tính lại đơn cũ.

Nhánh hiện tại không có Booking để chạy kiểm thử end-to-end. Đã đọc bản tham chiếu Git có sẵn origin/feature/member-4-booking-clean (không khẳng định đây là bản remote mới nhất):

- service/BookingService.java: createBooking chỉ kiểm tra SeatStatus, chưa kiểm tra Seat.active/loại được hỗ trợ. getSeatsByShowtime trả danh sách chưa lọc điều kiện này. Đây là rủi ro quan trọng: gửi ID trực tiếp có thể vượt qua danh sách đã lọc của Người 3. Người 4 phải kiểm tra lại trong transaction, bổ sung test từ chối ghế inactive/loại cũ.
- service/pricing/SeatPriceCalculatorFactory.java và CoupleSeatCalculator.java vẫn hỗ trợ COUPLE; cần bỏ nhánh nghiệp vụ này khi tích hợp. Không tự thay NORMAL/VIP hoặc fallback sang giá thường cho loại không hỗ trợ.
- Cần thống nhất chính sách tắt/đổi loại ghế đã có vé/giữ chỗ và chốt giá tại thời điểm Booking. Không sửa code, dữ liệu hoặc giá của module Người 4 trong bản này.

## Checklist MySQL QA — người dùng tự thực hiện

1. Dừng riêng ứng dụng QA đang ghi dữ liệu; sao lưu cineticketp3qa ngoài repository bằng Workbench. Dùng tài khoản chỉ có quyền trên QA, không root. Không dùng profile demo/test.
2. Export kết quả trước nâng cấp bằng truy vấn chỉ đọc bên dưới; ghi nhận A11/A12, COUPLE, inactive và các FK Booking hiện có.
3. Dùng JAR mới ở target/seat-simple-verification/target, cấu hình JDBC tường minh tới cineticketp3qa, cổng 18084, seed catalog/showtime đều false. Vì không cần đổi schema, ưu tiên ddl-auto=validate; không import script lối đi hoặc DROP bảng metadata. Xem mysql8-isolated-check.md để biết cách truyền password QA không lưu vào Git.
4. Trước khi lưu gì, mở/tải lại phòng cũ: đối chiếu CSV, không đổi ID/vị trí/loại/trạng thái; không xuất hiện form tạo lại; A11/A12 không tự thay đổi. Kiểm tra cả phòng chỉ có ghế inactive và hàng đánh số khác mẫu mới.
5. Tạo phòng QA mới 5 × 8: đúng 40 ghế A1–E8, toàn NORMAL/active; tải lại không thêm ghế. Thử gửi lại POST tạo sơ đồ (hoặc hai tab): một sơ đồ duy nhất, không ghi đè.
6. Đổi A1 VIP rồi NORMAL; tắt A2, tải lại vẫn inactive, chủ động bật lại; so sánh ID/vị trí. Không có lối đi/chọn nhiều/sửa hàng/số bắt đầu. Kiểm tra ghế inactive/COUPLE không có trong API ghế active. Nếu có Booking tích hợp, gửi ID trực tiếp phải bị từ chối; nếu không, ghi nhận FAIL tích hợp và bàn giao Người 4, không coi lọc giao diện là đủ.
7. Kiểm tra Theater, Room và lịch cùng phòng trùng/nối tiếp, CLOSED/CANCELLED, khách chưa đăng nhập, Customer bị chặn Admin, POST thiếu CSRF.
8. Dừng/khởi động lại riêng QA với cùng cấu hình, đối chiếu ghế và các Booking cũ. seat_row_layouts nếu có vẫn còn nguyên. Không tự chạy migration COUPLE; chỉ thực hiện sau khi nhóm duyệt và kiểm tra giá/tham chiếu.

```sql
SELECT DATABASE(), CURRENT_USER(), VERSION();
SHOW GRANTS FOR CURRENT_USER;
SELECT id, room_id, row_label, seat_number, seat_type,
       CAST(active AS UNSIGNED) AS active
FROM cineticketp3qa.seats
ORDER BY room_id, row_label, seat_number, id;
SELECT room_id, COUNT(*) AS seats, SUM(CAST(active AS UNSIGNED)) AS active_seats
FROM cineticketp3qa.seats GROUP BY room_id;
SELECT TABLE_NAME, COLUMN_NAME, CONSTRAINT_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA='cineticketp3qa'
  AND REFERENCED_TABLE_SCHEMA='cineticketp3qa'
  AND REFERENCED_TABLE_NAME='seats';
```

## Sẵn sàng commit/PR?

Phần Người 3 đã sẵn sàng để review, nhưng nên hoàn tất checklist MySQL QA trước commit theo quy trình nhóm. Chưa thể xác nhận Booking end-to-end an toàn cho ghế inactive/COUPLE vì phụ thuộc Người 4 nêu trên. Không nên merge như một thay đổi đã kiểm chứng toàn bộ Booking cho đến khi giải quyết và kiểm thử tích hợp.

Commit/PR dự kiến: refactor(seats): simplify initial layout creation and individual editing.

## Danh sách file thay đổi so với HEAD

- M	docs/mysql8-isolated-check.md
- M	docs/seat-designer.md
- M	docs/showtime-booking-contract.md
- M	docs/showtime-ui-integration.md
- M	src/main/java/com/example/movie/controller/AdminSeatController.java
- M	src/main/java/com/example/movie/controller/ScheduleWebAdvice.java
- D	src/main/java/com/example/movie/dto/SeatBatchForm.java
- D	src/main/java/com/example/movie/dto/SeatBulkUpdateForm.java
- D	src/main/java/com/example/movie/dto/SeatLayoutForm.java
- D	src/main/java/com/example/movie/dto/SeatLayoutResult.java
- D	src/main/java/com/example/movie/dto/SeatLayoutSeatForm.java
- D	src/main/java/com/example/movie/dto/SeatLayoutState.java
- D	src/main/java/com/example/movie/dto/SeatRowForm.java
- M	src/main/java/com/example/movie/model/SeatType.java
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
- D	src/test/java/com/example/movie/SeatBatchIntegrationTest.java
- M	src/test/java/com/example/movie/SeatDesignerIntegrationTest.java
- M	src/test/java/com/example/movie/ShowtimeServiceIntegrationTest.java
- M	src/test/java/com/example/movie/ShowtimeWebIntegrationTest.java
- M	src/test/java/com/example/movie/TheaterRoomSeatIntegrationTest.java
- M	src/test/resources/seat-designer-model.test.js
- M	src/test/scripts/seat-designer-browser.ps1
- A (chưa theo dõi) docs/seat-simplification-review.md
- A (chưa theo dõi) docs/seat-types-migration.md
- A (chưa theo dõi) src/main/java/com/example/movie/dto/SeatLayoutCreateForm.java
- A (chưa theo dõi) src/test/java/com/example/movie/SeatLayoutCreationIntegrationTest.java
