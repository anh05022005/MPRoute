# 01 — Quy tắc nghiệp vụ

## Quy tắc chung (CBR) — áp dụng toàn hệ thống

| Mã | Nội dung |
|---|---|
| CBR-01 | Mọi tuyến xuất phát từ kho và kết thúc tại kho. **Quãng đường chặng cuối từ điểm giao cuối cùng về kho phải được tính vào tổng.** |
| CBR-02 | Một đơn hàng chỉ thuộc đúng một tuyến. Không tách đơn ra nhiều xe. |
| CBR-03 | Tổng khối lượng các đơn trong tuyến ≤ tải trọng tối đa của xe. |
| CBR-04 | Tổng thể tích các đơn trong tuyến ≤ thể tích thùng xe. **Kiểm tra đồng thời với CBR-03.** |
| CBR-05 | Chỉ xe trạng thái `AVAILABLE` được đưa vào phân tuyến. |
| CBR-06 | Một tài xế chỉ được gán tối đa một tuyến trong cùng ngày. |
| CBR-07 | Đơn hàng phải có tọa độ hợp lệ trước khi phân tuyến. Thiếu tọa độ thì bị loại khỏi danh sách chờ và hiện cảnh báo. |
| CBR-08 | Ngày giao không được là ngày trong quá khứ. |
| CBR-09 | Đơn chỉ sửa/hủy được khi ở trạng thái `PENDING_APPROVAL` hoặc `APPROVED`. |
| CBR-10 | Tuyến đã chốt không sửa được, trừ khi hủy tuyến hoặc xử lý sự cố. |
| CBR-11 | Khoảng cách lấy từ OSRM. OSRM lỗi thì dùng Haversine × hệ số (mặc định 1.3) và ghi log. |
| CBR-12 | Ma trận khoảng cách phải được lưu vào DB. Tra DB trước, chỉ gọi OSRM khi chưa có. |
| CBR-13 | ETA điểm thứ N = giờ xuất phát + (quãng đường tích lũy ÷ tốc độ TB) + (N−1) × thời gian bốc dỡ. |
| CBR-14 | Mọi thay đổi trạng thái đơn hàng và tuyến phải ghi lại: thời điểm, người thực hiện. |
| CBR-15 | Kiểm tra quyền ở phía server cho mọi endpoint, không chỉ ẩn nút trên giao diện. |
| CBR-16 | Mật khẩu băm bằng BCrypt, không lưu văn bản thuần. |
| CBR-17 | Bản ghi đã phát sinh dữ liệu nghiệp vụ không xóa cứng, chỉ khóa/ngừng sử dụng. |
| CBR-18 | Chỉ số so sánh trước/sau tối ưu **lưu tại thời điểm chạy**, không tính lại về sau. |

---

## Trạng thái đơn hàng — `OrderStatus`

```
PENDING_APPROVAL ──> APPROVED ──> ROUTED ──> DELIVERING ──┬──> DELIVERED
       │                 │                                ├──> FAILED
       └──> CANCELLED <───┘                               └──> POSTPONED
```

| Mã | Tên hiển thị | Ai chuyển | Ghi chú |
|---|---|---|---|
| `PENDING_APPROVAL` | Chờ duyệt | Điều phối (tạo đơn) | Sửa/hủy được |
| `APPROVED` | Đã duyệt | Điều phối | Vào danh sách chờ phân tuyến |
| `ROUTED` | Đã phân tuyến | Hệ thống (chốt tuyến) | Không sửa được |
| `DELIVERING` | Đang giao | Hệ thống (tài xế nhận hàng) | |
| `DELIVERED` | Đã giao | Tài xế | Trạng thái kết thúc |
| `FAILED` | Giao thất bại | Tài xế | Kết thúc trong ngày, kèm lý do |
| `POSTPONED` | Hoãn | Tài xế / Hệ thống | Tự tạo lại ở ngày giao mới với trạng thái `APPROVED` |
| `CANCELLED` | Đã hủy | Điều phối | Trạng thái kết thúc, kèm lý do |

## Trạng thái tuyến — `RouteStatus`

```
DRAFT ──> CONFIRMED ──> IN_PROGRESS ──┬──> COMPLETED
  │           │                        └──> INCIDENT ──> IN_PROGRESS
  └──> CANCELLED <┘
```

| Mã | Tên hiển thị | Ghi chú |
|---|---|---|
| `DRAFT` | Nháp | Vừa sinh từ thuật toán, chưa gán tài xế, chạy lại được |
| `CONFIRMED` | Đã chốt | Đã gán tài xế, tài xế thấy trên điện thoại |
| `IN_PROGRESS` | Đang chạy | Tài xế đã xác nhận nhận hàng |
| `INCIDENT` | Có sự cố | Chờ điều phối chọn phương án |
| `COMPLETED` | Hoàn thành | Mọi điểm dừng đã có trạng thái kết thúc |
| `CANCELLED` | Đã hủy | Đơn về `APPROVED`, xe về `AVAILABLE` |

## Trạng thái điểm dừng — `StopStatus`

`PENDING` → `DELIVERING` → `DELIVERED` / `FAILED` / `POSTPONED`

## Trạng thái xe — `VehicleStatus`

| Mã | Tên | Ai chuyển |
|---|---|---|
| `AVAILABLE` | Sẵn sàng | Mặc định, hoặc hệ thống khi tuyến hoàn thành/hủy |
| `IN_USE` | Đang chạy | Hệ thống khi tuyến được chốt |
| `MAINTENANCE` | Bảo trì | Quản trị viên, hoặc hệ thống khi có sự cố "không thể tiếp tục" |

**Lưu ý:** tuyến dùng `IN_PROGRESS` còn xe dùng `IN_USE`. Hai khái niệm khác nhau — xe bị khóa ngay khi tuyến được **chốt**, trong khi tuyến chỉ chuyển `IN_PROGRESS` khi tài xế **xác nhận nhận hàng**.

## Trạng thái thanh toán đơn hàng — `PaymentStatus`

| Mã | Tên | Khi nào |
|---|---|---|
| `UNPAID` | Chưa thanh toán | Mặc định khi tạo đơn |
| `PAID` | Đã thanh toán | Tài xế thu đủ tiền mặt, hoặc khách đã chuyển khoản trước |
| `DEBT` | Ghi nợ | Đơn công nợ giao thành công |

---

## Ma trận phân quyền

| Nhóm chức năng | Quản trị | Điều phối | Tài xế | Khách |
|---|---|---|---|---|
| Quản lý danh mục | Toàn quyền | Chỉ xem | — | — |
| Quản lý tài khoản | Toàn quyền | — | — | — |
| Cấu hình tham số | Toàn quyền | — | — | — |
| Quản lý đơn hàng | Chỉ xem | Toàn quyền | Xem đơn trong tuyến | Tra cứu đơn của mình |
| Phân tuyến | Chỉ xem | Toàn quyền | — | — |
| Cập nhật giao hàng | — | — | Toàn quyền | — |
| Xử lý sự cố | Chỉ xem | Toàn quyền | Chỉ báo sự cố | — |
| Báo cáo | Toàn quyền | Chỉ xem | — | — |

Vai trò trong hệ thống — `Role`: `ADMIN`, `DISPATCHER`, `DRIVER`.

Spring Security cần tiền tố `ROLE_` khi tạo `GrantedAuthority`, nên `ADMIN` trong DB tương ứng `ROLE_ADMIN` trong `hasRole()`.

Đường dẫn công khai không cần đăng nhập: `/track`, `/login`, `/css/**`, `/js/**`.

---

## Quy tắc theo từng use case

### UC01 — Quản lý danh mục

| Mã | Nội dung |
|---|---|
| BR01-01 | Sản phẩm bắt buộc có `weight_per_box` và `volume_per_box`, cả hai > 0. **Đây là đầu vào của thuật toán, thiếu là không chạy được.** |
| BR01-02 | `products.code` duy nhất |
| BR01-03 | Xe bắt buộc có `max_weight` và `max_volume`, cả hai > 0. `license_plate` duy nhất |
| BR01-04 | Trạng thái `IN_USE` do hệ thống tự đặt, quản trị viên không chọn tay được |
| BR01-05 | Khách hàng bắt buộc có tọa độ. Nhập địa chỉ rồi bấm "Tìm tọa độ" (Nominatim), hoặc ghim tay trên bản đồ |
| BR01-06 | Nominatim trả nhiều kết quả thì hiện danh sách để chọn. Không có kết quả thì bắt ghim tay |
| BR01-07 | Khách hàng có trường `delivery_note` dạng văn bản tự do |
| BR01-08 | Số điện thoại đúng 10 chữ số, bắt đầu bằng 0 |
| BR01-09 | Bản ghi đã dùng trong nghiệp vụ chỉ chuyển `INACTIVE`, không xóa |
| BR01-10 | Chỉ một kho được đánh dấu `active = true` |

### UC02 — Tiếp nhận đơn hàng

| Mã | Nội dung |
|---|---|
| BR02-01 | Mã đơn tự sinh: `MP` + `yyMMdd` + 4 chữ số thứ tự trong ngày. Ví dụ `MP2608040042` |
| BR02-02 | Ít nhất một dòng chi tiết. `box_count` là số nguyên > 0 |
| BR02-03 | Thêm/xóa dòng chi tiết chỉ khi đơn ở `PENDING_APPROVAL` hoặc `APPROVED` |
| BR02-04 | Hệ thống tự tính `total_weight` và `total_volume`, không cho sửa tay |
| BR02-05 | Nếu đơn vượt sức chứa xe lớn nhất → chặn lưu, báo MSG-06, yêu cầu tách đơn |
| BR02-06 | Ngày giao mặc định là ngày mai, không nhỏ hơn hôm nay |
| BR02-07 | Chọn khách có sẵn thì tự điền địa chỉ, tọa độ, SĐT, ghi chú, hình thức thanh toán. Ghi đè địa chỉ cho riêng đơn được, không đổi danh mục |
| BR02-08 | Ghi đè địa chỉ thì bắt buộc xác định lại tọa độ |
| BR02-09 | Đơn mới có trạng thái `PENDING_APPROVAL`. Phải bấm "Duyệt đơn" mới sang `APPROVED` |
| BR02-10 | Bước duyệt để kiểm lại địa chỉ và tọa độ trước khi vào thuật toán |
| BR02-11 | Đơn từ `ROUTED` trở đi không sửa/hủy được |
| BR02-12 | Hủy đơn bắt buộc nhập lý do. Không xóa khỏi DB |
| BR02-13 | **Đơn giá lấy từ danh mục tại thời điểm tạo đơn và lưu vào `order_items.unit_price`.** Đổi giá về sau không ảnh hưởng đơn cũ |
| BR02-14 | `amount_to_collect` = `total_amount` nếu `CASH`, = 0 nếu `CREDIT` hoặc `PREPAID` |
| BR02-15 | Hình thức thanh toán điền sẵn theo khách hàng, sửa được cho riêng đơn, không đổi danh mục |
| BR02-16 | Đổi hình thức thanh toán thì tính lại `amount_to_collect` ngay |
| BR02-17 | Sau khi lưu, hiện khối tóm tắt đơn để điều phối viên đọc lại cho khách qua điện thoại |

### UC03 — Phân tuyến tự động

| Mã | Nội dung |
|---|---|
| BR03-01 | Chỉ lấy đơn `APPROVED` có `delivery_date` trùng ngày được chọn |
| BR03-02 | Chỉ dùng xe `AVAILABLE` và được điều phối tích chọn |
| BR03-03 | Ràng buộc khối lượng và thể tích kiểm đồng thời |
| BR03-04 | Tổng nhu cầu > tổng sức chứa → báo MSG-08, không sinh tuyến |
| BR03-05 | Mỗi tuyến khép kín kho → các điểm → kho |
| BR03-06 | Khoảng cách lấy từ bảng cache, thiếu thì gọi OSRM và lưu lại |
| BR03-07 | **Hai mốc so sánh, xem `07-thuc-nghiem.md`** |
| BR03-08 | Mức giảm = (mốc − sau tối ưu) ÷ mốc × 100 |
| BR03-09 | Chạy lại thì xóa kết quả `DRAFT` trước đó của cùng ngày |
| BR03-10 | Ghi nhận thời gian thực thi thuật toán (ms) |
| BR03-11 | Điều phối chọn thuật toán: `NN` hoặc `NN_2OPT` (mặc định) |
| BR03-12 | Xe thừa không gán tuyến, giữ `AVAILABLE` |
| BR03-13 | Tổng sức chứa đủ nhưng thuật toán vẫn không xếp hết đơn → **vẫn hiển thị các tuyến đã sinh**, kèm danh sách đơn chưa gán (CN-03-14). Không được im lặng bỏ sót |

### UC04 — Điều chỉnh tuyến và gán tài xế

| Mã | Nội dung |
|---|---|
| BR04-01 | Kéo thả đổi thứ tự điểm trong tuyến |
| BR04-02 | Kéo thả chuyển đơn giữa hai tuyến |
| BR04-03 | Sau mỗi thay đổi, tính lại ngay tổng km, kg, m³, ETA của tuyến bị ảnh hưởng |
| BR04-04 | Vi phạm sức chứa → cảnh báo đỏ + vô hiệu hóa nút "Gán tài xế" và "Chốt kế hoạch" |
| BR04-05 | Hiện song song quãng đường phương án tự động và phương án chỉnh tay |
| BR04-06 | Nút "Khôi phục kết quả tự động" |
| BR04-07 | Tài xế đã gán không xuất hiện trong danh sách chọn của tuyến khác cùng ngày |
| BR04-08 | Chốt tuyến → đơn sang `ROUTED`, xe sang `IN_USE` |
| BR04-09 | Hủy tuyến `CONFIRMED` → đơn về `APPROVED`, xe về `AVAILABLE` |
| BR04-10 | Không hủy được tuyến `IN_PROGRESS`, phải theo luồng sự cố |
| BR04-11 | Chốt tuyến → gửi email cho khách có địa chỉ email, gồm mã đơn, ETA, link tra cứu |
| BR04-12 | Khách không có email → vẫn ghi thông báo với trạng thái `NOT_SENT` để điều phối chủ động gọi |

### UC05 — Thực hiện giao hàng

| Mã | Nội dung |
|---|---|
| BR05-01 | Tài xế chỉ thấy tuyến của mình trong ngày hiện tại |
| BR05-02 | Tài xế không đổi được thứ tự điểm giao |
| BR05-03 | Phải bấm "Xác nhận nhận hàng" mới cập nhật được điểm giao. Tuyến sang `IN_PROGRESS` |
| BR05-04 | Giao diện làm nổi bật đúng một điểm đang xử lý |
| BR05-05 | Cập nhật theo đúng thứ tự, không nhảy cóc |
| BR05-06 | "Giao thất bại" bắt buộc chọn lý do: `CUSTOMER_ABSENT`, `CUSTOMER_REFUSED`, `WRONG_ADDRESS`, `DAMAGED_GOODS`, `OTHER`. Chọn `OTHER` thì bắt nhập mô tả |
| BR05-07 | "Hoãn" bắt buộc chọn ngày giao mới, không được ngày quá khứ |
| BR05-08 | Mọi cập nhật ghi kèm thời điểm và tọa độ GPS (nếu trình duyệt cho phép) |
| BR05-09 | Điểm cuối xong → tuyến `COMPLETED`, xe `AVAILABLE` |
| BR05-10 | Nút gọi dạng `tel:`, nút chỉ đường mở bản đồ ngoài với tọa độ |
| BR05-11 | Đơn có `amount_to_collect > 0` → khi bấm "Đã giao" bắt buộc nhập số tiền thực thu và hình thức |
| BR05-12 | Thu khác số phải thu → bắt buộc ghi chú lý do, gửi thông báo cho điều phối |
| BR05-13 | Đơn `FAILED` hoặc `POSTPONED` giữ trạng thái thanh toán `UNPAID` |
| BR05-14 | Đơn công nợ giao thành công → trạng thái thanh toán `DEBT` |

### UC06 — Xử lý sự cố

| Mã | Nội dung |
|---|---|
| BR06-01 | Loại sự cố: `VEHICLE_BREAKDOWN`, `TRAFFIC_JAM`, `ACCIDENT`, `OTHER`. Chọn `OTHER` thì bắt nhập mô tả |
| BR06-02 | Tự lấy tọa độ GPS. Không lấy được thì dùng tọa độ điểm giao gần nhất đã xong, ghi chú là ước lượng |
| BR06-03 | Mức độ: `CAN_CONTINUE` hoặc `CANNOT_CONTINUE` |
| BR06-04 | `CAN_CONTINUE`: nhập số phút chậm trễ, hệ thống chỉ cộng vào ETA các điểm còn lại, không sinh phương án |
| BR06-05 | `CANNOT_CONTINUE`: sinh 3 nhóm phương án, xếp theo km phát sinh tăng dần |
| BR06-06 | **Phương án A** (`A_RESCUE_VEHICLE`) — điều xe `AVAILABLE` từ kho ra sang hàng. Km phát sinh = kho → điểm sự cố + phần lộ trình còn lại |
| BR06-07 | **Phương án B** (`B_INSERT_OTHER_ROUTE`) — chèn các điểm còn lại vào tuyến khác đang chạy, dùng Cheapest Insertion. Phải kiểm sức chứa còn lại |
| BR06-08 | **Phương án C** (`C_POSTPONE`) — hoãn sang hôm sau. Km phát sinh = 0 nhưng luôn xếp cuối |
| BR06-09 | Chỉ hiện phương án khả thi. Vi phạm sức chứa thì loại |
| BR06-10 | Mỗi phương án hiện: km phát sinh, ETA mới của từng khách, nguồn lực cần huy động |
| BR06-11 | Phương án C luôn hiện như dự phòng cuối |
| BR06-12 | Chọn xong → cập nhật tuyến, tính lại ETA, ghi lịch sử |
| BR06-13 | Gửi email cho khách bị ảnh hưởng kèm ETA mới |
| BR06-14 | Xe `CANNOT_CONTINUE` → tự chuyển `MAINTENANCE` |

### UC07 — Tra cứu đơn hàng

| Mã | Nội dung |
|---|---|
| BR07-01 | Trang công khai, không đăng nhập |
| BR07-02 | Tra bằng mã đơn hoặc số điện thoại |
| BR07-03 | Tra bằng SĐT chỉ hiện đơn trong 30 ngày gần nhất |
| BR07-04 | **Không hiện**: giá trị đơn hàng, khách khác cùng tuyến, biển số xe, các điểm giao khác |
| BR07-05 | Chỉ hiện tên + SĐT tài xế khi đơn đang `DELIVERING` |
| BR07-06 | Hành trình hiển thị theo thứ tự thời gian |
| BR07-07 | Hiện ETA mới nhất, tự cập nhật khi có sự cố |
| BR07-08 | Không tìm thấy → MSG-11, không tiết lộ nguyên nhân |
| BR07-09 | Giới hạn 20 lần tra cứu / phút / IP |

### UC08 — Báo cáo

| Mã | Nội dung |
|---|---|
| BR08-01 | Khoảng thời gian tối đa 365 ngày |
| BR08-02 | Lấy từ chỉ số đã lưu (CBR-18), không tính lại |
| BR08-03 | Tuyến `approximate_distance = true` hiện riêng, không tính vào trung bình |
| BR08-04 | Tuyến `CANCELLED` không tính vào báo cáo nào |
| BR08-05 | Mọi báo cáo có nút xuất Excel |
| BR08-06 | Tỷ lệ giao thành công = số đơn `DELIVERED` ÷ tổng đơn đã phân tuyến |

---

## Thông báo hệ thống

Nội dung hiển thị cho người dùng nên **giữ nguyên tiếng Việt có dấu**.

| Mã | Loại | Nội dung |
|---|---|---|
| MSG-01 | Thành công | Lưu dữ liệu thành công. |
| MSG-02 | Thành công | Duyệt đơn hàng thành công. |
| MSG-03 | Thành công | Chốt kế hoạch thành công. |
| MSG-04 | Thành công | Đã áp dụng phương án xử lý sự cố. |
| MSG-05 | Cảnh báo | Bản ghi đã phát sinh dữ liệu nghiệp vụ. Thay đổi chỉ áp dụng cho đơn tạo mới. |
| MSG-06 | Lỗi | Đơn hàng vượt sức chứa xe lớn nhất ({0} so với {1}). Vui lòng tách đơn. |
| MSG-07 | Lỗi | Không sửa/hủy được đơn đã phân tuyến. |
| MSG-08 | Lỗi | Tổng sức chứa không đủ. Còn thiếu {0} kg / {1} m3. |
| MSG-09 | Cảnh báo | Không kết nối được OSRM. Đang dùng khoảng cách xấp xỉ. |
| MSG-10 | Lỗi | Không hủy được tuyến đang chạy. |
| MSG-11 | Thông tin | Không tìm thấy đơn hàng phù hợp. |
| MSG-12 | Lỗi | Tên đăng nhập hoặc mật khẩu không đúng. |
| MSG-13 | Lỗi | Tài khoản đã bị khóa. |
| MSG-14 | Cảnh báo | Tuyến {0} vượt quá {1} cho phép của xe. |
| MSG-15 | Xác nhận | Bạn có chắc chắn muốn thực hiện thao tác này? |
| MSG-16 | Cảnh báo | Còn {0} đơn chưa xếp được vào tuyến nào. Vui lòng chọn thêm xe. |

## Sự kiện sinh thông báo

| Mã | Sự kiện | Người nhận |
|---|---|---|
| NTF-01 | Tuyến được chốt | Tài xế được gán |
| NTF-02 | Tài xế báo sự cố | Điều phối viên |
| NTF-03 | Giao hàng thất bại | Điều phối viên |
| NTF-04 | Áp dụng phương án xử lý | Tài xế liên quan |
| NTF-05 | Tuyến bị hủy | Tài xế được gán |
| NTF-06 | Đơn bị hoãn | Điều phối viên |
| NTF-07 | ETA thay đổi | Khách hàng (email) |
| NTF-08 | Tuyến được chốt | Khách hàng (email) — mã đơn, ETA, link tra cứu |
| NTF-09 | Thu tiền lệch số phải thu | Điều phối viên |
