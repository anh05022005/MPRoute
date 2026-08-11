# 00 — Tổng quan dự án

## Doanh nghiệp

**Công ty Minh Phú** — doanh nghiệp nhỏ, sản xuất và phân phối hàng tiêu dùng tại Hà Nội.

- Kho và văn phòng: đường Phan Trọng Tuệ, Thanh Trì, Hà Nội
- Mặt hàng: bông tẩy trang, khăn rửa mặt dùng một lần, nước giặt, nước lau sàn, nước rửa tay
- Khách hàng: tạp hóa, đại lý, siêu thị nhỏ trong nội thành Hà Nội
- Quy mô đội xe: 3–5 xe

**Mô hình kinh doanh: doanh nghiệp tự giao hàng.** Minh Phú giao hàng của chính mình, không phải công ty vận chuyển thuê ngoài. Vì vậy hệ thống **không có** vai trò "khách gửi hàng" như các nền tảng 3PL.

---

## Quy trình hiện tại (AS-IS)

| Bước | Ai làm | Mô tả |
|---|---|---|
| 1 | Nhân viên sản xuất | Sản xuất, dán nhãn, đóng thùng, lưu kho |
| 2 | Khách hàng | Gọi điện đặt hàng, ví dụ "cần 220 thùng bông tẩy trang" |
| 3 | Điều phối viên | Ghi đơn ra sổ hoặc Excel rời rạc |
| 4 | Điều phối viên | Chọn xe đang trống theo kinh nghiệm, chất hàng |
| 5 | Tài xế | Chở đi giao, tự quyết thứ tự nếu có nhiều điểm |
| 6 | Điều phối viên | Xe hỏng giữa đường thì gọi khách báo hàng tới muộn |
| 7 | Tài xế | Giao xong về kho, báo miệng |

### Vấn đề

1. **Tốn quãng đường** — mỗi đơn thường một chuyến riêng, dù nhiều khách ở gần nhau
2. **Thứ tự giao theo cảm tính** — tài xế tự quyết, không tối ưu
3. **Chọn xe theo kinh nghiệm** — dễ quá tải hoặc chở thiếu so với sức chứa
4. **Hứa giờ giao không có căn cứ**
5. **Sự cố chỉ biết xin lỗi** — không có phương án thay thế
6. **Không có dữ liệu lịch sử** để đánh giá hiệu quả

---

## Hệ thống giải quyết thế nào

| Vấn đề | Cách giải quyết |
|---|---|
| Tốn quãng đường | Gom nhiều đơn vào một chuyến bằng thuật toán CVRP |
| Thứ tự cảm tính | Thuật toán sắp thứ tự tối thiểu hóa tổng quãng đường |
| Quá tải / chở thiếu | Ràng buộc cứng về khối lượng (kg) và thể tích (m³) |
| Hứa giờ cảm tính | Tính ETA từ khoảng cách thật, tốc độ và thời gian bốc dỡ |
| Sự cố | Sinh phương án thay thế xếp hạng theo km phát sinh, tự thông báo khách |
| Không có dữ liệu | Lưu chỉ số so sánh mỗi lần chạy, sinh báo cáo theo kỳ |

---

## Trong phạm vi

- Quản lý danh mục: sản phẩm, xe, tài xế, khách hàng, kho
- Tiếp nhận đơn hàng qua điện thoại, nhập vào hệ thống
- Phân tuyến tự động có ràng buộc khối lượng và thể tích
- Điều chỉnh tuyến thủ công và gán tài xế
- Giao diện tài xế trên điện thoại (web responsive)
- Báo và xử lý sự cố trên đường
- Tra cứu đơn hàng cho khách (không đăng nhập)
- Báo cáo thống kê và báo cáo hiệu quả tối ưu
- Ghi nhận thanh toán ở mức cơ bản (xem mục dưới)

## Ngoài phạm vi

Ghi rõ vào báo cáo để hội đồng không hiểu nhầm là thiếu sót:

- Quản lý sản xuất, tồn kho nguyên vật liệu
- Kế toán, hóa đơn VAT, sổ quỹ, đối soát tiền tài xế nộp về
- Quản lý công nợ chi tiết (hạn nợ, cảnh báo quá hạn, phiếu thu)
- Cổng đặt hàng trực tuyến cho khách — khách vẫn gọi điện
- Tích hợp SMS hoặc Zalo — chỉ gửi email
- Ứng dụng di động native — tài xế dùng trình duyệt điện thoại
- Nhiều chuyến mỗi xe mỗi ngày (multi-trip) — giả định mỗi xe một chuyến
- Khung giờ nhận hàng bắt buộc (VRPTW) — chỉ lưu ghi chú, thuật toán không ràng buộc
- Triển khai lên máy chủ thật — chạy localhost

---

## Mức thanh toán được chọn: Mức 1 — chỉ ghi nhận

Hệ thống **ghi nhận trạng thái thanh toán**, không quản lý dòng tiền.

Ba hình thức, gắn cố định với từng khách hàng trong danh mục, cho phép ghi đè theo từng đơn:

| Hình thức | Số tiền phải thu | Tài xế làm gì |
|---|---|---|
| `CREDIT` | 0đ | Giao xong, không thu tiền. Đơn chuyển trạng thái "Ghi nợ" |
| `CASH` | = tổng tiền hàng | Thu tiền, nhập số tiền thực thu khi bấm "Đã giao" |
| `PREPAID` | 0đ | Giao xong, không thu tiền |

Việc nộp tiền về công ty, đòi công nợ, đối chiếu sổ sách diễn ra **ngoài hệ thống**, y như hiện tại.

---

## Bốn tác nhân

| Tác nhân | Đăng nhập | Thiết bị | Số use case |
|---|---|---|---|
| Quản trị viên | Có | Máy tính | UC01, UC08 |
| Điều phối viên | Có | Máy tính | UC02, UC03, UC04, UC06 |
| Tài xế | Có | Điện thoại | UC05, UC06 |
| Khách hàng | Không | Bất kỳ | UC07 |

**Khách hàng đặt hàng qua điện thoại, không qua hệ thống.** Đây là chủ ý thiết kế: khách là chủ tạp hóa, quen gọi điện để vừa đặt vừa hỏi giá, hỏi hàng còn không. Bắt họ đăng nhập web sẽ khiến hệ thống không ai dùng.

---

## Tám use case

| Mã | Tên | Ưu tiên |
|---|---|---|
| UC01 | Quản lý danh mục dữ liệu | Cao |
| UC02 | Tiếp nhận và tạo đơn hàng | Cao |
| **UC03** | **Phân tuyến giao hàng tự động** | **Bắt buộc — không được cắt** |
| UC04 | Điều chỉnh tuyến thủ công và gán tài xế | Cao |
| UC05 | Thực hiện giao hàng | Cao |
| UC06 | Báo và xử lý sự cố trên đường | Trung bình |
| UC07 | Tra cứu đơn hàng | Trung bình |
| **UC08** | **Báo cáo hiệu quả tối ưu** | **Bắt buộc — không được cắt** |

UC03 và UC08 là phần làm nên giá trị học thuật. Mọi thứ khác chỉ hỗ trợ để hai phần đó chạy được và có dữ liệu.

---

## Thứ tự cắt bỏ nếu thiếu thời gian

Cắt theo thứ tự này, từ an toàn nhất:

1. Thống kê sự cố, hiệu suất tài xế (báo cáo phụ)
2. Đánh giá của khách hàng
3. Chụp ảnh xác nhận giao hàng
4. Gửi vị trí GPS định kỳ của tài xế
5. Nhập đơn hàng từ Excel
6. In phiếu giao hàng PDF
7. Toàn bộ UC06 — xử lý sự cố

**Không bao giờ cắt UC03 và UC08.**
