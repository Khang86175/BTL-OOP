# Seat Designer cập nhật trực tiếp — kết quả 25/10/2026

Nhánh fix/seat-aisle-layout. Thay đổi trước đợt này được sao lưu trong target/seat-direct-save-backup-20261025-193643 (file mới + working-tree.patch). Không stage, commit, push; không sửa main hoặc truy cập MySQL thật.

## Đã hoàn thành

- Input số hàng/số ghế cập nhật sơ đồ ngay; một nút tạo/cập nhật ghi thẳng DB, không có cơ chế bản nháp riêng hoặc xác nhận lưu lần hai.
- Tạo 5×8=40, tăng 6×8=48, tăng 6×10=60, giảm 5×8=40 khi đủ điều kiện.
- Giữ ID, loại và trạng thái mọi ghế cùng vị trí; ghế mới NORMAL/active, không sinh ngoài phạm vi hoặc tự bật inactive.
- Xóa riêng/xóa toàn bộ, xác nhận một lần; giảm kích thước xác nhận số ghế cũ cần bỏ. Backend vẫn đòi flag xác nhận và kiểm tra tham chiếu, không tin frontend.
- Room/Seat locks, transaction và FK metadata guard; có tham chiếu thì từ chối cả thao tác. Giữ nguyên Booking/ShowtimeSeat và giá lịch sử.
- Không có COUPLE trong thao tác tạo/chỉnh mới, không lối đi, số bắt đầu tùy chỉnh hoặc chọn/sửa nhiều ghế.

## Kiểm thử

| Bộ kiểm tra | Kết quả |
|---|---|
| Java, gồm các test cũ còn áp dụng và test mới | 150/150 PASS; 0 failure/error/skipped |
| JavaScript trong runtime Edge | 19/19 PASS |
| Edge headless | 25/25 PASS |
| Maven 3.9.9 clean package | BUILD SUCCESS |
| git diff --check | Sạch |

Java mới kiểm tra FK thực tế với các bảng H2 fixture booking_seats/showtime_seats/seat_notes, cả ON DELETE CASCADE; chỉ tham chiếu ghế giữ lại không chặn thu nhỏ an toàn. Kiểm tra schema thiếu FK, rollback sau xóa + insert một phần, thao tác đồng thời, quyền/CSRF, phòng khác và xóa không xác nhận.
Edge kiểm tra cập nhật tức thì chưa ghi DB, toàn chuỗi 40→48→60→40, hủy xác nhận không ghi, xác nhận một lần, giữ ID/loại/inactive, xóa riêng, tải lại và xóa toàn bộ. Theater/Room/Showtime, Movie/Auth, lịch xung đột và API đọc Booking vẫn nằm trong bộ Java test.

Build/test chạy bản sao source tại target/seat-direct-verification; đối chiếu hash src/pom với workspace. JAR mới:

`target/seat-direct-verification/target/movie-0.0.1-SNAPSHOT.jar`

JAR seat-simplified-qa trước đó thuộc phiên bản cũ, chưa được tự ghi đè. Không dùng nhầm để thử thay đổi mới.

## Giới hạn cần chú ý

- Chưa kiểm thử thay đổi mới trên MySQL 8.0.46. JDBC metadata/FK/locking/rollback đã thử H2, chưa phải bằng chứng end-to-end với Booking thật.
- Guard chặn mọi tham chiếu, kể cả ShowtimeSeat AVAILABLE. Không tự xóa hoặc di chuyển bản ghi Người 4 để cho phép thu nhỏ. Schema thiếu FK hoặc không đủ quyền đọc metadata sẽ chặn thao tác phá hủy.
- FK tới Seat.id và quyền xem đầy đủ metadata là hợp đồng tích hợp. Một hệ thống khác giữ ID không có FK không thể được tự phát hiện tổng quát; hai bảng Booking đã biết thiếu FK bị chặn an toàn. Không xóa dữ liệu trong schema khác.
- COUPLE còn @Deprecated để đọc dữ liệu lịch sử. Migration đề xuất vẫn chưa chạy; xem seat-types-migration.md.
- Không cần schema mới; bảng seat_row_layouts nếu có không được đọc/ghi/xóa.
- Bản tham chiếu Git có sẵn origin/feature/member-4-booking-clean chưa kiểm tra Seat.active/loại trong createBooking/getSeatsByShowtime và vẫn có CoupleSeatCalculator. Không sửa/merge nhánh đó; Người 4 phải chặn yêu cầu đặt trực tiếp ghế inactive/loại cũ và giữ giá chốt. API Người 3 không thay thế trạng thái ghế đã giữ/đặt.

## MySQL QA cần kiểm thử thủ công

1. Sao lưu riêng cineticketp3qa; dùng tài khoản QA giới hạn và JAR mới ở trên, cổng riêng, profile mysqlqa, seed false. Không dùng profile demo/test hoặc database gốc.
2. Export ID/phòng/hàng/số/loại/active và các FK trước thao tác. Mở trang cũ không đổi A11/A12, không bật inactive, không tạo ghế khi tải lại.
3. Trên phòng QA mới thử 40→48→60→40, đối chiếu ID và VIP/inactive của A1/A2 sau mỗi lần và sau khởi động lại.
4. Gắn fixture Booking bằng chức năng Người 4/QA đã được duyệt, thử giảm/xóa: có liên kết phải bị chặn, mọi ghế và Booking/giá giữ nguyên. Kiểm tra cả ShowtimeSeat chưa bán và cấu trúc FK thực tế.
5. Với phòng không có tham chiếu, thử hủy/xác nhận xóa riêng, xóa toàn bộ; tải lại không tái tạo ghế. Gửi request thiếu xác nhận, thiếu CSRF, Customer hoặc ID phòng khác phải không ghi dữ liệu.
6. Chạy lại Theater/Room/Showtime, xung đột lịch, CLOSED/CANCELLED và kiểm tra Người 4 nhận danh sách ghế mới đúng ID. Giá NORMAL/VIP không đổi.

Tài liệu thao tác: seat-designer.md; hợp đồng: showtime-booking-contract.md. Bản sửa sẵn sàng review, chưa nên coi MySQL/Booking đã được kiểm chứng đầy đủ trước các bước QA này.

## File mới/sửa riêng trong đợt cập nhật trực tiếp

Mới: SeatReferenceGuard.java, SeatLayoutResizeIntegrationTest.java, SeatDeletionSchemaSafetyTest.java và tài liệu này.
Sửa: SeatLayoutCreateForm.java, SeatService.java, SeatRepository.java, AdminSeatController.java; admin/seats/list.html; seat-designer-model.js, seat-designer.js; SeatDesignerIntegrationTest.java, ShowtimeWebIntegrationTest.java; test JS và script Edge. Cập nhật hướng dẫn hiện hành để bỏ quy tắc tạo một lần của phiên bản trước. Không sửa Entity Seat/Movie/User hoặc nghiệp vụ Người 1/2.

## Danh sách thay đổi tích lũy so với HEAD

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
- A (chưa theo dõi) docs/seat-direct-save-review.md
- A (chưa theo dõi) docs/seat-simplification-review.md
- A (chưa theo dõi) docs/seat-types-migration.md
- A (chưa theo dõi) src/main/java/com/example/movie/dto/SeatLayoutCreateForm.java
- A (chưa theo dõi) src/main/java/com/example/movie/service/SeatReferenceGuard.java
- A (chưa theo dõi) src/test/java/com/example/movie/SeatDeletionSchemaSafetyTest.java
- A (chưa theo dõi) src/test/java/com/example/movie/SeatLayoutCreationIntegrationTest.java
- A (chưa theo dõi) src/test/java/com/example/movie/SeatLayoutResizeIntegrationTest.java
