# 04 — Danh sách chức năng

Mỗi chức năng có **tiêu chí hoàn thành (DoD)** — làm xong là kiểm được ngay, không mơ hồ.

Ký hiệu: **★ bắt buộc** — **○ tùy chọn, cắt được**

---

## Nhóm 0 — Nền tảng

### ★ CN-00-01 Dựng project
`GET /` trả về trang chào, ứng dụng khởi động không lỗi, kết nối MySQL thành công, log hiện câu SQL.

### ★ CN-00-02 Tạo toàn bộ entity và repository
Chạy ứng dụng, MySQL tự sinh đủ 16 bảng theo `02-co-so-du-lieu.md`. Kiểm bằng `SHOW TABLES`.

### ★ CN-00-03 Đăng nhập, đăng xuất
- Sai tài khoản → MSG-12, không nói rõ sai gì
- Đúng → chuyển về trang chính theo vai trò
- Sai 5 lần → khóa 15 phút
- Mật khẩu trong DB là chuỗi băm BCrypt, không phải văn bản thuần

### ★ CN-00-04 Phân quyền
Đăng nhập vai trò `DRIVER` rồi gõ thẳng URL `/admin/products` → trả về trang 403, **không phải trang trắng hay lỗi 500**.

### ★ CN-00-05 Đổi mật khẩu
Nhập sai mật khẩu cũ → báo lỗi. Đúng → đổi thành công, đăng nhập lại bằng mật khẩu mới được.

### ★ CN-00-06 Bố cục chung
Có `layout.html` dùng chung: thanh tiêu đề, menu theo vai trò, vùng nội dung, vùng hiện thông báo. Mọi trang khác kế thừa từ đây.

---

## Nhóm 1 — Quản trị viên (UC01)

### ★ CN-01-01 Quản lý sản phẩm
Thêm, sửa, tìm kiếm, phân trang. Bắt buộc nhập `weight_per_box` và `volume_per_box` > 0 — để trống hoặc nhập 0 thì báo lỗi ngay dưới trường.

### ★ CN-01-02 Quản lý phương tiện
Thêm, sửa, đổi trạng thái. Bắt buộc `max_weight` và `max_volume` > 0. Không cho chọn tay trạng thái `IN_USE`.

### ★ CN-01-03 Quản lý tài xế
Thêm, sửa, gán xe mặc định. Số điện thoại không đúng 10 chữ số bắt đầu bằng 0 → báo lỗi.

### ★ CN-01-04 Quản lý khách hàng
Thêm, sửa, chọn hình thức thanh toán, nhập ghi chú giao hàng.

### ★ CN-01-05 Xác định tọa độ khách hàng
Nhập địa chỉ → bấm "Tìm tọa độ" → Nominatim trả kết quả → hiện ghim trên bản đồ. Không tìm được → bấm được vào bản đồ để ghim tay, tọa độ tự điền vào hai ô.

### ★ CN-01-06 Quản lý kho
Sửa được tên, địa chỉ, tọa độ. Có sẵn một kho Phan Trọng Tuệ từ dữ liệu mẫu.

### ★ CN-01-07 Quản lý tài khoản
Thêm tài khoản, gán vai trò, khóa/mở khóa. Không có nút xóa cứng.

### ★ CN-01-08 Cấu hình tham số hệ thống
Sửa được 9 tham số trong `02-co-so-du-lieu.md`. **Đổi `AVERAGE_SPEED` rồi chạy lại phân tuyến thì ETA phải đổi theo** — đây là cách kiểm chứng tham số thực sự được dùng.

### ○ CN-01-09 Nhật ký hoạt động
Bảng xem log, lọc theo người dùng và khoảng thời gian.

---

## Nhóm 2 — Đơn hàng (UC02)

### ★ CN-02-01 Danh sách đơn hàng
Lọc theo ngày giao, trạng thái, khách hàng. Phân trang.

### ★ CN-02-02 Tạo đơn hàng
Chọn khách → tự điền địa chỉ, tọa độ, SĐT, hình thức thanh toán. Thêm nhiều dòng chi tiết.

### ★ CN-02-03 Tự tính khối lượng, thể tích, tiền hàng
Nhập xong một dòng → tổng kg, m³, tiền hàng cập nhật **ngay không cần tải lại trang** (dùng JavaScript). Ba ô này ở chế độ chỉ đọc.

### ★ CN-02-04 Sinh mã đơn tự động
Đúng định dạng `MP` + yyMMdd + 4 số. Tạo 3 đơn cùng ngày → 3 mã liên tiếp, không trùng.

### ★ CN-02-05 Chặn đơn vượt sức chứa
Tạo đơn nặng hơn xe lớn nhất → MSG-06 kèm số liệu cụ thể, không cho lưu.

### ★ CN-02-06 Khối tóm tắt để đọc lại cho khách
Sau khi lưu, hiện khối gồm: mã đơn, khách, danh sách sản phẩm, ngày giao, hình thức thanh toán, số tiền phải thu. Có nút sao chép.

### ★ CN-02-07 Duyệt đơn
Bấm "Duyệt đơn" → trạng thái sang `APPROVED`, đơn xuất hiện trong danh sách chờ phân tuyến.

### ★ CN-02-08 Sửa đơn
Chỉ sửa được khi `PENDING_APPROVAL` hoặc `APPROVED`. Đơn `ROUTED` → nút Sửa bị vô hiệu hóa và MSG-07 khi cố truy cập bằng URL.

### ★ CN-02-09 Hủy đơn
Bắt buộc nhập lý do. Đơn không biến mất khỏi DB, chỉ chuyển `CANCELLED`.

### ★ CN-02-10 Thêm khách hàng ngay trong màn hình tạo đơn
Mở hộp thoại, nhập thông tin, tìm tọa độ, lưu xong tự chọn luôn cho đơn đang tạo.

### ○ CN-02-11 Nhập đơn từ Excel
Tải file mẫu, tải lên, xem trước dòng hợp lệ và dòng lỗi, xác nhận tạo hàng loạt.

---

## Nhóm 3 — Phân tuyến (UC03) — PHẦN LÕI

### ★ CN-03-01 Màn hình phân tuyến
Chọn ngày → hiện danh sách đơn `APPROVED`, tổng số đơn, tổng kg, tổng m³.

### ★ CN-03-02 Hiển thị điểm giao lên bản đồ
Leaflet nền OpenStreetMap. Kho là ô vuông đậm, khách là ghim. Rê chuột hiện tên khách và số thùng.

### ★ CN-03-03 Chọn xe khả dụng
Danh sách checkbox các xe `AVAILABLE` kèm tải trọng, thể tích. Hiện tổng sức chứa đã chọn để đối chiếu tổng nhu cầu.

### ★ CN-03-04 Chọn thuật toán
Dropdown: `Nearest Neighbor`, `Nearest Neighbor + 2-opt` (mặc định). **Chức năng này để demo so sánh trước hội đồng, bắt buộc có.**

### ★ CN-03-05 Chuẩn bị ma trận khoảng cách
Gọi `DistanceMatrixService`. Chạy lần hai với cùng tập điểm → không gọi OSRM nữa (kiểm bằng log), chứng tỏ cache hoạt động.

### ★ CN-03-06 Chạy thuật toán
Bấm nút → sinh các tuyến. Kiểm: **không tuyến nào vượt tải trọng hoặc thể tích**, mọi đơn đều được gán (nếu đủ sức chứa).

### ★ CN-03-07 Hiển thị kết quả từng tuyến
Mỗi tuyến một bảng: biển số, tổng km, kg/tải trọng, m³/thể tích, và các dòng gồm thứ tự, khách, số thùng, km chặng, ETA.

### ★ CN-03-08 Bảng chỉ số so sánh
Hiện: quãng đường mốc A, mốc B, sau tối ưu, mức giảm theo cả hai mốc, số chuyến tiết kiệm, thời gian thực thi (ms).

### ★ CN-03-09 Vẽ tuyến lên bản đồ
Mỗi tuyến một màu theo bảng màu ở `05-giao-dien.md`. **Đường đi bám theo lộ trình thật từ OSRM, không phải đường thẳng nối các điểm.**

### ★ CN-03-10 Xử lý thiếu sức chứa
Chọn ít xe → MSG-08 nêu rõ phần thiếu, không sinh tuyến nào.

### ★ CN-03-14 Hiển thị đơn không gán được
Tổng sức chứa **đủ** nhưng thuật toán vẫn không xếp hết đơn (bài toán xếp thùng, xem `08-du-lieu-mau.md`) → hiện khối cảnh báo riêng liệt kê: mã đơn, khách, kg, m³ của từng đơn chưa gán, kèm gợi ý chọn thêm xe.

Các tuyến đã sinh **vẫn hiển thị bình thường**, không được xóa đi. Nút "Chốt kế hoạch" bị vô hiệu hóa khi còn đơn chưa gán.

Phân biệt rõ với CN-03-10: CN-03-10 là *tổng sức chứa thiếu* → không sinh tuyến nào. CN-03-14 là *tổng đủ nhưng chia không được* → có tuyến nhưng còn đơn sót.

### ★ CN-03-11 Dự phòng khi OSRM lỗi
Tắt container OSRM rồi chạy → hệ thống vẫn ra kết quả bằng Haversine, hiện MSG-09, tuyến được đánh dấu `approximate_distance = true`.

### ★ CN-03-12 Chạy lại xóa kết quả cũ
Chạy hai lần cùng ngày → chỉ còn một bộ tuyến `DRAFT`, không nhân đôi.

### ★ CN-03-13 Lưu chỉ số vào `optimization_results`
Sau mỗi lần chạy có một bản ghi mới. Đây là nguồn dữ liệu cho báo cáo RPT-03.

---

## Nhóm 4 — Điều chỉnh và gán tài xế (UC04)

### ★ CN-04-01 Đổi thứ tự điểm trong tuyến
Kéo thả. Tổng km và ETA tính lại ngay.

### ★ CN-04-02 Chuyển đơn giữa hai tuyến
Kéo thả giữa hai bảng. Cả hai tuyến tính lại kg, m³, km, ETA.

### ★ CN-04-03 Cảnh báo vượt sức chứa
Kéo đơn nặng sang xe nhỏ → tiêu đề tuyến đỏ, MSG-14, nút "Gán tài xế" và "Chốt kế hoạch" bị vô hiệu hóa.

### ★ CN-04-04 So sánh tự động và chỉnh tay
Hiện song song hai con số quãng đường.

### ★ CN-04-05 Khôi phục kết quả tự động
Bấm nút → về đúng kết quả gốc của thuật toán.

### ★ CN-04-06 Gán tài xế
Hộp thoại chọn tài xế cho từng tuyến. Tài xế đã chọn không xuất hiện ở tuyến khác cùng ngày.

### ★ CN-04-07 Chốt kế hoạch
Xác nhận → tuyến `CONFIRMED`, đơn `ROUTED`, xe `IN_USE`, tài xế nhận thông báo.

### ★ CN-04-08 Gửi email ETA cho khách
Sau khi chốt, khách có email nhận được thư gồm mã đơn, ETA, link tra cứu. Khách không có email → ghi thông báo trạng thái `NOT_SENT`.

### ★ CN-04-09 Hủy tuyến
Tuyến `CONFIRMED` hủy được, đơn về `APPROVED`, xe về `AVAILABLE`. Tuyến `IN_PROGRESS` → MSG-10.

### ○ CN-04-10 Xuất kết quả phân tuyến ra Excel

### ★ CN-04-11 Màn hình theo dõi tiến độ giao (SCR-10)
`/tracking`. Chọn ngày → xem từng tuyến đã chốt: biển số, tài xế, tổng km, giờ xuất phát, tỷ lệ
điểm đã xử lý. Mỗi điểm dừng hiện ETA, giờ đến thực tế, tiền phải thu, trạng thái và lý do thất
bại nếu có.

Tuyến `DRAFT` và `CANCELLED` **không hiện** — màn hình này theo dõi việc đang chạy, bản nháp chưa
chốt thì chưa ai giao.

Đặt ở nhóm 4 vì tác nhân là điều phối, dù nội dung phản ánh hành động của tài xế ở nhóm 5.

**Bổ sung ngày 20/09/2026.** `docs/05` có SCR-10 và mục "Theo dõi" trong menu điều phối ngay từ
đầu, nhưng `docs/04` thiếu mục tương ứng nên màn hình không bao giờ được làm — bấm vào menu ra
404. Xem `docs/09` mục C.

---

## Nhóm 5 — Tài xế (UC05)

### ★ CN-05-01 Xem tuyến hôm nay
Đăng nhập trên điện thoại → hiện tuyến của mình. Tài xế khác đăng nhập → thấy tuyến khác, không thấy tuyến của người kia.

### ★ CN-05-02 Xác nhận nhận hàng
Bấm nút → tuyến `IN_PROGRESS`, ghi `departed_at`. Trước khi bấm thì không cập nhật được điểm giao nào.

### ★ CN-05-03 Danh sách điểm giao theo thứ tự
Điểm đang xử lý nổi bật, điểm đã xong mờ đi, điểm chưa tới hiện rút gọn.

### ★ CN-05-04 Chi tiết điểm giao
Địa chỉ, người liên hệ, SĐT, danh sách sản phẩm và số thùng, ghi chú giao hàng, ETA, **số tiền phải thu**.

### ★ CN-05-05 Nút gọi và chỉ đường
`tel:` mở ứng dụng gọi. Chỉ đường mở Google Maps với tọa độ điểm giao.

### ★ CN-05-06 Cập nhật đã giao
Bấm → xác nhận → điểm sang `DELIVERED`, ghi thời điểm và tọa độ GPS. Điểm tiếp theo tự nổi bật.

### ★ CN-05-07 Nhập tiền thu
Đơn có `amount_to_collect > 0` → hộp thoại hiện sẵn số tiền, tài xế xác nhận hoặc sửa. Sửa khác → bắt nhập lý do, gửi thông báo cho điều phối.

### ★ CN-05-08 Giao thất bại
Chọn lý do từ danh sách. Chọn `KHAC` → bắt nhập mô tả. Điều phối nhận thông báo.

### ★ CN-05-09 Hoãn giao
Chọn ngày mới → đơn sang `POSTPONED` rồi tự tạo lại ở ngày mới với trạng thái `APPROVED`.

### ★ CN-05-10 Tự hoàn thành tuyến
Điểm cuối cập nhật xong → tuyến `COMPLETED`, xe `AVAILABLE`, hiện màn hình tổng kết.

### ★ CN-05-11 Lịch sử tuyến đã chạy
Danh sách các tuyến ngày trước, số đơn thành công.

### ○ CN-05-12 Chụp ảnh xác nhận giao hàng
### ○ CN-05-13 Gửi vị trí GPS định kỳ

---

## Nhóm 6 — Sự cố (UC06)

### ★ CN-06-01 Nút báo sự cố
Đặt cố định cuối màn hình tài xế, viền đỏ, luôn bấm được khi tuyến `IN_PROGRESS`.

### ★ CN-06-02 Biểu mẫu báo sự cố
Chọn loại, mức độ, mô tả. `CAN_CONTINUE` → hiện thêm ô nhập số phút chậm trễ.

### ★ CN-06-03 Lấy tọa độ GPS
Tự lấy từ trình duyệt. Từ chối quyền → dùng tọa độ điểm giao gần nhất đã xong, đánh dấu `estimated_location = true`.

### ★ CN-06-04 Sự cố mức nhẹ
`CAN_CONTINUE` → cộng phút chậm trễ vào ETA các điểm còn lại, gửi email khách, không sinh phương án.

### ★ CN-06-05 Thông báo khẩn cho điều phối
Điều phối nhận thông báo kèm loại sự cố, vị trí, danh sách điểm bị ảnh hưởng.

### ★ CN-06-06 Màn hình xử lý sự cố
Hiện thông tin sự cố, vị trí xe trên bản đồ, danh sách điểm chưa giao.

### ★ CN-06-07 Sinh phương án A — xe cứu hộ
Tìm xe `AVAILABLE` đủ sức chứa. Tính km phát sinh = kho → điểm sự cố + lộ trình còn lại.

### ★ CN-06-08 Sinh phương án B — chèn vào tuyến khác
Dùng Cheapest Insertion. Kiểm sức chứa còn lại của tuyến ứng viên. Không đủ → loại khỏi danh sách.

### ★ CN-06-09 Sinh phương án C — hoãn
Luôn hiện, luôn xếp cuối.

### ★ CN-06-10 Xếp hạng phương án
Sắp theo km phát sinh tăng dần (trừ C luôn cuối). Mỗi phương án hiện: km phát sinh, ETA mới của từng khách, nguồn lực cần.

### ★ CN-06-11 Áp dụng phương án
Chọn → xác nhận → cập nhật tuyến liên quan, tính lại ETA, ghi lịch sử.

### ★ CN-06-12 Thông báo khách bị ảnh hưởng
Gửi email ETA mới cho từng khách.

### ★ CN-06-13 Xe hỏng tự chuyển bảo trì
Sự cố `CANNOT_CONTINUE` → xe sang `MAINTENANCE`, biến mất khỏi danh sách xe khả dụng.

### ○ CN-06-14 Xử lý thủ công
Điều phối tự chuyển từng đơn nếu không phương án nào phù hợp.

---

## Nhóm 7 — Khách hàng (UC07)

### ★ CN-07-01 Trang tra cứu công khai
`/track` truy cập được khi chưa đăng nhập.

### ★ CN-07-02 Tra bằng mã đơn hoặc SĐT
SĐT → hiện các đơn 30 ngày gần nhất.

### ★ CN-07-03 Hiển thị thông tin đơn
Mã đơn, khách, địa chỉ, số thùng, ngày giao, ETA, trạng thái. **Không hiện giá trị đơn hàng, biển số xe, khách khác cùng tuyến.**

### ★ CN-07-04 Hành trình đơn hàng
Bảng theo thứ tự thời gian, lấy từ `status_history`.

### ★ CN-07-05 Thông tin tài xế
Chỉ hiện tên và SĐT tài xế khi đơn đang `DELIVERING`.

### ★ CN-07-06 Không tìm thấy
MSG-11, không nói rõ nguyên nhân.

### ★ CN-07-07 Giới hạn tần suất
Quá 20 lần/phút cùng IP → chặn tạm.

### ○ CN-07-08 Đánh giá sau khi nhận hàng

---

## Nhóm 8 — Báo cáo (UC08)

### ★ CN-08-01 RPT-01 Thống kê đơn hàng
Số đơn theo ngày/tuần/tháng, số thùng theo sản phẩm. Biểu đồ cột.

### ★ CN-08-02 RPT-02 Tỷ lệ giao thành công
Thành công / thất bại / hoãn. Phân tích thất bại theo lý do. Biểu đồ tròn.

### ★ CN-08-03 RPT-03 Báo cáo hiệu quả tối ưu
**Báo cáo quan trọng nhất.** Bảng theo ngày gồm: số đơn, số tuyến, mốc A, mốc B, sau tối ưu, mức giảm so với A, mức giảm so với B, số chuyến tiết kiệm. Dòng trung bình toàn kỳ. Biểu đồ đường.

### ★ CN-08-04 Loại trừ dữ liệu không hợp lệ
Tuyến `CANCELLED` và tuyến `approximate_distance = true` không tính vào trung bình. Tuyến xấp xỉ hiện riêng có ghi chú.

### ★ CN-08-05 Xuất Excel
Mọi báo cáo có nút xuất, file chứa dữ liệu chi tiết theo bộ lọc hiện tại.

### ○ CN-08-06 RPT-04 Hiệu suất phương tiện
### ○ CN-08-07 RPT-05 Hiệu suất tài xế
### ○ CN-08-08 RPT-06 Thống kê sự cố

---

## Nhóm 9 — Thực nghiệm (cho chương 4 báo cáo)

### ★ CN-09-01 Chương trình chạy thuật toán độc lập
Class có `main()`, chạy được không cần khởi động web.

### ★ CN-09-02 Đọc bộ dữ liệu chuẩn Augerat
Đọc file định dạng `.vrp`, chạy thuật toán, in kết quả và gap so với lời giải tối ưu đã công bố.

### ★ CN-09-03 Bảng so sánh thuật toán
Chạy cùng dữ liệu qua `NN` và `NN_2OPT`, xuất bảng: quãng đường, mức cải thiện, thời gian chạy.

### ○ CN-09-04 Chạy thử nhiều lần lấy trung bình

---

## Tổng kết

| Nhóm | Bắt buộc | Tùy chọn |
|---|---|---|
| 0 — Nền tảng | 6 | 0 |
| 1 — Quản trị | 8 | 1 |
| 2 — Đơn hàng | 10 | 1 |
| 3 — Phân tuyến | 14 | 0 |
| 4 — Điều chỉnh | 9 | 1 |
| 5 — Tài xế | 11 | 2 |
| 6 — Sự cố | 13 | 1 |
| 7 — Khách hàng | 7 | 1 |
| 8 — Báo cáo | 5 | 3 |
| 9 — Thực nghiệm | 3 | 1 |
| **Tổng** | **86** | **11** |

## Thứ tự cắt nếu thiếu thời gian

1. Toàn bộ mục ○
2. Nhóm 8: CN-08-01, CN-08-02 (giữ CN-08-03)
3. Nhóm 6 toàn bộ (sự cố)
4. Nhóm 7: CN-07-04, CN-07-05

**Tuyệt đối không cắt Nhóm 3, CN-08-03, và Nhóm 9.**
