# Báo cáo cuối kỳ — Phần của Kiều Bảo Giang (bản tiếng Việt)

Bản dịch tiếng Việt của `giang.md`, dùng cho `build_report.py vi`. Nội dung giống bản tiếng Anh; khi sửa một bản thì sửa cả bản kia.

## Chương 1: Giới thiệu

### 1.1 Bối cảnh và lý do chọn đề tài

**Lý do chọn đề tài.**
Các nhà hàng vừa và nhỏ thường quản lý khu vực phục vụ bằng sổ tay, ứng dụng nhắn tin và trao đổi miệng.
Ngày 18/10/2026, nhóm đã phỏng vấn nhân viên của một nhà hàng bít tết và mì Ý tại Thành phố Hồ Chí Minh.
Trong báo cáo này, nhà hàng được gọi là "Ember & Oak" để giữ ẩn danh.
Buổi phỏng vấn cho thấy cách làm này gây ra những vấn đề thực tế sau:
- Khách đặt bàn qua tin nhắn mạng xã hội.
- Khách đặt bàn nhưng không đến (no-show) là vấn đề thường gặp nhất.
- Bàn của khách đến trễ được giữ tối đa 45 phút.
- Nhà hàng đã có ứng dụng gọi món, nhưng ứng dụng này không liên kết với đặt bàn hay thanh toán.
- Món hết hàng được báo cho nhau bằng lời.
- Tiền mặt và chuyển khoản được đối chiếu thủ công vào cuối ca.

**Tầm quan trọng và ý nghĩa thực tiễn.**
Tse và Poon [12] cho thấy khách không đến, khách hủy và khách vãng lai khiến một nhà hàng đông khách khó quyết định nên nhận bao nhiêu lượt đặt bàn.
Một hệ thống quản lý đặt bàn, khách vãng lai và trạng thái bàn ở cùng một nơi là bước đầu tiên để giải quyết vấn đề này.
Cũng hệ thống đó có thể chuyển một bàn đã có khách sang gọi món và thanh toán, nên nhân viên không còn phải chép thông tin giữa các công cụ.

### 1.2 Mục tiêu của dự án

**Mục tiêu tổng quát.**
Xây dựng một ứng dụng web với ba vai trò nhân viên, giúp nhà hàng vừa và nhỏ quản lý trong cùng một hệ thống thống nhất:
- đặt bàn;
- khách vãng lai;
- gọi món và chế biến tại bếp;
- thanh toán.

**Mục tiêu cụ thể, tình trạng ngày 27/11/2026:**

| # | Mục tiêu (Proposal §1.2) | Tình trạng |
|---|---|---|
| 1 | 15 use case hoàn chỉnh cho ba vai trò | UC-01 đến UC-06 (Bàn & Đặt bàn) hoàn thành. UC-07 đến UC-10 (Gọi món & Bếp) hoàn thành. UC-11 đến UC-15 (Thanh toán & Báo cáo) hoàn thành. |
| 2 | 13 quy tắc nghiệp vụ ở tầng service, mỗi quy tắc có ít nhất một kiểm thử tự động | BR-01 đến BR-05 hoàn thành, đều có kiểm thử. BR-06 đến BR-08 (Lâm) hoàn thành, có kiểm thử đơn vị và kiểm thử API (`KitchenTicketTest`, `OrderControllerTest`, `KitchenControllerTest`). BR-09 đến BR-13 (Khoa) hoàn thành, có kiểm thử đơn vị và kiểm thử API (`BillTest`, `BillControllerTest`, `ShiftControllerTest`). |
| 3 | Không có hai lượt đặt bàn trùng giờ, kể cả khi hai lượt đặt cùng một lúc | Hoàn thành: khóa dòng (row lock) và kiểm thử đồng thời tự động (TC-RES-27) |
| 4 | Ít nhất 30 test case có kết quả mong đợi và kết quả thực tế | Tổng cộng 120: 34 cho Bàn & Đặt bàn (TC-RES-01 đến TC-RES-34), 41 cho Gọi món & Bếp (TC-MENU, TC-ORD, TC-KIT) và 45 cho Thanh toán & Báo cáo. |
| 5 | Yêu cầu phi chức năng đo được, ví dụ tìm kiếm dưới 2 giây | Đạt NFR-01: với 50 bàn và 500 lượt đặt trong một tuần, lần tìm chậm nhất mất 16 ms (TC-RES-30, cơ sở dữ liệu kiểm thử H2). NFR-02 và NFR-03 có được nhờ cập nhật định kỳ mỗi 2 giây (màn hình bếp) và 3 giây (thực đơn ở màn hình gọi món); không đo bằng đồng hồ bấm giờ. Đạt NFR-04: hóa đơn 50 dòng mất 2 ms và báo cáo ca 300 hóa đơn mất 25 ms (TC-BIL-34, TC-SHF-10, cơ sở dữ liệu kiểm thử H2). |

### 1.3 Phạm vi nghiên cứu

**Phạm vi nghiên cứu.**
Quy trình đặt bàn, xếp chỗ, gọi món và thanh toán của một nhà hàng.
Nhóm tìm hiểu quy trình qua buổi phỏng vấn và các công trình đã công bố về đặt bàn nhà hàng.

**Phạm vi hệ thống: bao gồm.**
- Đăng nhập với ba vai trò: Phục vụ/Lễ tân, Nhân viên bếp, Quản lý.
- Trang web cho khách, không cần đăng nhập: thực đơn và đặt bàn trực tuyến (bổ sung sau Proposal).
- Bàn và khu vực.
- Đặt bàn: tìm bàn, tạo, sửa, hủy, nhận bàn (check-in), tự động đánh dấu khách không đến.
- Danh sách chờ cho khách vãng lai.
- Vòng đời của bàn và sơ đồ tổng quan khu phục vụ.
- Thực đơn, gọi món, thứ tự ra món, màn hình bếp, món hết hàng, chuyển món sang bàn khác.
- Hóa đơn, chia hóa đơn (chia đều và theo món), thanh toán (tiền mặt, thẻ, chuyển khoản, ví điện tử), ca làm việc và báo cáo ca có đối chiếu tiền mặt.

**Phạm vi hệ thống: không bao gồm.**
- Khách gọi món trực tuyến. Đặt bàn trực tuyến được bổ sung sau Proposal: trang web cho khách đặt bàn mà không cần đăng nhập (xem 3.3.2).
- Đặt cọc và đặt món trước khi đặt bàn. Nhà hàng có đề xuất (phỏng vấn F2); đây là hướng phát triển.
- SMS và email.
- Cổng thanh toán thật.
- Quản lý tài khoản nhân viên trên giao diện.
- Quản lý kho nguyên liệu.
- Nhiều chi nhánh.

**Phạm vi tổ chức và thời gian.**
- Ba sinh viên.
- Mười tuần, từ 14/9 đến 22/11/2026.
- Mỗi thành viên phụ trách một phân hệ:
  - Kiều Bảo Giang: Bàn & Đặt bàn, cùng với khung dự án chung, đăng nhập và vai trò nhóm trưởng.
  - Tăng Thoại Lâm: Gọi món & Bếp.
  - Nguyễn Nam Khoa: Thanh toán & Báo cáo.

### 1.4 Phương pháp nghiên cứu

- **Thu thập dữ liệu: nghiên cứu tài liệu.** Tài liệu môn học [1], [2], hướng dẫn về yêu cầu phần mềm [4], [7] và nghiên cứu về khách không đến [12].
- **Quan sát và khảo sát thực tế.**
  - Phỏng vấn bán cấu trúc tại nhà hàng ngày 18/10/2026, có mặt cả ba thành viên.
  - Hai nhân viên trả lời: một thu ngân và một phục vụ.
  - Biên bản được lưu tại `docs/interview/restaurant-interview-notes.md`.
- **Phân tích dữ liệu.**
  - Mỗi phát hiện từ buổi phỏng vấn (F1–F12) được gắn với yêu cầu mà nó ảnh hưởng.
  - Từ đó có năm thay đổi yêu cầu (CR-01 đến CR-05). Các thay đổi chính:
    - thời gian dùng bữa 90 → 180 phút;
    - giới hạn khách không đến 15 → 45 phút.
- **Phương pháp so sánh và thống kê.** Các mốc thời gian của BR-01 và BR-05 được đối chiếu với thời gian nhân viên cung cấp:
  - mỗi bàn dùng 3 giờ;
  - dọn bàn 15 phút;
  - khách đến sớm 30 phút hoặc trễ 45 phút.
- **Phỏng vấn và khảo sát.** Xem ở trên. Nhóm không làm khảo sát.

### 1.5 Cấu trúc báo cáo

- **Chương 1** giới thiệu bài toán, mục tiêu và phạm vi.
- **Chương 2** trình bày:
  - các công nghệ sử dụng: kiến trúc phân tầng, Spring Boot, JPA, React;
  - quy trình hiện tại của nhà hàng;
  - phân tích yêu cầu đầy đủ, đã cập nhật sau buổi phỏng vấn.
- **Chương 3** mô tả phân tích, thiết kế, hiện thực và kiểm thử của từng phân hệ.
- **Chương 4** so sánh kết quả với Proposal và nêu những gì mỗi thành viên học được.
- **Phụ lục** gồm:
  - đặc tả use case (Phụ lục A);
  - khai báo sử dụng AI (Phụ lục B);
  - bảng đóng góp (Phụ lục C);
  - nhật ký test case đầy đủ (Phụ lục D).

## Chương 3 — Các mục của phân hệ Bàn & Đặt bàn

### 3.1 Mô tả bài toán và mục tiêu hệ thống (phần phân hệ)

**Phát biểu bài toán.**
Lễ tân phải trả lời ba câu hỏi ngay tại cửa:
- Nhóm khách này có thể ngồi bàn nào ngay bây giờ?
- Những bàn nào phải giữ cho khách đã đặt?
- Ai là người chờ tiếp theo?

Hiện nay câu trả lời đến từ lịch sử tin nhắn và trí nhớ. Có bàn bị hứa cho hai nhóm, hoặc bị giao cho khách khác ngay trước giờ khách đặt đến.

**Mục tiêu của phân hệ.**
- Không bao giờ để một bàn được đặt hai lần trong khoảng thời gian chồng nhau (BR-01, NFR-06).
- Chỉ xếp nhóm khách vào bàn đủ chỗ (BR-02).
- Không bao giờ xếp khách vào bàn chưa dọn (BR-03).
- Xếp khách vãng lai công bằng (BR-04).
- Mọi bàn đi theo một vòng đời duy nhất mà các phân hệ khác cũng dùng (BR-05).

### 3.2 Phân tích và thiết kế hệ thống (phần phân hệ)

#### 3.2.1 Phân tích chức năng
- Use case UC-01 đến UC-06; xem sơ đồ use case, `docs/diagrams/giang/01-use-case.png`.
- Phân rã chức năng: `docs/diagrams/giang/09-functional-decomposition.png`.

#### 3.2.2 Luồng dữ liệu
- DFD mức 0: `10-dfd-0-context.png`.
- DFD mức 1, quy trình đặt bàn: `10-dfd-1a-reservation.png`.

#### 3.2.3 Thiết kế cơ sở dữ liệu
ERD: `docs/diagrams/giang/README.md` §2 (Mermaid, khớp với Flyway `V1__table_reservation_module.sql`).

| Bảng | Mục đích | Ràng buộc chính |
|---|---|---|
| `users` | Tài khoản nhân viên | `username` duy nhất; `password_hash` dùng BCrypt; `role` là một trong ba giá trị |
| `table_section` | Các khu vực (Hearth Hall, Duo Lounge, Cellar Room, Sky Terrace) | `name` duy nhất |
| `dining_table` | Bàn | `number` duy nhất; `capacity` CHECK 1–20; khóa ngoại đến khu vực; `status`; `active` |
| `reservation` | Lượt đặt bàn | `code` duy nhất; khóa ngoại đến bàn; chỉ mục (`table_id`, `start_time`) cho việc kiểm tra trùng giờ |
| `waitlist_entry` | Hàng chờ khách vãng lai | `seated_table_id` là NULL khi nhóm khách còn đang chờ |
| `reservation_settings` | Một dòng: số phút dùng bữa (180) và số phút dọn bàn (15) | Quản lý chỉnh sửa (30–300 và 0–60) |

#### 3.2.4 Thiết kế lớp và máy trạng thái
Hình cho báo cáo nằm trong `docs/diagrams/giang/updated/`; mã nguồn Mermaid nằm trong `docs/diagrams/giang/README.md`.
- Sơ đồ lớp: `w8-class-table-reservation.png` (README §1).
- ERD: `w8-erd-table-reservation.png` (README §2).
- Máy trạng thái của bàn: `w8-state-table.png` (README §3).
- Máy trạng thái của lượt đặt bàn: `w8-state-reservation.png` (README §4).
- Sơ đồ tuần tự: nhận bàn, `w8-sequence-check-in.png` (README §5); hai lượt đặt cùng lúc, `w8-sequence-concurrent-booking.png` (README §6).

Quyết định thiết kế: các quy tắc nằm trong lớp miền (domain), không nằm trong controller hay React (Guidelines §4.1 mục 4).
- `DiningTable.changeStatus` là cách duy nhất để đổi trạng thái bàn. Phương thức này từ chối mọi bước chuyển mà vòng đời không cho phép, nên phân hệ Gọi món và Thanh toán không thể bỏ qua BR-05.
- `Reservation.overlap` chứa công thức BR-01. Cả service và bước kiểm tra khách vãng lai đều gọi phương thức này.

```java
public void changeStatus(TableStatus target) {
    if (target == status) return;
    boolean allowed = switch (status) {
        case AVAILABLE -> target == TableStatus.RESERVED || target == TableStatus.OCCUPIED;
        case RESERVED -> target == TableStatus.OCCUPIED || target == TableStatus.AVAILABLE;
        case OCCUPIED -> target == TableStatus.NEEDS_CLEANING;
        case NEEDS_CLEANING -> target == TableStatus.AVAILABLE;
    };
    if (!allowed) throw new BusinessRuleException("Table " + number + " cannot go from " + label(status) + " to " + label(target));
    status = target;
}
```
*Đoạn mã 3.1. BR-05 trong `DiningTable`.*
Câu lệnh `switch` trên enum buộc trình biên dịch kiểm tra rằng mọi trạng thái đều được xử lý.
Thông báo lỗi cho nhân viên biết chính xác bước nào bị từ chối.

```java
public static boolean overlap(LocalDateTime startA, LocalDateTime startB, ReservationSettings s) {
    long block = s.getDiningMinutes() + s.getTurnMinutes();
    return startA.isBefore(startB.plusMinutes(block)) && startB.isBefore(startA.plusMinutes(block));
}
```
*Đoạn mã 3.2. BR-01 trong `Reservation`.*
Với giá trị mặc định, mỗi lượt đặt chiếm bàn 180 + 15 = 195 phút.
Lượt đặt lúc 19:00 giữ bàn đến 22:15. Lượt đặt lúc 22:15 được chấp nhận, còn 22:14 bị từ chối (TC-RES-17).

#### 3.2.5 Thiết kế giao diện
- Luồng điều hướng: `11-screen-navigation.png`.
- Các màn hình:
  - Đăng nhập, với các tài khoản demo ghi chữ nhỏ: `docs/screenshots/app/02-login.jpg`.
  - Tổng quan khu phục vụ: `docs/screenshots/app/01-floor.jpg`.
  - Đặt bàn, với biểu mẫu đặt bàn ba bước: `docs/screenshots/app/03-reservations.jpg`.
  - Nhận bàn bị từ chối vì đến quá sớm: `docs/screenshots/app/07-check-in-too-early.jpg`.
  - Danh sách chờ: `docs/screenshots/app/08-waiting-list.jpg`.
  - Trang web cho khách với đặt bàn trực tuyến: `docs/screenshots/app/06-guest-site.jpg`.
  - Hóa đơn ở giao diện tối và bằng tiếng Việt: `docs/screenshots/app/05-bill-dark-vi.jpg`.
  - Quản lý bàn (Quản lý).
- Bản xem trước thiết kế giao diện cho cả ba phân hệ là `docs/design/ui-demo.html`.

### 3.3 Phát triển và hiện thực hệ thống (phần phân hệ)

#### 3.3.1 Công nghệ và công cụ
| Tầng | Công nghệ |
|---|---|
| Ngôn ngữ | Java 17 (backend), JavaScript (frontend) |
| Backend | Spring Boot 3.5: Web, Data JPA, Security, Validation, Scheduling |
| Cơ sở dữ liệu | MySQL 8, thay đổi lược đồ bằng Flyway migration; H2 cho kiểm thử và hồ sơ demo |
| Frontend | React 18, React Router 6, Vite 6 |
| Kiểm thử | JUnit 5, Spring MockMvc, Spring Security Test |
| Công cụ | IntelliJ IDEA / VS Code, Git và GitHub (nhánh, pull request, tag), Maven, npm |

Phân hệ của Lâm và Khoa không thêm thư viện nào. Mỗi thư viện, phiên bản và giấy phép được liệt kê ở Chương 2, §2.1.7 (Guidelines §10).

#### 3.3.2 Các chức năng đã hiện thực

| Chức năng (FR / UC) | Đầu vào | Đầu ra | Endpoint | Ảnh màn hình |
|---|---|---|---|---|
| Đăng nhập / đăng xuất (FR-01, UC-01) | tên đăng nhập, mật khẩu | Cookie phiên và vai trò; menu chỉ hiện các màn hình của vai trò đó | `POST /api/auth/login`, `/logout`, `GET /api/auth/me` | — |
| Quản lý bàn và khu vực (FR-02, UC-02) | số bàn, sức chứa 1–20, khu vực | Lưu bàn. Từ chối ngừng sử dụng bàn khi còn lượt đặt trong tương lai | `POST/PUT /api/tables`, `/deactivate`, `/activate`, `POST /api/sections` | Trang Bàn |
| Tìm bàn trống (FR-03) | ngày, giờ bắt đầu, số khách | Các bàn đang hoạt động, đủ chỗ và không có lượt đặt trong ±195 phút | `GET /api/reservations/available-tables` | Biểu mẫu đặt bàn, bước 1 |
| Tạo lượt đặt bàn (FR-04, UC-03) | bàn, tên khách, số điện thoại, số khách, ngày, giờ, ghi chú | Lượt đặt Đã xác nhận với mã như `RVUKT2TB`. Lỗi 409 khi trùng giờ | `POST /api/reservations` | Biểu mẫu đặt bàn, bước 3 |
| Sửa / hủy (FR-05, UC-04) | mã lượt đặt, thông tin mới | Đã cập nhật hoặc Đã hủy. Bàn cũ được trả lại | `PUT /api/reservations/{id}`, `POST …/cancel` | Trang Đặt bàn |
| Nhận bàn (FR-05, UC-04) | mã lượt đặt | Lượt đặt Đã nhận bàn, bàn Có khách. Từ chối nếu sớm hơn 30 phút trước giờ đặt, hoặc từ 45 phút trở lên sau giờ đặt | `POST /api/reservations/{id}/check-in` | Trang Đặt bàn |
| Khách không đến và Đã đặt trước (FR-06, BR-05) | — (chạy mỗi 60 giây) | Không đến sau 45 phút kể từ giờ đặt, và bàn được trả lại. Bàn chuyển sang Đã đặt trước 30 phút trước giờ đặt | `TableStatusScheduler` → `ReservationService.refreshStatuses` | Tổng quan khu phục vụ |
| Danh sách chờ (FR-07, UC-05) | tên nhóm, số điện thoại, số khách | Hàng chờ có thứ tự theo giờ đến | `GET/POST/DELETE /api/waitlist` | Danh sách chờ |
| Xếp nhóm chờ tiếp theo (FR-08, BR-04) | mã bàn | Nhóm đến sớm nhất mà bàn đủ chỗ được xếp; bàn Có khách | `POST /api/waitlist/seat-next?tableId=` | Tổng quan khu phục vụ |
| Xếp khách vãng lai (BR-01, BR-02) | mã bàn, số khách | Bàn Có khách. Từ chối nếu có lượt đặt bắt đầu trong vòng 195 phút | `POST /api/tables/{id}/seat-walk-in` | Tổng quan khu phục vụ |
| Khách rời đi / đánh dấu đã dọn (FR-09, UC-06, BR-03) | mã bàn | Cần dọn (từ chối khi bàn còn đơn gọi món đang mở), sau đó Trống | `POST /api/tables/{id}/needs-cleaning`, `/clean` | Tổng quan khu phục vụ |
| Tổng quan khu phục vụ (FR-10) | — | Mọi bàn theo khu vực, tô màu theo trạng thái, kèm các thao tác được phép ở từng trạng thái | `GET /api/tables` | Tổng quan khu phục vụ |
| Cài đặt đặt bàn (UC-02 bước 5) | dùng bữa 30–300, dọn bàn 0–60 phút | Thời gian mới được BR-01 áp dụng ngay | `GET/PUT /api/settings/reservation` | — |
| Khách đặt bàn trực tuyến (bổ sung sau Proposal, trang web cho khách tại `/`) | tên, số điện thoại, ngày, giờ trong các khung còn trống, số khách, ghi chú | Lượt đặt Đã xác nhận trên bàn trống nhỏ nhất đủ chỗ (BR-01, BR-02); khách thấy mã đặt bàn; màn hình Đặt bàn của nhân viên hiện ngay lượt đặt với ghi chú "Online booking" | `GET /api/public/slots`, `POST /api/public/reservations`, `GET /api/public/menu` (không cần đăng nhập) | Trang web cho khách |

**Ghi chú hiện thực**

- **Hai lượt đặt cùng một lúc (NFR-06).**
  - `create`, `modify` và `checkIn` khóa dòng của bàn trước: `DiningTableRepository.findByIdForUpdate`, khóa JPA `PESSIMISTIC_WRITE`, tức là `SELECT … FOR UPDATE`.
  - Chỉ sau đó mới kiểm tra trùng giờ.
  - Yêu cầu thứ hai cho cùng bàn phải chờ đến khi yêu cầu thứ nhất commit. Khi đó nó thấy lượt đặt thứ nhất và nhận lỗi 409.
  - Nếu chờ khóa quá lâu, người dùng nhận thông báo "Someone else is changing this table right now. Please try again." và không bao giờ nhận lỗi 500.
- **Các quy tắc theo thời gian.**
  - `TableStatusScheduler` chạy mỗi 60 giây. Nó lấy thời gian từ một `Clock` được inject, nên kiểm thử có thể cố định thời gian.
  - Bộ lập lịch bị tắt trong hồ sơ test (`app.scheduling.enabled=false`), và kiểm thử gọi trực tiếp `refreshStatuses(now)`.
- **Liên kết với phân hệ Gọi món.** `markNeedsCleaning` từ chối khi bàn còn đơn gọi món ở trạng thái OPEN hoặc LOCKED (`CustomerOrder` của Lâm).
- **Lỗi.**
  - Mọi lỗi được trả về dạng `{ message, fieldErrors }`.
  - React hiển thị thông báo phía trên trang, hoặc cạnh ô nhập liệu (`docs/security-check.md` §1).

#### 3.3.3 Chức năng chưa hiện thực hoặc chưa hoàn chỉnh
| Mục | Tình trạng | Lý do |
|---|---|---|
| UC-06 bước 3: "làm nổi bật nhóm chờ vừa với bàn" | Thay thế | Mỗi ô bàn Trống có nút "Seat next waiting". Cách này đơn giản hơn và áp dụng cùng quy tắc BR-04 ở server |
| Gọi điện nhắc khách trễ sau 30 phút (phỏng vấn F5) | Chưa làm | SMS và gọi điện nằm ngoài phạm vi. Lượt đặt vẫn ở trạng thái Đã xác nhận từ phút 30 đến phút 45, nên nhân viên có thể gọi |
| Tổng quan khu phục vụ tự cập nhật | Một phần | Trang tải lại sau mỗi thao tác, nhưng không tải lại khi người dùng khác thay đổi bàn. Bước tiếp theo là polling hoặc WebSocket |
| Trạng thái Đã đặt trước ngay sau "Mark clean" | Trễ tối đa 1 phút | Do lần chạy tiếp theo của bộ lập lịch đặt (lối tắt được ghi chú trong `TableService.markClean`) |
| Đặt cọc và đặt món trước | Ngoài phạm vi | Proposal §1.3; nhà hàng có đề xuất (F2). Đặt bàn trực tuyến được làm sau (3.3.2). Hướng phát triển |
| Bàn còn hóa đơn chưa trả thì không được dọn (UC-06 luồng phụ 1a) | Hoàn thành | Hóa đơn đã tạo nhưng chưa thanh toán giữ đơn ở trạng thái LOCKED, và "Guests left" từ chối bàn có đơn LOCKED (TC-BIL-33) |

#### 3.3.4 Sản phẩm khác
- REST API: các endpoint trong bảng ở mục 3.3.2.
- Flyway migration `V1`.
- Hướng dẫn chạy trong README, với hồ sơ demo H2 không cần MySQL.
- Bản xem trước thiết kế: `docs/design/ui-demo.html`.
- Biên bản phỏng vấn và các thay đổi yêu cầu.
- Sơ đồ cập nhật: `docs/diagrams/giang/README.md` (Mermaid) và `docs/diagrams/giang/updated/` (PNG).
- Kiểm tra bảo mật: `docs/security-check.md`.

### 3.4 Kiểm thử và đánh giá (phần phân hệ)

#### 3.4.1 Quy trình kiểm thử
- **Phương pháp.**
  - Kiểm thử đơn vị tự động cho các quy tắc miền (JUnit 5, không dùng Spring).
  - Kiểm thử API bằng MockMvc trên cơ sở dữ liệu H2 trong bộ nhớ với dữ liệu mẫu thật.
  - Một kiểm thử đồng thời với hai luồng và giao dịch thật.
  - Kiểm thử đầu-cuối với đăng nhập thật:
    - toàn bộ một lượt khách trên một bàn (TC-RES-28);
    - một lượt khách đi qua các phân hệ: đặt bàn, gọi món, bếp, rồi trả bàn (TC-RES-31).
  - Một kiểm thử hiệu năng cho NFR-01 (TC-RES-30).
  - Kiểm tra thủ công trên trình duyệt cho các dòng đánh dấu "UI".
- **Test case.**
  - TC-RES-01 đến TC-RES-34, trong `tests/test-cases-table-reservation.md`, Phụ lục D.
  - Các cột: mã, mục đích, điều kiện trước, đầu vào, kết quả mong đợi, kết quả thực tế, đạt/không đạt, kiểm thử tự động.
- **Kết quả**:
  - `mvnw test` chạy 182 kiểm thử tự động: 60 cho phân hệ này, 71 cho Gọi món & Bếp và 51 cho Thanh toán & Báo cáo. 0 lỗi (27/11/2026).
  - Mọi test case đã ghi đều đạt.

#### 3.4.2 Đánh giá hệ thống
**Điểm mạnh**
- Mọi quy tắc được kiểm tra ở server và được kiểm thử tại giá trị biên, ví dụ 22:14 bị từ chối và 22:15 được chấp nhận.
- Không thể đặt trùng bàn, kể cả khi đặt đồng thời (TC-RES-27).
- Thông báo lỗi cho nhân viên biết cần làm gì, không chỉ báo điều gì sai.
- Đạt NFR-01 với khoảng cách lớn: với 50 bàn và 500 lượt đặt trong một tuần, lần tìm chậm nhất trong năm lần mất 16 ms, so với giới hạn 2.000 ms (TC-RES-30).

**Hạn chế**
- Chưa giới hạn số lần đăng nhập.
- Mật khẩu demo nằm trong README.
- Ứng dụng không tự cung cấp HTTPS.
- Tổng quan khu phục vụ không tự làm mới.
- NFR-01 được đo trên cơ sở dữ liệu kiểm thử H2 trong bộ nhớ, không phải MySQL. MySQL thêm thời gian mạng và đĩa, nhưng truy vấn dùng chỉ mục trên (`table_id`, `start_time`).
- Kiểm thử liên phân hệ đóng đơn trực tiếp, vì lúc viết kiểm thử phân hệ Thanh toán chưa được merge.

#### 3.4.3 So sánh lý thuyết và thực tế
- **Từ lý thuyết đến thực tế.**
  - Sơ đồ máy trạng thái trong thiết kế trở thành một câu lệnh `switch` trong `DiningTable`.
  - Vấn đề "lost update" học trong môn cơ sở dữ liệu trở thành một kiểm thử thật với hai luồng.
- **Khác biệt.**
  - Proposal giả định mỗi bữa ăn 90 phút và giới hạn khách không đến 15 phút. Nhà hàng thật dùng 3 giờ và 45 phút.
  - Một yêu cầu tưởng như đã chốt lại thay đổi sau một buổi phỏng vấn.
  - Thời gian dùng bữa và dọn bàn là cài đặt của Quản lý, nên CR-02 không cần sửa mã.
  - Giới hạn khách không đến là một hằng số (`Reservation.NO_SHOW_AFTER_MINUTES`), nên CR-01 chỉ cần sửa một dòng và thêm một kiểm thử.

## Chương 4: Kết luận

### 4.1 Tổng kết

**Tóm tắt công việc đã hoàn thành, so với Proposal**

| Hạng mục trong Proposal | Đã hoàn thành | Chưa hoàn thành / thay đổi |
|---|---|---|
| Đăng nhập và ba vai trò (FR-01) | Có: đăng nhập theo phiên, BCrypt, kiểm tra vai trò ở server | — |
| Bàn & Đặt bàn, FR-02 đến FR-10, UC-01 đến UC-06, BR-01 đến BR-05 | Mọi FR, cả năm quy tắc, mỗi quy tắc có kiểm thử tự động | UC-06 "làm nổi bật nhóm khách vừa bàn" được thay bằng nút "Seat next waiting" (xem 3.3.3) |
| Gọi món & Bếp, FR-11 đến FR-20, BR-06 đến BR-08 | FR-11 đến FR-19, UC-07 đến UC-10, BR-06 đến BR-08, mỗi quy tắc có kiểm thử đơn vị và kiểm thử API; màn hình gọi món và màn hình bếp; dữ liệu mẫu | FR-20 (Could) chưa làm. BR-08 khóa từng món thay vì cả đơn |
| Thanh toán & Báo cáo, FR-21 đến FR-29, BR-09 đến BR-13 | FR-21 đến FR-29, UC-11 đến UC-15, BR-09 đến BR-13, mỗi quy tắc có kiểm thử đơn vị và kiểm thử API; màn hình hóa đơn và ca làm việc; đo NFR-04; hóa đơn và ca mẫu | Thêm ví điện tử (CR-04); báo cáo ca được tính toán thay vì lưu sẵn; chưa có chức năng hoàn tiền |
| 30 test case có tài liệu | 120: 34 Bàn & Đặt bàn, 41 Gọi món & Bếp, 45 Thanh toán & Báo cáo | — |
| Tag phát hành v0.1, v0.2, v1.0 | v0.1 ngày 21/10/2026, v0.2 ngày 03/11/2026, v1.0 ngày 27/11/2026 | v0.2 và v1.0 trễ hơn kế hoạch (Tuần 7 và Tuần 9) |

**Thành tựu và kết quả**
- Một ứng dụng web chạy được: Spring Boot, React và MySQL, kèm hồ sơ demo không cần MySQL.
- Buổi phỏng vấn đã thay đổi hai quy tắc của nhóm Bàn & Đặt bàn (CR-01, CR-02) và ba quy tắc của Khoa (CR-03 đến CR-05). Mã nguồn theo đúng thời gian thực tế của nhà hàng.
- Ngăn được việc đặt trùng bàn kể cả khi hai nhân viên đặt cùng một giây. Điều này được chứng minh bằng kiểm thử tự động.
- Lâm: màn hình bếp hiện món theo thứ tự ra món trong chu kỳ cập nhật 2 giây, và một món không bao giờ bị gửi hai lần, kể cả khi bấm đúp. Khoa: các phần chia hóa đơn luôn cộng lại đủ tổng (BR-10), và thanh toán đóng hóa đơn và trả bàn trong cùng một giao dịch.

**Hạn chế và hướng phát triển**
- **Bảo mật khi đưa lên internet:** giới hạn số lần đăng nhập, dùng HTTPS, bắt buộc đổi mật khẩu thay cho mật khẩu demo trong README.
- **Cập nhật trực tiếp:** tổng quan khu phục vụ tự làm mới, dùng WebSocket thay cho polling ở màn hình bếp; có âm báo khi có phiếu bếp mới.
- **Ý tưởng từ buổi phỏng vấn:** đặt cọc cho lượt đặt trực tuyến để giảm khách không đến, và đặt món trước cùng lượt đặt bàn (F2).
- **Nhắc nhở:** nhắn SMS hoặc Zalo cho khách trễ ở phút thứ 30 (F5).
- **Gọi món:** xem các đơn của một bàn trong ca hiện tại (FR-20).
- **Thanh toán:** hoàn tiền hoặc điều chỉnh thanh toán có Quản lý duyệt; báo cáo doanh thu ngày và tháng có xuất Excel; in hoặc gửi email biên lai và hóa đơn VAT; giảm giá và phí phục vụ.

### 4.2 Kiến thức và kỹ năng đạt được

| Thành viên | Kỹ năng kỹ thuật | Kỹ năng chuyên môn | Kinh nghiệm thực tế |
|---|---|---|---|
| Kiều Bảo Giang | Thiết kế Spring Boot phân tầng; ánh xạ JPA và Flyway migration; phiên đăng nhập và vai trò với Spring Security; khóa bi quan (pessimistic locking) cho truy cập đồng thời; kiểm thử JUnit và MockMvc, kể cả kiểm thử nhiều luồng; trang React gọi REST API | Dẫn dắt nhóm ba người: chia phân hệ, review pull request, giữ nhánh `main` luôn build được; chuyển biên bản phỏng vấn thành thay đổi yêu cầu | Phỏng vấn nhân viên nhà hàng và nhận ra thời gian thực tế (bàn 3 giờ, giữ bàn 45 phút) khác với giả định |

## Các dòng phụ lục của Giang

### Phụ lục B — Khai báo sử dụng công cụ AI

| Công cụ | Dùng để làm gì | Phần công việc | Cách kiểm chứng kết quả |
|---|---|---|---|
| Claude (Anthropic), qua Claude Code | Tạo khung dự án chung, mã và màn hình phân hệ Bàn & Đặt bàn, trang web cho khách, giao diện thiết kế lại và `start.bat` | `src/backend`, `src/frontend`, `start.bat` | Giang đọc mọi lớp của phân hệ, chạy 182 kiểm thử tự động và kiểm tra thủ công từng màn hình ở cả hai giao diện và hai ngôn ngữ |
| Claude (Anthropic), qua Claude Code | Viết kiểm thử tự động và các test case TC-RES-01 đến TC-RES-34 | `src/backend/src/test`, `tests/test-cases-table-reservation.md` | Mỗi kết quả mong đợi được so với yêu cầu và quy tắc nghiệp vụ; giá trị biên BR-01 được tính lại thủ công (19:00 + 195 phút = 22:15) |
| Claude (Anthropic), qua Claude Code; Figma AI | Dịch và ẩn danh biên bản phỏng vấn; soạn sơ đồ, bản xem trước thiết kế giao diện và Chương 1, 3, 4 của báo cáo | `docs/interview`, `docs/diagrams/giang`, `docs/design`, `docs/report` | Biên bản được ba thành viên dự phỏng vấn kiểm tra; sơ đồ và nội dung báo cáo được đối chiếu với mã nguồn và kết quả kiểm thử |

### Phụ lục C — Bảng đóng góp của từng thành viên

| Thành viên | Trách nhiệm chính | Sản phẩm chính | Số commit | Tỉ lệ đóng góp (%) | Chữ ký |
|---|---|---|---|---|---|
| Kiều Bảo Giang (nhóm trưởng) | Nhóm trưởng; kho mã và quy trình làm việc; khung dự án chung, đăng nhập và vai trò; phân hệ Bàn & Đặt bàn; viết biên bản phỏng vấn nhà hàng; phần đầu báo cáo, Chương 1 và 4, ghép báo cáo cuối kỳ | Proposal; `docs/diagrams/giang/`; Flyway V1; backend và frontend Bàn & Đặt bàn; 60 kiểm thử tự động và TC-RES-01 đến TC-RES-34; trang web cho khách và đặt bàn trực tuyến; giao diện chung, giao diện sáng và tối, tiếng Việt; `start.bat`; tự kiểm, kiểm tra bảo mật | 48 | 59 | |
