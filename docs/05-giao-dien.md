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
| Ngoại lệ: nút **Tìm** / **Lọc** / **Xem báo cáo** trên thanh lọc cũng xanh đặc (cỡ nhỏ), hover đậm hơn. Ô tìm kiếm đứng cách các ô lọc bên cạnh một khoảng | Nút gửi bộ lọc là việc làm nhiều nhất trên màn danh sách, phải thấy ngay |
| Ngoại lệ ở cột Thao tác của màn danh mục: nút đổi trạng thái viền **cam** khi tắt (bảo trì, ngừng dùng, khóa), viền **xanh lá** khi bật lại (`.btn-turn-off` / `.btn-turn-on`). Nút không bấm được thì nền xám | Cùng màu với nhãn trạng thái mà nút dẫn tới, nhìn là biết bấm vào sẽ thành gì |
| Nút trong cột Thao tác có độ rộng tối thiểu cố định (`.row-actions`) | Cùng một thao tác thẳng cột từ dòng này xuống dòng kia |
| Màu trạng thái chỉ dùng cho trạng thái, không trang trí | |
| Bảng dữ liệu giữ trắng và xám, kẻ sọc bằng `--nen-soc` | |

Năm màu phân biệt tuyến đã chọn cùng độ đậm nhưng khác độ sáng, để người bị mù màu đỏ–xanh lá vẫn phân biệt được.

### Thông báo và nút bị khóa

| Quy tắc | Lý do |
|---|---|
| Thông báo sau khi lưu hiện bằng **toast** góc trên bên phải, không chen vào nội dung trang | Không đẩy bảng, form xuống |
| Toast thành công (xanh lá) tự tắt sau **4 giây** | Đọc một câu ngắn là đủ |
| Toast lỗi (đỏ) **không tự tắt**, phải bấm × | Lỗi cần người dùng đọc kỹ, tự tắt là bỏ lỡ |
| Nút bị khóa (`disabled`) nền xám, chữ xám, con trỏ hình cấm | Nhìn là biết không bấm được, không phải chỉ mờ đi một chút |
| Thao tác không hoàn tác được (hủy tuyến) hỏi lại bằng hộp thoại, nút xác nhận màu đỏ | Bấm nhầm một lần là mất kế hoạch đã chốt |

---

## Bố cục chung

```
┌──────────────────────────────────────────────────────────────┐
│ [MP] Minh Phu — Quan ly giao hang  Ten (vai tro) │ Doi MK (Dang xuat)│  xanh-900
├──────────────────────────────────────────────────────────────┤
│ Danh muc▾ | Ton kho | [Don hang] | Phan tuyen | ... | Bao cao│  xanh-700
├──────────────────────────────────────────────────────────────┤
│ [thanh cong cu / bo loc]                          │  trang
├──────────────────────────────────────────────────┤
│                                                   │
│              [noi dung chinh]                     │
│                                                   │
└──────────────────────────────────────────────────┘
```

Thanh tiêu đề: ô vuông xanh bo góc chữ **MP** thay cho logo, tên hệ thống in đậm; bên phải là
tên người dùng kèm vai trò chữ nhỏ mờ, một vạch ngăn, **Đổi mật khẩu** và nút **Đăng xuất** viền
trắng bo tròn. Thanh menu tô mục đang mở: chữ đậm, nền sáng hơn một chút, gạch chân trắng — mục
*Danh mục* sáng khi đang ở bất kỳ màn danh mục nào, và mục con tương ứng trong menu thả được tô
nền. Đường dẫn hiện tại do `NavigationAdvice` đưa vào model (`currentPath`) vì Thymeleaf 3.1
không còn cho template đọc `#request`.

Menu hiển thị theo vai trò:

| Vai trò | Menu |
|---|---|
| Quản trị | Danh mục, Tài khoản, Tham số, Báo cáo |
| Điều phối | Đơn hàng, Phân tuyến, Theo dõi, Sự cố, Báo cáo |
| Tài xế | (không có menu, chỉ một màn hình duy nhất) |

### Cỡ chữ

**Chỉ năm bậc**, khai một lần thành biến ở đầu `style.css`, không chỗ nào ghi số px riêng.
Trước đây mỗi màn tự chọn (bảng 14px, nút 16px, ô lọc 14px, tiêu đề `h4` 24px, số liệu
`style="font-size: 17px"` viết tay) nên chuyển màn là thấy chữ to nhỏ khác nhau.

| Biến | Cỡ | Dùng cho |
|---|---|---|
| `--fs-meta` | 13px | Đường dẫn, gợi ý dưới ô, câu báo lỗi dưới ô, nhãn trạng thái, chú thích, `.small` |
| `--fs-label` | 14px | Nhãn ô nhập, nhãn mục, nhãn trường chỉ đọc |
| `--fs-base` | 15px | Chữ thường, bảng, **mọi** ô nhập và nút (kể cả cỡ `-sm`), menu, menu thả, phân trang, toast |
| `--fs-heading` | 17px | Tiêu đề hộp thoại, tên hệ thống trên thanh tiêu đề, số liệu phụ (`.metric-value-sm`), nút lớn ở màn tài xế |
| `--fs-title` | 22px | Tiêu đề trang (`.page-title`, `h1`–`h4`), số liệu chính |

Lớp `-sm` của Bootstrap (`btn-sm`, `form-control-sm`) giờ **chỉ làm thấp hơn, không làm chữ nhỏ
đi**. Ngoại lệ duy nhất là chữ trong biểu đồ SVG và số trên ghim bản đồ — chúng cố định theo
kích thước hình chứ không theo chữ xung quanh.

Trong bảng, **giá trị của một cột không bao giờ dùng `.small`** — địa chỉ, tọa độ, hình thức
thanh toán cùng cỡ với tên và số điện thoại bên cạnh. Chữ nhỏ chỉ dành cho dòng phụ nằm dưới
giá trị chính trong cùng một ô (ghi chú giao hàng dưới tên cửa hàng, địa chỉ dưới tên khách).

Căn phải (`.num`) chỉ dành cho số lượng đem so hoặc cộng theo cột: số thùng, kg, m³, tiền.
Tọa độ là định danh vị trí chứ không phải số lượng, nên căn trái như địa chỉ. Ô tiền không
xuống dòng (`text-nowrap`) — "0đ · Chuyển khoản trước" bị ngắt đôi đọc như hai ý rời.

### Form thêm / sửa

Mọi form dùng chung một khuôn (`fragments/form-header.html` + lớp `.entry-form`):

- Đầu trang: đường dẫn *Danh mục › Phương tiện › Sửa phương tiện* (khúc cuối viết đủ, trùng tiêu đề), tiêu đề, nút **← Quay lại danh sách** bên phải
- Rộng hết màn hình; ô nhập cao hơn ô lọc trên danh sách, nhãn đậm đặt trên ô
- Nhãn trên ô nhập in đậm ở **mọi** màn, không riêng form: ô lọc trên danh sách, hộp thoại, màn tài xế, trang đăng nhập (`.form-label`, `.toolbar label`)
- Ô số có đơn vị dính liền phía sau (`kg`, `m³`, `đ`)
- Form không có bản đồ: nút **Lưu** · **Hủy** ở dưới cùng bên trái, sau một đường kẻ
- Form có bản đồ (khách hàng, kho): ô nhập bên trái, bản đồ bên phải, **Hủy** · **Lưu** dưới bản đồ

### Báo lỗi nhập liệu

**Không để trình duyệt hiện bong bóng** kiểu "Value must be less than or equal to 5000" — tiếng
Anh, tự tắt sau vài giây, trông khác hẳn lỗi máy chủ trả về. `js/form-validation.js` (nạp ở
`layout.html` và `login.html`) tắt bong bóng trên mọi form, tự kiểm lại các thuộc tính
`required` / `min` / `max` / `step` / `type="email"` và hiện **dòng chữ đỏ ngay dưới ô**, viền ô
đỏ — cùng kiểu với lỗi `th:errors` từ máy chủ. Câu báo ghép từ nhãn của ô: *"Vui lòng nhập tải
trọng tối đa từ 300 đến 5.000 kg"*, nên ô nào viết đúng nhãn là có câu đúng. Bắt đầu sửa ô là chữ
đỏ tắt. Câu báo phía máy chủ (`@DecimalMin`…) viết giống hệt câu phía trình duyệt.

### Hộp xác nhận

**Không dùng `confirm()` / `alert()` của trình duyệt** — chúng hiện hộp "localhost says", không
đặt được tiêu đề, không tô màu nút. Mọi thao tác cần xác nhận dùng hộp chung đặt sẵn trong
`layout.html`; form chỉ khai nội dung bằng thuộc tính `data-confirm-*` (xem đầu file
`js/confirm-dialog.js`). Nút đồng ý cùng màu với thao tác: cam khi tắt, xanh lá khi bật lại,
đỏ khi hủy đơn. Con trỏ đặt sẵn ở nút **Hủy** để lỡ tay bấm Enter không thành đồng ý.

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
┌──────────────────────────────────────────────────────────────────────┐
│ Ngay giao [30/10/2026] [Xem don]                                     │
├────────────────────────────────────────────┬─────────────────────────┤
│ ┌─ Don cho phan tuyen · 11 don ───────── ^┐│ ┌─ Vi tri cac diem giao ┐│
│ │ # │ Ma don │ Khach │Thung│  kg   │  m3  ││ │                       ││
│ │ Tong cong · 11 don │ 217 │3431,8 │14,468││ │    BAN DO LEAFLET     ││
│ └─────────────────────────────────────────┘│ │  (dinh lai khi cuon)  ││
│ ┌─ Chon xe · 6/6 xe · 5.800 kg · 55 m3 ─ v┐│ │ - Kho: o vuong + ten  ││
│ │ [x] Bien so          │ Tai trong│The tich│ │ - Khach: ghim so,     ││
│ │ Suc chua da chon     │ 5.800 kg │ 55,0 m3││ │   trung cho gop "2–4" ││
│ │ [ Chay toi uu ]                         ││ │ - Chua xep: cham xam  ││
│ └─────────────────────────────────────────┘│ │                       ││
│ ┌─ Ket qua toi uu ────────────────────────┐│ │ Chu thich mau theo xe ││
│ │ Da xep vao tuyen        10 / 11 don·4 xe││ └───────────────────────┘│
│ │ Moi don di mot chuyen rieng   253,50 km ││                          │
│ │ Giao theo thu tu nhan don      81,96 km ││                          │
│ │ Sau toi uu                     72,91 km ││                          │
│ │ Giam so voi ...      180,59 km (71,2%)  ││                          │
│ │ Lap day xe trung binh · Thoi gian chay  ││                          │
│ └─────────────────────────────────────────┘│                          │
│ ┌─ ■ TT26103003 — 29H-123.45 [Nhap]       ││                          │
│ │      Da chat: 99% tai trong · 85% m3  ^ ││                          │
│ │ [Chon tai xe ▾] [Giao tuyen]  [Huy tuyen]│                          │
│ │ # │ Khach hang     │Thung│ Chang │ ETA  ││                          │
│ │ 7 │ Ve kho Minh Phu│  —  │7,92 km│  —   ││                          │
│ │ Tong cong          │  92 │25,90km│      ││                          │
│ └─────────────────────────────────────────┘│                          │
│ [Chinh thu cong: so voi tu dong]  [Chot ke hoach]                     │
└────────────────────────────────────────────┴─────────────────────────┘
```

Bảng trái chiếm 2/3, bản đồ 1/3 và **dính trên màn hình khi cuộn** để đối chiếu ghim số với
bảng của tuyến đang xem. Mỗi khối là một thẻ thu gọn được; đã có tuyến thì thẻ "Chọn xe" tự
thu gọn, tóm tắt số xe và sức chứa ngay trên tiêu đề. Không dùng khối ghi chú nền vàng: đơn
chưa xếp được nằm luôn trong thẻ "Đơn chờ phân tuyến" và một dòng của bảng kết quả.
Sau khi chạy, mỗi đơn còn lại trong thẻ đó có nút **"Hoãn sang ngày mai"** (CN-03-14); bấm vào
thì hộp xác nhận nói rõ đơn sẽ được xếp trước trong lần phân tuyến tới. Đơn đã từng hoãn mang
nhãn "Đã hoãn N lần" cạnh tên khách (BR03-14).
Nút "Chốt kế hoạch" vẫn bấm được khi chưa đủ điều kiện; bấm vào thì hiện toast đỏ nói rõ vì
sao (còn đơn chưa xếp, hoặc còn tuyến nháp chưa giao tài xế). Chỉ khóa hẳn khi không còn
tuyến nháp nào.

---

## SCR-10 — Màn hình theo dõi tiến độ giao

Điều phối chọn ngày, xem từng tuyến đã chốt trong ngày đó.

```
┌────────────────────────────────────────────────────────────┐
│ Ngay giao [20/09/2026]  [Xem]                              │
├────────────────────────────────────────────────────────────┤
│ TONG HOP CA NGAY                                           │
│  So tuyen  Diem dung  Da giao  That bai·hoan  Con cho      │
│     2         13        12         1·0           0        │
│  ████████████████████████████████████████████ 13/13 (100%) │
├────────────────────────────────────────────────────────────┤
│ TT26091101 · 29H-123.45 · Nguyen Van Hung · 24,9 km        │
│ · xuat phat 07:45                          [Hoan thanh]    │
│  ██████████████████████████████████████████████████████    │
│  # │ Don        │ Khach          │ ETA  │ That │ Tien thu │
│  1 │ DH26091101 │ Tap hoa Minh Anh│08:12│08:20 │ 8.220.000d│
│  2 │ DH26091102 │ Sieu thi Hong Ha│08:35│08:41 │     —     │
└────────────────────────────────────────────────────────────┘
```

**Tuyến `DRAFT` và `CANCELLED` không hiện.** Màn hình này theo dõi việc đang chạy; bản nháp
chưa chốt thì chưa ai giao, đưa vào chỉ làm tiến độ cả ngày bị kéo xuống sai.

Thanh tiến độ đếm **điểm đã xử lý**, gồm cả giao thất bại và hoãn — một điểm giao thất bại vẫn
là điểm tài xế đã đến và đã xử lý xong, không còn chờ ai. Con số "Đã giao" đếm riêng các điểm
thành công. Hai con số này khác nhau, đọc lẫn là hiểu sai màn hình.

Khác SCR-13 (`/track`) dành cho khách: ở đây người xem đã đăng nhập nên hiện đầy đủ biển số xe,
tên tài xế và tiền phải thu — đúng những thứ BR07-04 cấm hiện trên trang công khai.

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
│ 1. Tap hoa Minh Anh  Da giao│  <- mo di (opacity 0.55),
│    50 thung | 08:32 Xem lai │     bam vao de mo lai thong tin
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

**Số tiền phải thu phải in đậm** khi lớn hơn 0. Không có ô nhập số thực thu: bấm "Đã giao xong" là đã thu đủ số đó.

**Điểm đã xong bấm vào được để xem lại** (tên khách có chữ "Xem lại" bên phải). Thẻ mở ra thì hết mờ, hiện địa chỉ, liên hệ, hàng, số đã thu hoặc lý do thất bại, và nút gọi khách. Không có nút cập nhật kết quả.

---

## Bản đồ Leaflet

### Khởi tạo

```javascript
var map = L.map('routing-map').setView([20.9497, 105.8172], 12);
L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; OpenStreetMap',
    maxZoom: 19
}).addTo(map);
```

Nền OpenStreetMap chuẩn: miễn phí, không cần khóa API, cùng nguồn dữ liệu với OSRM nên đường
vẽ khớp số km. Đã thử CARTO Voyager nhưng nền đó **đòi khóa API** — không có khóa thì mỗi ô bản
đồ hiện chữ *"API KEY REQUIRED"* dù máy chủ vẫn trả HTTP 200.

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
