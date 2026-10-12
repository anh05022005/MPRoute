# 09 — Thay đổi so với đặc tả ban đầu

> **Mục đích của file này:** hội đồng hay hỏi *"vì sao em làm khác đặc tả ban đầu"*. Có sẵn văn bản
> này thì trả lời được bằng lý do kỹ thuật cụ thể thay vì nói chung chung.
>
> **Phần B là phần ăn điểm** — nó cho thấy sinh viên tự soát ra lỗi trong chính thiết kế của mình
> trước khi viết code, chứ không phải sửa chữa chắp vá khi chạy hỏng.

**Cập nhật lần cuối:** 11/08/2026

---

## A. Đổi quy ước — theo quyết định của người làm đồ án

### A1. Bỏ luật viết code "dễ đọc bắt buộc"

Bản đặc tả đầu tiên cấm Lombok, cấm Stream API, bắt dùng vòng `for` thuần kể cả ngoài phần thuật
toán, với lý do "để giảng lại được trước hội đồng".

Nay viết như một lập trình viên Java bình thường. Chỉ còn **ba điều cấm**, và cả ba đều **không phải
quy tắc phong cách** mà là đúng/sai:

| Cấm | Vì sao |
|---|---|
| Import OR-Tools vào `optimization/heuristic/` | Package đó là bản tự cài đặt dùng làm mốc so sánh. Lẫn thư viện vào là mất ý nghĩa so sánh. Xem A3 |
| `float` ở bất kỳ đâu | Tiền dùng `BigDecimal`, tọa độ và khoảng cách dùng `double` |
| Gọi OSRM / Repository trong vòng lặp thuật toán | 2-opt gọi hàm khoảng cách hàng nghìn lần, mỗi lần một HTTP request là treo chương trình |

Ràng buộc thật sự vẫn giữ nguyên: **phải giảng lại được từng bước phần `service/optimization/` bằng
tiếng Việt.** Nhưng đó là yêu cầu về kết quả, không phải về cú pháp — một chuỗi Stream rõ ràng vẫn
giảng được, còn một vòng `for` lồng ba lớp thì chưa chắc.

### A2. Đổi toàn bộ định danh sang tiếng Anh

Tên bảng, tên cột, entity, biến, method, giá trị enum, URL, tên lớp trong `service/optimization`.

Giữ nguyên: comment trong code viết **tiếng Việt không dấu**, chuỗi hiển thị ra giao diện viết
**tiếng Việt có dấu** (mỗi enum có trường `displayName`).

Lý do: đây là quy ước của một dự án phần mềm thật. Bảng `orders` thì entity là `Order`, cột
`total_weight` thì thuộc tính là `totalWeight`.

Đợt đổi tên này để lại đúng **một chỗ sót**, phát hiện khi làm CN-00-04: `docs/04` vẫn ghi tiêu chí
nghiệm thu là gõ URL `/quan-tri/san-pham`. Đã sửa thành `/admin/products`.

---

### A3. Dùng OR-Tools làm bộ giải chính, giữ bản tự cài đặt làm mốc so sánh

**Đây là thay đổi lớn nhất về mặt định hướng của đồ án. Chuẩn bị kỹ câu trả lời cho mục này.**

Đặc tả ban đầu cấm tuyệt đối mọi thư viện VRP, với lý do "tự cài đặt là phần đóng góp học thuật".
Nay đổi thành: **hai bộ giải cùng tồn tại**, cắm vào cùng một interface `RoutingAlgorithm`.

| | Trước | Nay |
|---|---|---|
| Hệ thống chạy bằng | NN + 2-opt tự viết | **OR-Tools** |
| NN + 2-opt tự viết | Là sản phẩm | Là **mốc so sánh** ở chương 4 |
| Số mốc so sánh | 2 (A, B) | 3 (A, B, **C = OR-Tools**) |

**Vì sao đổi.** Con số quyết định nằm ngay trong `07-thuc-nghiem.md`: NN+2-opt cách lời giải
tốt nhất khoảng 10–25%, OR-Tools khoảng 0–5%. Chênh nhau khoảng 15 điểm phần trăm. Sản phẩm
bàn giao cho Minh Phú không có lý do gì để chạy bằng bản kém hơn khi bản tốt hơn chỉ là một
dependency.

**Cái gì mất đi.** Không còn nói được câu "em tự cài đặt hoàn toàn". Đây là mất mát thật,
đã cân nhắc và chấp nhận.

**Cái gì được thêm.** Chương thực nghiệm mạnh lên chứ không yếu đi, vì trước đây bản tự viết
không có gì để so ngoài chính nó. Nay có ba con số: hơn cách làm thủ công bao nhiêu, còn kém
lời giải tốt bao nhiêu, và vì sao kém. Câu trả lời "vì 2-opt chỉ sắp lại thứ tự bên trong một
tuyến, không sửa được cách chia đơn cho xe" chỉ chứng minh được khi có mốc C.

**Rào giữ cho việc so sánh còn ý nghĩa.** Package `optimization/heuristic/` cấm import
OR-Tools, có hook kiểm tra tự động chặn. Nếu thư viện lẫn vào đó thì hai
cột số không còn độc lập, và cả chương 4 mất giá trị.

**Đã kiểm chứng trước khi quyết.** `OrToolsSmokeTest` chạy trên chính máy phát triển
(Windows 11, JDK 23) — nạp được thư viện native, giải bài CVRP 4 điểm 2 xe ra đúng đáp số
tính tay 8000 m trong 1,5 giây. Không đổi kế hoạch dựa trên giả định là thư viện sẽ chạy được.

**Thứ tự làm không đổi:** tuần 6–7 vẫn viết NN rồi 2-opt trước, tuần 7 mới cắm OR-Tools vào.
Làm ngược lại thì lúc bản tự viết ra số lạ sẽ không biết là thuật toán yếu hay code sai.

### A4. Điều phối viên được **thêm mới** khách hàng, dù danh mục là "chỉ xem"

**Mâu thuẫn trong đặc tả gốc.** `docs/01` xếp toàn bộ danh mục là **"chỉ xem"** với điều phối viên.
Nhưng `docs/04` lại có **CN-02-10 — thêm khách hàng ngay trong màn hình tạo đơn**, và người tạo đơn
chính là điều phối viên. Hai chỗ này không thể cùng đúng.

**Quyết:** giữ CN-02-10, mở một ngoại lệ hẹp cho ma trận phân quyền.

**Vì sao chọn hướng này.** Khách gọi điện đặt hàng lần đầu là tình huống thật và thường gặp — lúc đó
điều phối viên đang gõ dở đơn, khách đang chờ máy. Bắt họ dừng lại, gọi quản trị viên thêm khách,
rồi quay lại gõ đơn từ đầu là biến một cuộc gọi hai phút thành mười phút. Đơn hàng bắt buộc phải trỏ
tới một khách có tọa độ (BR01-05, CBR-07) nên không có đường vòng nào khác.

**Ngoại lệ được bó hẹp hết mức** — đây là phần cần nói rõ khi hội đồng hỏi:

| Điều phối viên | Được / Không |
|---|---|
| `POST /orders/quick-customer` (hộp thoại trong màn tạo đơn) | **Được** — chỉ thêm mới |
| Gửi kèm `id` để sửa khách đang có qua đường đó | Không — `id` bị ép về `null`, hệ thống tạo bản ghi mới |
| Gửi kèm `status=INACTIVE` | Không — ép `ACTIVE`, không cho tự chọn |
| `/admin/customers/create`, `/admin/customers/{id}/edit`, `/admin/customers/save` | Không — **403** |
| `/admin/customers/{id}/toggle-status` (ngừng dùng khách) | Không — **403** |

Đã kiểm chứng đúng năm dòng trên bằng cách gửi thẳng dữ liệu lên máy chủ, không qua giao diện:
gửi `id=6&status=INACTIVE` thì hệ thống tạo khách **mới** ở trạng thái `ACTIVE`, khách #6 giữ nguyên
tên và trạng thái cũ.

**Được và mất.** Được: đúng quy trình làm việc thật, và CN-02-10 không phải cắt. Mất: ma trận phân
quyền không còn phát biểu gọn được thành một câu "danh mục là chỉ xem" — phải nhớ thêm ngoại lệ này.
Đổi lại thì nó nằm ở đúng một đường dẫn, đọc `OrderController.quickAddCustomer()` là thấy hết.

Hộp thoại này **cố ý không có bản đồ ghim tay**, chỉ tìm tọa độ theo địa chỉ hoặc gõ tay hai ô.
Lý do là kỹ thuật: `chon-toa-do.js` gắn cứng vào ba ô `address` / `latitude` / `longitude` của chính
trang, sửa nó thành dùng chung được thì phải sửa cả màn hình khách hàng và màn hình kho đang chạy tốt.
Muốn ghim tay chính xác thì vào màn hình Khách hàng sửa lại sau — thao tác đó không gấp như lúc
đang nghe điện thoại.

---

### A5. Bổ sung quản lý tồn kho thành phẩm — lỗ hổng bỏ sót trong đặc tả gốc

**Đặc tả gốc không có tồn kho.** `Product` chỉ có `weight_per_box`, `volume_per_box`, `unit_price`.
Luồng đơn hàng kiểm đúng một thứ: đơn có vượt sức chứa xe lớn nhất không (BR02-05). Không ở đâu hỏi
*kho có đủ hàng không*.

Hậu quả: khách gọi đặt 220 thùng → nhập đơn → duyệt → phân tuyến → gửi email hẹn giờ cho khách →
tài xế ra kho chất hàng → **phát hiện kho còn 80 thùng**. Lúc đó tuyến đã chốt, ETA đã gửi đi.

**Chỗ chí tử nằm trong chính `docs/00`:** phần Bốn tác nhân viết khách *"quen gọi điện để vừa đặt
vừa hỏi giá, **hỏi hàng còn không**"*. Đặc tả nói thẳng rằng điều phối viên cần biết tồn kho để trả
lời điện thoại — rồi hệ thống không cho họ biết. Hai chỗ này đọc cạnh nhau là thấy ngay.

**Không trái mục Ngoài phạm vi.** Mục đó loại trừ *"quản lý sản xuất, tồn kho **nguyên vật liệu**"*
— nguyên vật liệu, không phải thành phẩm. Tồn kho thành phẩm chưa bị loại trừ ở đâu cả, nó chỉ bị
**bỏ sót**. Sản xuất và nguyên vật liệu vẫn ngoài phạm vi.

**Quyết:** thêm `CN-01-10` và `CN-01-11`, một cột `products.stock_boxes` và một bảng `stock_movements`.

#### Bốn quyết định thiết kế cần giải thích được

**1. Lưu một con số, không lưu ba.** `stock_boxes` là số thùng *còn rảnh*. Hai con số kia suy ra
được: `đã cam kết` đếm từ `order_items` của đơn `APPROVED`/`ROUTED`/`DELIVERING`, `trong kho` là
tổng hai số. Số nào suy ra được thì không lưu — lưu là thêm một chỗ có thể lệch, và phải nhớ cập
nhật ở mọi bước chuyển trạng thái.

**2. Sổ kho chỉ ghi thêm.** Ghi sai thì ghi **bút toán đảo**, không sửa, không xóa. Ba lý do ở
`docs/02` bảng 17. Lý do mạnh nhất: xóa một dòng sổ là vi phạm thẳng **CBR-17**, mà đồ án đã áp
CBR-17 cho sản phẩm, xe, khách hàng, tài khoản — kho lại cho xóa cứng thì hệ thống tự mâu thuẫn.

**3. Trừ kho lúc duyệt đơn, không lúc tài xế lấy hàng.** Từ lúc duyệt, số hàng đó đã thuộc về đơn:
khách khác gọi đến thì không được hứa nữa. Trừ lúc lấy hàng thì giữa duyệt và lấy hàng hệ thống vẫn
tưởng hàng còn rảnh, và sẽ cho duyệt tiếp một đơn thứ hai bằng chính số hàng đó.

Đối xứng lại: lúc **tạo** đơn chỉ cảnh báo, vẫn lưu được. Giữ chỗ cho đơn chưa duyệt thì phải nhớ
nhả chỗ khi đơn bị hủy, và quên nhả là nguồn sai số kinh điển của mọi hệ tồn kho.

**4. Số thùng một đơn đang giữ được đếm từ SỔ KHO, không đọc từ dòng hàng của đơn.** Ba hậu quả,
cả ba đều có lợi: `updateOrder` gọi `getItems().clear()` trước khi dựng lại danh sách nên số lượng
cũ đã mất khỏi bộ nhớ — đọc từ sổ thì không cần chụp ảnh trước; gọi hai lần không hỏng gì vì lần
hai thấy cam kết đã bằng 0; và hủy một đơn chưa từng duyệt cũng không hoàn kho oan.

#### Số dư đầu kỳ — một giả định phải nói rõ

Tồn kho được thêm vào khi CSDL đã có sẵn năm sản phẩm và hàng trăm đơn, một số đơn đang ở trạng
thái đã duyệt tức là **đang giữ hàng**. `StockOpeningSeeder` (chạy sau `HistorySeeder`) làm hai
bước trong **một giao dịch**: ghi tồn đầu kỳ bằng số thùng *vật lý* trong kho, rồi trừ ngay phần
các đơn đang giữ.

Nhờ bước hai, đơn cũ hành xử y hệt đơn mới: hủy hoặc giao thất bại thì hoàn đúng số thùng đó về
kho. Không có bước hai thì chúng là một nhóm đơn đặc biệt không tham gia tồn kho, và `trong kho`
sẽ phóng lên (839 thay vì 800 với SP001) — đúng kiểu con số phải giải thích vòng vo khi vấn đáp.

**Không dựng lại mười ngày đơn hàng lịch sử thành các dòng xuất kho.** Đó là dữ liệu sinh ra trước
khi hệ thống theo dõi tồn kho, dựng lại cũng không dựa trên lần nhập kho thật nào. Mọi hệ thống
thật đều làm đúng thế khi đi vào hoạt động: chốt một con số ngày khai trương rồi ghi sổ từ đó.
Câu hỏi *"sao đơn tháng trước không trừ kho"* có câu trả lời sẵn, và nó là câu trả lời đúng chứ
không phải chống chế.

#### Phân quyền cố ý khác danh mục

Ma trận `docs/01` cho điều phối viên **chỉ xem** danh mục. Tồn kho tách khỏi quy tắc đó: nhập kho
là việc *vận hành* làm hàng ngày, giống đơn hàng chứ không giống danh mục. Nhưng *sửa lại* số tồn —
kiểm kê, hoàn tác — thì chỉ quản trị viên: người gõ số hàng ngày không nên là người được âm thầm
viết lại con số đó. Gõ nhầm 500 thành 5000 thì cách chữa đúng là báo lên để kiểm kê có lý do ghi
sổ, không phải tự kéo con số về.

Vì lý do đó **Tồn kho là một mục menu riêng**, không nhét vào menu Danh mục — gộp vào một chỗ là
mở đường cho nhầm lẫn về quyền.

#### Không đụng một dòng nào trong `service/optimization/`

Thuật toán chỉ nhận đơn `APPROVED` và không cần biết tồn kho tồn tại. UC03 và UC08 — hai phần làm
nên giá trị học thuật — rủi ro bằng không. Đây là lý do chính khiến việc bổ sung này đáng làm ở
giai đoạn muộn của đồ án.

#### Giới hạn đã biết, nói trước cho đỡ bị hỏi bất ngờ

| Giới hạn | Vì sao chấp nhận |
|---|---|
| Số tồn chỉ đúng nếu có người nhập kho | Chức năng Kiểm kê là van cứu khi số liệu lệch, nhưng nó là van, không phải nguồn dữ liệu |
| Số thùng còn rảnh hiện trên form tạo đơn chỉ là **tham khảo** | Hai đơn chưa duyệt cùng lúc đều thấy cùng một con số. Cửa chặn thật ở bước duyệt — có chủ ý, xem quyết định 3 |
| Không có lô sản xuất, hạn dùng, nhiều kho | Ngoài phạm vi. Nhiều kho còn là bài toán MDVRP, khác hẳn |

---

## B. Sửa lỗi logic tìm ra khi rà soát đặc tả

Bốn chỗ dưới đây là đặc tả gốc **sai hoặc thiếu**, không phải chuyện phong cách. Đây là phần quan
trọng nhất của tài liệu này.

### B1. Ma trận khoảng cách bất đối xứng làm hỏng công thức 2-opt

**Đã thêm bước B3 — đối xứng hóa ma trận — vào `loadMatrix` trong `docs/03-thuat-toan.md`.**

OSRM trả về khoảng cách theo **đường đi thật**, mà đường một chiều khiến khoảng cách hai chiều khác
nhau: `matrix[A][B] ≠ matrix[B][A]`.

Công thức delta rút gọn của 2-opt — chỉ cộng trừ bốn cạnh thay vì tính lại cả tuyến — **chỉ đúng khi
ma trận đối xứng**. Với ma trận bất đối xứng, phép đảo đoạn còn làm đổi chiều đi của mọi cạnh nằm
bên trong đoạn bị đảo, mà công thức bốn cạnh không tính tới phần đó.

Đã dựng một ma trận phản ví dụ để kiểm chứng: **công thức delta báo giảm 9, nhưng quãng đường thật
của tuyến lại tăng thêm 19.** Nghĩa là thuật toán sẽ "cải thiện" tuyến thành tệ hơn, lặp lại nhiều
lần, và **không báo lỗi gì cả**.

```
B3. DOI XUNG HOA ma tran:
        VOI moi cap (i, j) voi i < j:
            trungBinh   = (maTran[i][j] + maTran[j][i]) / 2
            maTran[i][j] = trungBinh
            maTran[j][i] = trungBinh
```

Đánh đổi: lấy trung bình làm khoảng cách sai lệch đôi chút so với đường thật. Nhưng thà sai vài phần
trăm một cách có kiểm soát, còn hơn để thuật toán tối ưu ngược mà không ai biết.

### B2. Trùng tên biến trong mã giả tính ETA

Mã giả tính ETA dùng chung một tên cho **tham số đầu vào** (số phút bốc dỡ mỗi điểm) và **kết quả
tích lũy** (tổng số phút bốc dỡ tới điểm hiện tại). Chép thẳng vào code là sai toàn bộ giờ dự kiến
giao hàng.

Đã tách thành `loadingMinutesPerStop` và `totalLoadingMinutes`.

### B3. Không có quy tắc nào cho trường hợp thuật toán để sót đơn

**Đã thêm BR03-13, MSG-16 và CN-03-14.**

Nearest Neighbor hoàn toàn có thể **không xếp hết đơn dù tổng sức chứa các xe vẫn còn thừa**. Nguyên
nhân là NN xếp tham lam theo điểm gần nhất, nên một đơn to có thể bị bỏ lại khi mọi xe đều đã nhận
đủ đơn nhỏ.

Đặc tả gốc không nói hệ thống phải làm gì trong tình huống này. Mà **im lặng bỏ sót đơn là lỗi nghiệp
vụ nặng**: điều phối bấm chốt tuyến, màn hình báo thành công, hôm sau khách không nhận được hàng và
không ai biết vì sao.

Nay: màn hình phân tuyến phải hiện rõ danh sách đơn chưa xếp được, kèm MSG-16
*"Còn {0} đơn chưa xếp được vào tuyến nào. Vui lòng chọn thêm xe."*

Đây cũng là **điểm nhấn demo bước 5a** — xem B4.

### B4. Dữ liệu mẫu quá ít nên phần thực nghiệm không chứng minh được gì

**Đã nâng dữ liệu seed từ 5–8 đơn/ngày lên 12–15 đơn/ngày trong `docs/08-du-lieu-mau.md`.**

Lý do là số học thuần túy. Đồ án dùng hai mốc so sánh:

- **Mốc A** — mỗi đơn một chuyến khứ hồi từ kho (mô phỏng cách làm hiện tại của doanh nghiệp)
- **Mốc B** — giữ nguyên cách chia đơn cho xe, chỉ đổi thứ tự giao

Mốc B mới là mốc chứng minh **đóng góp riêng của thuật toán sắp thứ tự**. Nhưng với tuyến chỉ có
**2 điểm giao**, quãng đường `kho → A → B → kho` và `kho → B → A → kho` **bằng đúng nhau**. Nghĩa là
Mốc B luôn cho mức giảm **0%**, bất kể thuật toán tốt đến đâu.

Với 5–8 đơn chia cho 2–3 xe, phần lớn tuyến chỉ có 2 điểm. Phần thực nghiệm sẽ ra một bảng số liệu
trông có vẻ đầy đủ nhưng không chứng minh được điều gì.

Kèm theo hai thay đổi nữa trong cùng file:

- **Viết lại kịch bản demo thành 3 chặng (bước 5a, 5b).** Cố ý **giữ lại** việc NN để sót đơn làm
  điểm nhấn, thay vì chỉnh dữ liệu cho chạy trơn tru. Đây là câu trả lời vấn đáp mạnh nhất: cho hội
  đồng thấy sinh viên hiểu **giới hạn** của thuật toán mình cài, không chỉ biết nó chạy được.
- `docs/07-thuc-nghiem.md` nới kỳ vọng gap benchmark Augerat thành **10–25%** sau 2-opt, kèm cảnh báo
  ngược: **gap dưới 5% gần như chắc chắn là code sai**, hay gặp nhất là quên chặng cuối về kho. Thêm
  3 hạn chế vào mục 4.7.

---

## C. Chi tiết kỹ thuật phát sinh khi code thật

Những thứ đặc tả viết trên giấy không thể biết trước, chỉ lộ ra khi chạy.

| Chỗ | Vấn đề và cách xử lý |
|---|---|
| `docs/02` | **Hibernate 6 sinh cột `ENUM` native của MySQL** thay vì `VARCHAR`. Không có tham số cấu hình nào tắt được — đã thử `hibernate.type.prefer_native_enum_types`, không ăn. Phải đánh dấu `@JdbcTypeCode(SqlTypes.VARCHAR)` **trên từng trường enum**. Đã thêm câu truy vấn `information_schema` để kiểm còn sót trường nào không |
| `docs/02` | Hệ quả còn lại: Hibernate vẫn sinh **22 CHECK constraint** khóa cứng danh sách giá trị enum, và `ddl-auto=update` **không cập nhật chúng**. Thêm một giá trị enum mới là gặp `ERROR 3819`. Giữ nguyên vì thà hỏng ồn ào còn hơn kiểu `ENUM` native hỏng âm thầm theo chỉ số. Đã ghi cách xử lý bằng `ALTER TABLE ... DROP CHECK` |
| `pom.xml` | Máy dùng JDK 23. Từ JDK 21, javac **không còn tự chạy annotation processor** tìm thấy trên classpath. Thiếu khối `annotationProcessorPaths` thì **Lombok im lặng không sinh getter**, code báo `cannot find symbol` mà không nói lý do |
| `pom.xml` | Hạ Spring Boot **4.1.0 → 3.5.16**. Bản 4.x có nhiều thay đổi phá vỡ, trong khi gần như toàn bộ giáo trình và câu trả lời tra cứu được đều viết cho 3.x |
| `application.properties` | MySQL chạy trong WSL nên đồng hồ máy chủ là **UTC, lệch 7 tiếng** so với giờ ứng dụng ghi vào. Riêng `serverTimezone=` **không đủ** — nó chỉ báo trình điều khiển cách diễn giải giờ. Phải có `forceConnectionTimeZoneToSession=true` thì `NOW()` bên MySQL mới đúng giờ Việt Nam. Chưa sửa thì mọi câu lọc theo `CURDATE()` chạy trong khoảng **0h–7h sáng** sẽ trả về ngày hôm trước |
| `docs/10-quy-uoc-ky-thuat.md` | Thêm thư mục `security/` vào sơ đồ cấu trúc thư mục |
| `Product.java` | Đơn vị mặc định `"Thung"` → `"Thùng"`. Đây là chuỗi hiển thị ra giao diện nên phải có dấu |
| `docs/04` ↔ `docs/05` | **Hai tài liệu mâu thuẫn nhau và không ai phát hiện suốt 11 tuần.** `docs/05` liệt kê `SCR-10 /tracking` trong bảng màn hình và đặt mục "Theo dõi" vào menu điều phối; `docs/04` không có mục chức năng nào tương ứng. Menu được dựng theo `docs/05`, màn hình không được làm vì `docs/04` không yêu cầu — bấm vào ra **404**. Phát hiện ngày 20/09/2026 do bấm thử menu, không phải do rà tài liệu. Đã làm màn hình và thêm mục **CN-04-11** vào `docs/04` |

---

## D. Những gì **không** đổi

Ghi ra để thấy rõ phạm vi thay đổi là có giới hạn:

- **Phạm vi chức năng**: **87 chức năng bắt buộc** + 11 tùy chọn. So với bản gốc chỉ thêm hai mục: CN-03-14, và CN-04-11 (màn hình `docs/05` đã có sẵn mà `docs/04` bỏ sót — xem mục C)
- **Kế hoạch 12 tuần**: nguyên vẹn
- **Thuật toán tự cài đặt**: vẫn Nearest Neighbor + 2-opt + Cheapest Insertion, viết tay đầy đủ,
  không cắt bớt dòng nào — chỉ đổi vai trò từ sản phẩm sang mốc so sánh (xem A3)
- **18 quy tắc CBR và 95 quy tắc BR**: nguyên vẹn về nội dung
- **16 bảng dữ liệu**: nguyên vẹn về cấu trúc
- **Bảng màu, bố cục màn hình, quy ước hiển thị số liệu**: nguyên vẹn

---

## E. Ba mâu thuẫn đã phát hiện, **chưa** sửa

Ghi ra để người làm đồ án quyết, không tự ý đổi vì cả ba đều dính tới phạm vi.

### E1. Khối lượng công việc vượt quá thời gian

86 chức năng bắt buộc / 12 tuần / một người. Ước lượng thực tế là **18–24 tuần**.

Cần **cắt chủ động trước tuần 8**, đừng đợi tuần 9 mới cắt trong hoảng loạn. Gợi ý cắt ngay: RPT-01,
RPT-02 và toàn bộ mục ○.

### E2. Thứ tự cắt tự mâu thuẫn

`docs/00-tong-quan.md` xếp **UC06 (xử lý sự cố)** vào diện cắt sớm. Nhưng kịch bản demo trong
`docs/08` lại lấy **bước 13 — ba phương án xử lý sự cố** — làm một trong bốn điểm nhấn.

Gợi ý dung hòa: giữ UC06 nhưng làm **phương án A và C trước**, phương án B (Cheapest Insertion) làm
sau nếu còn thời gian.

### E3. Phân quyền danh mục chưa thống nhất — **đã xử lý xong**

Ma trận phân quyền trong `docs/01` cho điều phối viên quyền **"chỉ xem"** danh mục. Nhưng `docs/05`
xếp SCR-02…06 là màn hình của **quản trị viên**.

Đã quyết theo `docs/01`: điều phối viên **xem được** sản phẩm, phương tiện, tài xế, khách hàng, kho
nhưng mọi đường ghi đều trả 403; tài khoản và tham số vẫn chỉ quản trị viên. Đã kiểm 14 địa chỉ.
Sau đó có thêm **một ngoại lệ duy nhất** cho việc thêm mới khách hàng từ màn hình tạo đơn — xem
mục **A4** ở trên.
