# Tiến độ đồ án

> Trạng thái hiện tại của đồ án. **Giữ dưới 60 dòng.** Cập nhật sau mỗi mốc hoàn thành.

**Cập nhật lần cuối:** 15/08/2026 · Kho: `github.com/anh05022005/MPRoute` (riêng tư), nhánh `main`

---

## Đang ở đâu

| | |
|---|---|
| Theo kế hoạch | Tuần 4 — đơn hàng |
| Thực tế | Xong Nhóm 0, trọn Nhóm 1. **Nhóm 2 xong CN-02-01 → CN-02-09**, chỉ còn CN-02-10 |

```
[x] T1  Chot de tai, hoi Minh Phu 3 cau
[x] T2  Project chay, 16 bang sinh ra, dang nhap duoc
[x] T3  CRUD 7 danh muc xong + tim toa do Nominatim
[~] T4  Tao don hang, tu tinh kg/m3/tien — xong het tru CN-02-10
[ ] T5  OSRM chay, ma tran co cache, ban do hien diem
[ ] T6  NearestNeighbor chay duoc bang main(), test xanh
[ ] T7  2-opt chay duoc, OR-Tools cam vao, bang so sanh 4 dong
[ ] T8  Man hinh phan tuyen hoan chinh, ve tuyen len ban do
[ ] T9  Man hinh tai xe, tra cuu, su co
[ ] T10 RPT-03 xong, benchmark Augerat xong, chuong 4 viet xong
[ ] T11 Seed du lieu, chay thu 3 lan, quay video
[ ] T12 Bao cao hoan chinh, slide, luyen van dap
```

---

## Đã xong

- 11 file đặc tả trong `docs/`, kể cả `10-quy-uoc-ky-thuat.md` (công nghệ, đặt tên, kiểm thử, bảo mật, Git)
- Spring Boot **3.5.16**, 11 dependency, package `com.minhphu.mproute`, kết nối MySQL 8.0.44
- Toàn bộ định danh tiếng Anh; comment tiếng Việt không dấu; chuỗi hiển thị tiếng Việt có dấu
- **★ CN-00-01** — `GET /` ra trang chào, `show-sql` in câu SQL
- **★ CN-00-02** — 18 enum + 16 entity + 16 repository → đủ **16 bảng**, không còn cột `ENUM` native
- **★ CN-00-03** — đăng nhập BCrypt, sai 5 lần khóa 15 phút (bộ đếm nằm ở cột DB nên khởi động lại không mất)
- **★ CN-00-04** — phân quyền 3 vai trò, `DRIVER` gõ `/admin/products` → **HTTP 403 + trang 403 riêng**
- **★ CN-00-05** — đổi mật khẩu, chặn đủ 4 nhánh sai
- **★ CN-00-06** — `layout.html` dùng chung, menu đổi theo vai trò bằng `sec:authorize`
- Bootstrap 5.3.3 **tải về `static/`**, không phụ thuộc mạng lúc bảo vệ
- Sửa lệch múi giờ: MySQL chạy trong WSL nên đồng hồ là UTC, lệch 7 tiếng. Đã thêm
  `forceConnectionTimeZoneToSession=true` — kiểm chứng `NOW()` từ 13:57 về đúng 20:57

- **★ CN-01-01** — danh mục sản phẩm: tìm theo mã/tên, lọc trạng thái, phân trang 15 dòng, thêm, sửa,
  ngừng dùng (không xóa cứng). Chặn được cả 4 nhánh sai: bỏ trống kg, kg = 0, m³ = 0, trùng mã.
  Đã nhập sẵn **5 sản phẩm thật** của Minh Phú theo `docs/08`, kiểm khớp từng byte UTF-8
- **★ CN-01-02** — danh mục phương tiện, cùng khuôn với sản phẩm. Chặn `maxWeight`/`maxVolume` ≤ 0,
  trùng biển số. **BR01-04 chặn ở tầng service, không chỉ ẩn ô chọn:** gửi thẳng `status=IN_USE` lên
  vẫn bị từ chối; sửa xe đang chạy thì đổi được tải trọng nhưng trạng thái giữ nguyên `IN_USE`.
  Đã nhập sẵn **4 xe thật** theo `docs/08`
- **★ CN-01-03** — danh mục tài xế, cùng khuôn. Gán được xe mặc định và tài khoản đăng nhập.
  **BR01-08 chặn đủ 4 kiểu SĐT sai**: 9 số, 11 số, đầu số 1, lẫn chữ cái. Một tài khoản chỉ gán cho
  một tài xế và phải đúng vai trò `DRIVER` — gửi thẳng `userId` của tài xế khác hoặc của `admin`
  lên vẫn bị từ chối ở tầng controller, không rơi vào lỗi trùng khóa của MySQL.
  Kiểm `HEX(full_name)` khớp UTF-8; ô không bắt buộc để trống lưu `NULL` chứ không lưu chuỗi rỗng
- **★ CN-01-07** — danh mục tài khoản: thêm, sửa, gán vai trò, khóa/mở khóa, không có nút xóa cứng.
  Chặn đủ **9 nhánh sai**: trống tên đăng nhập, tên có chữ hoa/dấu cách, tên dưới 3 ký tự, trùng tên,
  thêm mới thiếu mật khẩu, mật khẩu dưới 6 ký tự, hai lần nhập không khớp, sai định dạng email, thiếu vai trò.
  **Sửa mà để trống ô mật khẩu = giữ nguyên mật khẩu cũ** — kiểm chứng bằng cách đăng nhập lại sau khi sửa họ tên.
  **Hai rào tự bảo vệ:** không tự khóa và không tự hạ vai trò của chính mình — cộng lại bảo đảm hệ thống
  luôn còn ít nhất một quản trị viên `ACTIVE`, không bao giờ khóa hết đường vào phần quản trị.
  Khóa vĩnh viễn (`status`) và khóa tạm 15 phút (`locked_until`) hiển thị tách bạch, mỗi cái một nút gỡ riêng
- Tách `templates/fragments/pagination.html` dùng chung cho các màn hình danh mục — kiểm với 25 bản ghi:
  15 dòng trang 1, 10 dòng trang 2, giữ nguyên điều kiện lọc khi sang trang.
  Thêm tham số `role` cho màn hình tài khoản; ba màn hình còn lại truyền `null`

- Đưa code lên `github.com/anh05022005/MPRoute` (**riêng tư**), nhánh `main`, 17 commit chia theo mốc
  chức năng. Commit theo **Conventional Commits** — khuôn và ví dụ ở `docs/10-quy-uoc-ky-thuat.md`.
  **Lịch sử đã được viết lại một lần** (13/08/2026) nên mã commit cũ không còn dùng được;
  bản sao trước khi viết lại nằm ngoài kho ở `D:\JavaProject\MPRoute-BACKUP-20260813-2227.git`

- **Phân quyền danh mục theo đúng ma trận `docs/01`:** điều phối viên **xem được** sản phẩm, phương tiện,
  tài xế nhưng mọi đường ghi đều trả 403; tài khoản và tham số vẫn chỉ quản trị viên. Kiểm 14 địa chỉ:
  3 GET danh sách ra 200, 6 GET form sửa + 3 POST ghi + 2 màn hình quản trị ra 403.
  Giao diện điều phối hiện chữ "Chỉ xem" thay cho nút, menu không có mục Tài khoản/Tham số
- **★ CN-01-04** — danh mục khách hàng, cùng khuôn. Chặn đủ **12 nhánh sai**: trống tên/SĐT/địa chỉ,
  SĐT 9 số, 11 số, đầu số 1, lẫn chữ cái, trống vĩ độ, trống kinh độ, email sai định dạng, và
  **vĩ độ vượt 90** — nhánh cuối bắt đúng lỗi nhập đổi chỗ vĩ độ/kinh độ, vì kinh độ Hà Nội là
  105.8 nên đặt nhầm vào ô vĩ độ là bị chặn ngay. Trùng số điện thoại chỉ **cảnh báo, vẫn lưu**:
  cột `phone` không đặt unique (`docs/02` chỉ đánh index), thực tế có hai cửa hàng cùng chủ
  dùng chung số. Kiểm 30 phép, qua 30. Đã nhập sẵn **15 khách thật** theo `docs/08` — 3 khách có
  ghi chú giao hàng, 5 khách có email, tọa độ đều nằm trong 20.94–21.01 / 105.79–105.85,
  kiểm `HEX(name)` khớp UTF-8 từng byte
- **★ CN-01-06** — danh mục kho. **BR01-10 là toàn bộ nội dung của màn hình này:** hệ thống luôn
  giữ **đúng một** kho `active`, vì đó là điểm `c0` của bài toán — hai kho active thì không biết
  lấy cái nào, không có cái nào thì không phân tuyến được. Cách bảo đảm: **không làm nút "tắt kho
  chính"**, đổi kho là *bật* kho mới lên rồi một câu `UPDATE` tắt các kho khác, nên không bao giờ
  đi qua trạng thái trung gian không còn kho nào. Bỏ tích kho active cuối cùng bị chặn ở
  controller, và có thêm lưới an toàn ở service. Kiểm 24 phép, qua 24.
  Đã nhập **Kho Minh Phú** theo `docs/08`, kiểm `HEX(address)` khớp UTF-8 từng byte
- **★ CN-01-05** — tìm tọa độ bằng Nominatim, dùng chung cho form khách hàng và form kho.
  **Gọi qua máy chủ chứ không gọi thẳng từ trình duyệt**, vì ba lý do: chính sách Nominatim bắt
  khai `User-Agent` nhận dạng được (JavaScript không đặt được), giới hạn 1 request/giây chỉ chặn
  thật được ở máy chủ, và khỏi phụ thuộc CORS. Ba cách đặt tọa độ đều ghi vào cùng hai ô:
  chọn từ danh sách Nominatim, bấm/kéo ghim trên bản đồ (BR01-06), hoặc gõ tay.
  **Lỗi mạng nuốt có chủ đích** — Nominatim chết không được làm hỏng cả màn hình thêm khách.
  Leaflet 1.9.4 tải về `static/`, không phụ thuộc mạng lúc bảo vệ. Kiểm 27 phép, qua 27
- **★ CN-01-08** — tham số hệ thống: 9 tham số theo `docs/02`, `DataSeeder` tạo sẵn khi khởi động,
  sửa cả 9 trên **một form lưu một lần** (chúng hay được chỉnh cùng nhau). Cột `value` là VARCHAR
  nên **CSDL không bảo vệ được kiểu dữ liệu** — toàn bộ việc kiểm dồn vào `kiemGiaTri()`, tách theo
  từng mã. Chặn **14 nhánh sai**, trong đó có hai nhánh đáng nói: chặn cả **chặn trên** (`AVERAGE_SPEED`
  = 5000 không làm chương trình chết, chỉ làm mọi ETA sai mà không báo gì), và mã tham số lạ gửi lên
  **bị bỏ qua chứ không tự tạo bản ghi mới**. Kiểm 33 phép, qua 33
- **Thêm `OR_TOOLS` vào enum `AlgorithmType`** và đổi `DEFAULT_ALGORITHM` từ `NN_2OPT` sang
  `OR_TOOLS` — hệ quả trực tiếp của việc đổi bộ giải. Tham số này nay có ích thật: đổi nó là
  đổi bộ giải, đúng thứ cần cho bảng so sánh chương 4
- **★ Đổi định hướng thuật toán (14/08/2026): OR-Tools thành bộ giải chính**, bản tự cài đặt
  NN + 2-opt giữ nguyên nhưng đổi vai trò thành **mốc so sánh** ở chương thực nghiệm.
  Hai package tách bạch — `optimization/ortools/` và `optimization/heuristic/`, package sau
  **cấm import OR-Tools**, có hook chặn. Lý do và phần được/mất ghi ở `docs/09` mục **A3**.
  Đã kiểm chứng trước khi đổi kế hoạch: `OrToolsSmokeTest` giải bài CVRP 4 điểm 2 xe ra
  **đúng đáp số tính tay 8000 m** trong 1,5 s trên Windows 11 + JDK 23. Chương 4 nay có
  **ba mốc** A/B/C thay vì hai. Đã sửa 15 file: `docs/03,06,07,09,10`, `CLAUDE.md`,
  4 rule/agent, 3 skill, 2 hook
- **★ CN-02-01 → CN-02-06** — danh sách và tạo đơn hàng. **Phân quyền ngược với danh mục:**
  đơn hàng là việc của điều phối viên, quản trị viên chỉ xem — kiểm 10 địa chỉ, `admin` gửi
  `POST /orders/save` ra **403**, `taixe01` gõ `/orders` ra **403**.
  Kiểm tay bằng `curl`, tính tay đối chiếu từng số: 3 thùng bông tẩy trang + 2 thùng nước lau sàn
  = **67.6 kg / 0.317 m³ / 2.200.000đ**, khớp cả ba con số trên màn hình và trong CSDL.
  - **CN-02-04** — tạo 3 đơn cùng ngày giao ra `MP2608160001`, `0002`, `0003` liên tiếp, không trùng.
    Sáu chữ số giữa lấy theo **ngày giao** chứ không phải ngày tạo, để điều phối viên đọc mã qua
    điện thoại là biết ngày phải giao. Trùng mã thì tăng số thứ tự tiếp — vòng lặp này cần thật
    vì sửa đơn sang ngày giao khác sẽ làm số đếm lệch khỏi số thứ tự lớn nhất đã phát
  - **CN-02-05** — chặn đơn không xe nào chở nổi: điều kiện đúng là **tồn tại một xe thỏa cả hai
    ràng buộc cùng lúc**, không phải "có xe nào đó đủ tải" và "có xe nào đó đủ khối". Thử 1000 thùng
    nước lau sàn → chặn kèm số liệu `24800.00 kg / 31.000 m³ so với 1500.00 kg / 12.000 m³ của xe 29H-123.45`
  - **BR02-14** — kiểm bằng 3 đơn thật: `CASH` phải thu đủ 2.200.000đ, `CREDIT` và `PREPAID` đều 0đ
  - **BR02-08** — sửa địa chỉ giao cho riêng đơn mà giữ nguyên tọa độ cũ **bị chặn**. Đây là kiểu sai
    nguy hiểm nhất vì không ai nhìn ra: màn hình hiện địa chỉ mới, thuật toán vẫn giao về chỗ cũ
  - Chặn đủ **5 nhánh sai** phía máy chủ: thiếu dòng chi tiết, ngày giao quá khứ, vượt sức chứa,
    ghi đè địa chỉ thiếu tọa độ, chọn sản phẩm mà số thùng ≤ 0
  - **Mở `/admin/geocode` cho cả điều phối viên.** Trước đó chỉ quản trị viên, nhưng BR02-08 bắt
    điều phối viên xác định lại tọa độ khi ghi đè địa chỉ. Vẫn đóng với tài xế và khách
  - Khối tóm tắt đọc lại cho khách (CN-02-06) dựng ở **tầng service** chứ không ghép chuỗi trong
    file `.html` — có vòng lặp và định dạng tiền, để trong template thành biểu thức không sửa nổi
- **★ CN-02-07, CN-02-08, CN-02-09** — duyệt, sửa, hủy đơn. Ba nút nằm trên trang chi tiết đơn.
  - **Cả ba chuyển trạng thái đều ghi `status_history` (CBR-14).** Chạy thật một đơn qua đủ ba chặng
    `Chờ duyệt → Đã duyệt → Đã hủy`, bảng lịch sử ra đúng 3 dòng kèm người thực hiện và lý do hủy
  - **BR02-11** chặn ở máy chủ, không chỉ ẩn nút: đơn đã hủy thì `GET /orders/2/edit`,
    `POST /orders/save` với `id=2`, và `POST /orders/2/cancel` **đều bị chặn**, dữ liệu đơn không đổi
    một chữ nào. Câu báo phân biệt theo trạng thái — đơn đã hủy báo "đơn này đã hủy", chỉ đơn từ
    `ROUTED` trở đi mới báo đúng MSG-07 "đơn đã phân tuyến", báo nhầm thì người dùng đi tìm một
    tuyến không có thật
  - **Sửa đơn giữ nguyên mã đơn** kể cả khi đổi ngày giao — khách đã được đọc mã qua điện thoại.
    Đây chính là lý do `sinhMaDon()` phải có vòng lặp kiểm trùng: sau một lần đổi ngày giao, số đếm
    đơn trong ngày không còn khớp với số thứ tự lớn nhất đã phát cho ngày đó
  - **Sửa đơn dùng `items.clear()` chứ không `setItems(danh sách mới)`** — `orphanRemoval` bám vào
    đúng thể hiện danh sách Hibernate đang theo dõi, thay bằng danh sách khác thì dòng cũ không bị
    xóa và đơn cộng dồn cả hàng cũ lẫn hàng mới. Kiểm chứng: sửa đơn 2 dòng thành 1 dòng, tổng ra
    đúng 30.0 kg / 0.425 m³ / 2.400.000đ, khối tóm tắt chỉ còn một dòng hàng
  - **BR02-16** kiểm luôn trong lần sửa đó: đổi `CASH` sang `CREDIT` thì phải thu về **0đ** ngay
  - Chặn thêm **3 nhánh sai**: duyệt lại đơn đã duyệt, hủy không nhập lý do, lý do quá 500 ký tự
  - Phân quyền: `admin` gõ `/orders/1/edit` và POST duyệt/hủy đều ra **403**, tài xế cũng **403**
- **Sửa 405 thành 403** khi vai trò không đủ quyền gửi POST tới `/admin/**`. Nguyên nhân:
  `accessDeniedPage()` *chuyển tiếp* nên giữ nguyên method POST, mà `HomeController` khai `@GetMapping("/403")`.
  Đổi sang `@RequestMapping("/403")`

Tài khoản mẫu do `DataSeeder` tạo, khớp `docs/08`: `admin/admin123` · `dieuphoi/dieuphoi123` ·
`taixe01`, `taixe02`, `taixe03` cùng `taixe123`. Ba tài xế chứ không phải một, vì kịch bản demo
bước 12–14 (phương án B) cần hai tuyến chạy song song mới dựng được tình huống.

---

## Việc tiếp theo

1. **CN-02-10 thêm khách ngay trong màn tạo đơn** — vướng ma trận phân quyền,
   xem mục "Cần quyết định" bên dưới. Quyết xong mới làm
2. Xong CN-02-10 là hết Nhóm 2 → sang **tuần 5: OSRM + Leaflet + ma trận khoảng cách**,
   rồi tới tuần 6–7 là trái tim đồ án
3. Còn nợ một phép nghiệm thu của BR02-11: đơn `ROUTED` mới ra đúng chữ MSG-07.
   **Tuần 8 có màn hình phân tuyến mới thử được** — hiện chỉ thử được nhánh đơn đã hủy

## Ba việc còn nợ, phải quay lại làm

| Việc | Khi nào làm được | Vì sao chưa làm |
|---|---|---|
| **Nghiệm thu thật CN-01-08** | Tuần 8 | Tiêu chí không phải "sửa được" mà là *đổi `AVERAGE_SPEED` rồi chạy lại phân tuyến thì ETA phải đổi theo*. Màn hình phân tuyến tuần 8 mới có |
| **Thử tay phần bản đồ CN-01-05** | Bất cứ lúc nào | Đã kiểm tự động phía máy chủ (27 phép) nhưng **chưa ai bấm thử thật**: bấm vào bản đồ để ghim, kéo ghim, chọn dòng trong danh sách nhiều kết quả. Phần JavaScript này chỉ chạy trong trình duyệt |
| **Thử tay `don-hang.js` (CN-02-03)** | Bất cứ lúc nào | Cùng lý do: tổng kg/m³/tiền cập nhật ngay khi gõ, thêm/xóa dòng, chọn khách tự điền địa chỉ — toàn bộ chỉ chạy trong trình duyệt. Phía máy chủ đã kiểm xong, cú pháp JavaScript đã kiểm bằng `node --check`, nhưng **chưa ai gõ thử thật**. Thử luôn khối nhập lý do hủy (dùng `collapse` của Bootstrap) |

---

## Cần quyết định

| Việc | Tình hình |
|---|---|
| **Cắt phạm vi — quyết trước tuần 8** | 86 chức năng bắt buộc / 12 tuần / một người. Ước lượng thật là 18–24 tuần. **Cắt chủ động ngay chứ đừng đợi tuần 9 mới cắt trong hoảng loạn.** Cắt hẳn RPT-01, RPT-02 và toàn bộ mục ○ từ bây giờ |
| **Mâu thuẫn về thứ tự cắt** | `docs/00` xếp UC06 (sự cố) vào diện cắt sớm, nhưng kịch bản demo lại lấy bước 13 làm điểm nhấn. Dung hòa: giữ UC06 nhưng làm **phương án A và C trước**, phương án B (Cheapest Insertion) làm sau nếu còn thời gian |
| **CN-02-10 mâu thuẫn với ma trận phân quyền** | `docs/04` bảo điều phối viên thêm khách ngay trong màn tạo đơn, nhưng `docs/01` xếp danh mục là "chỉ xem" với vai trò này. Ba hướng: (a) mở riêng đường thêm khách cho điều phối viên và ghi rõ ngoại lệ vào `docs/09`; (b) cắt CN-02-10, điều phối viên nhờ quản trị viên thêm trước; (c) đổi ma trận. **Nghiêng về (a)** — thực tế khách gọi đặt hàng lần đầu thì không thể bắt họ chờ |
| Thư viện xuất Excel | CN-08-05 cần Apache POI. Thêm vào `pom.xml` khi làm tới tuần 10 |

---

## Đã biết trước, cố ý chưa sửa

| Vấn đề | Khi nào lộ ra | Vì sao chưa sửa |
|---|---|---|
| NN để sót đơn dù tổng sức chứa đủ | Tuần 8, lần đầu chạy trên dữ liệu mẫu | **Cố ý giữ.** Là điểm nhấn demo bước 5a và câu trả lời vấn đáp mạnh nhất. Kịch bản ở `docs/08` |
| Trạng thái xe là cờ toàn cục, không theo ngày | Chỉ lộ nếu thử chốt tuyến cho hai ngày liên tiếp | Sửa đúng phải đổi CBR-05 và tầng service. Đã ghi vào hạn chế `docs/07` 4.7 |
| Ghi chú "chỉ nhận buổi sáng trước 11h" mà thuật toán không ràng buộc khung giờ | Tuần 8 khi nhìn ETA thật | Giữ vì phản ánh khảo sát thật. Cần chạy thử và **ghi lại ETA điểm cuối từng tuyến** để trả lời bằng số |

---

## Nợ kỹ thuật

- Menu trỏ tới `/routing`, `/tracking`, `/incidents`, `/reports` chưa có Controller → bấm vào ra 404.
  `/orders` đã có. Hết tuần 5–10 là tự hết
- Trong CSDL đang có **4 đơn thử** do lần kiểm thử `curl` tạo ra: `MP2608160001` (Đã duyệt),
  `MP2608160002`, `MP2608160003`, `MP2608170001` (cả ba Đã hủy). Giữ lại để bấm thử giao diện;
  xóa lúc seed lại dữ liệu ở tuần 11
- Khi kiểm thử form bằng `curl` trong Git Bash: **phải percent-encode tiếng Việt sẵn** rồi dùng
  `--data-raw`. Git Bash chuyển tham số sang bảng mã ANSI khi gọi `curl.exe`, làm hỏng dấu tiếng Việt
  ngay từ máy khách — dễ tưởng nhầm là ứng dụng sai encoding. Kiểm bằng `HEX(cột)` trong MySQL.
  **Lệnh `docker exec ... mysql -e "…"` gọi từ PowerShell cũng hỏng y hệt**, mà kiểu khác: chữ bị mã hóa
  hai lần (`Nguyễn` thành `Nguyá»…n`, hex `C3A1C2BB…` thay vì `E1BB85`). Muốn sửa dữ liệu tiếng Việt
  bằng SQL trực tiếp thì dùng `UNHEX('…')` với chuỗi byte UTF-8 dựng sẵn, đừng gõ chữ có dấu vào lệnh
- **OpenStreetMap ở Hà Nội hầu như không có dữ liệu số nhà.** Nút "Tìm tọa độ" chỉ đưa về đúng
  *con đường*, không ra số nhà — `91 Chùa Bộc, Đống Đa` trả về đúng một kết quả là *"Cầu vượt Tây Sơn"*.
  Đây là giới hạn dữ liệu OSM, không sửa được từ phía đồ án; thêm `, Hà Nội` vào chuỗi tìm **không**
  cải thiện (đã đo). Vì vậy quy trình đúng là: tìm để nhảy tới đúng phố → **bấm tay lên bản đồ**
  đúng vị trí cửa hàng. Chính là điều BR01-06 đã lường trước.
  **Đã từng có hai lỗi ở đây, cả hai đã sửa:**
  (1) code cũ tự chọn luôn khi Nominatim trả về đúng 1 kết quả, nên nó im lặng nhảy tới chỗ sai —
  nay luôn hiện danh sách kèm tọa độ để soát mắt, bản đồ không tự di chuyển;
  (2) không giới hạn vùng nên Nominatim đoán bừa sang tỉnh khác — `15 Kim Giang, Thanh Xuân` trả về
  một đường ở **Đà Nẵng** cách 800 km. Nay thêm `viewbox` + `bounded=1` bó về khung Hà Nội, địa chỉ
  đó ra **0 kết quả** (đúng hơn hẳn một kết quả sai), còn `24 Láng Hạ` vẫn ra đủ 5 kết quả đúng
- **Ảnh nền bản đồ vẫn cần mạng.** Thư viện Leaflet đã tải về `static/` nên không phụ thuộc mạng,
  nhưng các ô ảnh bản đồ lấy từ `tile.openstreetmap.org`. Bảo vệ mà không có mạng thì khung bản đồ
  hiện xám — ghim, tọa độ và nút lưu **vẫn hoạt động bình thường**, chỉ mất phần nhìn.
  Nếu ngày bảo vệ không chắc có mạng thì nên chuẩn bị ảnh chụp màn hình sẵn
- Sau khi đăng nhập địa chỉ có đuôi `?continue` — hành vi mặc định của Spring Security 6, vô hại
- `application.properties` chứa mật khẩu MySQL `gbao`. Hiện không sao vì kho GitHub để **riêng tư**.
  **Nếu đổi kho sang công khai thì phải xóa dòng đó khỏi lịch sử trước** — nó nằm trong commit đầu `c55b2d3`,
  chuyển sang riêng tư rồi công khai lại không tự xóa được
- Máy này lưu sẵn thông tin đăng nhập GitHub của **hai tài khoản**. Remote cố ý ghi là
  `https://anh05022005@github.com/...` — có `anh05022005@` ở giữa thì Git mới lấy đúng tài khoản.
  Xóa phần đó đi là đẩy bằng tài khoản kia, báo lỗi 403
