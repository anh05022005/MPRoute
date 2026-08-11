# Tiến độ đồ án

> Trạng thái hiện tại của đồ án. **Giữ dưới 60 dòng.**
> Cập nhật sau mỗi mốc hoàn thành.

**Cập nhật lần cuối:** 08/08/2026

---

## Đang ở đâu

| | |
|---|---|
| Theo kế hoạch | Tuần 2 — nền tảng |
| Thực tế | Tuần 2 — mới dựng project Spring Boot |

---

## Bảng theo dõi 12 tuần

```
[x] T1  Chot de tai, hoi Minh Phu 3 cau
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

---

## Đã xong

- Viết xong 9 file đặc tả trong `docs/`
- Dựng project Spring Boot bằng Maven, chạy được
- Hạ Spring Boot từ 4.1.0 xuống **3.5.16**, thêm đủ 10 dependency (web, data-jpa, security, thymeleaf, validation, mail, thymeleaf-extras-springsecurity6, lombok, mysql-connector-j, test)
- Viết `application.properties` đầy đủ: chuỗi kết nối có encoding, `ddl-auto=update`, `show-sql=true`
- **Đổi quy tắc code:** bỏ luật "chỉ `for` thuần, cấm Lombok". Nay viết code như lập trình viên Java bình thường — Lombok, Stream API, lambda, annotation đầy đủ dùng thoải mái ở mọi nơi. Chỉ còn cấm thư viện VRP (OR-Tools/jsprit/OptaPlanner), cấm `float`, và cấm gọi OSRM/DB trong vòng lặp thuật toán
- **Đổi package** sang `com.minhphu.mproute`, lớp chính là `MpRouteApplication`. groupId/artifactId đổi thành `com.minhphu:mproute`
- **Kết nối MySQL chạy được:** database `mproute` đã tạo (utf8mb4 / utf8mb4_unicode_ci), MySQL 8.0.44, `mvnw test` xanh
- **Đổi toàn bộ định danh sang tiếng Anh:** tên bảng (số nhiều), tên cột, entity (số ít), giá trị enum, URL, tên lớp trong `service/optimization`. Comment vẫn tiếng Việt không dấu, chuỗi hiển thị vẫn tiếng Việt có dấu. Đã rà sạch trên 36 file docs
- **★ CN-00-02 ĐẠT:** 18 enum + 16 entity + 16 repository → `SHOW TABLES` ra đủ **16 bảng**. Không còn cột `ENUM` native. Đủ index và khóa: `idx_orders_date_status`, `uk_route_sequence`, `idx_status_history_entity`, `idx_customers_phone`, `uk_coordinate_pair`, unique `route_stops.order_id` (CBR-02). `mvnw test` xanh

---

## Đang làm

- (chưa có — vừa xong CN-00-02, chờ sang CN-00-03)

---

## Việc tiếp theo

1. `SecurityConfig` + đăng nhập, khóa 15 phút sau 5 lần sai (CN-00-03)
2. Phân quyền ba vai trò, gõ URL trái quyền ra trang 403 (CN-00-04)
3. Đổi mật khẩu (CN-00-05)
4. Layout Thymeleaf dùng chung, menu đổi theo vai trò (CN-00-06)
5. Trang chào ở `/` để đạt nốt CN-00-01

Xong 5 việc này là hết Nhóm 0 — Nền tảng, đúng lịch tuần 2.

---

## Cần quyết định

| Việc | Tình hình |
|---|---|
| **Cắt phạm vi — quyết trước tuần 8** | 86 chức năng bắt buộc / 12 tuần / một người. Tuần 8 phải xong ~20 chức năng, tuần 9 phải xong ~30. Ước lượng thật là 18–24 tuần. **Cắt chủ động ngay chứ đừng đợi tuần 9 mới cắt trong hoảng loạn.** |
| **Mâu thuẫn về thứ tự cắt** | `docs/00-tong-quan.md` xếp UC06 (sự cố) vào diện cắt sớm, nhưng kịch bản demo lại xếp bước 13 (ba phương án xử lý sự cố) là một trong bốn điểm nhấn. Gợi ý dung hòa: giữ UC06 nhưng làm **phương án A và C trước**, phương án B (Cheapest Insertion) làm sau nếu còn thời gian. Cắt hẳn RPT-01, RPT-02 và toàn bộ mục ○ ngay từ bây giờ |
| Thư viện xuất Excel | CN-08-05 cần Apache POI. Thêm vào `pom.xml` khi làm tới tuần 10, chưa cần bây giờ |

---

## Đã biết trước, cố ý chưa sửa

Ghi ở đây để không quên, và để khi nó lộ ra thì biết ngay là chuyện đã lường trước:

| Vấn đề | Khi nào lộ ra | Vì sao chưa sửa |
|---|---|---|
| NN để sót đơn dù tổng sức chứa đủ | Tuần 8, lần đầu chạy trên dữ liệu mẫu | **Cố ý giữ.** Đây là điểm nhấn demo bước 5a và là câu trả lời vấn đáp mạnh nhất. Đã ghi kịch bản trong `docs/08` |
| Trạng thái xe là cờ toàn cục, không theo ngày → không lập kế hoạch trước 2 ngày được | Chỉ lộ nếu thử chốt tuyến cho hai ngày liên tiếp | Sửa đúng thì phải đổi CBR-05 và tầng service. Không đáng đánh đổi thời gian trong 12 tuần. **Đã ghi vào mục hạn chế `docs/07` 4.7** |
| Ghi chú "chỉ nhận buổi sáng trước 11h" mà thuật toán không ràng buộc khung giờ | Tuần 8 khi nhìn ETA thật | Giữ nguyên ghi chú vì nó phản ánh khảo sát thật. Cần **chạy thử và ghi lại ETA điểm cuối từng tuyến** để trả lời bằng số |

---

## Nợ kỹ thuật

- (chưa có)
