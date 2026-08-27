# Nhật ký làm việc

> Ghi lại **đã làm gì và vì sao làm thế** qua từng tuần, kèm những con số đã chạy thật.
> Tách khỏi `TIEN-DO.md` vì file đó được nạp vào mọi phiên làm việc, còn file này chỉ mở ra
> khi cần tra — lúc viết chương 3, chương 4 của luận văn, hoặc khi hội đồng hỏi "vì sao em
> làm thế này".
>
> **Không xóa dòng nào ở đây.** Mỗi mục là một câu trả lời sẵn cho một câu hỏi vấn đáp.

**Trạng thái hiện tại và việc tiếp theo:** xem `TIEN-DO.md`.

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
- **★ CN-06-01 → CN-06-05, CN-06-13 — tài xế báo sự cố trên đường.** Nửa đầu Nhóm 6.
  - **Hai mức độ dẫn tới hai đường đi hoàn toàn khác nhau**, và đó là toàn bộ cái khó của nhóm này:
    `CAN_CONTINUE` hệ thống **tự xử lý ngay** (cộng phút chậm vào ETA, báo khách, đóng sự cố lại);
    `CANNOT_CONTINUE` hệ thống **không tự quyết** mà chờ điều phối viên chọn phương án
  - **Vì sao không để máy tự chọn cả trường hợp nặng:** ba phương án có chi phí rất gần nhau mà
    hậu quả thì khác hẳn — điều thêm một xe là thêm một tài xế phải làm ngoài giờ, còn hoãn sang
    hôm sau là một cuộc gọi xin lỗi khách. **Máy không biết hôm nay ai rảnh**
  - **★ CN-06-04** chạy thật: báo tắc đường chậm 30 phút → ETA điểm còn lại lùi từ **08:12:40
    sang 08:42:40**, đúng 30 phút; khách nhận **NTF-07** kèm giờ mới `08:42`; sự cố đóng luôn
    `RESOLVED` vì không còn gì cho điều phối quyết
  - **Thư báo ETA mới gửi SAU khi đã tính lại**, không phải trước — gửi trước thì khách nhận
    được đúng cái giờ cũ mà hệ thống vừa bỏ đi
  - **★ CN-06-03, BR06-02** không lấy được GPS thì dùng tọa độ **điểm giao gần nhất đã xong** và
    đánh dấu `estimated_location = true`; chưa giao điểm nào thì lấy tọa độ kho. Chạy thật cả hai
    nhánh: lần đầu không gửi tọa độ → ghi tọa độ kho, cờ ước lượng bật; lần sau có GPS → ghi đúng
    tọa độ, cờ tắt. **Không để trống**: điều phối cần biết xe đang ở đâu để điều xe cứu hộ tới,
    một tọa độ gần đúng vẫn hơn hẳn không có gì
  - **★ CN-06-05** thông báo khẩn cho điều phối gộp đủ ba thứ vào **một dòng đọc được ngay**:
    *"Tuyến TT26082701 báo sự cố tắc đường (vẫn tiếp tục được), chậm 30 phút. Vị trí 20.94970,
    105.81720 (ước lượng). Còn 1 điểm chưa giao: Cửa hàng Bảo Châu."*
  - **★ CN-06-13, BR06-14** báo xe hỏng → tuyến sang `INCIDENT`, **xe 29H-123.45 sang
    `MAINTENANCE`** và **biến mất khỏi danh sách xe khả dụng** của màn hình phân tuyến — kiểm
    chứng bằng cách mở màn hình phân tuyến ngày khác, ô chọn xe trống trơn.
    **Chuyển ngay chứ không đợi chọn xong phương án**: giữa lúc đó có thể có người chạy phân tuyến
    cho ngày mai và hệ thống lại xếp đúng chiếc xe đang nằm đường vào kế hoạch
  - **Tuyến sang `INCIDENT` chứ không phải `CANCELLED`**: các điểm đã giao vẫn giữ nguyên kết quả,
    và tuyến sẽ quay lại `IN_PROGRESS` nếu điều phối chọn phương án điều xe cứu hộ
  - **Ba nhánh chặn đã thử:** không chọn loại; chọn `OTHER` mà không mô tả (BR06-01); chọn
    `CAN_CONTINUE` mà không nhập số phút (BR06-04)
  - Nút **BÁO SỰ CỐ** viền đỏ đặt cuối màn hình tài xế, **chỉ hiện khi tuyến `IN_PROGRESS`** —
    trước đó xe còn ở kho nên chưa có gì trên đường để báo, sau đó thì tuyến đã xong
- **★ CN-06-06 → CN-06-12 — màn hình xử lý sự cố. Nhóm 6 xong trọn, và đây là chức năng
  cuối cùng của 9 nhóm chức năng bắt buộc.**
  - **Km phát sinh là thước đo duy nhất dùng để xếp hạng**, và nó phải được tính giống nhau cho
    cả ba phương án — **mọi con số đều từ MỘT ma trận chung**, nạp một lần cho toàn bộ điểm liên
    quan. Mỗi phương án một ma trận riêng thì không so sánh được với nhau, mà bảng xếp hạng lại
    chính là sản phẩm của màn hình này
  - **Chạy thật, cả ba phương án cùng lúc**, xếp đúng thứ tự:

    ```
    1. Chen vao tuyen TT26082702 — 29F-555.66      6,44 km   khong can huy dong xe moi
    2. Dieu xe 29B-222.11 ra sang hang            18,20 km   xe 29B-222.11 va mot tai xe ranh
    3. Hoan 1 diem con lai sang ngay mai           0,00 km   khach khong nhan duoc hang hom nay
    ```

    **Phương án C có km bằng 0 mà vẫn xếp cuối** (BR06-08, BR06-11) — không phát sinh km nào
    nhưng khách không nhận được hàng hôm nay, cái giá đó không đo bằng km được
  - **★ CN-06-11 áp dụng phương án B, con số dự đoán khớp chính xác con số thật:** trước khi áp
    dụng màn hình báo phát sinh **6,44 km**; sau khi áp dụng tuyến nhận đi từ `16 637,1 m` lên
    `23 079,2 m`, đúng **6 442,1 m**. Điểm dừng chuyển sang tuyến mới, tuyến hỏng `COMPLETED`,
    ETA tính lại (`08:16:38` và `08:48:30`), có dòng `status_history`, sự cố `RESOLVED` kèm
    `extra_distance`
  - **★ CN-06-12** khách bị ảnh hưởng nhận **NTF-07** kèm giờ mới và lý do *"xe gặp sự cố trên
    đường"*
  - **Chuyển điểm dừng sang tuyến khác chứ không tạo điểm mới** (phương án B): cột
    `route_stops.order_id` đặt `UNIQUE` toàn bảng (CBR-02), tạo dòng mới mà không xóa dòng cũ là
    đâm thẳng vào ràng buộc đó — đúng cái bẫy đã làm hỏng chức năng hủy tuyến ở tuần 8
  - **Phương án C dùng lại đúng hàm hoãn của màn hình tài xế** (CN-05-09) chứ không viết đường
    hoãn thứ hai. Đã tách phần lõi ra `movePostponed()`: hai đường vào khác nhau (tài xế bấm nút,
    điều phối chọn phương án) nhưng cùng một phần thân, nên không bao giờ lệch nhau
  - **`EtaCalculator` thêm tham số điểm xuất phát** thay vì viết hàm tính giờ thứ hai. Xe cứu hộ
    xuất phát từ điểm sự cố, xe được chèn thêm điểm thì đang ở giữa đường — cả hai đều không bắt
    đầu từ kho, nhưng **công thức tính giờ không đổi**, chỉ khác mốc xuất phát
  - **Chỉ hiện phương án khả thi** (BR06-09): xe không đủ sức chứa hoặc tuyến ứng viên không chèn
    được hết điểm đều bị loại ngay ở tầng service. Điều phối viên đang xử lý sự cố, không có thời
    gian mở từng cái ra mới biết không dùng được
  - **Phương án B phải chèn được HẾT** thì mới kể là khả thi — chèn được ba trong bốn điểm không
    giúp gì: điểm còn lại vẫn không ai giao, mà điều phối lại tưởng đã xong
  - **Phân quyền:** `admin` xem được nhưng POST áp dụng ra **403**; `taixe02` ra **403** cả hai
    đường. Áp dụng lần hai trên sự cố đã xử lý bị chặn
  - **Sửa hai lỗi thật lộ ra khi chạy thử:**
    - `generateOptions()` để `@Transactional(readOnly = true)` nhưng `loadMatrix()` **có ghi** —
      nó lưu các ô khoảng cách vừa lấy từ OSRM vào bộ nhớ đệm (CBR-12). MySQL từ chối thẳng với
      `Connection is read-only`, màn hình sự cố ra **500** đúng lúc cần nó nhất
    - `th:if="${a or b}"` trong `layout.html` làm **cả trang danh sách sự cố hỏng**: màn hình nào
      không đặt cờ nào thì biến đó là `null`, mà SpEL không đổi được `null` sang boolean
      (`EL1001E`). Phải so sánh `!= null` tường minh
- **Sửa một chỗ bỏ sót của tuần 9:** đơn **chưa bao giờ đi qua trạng thái `DELIVERING`**, nhảy
  thẳng `ROUTED → DELIVERED`, lệch với luồng trạng thái ở `docs/01`. Nay tài xế bấm nhận hàng thì
  mọi đơn của tuyến chuyển sang `DELIVERING` và điểm kế tiếp chuyển sang `DELIVERING`. **Không có
  chặng này thì CN-07-05 vô nghĩa** — không lúc nào đơn ở đúng trạng thái để hiện tài xế

---

## Tuần 10 — Báo cáo và thực nghiệm

### RPT-03 Báo cáo hiệu quả tối ưu (CN-08-03, CN-08-04)

- **★ CN-08-03** — `/reports/optimization`: bảng theo ngày (số đơn, số tuyến, mốc A, mốc B, sau
  tối ưu, giảm so với A, giảm so với B, số chuyến tiết kiệm), dòng toàn kỳ, biểu đồ đường.
  Chạy thật trên dữ liệu đang có: **4 ngày, 8 đơn, giảm 34,4% so với mốc A**, 6,4% so với mốc B,
  tiết kiệm 4 chuyến. Kiểm tay: 144,7 km → 94,9 km
- **Mỗi ngày chỉ lấy lần chạy gần nhất.** Ngày 21/08 có **9 dòng** trong `optimization_results`
  vì hôm đó chạy đi chạy lại để thử. Cả 9 đều là số liệu thật, nhưng chỉ lần cuối mới là kế
  hoạch được chốt và đem giao — nên báo cáo lấy `MAX(id)` của từng ngày
- **Phần trăm toàn kỳ tính từ TỔNG quãng đường**, không phải trung bình cộng phần trăm từng
  ngày. Trung bình cộng coi ngày 3 đơn nặng ngang ngày 15 đơn, không đúng nghĩa *"cả kỳ giảm
  bao nhiêu"*. Đây là câu hỏi vấn đáp dễ bị hỏi nhất của màn hình này, đã ghi sẵn ngay trên
  giao diện
- **★ CN-08-04** hai nhánh lọc đều thử được thật:
  - **BR08-04** — ngày 28/08 chỉ còn tuyến `CANCELLED` → **bị loại khỏi trung bình**, nhưng vẫn
    liệt kê ngày đó ra cuối trang. Loại âm thầm là giấu dữ liệu
  - **BR08-03** — tạm bật cờ `approximate_distance` một dòng: dòng đó **nhảy sang bảng riêng**
    kèm ghi chú, số ngày tính vào trung bình giảm 4 → 3. Đã trả lại nguyên trạng ngay sau khi thử
- **BR08-01** — chọn 01/2024 → 08/2026 thì tự rút về 365 ngày kèm thông báo, thay vì từ chối
  hiển thị. Gõ nhầm năm vẫn xem được báo cáo và vẫn biết hệ thống đã cắt
- **Biểu đồ vẽ thẳng bằng SVG**, toạ độ tính ở `ReportService`. Không nạp Chart.js: hai đường và
  vài chục điểm không đáng đổi lấy 200 KB phải tải về, một thư viện nữa phải giải thích khi vấn
  đáp, và một thứ nữa hỏng khi mất mạng. Biểu đồ vẽ **mức giảm** chứ không vẽ quãng đường — quãng
  đường lên xuống theo số đơn từng ngày nên chỉ phản ánh hôm đó nhiều hàng hay ít hàng
- **CBR-15** — `taixe02` gõ thẳng `/reports/optimization` ra **trang 403**, không phải trang trắng

### Benchmark Augerat (CN-09-02, CN-09-03)

- **Tải 10 bài từ CVRPLIB** về `src/main/resources/data/augerat/`. Địa chỉ cũ
  `vrp.galgos.inf.puc-rio.br` đã chuyển sang `galgos.inf.puc-rio.br/cvrplib`, tải theo id chứ
  không theo tên file. **Đã đối chiếu lời giải tối ưu trên trang web với `docs/07`: khớp cả 10**
- **★ CN-09-01/02** `VrpFileReader` + `BenchmarkRunner` chạy bằng `main()`, không cần web, không
  cần MySQL, không cần OSRM. Bảng kết quả đầy đủ ở `docs/07` mục "Kết quả đã chạy thật"
- **Số chốt của chương 4:** gap trung bình **NN 39,6% · NN+2-opt 33,4% · OR-Tools 1,8%**
- **★ Bằng chứng mạnh nhất rằng cách đo đúng: OR-Tools ra ĐÚNG BẰNG lời giải tối ưu đã công bố ở
  4/10 bài** — 784, 955, 741, 458, không lệch một đơn vị. Con số chỉ khớp chính xác khi cả ba
  thứ cùng đúng: quy ước làm tròn EUC_2D, kho ở điểm số 1, và **tổng quãng đường có tính chặng
  cuối về kho** (CBR-01). Thiếu chặng về kho thì mọi dòng đều nhỏ hơn tối ưu, không đời nào khớp
- **NN+2-opt ra 33,4%, cao hơn khoảng 10–25% mà bản thiết kế dự kiến.** Không giấu con số này:
  lý do là 2-opt chỉ gỡ **thứ tự bên trong một tuyến**, mà các bài này chỉ 6–9 điểm mỗi tuyến nên
  gần như không còn gì để gỡ — đo riêng thì 2-opt chỉ cải thiện **4,3%**. Phần sai số lớn còn lại
  nằm ở **khâu chia điểm cho xe**, đúng thứ mà 2-opt theo định nghĩa không đụng tới
- **P-n76-k5 cải thiện 14,8%, gấp ba lần trung bình** vì nó có nhiều điểm trên mỗi tuyến nhất
  (75 điểm / 5 xe = 15 điểm mỗi tuyến). Đây là câu trả lời có số cho *"2-opt có đáng làm không"*:
  càng nhiều điểm trên một tuyến thì càng đáng
- **A-n45-k6 là bài duy nhất NN bỏ sót điểm**, và đó không phải lỗi: bài này đòi lấp đầy **98,8%**
  đội xe (nhu cầu 593 / sức chứa 600), tức phải xếp thùng gần như hoàn hảo. Thuật toán tham lam
  về nguyên tắc không làm được — đúng điểm yếu của NN mà kịch bản demo `docs/08` dùng tới. Hai ô
  đó đánh dấu `*` và **không tính vào trung bình**: bên bỏ bớt điểm thì tuyến ngắn hơn là đương
  nhiên, đem so là kết luận ngược
- **Bỏ cấu hình "thứ tự ngẫu nhiên"** của bản thiết kế: `CN-09-03` chỉ yêu cầu `NN` và `NN_2OPT`,
  còn vai trò "mốc dưới" thì **mốc B** đã giữ bằng một cách đáng tin hơn — thứ tự nhận đơn là
  cách người điều phối thật sự làm, thứ tự ngẫu nhiên thì không ai làm bao giờ
- **Hai test thêm cho `VrpFileReader`** (ngoài 14 test bắt buộc). Lý do: quy ước làm tròn EUC_2D
  sai thì **không bao giờ báo lỗi** — chương trình vẫn chạy, vẫn in ra bảng, chỉ có mọi con số
  gap là sai. Test kiểm tay `d(kho, điểm 2) = round(√(14² + 32²)) = round(34,93) = 35`

### Seed 10 ngày lịch sử cho RPT-03 (docs/08)

- **`HistorySeeder` chạy đúng pipeline thật**, không chèn số vào `optimization_results`: tạo đơn
  → duyệt → chạy OR-Tools trên ma trận OSRM → gán tài xế → chốt kế hoạch → giao hàng. Mọi con số
  của báo cáo do chính thuật toán tính ra. Chèn thẳng vài dòng "nhìn cho hợp lý" vào bảng đó là
  đúng cái `docs/07` mở đầu bằng lời cảnh báo — hội đồng chỉ cần hỏi *"số này đo ở đâu"* là hỏng
- **Kết quả:** 10 ngày mới (13/08–25/08), 131 đơn, mỗi ngày 2 tuyến 6–7 điểm.
  **RPT-03 nay có 14 ngày, 139 đơn, giảm 65,9% so với mốc A và 33,1% so với mốc B, tiết kiệm
  114 chuyến xe**
- **★ Khẳng định được điều `docs/08` cảnh báo, bằng số của chính mình:** dữ liệu cũ 3 đơn/ngày
  cho mốc B **0–18%**, dữ liệu 12–15 đơn/ngày cho **22,6–44,6%**. Tuyến 2 điểm thì mốc B đúng
  bằng 0 vì ma trận đối xứng, không phải code sai. Đây là câu trả lời vấn đáp có số cho
  *"quy mô bài toán phải đủ lớn thì tối ưu thứ tự mới có ý nghĩa"*
- **Mốc A ra 65,9% chứ không phải 40–50% như dự kiến**, vì `baselineA = Σ 2 × d(kho, điểm)` cộng
  thêm trọn một số hạng cho mỗi đơn, còn tuyến tối ưu gom nhiều đơn vào một vòng nên dài thêm rất
  ít. Càng nhiều đơn thì hai bên càng giãn. **Khi vấn đáp phải gắn con số với quy mô** — nói
  trống "hệ thống giảm 66%" là chỗ dễ bị bẻ nhất
- **Bỏ qua ngày đã có đơn** (17/08, 21/08, 26/08 do các lần kiểm thử trước tạo ra) rồi lùi xa
  thêm cho đủ 10 ngày sạch, thay vì ghi đè. Ghi đè là mất mấy tình huống đang phục vụ phép thử
  khác — tuyến đang chạy, sự cố đã xử lý
- **Ba xe mượn tạm sang `AVAILABLE` rồi trả lại trạng thái cũ trong khối `finally`.** Phân tuyến
  chỉ nhận xe `AVAILABLE` (CBR-05), mà ba trong bốn xe đang `MAINTENANCE`/`IN_USE` là dàn sẵn cho
  kịch bản demo bước 2 và 5b. Trả trong `finally` để lỗi giữa chừng cũng không làm hỏng kịch bản
- **Ngân sách 75% sức chứa đội xe mỗi ngày.** Lấp gần đầy thì bài toán thành xếp thùng gần hoàn
  hảo, OR-Tools phải bỏ lại vài đơn, và `confirmPlan` từ chối chốt vì còn đơn chưa xếp (đúng theo
  CN-03-14) — ngày đó sẽ treo giữa chừng
- **Không gọi được `DeliveryService`** cho ngày đã qua: mọi hàm của nó bắt đầu bằng *"tuyến của
  tài xế đang đăng nhập TRONG NGÀY HÔM NAY"* (BR05-01). Các bước chuyển trạng thái trong seeder
  đi theo đúng `confirmPickup` / `markDelivered` / `markFailed` / `finishRouteIfDone`, có ghi chú
  chéo ở cả hai nơi — sửa luồng trạng thái bên kia thì phải sửa cả bên này
- **Một chỗ không thật được, đã cân nhắc:** `status_history.changed_at` ghi thời điểm chạy seed
  chứ không phải thời điểm lịch sử, vì `StatusHistoryService` luôn đặt `LocalDateTime.now()` và
  sửa chữ ký của một lớp dùng chung chỉ để phục vụ seed thì không đáng. Các mốc **thuộc về nghiệp
  vụ** (`departed_at`, `completed_at`, `actual_arrival`) đều đặt đúng ngày lịch sử
- **Tỷ lệ lấp đầy ra 97–99% kg nhưng chỉ 37–52% m³** — đúng đặc thù hàng của Minh Phú: nước giặt
  và nước lau sàn nặng mà gọn, nên xe đầy tải trọng trước khi đầy thùng. Chính là lý do phải kiểm
  **cả hai** ràng buộc chứ không chỉ một (CBR-03, CBR-04)

### RPT-01, RPT-02 và đo lại hệ số Haversine

- **★ CN-08-01 RPT-01** `/reports/orders` — số đơn theo **ngày/tuần/tháng** (chọn được), biểu đồ
  cột, và bảng số thùng theo từng sản phẩm. Chạy thật trên tháng 8: **142 đơn, 1 863 thùng**;
  bông tẩy trang nhiều thùng nhất (501) nhưng nước giặt nặng nhất (8 557 kg trên 398 thùng)
- **★ CN-08-02 RPT-02** `/reports/delivery` — **tỷ lệ giao thành công 92,1%** (129/140), biểu đồ
  tròn, và phân tích lý do thất bại: sai địa chỉ 2, khách không có mặt 1, khách từ chối 1
- **Mẫu số của tỷ lệ là đơn ĐÃ PHÂN TUYẾN, không phải mọi đơn trong kỳ** (BR08-06). Đơn còn chờ
  duyệt hay đã hủy thì chưa bao giờ lên xe — đếm vào mẫu số là tự hạ tỷ lệ của mình xuống vì một
  lý do không liên quan đến việc giao hàng. **Đã viết công thức thẳng lên màn hình**, vì đây đúng
  là câu hội đồng sẽ hỏi khi thấy con số 92%
- **Lý do thất bại đếm trên `route_stops`, không đếm trên `orders`.** Lý do thuộc về *lần giao*:
  một đơn hoãn rồi giao lại và lại hỏng là hai lần thất bại với hai lý do, mà cột trạng thái của
  đơn chỉ giữ được cái cuối. Đây cũng chính là lý do màn hình tài xế **bắt chọn** lý do từ danh
  sách thay vì gõ tự do (BR05-06) — gõ tự do thì không gom nhóm được
- **Gộp tuần/tháng làm ở tầng service chứ không trong SQL.** Hàm lấy số tuần là hàm riêng của
  từng hệ quản trị (`WEEK()` của MySQL, `EXTRACT` của Postgres), viết vào JPQL là khóa đồ án vào
  MySQL. CSDL gộp sẵn theo **ngày** — tối đa 365 dòng — rồi gộp tiếp trong bộ nhớ. Dùng
  `WeekFields.ISO` chứ không dùng mặc định của máy: mặc định đổi theo Locale, máy cài tiếng Anh
  Mỹ thì tuần bắt đầu từ Chủ nhật và số tuần lệch đi một
- **Biểu đồ tròn vẽ bằng `stroke-dasharray` trên các vòng tròn chồng lên nhau**, không vẽ bằng
  cung tròn `<path>`: cung tròn phải tính sin/cos cho từng đầu mút và phải đổi cờ khi miếng lớn
  hơn nửa đường tròn, sai một dấu là miếng bánh lật ngược. Cách dasharray chỉ cần một phép nhân
  và không có trường hợp đặc biệt nào. Kiểm lại: tổng bốn miếng ra **502,65** đúng bằng chu vi
  `2π × 80`
- **★ Đo lại `HAVERSINE_FACTOR`: 1.3 → 1.45.** Đo trên **264 cặp điểm OSRM thật** đang nằm trong
  `distance_matrix`. Lấy **tổng quãng đường OSRM chia tổng quãng đường chim bay** ra 1,453; lấy
  trung bình các tỷ lệ thì ra 1,499. **Chọn cách chia tổng** vì hệ số này dùng để ước lượng TỔNG
  quãng đường tuyến, mà các cặp điểm gần nhau có tỷ lệ rất cao (ngõ nhỏ, đường một chiều) lại
  đóng góp ít vào tổng. Tỷ lệ thấp nhất 1,035, cao nhất 3,066
- Ba báo cáo dùng chung một thanh tab và **mang theo khoảng ngày khi đổi tab** — đang xem tháng 8
  mà bấm sang báo cáo khác lại nhảy về mặc định 30 ngày thì lần nào cũng phải chọn lại
- **Phân quyền:** `taixe02` gõ cả ba đường dẫn đều ra **403**
