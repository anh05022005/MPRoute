# 11 — Cài đặt trên một máy khác

Hướng dẫn dựng lại hệ thống từ đầu trên một máy chưa từng chạy dự án. Viết ra vì hai lý do:
để chuyển máy làm việc mà không mất buổi, và để làm phụ lục "Hướng dẫn cài đặt" của quyển
báo cáo.

---

## Điều quan trọng nhất: clone về là **chưa đủ**

Kho git chứa toàn bộ mã nguồn và tài liệu, nhưng **không chứa dữ liệu**. Bảng dưới đây liệt
kê chính xác cái gì có, cái gì không — đọc kỹ chỗ này thì các bước sau chỉ là làm theo.

| Thứ | Có trong git? | Lấy ở đâu |
|---|---|---|
| Mã nguồn Java, template, CSS, JS | ✅ | `git clone` |
| Toàn bộ `docs/` | ✅ | `git clone` |
| Mười bài benchmark Augerat (`.vrp`) | ✅ | `git clone` |
| Maven wrapper (`mvnw.cmd`) | ✅ | `git clone` — tự tải Maven 3.9.16 lần chạy đầu |
| Thư viện (Spring, OR-Tools, POI) | ❌ | Maven tự tải về `~/.m2`, ~113 MB riêng OR-Tools |
| **Bảng cơ sở dữ liệu** | ❌ | Hibernate `ddl-auto=update` **tự sinh** khi chạy lần đầu |
| **Năm tài khoản đăng nhập** | ❌ | `DataSeeder` **tự tạo** khi chạy lần đầu |
| **Chín tham số hệ thống** | ❌ | `SystemParameterService.seedDefaults()` **tự tạo** |
| **Danh mục: kho, sản phẩm, xe, tài xế, khách hàng** | ❌ | **Phải mang bản kết xuất MySQL sang** — xem Bước 5 |
| **Đơn hàng, tuyến, lịch sử 10 ngày** | ❌ | Nằm trong cùng bản kết xuất đó |
| Dữ liệu bản đồ OSRM (327 MB + ~1,3 GB) | ❌ | Tải lại từ Geofabrik, xử lý lại — xem Bước 7 |
| `CLAUDE.md` và thư mục `.claude/` | ❌ | Bị chặn bằng `.git/info/exclude`, chép tay — xem cuối file |

Ba dòng in đậm là ba chỗ dễ mất buổi nhất. Đặc biệt dòng **danh mục**: `DataSeeder` chỉ tạo
tài khoản và tham số hệ thống, còn kho hàng, năm sản phẩm, bốn xe, ba tài xế và mười lăm
khách hàng thì được nhập tay qua giao diện trong các tuần 3–5. Chúng chỉ tồn tại trong MySQL
của máy cũ. Bỏ qua Bước 5 thì ứng dụng vẫn chạy, vẫn đăng nhập được, nhưng mọi màn hình đều
trống và không phân tuyến được gì.

---

## Bước 1 — Cài sẵn trên máy mới

| Cần | Kiểm bằng |
|---|---|
| JDK 21 trở lên | `java -version` |
| Docker | `docker ps` |
| MySQL 8 (máy đích đã có sẵn trong Docker) | `docker ps` thấy container MySQL |
| Git | `git --version` |

Không cần cài Maven — `mvnw.cmd` tự tải về. Không cần cài OR-Tools — Maven tự kéo thư viện
native, chỉ bản `ortools-win32-x86-64` được nạp lúc chạy.

---

## Bước 2 — Lấy mã nguồn

```
git clone https://github.com/anh05022005/MPRoute.git
cd MPRoute
```

Máy đích đã đăng nhập sẵn tài khoản `anh05022005` nên không phải nhập lại thông tin đăng nhập.

Muốn lấy đúng bản đã được đánh dấu là chạy được thay vì bản mới nhất:

```
git checkout t11-ban-demo
```

---

## Bước 3 — Tạo cơ sở dữ liệu rỗng

Chỉ tạo **database**, không cần tạo bảng — Hibernate tự sinh bảng từ các lớp entity.

```
docker exec -i <ten-container-mysql> mysql -uroot -p<mat-khau> -e "CREATE DATABASE mproute CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

`utf8mb4` là bắt buộc. Đặt sai bảng mã thì tên khách hàng tiếng Việt lưu vào thành dấu hỏi,
và lúc đó sửa rất mất công vì dữ liệu đã hỏng ngay từ khi ghi.

---

## Bước 4 — Khai báo mật khẩu MySQL

Ứng dụng đọc hai biến môi trường. Nếu MySQL trên máy mới dùng `root` và mật khẩu khác máy cũ
thì phải đặt lại, không thì Spring Boot khởi động là chết ngay ở bước mở kết nối.

PowerShell, đặt cho phiên hiện tại:

```
$env:MPROUTE_DB_USER = "root"
$env:MPROUTE_DB_PASSWORD = "<mat-khau-mysql-may-moi>"
```

Đặt vĩnh viễn (mở lại cửa sổ mới là có):

```
[Environment]::SetEnvironmentVariable("MPROUTE_DB_PASSWORD", "<mat-khau>", "User")
```

Không đặt gì thì `application.properties` lùi về mặc định `root` / `gbao` của máy cũ.

---

## Bước 5 — Mang dữ liệu sang

Đây là bước duy nhất git không làm thay được.

**Trên máy cũ**, kết xuất toàn bộ database ra một file:

```
docker exec <ten-container-mysql> mysqldump -uroot -p<mat-khau> --default-character-set=utf8mb4 mproute > mproute-full.sql
```

`--default-character-set=utf8mb4` là bắt buộc, thiếu nó thì tiếng Việt trong file kết xuất
hỏng. Chép `mproute-full.sql` sang máy mới bằng USB hoặc ổ đám mây — **đừng commit nó lên
git**, file này chứa dữ liệu và không thuộc về mã nguồn.

**Trên máy mới**, nạp vào:

```
docker exec -i <ten-container-mysql> mysql -uroot -p<mat-khau> --default-character-set=utf8mb4 mproute < mproute-full.sql
```

Nạp bằng đường này thì có luôn cả bảng, cả danh mục, cả mười ngày lịch sử — Bước 6 chạy phát
là dùng được ngay, không cần seed lại gì.

### Nếu không mang được file kết xuất

Vẫn dựng lại được, nhưng mất khoảng một tiếng nhập tay:

1. Chạy Bước 6 với database rỗng. Hibernate sinh bảng, `DataSeeder` tạo năm tài khoản
   (`admin/admin123`, `dieuphoi/dieuphoi123`, `taixe01..03/taixe123`) và chín tham số hệ thống.
2. Đăng nhập `admin`, nhập tay theo đúng số liệu ở `docs/08-du-lieu-mau.md`: kho Phan Trọng Tuệ,
   năm sản phẩm, bốn xe, ba tài xế, mười lăm khách hàng.
3. Sinh mười ngày lịch sử bằng `HistorySeeder`:

   ```
   mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--mproute.seed-history=true"
   ```

   Bộ sinh ngẫu nhiên dùng hạt giống cố định (`RANDOM_SEED = 20260827L`) nên ra đúng bộ đơn
   như máy cũ, con số trong luận văn vẫn khớp. Bật OSRM trước khi chạy, thiếu nó thì quãng
   đường tính bằng Haversine và báo cáo RPT-03 lệch so với số đã viết trong báo cáo.

---

## Bước 6 — Chạy

```
mvnw.cmd spring-boot:run
```

Lần đầu mất vài phút vì Maven tải thư viện. Xong thì mở `http://localhost:8080`.

Cổng 8080 bận thì đổi:

```
mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"
```

Kiểm nhanh phần thuật toán, **không cần web, không cần MySQL, không cần OSRM**:

```
mvnw.cmd test
```

Phải ra 17/17 xanh. Ra xanh đủ 17 là chắc chắn phần lõi đồ án đã dựng đúng trên máy mới.

---

## Bước 7 — OSRM (nên có, nhưng không bắt buộc)

Thiếu OSRM thì ứng dụng **vẫn chạy**: `DistanceMatrixService` lặng lẽ lùi về Haversine nhân
hệ số `HAVERSINE_FACTOR = 1.45`. Quãng đường sẽ lệch so với số trong báo cáo, và bản đồ vẽ
tuyến bằng đường thẳng nối các điểm thay vì bám theo đường phố. Demo trước hội đồng thì nên
có.

Dữ liệu bản đồ **không nằm trong git** (327 MB file gốc, khoảng 1,3 GB sau xử lý). Dựng lại:

```
# 1. Tai ban do Viet Nam tu Geofabrik (~327 MB)
#    https://download.geofabrik.de/asia/vietnam-latest.osm.pbf
#    De vao mot thu muc RIENG, ngoai kho git

# 2. Tien xu ly — khoang 10 phut, dinh 7,2 GB RAM o buoc extract
docker run -t -v "${PWD}:/data" osrm/osrm-backend osrm-extract -p /opt/car.lua /data/vietnam-latest.osm.pbf
docker run -t -v "${PWD}:/data" osrm/osrm-backend osrm-partition /data/vietnam-latest.osrm
docker run -t -v "${PWD}:/data" osrm/osrm-backend osrm-customize /data/vietnam-latest.osrm

# 3. Chay may chu, dat ten container de lan sau chi can "docker start"
docker run -t -i -p 5000:5000 -v "${PWD}:/data" --name osrm-vietnam osrm/osrm-backend osrm-routed --algorithm mld /data/vietnam-latest.osrm
```

Máy 8 GB RAM chạy `osrm-extract` sẽ chết vì thiếu bộ nhớ — khi đó phải tải bản đồ vùng nhỏ
hơn thay vì cả nước.

Những lần sau chỉ cần `docker start osrm-vietnam`.

Địa chỉ máy chủ OSRM **không nằm trong `application.properties`** mà là tham số hệ thống
trong database (`OSRM_URL`, mặc định `http://localhost:5000`). Đổi được ngay trên giao diện
quản trị mà không phải dịch lại mã nguồn.

---

## Kiểm lại đã cài đúng chưa

- [ ] `mvnw.cmd test` ra 17/17 xanh
- [ ] Vào được `http://localhost:8080`, đăng nhập `admin/admin123`
- [ ] Màn hình danh mục có đủ 5 sản phẩm, 4 xe, 3 tài xế, 15 khách hàng
- [ ] Tên tiếng Việt hiện đúng dấu, không phải dấu hỏi hay ký tự lạ
- [ ] Báo cáo RPT-03 có dữ liệu, không trống
- [ ] Đăng nhập `taixe02` rồi gõ thẳng `/admin/products` → ra trang 403
- [ ] Phân tuyến thử một ngày, bản đồ vẽ tuyến bám theo đường phố (nếu đã bật OSRM)

---

## Hai cái bẫy đã vấp rồi, ghi lại để khỏi vấp lại

**Múi giờ.** Nếu MySQL chạy trong WSL thì đồng hồ máy chủ là UTC, lệch 7 tiếng so với giờ Việt
Nam. Chuỗi kết nối trong `application.properties` đã có `forceConnectionTimeZoneToSession=true`
để xử lý, nhưng nếu tự viết truy vấn bằng `docker exec ... mysql` thì `CURDATE()` vẫn trả sai
ngày trong khoảng 0h–7h. Đừng vội kết luận là ứng dụng lọc sai.

**`ddl-auto=update` không sửa được ràng buộc CHECK.** Trên database rỗng thì Hibernate sinh
đúng ngay từ đầu nên không sao. Nhưng sau này nếu thêm giá trị mới vào một enum thì lệnh
INSERT sẽ chết với `Check constraint ... is violated` — phải `ALTER TABLE` bằng tay, Hibernate
không tự sửa ràng buộc cũ.

---

## Chuyển bộ công cụ trợ lý mã nguồn

`CLAUDE.md` và thư mục `.claude/` **cố ý không đưa lên git** — chúng bị chặn trong
`.git/info/exclude`, mà file đó là file cục bộ nên bản thân nó cũng không được đẩy lên. Vì vậy
clone về máy mới sẽ **không có** hai thứ này, và trợ lý mã nguồn bên đó sẽ không biết quy ước
riêng của đồ án: đặt tên bằng tiếng Anh, comment tiếng Việt không dấu, cấm OR-Tools trong
`optimization/heuristic/`, cấm `float`, phải cộng chặng cuối về kho.

Muốn có thì chép tay sang, đặt vào đúng thư mục gốc dự án:

```
CLAUDE.md
.claude/
```

Chép xong thì tạo lại phần chặn trên máy mới, không thì lần commit sau chúng sẽ bị đẩy lên:

```
git clone https://github.com/anh05022005/MPRoute.git
cd MPRoute
printf '.claude/\nCLAUDE.md\n' >> .git/info/exclude
```

Làm ngược thứ tự — chép file vào trước khi ghi dòng chặn — thì `git status` sẽ hiện chúng ra,
và chỉ cần một lần `git add .` là lên kho.
