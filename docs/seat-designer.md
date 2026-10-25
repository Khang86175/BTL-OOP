# Seat Designer — cập nhật trực tiếp, không có bản nháp riêng

## Thao tác

1. Vào /admin/seats?roomId=... với tài khoản ADMIN.
2. Nhập số hàng và số ghế mỗi hàng. Sơ đồ hiển thị thay đổi ngay; chưa có dữ liệu nào được ghi chỉ bởi thay đổi input.
3. Nhấn Tạo sơ đồ cho phòng trống hoặc Cập nhật sơ đồ cho phòng có ghế. Một POST lưu trực tiếp; không có Lưu bản nháp, không lưu bản nháp vào DB/localStorage, không bước duyệt lưu lần hai.
4. Nhấn từng ghế đã lưu để sửa NORMAL/VIP hoặc active/inactive, rồi Lưu ghế. Ghế mới trong hình xem trước chỉ sửa sau khi đã được tạo.
5. Xóa ghế đang chọn hoặc Xóa toàn bộ sơ đồ cần một lần xác nhận. Giảm kích thước cũng chỉ xác nhận thao tác xóa các ghế ngoài phạm vi; nếu hủy thì không gửi yêu cầu ghi.

Hàng luôn A, B, C… (sau Z là AA), số ghế từ 1. Ghế mới mặc định NORMAL/active. Giới hạn 1–30 hàng, 1–200 ghế mỗi hàng, tổng tối đa 1.000 ghế.
Không có COUPLE trong lựa chọn, lối đi, số bắt đầu tùy chỉnh, chọn nhiều ghế hoặc đổi loại hàng loạt.

## Bảo toàn và xóa an toàn

- Cùng vị trí giữ ID, loại và trạng thái, kể cả inactive; đổi kích thước không tự bật lại ghế.
- Ghế ngoài hình chữ nhật chỉ bị xóa khi Admin xác nhận và tất cả ghế đó chưa được tham chiếu. Nếu một ghế bị tham chiếu, toàn bộ cập nhật bị từ chối, không xóa một phần hoặc thêm một phần.
- Khóa Room và các Seat trong transaction; kiểm tra metadata FK và các bảng con trước DELETE; rollback nếu có lỗi.
- Chặn cả booking_seats, showtime_seats và bảng khác có FK tới Seat.id, kể cả FK CASCADE. Không tự dọn ShowtimeSeat AVAILABLE; đây vẫn là tham chiếu cần giữ.
- Nếu bảng Booking/ShowtimeSeat đã có nhưng thiếu FK seat_id, không đọc được metadata hoặc không xác minh được cấu trúc, thao tác xóa bị chặn. Cần quyền đọc metadata và FK thực tế trước khi cho phép xóa.
- Không thay đổi phòng khác. ID không di chuyển sang hàng/số khác.
- Xóa riêng không đánh lại số ghế còn lại. GET/tải lại không tạo lại ghế đã xóa. Nếu Admin chủ động cập nhật hình chữ nhật chứa vị trí đã xóa, vị trí thiếu được tạo mới với ID mới, NORMAL/active.
- A11/A12 hoặc hàng cũ bất thường không tự thay đổi khi mở trang. Nếu chúng ngoài kích thước Admin mới chọn, áp dụng cùng quy tắc xác nhận/kiểm tra tham chiếu, không tự bỏ qua kiểm tra.
- Không cần bảng SeatRowLayout hoặc schema mới. Bảng seat_row_layouts nếu từng có vẫn để nguyên; không có DROP/migration tự động.

## Endpoint quản trị

- GET /admin/seats?roomId=...: chỉ đọc; kích thước mặc định suy ra từ ghế thực tế, hoặc 5 × 8 khi phòng trống.
- POST /admin/seats/layout: SeatLayoutCreateForm(roomId, rowCount, seatsPerRow, confirmRemoval). Flag xóa không thay thế kiểm tra tham chiếu ở server.
- GET /admin/seats/{id}/edit và POST /admin/seats/{id}: sửa riêng loại/trạng thái theo SeatForm cũ.
- POST /admin/seats/{id}/delete: roomId, confirmed; kiểm tra ghế thuộc đúng phòng.
- POST /admin/seats/layout/delete: roomId, confirmed; tất cả hoặc không xóa gì.
- Mọi POST có CSRF, mọi URL quản trị yêu cầu ADMIN. Service kiểm tra Admin kể cả gọi trực tiếp. Endpoint batch/designer cũ vẫn bị gỡ.

SeatService.createLayout vẫn có để khởi tạo nghiêm ngặt cho Java caller cũ; web dùng saveLayout để tạo/cập nhật trực tiếp. API Booking và SeatSummary không đổi; ghế active không đồng nghĩa còn vé.

COUPLE chỉ tạm giữ trong enum để JPA đọc dữ liệu cũ; không cho gán mới. Chọn NORMAL/VIP để chuyển đổi rõ ràng; không tự chạy migration. Xem seat-types-migration.md.

Kết quả kiểm thử và danh sách file hiện tại: seat-direct-save-review.md. Các báo cáo tinh giản trước đó chỉ mô tả phiên bản lịch sử.
