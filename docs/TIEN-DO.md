# Tiến độ đồ án

> Trạng thái hiện tại của đồ án. **Giữ dưới 60 dòng.** Cập nhật sau mỗi mốc hoàn thành.

**Cập nhật lần cuối:** 12/08/2026 · Kho: `github.com/anh05022005/MPRoute` (riêng tư), nhánh `main`

---

## Đang ở đâu

| | |
|---|---|
| Theo kế hoạch | Tuần 3 — CRUD danh mục |
| Thực tế | **Xong Nhóm 0 + sản phẩm + phương tiện + tài xế + tài khoản.** Đúng lịch |

```
[x] T1  Chot de tai, hoi Minh Phu 3 cau
[x] T2  Project chay, 16 bang sinh ra, dang nhap duoc
[~] T3  CRUD 7 danh muc xong  (4/7: san pham, phuong tien, tai xe, tai khoan)
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

---

## Đã xong

- 11 file đặc tả trong `docs/`, kể cả `10-quy-uoc-ky-thuat.md` (công nghệ, đặt tên, kiểm thử, bảo mật, Git)
- Spring Boot **3.5.16**, 10 dependency, package `com.minhphu.mproute`, kết nối MySQL 8.0.44
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

- Đưa code lên `github.com/anh05022005/MPRoute` (**riêng tư**), nhánh `main`, 11 commit chia theo mốc
  chức năng. Từ nay commit theo **Conventional Commits** — khuôn và ví dụ ở `docs/10-quy-uoc-ky-thuat.md`

- **Phân quyền danh mục theo đúng ma trận `docs/01`:** điều phối viên **xem được** sản phẩm, phương tiện,
  tài xế nhưng mọi đường ghi đều trả 403; tài khoản và tham số vẫn chỉ quản trị viên. Kiểm 14 địa chỉ:
  3 GET danh sách ra 200, 6 GET form sửa + 3 POST ghi + 2 màn hình quản trị ra 403.
  Giao diện điều phối hiện chữ "Chỉ xem" thay cho nút, menu không có mục Tài khoản/Tham số
- **Sửa 405 thành 403** khi vai trò không đủ quyền gửi POST tới `/admin/**`. Nguyên nhân:
  `accessDeniedPage()` *chuyển tiếp* nên giữ nguyên method POST, mà `HomeController` khai `@GetMapping("/403")`.
  Đổi sang `@RequestMapping("/403")`

Tài khoản mẫu do `DataSeeder` tạo, khớp `docs/08`: `admin/admin123` · `dieuphoi/dieuphoi123` ·
`taixe01`, `taixe02`, `taixe03` cùng `taixe123`. Ba tài xế chứ không phải một, vì kịch bản demo
bước 12–14 (phương án B) cần hai tuyến chạy song song mới dựng được tình huống.

---

## Việc tiếp theo — tuần 3, 4 danh mục còn lại

Khuôn đã chạy qua bốn lần (`XxxForm` → `XxxService` → `XxxController` → `xxxs.html` +
`xxx-form.html` + fragment phân trang), các màn hình sau chép theo:

1. CN-01-04 Khách hàng — nặng hơn vì có tọa độ · CN-01-06 Kho (BR01-10: chỉ một kho `active`)
2. CN-01-08 Tham số hệ thống — 9 tham số, kiểm bằng cách đổi `AVERAGE_SPEED` rồi xem ETA có đổi
3. CN-01-05 Tìm tọa độ bằng Nominatim — làm sau cùng vì cần gọi mạng ngoài

---

## Cần quyết định

| Việc | Tình hình |
|---|---|
| **Cắt phạm vi — quyết trước tuần 8** | 86 chức năng bắt buộc / 12 tuần / một người. Ước lượng thật là 18–24 tuần. **Cắt chủ động ngay chứ đừng đợi tuần 9 mới cắt trong hoảng loạn.** Cắt hẳn RPT-01, RPT-02 và toàn bộ mục ○ từ bây giờ |
| **Mâu thuẫn về thứ tự cắt** | `docs/00` xếp UC06 (sự cố) vào diện cắt sớm, nhưng kịch bản demo lại lấy bước 13 làm điểm nhấn. Dung hòa: giữ UC06 nhưng làm **phương án A và C trước**, phương án B (Cheapest Insertion) làm sau nếu còn thời gian |
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

- Menu trỏ tới `/orders`, `/routing`… chưa có Controller → bấm vào ra 404. Hết tuần 3–5 là tự hết
- Khi kiểm thử form bằng `curl` trong Git Bash: **phải percent-encode tiếng Việt sẵn** rồi dùng
  `--data-raw`. Git Bash chuyển tham số sang bảng mã ANSI khi gọi `curl.exe`, làm hỏng dấu tiếng Việt
  ngay từ máy khách — dễ tưởng nhầm là ứng dụng sai encoding. Kiểm bằng `HEX(cột)` trong MySQL.
  **Lệnh `docker exec ... mysql -e "…"` gọi từ PowerShell cũng hỏng y hệt**, mà kiểu khác: chữ bị mã hóa
  hai lần (`Nguyễn` thành `Nguyá»…n`, hex `C3A1C2BB…` thay vì `E1BB85`). Muốn sửa dữ liệu tiếng Việt
  bằng SQL trực tiếp thì dùng `UNHEX('…')` với chuỗi byte UTF-8 dựng sẵn, đừng gõ chữ có dấu vào lệnh
- Sau khi đăng nhập địa chỉ có đuôi `?continue` — hành vi mặc định của Spring Security 6, vô hại
- `application.properties` chứa mật khẩu MySQL `gbao`. Hiện không sao vì kho GitHub để **riêng tư**.
  **Nếu đổi kho sang công khai thì phải xóa dòng đó khỏi lịch sử trước** — nó nằm trong commit đầu `d48a491`,
  chuyển sang riêng tư rồi công khai lại không tự xóa được
- Máy này lưu sẵn thông tin đăng nhập GitHub của **hai tài khoản**. Remote cố ý ghi là
  `https://anh05022005@github.com/...` — có `anh05022005@` ở giữa thì Git mới lấy đúng tài khoản.
  Xóa phần đó đi là đẩy bằng tài khoản kia, báo lỗi 403
