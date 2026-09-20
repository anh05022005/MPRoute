# Tiến độ đồ án

> Trạng thái hiện tại và việc tiếp theo. **Giữ ngắn** — file này được nạp vào mọi phiên làm việc.
> Nhật ký chi tiết từng tuần nằm ở `NHAT-KY.md`, không đưa vào đây.

**Cập nhật lần cuối:** 27/08/2026 · Kho: `github.com/anh05022005/MPRoute` (riêng tư), nhánh `main`

---

## Đang ở đâu

| | |
|---|---|
| Theo kế hoạch | Tuần 10 — báo cáo, thực nghiệm benchmark |
| Thực tế | **XONG TOÀN BỘ 86 CHỨC NĂNG BẮT BUỘC.** Ba báo cáo chạy trên dữ liệu thật và xuất Excel được, benchmark Augerat có số. Còn lại là viết chương 4, chạy thử, quay video và luận văn |

```
[x] T1  Chot de tai, hoi Minh Phu 3 cau
[x] T2  Project chay, 16 bang sinh ra, dang nhap duoc
[x] T3  CRUD 7 danh muc xong + tim toa do Nominatim
[x] T4  Tao don hang, tu tinh kg/m3/tien — xong tron Nhom 2
[x] T5  OSRM chay, ma tran co cache, ban do hien diem
[x] T6  NearestNeighbor chay duoc bang main(), test xanh
[x] T7  2-opt chay duoc, OR-Tools cam vao, bang so sanh 4 dong
[x] T8  Man hinh phan tuyen hoan chinh, ve tuyen len ban do
[x] T9  Man hinh tai xe, tra cuu, su co
[~] T10 RPT-03 xong, benchmark Augerat xong — CON LAI: viet chuong 4
[~] T11 Seed 10 ngay xong — CON LAI: chay thu 3 lan, quay video
[ ] T12 Bao cao hoan chinh, slide, luyen van dap
```

**14/14 test bắt buộc xanh** (`mvnw.cmd test` → **17 test**: 14 bắt buộc + 2 test cho
`VrpFileReader` + 1 lần nạp Spring context).

**`HAVERSINE_FACTOR` đã đo lại: 1.45** (trước để 1.3). Đo trên 264 cặp điểm OSRM thật trong
`distance_matrix` — xem `NHAT-KY.md`. Chỉ ảnh hưởng lúc OSRM chết, nhưng đúng lúc đó thì con số
phải sát nhất có thể.

---

## Việc tiếp theo — TUẦN 10 (phần còn lại) và TUẦN 11

**Hết việc code.** Còn lại là làm báo cáo và chuẩn bị bảo vệ:

1. **Viết chương 4 báo cáo** — số liệu đã có đủ ở `docs/07`: bảng gap 10 bài Augerat, bảng
   riêng đóng góp 2-opt, và RPT-03 trên **14 ngày / 139 đơn** dữ liệu thật
2. **Chạy thử kịch bản demo 3 lần** theo `docs/08`, rồi quay video. Thêm hai bước vào kịch
   bản: mở RPT-01/RPT-02 và bấm **Xuất Excel** một lần
3. Luyện vấn đáp, đánh tag bản chắc chắn chạy được trước ngày bảo vệ
4. Bấm thử bằng chuột thật mấy màn hình còn nợ ở mục dưới

**Seed lại dữ liệu lịch sử** (bật `mysql-8.0.44` và `osrm-vietnam` trước; ngày nào đã có đơn
thì tự bỏ qua nên chạy lại nhiều lần không sao):

```
mvnw.cmd spring-boot:run -Dspring-boot.run.arguments=--mproute.seed-history=true
```

**Cách chạy lại benchmark** (không cần web, MySQL hay OSRM — bấm Run trong IDE là gọn nhất):

```
mvnw.cmd compile
mvnw.cmd dependency:build-classpath -Dmdep.outputFile=cp.txt
java -cp "target/classes;<noi dung cp.txt>" com.minhphu.mproute.service.optimization.BenchmarkRunner
```

Đừng dùng `mvnw.cmd exec:java` — classloader của nó không tìm thấy thư viện native OR-Tools.

---

## Còn nợ nghiệm thu

Chức năng đã viết xong và đã chặn đúng ở tầng service, nhưng **chưa dựng được tình huống để
chạy thử thật**. Làm được lúc nào cũng được, muộn nhất là lúc seed dữ liệu ở tuần 11.

| Việc | Vì sao chưa thử được |
|---|---|
| **CN-03-10 (MSG-08)** tổng nhu cầu > tổng sức chứa | Xe nhỏ nhất đã 500 kg / 6 m³ trong khi các đơn thử chỉ vài trăm kg. Cần một đơn thật to |
| **`approximate_distance` của bảng `routes`** (CN-03-11) | Phải tắt `osrm-vietnam` rồi chạy tối ưu **với tập điểm chưa có trong `distance_matrix`**. Lần thử trước chỉ chứng minh được phần bản đồ, vì ma trận của các điểm đó đã nằm sẵn trong cache |
| **BR07-03** tra SĐT chỉ hiện đơn 30 ngày gần nhất | Đơn cũ nhất mới là 13/08, chưa quá 30 ngày. Seed thêm một ngày thật xa là thử được |
| **CN-04-08 gửi thư thật** | Mở năm dòng SMTP trong `application.properties`, đặt `MPROUTE_MAIL_USERNAME` / `MPROUTE_MAIL_PASSWORD` bằng mật khẩu ứng dụng Gmail, đổi email một khách thành email thật rồi chốt một tuyến. Nhánh ghi `notifications` và nội dung thư đã kiểm xong |
| **BR02-11** đơn `ROUTED` ra đúng chữ MSG-07 | Nay đã có đơn `ROUTED` thật trong CSDL nên thử được ngay |

**Chưa mở file Excel bằng Excel thật.** Ba file `.xlsx` đã kiểm bằng chương trình: đúng
định dạng zip OOXML, đúng tên trang tính, tiếng Việt có dấu không hỏng, số khớp với màn hình.
Nhưng chưa ai bấm nút rồi mở file bằng Microsoft Excel. **Làm luôn khi chạy thử kịch bản demo.**

**Hai màn hình chưa ai bấm thử bằng chuột thật** — phía máy chủ đã kiểm hết, nhưng phần
JavaScript chỉ chạy trong trình duyệt:

- **Bản đồ chọn tọa độ (CN-01-05):** bấm để ghim, kéo ghim, chọn dòng trong danh sách kết quả
- **Form đơn hàng (CN-02-03):** tổng kg/m³/tiền cập nhật khi gõ, thêm/xóa dòng, chọn khách tự
  điền địa chỉ. Thử luôn khối nhập lý do hủy
- **Kéo thả chỉnh tuyến (CN-04-01, CN-04-02)** và **màn hình tài xế trên khung 360px**

---

## Cần quyết định

| Việc | Tình hình |
|---|---|
| **Mật khẩu lệch tài liệu** | `dieuphoi` và `taixe01` đã bị đổi khỏi giá trị của `DataSeeder`. `seedUser()` chỉ tạo khi thiếu chứ không ghi đè nên khởi động lại không tự về mặc định. **Trước ngày bảo vệ phải đưa về đúng `docs/08`, hoặc sửa `docs/08`** — kịch bản demo đọc mật khẩu từ đó, lệch là hỏng ngay bước đăng nhập đầu tiên. `admin`, `taixe02`, `taixe03` vẫn đúng mặc định |

---

## Đã biết trước, cố ý chưa sửa

| Vấn đề | Vì sao chưa sửa |
|---|---|
| NN để sót đơn dù tổng sức chứa đủ | **Cố ý giữ.** Là điểm nhấn demo bước 5a và câu trả lời vấn đáp mạnh nhất. Kịch bản ở `docs/08` |
| Trạng thái xe là cờ toàn cục, không theo ngày | Sửa đúng phải đổi CBR-05 và tầng service. Đã ghi vào hạn chế `docs/07` 4.7 |
| Thuật toán không ràng buộc khung giờ, dù có khách ghi "chỉ nhận buổi sáng trước 11h" | Giữ vì phản ánh khảo sát thật. **Đã có số trả lời:** tuyến 3 điểm tới điểm cuối lúc **09:09**, còn sớm hơn mốc 11h khá nhiều — với quy mô hiện tại của Minh Phú thì ràng buộc này chưa bị vi phạm |
| **Thu thiếu tiền mà đơn vẫn sang `PAID`** | `docs/01` chỉ có BR05-14 cho đơn công nợ, **không có luật nào cho việc thu thiếu**. Tự nghĩ ra luật là thêm phạm vi ngoài `docs/04`. Số thực thu vẫn ở `route_stops.collected_amount`, lý do ở `collection_note`, điều phối đã nhận NTF-09 — không mất dữ liệu, chỉ là cột `payment_status` chưa phản ánh. **Muốn sửa thì thêm một luật vào `docs/01` trước** |

---

## Nợ kỹ thuật và bẫy của máy này

**Phải bật trước khi chạy ứng dụng** — cả hai đều không phải service Windows, máy khởi động lại
là tắt hết. Docker Desktop cũng phải chạy trước:

| Container | Việc | Mở lại |
|---|---|---|
| `mysql-8.0.44` | CSDL. Tắt là app báo `Communications link failure` và **không khởi động được** | `docker start mysql-8.0.44` |
| `osrm-vietnam` | Khoảng cách đường thật, cổng 5000, ~2,4 GB RAM | `docker start osrm-vietnam` |

Quên bật `osrm-vietnam` thì **app vẫn chạy**, chỉ lùi về Haversine và hiện MSG-09 — đúng thiết kế,
nhưng số km là xấp xỉ, **đừng lấy số đó đưa vào báo cáo**.

- **`ddl-auto=update` KHÔNG cập nhật ràng buộc `CHECK` khi thêm giá trị enum.** Hibernate sinh
  `CHECK (cột IN (...))` lúc *tạo bảng* rồi không bao giờ động vào nữa, nên thêm một hằng số enum
  là câu `INSERT` sau đó chết với `Check constraint ... is violated` mà không nhắc gì tới enum.
  Đã sửa tay `routes` và `optimization_results` cho `OR_TOOLS`. **Lần sau thêm giá trị enum bất kỳ
  thì phải `ALTER` tay**, hoặc xóa bảng cho Hibernate tạo lại
- **Tiếng Việt hỏng khi gõ qua dòng lệnh, cả hai hướng.** `curl` trong Git Bash: phải
  **percent-encode sẵn** rồi dùng `--data-raw`, nếu không Git Bash chuyển sang ANSI làm hỏng dấu
  ngay từ máy khách — rất dễ tưởng nhầm ứng dụng sai encoding. `docker exec ... mysql -e "…"` gọi
  từ PowerShell hỏng kiểu khác: chữ bị mã hóa **hai lần**. Muốn sửa dữ liệu có dấu bằng SQL thì
  dùng `UNHEX('…')`. Kiểm bằng `HEX(cột)` trong MySQL
- **OpenStreetMap ở Hà Nội hầu như không có dữ liệu số nhà.** "Tìm tọa độ" chỉ đưa về đúng *con
  đường*. Đây là giới hạn dữ liệu OSM, không sửa được từ phía đồ án. Quy trình đúng: tìm để nhảy
  tới đúng phố → **bấm tay lên bản đồ**. Chính là điều BR01-06 đã lường trước
- **Ảnh nền bản đồ vẫn cần mạng.** Leaflet đã tải về `static/` nhưng các ô ảnh lấy từ
  `tile.openstreetmap.org`. Bảo vệ mà mất mạng thì khung bản đồ xám — ghim và tọa độ **vẫn chạy**,
  chỉ mất phần nhìn. Nên chuẩn bị sẵn ảnh chụp màn hình
- `application.properties` đọc mật khẩu MySQL từ `MPROUTE_DB_PASSWORD`, mặc định `gbao` cho máy
  này. **Chuỗi `gbao` vẫn còn trong lịch sử git** ở commit đầu `c55b2d3` — kho đang riêng tư nên
  không sao, nhưng đổi sang công khai thì phải xóa khỏi lịch sử trước
- Máy này lưu thông tin đăng nhập GitHub của **hai tài khoản**. Remote cố ý ghi là
  `https://anh05022005@github.com/...` — xóa phần `anh05022005@` đi là đẩy bằng tài khoản kia,
  báo lỗi 403
- Sau khi đăng nhập địa chỉ có đuôi `?continue` — hành vi mặc định của Spring Security 6, vô hại

### Dữ liệu thử đang có trong CSDL

Toàn bộ là dữ liệu do các lần kiểm thử tạo ra, **xóa lúc seed lại ở tuần 11**. Giữ đến lúc đó vì
mỗi thứ đang phục vụ một phép thử:

- **131 đơn lịch sử của 10 ngày 13/08–25/08** do `HistorySeeder` sinh, đã giao xong. Đây là
  nguồn dữ liệu của RPT-03, **giữ lại đến ngày bảo vệ**
- 15 đơn thử rải từ 16/08 đến 28/08, đủ mọi trạng thái kể cả `FAILED` và `POSTPONED`
- Ngày 27/08: tuyến `TT26082702` **đang chạy**, có đơn `DELIVERING` để thử `/track`; tuyến
  `TT26082701` đã đóng sau khi xử lý sự cố
- 2 sự cố đã xử lý: một mức nhẹ (tự lùi ETA 30 phút), một xe hỏng (áp dụng phương án chèn tuyến)
- Ba khách thử của CN-02-10 (`#22`, `#23`, `#24`) đã chuyển `INACTIVE`
- Xe 29H-123.45 và 29C-678.90 đang `MAINTENANCE`, 29F-555.66 đang `IN_USE`

---

**Nhật ký chi tiết từng tuần — đã làm gì, vì sao làm thế, số liệu chạy thật:** xem `NHAT-KY.md`.
