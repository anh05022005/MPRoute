# 11 — Cài đặt trên một máy khác

Hướng dẫn dựng lại hệ thống từ đầu trên một máy chưa từng chạy dự án. Viết ra vì hai lý do:
để chuyển máy làm việc mà không mất buổi, và để làm phụ lục "Hướng dẫn cài đặt" của quyển
báo cáo.

---

## Clone về là đủ — trừ bản đồ

Kho git không chứa dữ liệu, nhưng **dữ liệu sinh lại được từ mã nguồn**: ba lớp seed dựng
toàn bộ tài khoản, danh mục và mười ngày lịch sử ngay lần khởi động đầu tiên. Không phải
mang bản kết xuất MySQL từ máy nào sang.

Thứ duy nhất không sinh lại được là dữ liệu bản đồ OSRM, và thiếu nó ứng dụng vẫn chạy.

Bảng dưới đây liệt kê chính xác cái gì có, cái gì không.

| Thứ | Có trong git? | Lấy ở đâu |
|---|---|---|
| Mã nguồn Java, template, CSS, JS | ✅ | `git clone` |
| Toàn bộ `docs/` | ✅ | `git clone` |
| Mười bài benchmark Augerat (`.vrp`) | ✅ | `git clone` |
| Maven wrapper (`mvnw.cmd`) | ✅ | `git clone` — tự tải Maven 3.9.16 lần chạy đầu |
| Thư viện (Spring, OR-Tools, POI) | ❌ | Maven tự tải về `~/.m2`, ~113 MB riêng OR-Tools |
| Bảng cơ sở dữ liệu | ❌ | Hibernate `ddl-auto=update` **tự sinh** khi chạy lần đầu |
| Năm tài khoản đăng nhập | ❌ | `DataSeeder` **tự tạo** khi chạy lần đầu |
| Chín tham số hệ thống | ❌ | `SystemParameterService.seedDefaults()` **tự tạo** |
| Danh mục: kho, sản phẩm, xe, tài xế, khách hàng | ❌ | `CatalogSeeder` **tự tạo** khi chạy lần đầu |
| Đơn hàng, tuyến, lịch sử 10 ngày | ❌ | `HistorySeeder` **tự sinh** khi bật cờ — xem Bước 5 |
| Dữ liệu bản đồ OSRM (327 MB + ~1,3 GB) | ❌ | Tải lại từ Geofabrik, xử lý lại — xem Bước 7 |
| `CLAUDE.md` và thư mục `.claude/` | ❌ | Bị chặn bằng `.git/info/exclude`, chép tay — xem cuối file |

Ba lớp seed chạy nối tiếp theo `@Order`: `DataSeeder` (1) tạo tài khoản và tham số hệ thống,
`CatalogSeeder` (2) tạo danh mục, `HistorySeeder` (3) sinh mười ngày lịch sử. Cả ba kiểm từng
bản ghi trước khi ghi nên chạy lại bao nhiêu lần cũng không sinh trùng.

Cho đến trước ngày 04/09/2026, danh mục vẫn phải nhập tay qua giao diện và chỉ tồn tại trong
MySQL của một máy — chuyển máy là mất buổi. `CatalogSeeder` sinh ra để trả món nợ đó.

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

## Bước 5 — Sinh mười ngày lịch sử

Bảng, tài khoản và danh mục thì ba lớp seed tự lo ở Bước 6, không phải làm gì. Riêng mười
ngày đơn hàng đã giao thì phải **bật cờ** mới sinh, vì nó ghi vào database khá nhiều và không
nên chạy mỗi lần khởi động:

```
mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--mproute.seed-history=true"
```

Chạy một lần là đủ. Những lần sau khởi động bình thường, dữ liệu vẫn còn.

**Bật OSRM trước khi chạy lệnh này** (Bước 7). Thiếu nó thì quãng đường tính bằng Haversine
nhân hệ số, và báo cáo RPT-03 sẽ ra những con số khác với những gì đã viết trong luận văn.

Bộ sinh ngẫu nhiên dùng hạt giống cố định (`RANDOM_SEED = 20260827L` trong `HistorySeeder`)
nên máy nào chạy cũng ra đúng bộ đơn như nhau — đó là lý do số liệu trong luận văn tái lập
được, và cũng là lý do **không được đổi thứ tự chèn khách hàng trong `CatalogSeeder`**.

### Muốn giống máy cũ đến từng bản ghi

Ba lớp seed đã sinh đúng bộ dữ liệu ấy rồi. Chỉ khi nào máy cũ có dữ liệu **nhập tay thêm
ngoài `docs/08`** — vài đơn thử, một khách mới thêm — mà muốn giữ, thì mới cần mang bản kết
xuất sang:

```
# tren may cu
docker exec <container-mysql> mysqldump -uroot -p<mk> --default-character-set=utf8mb4 mproute > mproute-full.sql

# tren may moi
docker exec -i <container-mysql> mysql -uroot -p<mk> --default-character-set=utf8mb4 mproute < mproute-full.sql
```

Cờ `--default-character-set=utf8mb4` bắt buộc ở cả hai đầu, thiếu là tiếng Việt trong file
hỏng. **Đừng commit file kết xuất lên git** — nó là dữ liệu, không phải mã nguồn.

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
