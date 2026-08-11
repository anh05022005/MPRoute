# 05 — Giao diện

## Triết lý thiết kế

Đây là **phần mềm nội bộ dùng hằng ngày**, không phải trang giới thiệu sản phẩm.

- Bảng kẻ viền đầy đủ, dòng sát nhau — ưu tiên xem nhiều dữ liệu trên một màn hình
- Chỉ một màu nhấn duy nhất cho hành động chính
- Số liệu quan trọng để dạng bảng, không dùng thẻ lớn màu mè
- Không gradient, không bo góc lớn, không icon màu, không hiệu ứng động

Không dành thời gian làm đẹp. Bootstrap 5 mặc định là đủ.

---

## Bảng màu

Khai báo một lần trong `src/main/resources/static/css/style.css`:

```css
:root {
  /* Mau chinh - xanh duong */
  --xanh-900: #0B2C4A;   /* thanh tieu de tren cung */
  --xanh-700: #14568C;   /* thanh menu */
  --xanh-500: #1E77BF;   /* tab dang chon, nut chinh */
  --xanh-100: #DCEBF7;   /* nen nhat, dong duoc chon */
  --xanh-50:  #F0F6FB;   /* nen khoi thong tin */

  /* Trung tinh */
  --chu-chinh: #1C2430;
  --chu-phu:   #5A6674;
  --chu-mo:    #8B97A5;
  --vien:      #DDE3E9;
  --nen-bang:  #F2F5F8;
  --nen-soc:   #FAFBFC;  /* ke soc dong bang */

  /* Trang thai */
  --thanh-cong: #1B8A5A;
  --canh-bao:   #B7791F;
  --loi:        #C0392B;
  --cho-xu-ly:  #5A6674;

  /* Nen nhat cua trang thai */
  --nen-thanh-cong: #E3F4EC;
  --nen-canh-bao:   #FCF2E0;
  --nen-loi:        #FBE9E7;
  --nen-cho:        #EDF0F3;

  /* Mau phan biet tuyen tren ban do */
  --tuyen-1: #1E77BF;
  --tuyen-2: #C75B39;
  --tuyen-3: #1B8A5A;
  --tuyen-4: #7D5BA6;
  --tuyen-5: #B7791F;
}
```

### Ghi đè Bootstrap

```css
.btn-primary {
  background-color: var(--xanh-500);
  border-color: var(--xanh-500);
}
.btn-primary:hover {
  background-color: var(--xanh-700);
  border-color: var(--xanh-700);
}
.table > thead {
  background-color: var(--nen-bang);
}
.table-striped > tbody > tr:nth-of-type(odd) {
  background-color: var(--nen-soc);
}
```

### Quy tắc dùng màu

| Quy tắc | Lý do |
|---|---|
| Xanh 900 chỉ dùng cho thanh tiêu đề trên cùng | Tạo lớp lang |
| Xanh 700 cho thanh menu, xanh 500 cho tab đang chọn và nút chính | |
| **Mỗi màn hình chỉ một nút xanh đặc.** Nút còn lại nền trắng viền xám | Ba nút cùng xanh thì không biết bấm cái nào |
| Màu trạng thái chỉ dùng cho trạng thái, không trang trí | |
| Bảng dữ liệu giữ trắng và xám, kẻ sọc bằng `--nen-soc` | |

Năm màu phân biệt tuyến đã chọn cùng độ đậm nhưng khác độ sáng, để người bị mù màu đỏ–xanh lá vẫn phân biệt được.

---

## Bố cục chung

```
┌──────────────────────────────────────────────────┐
│ Minh Phu — Quan ly giao hang     user | Dang xuat│  xanh-900
├──────────────────────────────────────────────────┤
│ Phan tuyen | Don hang | Theo doi | Su co | Bao cao│  xanh-700
├──────────────────────────────────────────────────┤
│ [thanh cong cu / bo loc]                          │  trang
├──────────────────────────────────────────────────┤
│                                                   │
│              [noi dung chinh]                     │
│                                                   │
└──────────────────────────────────────────────────┘
```

Menu hiển thị theo vai trò:

| Vai trò | Menu |
|---|---|
| Quản trị | Danh mục, Tài khoản, Tham số, Báo cáo |
| Điều phối | Đơn hàng, Phân tuyến, Theo dõi, Sự cố, Báo cáo |
| Tài xế | (không có menu, chỉ một màn hình duy nhất) |

---

## Danh sách màn hình

| Mã | Đường dẫn | Vai trò |
|---|---|---|
| SCR-01 | `/login` | Công khai |
| SCR-02 | `/admin/products` | Quản trị |
| SCR-03 | `/admin/vehicles` | Quản trị |
| SCR-04 | `/admin/customers` | Quản trị |
| SCR-05 | `/admin/drivers` | Quản trị |
| SCR-06 | `/admin/settings` | Quản trị |
| SCR-07 | `/orders` | Điều phối |
| SCR-08 | `/orders/create` | Điều phối |
| SCR-09 | `/routing` | Điều phối |
| SCR-10 | `/tracking` | Điều phối |
| SCR-11 | `/incidents/{id}` | Điều phối |
| SCR-12 | `/driver/my-route` | Tài xế |
| SCR-13 | `/track` | Công khai |
| SCR-14 | `/reports` | Quản trị, Điều phối |

---

## SCR-09 — Màn hình phân tuyến (quan trọng nhất)

```
┌────────────────────────────────────────────────────────────┐
│ Ngay giao [04/08/2026]  Thuat toan [NN + 2-opt ▾]          │
│                                        [ Chay toi uu ]      │
├──────────────────────┬─────────────────────────────────────┤
│                      │ ┌─ Chi so so sanh ────────────────┐ │
│                      │ │ Moc A (moi don 1 chuyen) 108.0km│ │
│    BAN DO LEAFLET    │ │ Moc B (cung xe, thu tu nhan)     │ │
│                      │ │                          68.3 km │ │
│    - Kho: o vuong    │ │ Sau toi uu               59.5 km │ │
│    - Khach: ghim so  │ │ Giam so voi A     48.5 km (44.9%)│ │
│    - Tuyen: polyline │ │ Giam so voi B      8.8 km (12.9%)│ │
│      theo duong that │ │ Thoi gian chay           412 ms  │ │
│                      │ └─────────────────────────────────┘ │
│    Chu thich mau     │                                     │
│                      │ ■ Tuyen 1 — 29H-123.45              │
│                      │   32.5 km | 1399/1500 kg | 6.6/12 m3│
│                      │   ┌───┬──────────┬─────┬────┬─────┐ │
│                      │   │ # │ Khach    │Thung│ Km │ ETA │ │
│                      │   ├───┼──────────┼─────┼────┼─────┤ │
│                      │   │ 1 │ Minh Anh │  50 │8.2 │08:16│ │
│                      │   └───┴──────────┴─────┴────┴─────┘ │
│                      │                                     │
│                      │ ■ Tuyen 2 — 29C-678.90              │
│                      │   ...                               │
│                      │                                     │
│                      │ [Gan tai xe] [Chinh tay] [Xuat Excel]│
└──────────────────────┴─────────────────────────────────────┘
```

**Khối chỉ số so sánh là thứ hội đồng nhìn vào đầu tiên.** Đặt ở vị trí nổi bật nhất, nền `--xanh-50`.

---

## SCR-12 — Màn hình tài xế (mobile)

Chiều rộng tối thiểu hỗ trợ: 360px.

```
┌────────────────────────────┐
│ Tuyen 1 — 29H-123.45       │
│ 04/08/2026 | Nguyen V Hung │
├────────────────────────────┤
│ Tien do    1 / 3 diem      │
│ Con lai    24.3 km         │
├────────────────────────────┤
│ 1. Tap hoa Minh Anh  Da giao│  <- mo di (opacity 0.55)
│    50 thung | 08:32         │
├────────────────────────────┤
│▌2. Sieu thi Hong Ha        │  <- vien trai dam, noi bat
│    So 8 Thai Ha, Dong Da   │
│    Tran Thi Binh 0912...   │
│    45 thung | Du kien 09:20│
│    ┌──────────────────────┐│
│    │ Nuoc giat 3.5L x 30  ││
│    │ Khan rua mat   x 15  ││
│    └──────────────────────┘│
│    Phai thu: 0d (Cong no)  │
│    [ Goi ]  [ Chi duong ]  │
│    [Da giao] [That bai]    │
├────────────────────────────┤
│ 3. Cua hang Bao Chau       │  <- rut gon
│    15 thung | Du kien 10:05│
├────────────────────────────┤
│    [   BAO SU CO   ]       │  <- vien do, co dinh cuoi
└────────────────────────────┘
```

**Nguyên tắc:** tài xế đang ngồi trên xe, không có thời gian đọc cả danh sách. Luôn chỉ làm nổi bật **một** điểm đang xử lý.

**Số tiền phải thu phải in đậm** khi lớn hơn 0.

---

## Bản đồ Leaflet

### Khởi tạo

```javascript
var banDo = L.map('banDo').setView([20.9584, 105.8342], 12);
L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '© OpenStreetMap contributors',
    maxZoom: 19
}).addTo(banDo);
```

### Ghim đánh số

Dùng `divIcon` với HTML tự viết:

```javascript
function taoGhim(soThuTu, mauSac) {
    return L.divIcon({
        className: 'ghim-diem-giao',
        html: '<div style="background:' + mauSac + '">' + soThuTu + '</div>',
        iconSize: [26, 26],
        iconAnchor: [13, 26]
    });
}
```

### Vẽ tuyến — điểm dễ sai nhất

**Không được** nối thẳng tọa độ các khách hàng với nhau. Phải dùng mảng `geometry` từ OSRM Route API:

```
GET http://localhost:5000/route/v1/driving/{lon1},{lat1};{lon2},{lat2};...
    ?overview=full&geometries=geojson
```

Trả về `routes[0].geometry.coordinates` là mảng `[lon, lat]`. Leaflet cần `[lat, lon]` nên **phải đảo thứ tự**:

```javascript
var toaDoLeaflet = [];
for (var i = 0; i < coordinates.length; i++) {
    toaDoLeaflet.push([coordinates[i][1], coordinates[i][0]]);
}
L.polyline(toaDoLeaflet, { color: mauTuyen, weight: 4 }).addTo(banDo);
```

Nếu OSRM không chạy được, vẽ tạm đường thẳng nối các điểm và **ghi rõ trong báo cáo** là bản demo dùng đường thẳng.

---

## Quy ước hiển thị

| Nội dung | Định dạng | Ví dụ |
|---|---|---|
| Ngày | `dd/MM/yyyy` | 04/08/2026 |
| Giờ | `HH:mm` | 08:42 |
| Khối lượng | 1 chữ số thập phân + " kg" | 1399.5 kg |
| Thể tích | 3 chữ số thập phân + " m³" | 6.635 m³ |
| Quãng đường | 1 chữ số thập phân + " km" | 32.5 km |
| Tiền | Dấu chấm ngăn nghìn + "đ" | 3.720.000đ |
| Phần trăm | 1 chữ số thập phân + "%" | 44.9% |

**Lưu ý:** khoảng cách lưu trong DB bằng mét, chia 1000 khi hiển thị.

### Nhãn trạng thái

```html
<span class="nhan nhan-thanh-cong">Da giao</span>
```

```css
.nhan { font-size: 12px; padding: 2px 8px; border-radius: 4px; }
.nhan-thanh-cong { background: var(--nen-thanh-cong); color: var(--thanh-cong); }
.nhan-canh-bao   { background: var(--nen-canh-bao);   color: var(--canh-bao); }
.nhan-loi        { background: var(--nen-loi);        color: var(--loi); }
.nhan-cho        { background: var(--nen-cho);        color: var(--cho-xu-ly); }
```

| Trạng thái | Nhãn |
|---|---|
| `DELIVERED`, `COMPLETED`, `PAID` | thành công |
| `DELIVERING`, `IN_PROGRESS`, `IN_USE`, `CONFIRMED` | mặc định (xanh dương) |
| `PENDING_APPROVAL`, `PENDING`, `DRAFT` | chờ |
| `POSTPONED`, `DEBT`, `MAINTENANCE` | cảnh báo |
| `FAILED`, `CANCELLED`, `INCIDENT` | lỗi |

---

## Encoding tiếng Việt

Mọi file HTML phải có:

```html
<meta charset="UTF-8">
```

Và trong `application.properties`:

```properties
spring.thymeleaf.encoding=UTF-8
server.servlet.encoding.charset=UTF-8
server.servlet.encoding.enabled=true
server.servlet.encoding.force=true
```

**Giao diện hiển thị tiếng Việt có dấu bình thường.** Chỉ tên biến, tên bảng, tên cột và giá trị enum mới viết không dấu.
