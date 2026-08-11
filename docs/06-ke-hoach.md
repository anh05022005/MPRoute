# 06 — Kế hoạch 12 tuần

**Bối cảnh:** một người làm, 12 tuần, vừa code vừa viết báo cáo.

## Nguyên tắc bất di bất dịch

1. **Tuần 6–7 dành cho thuật toán. Không được lùi.** Nếu tuần 5 mà CRUD chưa xong thì cắt bớt CRUD, tuyệt đối không lùi lịch thuật toán.
2. **Viết báo cáo song song.** Xong phần nào viết phần đó. Dồn về cuối là không kịp.
3. **Tuần 11 phải là bản chạy được, không phải bản hoàn hảo.** Thiếu chức năng phụ thì để vào "Hướng phát triển".

---

## Tổng quan

| Tuần | Việc chính | Chương báo cáo viết song song |
|---|---|---|
| 1 | Chốt đề tài, khảo sát, use case | Chương 1 — Tổng quan |
| 2 | Dựng project, entity, đăng nhập | Chương 2 — Cơ sở lý thuyết |
| 3 | CRUD danh mục | Chương 3 — Phân tích thiết kế |
| 4 | Đơn hàng | Chương 3 |
| 5 | OSRM, Leaflet, ma trận khoảng cách | Chương 2 (phần thuật toán) |
| **6** | **Thuật toán giai đoạn 1 — Nearest Neighbor** | Chương 2 |
| **7** | **Thuật toán giai đoạn 2 — 2-opt + chỉ số** | Chương 2 |
| 8 | Gắn thuật toán vào web | Chương 3 |
| 9 | Giao diện tài xế, tra cứu, sự cố | Chương 3 |
| 10 | Báo cáo thống kê + thực nghiệm benchmark | **Chương 4 — Thực nghiệm** |
| 11 | Seed dữ liệu, chạy thử, quay video | Chương 5 — Kết luận |
| 12 | Hoàn thiện báo cáo, slide, luyện vấn đáp | Rà soát toàn bộ |

---

## TUẦN 1 — Chuẩn bị

| Việc | Kết quả cần có |
|---|---|
| Chốt đề tài với giảng viên hướng dẫn | Đề cương được duyệt |
| **Hỏi Minh Phú: quy mô thật** | Số khách/ngày, số xe, có gom đơn không |
| **Hỏi Minh Phú: một xe mấy chuyến/ngày** | Xác nhận giả định mỗi xe một chuyến |
| **Hỏi Minh Phú: khách có ràng buộc giờ nhận không** | Quyết định có cần cảnh báo giờ |
| Vẽ sơ đồ use case | Hình đưa vào chương 3 |
| Vẽ sơ đồ hoạt động cho UC03 | Hình đưa vào chương 3 |
| Cài JDK 21, IntelliJ, MySQL | `java -version` hiện 21 |

Ba câu hỏi in đậm quan trọng nhất — chúng quyết định mô hình toán học. Nếu không liên hệ được Minh Phú thì ghi rõ trong báo cáo là dữ liệu giả lập.

---

## TUẦN 2 — Nền tảng

| Việc | Chức năng | DoD |
|---|---|---|
| Tạo project trên start.spring.io | CN-00-01 | Chạy được, `/` trả về trang chào |
| Cấu hình MySQL | CN-00-01 | Log hiện câu SQL |
| Tạo 18 enum | — | Đủ theo `02-co-so-du-lieu.md` |
| Tạo entity theo thứ tự 1→9 | CN-00-02 | `SHOW TABLES` ra đủ 16 bảng |
| Tạo repository cho mỗi entity | CN-00-02 | |
| Spring Security + đăng nhập | CN-00-03 | Sai 5 lần bị khóa 15 phút |
| Phân quyền theo vai trò | CN-00-04 | Gõ URL trái quyền → 403 |
| Layout Thymeleaf dùng chung | CN-00-06 | Menu đổi theo vai trò |

**Cảnh báo:** thêm Spring Security vào là mọi trang bị khóa ngay, hiện form đăng nhập mặc định với mật khẩu in ở Console. Đó là bình thường. Nếu thấy vướng khi làm CRUD thì tạm gỡ dependency, thêm lại sau.

---

## TUẦN 3 — CRUD danh mục

| Việc | Chức năng |
|---|---|
| Sản phẩm — có kiểm tra kg và m³ > 0 | CN-01-01 |
| Phương tiện — có kiểm tra tải trọng và thể tích | CN-01-02 |
| Tài xế | CN-01-03 |
| Khách hàng | CN-01-04 |
| Kho | CN-01-06 |
| Tài khoản | CN-01-07 |
| Tham số hệ thống | CN-01-08 |
| Đổi mật khẩu | CN-00-05 |

Làm một màn hình mẫu (sản phẩm) thật kỹ rồi nhân bản cho các màn hình còn lại. Đừng làm song song cả 7 màn hình.

---

## TUẦN 4 — Đơn hàng

| Việc | Chức năng |
|---|---|
| Danh sách đơn hàng có lọc | CN-02-01 |
| Tạo đơn, chọn khách tự điền | CN-02-02 |
| Tự tính kg, m³, tiền bằng JavaScript | CN-02-03 |
| Sinh mã đơn tự động | CN-02-04 |
| Chặn đơn vượt sức chứa xe lớn nhất | CN-02-05 |
| Khối tóm tắt đọc lại cho khách | CN-02-06 |
| Duyệt đơn, sửa đơn, hủy đơn | CN-02-07, 08, 09 |
| Thêm khách ngay trong màn tạo đơn | CN-02-10 |

Phần thanh toán Mức 1 làm luôn ở đây: `hinhThucThanhToan`, `donGia`, `soTienPhaiThu`, `trangThaiThanhToan`.

---

## TUẦN 5 — Bản đồ và khoảng cách

| Việc | Chức năng | Ghi chú |
|---|---|---|
| Cài Docker, tải bản đồ, dựng OSRM | — | Làm ngay đầu tuần, dễ tốn cả buổi |
| Tích hợp Nominatim tìm tọa độ | CN-01-05 | |
| Ghim tay trên bản đồ | CN-01-05 | |
| Viết `DistanceCalculator` interface | — | |
| Viết `HaversineDistanceCalculator` | — | Làm trước, không cần OSRM |
| Viết `OsrmDistanceCalculator` | — | |
| Viết `DistanceMatrixService` có cache | CN-03-05 | **Quan trọng nhất tuần này** |
| Hiển thị điểm giao lên Leaflet | CN-03-02 | |

### Lệnh dựng OSRM

```bash
# Tai ban do Viet Nam (~250MB)
wget https://download.geofabrik.de/asia/vietnam-latest.osm.pbf

# Tien xu ly - can khoang 6-8GB RAM, mat 10-30 phut
docker run -t -v "${PWD}:/data" osrm/osrm-backend osrm-extract -p /opt/car.lua /data/vietnam-latest.osm.pbf
docker run -t -v "${PWD}:/data" osrm/osrm-backend osrm-partition /data/vietnam-latest.osrm
docker run -t -v "${PWD}:/data" osrm/osrm-backend osrm-customize /data/vietnam-latest.osrm

# Chay server (lan sau chi can lenh nay)
docker run -t -i -p 5000:5000 -v "${PWD}:/data" osrm/osrm-backend osrm-routed --algorithm mld /data/vietnam-latest.osrm
```

Máy dưới 8GB RAM thì cắt riêng vùng Hà Nội bằng `osmium extract` trước.

---

## TUẦN 6 — Thuật toán giai đoạn 1 ⚠ TUẦN QUAN TRỌNG

**Không gắn web. Chỉ viết class thuần Java và chạy bằng `main()`.**

| Ngày | Việc |
|---|---|
| 1 | Chạy tay thuật toán trên giấy với 5 điểm. Vẽ ra, tự tính Nearest Neighbor chọn điểm nào trước |
| 2 | Viết `OptimizationPoint`, `OptimizationVehicle`, `OptimizationRoute`, `RoutingResult` |
| 3 | Viết `NearestNeighborSolver` theo mã giả ở `03-thuat-toan.md` |
| 4 | Viết `AlgorithmRunner` có `main()`, dữ liệu cứng 10 điểm 3 xe. In kết quả ra console |
| 5 | Viết `calculateTotalDistance` — **kiểm kỹ chặng về kho** |
| 6 | Viết unit test: không vượt tải trọng, không vượt thể tích, không sót điểm |
| 7 | Giải thích lại toàn bộ code cho chính mình bằng tiếng Việt. Không giải thích được dòng nào thì viết lại dòng đó |

**Không sang tuần 7 khi chưa chạy được `main()` và in ra tuyến hợp lệ.**

---

## TUẦN 7 — Thuật toán giai đoạn 2 ⚠ TUẦN QUAN TRỌNG

| Ngày | Việc |
|---|---|
| 1 | Chạy tay 2-opt trên giấy. Vẽ tuyến 5 điểm, thử đảo một đoạn, tự tính độ lệch |
| 2 | Viết `TwoOptImprover` |
| 3 | Viết `NN2OptSolver` kết hợp hai giai đoạn |
| 4 | Viết `MetricsService`: mốc A, mốc B, mức giảm, hiệu suất xe |
| 5 | Viết hàm tính ETA |
| 6 | Unit test: 2-opt không làm tăng quãng đường, không đổi tập điểm |
| 7 | Chạy thử với 30 điểm, so sánh NN và NN+2-opt, ghi lại con số |

Cuối tuần 7 phải trả lời được ba câu:
1. Nearest Neighbor hoạt động thế nào? (giải thích từng bước)
2. 2-opt hoạt động thế nào? Công thức độ lệch từ đâu ra?
3. Vì sao 2-opt không phá vỡ ràng buộc sức chứa?

---

## TUẦN 8 — Gắn thuật toán vào web

| Việc | Chức năng |
|---|---|
| `RoutingService` gọi solver | CN-03-06 |
| Màn hình phân tuyến: chọn ngày, chọn xe, chọn thuật toán | CN-03-01, 03, 04 |
| Hiển thị kết quả từng tuyến | CN-03-07 |
| Bảng chỉ số so sánh | CN-03-08 |
| Vẽ tuyến lên bản đồ theo lộ trình OSRM | CN-03-09 |
| Xử lý thiếu sức chứa | CN-03-10 |
| Dự phòng OSRM lỗi | CN-03-11 |
| Lưu vào `optimization_results` | CN-03-13 |
| Điều chỉnh thủ công, gán tài xế, chốt kế hoạch | CN-04-01 → 07 |

---

## TUẦN 9 — Tài xế, tra cứu, sự cố

| Việc | Chức năng |
|---|---|
| Màn hình tài xế responsive | CN-05-01 → 06 |
| Nhập tiền thu, giao thất bại, hoãn | CN-05-07, 08, 09 |
| Tự hoàn thành tuyến | CN-05-10 |
| Trang tra cứu công khai | CN-07-01 → 06 |
| Gửi email ETA sau khi chốt tuyến | CN-04-08 |
| Báo sự cố, sinh phương án, áp dụng | CN-06-01 → 13 |

Nếu đuối, cắt toàn bộ nhóm sự cố (CN-06) và đưa vào Hướng phát triển.

---

## TUẦN 10 — Báo cáo và thực nghiệm

| Việc | Chức năng |
|---|---|
| RPT-03 Báo cáo hiệu quả tối ưu | CN-08-03 |
| RPT-01, RPT-02 | CN-08-01, 02 |
| Xuất Excel | CN-08-05 |
| **Tải bộ Augerat, viết hàm đọc file .vrp** | CN-09-02 |
| **Chạy benchmark, lập bảng gap** | CN-09-02 |
| **Bảng so sánh NN và NN+2-opt** | CN-09-03 |
| **Viết chương 4 báo cáo** | — |

Chi tiết cách chạy thực nghiệm ở `07-thuc-nghiem.md`.

---

## TUẦN 11 — Chuẩn bị demo

| Việc | Kết quả |
|---|---|
| Viết script seed dữ liệu mẫu | 15 khách, 4 xe, 3 tài xế, 30 đơn 3 ngày |
| Chạy thử toàn bộ luồng lần 1 | Ghi lại lỗi |
| Sửa lỗi | |
| Chạy thử lần 2, lần 3 | Không lỗi |
| **Quay video demo 5–7 phút** | File mp4 dự phòng |
| Viết bảng thao tác demo ra giấy | Thứ tự bấm gì trước gì sau |
| Cài Cloudflare Tunnel (nếu muốn dùng điện thoại thật) | Một lệnh, không bắt buộc |

**Video demo quan trọng hơn mọi thứ khác trong tuần này.** Máy chết, MySQL không lên, IntelliJ lỗi — có video là vẫn bảo vệ được.

---

## TUẦN 12 — Hoàn thiện

| Việc |
|---|
| Rà soát toàn bộ báo cáo, thống nhất định dạng |
| Kiểm tra mục lục, đánh số hình, đánh số bảng |
| Làm slide (15–20 slide) |
| Luyện trả lời ba câu hỏi ở `07-thuc-nghiem.md` mục "Câu hỏi hội đồng" |
| Chạy thử demo thêm 2 lần |
| Nộp |

---

## Bảng theo dõi tiến độ

Đánh dấu khi hoàn thành:

```
[ ] T1  Chot de tai, hoi Minh Phu 3 cau
[ ] T2  Project chay, 16 bang sinh ra, dang nhap duoc
[ ] T3  CRUD 7 danh muc xong
[ ] T4  Tao don hang, tu tinh kg/m3/tien
[ ] T5  OSRM chay, ma tran co cache, ban do hien diem
[ ] T6  NearestNeighbor chay duoc bang main(), test xanh
[ ] T7  2-opt chay duoc, co so sanh NN vs NN+2opt
[ ] T8  Man hinh phan tuyen hoan chinh, ve tuyen len ban do
[ ] T9  Man hinh tai xe, tra cuu, su co
[ ] T10 RPT-03 xong, benchmark Augerat xong, chuong 4 viet xong
[ ] T11 Seed du lieu, chay thu 3 lan, quay video
[ ] T12 Bao cao hoan chinh, slide, luyen van dap
```

## Điểm kiểm tra sống còn

| Thời điểm | Nếu chưa đạt thì làm gì |
|---|---|
| Cuối tuần 5 | CRUD chưa xong → **cắt bớt CRUD**, vẫn sang tuần 6 đúng hạn |
| Cuối tuần 7 | Thuật toán chưa chạy → **dừng mọi thứ khác**, dồn toàn bộ tuần 8 cho thuật toán |
| Cuối tuần 9 | Đuối → **cắt toàn bộ nhóm sự cố CN-06** |
| Cuối tuần 10 | Chương 4 chưa có số liệu → **ưu tiên benchmark Augerat**, bỏ báo cáo RPT-01, RPT-02 |
