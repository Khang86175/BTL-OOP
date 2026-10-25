# Chỉ hỗ trợ Thường và VIP — chuyển tiếp dữ liệu cũ

## Phạm vi và tương thích

- Chức năng nghiệp vụ chỉ nhận NORMAL/VIP; ghế mới mặc định NORMAL. Không thay đổi mức giá của hai loại này.
- SeatType.COUPLE tạm giữ với @Deprecated chỉ để JPA đọc dữ liệu lịch sử, tránh lỗi khi tải Seat đã lưu. Không tạo/gán mới qua form hoặc Service; các endpoint JSON designer/batch đã gỡ; không trả loại này trong ShowtimeService.findActiveSeats.
- Designer hiển thị cảnh báo loại cũ, giữ ID/loại/trạng thái nếu Admin chưa chọn chuyển đổi. Khi lưu thay đổi ghế cũ phải chọn rõ NORMAL hoặc VIP; giữ inactive nếu không chủ động bật. Không tự chuyển đổi dữ liệu lúc khởi động hoặc đọc.
- Phương án mặc định đề xuất là COUPLE → NORMAL. Chỉ bỏ hẳn enum sau khi nhóm xác nhận mọi database/backup được sử dụng, seed, test và module Booking không còn phụ thuộc. Hiện chưa có quyền truy cập hoặc bằng chứng dữ liệu thực tế trên QA/máy thành viên.

## Kiểm tra Booking — phụ thuộc chưa tích hợp

Nhánh fix/seat-aisle-layout hiện không có Booking/Payment hoặc bộ tính giá theo SeatType.
Bản tham chiếu Git có sẵn origin/feature/member-4-booking-clean vẫn có:

- service/pricing/CoupleSeatCalculator.java.
- service/pricing/SeatPriceCalculatorFactory.java: đăng ký SeatType.COUPLE, và fallback loại chưa biết thành giá thường.
- BookingService tính tổng qua factory rồi lưu seatPrice vào BookingSeat. Bản tham chiếu hiện có chưa kiểm tra Seat.active/loại được hỗ trợ trong createBooking và getSeatsByShowtime; cần bổ sung kiểm tra server trước khi tích hợp. Không thể chỉ dựa vào việc lọc danh sách ở Người 3.

Chưa sửa hoặc merge nhánh Người 4. Trước khi tích hợp, Người 4 cần bỏ calculator/registration cũ, từ chối loại không hỗ trợ tại đường đặt vé trực tiếp (không chỉ lọc UI), kiểm tra giữ ghế và bổ sung test. Giữ nguyên quy tắc NORMAL/VIP; không tự tính lại tổng tiền hoặc giá chốt của Booking cũ.

## Kiểm kê chỉ đọc — người quản lý tự thực hiện

Kết nối bằng tài khoản chỉ có quyền cineticketp3qa. Không dùng tài khoản root, không mở database gốc. Với database máy thành viên khác, chính chủ thay schema của họ và chạy kiểm kê; không chạy migration tự động hàng loạt.

```sql
SELECT DATABASE(), CURRENT_USER(), VERSION();
SHOW GRANTS FOR CURRENT_USER;
SELECT seat_type, COUNT(*) AS seat_count
FROM cineticketp3qa.seats GROUP BY seat_type;
SELECT id, room_id, row_label, seat_number, seat_type, CAST(active AS UNSIGNED) AS active
FROM cineticketp3qa.seats WHERE seat_type='COUPLE'
ORDER BY room_id,row_label,seat_number;
SELECT TABLE_NAME,COLUMN_NAME,CONSTRAINT_NAME,REFERENCED_COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA='cineticketp3qa'
  AND REFERENCED_TABLE_SCHEMA='cineticketp3qa'
  AND REFERENCED_TABLE_NAME='seats';
```

Export toàn bộ ghế và sao lưu QA ngoài repository. Nếu có Booking đang giữ/đang xử lý, chốt với Người 4 cách kết thúc hoặc xử lý trước khi đổi loại. Kiểm kê các bảng liên quan theo FK thực tế; không giả định tên bảng hoặc sửa các bản ghi lịch sử.

## Migration đề xuất — CHƯA CHẠY

1. Thống nhất NORMAL (hoặc danh sách ID riêng chuyển VIP) với nhóm, dừng các ứng dụng ghi vào QA.
2. Sao lưu, xuất bản đối chiếu ID/loại/trạng thái, kiểm tra cơ chế giá chốt của Booking.
3. Người quản lý tự chạy transaction sau trên đúng QA đã xác nhận:

```sql
START TRANSACTION;
UPDATE cineticketp3qa.seats SET seat_type='NORMAL' WHERE seat_type='COUPLE';
SELECT ROW_COUNT() AS migrated_seats;
SELECT COUNT(*) AS remaining_legacy FROM cineticketp3qa.seats WHERE seat_type='COUPLE';
-- Kiểm tra số lượng/ID/trạng thái/Booking bằng bản đối chiếu.
-- Chỉ chọn MỘT sau khi kiểm tra: COMMIT; hoặc ROLLBACK;
```

Chỉ đổi cột seat_type, không DELETE/INSERT, không đổi ID/room/vị trí/trạng thái. FK tham chiếu Seat.id không thay đổi; nếu QA có trigger hoặc schema khác, cần rà soát trước. Không sửa priceAtBooking/seatPrice/tổng tiền lịch sử hoặc suy diễn giá mới từ loại ghế sau migration.

4. Khởi động lại QA, tải sơ đồ, kiểm tra NORMAL/VIP và active/inactive, đối chiếu ID, giá và Booking trước/sau. Tab sơ đồ mở từ trước migration phải tải lại để đọc đúng loại ghế vừa chuyển đổi.
5. Chỉ khi kiểm kê tất cả môi trường và tích hợp Booking hoàn tất mới làm đợt riêng xóa constant COUPLE. Không coi bước này đã hoàn thành trong bản sửa hiện tại.

Tài liệu phân công gốc còn đề cập CoupleSeat như phương án cũ; quyết định mới của nhóm trong yêu cầu này được ưu tiên. Tài liệu gốc không bị sửa.
