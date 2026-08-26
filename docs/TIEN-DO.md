# Tiến độ đồ án

> Trạng thái hiện tại của đồ án. **Giữ dưới 60 dòng.** Cập nhật sau mỗi mốc hoàn thành.

**Cập nhật lần cuối:** 24/08/2026 · Kho: `github.com/anh05022005/MPRoute` (riêng tư), nhánh `main`

---

## Đang ở đâu

| | |
|---|---|
| Theo kế hoạch | Tuần 9 — tài xế, tra cứu, sự cố |
| Thực tế | **Đúng tiến độ.** Tuần 8 xong trọn. Tuần 9: **xong Nhóm 5 (CN-05-01 → 11) và Nhóm 7 (CN-07-01 → 07)** — đơn hàng đi trọn vòng đời, và khách tra cứu được từ ngoài. Còn email ETA và nhóm sự cố |

```
[x] T1  Chot de tai, hoi Minh Phu 3 cau
[x] T2  Project chay, 16 bang sinh ra, dang nhap duoc
[x] T3  CRUD 7 danh muc xong + tim toa do Nominatim
[x] T4  Tao don hang, tu tinh kg/m3/tien — xong tron Nhom 2
[x] T5  OSRM chay, ma tran co cache, ban do hien diem
[x] T6  NearestNeighbor chay duoc bang main(), test xanh
[x] T7  2-opt chay duoc, OR-Tools cam vao, bang so sanh 4 dong
[x] T8  Man hinh phan tuyen hoan chinh, ve tuyen len ban do
[~] T9  Man hinh tai xe, tra cuu, su co  <- dang lam
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
  **ba mốc** A/B/C thay vì hai. Đã sửa 15 file: `docs/03,06,07,09,10`, 
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
- **★ CN-02-10** — thêm khách ngay trong màn tạo đơn, hộp thoại gọi `POST /orders/quick-customer`
  trả JSON, thêm xong tự chọn luôn cho đơn đang tạo, **không tải lại trang** (tải lại là mất hết
  dòng chi tiết vừa gõ).
  - **Đây là một ngoại lệ có chủ ý của ma trận phân quyền**, đã ghi vào `docs/09` mục **A4** kèm
    lý do và phần được/mất. Ngoại lệ bó hẹp hết mức và đã kiểm chứng bằng cách gửi thẳng dữ liệu
    lên máy chủ: gửi kèm `id=6` thì hệ thống **tạo khách mới**, khách #6 giữ nguyên tên và trạng thái;
    gửi `status=INACTIVE` thì bị **ép về `ACTIVE`**; điều phối viên vẫn ra **403** ở cả 4 đường ghi
    của `/admin/customers/**`
  - Hộp thoại **cố ý không có bản đồ ghim tay** — chỉ tìm tọa độ theo địa chỉ hoặc gõ tay. Lý do kỹ
    thuật ghi ở `docs/09` A4: `chon-toa-do.js` gắn cứng vào ba ô của chính trang, sửa thành dùng
    chung thì phải đụng vào hai màn hình đang chạy tốt
  - Trùng số điện thoại vẫn chỉ **cảnh báo mềm**, giống CN-01-04

- **★ Tuần 5 — nền tảng khoảng cách.** Package `service/optimization/` bắt đầu có người ở:
  `OptimizationPoint`, `DistanceCalculator` (interface), `HaversineDistanceCalculator`,
  `OsrmDistanceCalculator`, `DistanceMatrixService`.
  - **Strategy pattern ở chỗ tính khoảng cách** — đổi cách tính không phải sửa một dòng nào của
    thuật toán. Đây vừa là điều kiện để hệ thống sống sót khi OSRM chết (CBR-11), vừa là một ý
    trả lời sẵn khi vấn đáp
  - **`DistanceMatrixService` đủ 4 bước B1→B4** theo `docs/03`. Nạp ma trận vào bộ nhớ **một lần**,
    sau đó thuật toán chỉ tra `matrix[i][j]` — đúng điều cấm số 4. Bộ nhớ đệm nạp bằng **một câu
    truy vấn** rồi đổ vào `HashMap`, không phải n² câu SELECT
  - **Bước B3 đối xứng hóa không được quên.** OSRM trả ma trận bất đối xứng (đường một chiều),
    mà công thức bốn cạnh của 2-opt chỉ đúng khi ma trận đối xứng
  - **Cố ý KHÔNG lưu giá trị Haversine vào bảng `distance_matrix`.** Bảng đó là bộ nhớ đệm *của
    OSRM*; lưu số xấp xỉ vào thì lần sau tra thấy "đã có" rồi vĩnh viễn không gọi OSRM nữa, mà
    lúc đó cờ `dungXapXi` cũng không còn bật lên để ai biết
  - **★ Test bắt buộc số 2 `haversineMatchesHandCalculatedDistances` — đã xanh.** Một độ vĩ độ ra đúng
    `R × π/180 = 111194,93 m`; kho → 91 Chùa Bộc ra `6455,43 m` chim bay, `8392,06 m` sau khi
    nhân hệ số 1.3. Đối chiếu độc lập bằng phép xấp xỉ mặt phẳng, lệch dưới 1 mm
  - **★ CN-03-02** — màn hình `/routing` chọn ngày, hiện đơn `APPROVED` kèm tổng kg/m³, vẽ điểm
    giao lên Leaflet: kho là ô vuông đậm, khách là chấm tròn, rê chuột hiện tên và số thùng.
    Dữ liệu ghim truyền qua khối JSON `th:text` (tự escape) chứ không nối chuỗi vào JavaScript
  - **★ CN-03-11 (nhánh dự phòng) đã chạy thật:** tắt OSRM rồi bấm "Chuẩn bị ma trận" → ra
    `ma trận 4×4 (12 ô) trong 67 ms, lấy từ bộ nhớ đệm: 0, phải lấy mới: 12, nguồn Haversine (xấp xỉ)`
    kèm **MSG-09**, bảng `distance_matrix` vẫn rỗng đúng như thiết kế
  - Phân quyền như màn hình đơn: `admin` gõ `/routing` ra 200 nhưng POST chuẩn bị ma trận ra **403**,
    tài xế **403** cả hai

- **★ OSRM đã dựng xong và chạy được** (21/08/2026). Bản đồ Việt Nam của Geofabrik, 327 MB,
  tiền xử lý bằng Docker hết khoảng 10 phút: `osrm-extract` đỉnh **7,2 GB RAM** / 19,3 triệu cạnh,
  `osrm-partition` 4,4 GB, `osrm-customize` 2,7 GB. Máy 16 GB chạy thoải mái, **máy 8 GB thì phải
  cắt riêng vùng Hà Nội trước**.
  - Dữ liệu để ở **`D:\JavaProject\osrm-data`, NGOÀI kho git** (327 MB + ~1,3 GB sau xử lý).
    Đã thêm `*.osm.pbf` và `*.osrm*` vào `.gitignore` làm lưới an toàn
  - Máy chủ chạy trong container tên **`osrm-vietnam`** cổng 5000, chiếm ~2,4 GB RAM.
    Mở lại sau khi khởi động máy: `docker start osrm-vietnam`
  - **★ CN-03-05 nghiệm thu đạt.** Cùng một tập 4 điểm, bấm "Chuẩn bị ma trận" hai lần:
    lần 1 `0 ô từ bộ nhớ đệm / 12 ô lấy mới / 307 ms`, lần 2 `12 ô từ bộ nhớ đệm / 0 ô lấy mới / 31 ms`.
    **Log lần 2 không có dòng nào gọi OSRM** — đúng tiêu chí. Nhanh gấp 10 lần
  - **Con số chứng minh vì sao phải đối xứng hóa (bước B3), lấy từ chính dữ liệu thật:**
    kho → 91 Chùa Bộc là **10 481 m**, chiều ngược lại **11 538 m** — lệch **1 057 m, hơn 10%**.
    Đây là số nên đưa vào báo cáo chương 4, mạnh hơn hẳn ví dụ dựng sẵn trong `docs/03`
  - Đối chiếu Haversine với đường thật trên chính cặp điểm đó: xấp xỉ ra 8 392 m, đường thật
    trung bình hai chiều 11 010 m → **hệ số 1.3 đang ước lượng THIẾU khoảng 24%** cho tuyến này.
    Chưa đổi vội, đợi có nhiều cặp điểm hơn ở tuần 8 rồi tính lại `HAVERSINE_FACTOR` bằng số liệu
- **★ Tuần 6 — Nearest Neighbor chạy được bằng `main()`.** Mốc sống còn của đồ án đã qua.
  - Lớp dữ liệu: `OptimizationVehicle`, `OptimizationRoute`, `RoutingResult`, interface
    `RoutingAlgorithm` (Strategy pattern thứ hai), và `RouteDistanceCalculator`
  - **`RouteDistanceCalculator` tách riêng một lớp, dùng `static`, để CẢ HAI bộ giải gọi
    chung một hàm.** Mỗi bên tự tính lấy thì chỉ cần lệch một chặng là bảng so sánh chương 4
    mất ý nghĩa
  - `NearestNeighborSolver` nằm ở `optimization/heuristic/`, viết bằng vòng lặp `for`
    tường minh **ánh xạ 1–1 với mã giả B1…B28** của `docs/03` — để hội đồng chỉ vào dòng nào
    cũng đối chiếu được với tài liệu
  - **`AlgorithmRunner` có `main()`**, chạy bằng
    `mvnw.cmd compile exec:java -Dexec.mainClass=...AlgorithmRunner`,
    **không cần web, không cần MySQL, không cần OSRM** (dùng Haversine)
  - **★ Kết quả chạy khớp CHÍNH XÁC với `docs/08`** — cùng thứ tự điểm, cùng số kg, cùng đơn bị sót:

    ```
    29H-123.45  22.12 km  1455.0/1500 kg (97%)  Binh Minh -> An Phat -> Bao Chau -> Minh Anh
    29F-555.66  18.46 km   457.5/1000 kg (46%)  Duc Loi -> Kim Ngan -> Thanh Tam
    KHONG GAN DUOC: Sieu thi Hong Ha (727.5 kg)      Tong 40.58 km, chay het 5 ms
    ```

    Đây là **điểm nhấn demo mạnh nhất**: tổng sức chứa 3000 kg > tổng nhu cầu 2640 kg mà vẫn
    sót một đơn, vì phân đơn cho xe là bài toán xếp thùng còn NN thì tham lam theo khoảng cách.
    **Đừng sửa dữ liệu để giấu tình huống này đi**
  - **6/14 test bắt buộc đã xanh:** `totalDistanceIncludesReturnLegToDepot`, `haversineMatchesHandCalculatedDistances`,
    `nearestNeighborNeverExceedsWeightCapacity`, `nearestNeighborNeverExceedsVolumeCapacity`, `nearestNeighborAssignsEveryPointWhenCapacityAllows`,
    `nearestNeighborReportsUnassignedPointsWhenCapacityRunsOut`. Tất cả dùng **một hình chữ nhật 4×3 km** với ma trận
    số nguyên (đường chéo 3-4-5), cộng tay ra đáp số không cần máy tính
  - Test tải trọng cố ý đặt một điểm **vừa đúng bằng** giới hạn 700/700 kg — cài đặt nào viết
    nhầm `>=` thay vì `>` là test đỏ ngay
  - Nới hai luật của hook kiểm tra quy tắc: cho phép `System.out` trong file
    tên `*Runner` (chính là chương trình chạy thử console mà quy tắc đã trừ ra), và chỉ soi
    luật "chặng về kho" ở file cài đặt chứ không soi file test
- **★ Tuần 7 — hai bộ giải chạy song song, bảng so sánh có số.**
  - `TwoOptImprover` ở `optimization/heuristic/`: công thức bốn cạnh
    `delta = D[a][c] + D[b][d] - D[a][b] - D[c][d]`, ngưỡng `-0.001` chứ không phải `< 0`
    (sai số `double` làm vòng lặp không bao giờ dừng), chặn trên 100 vòng quét
  - `NN2OptSolver` ghép hai giai đoạn, `EtaCalculator` tính ETA (CBR-13),
    `CheapestInsertion` cho xử lý sự cố tuần 9
  - `OrToolsSolver` ở `optimization/ortools/` — **hai `Dimension` riêng** tên `Weight` và
    `Volume`, `addDisjunction` với tiền phạt `10 × ô lớn nhất`, `setTimeLimit` 5 giây.
    Làm tròn **nhu cầu LÊN, sức chứa XUỐNG** để sai số làm tròn không tạo ra tuyến quá tải
  - `MetricsService` — mốc A, mốc B, `reductionPercent`, `gapPercent`, hiệu suất xe.
    **Mốc A chỉ tính trên các điểm đã được xếp tuyến**, không tính trên toàn bộ điểm đầu vào:
    so sánh chỉ có nghĩa khi hai bên phủ đúng một tập điểm
  - **★ 14/14 test bắt buộc đã xanh.** Tám test mới: `twoOptNeverIncreasesTotalDistance`,
    `twoOptPreservesPointSet`, `etaOfFirstStopMatchesHandCalculation`,
    `cheapestInsertionPicksCorrectPosition`, `orToolsRespectsBothCapacityDimensions`,
    `orToolsDropsPointsWhenCapacityIsInsufficient`, `bothSolversCoverTheSamePointSet`
  - **Số liệu chạy thật, 30 điểm sinh với hạt giống cố định `20260824`, 6 xe, Haversine × 1.3**
    (`AlgorithmRunner 30`, chạy lại bao nhiêu lần cũng ra đúng bộ số này):

    ```
    Cach phan tuyen                         Diem         Km     Giam Thoi gian
    Moc A - moi don mot chuyen khu hoi        30  511.06 km        -         -
    Moc B - dung cach chia xe, sai thu tu     30  222.33 km    56.5%         -
    Nearest Neighbor (giai doan 1)            30  186.21 km    63.6%      9 ms
    NN + 2-opt tu cai dat                     30  183.87 km    64.0%    < 1 ms
    OR-Tools (bo giai chinh)                  30  149.02 km    70.8%   6576 ms
    ```

    **NN + 2-opt còn dài hơn OR-Tools 23,4%** — nằm đúng khoảng 10–25% mà `docs/07` dự đoán.
    Đây là con số trả lời câu "sao em không tự viết hết": em tự viết, đo được nó hơn cách
    làm thủ công bao nhiêu **và** còn kém lời giải tốt bao nhiêu, rồi cho sản phẩm chạy
    bằng cái tốt hơn
  - **2-opt chỉ cải thiện thêm 1,3%** so với NN thuần. Không phải cài sai — 30 điểm chia 6 xe
    thì mỗi tuyến chỉ 5 điểm, 2-opt gần như không còn gì để đảo. Đây chính là **hạn chế phải
    nói thẳng khi vấn đáp**: 2-opt chỉ sắp lại thứ tự *bên trong* một tuyến, nó không bao giờ
    chuyển được một điểm sang xe khác — OR-Tools thì làm được, và đó là toàn bộ khoảng cách 23,4%
  - `AlgorithmRunner` **in cảnh báo và tính lại cột "Giảm" riêng cho từng tập điểm** khi ba bộ
    giải không phủ cùng tập điểm. Với dữ liệu mẫu `docs/08` thì NN bỏ sót 1 điểm còn OR-Tools
    xếp đủ 8, nếu cứ đặt 40 km cạnh 51 km thì kết luận sẽ **ngược hẳn** với sự thật
- **★ Tuần 8 — thuật toán đã chạy trên web.** Bấm nút trên `/routing` là ra tuyến thật, lưu xuống CSDL.
  - **`RoutingService`** là lớp nối duy nhất giữa thuật toán và phần còn lại: đọc CSDL → dựng đầu
    vào → gọi bộ giải → ghi CSDL. Nhờ ranh giới này `service/optimization/` vẫn sạch JPA, vẫn chạy
    được bằng `main()` và vẫn chạy được benchmark Augerat ở tuần 10
  - **Cả hàm `optimize()` nằm trong MỘT giao dịch.** Không phải chi tiết trang trí: bước B1 xóa kết
    quả nháp cũ, mà bước B4 có thể ném lỗi thiếu sức chứa. Chung một giao dịch thì lần chạy hỏng
    **hoàn tác cả bước xóa** — chọn thiếu xe không làm mất kết quả đang có trên màn hình
  - **★ CN-03-06, CN-03-13** — chạy thật trên 3 đơn ngày 21/08, xe 29F-555.66, OSRM bật:
    `TT26082101 · 25 173,7 m · 3 điểm · OR-Tools · 5 003 ms`. Ba dòng `route_stops` có `leg_distance`
    và ETA, một dòng `optimization_results` với đủ hai mốc
  - **Kiểm tay CBR-01 ngay trên số thật:** ba chặng cộng lại 9 265,5 + 4 557,7 + 6 113,0 = **19 936,2 m**,
    trong khi `total_distance` = **25 173,7 m**. Chênh **5 237,5 m** chính là chặng cuối về kho.
    Màn hình có hẳn một dòng "↩ Về kho" luôn hiện để chỉ vào lúc vấn đáp
  - **Kiểm tay ETA khớp từng giây:** 9 265,5 m ÷ 30 km/h = 18 phút 32 giây → **08:18:32** đúng như
    CSDL. Điểm 2 cộng thêm 1 × 15 phút bốc dỡ → **08:42:39**. Điểm 3 cộng 2 × 15 phút → **09:09:52**.
    Đây cũng chính là **phép nghiệm thu còn nợ của CN-01-08**: đổi `AVERAGE_SPEED` là ETA đổi theo
  - **★ CN-03-12** — chạy lại 3 lần cùng ngày: bảng `routes` vẫn đúng **1 dòng**, `route_stops` đúng
    **3 dòng**, không nhân đôi. Đơn được trả về `APPROVED` **trước khi** xóa tuyến cũ, nếu không đơn
    sẽ biến mất khỏi cả hai màn hình. Cả 4 lần chuyển trạng thái đều có dòng `status_history` (CBR-14)
  - **★ CN-03-03, CN-03-04** — tích chọn xe `AVAILABLE`, tổng sức chứa cộng ngay tại chỗ và **báo đỏ
    phần còn thiếu theo cả kg lẫn m³**. Ô chọn thuật toán ba dòng: đổi sang `NN + 2-opt` chạy lại ra
    **cùng 25,17 km nhưng chỉ 1 ms** so với 5 003 ms của OR-Tools — bài 3 điểm 1 xe thì lời giải tối ưu
    là hiển nhiên, chênh lệch chỉ là thời gian. Đây là chỗ demo so sánh trước hội đồng
  - **★ CN-03-07, CN-03-08** — bảng từng tuyến kèm ETA, và khối chỉ số so sánh đặt trên cùng:
    mốc A `51,02 km`, mốc B `30,96 km`, sau tối ưu `25,17 km`, giảm **50,7%** so với A và **18,7%** so với B
  - **★ CN-03-14** — đơn còn `APPROVED` trong khi đã có tuyến nháp **chính là** đơn chưa xếp được.
    Suy ra từ trạng thái nên tải lại trang vẫn còn cảnh báo, không phụ thuộc flash message
  - **★ CN-03-09 — vẽ lộ trình thật lên bản đồ.** `RouteGeometryService` gọi **OSRM Route API**
    (`/route/v1/driving/...?overview=full&geometries=geojson`) cho từng tuyến, kho → các điểm → **kho**.
    Chạy thật ra **706 điểm hình dạng** cho tuyến 3 khách, đường khép kín đúng tại kho
    `(20.94995, 105.81735)` — không phải đường thẳng nối các điểm
    - **Đảo `[kinh độ, vĩ độ]` của GeoJSON thành `[vĩ độ, kinh độ]` của Leaflet ngay ở máy chủ.**
      Quên đảo thì không báo lỗi gì cả, đường chỉ hiện ở giữa Somalia — lỗi im lặng kinh điển
    - **Lớp này cố ý KHÔNG nằm trong `service/optimization/`**: nó không tham gia giải bài toán,
      chỉ chạy sau khi tuyến đã chốt thứ tự điểm, gọi một lần cho một tuyến, **ngoài mọi vòng lặp
      thuật toán** nên không phạm điều cấm số 4
    - **Ghim đánh số theo màu tuyến**: số trên ghim chính là cột `#` của bảng kết quả bên cạnh,
      đối chiếu bảng với bản đồ không phải đếm tay
    - **Đã thử nhiều tuyến:** tạm thu nhỏ tải trọng hai xe để bắt buộc tách làm 2 tuyến →
      `TT26082101` màu `#1E77BF`, `TT26082102` màu `#C75B39`, đường 301 và 651 điểm, ghim đánh số
      lại từ 1 ở mỗi tuyến, chú thích màu hiện đủ hai biển số. **Đã trả lại tải trọng gốc 500/1000 kg**
  - **★ CN-03-11 nghiệm thu trên bản đồ:** `docker stop osrm-vietnam` rồi tải lại trang →
    đường tụt về **5 điểm** (kho + 3 khách + kho), cờ `approximate = true`, bản đồ vẽ **nét đứt**
    và màn hình hiện đúng dòng cảnh báo. Bật lại container thì quay về 706 điểm. Cố ý vẽ khác hẳn
    để nhìn là biết ngay, không được vẽ giống đường thật rồi để người xem tưởng nhầm
  - **Sửa một lỗi chỉ lộ ra sau khi phân tuyến chạy được:** bản đồ trước đây vẽ ghim từ danh sách
    đơn `APPROVED`, mà chạy xong thì đơn chuyển hết sang `ROUTED` nên **bản đồ chỉ còn mỗi cái kho**.
    Nay tách hai khối: điểm đã có tuyến đi theo `routeLinesJson` (ghim đánh số, màu tuyến), điểm
    chưa xếp được đi theo `mapPointsJson` (chấm xám rỗng ruột). Hai khối không bao giờ chồng nhau
    vì đơn đã xếp thì không còn ở trạng thái `APPROVED`
  - **★ CN-04-06, CN-04-07, CN-04-09 — `RoutePlanService`.** Đây là chỗ một tuyến đổi từ *nháp*
    sang *thật*: trước khi chốt, chạy lại thuật toán là xóa sạch; sau khi chốt, xe bị khóa và
    tài xế nhìn thấy tuyến trên điện thoại. Tách khỏi `RoutingService` vì hai việc ngược nhau —
    một bên xóa tuyến nháp để sinh lại, một bên khóa tuyến lại không cho sinh nữa
    - **Chạy thật đủ vòng đời trên 2 tuyến** ngày 21/08 (tạm thu nhỏ tải trọng để ép tách tuyến,
      **đã trả lại 1500/500/1000 kg**):
      chốt khi chưa gán tài xế → *"Chưa gán tài xế cho tuyến TT26082101, TT26082102"*;
      gán Hùng cho tuyến 1 → ô chọn của tuyến 2 **mất Hùng** (BR04-07) nhưng ô chọn tuyến 1
      **vẫn còn** Hùng; gửi thẳng `driverId=1` cho tuyến 2 → **bị chặn ở tầng service** (CBR-06)
    - **Chốt xong kiểm CSDL:** 2 tuyến `CONFIRMED`, xe 1 và 3 sang **`IN_USE`**, 2 dòng
      `notifications` **NTF-01** gửi đúng `taixe01` và `taixe03`, 2 dòng `status_history`
      `DRAFT → CONFIRMED`. Xe bị khóa **ngay lúc chốt** chứ không đợi tài xế nhận hàng (docs/01)
    - **Hủy tuyến TT26082102 kiểm CSDL:** tuyến `CANCELLED`, **xe 3 về `AVAILABLE`** còn xe 1
      vẫn `IN_USE` vì tuyến kia còn sống, **2 đơn về `APPROVED`** còn đơn của tuyến kia giữ
      `ROUTED`, có **NTF-05**, lịch sử ghi kèm lý do. Kiểm `HEX` lý do và nội dung thông báo
      đều khớp UTF-8
    - **Ba nhánh chặn khác đã thử:** đổi tài xế tuyến đã chốt → chặn (CBR-10); chốt lần hai →
      *"chưa có tuyến nháp nào để chốt"*; hủy không nhập lý do → chặn
    - **Màn hình nay hiện cả tuyến nháp lẫn tuyến đã chốt.** Trước đó chỉ lấy `DRAFT` nên bấm
      Chốt xong là **cả bảng biến mất** — lỗi chỉ lộ ra khi có nút chốt. Tuyến đã hủy thì không
      hiện nữa, bản ghi vẫn nằm trong CSDL cho báo cáo
    - Nút Chốt kế hoạch bị khóa kèm **câu giải thích vì sao**; máy chủ vẫn chặn lại bằng đúng ba
      điều kiện đó, ẩn nút chỉ để người dùng khỏi mất công (CBR-15)
  - **★ CN-04-01 → CN-04-05 — kéo thả chỉnh tay.** Kéo dòng khách để đổi thứ tự trong tuyến,
    hoặc kéo sang bảng của tuyến khác; km, kg, m³ và ETA đổi **ngay lúc kéo** (BR04-03).
    - **Trình duyệt chỉ gửi lên THỨ TỰ ĐƠN** — không gửi km, không gửi kg, không gửi ETA.
      Máy chủ tính lại bằng đúng `RouteDistanceCalculator` và `EtaCalculator` của thuật toán
      rồi mới ghi xuống. Tin con số của trình duyệt là mở đường cho bất kỳ ai sửa JavaScript
      cũng ghi được một tuyến 0 km vào báo cáo chương 4
    - **Công thức trong `routing-arrange.js` phải giống hệt bản Java, kể cả chặng cuối về kho.**
      Quên chặng đó thì con số xem trước luôn ngắn hơn con số thật, và người dùng thấy km
      "nhảy" một cái mỗi lần bấm Lưu
    - **Kiểm chứng bằng số tính tay:** tuyến 3 điểm, kéo về thứ tự nhận đơn `5 → 6 → 7` ra
      **30 956,8 m**, đúng bằng con số tính tay từ ma trận **và đúng bằng mốc B** trong bảng
      chỉ số — ba đường tính độc lập (JavaScript, Java, `MetricsService.baselineB`) cho cùng
      một số. ETA khớp từng giây: 11 009,6 m ÷ 30 km/h = 22 phút 1 giây → **08:22:01**
    - **★ CN-04-02** chuyển đơn giữa hai tuyến: cả hai tuyến tính lại kg, m³, km, ETA và cùng
      được đánh dấu `MANUAL`
    - **★ CN-04-03** kéo hết đơn sang xe nhỏ → chặn ở tầng service kèm **MSG-14**:
      *"Tuyến TT26082104 vượt quá tải trọng cho phép của xe 29F-555.66: 237.3 kg so với 200 kg"*.
      Giao diện tô đỏ tiêu đề tuyến và khóa nút Lưu/Chốt, nhưng đó chỉ là tiện nghi
    - **Hai kiểu hỏng do kéo thả đều bị chặn:** làm mất một đơn → *"danh sách đơn không khớp"*;
      một đơn nằm ở cả hai tuyến → *"có đơn bị lặp lại"*. Đây là loại lỗi không ai phát hiện
      ra cho tới lúc khách gọi đến hỏi, nên phải chặn ở máy chủ
    - **★ CN-04-04** đặt hai con số cạnh nhau: thuật toán tìm ra `25,17 km`, phương án hiện tại
      `30,96 km`, chênh lệch `Dài hơn 5,78 km`. Đổ sẵn từ máy chủ chứ không để trống chờ
      JavaScript — tắt JavaScript thì ô này vẫn phải đúng
    - **★ CN-04-05** khôi phục: chạy lại đúng bộ giải đã ghi trong bảng chỉ số, trên đúng đội xe
      đang giữ tuyến. Ra lại **25 173,7 m** và `algorithm` về `OR_TOOLS`
  - **Sửa hai lỗi thật lộ ra khi chạy thử, cả hai đều sẽ hỏng ngay trong buổi demo:**
    - **Hủy tuyến xong thì đơn không bao giờ phân tuyến lại được.** Cột `route_stops.order_id`
      đặt `UNIQUE` trên toàn bảng để giữ CBR-02, mà hủy tuyến chỉ đổi trạng thái chứ không xóa
      các điểm dừng — lần chạy sau đâm thẳng vào ràng buộc đó và chết với
      `Duplicate entry ... for key route_stops`. Nay hủy tuyến **xóa luôn các điểm dừng**: ý của
      CBR-02 là một đơn nằm trên tối đa một tuyến *đang sống*, mà chính vòng lặp ngay trên đó vừa
      trả đơn về hàng chờ. Không mất dấu vết vì lần hủy đã có trong `status_history` kèm lý do —
      và giữ lại còn sinh lỗi khác: trang tra cứu sẽ trả ETA của một tuyến đã hủy
    - **Spring tự tách chuỗi có dấu phẩy khi bind vào `List<String>`**, nên `orderIdsCsv=5,6,7`
      của một tuyến biến thành ba phần tử và hai danh sách song song lệch nhau ngay. Đổi sang
      ngăn cách bằng dấu gạch ngang. Lỗi này không báo gì rõ ràng, chỉ ra "dữ liệu không hợp lệ"
  - **Nhập nốt 2 tài xế còn thiếu theo `docs/08`** (Lê Văn Nam, Phạm Văn Tú) — trước đó danh mục
    chỉ có 1 người nên không thử được CBR-06. Nhập qua chính màn hình danh mục, kiểm `HEX(full_name)`
    khớp UTF-8. Tiện thể nghiệm thu lại rào của CN-01-03: gán nhầm tài khoản `dieuphoi` cho tài xế
    thì bị chặn vì tài khoản đó không phải vai trò `DRIVER`
  - **Phân quyền kiểm lại đủ 4 phép:** `admin` GET `/routing` ra **200 nhưng không có nút chạy**,
    POST `/routing/optimize` ra **403**; `taixe01` ra **403** cả hai. Xe `MAINTENANCE` gửi thẳng
    `vehicleIds=4` lên vẫn **bị chặn ở tầng service** chứ không chỉ ẩn khỏi ô chọn (CBR-05, CBR-15)
- **Sửa 405 thành 403** khi vai trò không đủ quyền gửi POST tới `/admin/**`. Nguyên nhân:
  `accessDeniedPage()` *chuyển tiếp* nên giữ nguyên method POST, mà `HomeController` khai `@GetMapping("/403")`.
  Đổi sang `@RequestMapping("/403")`

Tài khoản mẫu do `DataSeeder` tạo, khớp `docs/08`: `admin/admin123` · `dieuphoi/dieuphoi123` ·
`taixe01`, `taixe02`, `taixe03` cùng `taixe123`. Ba tài xế chứ không phải một, vì kịch bản demo
bước 12–14 (phương án B) cần hai tuyến chạy song song mới dựng được tình huống.

---

- **★ Tuần 9 — CN-05-01 → CN-05-05: màn hình tài xế xem được tuyến của mình.**
  - **BR05-01 chặn theo chiều ngược lại, đó là điểm mấu chốt:** không endpoint nào của
    `/driver/**` nhận `routeId` từ bên ngoài. Luôn đi từ **tài khoản đang đăng nhập → hồ sơ tài
    xế → tuyến của hôm nay**, nên **không tồn tại đường nào** trỏ tới tuyến của tài xế khác, kể
    cả khi đoán đúng id. Chặn kiểu này chắc hơn hẳn kiểu nhận id rồi mới đi kiểm chủ sở hữu
  - **Ngày lấy từ đồng hồ ứng dụng chứ không phải `CURDATE()`** — MySQL chạy trong WSL nên đồng
    hồ là UTC, lệch 7 tiếng: dùng `CURDATE()` thì từ 0h đến 7h sáng tài xế mở điện thoại ra
    không thấy tuyến nào
  - **Tuyến `DRAFT` cố ý không hiện cho tài xế.** Kế hoạch chưa chốt thì điều phối viên còn chạy
    lại thuật toán được; tài xế nhìn thấy rồi đi giao trước là giao theo một kế hoạch sắp bị xóa
  - **★ CN-05-02** xác nhận nhận hàng → tuyến `IN_PROGRESS`, ghi `departed_at`, có dòng
    `status_history`. Bấm lần hai bị chặn. `departed_at` chính là mốc để đối chiếu với ETA khi
    đo độ chính xác của mô hình ở chương 4
  - **★ CN-05-03** đúng **một** điểm được làm nổi bật (viền trái đậm), điểm đã xong mờ đi, điểm
    chưa tới hiện rút gọn. Trước khi bấm nhận hàng thì **không điểm nào** được làm nổi bật —
    đúng BR05-03, chưa rời kho thì chưa có việc gì để làm
  - **★ CN-05-04** hiện địa chỉ, người liên hệ, SĐT, danh sách hàng phải dỡ, ghi chú giao hàng,
    ETA và **số tiền phải thu in đậm**. Chạy thật: `5.400.000đ`, `Nước rửa tay 500ml x 12`
  - **★ CN-05-05** nút Gọi dùng `tel:0912345003`, nút Chỉ đường mở Google Maps bằng **tọa độ**
    `20.92279,105.84235` chứ không bằng địa chỉ — địa chỉ ở Hà Nội trả về không đúng số nhà
    (xem nợ kỹ thuật bên dưới), còn tọa độ thì luôn đúng chỗ đã ghim lúc tạo đơn
  - **Phân quyền kiểm 4 phép:** `taixe02` gõ `/routing` và `/orders` đều **403**; `dieuphoi` gõ
    `/driver/my-route` và POST `/driver/my-route/pickup` đều **403**. Tài xế không có thanh menu
  - **Đã dựng dữ liệu thật cho hôm nay** theo kịch bản `docs/08`: 3 đơn, tuyến `TT26082602`
    31,11 km giao cho Lê Văn Nam, hai đơn có `amount_to_collect > 0` để tuần này demo nhập tiền
  - Tiện thể nghiệm thu lại **BR02-08**: tạo đơn mà sửa địa chỉ giao nhưng giữ nguyên tọa độ của
    khách thì **bị chặn** — đúng như thiết kế, và đây là lần đầu chạm vào nhánh đó từ ngoài
- **★ CN-05-06 → CN-05-10: tài xế cập nhật kết quả giao.** Chạy thật trọn một tuyến 3 điểm,
  mỗi điểm một kết cục khác nhau để chạm hết các nhánh:
  - **★ CN-05-06, CN-05-07** điểm 1 giao thành công, thu **5.000.000đ** trong khi phải thu
    5.400.000đ kèm lý do → đơn `DELIVERED`, ghi `actual_arrival` và **tọa độ GPS** (BR05-08),
    có **NTF-09** báo điều phối (BR05-12)
  - **★ CN-05-08** điểm 2 thất bại `CUSTOMER_ABSENT` → đơn `FAILED`, thanh toán giữ `UNPAID`
    (BR05-13), có **NTF-03**
  - **★ CN-05-09** điểm 3 hoãn sang 28/08 → đơn `POSTPONED` và hệ thống **tự tạo đơn mới
    `MP2608280001` ở ngày mới với trạng thái `APPROVED`**, có **NTF-06**
  - **★ CN-05-10** điểm cuối xong → tuyến tự `COMPLETED`, ghi `completed_at`, **xe
    29H-123.45 về `AVAILABLE`** (BR05-09), màn hình đổi sang khối tổng kết `1/1/1` và
    `Tổng tiền đã thu 5.000.000đ`
  - **Bốn nhánh chặn đều thử ở tầng service:** nhảy cóc sang điểm 3 → *"Phải cập nhật theo đúng
    thứ tự. Điểm đang tới lượt là điểm 1"* (BR05-05); đơn phải thu mà không nhập tiền (BR05-11);
    thu khác số phải thu mà không ghi lý do (BR05-12); chọn lý do `OTHER` mà không mô tả (BR05-06);
    hoãn về ngày quá khứ (BR05-07). Giao diện có mở/ẩn ô tương ứng nhưng đó chỉ là tiện nghi
  - **Tọa độ GPS lấy MỘT LẦN lúc mở trang** chứ không lúc bấm nút: trình duyệt hỏi quyền bằng một
    hộp thoại, bắt tài xế chờ đúng lúc đang vội là hỏng thao tác. **Thiếu tọa độ không chặn việc
    cập nhật** — điện thoại có thể đang trong hầm, mà tài xế thì vẫn đang đứng trước cửa hàng
  - **Hoãn thì tạo đơn MỚI chứ không đổi ngày của đơn cũ.** Đổi ngày là mất dấu vết lần giao hụt:
    RPT-02 không còn thấy lần hoãn nào, và điểm dừng của tuyến hôm nay trỏ tới một đơn có ngày
    giao khác hẳn
  - **Định dạng tiền dùng `DecimalFormatSymbols` đặt tay** chứ không dựa vào locale của máy: máy
    cài tiếng Anh ra `5,000,000đ`, máy tiếng Việt ra `5.000.000đ` — cùng một dòng code mà hai máy
    hai kết quả là loại lỗi chỉ lộ ra khi đổi máy, đúng lúc sắp bảo vệ
- **★ CN-05-11 — lịch sử tuyến đã chạy.** Sang ngày 27/08 thì tuyến hôm qua tự rơi vào lịch sử:
  `26/08/2026 · TT26082602 · 29H-123.45 · 31,1 km · 1/3 thành công · 1 thất bại · 1 hoãn ·
  đã thu 5.000.000đ`
  - **Lọc theo `driver.id` ngay trong câu truy vấn** chứ không lấy hết rồi lọc trong Java —
    chỗ chặn chắc nhất là ở tầng dữ liệu (BR05-01). Kiểm chứng: `taixe03` mở lịch sử thấy
    **0 tuyến** và không thấy `TT26082602`; `admin` gõ `/driver/history` ra **403**
  - **Bỏ tuyến đã hủy** khỏi lịch sử: tuyến đó không được chạy ngày nào, mà sau khi hủy thì các
    điểm dừng cũng đã bị xóa nên hiện ra chỉ là một dòng `0/0` vô nghĩa
  - Ba con số để **tách bạch** chứ không gộp thành `3/3`: giao thất bại và hoãn là hai việc khác
    hẳn nhau, gộp lại thì cuối tháng không đối chiếu được với báo cáo của điều phối
- **★ CN-07-01 → CN-07-07 — trang tra cứu công khai `/track`.** Đây là **đường duy nhất của hệ
  thống mở cho người chưa đăng nhập**, nên viết theo một nguyên tắc khác hẳn các màn hình còn lại.
  - **Không bao giờ trả entity `Order` ra ngoài.** `TrackingService` đổi sang record `TrackedOrder`
    chỉ chứa đúng những trường được phép hiện. Nếu trả `Order` rồi để template tự chọn thì ngày mai
    thêm một dòng `th:text` là lộ giá trị đơn mà không ai phát hiện — **cắt ở tầng dữ liệu thì
    template có sửa cũng không lộ được, vì trong tay nó không còn số tiền nào cả**
  - **Kiểm BR07-04 bằng cách quét chính HTML trả về:** không có giá trị đơn, không có biển số xe,
    không có mã tuyến, không có tên khách khác cùng tuyến. Bốn thứ bị cấm, quét sạch cả bốn
  - **★ CN-07-04** hành trình lấy từ `status_history` nhưng **chỉ lấy thời điểm và tên trạng thái,
    bỏ cột `note`**: ghi chú là văn bản nội bộ, nó chứa những thứ như `Phan tuyen TT26082101` —
    mã tuyến và cách làm việc bên trong không phải việc của khách
  - **★ CN-07-05** chạy thật hai đơn cạnh nhau: đơn `DELIVERING` hiện *"Đơn đang trên đường giao.
    Tài xế Lê Văn Nam — 0987654322"*, đơn `DELIVERED` **không hiện gì**. Cắt ở tầng service chứ
    không để template tự ẩn — đơn giao xong hôm qua mà vẫn trả về SĐT tài xế thì ai biết mã đơn
    cũng gọi thẳng cho anh ta được, mà đó là thông tin cá nhân của nhân viên
  - **★ CN-07-06** sai mã đơn và SĐT chưa có đơn nào đều ra **cùng một câu MSG-11**. Tách hai câu
    là biến trang này thành công cụ dò xem số điện thoại nào từng mua hàng của Minh Phú (BR07-08)
  - **★ CN-07-07** chạy 25 lần liên tiếp → **bị chặn đúng ở lần thứ 21**, sang phút sau tra lại
    được ngay. Hạn mức đọc từ tham số `LOOKUP_RATE_LIMIT` nên đổi được ở màn hình cài đặt
  - **Dùng POST chứ không phải GET** cho ô tra cứu: ô nhập là **số điện thoại của khách**, để GET
    thì số đó nằm trong lịch sử trình duyệt (có thể là máy ở quán net) và trong log truy cập của
    máy chủ. Đổi lại là bấm F5 sẽ hỏi gửi lại — chấp nhận được
  - **Comment của `track.html` viết dạng `<!--/* */-->`** để Thymeleaf gỡ khỏi kết quả trả về.
    Các màn hình khác chỉ người trong công ty xem nên không sao, riêng trang này ai cũng mở được
    và bấm xem mã nguồn. Kiểm chứng: **0 dòng ghi chú nội bộ trong HTML trả về**
  - **`LookupRateLimiter` đếm trong bộ nhớ**, khóa là `IP@phút` nên sang phút mới là tự có bộ đếm
    mới, không phải dọn gì. Có chặn trên 10.000 địa chỉ rồi dọn sạch — không có chặn đó thì một
    đợt quét từ hàng chục nghìn IP làm hết bộ nhớ đúng bằng cái mà nó định chặn
- **★ CN-04-08 — gửi email báo giờ giao cho khách sau khi chốt tuyến** (BR04-11, NTF-08).
  Món cuối còn thiếu của Nhóm 4, nay **Nhóm 4 xong trọn**.
  - **Mỗi lần chốt đều ghi một dòng `notifications`, kể cả khi không gửi được.** Chạy thật một
    tuyến 2 điểm: khách có email → dòng `NOT_SENT` kèm địa chỉ; khách **không có email** → dòng
    `NOT_SENT` với `recipient_email = NULL` (BR04-12). Màn hình báo thẳng
    *"Còn 2 khách chưa báo được bằng email, cần gọi điện"* — điều phối viên biết chính xác phải
    gọi cho ai
  - **Ba trạng thái phân biệt ba tình huống khác hẳn nhau:** `SENT` đã đẩy sang SMTP ·
    `NOT_SENT` khách không có email **hoặc** hệ thống chưa cấu hình SMTP · `ERROR` có email, có
    cấu hình, nhưng gửi hỏng
  - **`ObjectProvider<JavaMailSender>` chứ không tiêm thẳng.** Spring Boot chỉ tạo bean đó khi
    có `spring.mail.host`; máy chưa khai báo SMTP mà tiêm thẳng là **ứng dụng không khởi động
    được**. Phân tuyến là chức năng lõi, không được chết chỉ vì chưa cấu hình email
  - **Chưa cấu hình SMTP thì ghi cả nội dung thư vào log**, nên kiểm chứng được ngay mà không
    cần tài khoản thật, và lúc cấu hình xong **không phải sửa một dòng code nào**. Nội dung chạy
    thật có đủ mã đơn, ngày giao, giờ dự kiến `08:38`, địa chỉ và link tra cứu
  - **Thư là văn bản thuần chứ không phải HTML**: tên khách và địa chỉ là dữ liệu người dùng
    nhập, ghép vào HTML thì phải escape từng chỗ, quên một chỗ là mở đường cho mã độc chạy trong
    hộp thư của khách
  - **Cố ý không ghi số tiền, biển số xe hay tên tài xế vào thư.** Email đi qua nhiều máy chủ
    trung gian và nằm lại trong hộp thư rất lâu nên còn dễ lộ hơn cả trang `/track` — cùng mức
    giới hạn của BR07-04 thì càng phải giữ chặt ở đây
  - **Thêm `GET /track/{mã đơn}`** để link trong thư bấm được. Đây là đường **duy nhất** của
    trang tra cứu dùng GET và **chỉ nhận mã đơn**, không nhận số điện thoại: mã đơn đi trong thư
    gửi riêng cho chính khách đó nên đặt lên thanh địa chỉ là chấp nhận được, còn số điện thoại
    thì tra ra được nhiều đơn và dễ dò tuần tự. Kiểm chứng: bấm link ra đúng đơn, sai mã ra MSG-11
  - `application.properties` **không commit mật khẩu nào**: năm dòng SMTP vẫn đóng, và khi mở thì
    đọc từ biến môi trường `MPROUTE_MAIL_USERNAME` / `MPROUTE_MAIL_PASSWORD` giống cách làm với
    mật khẩu MySQL
- **Sửa một chỗ bỏ sót của tuần 9:** đơn **chưa bao giờ đi qua trạng thái `DELIVERING`**, nhảy
  thẳng `ROUTED → DELIVERED`, lệch với luồng trạng thái ở `docs/01`. Nay tài xế bấm nhận hàng thì
  mọi đơn của tuyến chuyển sang `DELIVERING` và điểm kế tiếp chuyển sang `DELIVERING`. **Không có
  chặng này thì CN-07-05 vô nghĩa** — không lúc nào đơn ở đúng trạng thái để hiện tài xế

---

## Việc tiếp theo — TUẦN 9

1. **Xử lý sự cố** (CN-06) — nếu đuối thì cắt cả nhóm, đưa vào Hướng phát triển.
   `CheapestInsertion` đã viết sẵn từ tuần 7 nên phương án B không tốn nhiều
2. **Nghiệm thu BR07-03** — tra bằng SĐT chỉ hiện đơn 30 ngày gần nhất. Code đã lọc đúng nhưng
   **chưa thử được**: CSDL chưa có đơn nào cũ hơn 30 ngày. Thử luôn khi seed dữ liệu 10 ngày
   trước ở tuần 11
3. **Gửi thư thật một lần** để nghiệm thu trọn CN-04-08: mở năm dòng SMTP trong
   `application.properties`, đặt `MPROUTE_MAIL_USERNAME` / `MPROUTE_MAIL_PASSWORD` bằng mật khẩu
   ứng dụng của Gmail, đổi email của một khách trong danh mục thành email thật rồi chốt một tuyến.
   Hiện đã kiểm được nhánh ghi `notifications` và nội dung thư, **chưa kiểm được nhánh gửi thật**

Còn nợ của tuần 8, làm được lúc nào cũng được:

1. **Nghiệm thu CN-03-10 (MSG-08) bằng dữ liệu thật.** Code đã có và đã chặn đúng khi *không chọn xe
   nào*, nhưng **chưa dựng được tình huống tổng nhu cầu > tổng sức chứa**: xe nhỏ nhất đã 500 kg /
   6 m³ trong khi 3 đơn thử chỉ 237 kg / 0,61 m³. Tạo một đơn to lúc seed dữ liệu tuần 11 rồi thử
2. **Nghiệm thu nhánh `approximate_distance` của chính bảng `routes`**: tắt `osrm-vietnam` rồi bấm
   Chạy tối ưu **với tập điểm chưa có trong `distance_matrix`**. Lần thử vừa rồi chỉ chứng minh được
   phần bản đồ vì ma trận khoảng cách của ba điểm đó đã nằm sẵn trong bộ nhớ đệm

Hai việc còn nợ của tuần 7:
- **Đo lại `HAVERSINE_FACTOR` bằng số liệu thật.** Hiện để 1.3, đo ở tuần 5 thấy đang **ước lượng
  thiếu ~24%** trên cặp kho → 91 Chùa Bộc. Nay bảng `distance_matrix` đã có nhiều cặp điểm OSRM thật
  thì tính lại được bằng một câu truy vấn
- ~~Ghi lại ETA điểm cuối từng tuyến~~ — **đã có số**: tuyến 3 điểm ngày 21/08 tới điểm cuối lúc
  **09:09**, còn sớm hơn mốc "trước 11h" khá nhiều. Câu trả lời cho vấn đáp là: với quy mô hiện tại
  của Minh Phú thì ràng buộc khung giờ chưa bị vi phạm, nên chưa đưa vào mô hình

Còn nợ một phép nghiệm thu của BR02-11: đơn `ROUTED` mới ra đúng chữ MSG-07.
**Nay đã có đơn `ROUTED` thật trong CSDL** (đơn 5, 6, 7 ngày 21/08) nên thử được ngay

## Ba việc còn nợ, phải quay lại làm

| Việc | Khi nào làm được | Vì sao chưa làm |
|---|---|---|
| ~~Nghiệm thu thật CN-01-08~~ | ~~Tuần 8~~ | **Xong.** ETA của tuyến thật khớp phép tính tay tới từng giây bằng đúng ba tham số `AVERAGE_SPEED`, `LOADING_TIME`, `DEPARTURE_TIME` đọc từ bảng — đổi tham số là ETA đổi theo |
| **Thử tay phần bản đồ CN-01-05** | Bất cứ lúc nào | Đã kiểm tự động phía máy chủ (27 phép) nhưng **chưa ai bấm thử thật**: bấm vào bản đồ để ghim, kéo ghim, chọn dòng trong danh sách nhiều kết quả. Phần JavaScript này chỉ chạy trong trình duyệt |
| **Thử tay `don-hang.js` (CN-02-03)** | Bất cứ lúc nào | Cùng lý do: tổng kg/m³/tiền cập nhật ngay khi gõ, thêm/xóa dòng, chọn khách tự điền địa chỉ — toàn bộ chỉ chạy trong trình duyệt. Phía máy chủ đã kiểm xong, cú pháp JavaScript đã kiểm bằng `node --check`, nhưng **chưa ai gõ thử thật**. Thử luôn khối nhập lý do hủy (dùng `collapse` của Bootstrap) |

---

## Cần quyết định

| Việc | Tình hình |
|---|---|
| **Cắt phạm vi — quyết trước tuần 8** | 86 chức năng bắt buộc / 12 tuần / một người. Ước lượng thật là 18–24 tuần. **Cắt chủ động ngay chứ đừng đợi tuần 9 mới cắt trong hoảng loạn.** Cắt hẳn RPT-01, RPT-02 và toàn bộ mục ○ từ bây giờ |
| **Mâu thuẫn về thứ tự cắt** | `docs/00` xếp UC06 (sự cố) vào diện cắt sớm, nhưng kịch bản demo lại lấy bước 13 làm điểm nhấn. Dung hòa: giữ UC06 nhưng làm **phương án A và C trước**, phương án B (Cheapest Insertion) làm sau nếu còn thời gian |
| Thư viện xuất Excel | CN-08-05 cần Apache POI. Thêm vào `pom.xml` khi làm tới tuần 10 |
| ~~CN-02-10 mâu thuẫn với ma trận phân quyền~~ | **Đã quyết:** mở riêng đường thêm mới khách cho điều phối viên, bó hẹp hết mức, ghi vào `docs/09` mục **A4**. Muốn đổi ý thì gỡ đúng một endpoint và một hộp thoại |

---

## Đã biết trước, cố ý chưa sửa

| Vấn đề | Khi nào lộ ra | Vì sao chưa sửa |
|---|---|---|
| NN để sót đơn dù tổng sức chứa đủ | Tuần 8, lần đầu chạy trên dữ liệu mẫu | **Cố ý giữ.** Là điểm nhấn demo bước 5a và câu trả lời vấn đáp mạnh nhất. Kịch bản ở `docs/08` |
| Trạng thái xe là cờ toàn cục, không theo ngày | Chỉ lộ nếu thử chốt tuyến cho hai ngày liên tiếp | Sửa đúng phải đổi CBR-05 và tầng service. Đã ghi vào hạn chế `docs/07` 4.7 |
| Ghi chú "chỉ nhận buổi sáng trước 11h" mà thuật toán không ràng buộc khung giờ | Tuần 8 khi nhìn ETA thật | Giữ vì phản ánh khảo sát thật. Cần chạy thử và **ghi lại ETA điểm cuối từng tuyến** để trả lời bằng số |
| **Thu thiếu tiền mà đơn vẫn sang `PAID`** | Tuần 9, đơn `MP2608260002` thu 5.000.000đ trong khi phải thu 5.400.000đ | `docs/01` chỉ có BR05-14 cho đơn công nợ, **không có luật nào cho việc thu thiếu**. Tự nghĩ ra luật là thêm phạm vi ngoài `docs/04`. Số thực thu vẫn nằm ở `route_stops.collected_amount`, lý do nằm ở `collection_note`, và điều phối đã nhận NTF-09 — nên không mất dữ liệu, chỉ là cột `payment_status` chưa phản ánh. **Nếu hội đồng hỏi thì trả lời đúng như vậy**; muốn sửa thì thêm một luật vào `docs/01` trước |

---

## Nợ kỹ thuật

- Menu trỏ tới `/tracking`, `/incidents`, `/reports` chưa có Controller → bấm vào ra 404.
  `/orders` và `/routing` đã có. Hết tuần 9–10 là tự hết
- **`ddl-auto=update` KHÔNG cập nhật ràng buộc `CHECK` khi thêm giá trị enum.** Hibernate sinh
  `CHECK (algorithm IN (...))` lúc **tạo bảng** rồi không bao giờ động vào nữa. Thêm `OR_TOOLS` vào
  `AlgorithmType` từ tuần 7 nhưng mãi tuần 8 mới lộ, vì đây là lần đầu thực sự ghi bảng `routes` —
  lỗi ra `Check constraint 'routes_chk_1' is violated` chứ không nhắc gì tới enum. Đã sửa tay hai bảng
  `routes` và `optimization_results`:

  ```sql
  ALTER TABLE routes DROP CHECK routes_chk_1;
  ALTER TABLE routes ADD CONSTRAINT routes_chk_1
      CHECK (algorithm IN ('OR_TOOLS','NN','NN_2OPT','MANUAL'));
  ```

  **Lần sau thêm giá trị enum bất kỳ thì phải sửa `CHECK` tay**, hoặc xóa bảng cho Hibernate tạo lại.
  Máy khác dựng lại CSDL từ đầu thì không gặp lỗi này — càng dễ tưởng là lỗi của riêng máy mình
- **Mật khẩu `dieuphoi` và `taixe01` đã bị đổi khỏi giá trị mặc định của `DataSeeder`.**
  `seedUser()` chỉ tạo khi thiếu chứ không ghi đè, nên khởi động lại không đưa về mặc định.
  `docs/08` vẫn ghi `dieuphoi123` và `taixe123` — **trước ngày bảo vệ phải đưa về đúng tài liệu**,
  hoặc sửa `docs/08`. `admin`, `taixe02`, `taixe03` vẫn đúng mật khẩu mặc định.
  Kịch bản demo đọc mật khẩu từ `docs/08`, lệch là hỏng ngay bước đăng nhập đầu tiên
- Trong CSDL đang có **8 đơn thử** do các lần kiểm thử tạo ra. Ngày 21/08 hiện có tuyến
  `TT26082101` **đã chốt** (xe 29H-123.45 đang `IN_USE`, tài xế Nguyễn Văn Hùng) và 2 đơn quay
  lại hàng chờ sau khi hủy `TT26082102`. Hủy nốt tuyến đó là xe về `AVAILABLE`. Giữ lại để bấm thử;
  xóa lúc seed lại dữ liệu ở tuần 11. Ba khách thử của CN-02-10 (`#22`, `#23`, `#24`) đã chuyển
  **`INACTIVE`** nên không còn hiện trong ô chọn khách lúc tạo đơn
- **Hai container Docker phải bật trước khi chạy ứng dụng**, cả hai đều không phải service Windows:

  | Container | Việc | Mở lại |
  |---|---|---|
  | `mysql-8.0.44` | CSDL. Tắt là app báo `Communications link failure` | `docker start mysql-8.0.44` |
  | `osrm-vietnam` | Khoảng cách đường thật, cổng 5000, ~2,4 GB RAM | `docker start osrm-vietnam` |

  Quên bật `osrm-vietnam` thì **app vẫn chạy**, chỉ lùi về Haversine và hiện MSG-09 — đúng thiết kế,
  nhưng số km sẽ là xấp xỉ, đừng lấy số đó đưa vào báo cáo
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
- `application.properties` nay đọc mật khẩu MySQL từ biến môi trường `MPROUTE_DB_PASSWORD`,
  mặc định `gbao` cho máy này. **Chuỗi `gbao` vẫn còn trong lịch sử git** ở commit đầu `c55b2d3` —
  kho đang **riêng tư** nên không sao, nhưng đổi sang công khai thì phải xóa khỏi lịch sử trước
- Máy này lưu sẵn thông tin đăng nhập GitHub của **hai tài khoản**. Remote cố ý ghi là
  `https://anh05022005@github.com/...` — có `anh05022005@` ở giữa thì Git mới lấy đúng tài khoản.
  Xóa phần đó đi là đẩy bằng tài khoản kia, báo lỗi 403
