# Báo cáo cuối kỳ - Phần của Tăng Thoại Lâm (bản tiếng Việt)

Bản dịch tiếng Việt của `lam.md`, dùng cho `build_report.py vi`. Nội dung giống bản tiếng Anh; khi sửa một bản thì sửa cả bản kia.

## Chương 2: Cơ sở lý thuyết và phân tích yêu cầu

### 2.1 Công nghệ và kiến trúc

#### 2.1.1 Kiến trúc phân tầng
Backend được chia thành các tầng [3], [6]. Mỗi tầng chỉ làm việc với tầng ngay bên dưới.

| Tầng | Package | Nhiệm vụ | Ví dụ |
|---|---|---|---|
| Trình bày | `controller`, `dto` | Nhận yêu cầu HTTP, kiểm tra vai trò và định dạng đầu vào, trả về JSON | `OrderController`, `OrderLineRequest` |
| Nghiệp vụ | `service`, `domain` | Chạy use case trong một giao dịch và áp dụng các quy tắc nghiệp vụ | `OrderService`, `CustomerOrder.sendToKitchen` |
| Truy cập dữ liệu | `repository` | Đọc và ghi cơ sở dữ liệu | `CustomerOrderRepository` |
| Dùng chung | `exception`, `config` | Đưa mọi lỗi về một dạng JSON; tạo dữ liệu mẫu | `GlobalExceptionHandler`, `OrderSeeder` |

Các quy tắc nghiệp vụ nằm trong lớp miền, không nằm trong controller hay React.
Một quy tắc như BR-06 chỉ được viết một lần, trong `CustomerOrder`, và kiểm thử đơn vị có thể kiểm tra nó mà không cần cơ sở dữ liệu hay trình duyệt.
Ứng dụng React chỉ hiển thị những gì server cho phép, và server kiểm tra lại mọi quy tắc.

#### 2.1.2 Spring Boot, JPA và Flyway
- **Spring Boot** [8] khởi động web server và nối các lớp với nhau (dependency injection). Một service nhận repository qua hàm khởi tạo, nên kiểm thử có thể truyền repository khác vào.
- **Spring Data JPA với Hibernate** ánh xạ mỗi lớp miền vào một bảng. Repository là một interface; Spring tự viết câu SQL cho các phương thức như `findByTableIdAndStatusIn`.
- **Flyway** quản lý lược đồ cơ sở dữ liệu, chạy trên MySQL 8 [10]. Mỗi thay đổi là một tệp SQL có đánh số (`V1` Giang, `V2` và `V3` Lâm, `V4` Khoa). Hibernate chỉ ánh xạ các bảng và không bao giờ thay đổi chúng (`ddl-auto=none`). Vì vậy cơ sở dữ liệu của mọi thành viên có cùng lược đồ.

#### 2.1.3 Giao dịch và khóa
Hai vấn đề trong lý thuyết cơ sở dữ liệu xuất hiện trong nhà hàng:
- **Mất cập nhật (lost update).** Hai nhân viên sửa cùng một dòng cùng lúc, và thay đổi này ghi đè thay đổi kia.
- **Làm dở dang.** Một yêu cầu lỗi giữa chừng, và dữ liệu bị thay đổi một nửa.

Dự án dùng ba công cụ để chống lại các vấn đề này:
- **Giao dịch.** Mỗi phương thức service chạy trong một giao dịch (`@Transactional`). Nếu một bước bất kỳ lỗi, không có gì được lưu. NFR-07 dựa vào điều này: gửi bếp thất bại thì đơn vẫn chưa được gửi.
- **Khóa bi quan (pessimistic locking)** [6]. `SELECT … FOR UPDATE` khóa một dòng đến khi giao dịch kết thúc, và yêu cầu thứ hai phải chờ. Khóa này dùng cho dòng bàn khi đặt bàn (NFR-06) và dòng đơn gọi món khi gửi bếp (NFR-07).
- **Khóa lạc quan (optimistic locking)** [6]. Cột `version` được kiểm tra khi lưu dòng. Nếu người khác đã lưu trước, lần lưu thất bại và người dùng nhận lỗi 409. Khóa này dùng cho các món trên phiếu bếp, nơi đầu bếp và phục vụ có thể cùng sửa một món.

#### 2.1.4 Spring Security
- Đăng nhập tạo một phiên trên server. Trình duyệt chỉ giữ cookie phiên (`SameSite=Strict`).
- Mật khẩu được lưu dưới dạng băm BCrypt (NFR-10).
- Mỗi endpoint ghi rõ các vai trò được phép gọi, ví dụ `@PreAuthorize(AccessRules.KITCHEN)`. Yêu cầu từ vai trò khác nhận lỗi 403, kể cả khi ai đó tự gõ URL.

#### 2.1.5 Ứng dụng React và short polling
- Phía client là ứng dụng một trang (SPA) viết bằng React [9] với React Router, build bằng Vite. Khi phát triển, Vite chuyển tiếp `/api` đến Spring Boot, nên trình duyệt chỉ thấy một nguồn (origin).
- Màn hình bếp và màn hình gọi món phải thay đổi khi người dùng khác thao tác (NFR-02, NFR-03). Dự án dùng **short polling**: trang hỏi lại server mỗi 2 hoặc 3 giây. Cách này đơn giản hơn WebSocket và đáp ứng cả hai giới hạn. Cái giá là một yêu cầu nhỏ mỗi vài giây cho mỗi màn hình đang mở.

#### 2.1.6 Kiểm thử
- Kiểm thử đơn vị **JUnit 5** [11] kiểm tra các quy tắc miền mà không cần Spring.
- Kiểm thử API **Spring MockMvc** gửi yêu cầu HTTP thật đến toàn bộ ứng dụng trên cơ sở dữ liệu **H2** trong bộ nhớ ở chế độ MySQL. Mỗi kiểm thử chạy trong một giao dịch và được hoàn tác (rollback).
- **Spring Security Test** đăng nhập với một vai trò định trước cho mỗi yêu cầu.

#### 2.1.7 Thư viện và giấy phép

| Thư viện | Phiên bản | Giấy phép |
|---|---|---|
| Spring Boot (Web, Data JPA, Security, Validation) | 3.5.16 | Apache 2.0 |
| Hibernate ORM (qua Spring Data JPA) | 6.6.53 | LGPL 2.1 |
| Flyway | 11.7.2 | Apache 2.0 |
| MySQL Connector/J | 9.7.0 | GPL 2.0 với Universal FOSS Exception |
| H2 Database | 2.3.232 | MPL 2.0 hoặc EPL 1.0 |
| JUnit 5, Mockito (qua spring-boot-starter-test) | 5.12.2; 5.17.0 | EPL 2.0; MIT |
| React | 18.3.1 | MIT |
| React Router | 6.30.6 | MIT |
| Vite | 6.4.5 | MIT |

### 2.2 Quy trình hiện tại của nhà hàng

Ngày 18/10/2026, nhóm phỏng vấn một thu ngân và một phục vụ của một nhà hàng bít tết và mì Ý ở quận Gò Vấp (`docs/interview/restaurant-interview-notes.md`). Tên nhà hàng không được ghi lại.

- **Đặt bàn** đến qua các nền tảng mạng xã hội. Vấn đề thường gặp nhất là khách đặt bàn, đôi khi đặt món trước, rồi không đến (F1, F2).
- **Bàn.** Một lượt đặt giữ bàn tối đa 3 giờ, và dọn bàn mất 15 phút. Khách trễ được giữ bàn 45 phút; sau 30 phút nhân viên gọi điện cho khách (F3 đến F5).
- **Gọi món** đã được nhập trên một ứng dụng gọi món. Ứng dụng này không liên kết với đặt bàn hay thanh toán (F9).
- **Món hết hàng** được báo bằng lời: bếp báo cho phục vụ, phục vụ báo cho khách (F9).
- **Thay đổi món sau khi bếp đã nấu** hiếm khi xảy ra, và nhân viên chưa phải xử lý trường hợp nào (F10).
- **Hóa đơn** hiếm khi được chia tại bàn. Cuối ca, tiền mặt được đếm, còn ảnh chụp chuyển khoản được gửi vào nhóm chat của nhân viên (F11, F12).

Mô tả này thay cho mô tả trong Proposal về phiếu gọi món viết tay và sổ đặt bàn.
Hệ thống lấp đầy cùng những khoảng trống:
- các bước của một lượt khách không được liên kết với nhau;
- tin món hết hàng được truyền bằng lời;
- không có gì ngăn một bàn bị hứa cho hai nhóm khách.

### 2.3 Phân tích yêu cầu

Các yêu cầu dưới đây là của Proposal, đã áp dụng những thay đổi sau buổi phỏng vấn (`docs/requirements/requirement-changes.md`, CR-01 đến CR-05).

#### 2.3.1 Tác nhân

| Tác nhân | Mô tả |
|---|---|
| Phục vụ/Lễ tân | Nhân viên khu phục vụ: đặt bàn, xếp chỗ, danh sách chờ, gọi món, hóa đơn và thanh toán |
| Nhân viên bếp | Xem phiếu bếp trên màn hình bếp, cập nhật trạng thái chế biến, đánh dấu món hết hàng |
| Quản lý | Cấu hình bàn và thực đơn, mở và đóng ca, xem báo cáo, làm được mọi việc của Phục vụ/Lễ tân |

#### 2.3.2 Yêu cầu chức năng

Độ ưu tiên: Must, Should hoặc Could. Yêu cầu đã thay đổi được ghi kèm mã thay đổi.

| Mã | Yêu cầu | Ưu tiên | Phân hệ |
|---|---|---|---|
| FR-01 | Hệ thống phải yêu cầu mọi người dùng đăng nhập bằng tên đăng nhập và mật khẩu, và chỉ hiển thị các màn hình và thao tác được phép cho vai trò của người dùng đó. | Must | Giang |
| FR-02 | Hệ thống phải cho phép Quản lý tạo, sửa và ngừng sử dụng bàn, với số bàn duy nhất, sức chứa từ 1 đến 20 và khu vực. Bàn còn lượt đặt trong tương lai thì không được ngừng sử dụng. | Must | Giang |
| FR-03 | Hệ thống phải cho phép Phục vụ/Lễ tân tìm bàn trống theo ngày, giờ bắt đầu và số khách, chỉ trả về các bàn có sức chứa không nhỏ hơn số khách và không có lượt đặt trùng giờ. | Must | Giang |
| FR-04 | Hệ thống phải cho phép Phục vụ/Lễ tân tạo lượt đặt bàn với tên khách, số điện thoại, số khách, ngày, giờ bắt đầu và ghi chú tùy chọn, và phải từ chối nếu lượt đặt trùng giờ với lượt đặt khác trên cùng bàn (BR-01). | Must | Giang |
| FR-05 | Hệ thống phải cho phép Phục vụ/Lễ tân sửa hoặc hủy lượt đặt trước giờ bắt đầu, và nhận bàn cho lượt đặt khi khách đến. | Must | Giang |
| FR-06 | Hệ thống phải đánh dấu lượt đặt là Không đến khi khách chưa nhận bàn sau 45 phút kể từ giờ bắt đầu, và trả lại bàn. Khi khách trễ từ 30 đến 45 phút, lượt đặt vẫn ở trạng thái Đã xác nhận để nhân viên gọi điện cho khách. **(CR-01)** | Should | Giang |
| FR-07 | Hệ thống phải cho phép Phục vụ/Lễ tân thêm nhóm khách vãng lai vào danh sách chờ theo thứ tự đến, và hiển thị vị trí của từng nhóm. | Must | Giang |
| FR-08 | Hệ thống phải cho phép Phục vụ/Lễ tân xếp nhóm chờ đầu tiên có số người vừa với một bàn vừa chuyển sang Trống. | Must | Giang |
| FR-09 | Hệ thống phải tuân theo vòng đời trạng thái bàn (BR-05); bàn ở trạng thái Cần dọn không được giao cho khách đến khi được đánh dấu đã dọn. | Must | Giang |
| FR-10 | Hệ thống phải hiển thị sơ đồ tổng quan của mọi bàn với số bàn, khu vực, sức chứa và trạng thái, tô màu theo trạng thái. | Should | Giang |
| FR-11 | Hệ thống phải cho phép Phục vụ/Lễ tân tạo đơn gọi món cho bàn Có khách bằng cách chọn món từ thực đơn nhóm theo danh mục; món hết hàng không được hiển thị. | Must | Lâm |
| FR-12 | Hệ thống phải cho phép Phục vụ/Lễ tân thêm món, đổi số lượng (1-50), xóa món và thêm ghi chú tối đa 200 ký tự cho từng dòng món trước khi gửi bếp. | Must | Lâm |
| FR-13 | Hệ thống phải gán mỗi món trong thực đơn vào một lượt món (Khai vị, Món chính, Tráng miệng) và gửi món xuống bếp theo thứ tự lượt món (BR-06). | Must | Lâm |
| FR-14 | Khi Phục vụ/Lễ tân gửi đơn, hệ thống phải tạo một phiếu bếp chứa các món đã gửi và hiển thị phiếu trên màn hình bếp. | Must | Lâm |
| FR-15 | Hệ thống phải cho phép Nhân viên bếp đổi trạng thái từng món trên phiếu từ Mới sang Đang làm sang Sẵn sàng, và hiển thị trạng thái cho Phục vụ/Lễ tân. | Must | Lâm |
| FR-16 | Hệ thống phải cho phép Quản lý thêm, sửa và ẩn món, với tên, danh mục, lượt món và giá lớn hơn 0 VND. Món đã có trong đơn cũ chỉ được ẩn, không được xóa. | Must | Lâm |
| FR-17 | Hệ thống phải cho phép Nhân viên bếp hoặc Quản lý đánh dấu món hết hàng hoặc có hàng trở lại; món hết hàng biến mất khỏi mọi màn hình gọi món (BR-07). | Must | Lâm |
| FR-18 | Hệ thống phải từ chối sửa hoặc hủy một món khi món đó trên phiếu bếp đã Đang làm (BR-08); vẫn được thêm món mới, và món mới được gửi bằng phiếu mới. | Must | Lâm |
| FR-19 | Hệ thống phải cho phép Phục vụ/Lễ tân chuyển đơn đang mở sang bàn khác. | Could | Lâm |
| FR-20 | Hệ thống phải cho phép Phục vụ/Lễ tân xem các đơn của một bàn trong ca hiện tại. | Could | Lâm |
| FR-21 | Hệ thống phải cho phép Phục vụ/Lễ tân tạo hóa đơn cho bàn Có khách từ đơn của bàn, sao chép từng dòng món với số lượng, đơn giá và thành tiền (BR-09). | Must | Khoa |
| FR-22 | Hệ thống phải hiển thị hóa đơn với các dòng món, tổng tiền, số tiền đã trả và số tiền còn lại. | Must | Khoa |
| FR-23 | Hệ thống phải cho phép Phục vụ/Lễ tân chia đều hóa đơn thành 2 đến 20 phần theo quy tắc làm tròn BR-10. | Must | Khoa |
| FR-24 | Hệ thống phải cho phép Phục vụ/Lễ tân chia hóa đơn theo món, mỗi dòng hóa đơn thuộc đúng một phần. **(CR-03: trước là Should)** | Could | Khoa |
| FR-25 | Hệ thống phải cho phép Phục vụ/Lễ tân ghi nhận một hoặc nhiều lần thanh toán (tiền mặt, thẻ, chuyển khoản, ví điện tử) cho một hóa đơn hoặc một phần, và tính tiền thối cho thanh toán tiền mặt (BR-11). **(CR-04)** | Must | Khoa |
| FR-26 | Hệ thống phải đóng hóa đơn khi được trả đủ; hóa đơn đã đóng chỉ được xem (BR-12) và bàn chuyển sang Cần dọn (BR-05). | Must | Khoa |
| FR-27 | Hệ thống phải cho phép Quản lý mở ca và đóng ca; mọi khoản thanh toán thuộc về ca đang mở khi ghi nhận (BR-13). | Must | Khoa |
| FR-28 | Hệ thống phải cho phép Quản lý tạo báo cáo ca gồm số hóa đơn đã đóng, tổng doanh thu, doanh thu theo phương thức thanh toán và số hóa đơn đã chia. | Must | Khoa |
| FR-29 | Khi đóng ca, hệ thống phải cho Quản lý nhập số tiền mặt đếm được; báo cáo ca hiển thị tiền mặt dự kiến, tiền mặt đếm được và chênh lệch. **(CR-05, mới)** | Could | Khoa |

Nếu thiếu thời gian, các yêu cầu được cắt theo thứ tự: FR-19, FR-20, FR-24, sau đó FR-10 và FR-06 (CR-03).

#### 2.3.3 Yêu cầu phi chức năng

| Mã | Loại | Yêu cầu |
|---|---|---|
| NFR-01 | Hiệu năng | Tìm bàn trống trả kết quả trong 2 giây với tối đa 50 bàn và 500 lượt đặt mỗi tuần. |
| NFR-02 | Hiệu năng | Đơn đã gửi xuất hiện trên màn hình bếp trong 3 giây. |
| NFR-03 | Hiệu năng | Việc đánh dấu món hết hàng được phản ánh trên mọi màn hình gọi món đang mở trong 5 giây, không cần tải lại trang. |
| NFR-04 | Hiệu năng | Hóa đơn tối đa 50 dòng được hiển thị trong 2 giây; báo cáo ca tối đa 300 hóa đơn được tạo trong 3 giây. |
| NFR-05 | Dễ sử dụng | Phục vụ/Lễ tân tạo lượt đặt bàn trong tối đa 3 bước, nhập một đơn chuẩn 5 món trong tối đa 60 giây, và ghi nhận một lần thanh toán trong tối đa 4 bước từ màn hình bàn. |
| NFR-06 | Toàn vẹn dữ liệu | Khi hai người dùng cùng lúc xác nhận cùng một bàn cho khoảng thời gian chồng nhau, chỉ đúng một lượt đặt được lưu và người kia thấy thông báo xung đột. |
| NFR-07 | Toàn vẹn dữ liệu | Gửi đơn là thao tác nguyên tử: nếu lưu thất bại, đơn vẫn chưa gửi, không có phiếu bếp nào được tạo, và gửi lại không bao giờ tạo phiếu trùng. |
| NFR-08 | Toàn vẹn dữ liệu | Ghi nhận lần thanh toán cuối và đóng hóa đơn diễn ra trong cùng một giao dịch cơ sở dữ liệu; tiền được lưu bằng số nguyên VND, không bao giờ dùng số thực. |
| NFR-09 | Tin cậy | Mọi dữ liệu được lưu trong MySQL và không thay đổi sau khi khởi động lại ứng dụng. |
| NFR-10 | Bảo mật | Mật khẩu chỉ được lưu dưới dạng băm BCrypt; kiểm tra vai trò được thực hiện ở server, và yêu cầu ngoài quyền của vai trò nhận phản hồi "từ chối truy cập". |
| NFR-11 | Kiểm tra dữ liệu | Mọi đầu vào được kiểm tra ở server; đầu vào sai tạo thông báo cạnh ô nhập và không bao giờ gây lỗi chưa xử lý. |
| NFR-12 | Khả chuyển | Ứng dụng chạy trên Windows và macOS với Java 17, Node.js 18 trở lên và MySQL 8, trên phiên bản hiện tại của Chrome và Edge, và build được từ một bản clone mới theo README. |

#### 2.3.4 Quy tắc nghiệp vụ

| Mã | Quy tắc | Phụ trách |
|---|---|---|
| BR-01 | **Không đặt trùng bàn.** Hai lượt đặt của cùng một bàn trùng nhau nếu `startA < endB + turnTime` và `startB < endA + turnTime`, với `end = start + diningDuration`. Lượt đặt trùng giờ bị từ chối; khách vãng lai không được xếp vào bàn có lượt đặt tiếp theo bắt đầu trong vòng một thời gian dùng bữa cộng thời gian dọn bàn. Mặc định: 180 và 15 phút, do Quản lý cài đặt **(CR-02: trước là 90 và 15)**. | Giang |
| BR-02 | **Vừa sức chứa.** Nhóm khách chỉ được xếp vào bàn có sức chứa không nhỏ hơn số khách. | Giang |
| BR-03 | **Dọn trước khi dùng lại.** Bàn Cần dọn không được giao cho khách đến khi được đánh dấu đã dọn. | Giang |
| BR-04 | **Thứ tự danh sách chờ.** Khách vãng lai được xếp theo thứ tự đến; nhóm đến sau chỉ được xếp trước nếu không có nhóm đến trước nào vừa với bàn trống. | Giang |
| BR-05 | **Vòng đời của bàn.** Trống → Đã đặt trước (từ 30 phút trước một lượt đặt đã xác nhận) → Có khách (nhận bàn, kể cả nhận sớm từ Trống, hoặc xếp khách vãng lai) → Cần dọn (tự động khi hóa đơn của bàn được đóng, hoặc thủ công khi bàn không còn đơn đang mở) → Trống (đánh dấu đã dọn). Khách không đến, hủy, hoặc đổi giờ hay đổi bàn đưa bàn từ Đã đặt trước về Trống. Không cho phép bước chuyển nào khác. | Giang |
| BR-06 | **Thứ tự lượt món.** Trong một đơn, các món được gửi theo thứ tự lượt món (Khai vị → Món chính → Tráng miệng): món của lượt sau không được gửi khi trong cùng đơn vẫn còn món của lượt trước chưa gửi. Màn hình bếp liệt kê món theo lượt món, rồi theo giờ gửi. | Lâm |
| BR-07 | **Hết hàng.** Món hết hàng bị gỡ khỏi mọi màn hình gọi món và không thể thêm hay gửi; các món đã nằm trong phiếu đã gửi không bị ảnh hưởng. | Lâm |
| BR-08 | **Khóa khi bếp đã nấu.** Khi một món trên phiếu đã Đang làm thì không được sửa hay hủy. Vẫn được thêm món mới vào đơn, và món mới được gửi bằng phiếu mới. | Lâm |
| BR-09 | **Chỉ lập hóa đơn khi bếp xong.** Chỉ được tạo hóa đơn khi không còn món nào của đơn ở trạng thái Mới hoặc Đang làm. Tạo hóa đơn sẽ khóa đơn; muốn thêm món thì phải hủy hóa đơn chưa trả trước, và chỉ được hủy khi chưa ghi nhận khoản thanh toán nào. Hóa đơn đã hủy được giữ lại với trạng thái Đã hủy, nên mỗi đơn có tối đa một hóa đơn chưa bị hủy. | Khoa |
| BR-10 | **Làm tròn khi chia đều.** Với N phần, mỗi phần là `floor(total / N / 1,000) × 1,000` VND và phần dư được cộng vào phần đầu tiên, nên tổng các phần luôn đúng bằng tổng hóa đơn. Ví dụ: 1.000.000 VND chia 3 được 334.000 + 333.000 + 333.000. | Khoa |
| BR-11 | **Thanh toán hợp lệ.** Số tiền thanh toán phải lớn hơn 0. Thanh toán bằng thẻ, chuyển khoản và ví điện tử không được vượt quá số tiền còn lại; thanh toán tiền mặt được vượt, và tiền thối được ghi lại **(CR-04)**. | Khoa |
| BR-12 | **Hóa đơn đã đóng là cuối cùng.** Hóa đơn chỉ đóng khi các khoản thanh toán đủ tổng tiền. Hóa đơn đã đóng không được sửa, xóa hay nhận thêm thanh toán. | Khoa |
| BR-13 | **Kiểm soát ca.** Chỉ được ghi nhận thanh toán khi có ca đang mở; mỗi lúc chỉ có một ca mở; không được đóng ca khi còn hóa đơn chưa trả. | Khoa |

#### 2.3.5 Use case
Đặc tả đầy đủ nằm ở Phụ lục A. Sơ đồ use case, vẽ theo UML [5], là `docs/diagrams/giang/01-use-case.png`.

| Mã | Use case | Tác nhân chính | Yêu cầu | Phụ trách |
|---|---|---|---|---|
| UC-01 | Đăng nhập và đăng xuất | Mọi vai trò | FR-01 | Giang |
| UC-02 | Quản lý bàn và khu vực | Quản lý | FR-02 | Giang |
| UC-03 | Tạo lượt đặt bàn | Phục vụ/Lễ tân | FR-03, FR-04 | Giang |
| UC-04 | Sửa, hủy hoặc nhận bàn cho lượt đặt | Phục vụ/Lễ tân | FR-05, FR-06 | Giang |
| UC-05 | Quản lý danh sách chờ khách vãng lai | Phục vụ/Lễ tân | FR-07, FR-08 | Giang |
| UC-06 | Cập nhật trạng thái bàn | Phục vụ/Lễ tân | FR-09, FR-10 | Giang |
| UC-07 | Quản lý thực đơn | Quản lý | FR-16 | Lâm |
| UC-08 | Tạo đơn và gửi bếp | Phục vụ/Lễ tân | FR-11 đến FR-14, FR-18 | Lâm |
| UC-09 | Xử lý phiếu bếp | Nhân viên bếp | FR-15 | Lâm |
| UC-10 | Đánh dấu món hết hàng hoặc có hàng trở lại | Nhân viên bếp, Quản lý | FR-17 | Lâm |
| UC-11 | Mở và đóng ca | Quản lý | FR-27, FR-29 | Khoa |
| UC-12 | Tạo hóa đơn | Phục vụ/Lễ tân | FR-21, FR-22 | Khoa |
| UC-13 | Chia hóa đơn | Phục vụ/Lễ tân | FR-23, FR-24 | Khoa |
| UC-14 | Ghi nhận thanh toán và đóng hóa đơn | Phục vụ/Lễ tân | FR-25, FR-26 | Khoa |
| UC-15 | Tạo báo cáo ca | Quản lý | FR-28, FR-29 | Khoa |

#### 2.3.6 Thay đổi so với Proposal

| Thay đổi | Nội dung | Lý do (phát hiện từ phỏng vấn) |
|---|---|---|
| CR-01 | Khách không đến sau 45 phút thay vì 15 phút | Khách trễ được giữ bàn 45 phút và được gọi điện sau 30 phút (F5) |
| CR-02 | Thời gian dùng bữa mặc định 180 phút thay vì 90 phút | Một lượt đặt giữ bàn tối đa 3 giờ (F3) |
| CR-03 | FR-24 chia theo món hạ xuống Could | Hóa đơn hiếm khi được chia tại bàn (F11) |
| CR-04 | Thêm ví điện tử làm phương thức thanh toán | Nhà hàng nhận mọi phương thức phổ biến (F12) |
| CR-05 | FR-29 mới: tiền mặt đếm được trong báo cáo ca | Tiền mặt được đếm thủ công cuối ca (F12) |

Các yêu cầu của phân hệ Gọi món & Bếp không thay đổi. Buổi phỏng vấn xác nhận FR-17 và BR-07 (hiện nay tin hết hàng được truyền bằng lời) và giữ BR-08, dù khách hiếm khi đổi món sau khi bếp đã nấu (F9, F10).

## Chương 3: Các mục của phân hệ Gọi món & Bếp

### 3.1 Mô tả bài toán và mục tiêu hệ thống (phần phân hệ)

**Phát biểu bài toán.**
Giữa bàn ăn và nhà bếp, có ba điều thường sai:
- Bếp nhận món sai thứ tự, nên món chính ra trước món khai vị.
- Một món hết, nhưng phục vụ biết muộn và vẫn tiếp tục nhận món đó.
- Khách đổi một món đang được nấu.

Ứng dụng gọi món của nhà hàng không biết bàn nào đã có khách, và tin hết hàng được truyền bằng lời.

**Mục tiêu của phân hệ.**
- Chỉ nhận đơn cho bàn đã có khách, từ thực đơn đã ẩn các món hết hàng (FR-11, BR-07).
- Gửi món theo thứ tự lượt món và hiển thị ngay cho bếp (BR-06, NFR-02).
- Không bao giờ gửi một món hai lần, kể cả khi bấm đúp hay lưu thất bại (NFR-07).
- Khóa món ngay khi bếp bắt đầu nấu (BR-08).
- Báo cho phục vụ khi từng món đã sẵn sàng (FR-15).

### 3.2 Phân tích và thiết kế hệ thống (phần phân hệ)

#### 3.2.1 Phân tích chức năng
- Use case UC-07 đến UC-10, Phụ lục A; sơ đồ use case `docs/diagrams/giang/01-use-case.png`.
- Sơ đồ hoạt động của luồng gọi món và bếp: `docs/diagrams/lam/12-activity-4-order-kitchen.png`.

#### 3.2.2 Luồng dữ liệu
- DFD mức 1, quy trình gọi món và bếp: `docs/diagrams/lam/10-dfd-1b-order-kitchen.png`.

#### 3.2.3 Thiết kế cơ sở dữ liệu
ERD: `docs/diagrams/lam/updated/w8-erd-order-kitchen.png` (README §3), khớp với Flyway `V2__order_kitchen_module.sql` và `V3__kitchen_tickets.sql`.

| Bảng | Mục đích | Ràng buộc chính |
|---|---|---|
| `menu_category` | Nhóm món (Khai vị, Súp, Món chính, Tráng miệng, Đồ uống) | `name` duy nhất |
| `menu_item` | Món ăn | Tên duy nhất trong danh mục; `price` BIGINT VND, CHECK > 0; `course`; `in_stock`; `active` (ẩn, không bao giờ xóa) |
| `customer_order` | Đơn gọi món của một bàn (ORDER là từ khóa trong SQL) | Khóa ngoại đến `dining_table` và `users`; `status` OPEN, LOCKED hoặc CLOSED; chỉ mục (`table_id`, `status`) |
| `order_line` | Một món trong đơn | `quantity` CHECK 1-50; `unit_price` sao chép từ thực đơn khi thêm dòng; `note` tối đa 200 |
| `kitchen_ticket` | Các món được gửi cùng lúc | Khóa ngoại đến `customer_order`; `sent_at`, có chỉ mục cho màn hình bếp |
| `kitchen_ticket_item` | Một món trên phiếu, kèm trạng thái ở bếp | `order_line_id` **duy nhất**, nên một dòng không bao giờ bị gửi hai lần (NFR-07); `version` cho khóa lạc quan |
| `item_status_change` | Lịch sử của từng món | `status`, `changed_at`, `reason` (cho món bị trả lại) |

#### 3.2.4 Thiết kế lớp và máy trạng thái
Hình cho báo cáo nằm trong `docs/diagrams/lam/updated/`; mã nguồn Mermaid nằm trong `docs/diagrams/lam/README.md`.
- Sơ đồ lớp: `w8-class-order-menu.png` (README §1) và `w8-class-kitchen-tickets.png` (README §2).
- Máy trạng thái: món trên phiếu `08-state-3-ticket-item.png`; đơn gọi món `08-state-4-order.png`.
- Sơ đồ tuần tự: gửi bếp, `w8-sequence-send-to-kitchen.png` (README §4); màn hình bếp, `w8-sequence-kitchen-display.png` (README §5). Nhận đơn, `w8-sequence-take-order.png` (README §6).

Quyết định thiết kế:
- **BR-08 khóa từng món.** Khóa nằm trên món của phiếu ứng với từng dòng đơn, nên vẫn thêm được món mới. Trạng thái `LOCKED` ở mức đơn được giữ cho phân hệ Thanh toán: nó nghĩa là "đã tạo hóa đơn" (BR-09).
- **Món trên phiếu đọc số lượng và ghi chú từ dòng đơn.** Dòng đã gửi không thể thay đổi, nên không có bản sao thứ hai có thể lệch nhau.
- **Mọi kiểm tra chạy trước khi thay đổi bất cứ thứ gì.** Một lần gửi bị từ chối giữ nguyên đơn như cũ.

```java
public KitchenTicket sendToKitchen(List<OrderLine> selection, LocalDateTime at) {
    requireOpen();
    if (selection.isEmpty()) throw new BusinessRuleException("There is nothing new to send");
    for (OrderLine line : selection) {
        requireOwnLine(line);
        if (line.isSent()) throw new BusinessRuleException("\"" + line.getMenuItem().getName() + "\" was already sent");
        if (!line.getMenuItem().isOrderable()) throw new BusinessRuleException("\"" + line.getMenuItem().getName() + "\" ran out; remove it before sending");
    }
    Course latestSent = selection.stream().map(l -> l.getMenuItem().getCourse()).max(Comparator.naturalOrder()).orElseThrow();
    unsentLines().stream()
            .filter(line -> !selection.contains(line))
            .map(line -> line.getMenuItem().getCourse())
            .filter(course -> course.compareTo(latestSent) < 0)
            .min(Comparator.naturalOrder())
            .ifPresent(earlier -> { throw new BusinessRuleException("Send the " + courseLabel(earlier)
                    + " dishes before the " + courseLabel(latestSent) + " dishes (BR-06)"); });
    KitchenTicket ticket = new KitchenTicket(this, selection, at);
    tickets.add(ticket);
    return ticket;
}
```
*Đoạn mã 3.3. BR-06, BR-07 và NFR-07 trong `CustomerOrder`.*
Enum `Course` được khai báo theo thứ tự Starter, Main, Dessert, nên so sánh hai lượt món là so sánh vị trí của chúng trong bữa ăn.
Nếu còn món Khai vị chưa gửi mà lại gửi Món chính, lần gửi bị từ chối và không có gì được lưu.

```java
public void changeStatus(ItemStatus target, String reason, LocalDateTime at) {
    boolean allowed = switch (status) {
        case NEW -> target == ItemStatus.IN_PROGRESS || target == ItemStatus.CANCELLED;
        case IN_PROGRESS -> target == ItemStatus.READY;
        case READY -> target == ItemStatus.IN_PROGRESS;
        case CANCELLED -> false;
    };
    ...
}
```
*Đoạn mã 3.4. Máy trạng thái của món trên phiếu (BR-08) trong `KitchenTicketItem`.*
Chỉ từ Mới mới chuyển được sang Đã hủy, nên món Đang làm không bao giờ bị hủy.
Chuyển ngược từ Sẵn sàng cần nêu lý do, và lý do được lưu trong `item_status_change`.

#### 3.2.5 Thiết kế giao diện
- Wireframe: `13-wireframe-1-order-screen.png`, `13-wireframe-2-kitchen-display.png`.
- Các màn hình, trong `docs/screenshots/lam/`:
  - Màn hình gọi món (Phục vụ/Lễ tân): một bên là thực đơn theo danh mục, bên kia là đơn với trạng thái ở bếp của từng món. `01-order-screen.jpg`.
  - Màn hình bếp (Nhân viên bếp): các phiếu đang mở, cũ nhất trước, kèm số bàn và thời gian chờ; mỗi món một nút cho bước tiếp theo. Khung món hết hàng (UC-10) nằm bên dưới các phiếu. `02-kitchen-display.jpg`.
  - Thực đơn (Quản lý). `03-menu-manager.jpg`.

### 3.3 Phát triển và hiện thực hệ thống (phần phân hệ)

#### 3.3.1 Công nghệ và công cụ
Giống bảng của Giang. Phân hệ của Lâm không thêm thư viện nào.

#### 3.3.2 Các chức năng đã hiện thực

| Chức năng (FR / UC) | Đầu vào | Đầu ra | Endpoint | Màn hình |
|---|---|---|---|---|
| Quản lý thực đơn (FR-16, UC-07) | tên, danh mục, lượt món, giá 1-10.000.000 VND | Lưu món; tên đã có trong danh mục bị từ chối. Món chỉ được ẩn, không bao giờ xóa | `GET/POST /api/menu/items`, `PUT /api/menu/items/{id}`, `/deactivate`, `/activate`, `GET/POST /api/menu/categories` | Thực đơn |
| Hết hàng / có hàng trở lại (FR-17, UC-10, BR-07) | mã món | Món biến mất khỏi thực đơn ở màn hình gọi món trong 3 giây | `POST /api/menu/items/{id}/out-of-stock`, `/restock` | Màn hình bếp, khung món hết hàng |
| Mở đơn (FR-11, UC-08) | mã bàn | Đơn OPEN. Từ chối nếu bàn không Có khách hoặc đã có đơn đang mở | `POST /api/orders`, `GET /api/orders?tableId=` | Màn hình gọi món |
| Sửa dòng món (FR-12) | món, số lượng 1-50, ghi chú tối đa 200 | Thêm dòng với giá sao chép từ thực đơn; đổi số lượng và ghi chú; xóa dòng | `POST /api/orders/{id}/lines`, `PUT` và `DELETE /api/orders/{id}/lines/{lineId}` | Màn hình gọi món |
| Gửi bếp (FR-13, FR-14, BR-06, NFR-07) | mã đơn; mọi món chưa gửi, hoặc chỉ lượt món sớm nhất chưa gửi | Một phiếu theo thứ tự lượt món; từ chối toàn bộ nếu có món đã hết hoặc còn lượt món trước chưa gửi | `POST /api/orders/{id}/send` | Màn hình gọi món |
| Chuyển đơn sang bàn khác (FR-19) | mã đơn, bàn mới, số khách | Đơn sang bàn mới, bàn mới thành Có khách; bàn cũ cần dọn. Từ chối nếu bàn mới không trống, quá nhỏ hoặc sắp có khách đặt (BR-01 đến BR-03), hoặc đơn đã bị khóa | `POST /api/orders/{id}/move` | Màn hình gọi món |
| Hủy món đã gửi (FR-18, BR-08) | mã đơn, mã dòng | Hủy khi còn Mới và trừ khỏi tổng; từ chối khi đã Đang làm | `DELETE /api/orders/{id}/lines/{lineId}` | Màn hình gọi món |
| Màn hình bếp (FR-14, FR-15, UC-09) | Không (hỏi lại mỗi 2 giây) | Các phiếu còn món Mới hoặc Đang làm, cũ nhất trước, món theo lượt món | `GET /api/kitchen/tickets` | Màn hình bếp |
| Đổi trạng thái món (FR-15, UC-09) | mã món; lý do khi trả lại | Mới → Đang làm → Sẵn sàng; Sẵn sàng → Đang làm kèm lý do. Phục vụ thấy trạng thái | `POST /api/kitchen/items/{id}/start`, `/ready`, `/send-back` | Màn hình bếp, màn hình gọi món |
| Phiếu đã xong (UC-09 luồng phụ 3a) | Không (hỏi lại mỗi 2 giây) | Các phiếu đã xong mọi món trong khi đơn vẫn mở, nên vẫn trả lại món được | `GET /api/kitchen/tickets/finished` | Màn hình bếp |
| Dữ liệu mẫu (Tuần 8) | Không | 30 món và 12 đơn đã xong trong tuần trước, tạo qua các phương thức miền và được lập hóa đơn bởi `BillingSeeder` của Khoa | `MenuSeeder`, `OrderSeeder` | Không |

**Ghi chú hiện thực**
- **Bấm đúp nút Gửi (NFR-07).**
  - `OrderService.sendToKitchen` khóa dòng của đơn trước (`findByIdForUpdate`, `SELECT … FOR UPDATE`).
  - Yêu cầu thứ hai phải chờ, sau đó thấy không còn gì để gửi và nhận lỗi 409 "There is nothing new to send".
  - Khóa duy nhất trên `kitchen_ticket_item.order_line_id` chặn bản trùng kể cả khi mã có lỗi.
- **Đầu bếp và phục vụ thao tác cùng lúc (BR-08).** Phục vụ hủy một món và đầu bếp bắt đầu nấu món đó đều lưu cùng một món trên phiếu. Cột `version` chỉ cho lần lưu đầu tiên thành công. Người thứ hai nhận lỗi 409 "Someone else changed this dish a moment ago".
- **Liên kết với các phân hệ khác.**
  - Mở đơn kiểm tra trạng thái bàn của Giang (Có khách). Phân hệ gọi món không bao giờ tự đổi trạng thái bàn.
  - `Bill.generate` của Khoa từ chối lập hóa đơn khi còn món Mới hoặc Đang làm (BR-09), sau đó gọi `lock()`. Hủy hóa đơn chưa trả gọi `unlock()`, thanh toán xong gọi `close()`.
- **Lỗi.** Mọi trường hợp từ chối là `BusinessRuleException` (409) hoặc lỗi theo ô nhập (400) kèm thông báo phục vụ có thể làm theo, ví dụ "Send the Starter dishes before the Main dishes (BR-06)".

#### 3.3.3 Chức năng chưa hiện thực hoặc chưa hoàn chỉnh

| Mục | Tình trạng | Lý do |
|---|---|---|
| FR-20 Các đơn của một bàn trong ca | Chưa làm | Yêu cầu Could; màn hình gọi món chỉ hiển thị đơn hiện tại |
| NFR-02 và NFR-03 cập nhật trực tiếp | Bằng polling | Các trang hỏi lại mỗi 2 giây (bếp) và 3 giây (thực đơn). WebSocket sẽ bỏ được các yêu cầu polling |
| NFR-05: đơn 5 món trong 60 giây | Đếm thao tác, chưa đo giờ | 6 cú bấm trên màn hình gọi món: mỗi món một cú và một cú cho Gửi. Chưa đo bằng đồng hồ bấm giờ hay với người dùng thật |
| Lý do "trả lại món" | Hộp nhập đơn giản | Dùng hộp prompt của trình duyệt thay vì biểu mẫu |
| Đặt món trước cùng lượt đặt bàn (phỏng vấn F2) | Ngoài phạm vi | Hướng phát triển |

#### 3.3.4 Sản phẩm khác
- Flyway migration `V2` và `V3`.
- Wireframe, sơ đồ triển khai và các sơ đồ UML của phân hệ trong `docs/diagrams/lam/`, với các sơ đồ đã cập nhật theo mã nguồn trong `updated/`.
- Test case `tests/test-cases-order-kitchen.md`.

### 3.4 Kiểm thử và đánh giá (phần phân hệ)

#### 3.4.1 Quy trình kiểm thử
- **Phương pháp.**
  - Kiểm thử đơn vị cho các quy tắc miền không dùng Spring: `CustomerOrderTest`, `MenuItemTest`, `KitchenTicketTest`.
  - Kiểm thử API bằng MockMvc trên H2 với dữ liệu mẫu thật: `MenuControllerTest`, `OrderControllerTest`, `KitchenControllerTest`.
  - Kiểm thử lưu trữ cho phần ánh xạ đơn gọi món: `OrderPersistenceTest`.
  - Kiểm thử rằng dữ liệu mẫu tuân theo các quy tắc: `OrderSeederTest`.
  - Các màn hình được chạy thử trên trình duyệt với hồ sơ H2: mở đơn, thêm món, đổi số lượng và ghi chú, gửi bếp, rồi bấm Start và Ready trên màn hình bếp.
- **Test case.** TC-MENU-01 đến 09, TC-ORD-01 đến 18 và TC-KIT-01 đến 14 (41 test case) trong `tests/test-cases-order-kitchen.md`, Phụ lục D.
- **Kết quả** ngày 24/11/2026:
  - `mvn test`: 182 kiểm thử tự động, 0 lỗi. 71 trong số đó thuộc Gọi món & Bếp. Chạy trên `main` khi cả ba phân hệ và giao diện mới đã được merge (Thanh toán ở PR #23, giao diện ở PR #26).
  - Cả 41 test case đã ghi đều đạt.

#### 3.4.2 Đánh giá hệ thống
**Điểm mạnh**
- Một món không bao giờ bị gửi hai lần. Có hai cơ chế ngăn điều này: khóa dòng và khóa duy nhất (TC-KIT-02).
- Lần gửi bị từ chối không thay đổi gì (TC-KIT-04).
- BR-08 khóa từng món, không khóa cả đơn, nên khách vẫn gọi thêm được (TC-KIT-08, TC-KIT-12).
- Mọi lần đổi trạng thái của món đều được lưu, kể cả lý do khi món bị trả lại.
- Dữ liệu mẫu được tạo qua chính các phương thức miền, nên không thể vi phạm quy tắc.

**Hạn chế**
- Polling gửi một yêu cầu mỗi 2 hoặc 3 giây từ mỗi màn hình đang mở.
- Các màn hình React chưa có kiểm thử tự động; chúng được kiểm tra thủ công.
- NFR-02 và NFR-03 có được nhờ chu kỳ polling nhưng chưa được đo giờ. NFR-05 chưa được đo.
- Màn hình bếp chưa có âm thanh hay nhấp nháy khi có phiếu mới.

#### 3.4.3 So sánh lý thuyết và thực tế
- **Từ lý thuyết đến thực tế.**
  - Sơ đồ trạng thái của món trên phiếu trở thành một câu lệnh `switch`; bước chuyển không có trong sơ đồ thì không thể xảy ra.
  - "Mất cập nhật" xuất hiện dưới hai dạng. Bấm đúp nút Gửi được giải quyết bằng khóa bi quan. Đầu bếp và phục vụ cùng thao tác được giải quyết bằng khóa lạc quan, vì xung đột ở đây hiếm.
  - Để đảm bảo tính nguyên tử, phương thức miền kiểm tra mọi thứ trước khi thay đổi bất cứ điều gì. Trong trường hợp bị từ chối thông thường, giao dịch không có gì phải hoàn tác.
- **Khác biệt.**
  - Mã ở Tuần 5 khóa cả đơn khi bếp bắt đầu nấu. Đọc lại BR-08 và FR-18 ở Tuần 7 cho thấy cách này sẽ không cho khách gọi thêm. Khóa được chuyển xuống món trên phiếu, và trạng thái `LOCKED` của đơn được giữ cho hóa đơn (BR-09).
  - Buổi phỏng vấn cho thấy khách hiếm khi đổi món sau khi bếp đã nấu (F10). Quy tắc vẫn được giữ vì nó bảo vệ bếp trong những trường hợp có xảy ra.

## Các dòng Chương 4 (để Giang ghép)

**4.2 Kiến thức và kỹ năng đạt được**

| Thành viên | Kỹ năng kỹ thuật | Kỹ năng chuyên môn | Kinh nghiệm thực tế |
|---|---|---|---|
| Tăng Thoại Lâm | Ánh xạ đơn gọi món, các dòng món, phiếu bếp và lịch sử trạng thái bằng JPA; khóa bi quan trên đơn và khóa lạc quan (`version`) trên từng món của phiếu; chuyển sơ đồ trạng thái món trên phiếu thành một phương thức `changeStatus`; kiểm thử API bằng MockMvc; màn hình React hỏi lại server mỗi 2-3 giây | Làm việc trên nhánh tính năng với pull request được nhóm trưởng review; ghi kết quả kiểm thử thật kèm ngày; giữ báo cáo, sơ đồ và ảnh màn hình khớp với mã nguồn sau mỗi lần merge | Buổi phỏng vấn cho thấy nhà hàng đã dùng ứng dụng gọi món, nhưng ứng dụng không liên kết với bàn hay hóa đơn, và món hết hàng được báo bằng lời. Điều đó khiến nút báo hết hàng và mối liên kết giữa bàn Có khách, đơn gọi món và hóa đơn trở thành cốt lõi của phân hệ |

## Các dòng phụ lục của Lâm

### Phụ lục B: Khai báo sử dụng công cụ AI

| Công cụ | Dùng để làm gì | Phần công việc | Cách kiểm chứng kết quả |
|---|---|---|---|
| Claude (Anthropic), qua Claude Code | Tạo mã, màn hình, kiểm thử và test case phân hệ Gọi món & Bếp; dịch bản nháp yêu cầu của Lâm; soạn Chương 2 và phần phân hệ trong Chương 3 | `src/backend`, `src/frontend`, `tests/test-cases-order-kitchen.md`, `docs/diagrams/lam`, `docs/report` | Lâm chạy kiểm thử tự động, chạy thử các màn hình trên trình duyệt, sửa mã gọi món và bếp trong các pull request sau (#21, #22) và đối chiếu Chương 2 với biên bản phỏng vấn |

### Phụ lục C: Bảng đóng góp của từng thành viên

| Thành viên | Trách nhiệm chính | Sản phẩm chính | Số commit | Tỉ lệ đóng góp (%) | Chữ ký |
|---|---|---|---|---|---|
| Tăng Thoại Lâm | Phân hệ Gọi món & Bếp: thực đơn, gọi món, thứ tự lượt món, màn hình bếp, hết hàng; Chương 2 của báo cáo | Bản nháp yêu cầu; `docs/diagrams/lam/`; Flyway V2 và V3; backend và frontend Gọi món & Bếp; 71 kiểm thử tự động và 41 test case; dữ liệu mẫu | 28 | 35 | |
