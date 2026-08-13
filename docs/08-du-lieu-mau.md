# 08 — Dữ liệu mẫu

Dữ liệu này dùng để demo. **Phải seed sẵn, tuyệt đối không nhập tay trước mặt hội đồng.**

Cách nạp: viết class `DataSeeder` implements `CommandLineRunner` trong package `config`, kiểm tra nếu bảng rỗng thì mới nạp.

```java
@Component
public class DataSeeder implements CommandLineRunner {
    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return; // da co du lieu roi, khong nap lai
        }
        seedUsers();
        seedDepot();
        seedProducts();
        // ...
    }
}
```

---

## Tài khoản

| Tên đăng nhập | Mật khẩu | Vai trò | Họ tên |
|---|---|---|---|
| `admin` | `admin123` | ADMIN | Nguyễn Thị Lan |
| `dieuphoi` | `dieuphoi123` | DISPATCHER | Trần Văn Minh |
| `taixe01` | `taixe123` | DRIVER | Nguyễn Văn Hùng |
| `taixe02` | `taixe123` | DRIVER | Lê Văn Nam |
| `taixe03` | `taixe123` | DRIVER | Phạm Văn Tú |

Mật khẩu phải băm BCrypt khi seed, không lưu văn bản thuần.

Tài xế đánh số hai chữ số — `taixe01`, không phải dạng một chữ số — để khi sắp xếp theo tên đăng nhập
thì thứ tự vẫn đúng nếu về sau có quá 9 tài xế: sắp theo chữ cái thì `taixe10` đứng trước `taixe9`.

**Họ tên tài khoản phải trùng với họ tên trong danh mục tài xế.** Màn hình tài xế lấy tên từ
tài khoản đăng nhập, còn màn hình phân tuyến lấy tên từ hồ sơ tài xế — lệch nhau thì lúc demo
hai màn hình hiện hai tên khác nhau cho cùng một người.

---

## Kho

| Tên | Địa chỉ | Vĩ độ | Kinh độ |
|---|---|---|---|
| Kho Minh Phú | Đường Phan Trọng Tuệ, Thanh Trì, Hà Nội | 20.9497 | 105.8172 |

---

## Sản phẩm

| Mã | Tên | Đơn vị | kg/thùng | m³/thùng | Đơn giá |
|---|---|---|---|---|---|
| SP001 | Bông tẩy trang | Thùng | 6.0 | 0.085 | 480.000 |
| SP002 | Khăn rửa mặt dùng 1 lần | Thùng | 5.5 | 0.072 | 520.000 |
| SP003 | Nước giặt 3.5L | Thùng | 21.5 | 0.028 | 720.000 |
| SP004 | Nước lau sàn 2L | Thùng | 24.8 | 0.031 | 380.000 |
| SP005 | Nước rửa tay 500ml | Thùng | 14.2 | 0.024 | 450.000 |

**Chú ý sự tương phản:** bông tẩy trang 6 kg nhưng 0.085 m³ (nhẹ, cồng kềnh); nước lau sàn 24.8 kg nhưng 0.031 m³ (nặng, gọn). Đây là dữ liệu để chứng minh vì sao cần hai ràng buộc — khi demo nhớ chỉ vào chỗ này.

---

## Phương tiện

| Biển số | Loại xe | Tải trọng (kg) | Thể tích (m³) | Trạng thái |
|---|---|---|---|---|
| 29H-123.45 | Xe tải 1.5 tấn | 1500 | 12.0 | AVAILABLE |
| 29C-678.90 | Xe tải 500kg | 500 | 6.0 | AVAILABLE |
| 29F-555.66 | Xe tải 1 tấn | 1000 | 8.0 | AVAILABLE |
| 29B-222.11 | Xe tải 800kg | 800 | 7.0 | MAINTENANCE |

Để một xe ở trạng thái `MAINTENANCE` để demo được việc hệ thống loại xe không sẵn sàng ra khỏi danh sách.

---

## Tài xế

| Họ tên | Tài khoản | SĐT | Xe mặc định |
|---|---|---|---|
| Nguyễn Văn Hùng | taixe01 | 0987654321 | 29H-123.45 |
| Lê Văn Nam | taixe02 | 0987654322 | 29C-678.90 |
| Phạm Văn Tú | taixe03 | 0987654323 | 29F-555.66 |

---

## Khách hàng

Tọa độ dưới đây là các vị trí có thật trong nội thành Hà Nội, cách kho Phan Trọng Tuệ từ 5 đến 15 km — đủ rải rác để thuật toán có việc làm.

| # | Tên | Người liên hệ | SĐT | Địa chỉ | Vĩ độ | Kinh độ | Thanh toán |
|---|---|---|---|---|---|---|---|
| 1 | Tạp hóa Minh Anh | Ngô Thị Minh | 0912345001 | 24 Láng Hạ, Đống Đa | 21.0148 | 105.8125 | CREDIT |
| 2 | Siêu thị Hồng Hà | Trần Thị Bình | 0912345002 | 8 Thái Hà, Đống Đa | 21.0106 | 105.8215 | CREDIT |
| 3 | Cửa hàng Bảo Châu | Lê Văn Bảo | 0912345003 | 91 Chùa Bộc, Đống Đa | 21.0067 | 105.8290 | CASH |
| 4 | Đại lý Thanh Tâm | Phạm Thanh Tâm | 0912345004 | 12 Trần Duy Hưng, Cầu Giấy | 21.0102 | 105.7963 | PREPAID |
| 5 | Tạp hóa Đức Lợi | Vũ Đức Lợi | 0912345005 | 55 Nguyễn Trãi, Thanh Xuân | 20.9955 | 105.8055 | CASH |
| 6 | Cửa hàng Kim Ngân | Đỗ Kim Ngân | 0912345006 | 7 Lê Văn Lương, Thanh Xuân | 21.0025 | 105.8010 | CREDIT |
| 7 | Siêu thị An Phát | Hoàng Văn An | 0912345007 | 145 Giải Phóng, Hai Bà Trưng | 20.9895 | 105.8412 | CREDIT |
| 8 | Tạp hóa Bình Minh | Nguyễn Thị Hoa | 0912345008 | 30 Trương Định, Hoàng Mai | 20.9820 | 105.8450 | CASH |
| 9 | Đại lý Phú Quý | Trần Văn Quý | 0912345009 | 88 Nguyễn Xiển, Thanh Xuân | 20.9835 | 105.8005 | CREDIT |
| 10 | Cửa hàng Hải Yến | Lý Hải Yến | 0912345010 | 15 Kim Giang, Thanh Xuân | 20.9760 | 105.8130 | CASH |
| 11 | Tạp hóa Thu Hà | Bùi Thu Hà | 0912345011 | 60 Định Công, Hoàng Mai | 20.9788 | 105.8320 | CREDIT |
| 12 | Siêu thị Tân Việt | Đặng Tân | 0912345012 | 22 Ngọc Hồi, Thanh Trì | 20.9420 | 105.8425 | CREDIT |
| 13 | Cửa hàng Vạn Xuân | Ngô Văn Xuân | 0912345013 | 5 Cầu Bươu, Thanh Trì | 20.9585 | 105.7955 | CASH |
| 14 | Đại lý Hồng Phúc | Phan Hồng Phúc | 0912345014 | 40 Tựu Liệt, Thanh Trì | 20.9520 | 105.8280 | CREDIT |
| 15 | Tạp hóa Mai Lan | Trịnh Mai Lan | 0912345015 | 18 Linh Đàm, Hoàng Mai | 20.9668 | 105.8280 | CASH |

Ghi chú giao hàng đặt cho vài khách để demo trường được dùng:

| Khách | Ghi chú |
|---|---|
| Siêu thị Hồng Hà | Chỉ nhận hàng buổi sáng trước 11h |
| Cửa hàng Vạn Xuân | Ngõ nhỏ, xe tải trên 1 tấn không vào được |
| Tạp hóa Mai Lan | Gọi trước 15 phút khi tới |

Email: đặt cho 5 khách đầu để demo chức năng gửi thông báo ETA. Dùng email thật của bạn để kiểm tra được.

---

## Đơn hàng mẫu

Tạo đơn cho **ba ngày**: hôm qua (đã hoàn thành), hôm nay (đang giao), ngày mai (chờ phân tuyến).

### Ngày mai — trạng thái `APPROVED`, dùng để demo phân tuyến

Đây là bộ dữ liệu chính khi demo. Thiết kế sao cho:
- Đủ đơn để thuật toán có việc làm (8 đơn)
- Tổng nhu cầu vừa đủ cho 2–3 xe, không thừa quá
- Có ít nhất một tuyến bị giới hạn bởi **khối lượng**, một tuyến bị giới hạn bởi **thể tích**

| # | Khách | Sản phẩm | Số thùng | kg | m³ |
|---|---|---|---|---|---|
| 1 | Tạp hóa Minh Anh | SP001 Bông tẩy trang | 50 | 300.0 | 4.250 |
| 2 | Siêu thị Hồng Hà | SP003 ×30, SP002 ×15 | 45 | 727.5 | 1.920 |
| 3 | Cửa hàng Bảo Châu | SP004 Nước lau sàn | 15 | 372.0 | 0.465 |
| 4 | Đại lý Thanh Tâm | SP001 Bông tẩy trang | 40 | 240.0 | 3.400 |
| 5 | Tạp hóa Đức Lợi | SP002 Khăn rửa mặt | 20 | 110.0 | 1.440 |
| 6 | Cửa hàng Kim Ngân | SP003 Nước giặt | 5 | 107.5 | 0.140 |
| 7 | Siêu thị An Phát | SP005 Nước rửa tay | 25 | 355.0 | 0.600 |
| 8 | Tạp hóa Bình Minh | SP001 ×30, SP004 ×10 | 40 | 428.0 | 2.860 |

**Tổng: 2640.0 kg và 15.075 m³**

### Bộ dữ liệu này cho ba giai đoạn demo — đã chạy thử và kiểm chứng

| Chọn xe | Sức chứa | Kết quả thật khi chạy Nearest Neighbor |
|---|---|---|
| 29H + 29C | 2000 kg / 18 m³ | **Thiếu tổng** → MSG-08, không sinh tuyến nào |
| 29H + 29C + 29F | 3000 kg / 26 m³ | **Tổng đủ nhưng vẫn sót 1 đơn** — 2 tuyến, đơn Hồng Hà (727.5 kg) không gán được |
| Thêm 29B (bỏ bảo trì) | 3800 kg / 33 m³ | Gán đủ 8/8 đơn, 3 tuyến |

**Giai đoạn 2 không phải lỗi — đây là điểm nhấn mạnh nhất của buổi bảo vệ.**

Kết quả thật khi chạy NN với 3 xe:

```
29H-123.45   4 diem: Binh Minh -> An Phat -> Bao Chau -> Minh Anh   1455.0/1500 kg
29F-555.66   3 diem: Duc Loi -> Kim Ngan -> Thanh Tam                457.5/1000 kg
KHONG GAN DUOC: Hong Ha (727.5 kg)
```

Vì sao: tổng sức chứa 3000 kg > tổng nhu cầu 2640 kg nên qua được kiểm tra BR03-04. Nhưng **phân đơn cho xe là bài toán xếp thùng**, tổng đủ không có nghĩa là chia được. Nearest Neighbor tham lam theo khoảng cách, nhồi xe 29H tới 1455/1500 kg rồi chừa lại 45 kg vô dụng. Đơn Hồng Hà 727.5 kg còn lại không lọt vào xe 29C (500 kg).

Vét cạn 6561 tổ hợp cho thấy **có 129 cách chia hợp lệ** — NN không tìm ra cách nào trong số đó.

Câu để nói trước hội đồng:

> Ở đây tổng sức chứa đã đủ nhưng thuật toán vẫn không gán hết được một đơn. Đó là hạn chế thật của Nearest Neighbor: nó chọn tốt nhất ở mỗi bước theo khoảng cách mà không nhìn xa, nên việc phân đơn cho xe — vốn là bài toán xếp thùng — không được giải tối ưu. Em kiểm chứng bằng cách vét cạn thì thấy có 129 cách chia hợp lệ, thuật toán không tìm ra cách nào. Vì vậy hệ thống không im lặng bỏ sót đơn mà báo rõ cho điều phối viên biết đơn nào chưa xếp được để chủ động bổ sung xe.

Trả lời được như vậy chứng minh bạn hiểu thuật toán mình viết chứ không chỉ chạy được nó. **Đừng sửa dữ liệu để giấu tình huống này đi.**

### Hôm nay — trạng thái `DELIVERING`, dùng để demo màn hình tài xế và sự cố

Tạo 1 tuyến đã chốt, gán cho `taixe01`, có 3 điểm giao trong đó:
- Điểm 1: đã `DELIVERED`
- Điểm 2: đang `DELIVERING` — **có `amount_to_collect > 0` để demo nhập tiền**
- Điểm 3: `PENDING`

Tạo thêm 1 tuyến cho `taixe02` cũng `IN_PROGRESS` với 2 điểm chưa giao, để demo được **Phương án B — chèn vào tuyến khác** khi xử lý sự cố.

### Hôm qua và các ngày trước — trạng thái `COMPLETED`

Tạo dữ liệu cho **ít nhất 10 ngày trước**, **mỗi ngày 12–15 đơn**, kèm bản ghi trong `optimization_results`. Đây là nguồn dữ liệu cho báo cáo RPT-03 — không có thì báo cáo hiệu quả tối ưu sẽ trống trơn khi demo.

Thêm vài đơn `FAILED` với các lý do khác nhau để báo cáo RPT-02 có dữ liệu.

#### Vì sao phải 12–15 đơn/ngày chứ không phải 5–8

Đây là chỗ dễ hỏng nhất của chương 4, và hỏng thì tới tuần 10 mới phát hiện.

**Mốc B chỉ đo được đóng góp của việc sắp thứ tự.** Mà với ma trận đối xứng, một tuyến chỉ có **2 điểm** thì hai thứ tự dài **bằng nhau tuyệt đối**:

```
KHO -> A -> B -> KHO  =  d(K,A) + d(A,B) + d(B,K)
KHO -> B -> A -> KHO  =  d(K,B) + d(B,A) + d(A,K)
```

Hai vế bằng nhau vì `d(A,B) = d(B,A)`. Nghĩa là **tuyến 2 điểm cho mốc B đúng 0%, và 2-opt cũng đúng 0%** — không phải do code sai.

Với 5–8 đơn chia cho 3 xe, tuyến trung bình chỉ 2–3 điểm. Cột *"Giảm so với mốc B"* trong RPT-03 sẽ gần như toàn 0, trong khi `07-thuc-nghiem.md` lại dựa vào chính mốc B để chứng minh giá trị của thuật toán.

Với 12–15 đơn chia cho 3 xe, tuyến có 4–5 điểm, lúc đó thứ tự giao mới thực sự có chỗ để tạo ra chênh lệch.

**Đây cũng là một câu trả lời tốt khi vấn đáp:** *"quy mô bài toán phải đủ lớn thì việc tối ưu thứ tự mới có ý nghĩa; với tuyến hai điểm thì mọi thuật toán đều cho cùng kết quả."*

---

## Tham số hệ thống

| Mã | Giá trị |
|---|---|
| AVERAGE_SPEED | 30 |
| LOADING_TIME | 15 |
| DEPARTURE_TIME | 08:00 |
| HAVERSINE_FACTOR | 1.3 |
| DEFAULT_ALGORITHM | NN_2OPT |
| OSRM_URL | http://localhost:5000 |
| OSRM_TIMEOUT | 10 |
| PAGE_SIZE | 20 |
| LOOKUP_RATE_LIMIT | 20 |

---

## Kịch bản demo trước hội đồng

Chuẩn bị theo đúng thứ tự này, tập ít nhất 3 lần. **In ra giấy cầm theo.**

| Bước | Thao tác | Điểm cần nhấn mạnh |
|---|---|---|
| 1 | Đăng nhập `admin`, mở danh mục sản phẩm | Chỉ vào cột kg và m³, giải thích vì sao cần cả hai |
| 2 | Mở danh mục xe | Chỉ xe đang bảo trì, nói sẽ bị loại khỏi phân tuyến |
| 3 | Đăng nhập `dieuphoi`, mở danh sách đơn ngày mai | 8 đơn đã duyệt, tổng kg và m³ |
| 4 | Mở màn hình phân tuyến, chọn 2 xe, bấm chạy | **Hiện MSG-08 báo thiếu sức chứa** |
| 5a | Thêm xe 29F, chạy lại | **Tổng đủ nhưng vẫn sót đơn Hồng Hà — giải thích hạn chế của thuật toán tham lam.** Điểm nhấn, xem mục trên |
| 5b | Bỏ xe 29B khỏi bảo trì, chọn thêm, chạy lại | Ra 3 tuyến đủ 8 đơn, bản đồ vẽ đường |
| 6 | Chỉ vào bảng chỉ số | **Mốc A, mốc B, mức giảm — giải thích hai mốc** |
| 7 | Đổi thuật toán sang chỉ NN, chạy lại | So sánh hai con số, chứng minh 2-opt có tác dụng |
| 8 | Đổi lại NN+2-opt, kéo thả một đơn sang xe nhỏ | **Hiện cảnh báo đỏ vượt tải, nút chốt bị khóa** |
| 9 | Khôi phục kết quả tự động, gán tài xế, chốt | Email gửi đi |
| 10 | Mở tab mới, đăng nhập `taixe01` trên chế độ giả lập điện thoại | Giao diện mobile, một điểm nổi bật |
| 11 | Bấm "Đã giao" ở điểm có tiền mặt | Hộp thoại nhập tiền |
| 12 | Bấm "Báo sự cố", chọn xe hỏng, không thể tiếp tục | |
| 13 | Quay lại tab điều phối, mở xử lý sự cố | **Ba phương án kèm km phát sinh — đây là điểm nhấn** |
| 14 | Chọn phương án B, áp dụng | ETA cập nhật, email gửi khách |
| 15 | Mở trang tra cứu, nhập mã đơn | Khách thấy hành trình và ETA mới |
| 16 | Đăng nhập `admin`, mở báo cáo RPT-03 | **Bảng 10 ngày, mức giảm trung bình** |

Tổng thời gian: 8–10 phút. Bốn bước in đậm là phần quan trọng nhất — nếu bị giục thời gian thì giữ bằng được **bước 5a, 6, 13, 16**.

---

## Checklist trước ngày bảo vệ

```
[ ] MySQL khoi dong duoc, du lieu mau day du
[ ] Container OSRM chay duoc
[ ] Bang distance_matrix da co du lieu cache (chay thu truoc de nap)
[ ] Ung dung chay khong loi tu lenh dau tien
[ ] Da chay tron ven kich ban demo 3 lan
[ ] Video demo 5-7 phut da quay xong
[ ] Bang thao tac demo da in ra giay
[ ] Laptop sac day, mang du phong (phat 4G tu dien thoai)
[ ] Trinh duyet da mo san cac tab can thiet
[ ] Da xoa cac tab rieng tu khoi trinh duyet
```

**Bảng `distance_matrix` phải có sẵn dữ liệu cache trước khi demo.** Nếu OSRM không lên được hôm đó, hệ thống vẫn chạy bình thường bằng dữ liệu đã lưu. Đây là bảo hiểm quan trọng nhất.
