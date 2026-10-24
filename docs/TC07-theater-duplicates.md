# TC07 — Trùng tên rạp: cần thống nhất quy tắc nghiệp vụ

## Yêu cầu gốc

- phan_chia_cong_viec.md, mục Người 3, dòng 140 chỉ liệt kê Theater: id, name, address, active.
- Dòng 141 quy định tên phòng nên duy nhất trong cùng rạp.
- Dòng 142 quy định unique vị trí ghế (roomId, rowLabel, seatNumber).
- Không có yêu cầu tên rạp hoặc cặp tên + địa chỉ phải duy nhất trong mục Người 3,
  README, tài liệu bàn giao hiện có. HUONG_DAN_CHAY_DU_AN.docx là tài liệu chạy dự án,
  cũng không quy định tính duy nhất của rạp.
- SQL hiện tại chỉ có khóa chính cho theaters; không có unique tên/địa chỉ.

## Hành vi hiện tại và nguyên nhân

Theater chỉ kiểm tra tên/địa chỉ không rỗng và độ dài.
TheaterRepository không có truy vấn kiểm tra trùng.
TheaterService.create() tạo đối tượng mới; save() chỉ strip() tên/địa chỉ rồi saveAndFlush().
AdminTheaterController kiểm tra form và gọi Service, không áp đặt thêm quy tắc trùng.
Vì vậy hai bản ghi cùng tên CineTest Center và địa chỉ 123 Nguyễn Trãi được lưu với hai ID khác nhau,
kể cả khi cả hai active. Đây là hành vi được code hiện tại cho phép; chưa có căn cứ
để đánh dấu FAIL do vi phạm yêu cầu. Không truy cập phiên H2 đang mở để sửa dữ liệu người dùng.

## Kết luận

TC07: CẦN THỐNG NHẤT THÊM QUY TẮC NGHIỆP VỤ, chưa kết luận lỗi.
Không sửa TheaterService, TheaterRepository, Theater, AdminTheaterController hoặc schema vì TC07.
Không xóa/gộp rạp và không tác động phòng, ghế, lịch liên quan.
Chưa áp dụng bản vá, nên không có kết quả test sau sửa TC07.

## Đề xuất chờ chấp thuận

Không cho tạo/sửa thành một cặp (tên, địa chỉ) trùng sau khi strip, gộp khoảng trắng
và so sánh không phân biệt hoa/thường. Không ép tên rạp duy nhất trên toàn hệ thống
vì các chi nhánh có thể cùng thương hiệu. Áp dụng cả bản ghi ngừng hoạt động;
khi cần dùng lại rạp cũ thì bật hoạt động để giữ tham chiếu.

Nếu được duyệt, dự kiến sửa TheaterService (chuẩn hóa/validation),
TheaterRepository (kiểm tra trùng bỏ qua chính ID đang sửa),
Theater và database/theater-showtime.sql (ràng buộc DB tương ứng sau khi chốt cách
chuẩn hóa/collation), cùng các test backend/web của Người 3.
Cần khảo sát bản ghi trùng trước khi thêm unique vào database đã có dữ liệu;
không tự di chuyển, xóa hoặc gộp dữ liệu. Collation MySQL mặc định có thể bỏ qua dấu,
khác với yêu cầu chỉ bỏ qua hoa/thường; phải chốt rõ trước khi chọn unique/cột chuẩn hóa.
