# B.3 — Trình thiết kế sơ đồ ghế

## Mở trên H2 Demo

Build rồi khởi động JAR mới:

```powershell
java -jar target/movie-0.0.1-SNAPSHOT.jar --spring.profiles.active=demo --showtime.seed.enabled=true
```

Đăng nhập admin@cineticket.test / DemoAdmin123!, mở Phòng và ghế, chọn Ghế của một phòng.
URL chính giữ nguyên: /admin/seats?roomId=ID.
Seed demo chỉ tạo rạp/phòng/lịch chiếu, không tạo ghế hay dựng sẵn A/B/C.
Phòng demo mới bắt đầu rỗng. Ghế đã được lưu trước đây vẫn được tải để bảo toàn dữ liệu;
seed không tự xóa hoặc đặt lại sơ đồ đã có.
Phòng rỗng hiện chế độ Tạo sơ đồ mới; phòng đã có dữ liệu hiện Chỉnh sửa sơ đồ hiện có.
JavaScript cần bật. Khi tắt JavaScript, vẫn có liên kết thêm/sửa ghế riêng.

## Thao tác trực tiếp trên sơ đồ

1. Nhập hàng bắt đầu A, tổng 5 hàng, 10 ghế mặc định. Tạo sơ đồ sẽ hiện A–E ngay,
   mỗi hàng 10 vị trí. Tất cả ghế mới khởi tạo là NORMAL (Thường), chỉ là mặc định
   kỹ thuật; hệ thống không tự gán VIP/Ghế đôi theo vị trí hàng.
   Nút Tạo / cập nhật bố cục cập nhật cả số ghế của các hàng đã có trong khoảng chọn,
   không bỏ qua A/B/C. Ví dụ A–E, 10 ghế sẽ mở rộng A/B/C từ 8 lên 10 và thêm D/E.
   Giữ nguyên ID, loại và trạng thái từng ghế cũ; vị trí mới là Thường.
   Khi giảm số ghế, ghế cũ ngoài khoảng chỉ chuyển ngừng hoạt động và vẫn hiện xám.
   Hàng ngoài khoảng chọn không bị tự xóa.
2. Nhấn chữ C để đặt 14 ghế VIP, D 14 VIP, E 8 ghế đôi. Ghế luôn căn giữa cùng
   trục màn hình; nhãn hàng dùng cột riêng bên trái và căn giữa theo chiều dọc.
3. Các trường Loại/Trạng thái mặc định giữ tùy chỉnh riêng từng ghế.
   Muốn áp dụng loại/trạng thái chọn cho cả ghế cũ, tích ô xác nhận trong bảng hàng.
   Nếu không tích, lựa chọn chỉ áp dụng cho ghế mới; ghế cũ giữ nguyên thuộc tính.
4. Nhấn một ghế để sửa loại/trạng thái trong bản nháp; trang sửa riêng cũ vẫn có liên kết.
   Dùng ô chọn để chỉnh nhiều ghế, hoặc Chọn toàn bộ hàng này từ bảng hàng.
5. Chừa lối đi bằng danh sách số vị trí, ví dụ 6,7. Số lượng là số ghế thực, không
   bao gồm lối đi: 12 ghế bắt đầu từ 1, bỏ 6,7 sẽ dùng 1–5 và 8–14.
   Không có bản ghi ghế giả.
6. Nhấn Lưu sơ đồ phòng chiếu. Nếu có đổi loại/trạng thái ghế cũ, cần tích xác nhận
   ở phần lưu. Tất cả thay đổi được gửi trong một transaction và trang tải lại từ DB.

## Giảm số ghế, xóa hàng và bảo toàn dữ liệu

- Hàng/ghế mới chưa lưu: bỏ khỏi bản nháp ngay, kể cả hàng cuối cùng.
- Ghế đã lưu nằm ngoài khoảng mới, tại lối đi hoặc trong hàng bị xóa: chuyển ngừng
  hoạt động, vẫn hiện xám trên bản nháp và sau khi lưu. Không hard delete và không đổi ID.
- Vì schema chưa có khái niệm vị trí đã loại khỏi sơ đồ khác với ghế tạm ngừng hoạt động,
  ghế cũ vẫn hiển thị xám tại vị trí cũ. Đây là lựa chọn bảo toàn dữ liệu được thông báo
  rõ; không giả vờ đã xóa ghế hay đã kiểm tra module Booking chưa tồn tại.
- Muốn bật lại, chọn ghế/hàng đó, chọn Hoạt động và xác nhận thay đổi ghế cũ.
- Không tự tính ghế đôi thành hai vé. Không thay đổi API Booking, Entity/schema hoặc
  logic User/Auth/Movie/Category.

## Đồng thời, xác nhận và giới hạn

- loadLayout() trả ghế và snapshot từ cùng một lần đọc.
- saveLayout() khóa Room, đối chiếu snapshot, xác thực vị trí/ID và yêu cầu xác nhận
  khi có thay đổi ghế đã lưu. Snapshot cũ trả 409, không ghi đè chỉnh sửa ở nơi khác.
- Ghế cũ được khớp bằng ID và vị trí; vị trí đã có không bị coi là insert mới.
  Không nhận ID phòng khác và không chuyển ID sang vị trí khác trong trình thiết kế.
- Ghế cũ bị bỏ khỏi yêu cầu chỉ ngừng hoạt động. Ghế mới chỉ được insert sau khi
  toàn bộ yêu cầu hợp lệ. Lỗi DB rollback cả cập nhật cũ và insert mới.
- Không bắt buộc phòng có hàng khi lưu sơ đồ rỗng. Hàng rỗng chưa có ghế chỉ tồn tại
  trong bản nháp, vì schema không có bảng hàng ghế riêng.
- Trình thiết kế giới hạn 1.000 ghế; công cụ sinh hàng tối đa 30 hàng mỗi lần,
  số vị trí mới từ 1 đến 200. ID được giữ dưới dạng chuỗi trong JavaScript để không
  bị mất độ chính xác số nguyên Long.

## Kiểm thử

- SeatDesignerIntegrationTest: tạo A–E, sửa hàng đã có, tăng/giảm, giữ ID, ngừng hoạt
  động cả hàng, bảo vệ tùy chỉnh, snapshot, ID sai, rollback, đồng thời, quyền/CSRF.
- seat-designer-model.test.js: kiểm tra mô hình bản nháp và công thức tọa độ, độc lập DOM.
- src/test/scripts/seat-designer-browser.ps1: chạy JAR H2 riêng cổng 18083 và Edge headless
  để thao tác thực, đo căn giữa/nhãn hàng theo pixel, lưu/tải lại, đối chiếu ID.
  Script tự dừng tiến trình do nó tạo; không chạm ứng dụng demo đang chạy cổng 8080.
  Kết quả/ảnh nằm trong target/seat-designer-browser, không đưa vào Git.

Các endpoint B.2 vẫn giữ để tương thích các luồng/test cũ, nhưng trang chính đã chuyển
sang trình thiết kế. TC07 về trùng rạp vẫn chưa áp dụng quy tắc mới.
