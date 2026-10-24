# Người 3 — Backend bàn giao Người 4

- Inject ShowtimeService; findOpenShowtimes(movieId, theaterId, date) lọc theo ngày bắt đầu, khoảng [00:00, 00:00 hôm sau). ID bộ lọc có thể null; date null là hôm nay.
- getBookableShowtime(showtimeId) trả ShowtimeSummary gồm ID phim/rạp/phòng, tên, địa chỉ rạp, loại phòng, giờ bắt đầu/kết thúc, giá và trạng thái.
- findActiveSeats(showtimeId) trả SeatSummary: ID ghế ổn định, roomId, hàng, số, loại, active. Đây là ghế vật lý, KHÔNG phải tình trạng còn vé.
- Thời gian LocalDateTime theo Asia/Ho_Chi_Minh. Giá BigDecimal VND; basePrice đã gồm giá phòng, chưa gồm phụ phí ghế. Giá/phụ phí cuối do Booking tính phía server.
- Chỉ OPEN, giờ bắt đầu còn ở tương lai, phim visible, rạp/phòng active mới được đặt. Không có lịch/không mở bán: ScheduleValidationException (field showtimeId).
- CLOSED vẫn chiếm phòng; CANCELLED không chiếm phòng. Hai khoảng nối tiếp được phép; buffer hiện là 0. endTime chốt theo thời lượng phim lúc tạo/sửa lịch; không tự đổi khi Movie đổi thời lượng.
- Các form quản trị không nhận endTime. Mọi thao tác ghi trong bốn service yêu cầu ROLE_ADMIN kể cả gọi trực tiếp. SecurityConfig hiện mở GET /showtimes và /showtimes/**; /admin/** và CSRF giữ nguyên.
- Không cung cấp xóa cứng. Ghế cập nhật tại cùng ID; không chuyển ghế/phòng sang cha khác. Sức chứa = số ghế active.
- Người 4 sở hữu ShowtimeSeat, giữ/bán ghế, transaction booking, phụ phí và snapshot giá. Phải kiểm tra lại lịch và ghế đúng phòng trong transaction; kết quả đọc ở trên không phải giữ chỗ.
- Trước khi tích hợp Booking cần thêm kiểm tra tham chiếu để chặn đổi phim/phòng/giờ của lịch, đổi vị trí ghế hoặc tắt ghế đã có đơn/giữ chỗ theo chính sách nhóm. Hiện chưa có module Booking để kiểm chứng. Giữ ID và không hard delete chưa đồng nghĩa đã đồng bộ Booking.
- Khóa Room tuần tự hóa tạo/sửa lịch trong cùng phòng. Kiểm thử đồng thời hiện chạy H2; cần xác nhận thêm MySQL 8 trên DB thử riêng.
- SQL database/theater-showtime.sql chỉ tạo bốn bảng mới, không tự chạy. IF NOT EXISTS không nâng cấp bảng đã tồn tại khác cấu trúc.
- Đã có Controller và giao diện khách/Admin. Seed Người 3 chỉ bật khi profile demo và showtime.seed.enabled=true, chạy sau seed phim; demo mới tạo phòng trống, không dựng sẵn ghế. Test dùng các database H2 riêng, chưa kiểm chứng MySQL 8.

## Sơ đồ ghế B.3

- SeatService.loadLayout(roomId) và saveLayout(form) dành cho Admin, không phải API giữ/bán ghế.
- Ghế đã có được khớp bằng ID/vị trí; ghế mới có ID mới. Bỏ hàng/giảm số ghế cũ chỉ đổi active=false, không xóa bản ghi.
- Lưu cả sơ đồ dùng transaction, khóa Room và đối chiếu snapshot. Snapshot cũ bị từ chối; thay đổi ghế đã lưu phải được xác nhận.
- findActiveSeats(showtimeId) vẫn giữ chữ ký và ý nghĩa cũ. Nếu Admin tắt một ghế, nó biến mất khỏi danh sách ghế vật lý hoạt động của các lần đọc sau, nhưng trạng thái Booking/ShowtimeSeat hiện chưa được cập nhật vì module đó chưa tồn tại.
- Trình thiết kế không di chuyển ID sang vị trí khác; trang sửa riêng vẫn có thể đổi hàng/số của ghế tại cùng ID. Người 4 cần thống nhất chặn thao tác này khi ghế đã được tham chiếu, hoặc lưu thông tin vé tại thời điểm đặt.

## Điều kiện Người 4 phải kiểm tra trong transaction cuối

1. Lịch vẫn OPEN và chưa bắt đầu; phim/rạp/phòng vẫn cho phép đặt.
2. Ghế tồn tại, active và thuộc đúng roomId của lịch. Danh sách ID không rỗng, không trùng.
3. Khóa/cập nhật có điều kiện trạng thái ShowtimeSeat để hai booking không cùng nhận một ghế.
4. Thống nhất thứ tự khóa giữa các module, phù hợp thứ tự Room trước Showtime/Seat hiện có; xử lý deadlock/timeout có giới hạn trên MySQL.
5. Server tính giá cuối; lưu priceAtBooking để việc đổi basePrice/loại ghế không đổi số tiền đơn cũ.
6. Chốt chính sách lịch đã có booking khi Admin đóng/hủy lịch hoặc ngừng hoạt động rạp/phòng/ghế. Không tự xem thao tác Admin hiện tại là quy trình hoàn/hủy vé.

Chưa có REST /api/booking trong Người 3. Ba phương thức đầu tài liệu là hợp đồng Java Service;
không cần Booking gọi endpoint quản trị hoặc dùng session Admin.
