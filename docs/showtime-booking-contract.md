# Người 3 — Backend bàn giao Người 4

- Inject ShowtimeService; findOpenShowtimes(movieId, theaterId, date) lọc theo ngày bắt đầu, khoảng [00:00, 00:00 hôm sau). ID bộ lọc có thể null; date null là hôm nay.
- getBookableShowtime(showtimeId) trả ShowtimeSummary gồm ID phim/rạp/phòng, tên, địa chỉ rạp, loại phòng, giờ bắt đầu/kết thúc, giá và trạng thái.
- findActiveSeats(showtimeId) trả SeatSummary: ID ghế ổn định, roomId, hàng, số, loại, active. Đây là ghế vật lý, KHÔNG phải tình trạng còn vé.
- Thời gian LocalDateTime theo Asia/Ho_Chi_Minh. Giá BigDecimal VND, hai mức độc lập normalPrice/vipPrice trong ShowtimeSummary. NORMAL dùng normalPrice, VIP dùng vipPrice, không cộng phụ phí cố định. basePrice chỉ giữ tương thích legacy; xem showtime-prices-review.md trước khi tích hợp.
- Chỉ OPEN, giờ bắt đầu còn ở tương lai, phim visible, rạp/phòng active và đủ hai giá dương mới được đặt. Lịch cũ thiếu giá vẫn ở Admin nhưng tạm không mở bán; không tự đoán giá VIP. Không có lịch/không mở bán: ScheduleValidationException (field showtimeId).
- CLOSED vẫn chiếm phòng; CANCELLED không chiếm phòng. Hai khoảng nối tiếp được phép; buffer hiện là 0. endTime chốt theo thời lượng phim lúc tạo/sửa lịch; không tự đổi khi Movie đổi thời lượng.
- Các form quản trị không nhận endTime. Mọi thao tác ghi trong bốn service yêu cầu ROLE_ADMIN kể cả gọi trực tiếp. SecurityConfig hiện mở GET /showtimes và /showtimes/**; /admin/** và CSRF giữ nguyên.
- Chỉ xóa Seat chưa được tham chiếu sau xác nhận và kiểm tra FK. Cùng vị trí giữ ID; không di chuyển ID sang phòng/vị trí khác. Sức chứa = số ghế active.
- Người 4 sở hữu ShowtimeSeat, giữ/bán ghế, transaction booking và snapshot giá đã chọn theo loại ghế của suất chiếu. Phải kiểm tra lại lịch và ghế đúng phòng trong transaction; kết quả đọc ở trên không phải giữ chỗ.
- Trước khi tích hợp Booking cần thêm kiểm tra tham chiếu để chặn đổi phim/phòng/giờ của lịch, tắt ghế đã có đơn/giữ chỗ theo chính sách nhóm. Hiện chưa có module Booking để kiểm chứng. Giữ ID và không hard delete chưa đồng nghĩa đã đồng bộ Booking.
- Khóa Room tuần tự hóa tạo/sửa lịch trong cùng phòng. Kiểm thử đồng thời hiện chạy H2; cần xác nhận thêm MySQL 8 trên DB thử riêng.
- SQL database/theater-showtime.sql chỉ tạo bốn bảng mới, không tự chạy. IF NOT EXISTS không nâng cấp bảng đã tồn tại khác cấu trúc.
- Đã có Controller và giao diện khách/Admin. Seed Người 3 chỉ bật khi profile demo và showtime.seed.enabled=true, chạy sau seed phim; demo mới tạo phòng trống, không dựng sẵn ghế. Test dùng các database H2 riêng, chưa kiểm chứng MySQL 8.

## Quản lý ghế đã tinh giản

- SeatService.createLayout(roomId, rowCount, seatsPerRow) thông qua SeatLayoutCreateForm chỉ dành cho Admin, khởi tạo nghiêm ngặt phòng chưa có Seat; web dùng saveLayout để tạo/cập nhật trực tiếp.
- SeatService.saveLayout(SeatLayoutCreateForm) tạo/cập nhật hình chữ nhật; giữ ID/loại/active của vị trí còn lại, ghế mới NORMAL. Giảm kích thước cần xác nhận, khóa Room/Seat và kiểm tra FK trước khi xóa; có tham chiếu thì rollback toàn bộ. deleteSeat/deleteLayout cùng quy tắc. update(id, SeatForm) vẫn chỉ sửa riêng loại/active; không có batch hoặc đổi vị trí ID.
- findByRoom(roomId) giữ nguyên chữ ký và trả toàn bộ ghế vật lý, kể cả inactive/loại cũ, để hiển thị và tích hợp.
- findActiveSeats(showtimeId) giữ nguyên chữ ký SeatSummary; chỉ trả ghế active với NORMAL/VIP. COUPLE tạm còn trong enum để đọc dữ liệu cũ, không trả cho đặt vé mới, không tự thay đổi dữ liệu.
- Không có schema lối đi hoặc phụ thuộc seat_row_layouts; bảng cũ nếu có được giữ nguyên, không tự xóa.
- Không xóa Seat đang được tham chiếu hoặc di chuyển ID. A11/A12 không tự thay đổi khi đọc; chỉ có thể xóa khi Admin chủ động thu nhỏ/xóa, xác nhận và kiểm tra không có tham chiếu. Đổi loại không tính lại giá Booking cũ. Sau khi xóa ghế trống, cập nhật kích thước chủ động có thể tạo lại vị trí thiếu bằng ID mới; GET không tự tạo.

## Điều kiện Người 4 phải kiểm tra trong transaction cuối

1. Lịch vẫn OPEN và chưa bắt đầu; phim/rạp/phòng vẫn cho phép đặt.
2. Ghế tồn tại, active, có loại NORMAL/VIP và thuộc đúng roomId của lịch. Danh sách ID không rỗng, không trùng.
3. Khóa/cập nhật có điều kiện trạng thái ShowtimeSeat để hai booking không cùng nhận một ghế.
4. Thống nhất thứ tự khóa giữa các module, phù hợp thứ tự Room trước Showtime/Seat hiện có; xử lý deadlock/timeout có giới hạn trên MySQL.
5. Server chọn Showtime.normalPrice hoặc vipPrice theo SeatType (có thể gọi Showtime.priceFor); lưu priceAtBooking và tổng đơn chốt để việc đổi giá/loại ghế không đổi số tiền đơn cũ. Bỏ công thức basePrice + phụ phí trong Booking; không tự tính lại hóa đơn cũ.
6. Chốt chính sách lịch đã có booking khi Admin đóng/hủy lịch hoặc ngừng hoạt động rạp/phòng/ghế. Không tự xem thao tác Admin hiện tại là quy trình hoàn/hủy vé.

Chưa có REST /api/booking trong Người 3. Ba phương thức đầu tài liệu là hợp đồng Java Service;
không cần Booking gọi endpoint quản trị hoặc dùng session Admin.
