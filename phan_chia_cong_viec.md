# Phân chia công việc — Website đặt vé xem phim

## 1. Ngôn ngữ và công nghệ đề xuất

### Lựa chọn phù hợp nhất với nhóm
- **Backend:** Java 17 hoặc 21, Spring Boot theo mô hình MVC.
- **Giao diện:** HTML, CSS, một ít JavaScript; dùng Thymeleaf để render trang từ Java. Có thể dùng Bootstrap để làm giao diện nhanh.
- **Cơ sở dữ liệu:** MySQL.
- **Kết nối DB:** Spring Data JPA/Hibernate; viết truy vấn SQL khi cần báo cáo hoặc khi giảng viên yêu cầu.
- **Build:** Maven; quản lý mã nguồn bằng Git và thống nhất nhánh `main`/nhánh tính năng.
- **Mật khẩu:** Spring Security và BCrypt (chỉ lưu chuỗi hash, tuyệt đối không lưu mật khẩu gốc).

## 2. Kiến trúc và quy ước dùng chung

### Luồng xử lý
`Browser → Controller → Service → Repository → MySQL`

- **Controller:** nhận request, kiểm tra đầu vào cơ bản, trả trang hoặc điều hướng; không viết truy vấn SQL và nghiệp vụ dài ở đây.
- **Service:** chứa quy tắc nghiệp vụ, ví dụ kiểm tra quyền, trạng thái phim, chỗ ngồi còn trống.
- **Repository:** đọc/ghi dữ liệu.
- **Entity/Model:** ánh xạ dữ liệu và quan hệ.
- **Template:** các trang Thymeleaf; CSS/ảnh để trong `static`.

Không để mỗi thành viên tạo một project hoặc một cấu hình kết nối DB riêng. Cùng dùng một project, một file cấu hình mẫu và cùng quy ước tên.

### Các bảng chính cần thống nhất sớm
- `users`: tài khoản, email duy nhất, `password_hash`, trạng thái, vai trò.
- `movies`, `categories`, `movie_category`: phim và thể loại.
- `theaters`, `rooms`, `seats`, `showtimes`: rạp, phòng, ghế và lịch chiếu.
- `bookings`, `booking_seats`, `payments`: đơn đặt, ghế đã đặt và giao dịch giả lập.

Các tên trên là gợi ý; cả nhóm cần thống nhất khóa chính/khóa ngoại, kiểu dữ liệu, tên trạng thái và lưu thành migration hoặc file `schema.sql` trước khi chia nhau làm sâu. Không để mỗi người tự sửa schema theo cách không báo cho nhóm.

### Quy tắc bàn giao giữa các phần
- Người 1 bàn giao API/service xác thực và thông tin người dùng hiện tại cho các trang cần đăng nhập.
- Người 2 bàn giao hàm lấy danh sách phim đang hiển thị và chi tiết phim cho người 3 tạo lịch chiếu.
- Người 3 bàn giao lịch chiếu cho người 4; người 4 bàn giao booking đã tạo cho người 5 xử lý thanh toán.
- Dùng chung ID entity và DB, không truyền nguyên đối tượng qua các module nếu chỉ cần ID.

## 3. Phân công theo thành viên

### Người 1 — Tài khoản, đăng nhập và phân quyền (User & Authentication)

**Mục tiêu:** người dùng đăng ký/đăng nhập được; người chưa đăng nhập, khách hàng và admin chỉ truy cập đúng chức năng của mình.

#### Phạm vi công việc
1. Đăng ký tài khoản Customer; đăng nhập, đăng xuất và xem/sửa hồ sơ cá nhân.
2. Phân quyền tối thiểu `CUSTOMER` và `ADMIN`. Chỉ mở rộng `STAFF` khi luồng sử dụng đã rõ (ví dụ được xem/soát vé nhưng không quản lý phim).
3. Bảo vệ các trang quản trị ở phía server, không chỉ ẩn nút Admin trên giao diện.
4. Tạo một tài khoản Admin mẫu bằng seed data hoặc hướng dẫn cấu hình; không cho người dùng tự đăng ký với quyền Admin.
5. Quên mật khẩu: nếu không có email thật, làm luồng demo có token/mã dùng một lần và ghi rõ là giả lập. Không gửi mật khẩu cũ hoặc đặt mật khẩu mặc định dễ đoán.

#### Dữ liệu gợi ý
`User`: `id`, `fullName`, `email` (unique), `passwordHash`, `phone` (nếu cần), `role`, `enabled`, `createdAt`.

Email nên được chuẩn hóa (trim, lowercase) và kiểm tra trùng. Không lưu mật khẩu gốc. Dùng BCrypt qua Spring Security; không tự viết thuật toán mã hóa mật khẩu.

#### Gợi ý cấu trúc Java
- `User` entity/model và `Role` enum.
- `UserRepository`: tìm theo email, kiểm tra email đã tồn tại.
- `AuthService`/`UserService`: đăng ký, cập nhật hồ sơ, đổi hoặc đặt lại mật khẩu.
- Controller cho `/register`, `/login`, `/logout`, `/profile`; cấu hình bảo vệ đường dẫn `/admin/**`.
- DTO/form riêng cho đăng ký và cập nhật hồ sơ; không bind trực tiếp trường `role` hoặc `passwordHash` từ form người dùng.

#### Chọn cách thể hiện OOP
Về thiết kế ứng dụng, phương án đơn giản và dễ quản lý nhất là **một `User` entity có `Role` enum** (`CUSTOMER`, `ADMIN`, có thể thêm `STAFF`). Phân quyền được cấu hình rõ ở Spring Security.

Nếu giảng viên **bắt buộc** phải có `abstract class User`, `Admin` và `Customer` kế thừa, hãy xác nhận trước rồi mới làm. Khi đó có thể dùng JPA inheritance kiểu `SINGLE_TABLE`, nhưng sẽ phức tạp hơn khi tạo tài khoản, truy vấn và phân quyền. Tránh vừa có subclass vừa có một trường role hoạt động độc lập mà không quy định cái nào là nguồn dữ liệu chính.

#### Trình tự thực hiện
1. Cùng nhóm chốt schema `users`, vai trò và cách đăng nhập (session/Spring Security).
2. Tạo entity, repository và seed Admin; chạy thử kết nối MySQL.
3. Làm đăng ký và validation: email đúng định dạng, email không trùng, mật khẩu đạt quy tắc tối thiểu.
4. Làm đăng nhập/đăng xuất; kiểm tra mật khẩu qua BCrypt.
5. Bảo vệ trang quản trị ở server; thử truy cập URL Admin trực tiếp bằng tài khoản Customer và trạng thái chưa đăng nhập.
6. Làm hồ sơ cá nhân và luồng quên mật khẩu demo nếu còn thời gian.
7. Viết test và bàn giao hàm/API cho các thành viên khác.

#### Tiêu chí hoàn thành
- Đăng ký hợp lệ tạo tài khoản Customer; email trùng bị từ chối.
- Database chỉ có password hash; đăng nhập đúng thành công, sai thất bại.
- Customer không truy cập được chức năng Admin kể cả khi nhập URL trực tiếp.
- Admin mẫu đăng nhập được; cập nhật hồ sơ không thể tự đổi role.
- Có hướng dẫn cấu hình tài khoản DB và dữ liệu Admin mẫu trong README, không commit mật khẩu thật.

#### Người 1 không phụ trách
Không tự sửa module phim/lịch chiếu/đặt vé. Khi module khác cần thông tin người dùng, cung cấp phương thức dùng chung để lấy user hiện tại thay vì mỗi người tự truy vấn bảng `users`.

### Người 2 — Phim và thể loại (Movie Catalog)

**Mục tiêu:** Admin quản lý được phim; khách xem được phim đang chiếu/sắp chiếu, tìm kiếm, lọc và xem chi tiết.

#### Dữ liệu gợi ý
- `Movie`: `id`, `title`, `director`, `durationMinutes`, `releaseDate`, `ageRating`, `synopsis`, `posterUrl`, `trailerUrl`, `status` hoặc `visible`, `createdAt`.
- `Category`: `id`, `name` (unique).
- `movie_category`: khóa ngoại `movie_id`, `category_id`, khóa chính ghép để tránh gắn trùng.

Một phim có thể có nhiều thể loại và một thể loại có thể thuộc nhiều phim, vì vậy dùng `@ManyToMany` hoặc entity trung gian `MovieCategory`; không dùng composition cho quan hệ này. Nếu cần lưu thêm thông tin trên lần gắn thể loại (ví dụ thứ tự/ghi chú), dùng entity trung gian.

#### Trạng thái và xóa phim
- Tách **trạng thái theo ngày chiếu** khỏi **ẩn/hiện trên website**. Trạng thái `NOW_SHOWING`/`COMING_SOON` có thể suy ra từ `releaseDate` và ngày hiện tại; trường `visible`/`active` dùng để Admin ẩn nội dung.
- Không xóa cứng phim đã được lịch chiếu/đơn đặt tham chiếu. Dùng ẩn/soft delete hoặc từ chối xóa khi đã có dữ liệu liên quan.
- Để đơn giản đồ án, poster/trailer có thể là URL; chỉ làm upload file nếu yêu cầu và thời gian cho phép.

#### Chức năng
- Admin: danh sách phim, thêm, sửa, kiểm tra dữ liệu, ẩn/hiện phim; quản lý thể loại nếu nằm trong phạm vi.
- Khách: danh sách phim đang chiếu/sắp chiếu, trang chi tiết, tìm theo tên và lọc theo thể loại.
- Sắp xếp theo ngày ra mắt hoặc tên; phân trang chỉ làm nếu số dữ liệu lớn hoặc đề bài yêu cầu.

#### Gợi ý cấu trúc Java
- `Movie` và `Category` entity/model.
- `MovieRepository`: tìm phim hiển thị, tìm theo tên, lọc theo category, lấy chi tiết.
- `MovieService`: kiểm tra thời lượng/ngày/tuổi, xử lý trạng thái hiển thị và quy tắc xóa.
- Controller cho trang danh sách, chi tiết và các form Admin; chỉ Admin được tạo/sửa/ẩn.
- Các template như `movies/list`, `movies/detail`, `admin/movies/form`.

#### Trình tự thực hiện
1. Chốt với nhóm các trường phim, category và trạng thái; tạo migration/seed vài phim mẫu.
2. Làm entity/repository và xác nhận quan hệ phim–thể loại chạy được trên MySQL.
3. Làm chức năng xem danh sách và chi tiết trước để người 3 có dữ liệu phim tạo lịch chiếu.
4. Làm tìm kiếm/lọc/sắp xếp; xử lý trường hợp không có kết quả và phim đã bị ẩn.
5. Làm form Admin thêm/sửa/ẩn; kiểm tra đầu vào cả server-side.
6. Viết test cho tìm kiếm, lọc, trạng thái và bàn giao phương thức lấy phim cho người 3.

#### Tiêu chí hoàn thành
- Khách chỉ thấy phim được phép hiển thị; lọc và tìm kiếm cho kết quả chính xác.
- Admin thêm/sửa/ẩn được phim; dữ liệu sai như thời lượng âm hoặc thiếu tên bị từ chối.
- Có thể gắn một phim với nhiều thể loại và một thể loại với nhiều phim.
- Phim đang được lịch chiếu/đặt vé sử dụng không bị xóa gây hỏng khóa ngoại.
- Có dữ liệu mẫu và hướng dẫn trang/luồng kiểm thử.

#### Người 2 không phụ trách
Không tự quản lý phòng chiếu, lịch chiếu, ghế hoặc booking. Người 3 chỉ cần nhận `movieId` và các hàm truy vấn phim đã thống nhất.

### Người 3 — Rạp, phòng và lịch chiếu (Theater & Showtime)

**Mục tiêu:** Admin tạo được lịch chiếu hợp lệ cho một phim trong một phòng; khách xem được lịch theo phim/rạp/ngày và chọn lịch đó để đặt vé.

#### Dữ liệu gợi ý
- `Theater`: `id`, `name`, `address`, `active`.
- `Room`: `id`, `theaterId`, `name`, `roomType`, `active`. Tên phòng nên duy nhất trong cùng rạp.
- `Seat`: `id`, `roomId`, `rowLabel`, `seatNumber`, `seatType`, `active`; unique theo `(roomId, rowLabel, seatNumber)`.
- `Showtime`: `id`, `movieId`, `roomId`, `startTime`, `endTime`, `basePrice`, `status`.

Sức chứa nên được tính từ số ghế đang hoạt động trong phòng thay vì vừa lưu một con số vừa lưu danh sách ghế dễ lệch nhau. `endTime` được tính từ thời lượng phim; nếu nhóm quy định thời gian dọn phòng thì cộng thêm buffer, ví dụ 15 phút. Dùng `LocalDateTime`/`datetime` và thống nhất múi giờ với cả nhóm.

#### Quy tắc nghiệp vụ
- Chỉ tạo lịch cho phim/phòng đang hoạt động; không tạo lịch chiếu bắt đầu trong quá khứ.
- Hai lịch trong **cùng phòng** bị trùng nếu `lịch_cũ.startTime < lịch_mới.endTime` và `lịch_cũ.endTime > lịch_mới.startTime`. Hai lịch ở hai phòng khác nhau có thể diễn ra cùng giờ.
- Không cho xóa cứng phòng/phim đã được lịch chiếu hoặc booking tham chiếu; dùng trạng thái ẩn/ngừng hoạt động.
- Loại phòng 2D/3D/IMAX có thể dùng enum nếu chỉ khác phụ phí. Chỉ tạo interface/strategy và các implementation riêng khi mỗi loại thực sự có cách tính giá/hành vi khác nhau.
- Giá cơ bản và phụ phí phải được xác định ở server. Người 4 sẽ cộng giá loại ghế; cần chốt công thức với họ trước khi triển khai.

#### Gợi ý cấu trúc Java và trang
- Entity/model: `Theater`, `Room`, `Seat`, `Showtime`.
- Repository: tìm rạp/phòng, lịch theo phim/rạp/ngày, và các lịch trong khoảng thời gian để kiểm tra trùng.
- Service: `ShowtimeService` tính `endTime`, kiểm tra xung đột và tạo/cập nhật lịch trong transaction.
- Controller/trang: Admin quản lý rạp/phòng/lịch; khách xem lịch theo phim và chọn ngày/rạp.
- Chỉ Admin được sửa dữ liệu quản trị; khách chỉ xem lịch đang mở bán.

#### Trình tự thực hiện
1. Chốt với người 2 cách lấy `Movie`/thời lượng phim; chốt với người 4 cấu trúc phòng, ghế và giá.
2. Tạo rạp, phòng và sơ đồ ghế mẫu; kiểm tra tổng ghế và loại ghế.
3. Làm trang xem lịch theo phim/ngày trước, để kiểm tra luồng người dùng.
4. Làm form Admin tạo/sửa/ẩn lịch; tính giờ kết thúc ở server.
5. Viết hàm kiểm tra trùng lịch và test các trường hợp chồng lấn, nối tiếp, khác phòng.
6. Bàn giao `showtimeId`, `movieId`, `roomId`, giờ bắt đầu/kết thúc, thông tin ghế và giá cho người 4.

#### Tiêu chí hoàn thành
- Không thể tạo hai lịch chồng thời gian trong cùng phòng; có thể tạo lịch cùng giờ ở phòng khác.
- Lịch không hợp lệ (phim/phòng bị ẩn, thời gian quá khứ, giá âm) bị từ chối ở server.
- Khách xem được lịch theo phim và ngày; lịch đã đóng/ẩn không được chọn để đặt.
- Phòng có sơ đồ ghế ổn định; ID ghế không thay đổi khi chỉ đổi tên hiển thị.
- Có test kiểm tra xung đột và dữ liệu mẫu đủ để demo.

#### Người 3 không phụ trách
Không tự tạo booking hoặc quyết định ghế đã đặt hay chưa. Trạng thái giữ/đặt ghế do người 4 quản lý; người 3 chỉ cung cấp phòng, ghế vật lý và lịch chiếu.

### Người 4 — Đặt vé và chọn ghế (Booking Core)

**Mục tiêu:** khách chọn lịch chiếu và ghế, thấy đúng giá, rồi tạo được booking mà không thể đặt trùng ghế với khách khác.

#### Dữ liệu và ranh giới với người 3/người 5
- Nhận từ người 3: `showtimeId`, danh sách ghế của phòng, loại ghế và giá cơ bản/phụ phí phòng.
- `Booking`: `id`, `userId`, `showtimeId`, `status`, `totalAmount`, `createdAt`, `expiresAt` (nếu giữ ghế tạm).
- `BookingSeat`: `id`, `bookingId`, `showtimeId`, `seatId`, `priceAtBooking`. Lưu giá tại thời điểm đặt để việc đổi giá ghế sau này không sửa hóa đơn cũ.
- Bàn giao cho người 5: booking đã tạo, số tiền server tính, danh sách ghế và trạng thái thanh toán ban đầu.

Hai người có thể bấm cùng một ghế gần như đồng thời, nên chỉ kiểm tra ở giao diện là không đủ. Phương án rõ ràng cho đồ án là có `ShowtimeSeat` (mỗi cặp lịch chiếu–ghế có một dòng, unique theo `(showtimeId, seatId)`) với trạng thái `AVAILABLE`, `HELD`, `BOOKED` và thời điểm hết hạn giữ ghế. Khi chọn/xác nhận ghế, cập nhật trạng thái trong transaction và chỉ thành công nếu ghế vẫn còn trống. Nếu nhóm bỏ giữ ghế tạm để giảm phạm vi, vẫn phải khóa/kiểm tra lại ghế trong transaction lúc tạo booking.

#### Giá và loại ghế
- Giá cuối được tính trong service Java: `giá cơ bản + phụ phí phòng + phụ phí ghế` (hoặc công thức nhóm thống nhất). Không nhận `totalAmount` do browser gửi lên làm giá có thẩm quyền.
- Nếu dùng `NormalSeat`, `VIPSeat`, `CoupleSeat` để thể hiện đa hình, mỗi loại cung cấp quy tắc phụ phí/tính giá; service booking gọi chung phương thức tính giá.
- Ghế đôi cần chốt cách mô hình: nếu chiếm hai vị trí vật lý thì phải giữ cả hai vị trí cùng lúc, hoặc mô hình nó là một đơn vị ghế riêng không cho đặt một nửa. Không để hai booking cùng chiếm một bên của ghế đôi.
- Chặn số lượng vé tối đa (ví dụ 5) ở server, kiểm tra số ghế duy nhất trong yêu cầu và từ chối danh sách rỗng/trùng.

#### Luồng đặt vé đề xuất
1. Khách mở lịch chiếu; server trả sơ đồ ghế và trạng thái còn trống/đang giữ/đã đặt.
2. Khách chọn ghế; server kiểm tra ghế thuộc đúng phòng của lịch chiếu và tính giá tạm tính.
3. Khi xác nhận, server kiểm tra lại trạng thái ghế trong transaction; nếu còn trống thì tạo booking `PENDING_PAYMENT` và giữ ghế trong thời hạn đã thống nhất.
4. Người 5 xử lý thanh toán. Thành công thì booking/ghế thành `CONFIRMED`/`BOOKED`; thất bại cho phép thử lại hoặc hủy theo quy tắc nhóm.
5. Khi hết thời gian giữ mà chưa thanh toán, booking thành `EXPIRED` và ghế được trả lại `AVAILABLE`.

Thống nhất các trạng thái với người 5 trước: gợi ý booking `PENDING_PAYMENT`, `CONFIRMED`, `CANCELLED`, `EXPIRED`; thanh toán `PENDING`, `SUCCESS`, `FAILED`. Không để hai module dùng enum khác tên cho cùng ý nghĩa.

#### Gợi ý cấu trúc Java
- `Booking`, `BookingSeat`, `ShowtimeSeat` và các enum trạng thái.
- `BookingRepository`/repository ghế: truy vấn booking của khách, trạng thái ghế theo showtime; cần khóa hoặc cập nhật có điều kiện khi nhận đặt ghế.
- `BookingService`: xác thực quyền sở hữu lịch/ghế, tính giá, giới hạn số vé, tạo/hủy booking và xử lý hết hạn.
- Controller chỉ nhận `showtimeId` và danh sách ID ghế; không tin giá hoặc thông tin trạng thái từ client.
- Trang chọn ghế hiển thị chú giải màu sắc; trạng thái ghế cũng phải có chữ/nhãn, không chỉ phân biệt bằng màu.

#### Trình tự thực hiện
1. Vẽ luồng và chốt schema/trạng thái với người 3 và người 5 trước khi code.
2. Làm trang sơ đồ ghế mẫu và lấy đúng ghế theo `showtimeId`.
3. Làm service tính giá và giới hạn số lượng; viết test giá cho từng loại ghế.
4. Làm tạo booking có kiểm tra trạng thái ghế trong transaction; thử hai lần đặt cùng ghế.
5. Nối kết quả booking với bước thanh toán giả lập của người 5; xử lý thành công, thất bại, hủy và hết hạn.
6. Viết test và bàn giao trạng thái/API rõ ràng; không chỉ bàn giao bằng cách giải thích miệng.

#### Tiêu chí hoàn thành
- Không đặt được ghế đã giữ/đã bán; hai yêu cầu đồng thời không tạo được hai booking cho cùng một ghế/lịch.
- Không đặt ghế thuộc phòng khác hoặc lịch chiếu đã đóng.
- Giá, tổng tiền và giới hạn số vé được tính/kiểm tra ở server.
- Ghế được trả lại khi booking hết hạn/hủy theo quy tắc đã chọn; booking thành công giữ đúng ghế.
- Có test giá, đặt trùng, quá số lượng, ghế không thuộc lịch và luồng booking hết hạn.

#### Người 4 không phụ trách
Không tự xác nhận giao dịch thanh toán hoặc tính doanh thu. Người 4 chịu trách nhiệm booking/ghế; người 5 chịu trách nhiệm payment/biên nhận và thống kê.

### Người 5 — Thanh toán giả lập, vé và thống kê (Payment & Dashboard)

**Mục tiêu:** hoàn tất luồng thanh toán demo cho booking, phát hành thông tin vé/lịch sử và cung cấp thống kê Admin từ dữ liệu đã thanh toán.

#### Dữ liệu và trạng thái cần thống nhất với người 4
- `Payment`: `id`, `bookingId`, `method`, `amount`, `status`, `transactionRef`, `createdAt`, `paidAt`. Có thể cho phép nhiều lần thử thanh toán cho cùng một booking; mỗi lần thử là một payment riêng.
- Một booking chỉ được xác nhận khi có payment `SUCCESS`; amount của payment phải bằng tổng tiền đã chốt trong booking.
- Tên trạng thái gợi ý: payment `PENDING`, `SUCCESS`, `FAILED`; booking dùng đúng enum/thỏa thuận ở phần người 4.
- Thống nhất chính sách khi thanh toán thất bại: cho thử lại trong thời gian giữ ghế; hết hạn thì booking bị `EXPIRED` và ghế được trả lại.

#### Phạm vi thanh toán an toàn cho đồ án
- Làm màn hình chọn phương thức và nút **Thanh toán giả lập**; hệ thống trả kết quả thành công/thất bại theo lựa chọn demo hoặc quy tắc đơn giản.
- `PaymentStrategy` có thể có `PaymentStrategy`, `MockCardPayment`, `MockWalletPayment`; interface nhận số tiền và trả kết quả thanh toán. Tên Momo/ZaloPay chỉ dùng để minh họa, không giả vờ là tích hợp thật.
- Không hỏi hoặc lưu số thẻ, CVV, mật khẩu ví, OTP. Không tự lưu dữ liệu nhạy cảm vào MySQL/log.
- Xử lý việc bấm thanh toán nhiều lần: nếu booking đã thành công thì không tạo giao dịch thành công thứ hai; service phải kiểm tra trạng thái hiện tại trước khi cập nhật.

#### Vé và lịch sử đặt
- Chỉ phát hành vé sau khi thanh toán thành công. Vé có mã duy nhất, booking, showtime, ghế, phim và thời gian chiếu; QR là phần tùy chọn, có thể thay bằng mã vé dạng text.
- Khách chỉ xem booking/payment/vé thuộc tài khoản của mình; Admin mới được xem dữ liệu tổng hợp toàn hệ thống.
- Trang lịch sử nên hiển thị trạng thái rõ ràng (đang chờ, đã thanh toán, thất bại, hết hạn) và thông tin liên quan; không để khách sửa giá/trạng thái qua request.

#### Thống kê
- Làm phần tối thiểu trước: tổng doanh thu theo tháng và danh sách phim có doanh thu cao.
- Nếu làm theo rạp, lấy rạp từ showtime/room của booking; xác nhận đường liên kết dữ liệu với người 3.
- Chỉ cộng payment `SUCCESS`/booking `CONFIRMED`. Định nghĩa thống nhất “doanh thu” là tiền vé đã thanh toán, không tính booking đang chờ hoặc thất bại.
- Cẩn thận khi join payment với từng ghế: booking nhiều ghế có thể làm một payment bị lặp nhiều dòng và nhân doanh thu. Hãy tổng hợp payment/booking ở mức một giao dịch trước khi nhóm theo phim/rạp/tháng.
- Dùng khoảng ngày rõ ràng và thống nhất timezone; kiểm tra cả tháng không có giao dịch để biểu đồ không lỗi.

#### Gợi ý cấu trúc Java và trang
- `Payment`, `PaymentStatus`, `PaymentMethod`, interface `PaymentStrategy` và một implementation giả lập.
- `PaymentService`: tạo payment attempt, nhận kết quả, cập nhật payment và booking trong transaction; bảo đảm xử lý lặp an toàn.
- `Ticket`/mã vé chỉ tạo khi giao dịch thành công; `DashboardService` thực hiện truy vấn tổng hợp.
- Trang khách: thanh toán, kết quả thanh toán, vé và lịch sử. Trang Admin: bộ lọc tháng/phim/rạp và số liệu tổng hợp.

#### Trình tự thực hiện
1. Cùng người 4 vẽ trạng thái booking/payment và thống nhất thời điểm giữ/trả ghế.
2. Tạo payment giả lập chạy được cho một booking mẫu; test thành công và thất bại.
3. Đảm bảo transaction cập nhật payment, booking và trạng thái ghế nhất quán; không để payment thành công nhưng booking vẫn chờ nếu xử lý cùng một thao tác.
4. Tạo mã vé/lịch sử sau khi thành công; kiểm tra khách không xem được vé người khác.
5. Làm thống kê tháng/phim; sau đó mới bổ sung rạp/biểu đồ nếu còn thời gian.
6. Viết test số tiền, trạng thái, payment lặp, lịch sử và kết quả tổng doanh thu.

#### Tiêu chí hoàn thành
- Booking chưa thanh toán không có vé xác nhận và không được tính doanh thu.
- Thanh toán thành công ghi đúng số tiền/phương thức/thời điểm, booking được xác nhận và vé được tạo.
- Thanh toán thất bại không làm booking thành công; chính sách thử lại/hết hạn hoạt động đúng.
- Bấm lặp không nhân đôi vé hoặc doanh thu; khách không đọc được dữ liệu của người khác.
- Thống kê khớp với dữ liệu demo tính thủ công; booking nhiều ghế không làm doanh thu bị nhân lên.

#### Người 5 không phụ trách
Không tự đổi trạng thái ghế hoặc tính lại giá booking độc lập. Người 5 gọi service/quy trình đã thống nhất với người 4 để xác nhận hoặc hủy booking.

## 5. Kiểm thử, tích hợp và cách làm việc nhóm

- Mỗi thành viên tạo nhánh theo tính năng, commit nhỏ và mở pull request để ít nhất một người khác xem trước khi ghép vào `main`.
- Thống nhất format thời gian, tiền tệ, tên bảng/cột, enum trạng thái và quy tắc đặt tên Java ngay từ đầu.
- Không commit mật khẩu DB, khóa bí mật hoặc dữ liệu cá nhân thật. Dùng `application-example.properties`/biến môi trường làm cấu hình mẫu.
- Có test cho nghiệp vụ chính: đăng nhập sai/đúng và phân quyền; tìm/lọc phim; trùng lịch chiếu; đặt ghế trùng; thanh toán thất bại/thành công.
- Chạy thử từ database rỗng bằng schema/migration và seed data. Mỗi người không chỉ kiểm tra trên máy của mình.
- README cần có: yêu cầu JDK, cách tạo DB, cấu hình kết nối, lệnh chạy, tài khoản demo, chức năng đã làm và giới hạn còn lại.

## 6. Thứ tự triển khai gợi ý

1. **Cả nhóm:** xác nhận rubric/đề bài, chốt stack, vẽ luồng người dùng, schema và giao diện tối thiểu.
2. **Khung project:** dựng ứng dụng Spring Boot kết nối MySQL, thêm layout chung và dữ liệu mẫu; kiểm tra máy tất cả thành viên chạy được.
3. **Làm song song:** người 1 làm xác thực; người 2 làm catalog; người 3 chuẩn bị rạp/phòng; người 4 chốt mô hình ghế/booking với người 3; người 5 thống nhất trạng thái booking/payment với người 4.
4. **Ghép theo luồng:** xem phim → chọn lịch → chọn ghế → tạo booking → thanh toán giả lập → xem vé/lịch sử.
5. **Hoàn thiện:** test quyền và lỗi, seed dữ liệu demo, sửa giao diện, viết README và tập demo/bảo vệ.

Ưu tiên làm một luồng đặt vé tối thiểu chạy xuyên suốt trước khi thêm biểu đồ, email thật, upload ảnh hoặc tích hợp cổng thanh toán.

## 7. Tham khảo giao diện

- Beta Cinemas: https://betacinemas.vn/
- Chỉ tham khảo bố cục và cách trình bày; không sao chép nguyên logo, nội dung, hình ảnh hoặc nhận diện thương hiệu.
