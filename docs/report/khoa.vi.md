# Báo cáo cuối kỳ - Phần của Nguyễn Nam Khoa (bản tiếng Việt)

Bản dịch tiếng Việt của `khoa.md`, dùng cho `build_report.py vi`. Nội dung giống bản tiếng Anh; khi sửa một bản thì sửa cả bản kia.

## Chương 3 — Các mục của phân hệ Thanh toán & Báo cáo

### 3.1 Mô tả bài toán và mục tiêu hệ thống (phần phân hệ)

**Phát biểu bài toán.**
Cuối bữa ăn và cuối ca, nhà hàng được phỏng vấn (18/10/2026, phát hiện F11 và F12) gặp ba vấn đề:
- Chia hóa đơn giữa các khách được làm bằng máy tính bỏ túi, và các phần thường không cộng lại đúng tổng.
- Nhà hàng nhận tiền mặt, thẻ, chuyển khoản và ví điện tử, nhưng không ai nói nhanh được mỗi phương thức thu bao nhiêu.
- Đối chiếu cuối ca làm thủ công: đếm két tiền và gom ảnh chụp chuyển khoản.

Ứng dụng gọi món nhà hàng đang dùng không liên kết với hóa đơn, nên có thể tính tiền một bàn khi bếp còn đang nấu, và có thể sửa hóa đơn sau khi đã trả.

**Mục tiêu của phân hệ.**
- Chỉ tạo hóa đơn từ đơn gọi món khi bếp đã xong, và khóa đơn (FR-21, BR-09).
- Chia hóa đơn đều hoặc theo món sao cho các phần luôn cộng lại đúng tổng (FR-23, FR-24, BR-10).
- Ghi nhận thanh toán tiền mặt, thẻ, chuyển khoản và ví điện tử, có tiền thối cho tiền mặt (FR-25, BR-11, CR-04).
- Đóng hóa đơn đã trả đủ, chuyển hóa đơn sang chỉ xem và trả bàn, trong một giao dịch (FR-26, BR-12, NFR-08).
- Gắn mọi khoản thanh toán với ca đang mở và cung cấp cho Quản lý báo cáo ca có đối chiếu tiền mặt (FR-27 đến FR-29, BR-13).

### 3.2 Phân tích và thiết kế hệ thống (phần phân hệ)

#### 3.2.1 Phân tích chức năng
- Use case UC-11 đến UC-15, Phụ lục A; sơ đồ use case `docs/diagrams/giang/01-use-case.png`.
- Sơ đồ hoạt động của luồng thanh toán: `docs/diagrams/khoa/12-activity-5-billing.png`.

#### 3.2.2 Luồng dữ liệu
- DFD mức 1, quy trình thanh toán: `docs/diagrams/khoa/10-dfd-1c-billing.png`.

#### 3.2.3 Thiết kế cơ sở dữ liệu
ERD: `docs/diagrams/khoa/updated/w9-erd-billing.png` (README §2), khớp với Flyway `V4__billing_reporting.sql`.

| Bảng | Mục đích | Ràng buộc chính |
|---|---|---|
| `shift` | Ca làm việc do Quản lý mở và đóng | `opened_by_id`, `closed_by_id` khóa ngoại đến `users`; `counted_cash` (FR-29) ≥ 0 hoặc null; `open_marker` là 1 khi ca mở và null khi đã đóng, và **duy nhất**, nên chính cơ sở dữ liệu chỉ cho phép một ca mở (BR-13) |
| `bill` | Hóa đơn của một đơn gọi món | Khóa ngoại đến `customer_order`; `total` BIGINT VND, CHECK > 0; `status` OPEN, CLOSED hoặc CANCELLED; `split_type` NONE, EVEN hoặc BY_ITEM; `shift_id` = ca mà hóa đơn được đóng |
| `bill_line` | Một món trên hóa đơn | Tên, số lượng và đơn giá được **sao chép** từ dòng đơn, nên thay đổi thực đơn sau này không làm đổi hóa đơn; `share_id` chỉ có khi chia theo món |
| `bill_share` | Phần của một khách trong hóa đơn đã chia | (`bill_id`, `number`) duy nhất; `amount` CHECK > 0 |
| `payment` | Tiền đã nhận | Khóa ngoại đến `bill`, `shift`, `users` và tùy chọn `bill_share`; `amount` là số tiền khách đưa, CHECK > 0; `change_amount` ≥ 0 và < amount; doanh thu là `amount - change_amount` |

Tiền luôn là số nguyên VND trong cột `BIGINT` và kiểu `long` của Java, không bao giờ là số thực (NFR-08).

#### 3.2.4 Thiết kế lớp và máy trạng thái
Hình cho báo cáo nằm trong `docs/diagrams/khoa/updated/`; mã nguồn Mermaid nằm trong `docs/diagrams/khoa/README.md`.
- Sơ đồ lớp: `w9-class-billing.png` (README §1).
- Máy trạng thái: hóa đơn `w9-state-bill.png` (README §3); ca làm việc `w9-state-shift.png` (README §4).
- Sơ đồ tuần tự: tạo và chia hóa đơn, `w9-sequence-generate-split.png` (README §5); ghi nhận thanh toán và đóng hóa đơn, `w9-sequence-pay-close.png` (README §6).

Quyết định thiết kế:
- **Các quy tắc nằm trong `Bill`.** BR-09 đến BR-12 và phần thanh toán của BR-13 là các phương thức của entity `Bill`, nên `BillTest` kiểm tra chúng mà không cần Spring hay cơ sở dữ liệu. Các service chỉ nạp và khóa các dòng.
- **Lưu số tiền khách đưa, không chỉ số tiền được tính.** `Payment.appliedAmount() = amount - changeAmount`. Hóa đơn, các phần và báo cáo đều cộng số tiền được tính, nên tờ 500.000 cho hóa đơn 465.000 được tính là 465.000 doanh thu, và 35.000 tiền thối vẫn được ghi lại.
- **Báo cáo được tính toán, không lưu sẵn.** `ShiftReport` trong sơ đồ lớp Tuần 4 trở thành `ShiftReportResponse`, được tạo khi có yêu cầu từ hai truy vấn. Không có gì phải cập nhật đồng bộ khi thêm một khoản thanh toán.
- **Một ca mở, kiểm tra hai lần.** Service từ chối ca mở thứ hai với thông báo rõ ràng; khóa duy nhất trên `open_marker` bắt được trường hợp hai Quản lý bấm cùng lúc.

```java
public static long[] evenShares(long total, int count) {
    if (count < MIN_SHARES || count > MAX_SHARES) {
        throw new InvalidInputException("shares", "The number of shares must be between 2 and 20");
    }
    long each = total / count / ROUNDING_UNIT * ROUNDING_UNIT;
    if (each <= 0) {
        throw new BusinessRuleException("A bill of " + Money.format(total) + " is too small to split into " + count + " shares");
    }
    long[] amounts = new long[count];
    Arrays.fill(amounts, each);
    amounts[0] = total - each * (count - 1);
    return amounts;
}
```
*Đoạn mã 3.5. BR-10 trong `Bill`.*
Phép chia số nguyên làm tròn xuống, nên `total / count / 1000 * 1000` là phần chia làm tròn xuống đến 1.000 VND.
Phần 1 nhận phần còn lại, nên các phần luôn cộng lại đúng tổng: 1.000.000 chia 3 được 334.000 + 333.000 + 333.000.

```java
public Payment pay(BillShare share, PaymentMethod method, long amount, Shift openShift, User recordedBy, LocalDateTime at) {
    requireOpen();                                                   // BR-12
    if (openShift == null || !openShift.isOpen()) throw new BusinessRuleException("No shift is open; …");   // BR-13
    if (amount <= 0) throw new InvalidInputException("amount", "The amount must be greater than 0");       // BR-11
    long due = isSplit() ? share.remaining() : remaining();          // (share checks left out here)
    long change = 0;
    if (method == PaymentMethod.CASH) change = Math.max(0, amount - due);
    else if (amount > due) throw new InvalidInputException("amount", "A card payment cannot be more than …"); // BR-11
    Payment payment = new Payment(this, share, openShift, method, amount, change, at, recordedBy);
    payments.add(payment);
    if (remaining() == 0) close(openShift, at);                      // FR-26
    return payment;
}

private void close(Shift closingShift, LocalDateTime at) {
    status = BillStatus.CLOSED;
    closedAt = at;
    shift = closingShift;
    order.close();
    order.getTable().changeStatus(TableStatus.NEEDS_CLEANING);       // BR-05, Giang's state machine
}
```
*Đoạn mã 3.6. BR-11 đến BR-13 và FR-26 trong `Bill` (rút gọn).*
Mọi kiểm tra chạy trước khi thêm bất cứ thứ gì. Khoản thanh toán cuối đóng hóa đơn, đơn gọi món và bàn trong cùng một giao dịch (`BillingService.pay`), nên hoặc tất cả được lưu, hoặc không gì được lưu (NFR-08).

#### 3.2.5 Thiết kế giao diện
Các màn hình, trong `docs/screenshots/khoa/`:
- **Màn hình hóa đơn** (Phục vụ/Lễ tân), mở bằng nút **Bill** của bàn Có khách trên tổng quan khu phục vụ hoặc từ màn hình gọi món. Bên trái là các dòng hóa đơn, tổng, đã trả và còn lại; bên phải là thanh toán và chia hóa đơn. Ảnh màn hình: `01-bill-generated.png`, `02-bill-split-evenly-cash-change.png`, `04-bill-split-by-item.png`, `03-bill-paid-and-closed.png`.
  - Ô số tiền được điền sẵn số tiền phải trả, nên một lần thanh toán mất 4 bước từ tổng quan khu phục vụ: Bill → Generate bill → phương thức → Record payment (NFR-05).
  - Với tiền mặt, tiền thối được hiện trước khi gửi thanh toán; với thẻ, chuyển khoản và ví điện tử, số tiền vượt mức phải trả được báo cạnh ô nhập (`06-card-more-than-due.png`).
  - Các trường hợp bị từ chối được hiện bằng thông báo đỏ, ví dụ bếp còn đang nấu (`05-bill-refused-kitchen-busy.png`).
- **Màn hình ca làm việc** (Quản lý): mở hoặc đóng ca với số tiền mặt đếm được, danh sách ca, và báo cáo của ca được chọn. Ảnh màn hình: `07-shift-report-provisional.png`, `08-close-shift-refused-unpaid.png`, `09-shift-report-closed-cash-check.png`.
- Tổng quan khu phục vụ với nút Bill: `10-floor-bill-button.png`.

### 3.3 Phát triển và hiện thực hệ thống (phần phân hệ)

#### 3.3.1 Công nghệ và công cụ
Giống bảng của Giang. Phân hệ của Khoa không thêm thư viện nào.

#### 3.3.2 Các chức năng đã hiện thực

| Chức năng (FR / UC) | Đầu vào | Đầu ra | Endpoint | Màn hình |
|---|---|---|---|---|
| Tạo hóa đơn (FR-21, UC-12, BR-09) | mã đơn | Hóa đơn OPEN với các dòng tính tiền được sao chép; đơn LOCKED. Từ chối khi còn món Mới, Đang làm hoặc chưa gửi, khi chưa có ca mở, hoặc khi đơn đã có hóa đơn | `POST /api/bills` | Hóa đơn |
| Xem hóa đơn (FR-22) | mã bàn hoặc mã hóa đơn | Các dòng, tổng, đã trả, còn lại, các phần và các khoản thanh toán | `GET /api/bills?tableId=`, `GET /api/bills/{id}` | Hóa đơn |
| Hủy hóa đơn (UC-12 4a, BR-09) | mã hóa đơn | CANCELLED (được giữ lại) và đơn trở lại OPEN; từ chối khi đã có thanh toán | `POST /api/bills/{id}/cancel` | Hóa đơn |
| Chia đều (FR-23, BR-10) | 2–20 phần | Các phần làm tròn đến 1.000 VND, phần dư cộng vào phần 1; thay cho cách chia trước đó; từ chối khi đã có thanh toán | `POST /api/bills/{id}/split/even`, `DELETE /api/bills/{id}/split` | Hóa đơn |
| Chia theo món (FR-24) | các món của từng phần | Mỗi nhóm một phần, số tiền = các món của nhóm; từ chối nếu có món bị bỏ sót hoặc được chia hai lần | `POST /api/bills/{id}/split/by-item` | Hóa đơn |
| Ghi nhận thanh toán (FR-25, BR-11, CR-04) | phương thức (tiền mặt, thẻ, chuyển khoản, ví điện tử), số tiền, phần khi đã chia | Lưu khoản thanh toán kèm tiền thối cho tiền mặt; từ chối số tiền 0, số tiền không phải tiền mặt vượt mức phải trả, hoặc hóa đơn đã đóng | `POST /api/bills/{id}/payments` | Hóa đơn |
| Đóng hóa đơn (FR-26, BR-12) | — (tự động) | Khi trả đủ: hóa đơn CLOSED và chỉ xem, đơn CLOSED, bàn Cần dọn, trong một giao dịch | (một phần của thanh toán) | Hóa đơn, tổng quan khu phục vụ |
| Mở và đóng ca (FR-27, BR-13, UC-11) | tiền mặt đếm được, tùy chọn (FR-29) | Mỗi lúc một ca mở; từ chối đóng ca khi còn hóa đơn chưa trả, kèm danh sách bàn | `POST /api/shifts`, `POST /api/shifts/{id}/close`, `GET /api/shifts`, `GET /api/shifts/current` | Ca làm việc |
| Báo cáo ca (FR-28, FR-29, UC-15) | mã ca | Hóa đơn đã đóng, doanh thu, doanh thu theo phương thức, số khoản thanh toán và số hóa đơn đã chia, tiền mặt dự kiến và đếm được cùng chênh lệch; ghi là tạm tính khi ca còn mở | `GET /api/shifts/{id}/report` | Ca làm việc |
| Dữ liệu mẫu | — | 12 đơn mẫu được lập hóa đơn và thanh toán trong 12 ca đã qua (trưa 10:00–14:30, tối 17:00–22:30) qua các phương thức miền; một ca thiếu 20.000 VND trong két | `BillingSeeder` (được `OrderSeeder` của Lâm gọi) | Ca làm việc |

**Ghi chú hiện thực**
- **Khóa theo một thứ tự.** Mọi yêu cầu thanh toán lấy khóa dòng theo cùng thứ tự: ca đang mở → đơn hoặc hóa đơn → bàn (`SELECT … FOR UPDATE`). Vì vậy hai yêu cầu không bao giờ chờ nhau vòng tròn (không có deadlock).
  - Một khoản thanh toán và việc Quản lý đóng ca đều khóa dòng ca trước, nên ca không thể bị đóng giữa lúc kiểm tra "có ca đang mở" và lúc lưu khoản thanh toán (BR-13).
  - Hai khoản thanh toán cho cùng hóa đơn được xử lý lần lượt, nên hóa đơn không bao giờ bị trả hai lần.
- **Liên kết với các phân hệ khác.**
  - `CustomerOrder` của Lâm cung cấp `lock()`, `close()` và trạng thái ở bếp của từng dòng; Khoa thêm `unlock()` cho hóa đơn bị hủy.
  - Bàn chỉ chuyển sang Cần dọn qua `DiningTable.changeStatus` của Giang, nên vòng đời bàn của BR-05 vẫn được kiểm soát ở một nơi.
  - Thao tác "Guests left" của Giang vốn từ chối bàn có đơn LOCKED, giờ nghĩa là "hóa đơn chưa trả" (UC-06 1a, TC-BIL-33).
- **Chia lại.** Các phần được đánh số 1..N với khóa duy nhất. Hibernate chèn dòng mới trước khi xóa dòng cũ, nên service xóa cách chia cũ và flush trước khi tạo cách chia mới.
- **Lỗi.** Vi phạm quy tắc trả về 409 kèm thông báo phục vụ có thể làm theo ("The shift cannot be closed: unpaid bills at table 3, table 7 (BR-13)"); đầu vào sai trả về 400 với thông báo cạnh ô nhập.

#### 3.3.3 Chức năng chưa hiện thực hoặc chưa hoàn chỉnh

| Mục | Tình trạng | Lý do |
|---|---|---|
| In biên lai | Chưa làm | Không có trong yêu cầu; màn hình hóa đơn hiển thị hóa đơn đã đóng |
| Hoàn tiền hoặc điều chỉnh thanh toán | Chưa làm | BR-12 làm hóa đơn đã đóng là cuối cùng; lỗi trước khi đóng chỉ sửa được bằng cách trả phần còn lại |
| Chuyển hóa đơn sang bàn khác | Không thể | FR-19 của Lâm chỉ chuyển đơn OPEN; đơn LOCKED (đã tạo hóa đơn) ở lại bàn của nó. Hủy hóa đơn, chuyển bàn, rồi tạo lại hóa đơn |
| Giảm giá, phí phục vụ, VAT | Chưa làm | Không có trong yêu cầu; giá thực đơn là giá cuối cùng |
| NFR-05, một lần thanh toán trong 4 bước | Đếm trên màn hình | 4 cú bấm từ tổng quan khu phục vụ; chưa đo giờ với người dùng |

#### 3.3.4 Sản phẩm khác
- Flyway migration `V4__billing_reporting.sql`.
- Sơ đồ cập nhật theo mã nguồn: `docs/diagrams/khoa/updated/`, mã nguồn trong `docs/diagrams/khoa/README.md`.
- Test case `tests/test-cases-billing.md` và mẫu test case dùng chung `tests/test-case-template.md`.
- Hướng dẫn chạy `docs/run-guide.md` (cài đặt và chạy thử toàn bộ một lượt khách, từ đặt bàn đến báo cáo ca).

### 3.4 Kiểm thử và đánh giá (phần phân hệ)

#### 3.4.1 Quy trình kiểm thử
- **Phương pháp.**
  - Kiểm thử đơn vị cho các quy tắc thanh toán không dùng Spring: `BillTest` (BR-09 đến BR-13, kể cả kiểm thử tham số hóa BR-10 với năm tổng tiền).
  - Kiểm thử API bằng MockMvc trên H2 với dữ liệu mẫu thật: `BillControllerTest`, `ShiftControllerTest`.
  - Kiểm thử hiệu năng cho NFR-04: `BillingPerformanceTest`.
  - Kiểm thử rằng các hóa đơn mẫu tuân theo quy tắc: `BillingSeederTest`.
  - Các màn hình được chạy thử trên trình duyệt với hồ sơ H2 bằng script Playwright tự động (mở ca, tạo hóa đơn, chia đều, trả tiền mặt có tiền thối, thẻ, ví điện tử, chia theo món, đóng ca với tiền mặt đếm được). Ảnh màn hình trong `docs/screenshots/khoa/` lấy từ lần chạy đó. Lần chạy thử phát hiện một lỗi, đã sửa trước khi phát hành: ô số tiền có `step="1000"`, nên trình duyệt từ chối các số như 150.000.
- **Test case.** TC-BIL-01 đến 35 và TC-SHF-01 đến 10 (45 test case) trong `tests/test-cases-billing.md`, Phụ lục D.
- **Kết quả** ngày 21/11/2026:
  - `mvn test`: 176 kiểm thử tự động, 0 lỗi. 51 trong số đó thuộc Thanh toán & Báo cáo.
  - NFR-04: hóa đơn 50 dòng trả lời trong 2 ms (giới hạn 2 giây); báo cáo 300 hóa đơn trong 25 ms (giới hạn 3 giây), trên H2.
  - Cả 45 test case đã ghi đều đạt.

#### 3.4.2 Đánh giá hệ thống
**Điểm mạnh**
- Các phần chia đều luôn cộng lại đúng tổng (TC-BIL-07), đây là phàn nàn chính của nhà hàng về chia hóa đơn (F11).
- Khoản thanh toán cuối, đóng hóa đơn và trả bàn nằm trong một giao dịch (NFR-08); hóa đơn đã đóng không thể thay đổi (TC-BIL-18).
- Đối chiếu tiền mặt (dự kiến, đếm được, chênh lệch) thay cho việc đếm két và so với ảnh chụp (F12, CR-05).
- Báo cáo chỉ cần hai truy vấn dù có bao nhiêu hóa đơn, nên NFR-04 đạt với khoảng cách lớn.
- Chỉ một ca được mở kể cả khi hai Quản lý bấm cùng lúc (khóa duy nhất), và ca không thể đóng giữa lúc đang thanh toán (khóa dòng).

**Hạn chế**
- NFR-04 được đo trên H2 trong bộ nhớ, không phải MySQL trên máy tính.
- Các màn hình React chưa có kiểm thử thành phần tự động; chúng được kiểm tra bằng lần chạy Playwright và thủ công.
- Khoản thanh toán nhập nhầm không thể hoàn lại (chưa có chức năng hoàn tiền).
- Doanh thu được tính theo ca; chưa có báo cáo ngày hay tháng.

#### 3.4.3 So sánh lý thuyết và thực tế
- **Từ lý thuyết đến thực tế.**
  - Sơ đồ trạng thái hóa đơn trở thành ba trạng thái và một điều kiện chặn (`requireOpen`): hóa đơn đã đóng hoặc đã hủy từ chối mọi thay đổi.
  - "Tiền là số nguyên" (NFR-08) loại bỏ mọi vấn đề làm tròn, trừ vấn đề mà BR-10 cố ý quy định.
  - Tính nguyên tử: phương thức miền kiểm tra mọi thứ trước khi thêm khoản thanh toán, và service chạy toàn bộ việc thanh toán trong một giao dịch.
- **Khác biệt.**
  - Sơ đồ lớp Tuần 4 lưu `ShiftReport`; trong mã nguồn báo cáo được tính toán, nên không bao giờ lệch với các khoản thanh toán.
  - ERD Tuần 4 không có liên kết từ hóa đơn đến ca. Báo cáo cần biết hóa đơn được đóng trong ca nào, nên đã thêm `bill.shift_id`.
  - Buổi phỏng vấn thêm ví điện tử (CR-04) và tiền mặt đếm được (CR-05, FR-29); cả hai đều đã làm. Chia theo món (FR-24) hạ xuống Could (CR-03) nhưng vẫn được làm.

## Các dòng Chương 4 (để Giang ghép)

**4.2 Kiến thức và kỹ năng đạt được**

| Thành viên | Kỹ năng kỹ thuật | Kỹ năng chuyên môn | Kinh nghiệm thực tế |
|---|---|---|---|
| Nguyễn Nam Khoa | Lưu tiền bằng số nguyên VND trong `long`/`BIGINT` và giữ việc làm tròn ở một nơi (BR-10); ánh xạ hóa đơn với các dòng, các phần và các khoản thanh toán bằng JPA (cascade, orphan removal, và lý do Hibernate chèn trước khi xóa); khóa dòng bi quan và lấy khóa theo thứ tự cố định để tránh deadlock; khóa duy nhất làm lớp bảo vệ thứ hai cho "chỉ một ca mở"; truy vấn JPQL tổng hợp với `GROUP BY` và `join fetch` cho báo cáo nhanh; kiểm thử đơn vị không dùng Spring và kiểm thử API bằng MockMvc; màn hình React hiện tiền thối trước khi gửi thanh toán | Viết kết quả mong đợi của test case từ yêu cầu trước khi chạy; đọc mã của thành viên khác (đơn gọi món của Lâm, vòng đời bàn của Giang) và chỉ thay đổi qua các phương thức public của họ; làm việc trên nhánh tính năng và mô tả thay đổi trong commit; ghi kết quả kiểm thử thật kèm ngày | Buổi phỏng vấn cho thấy khó khăn thật của nhà hàng là cuối ca: đếm két và đối chiếu ảnh chụp chuyển khoản thủ công. Điều đó biến ô tiền mặt đếm được (CR-05) từ một chi tiết nhỏ thành phần hữu ích nhất của báo cáo. Chia hóa đơn "bằng máy tính bỏ túi" bị sai vì làm tròn, nên BR-10 cộng phần dư vào một phần. Phân hệ cũng phụ thuộc vào hai phân hệ còn lại: hóa đơn cần bếp đã xong, và đóng hóa đơn phải trả bàn |

## Các dòng phụ lục của Khoa

### Phụ lục B — Khai báo sử dụng công cụ AI

| Công cụ | Dùng để làm gì | Phần công việc | Cách kiểm chứng kết quả |
|---|---|---|---|
| Claude (Anthropic), qua Claude Code | Tạo mã, màn hình, kiểm thử và test case phân hệ Thanh toán & Báo cáo; soạn sơ đồ, hướng dẫn chạy và phần phân hệ trong Chương 3 | `src/backend`, `src/frontend`, `tests/test-cases-billing.md`, `docs/diagrams/khoa`, `docs/run-guide.md` | Khoa chạy kiểm thử tự động, chạy thử các màn hình trên trình duyệt, đọc `Bill` và `V4__billing_reporting.sql` đối chiếu với BR-09 đến BR-13 và tính lại thủ công các ví dụ BR-10 |

### Phụ lục C — Bảng đóng góp của từng thành viên

| Thành viên | Trách nhiệm chính | Sản phẩm chính | Số commit | Tỉ lệ đóng góp (%) | Chữ ký |
|---|---|---|---|---|---|
| Nguyễn Nam Khoa | Phân hệ Thanh toán & Báo cáo: hóa đơn, chia hóa đơn, thanh toán, ca làm việc, báo cáo ca; mẫu test case dùng chung | Bản nháp yêu cầu; `docs/diagrams/khoa/`; Flyway V4; backend và frontend Thanh toán & Báo cáo; 51 kiểm thử tự động và 45 test case; hóa đơn và ca mẫu; hướng dẫn chạy | 5 | 6 | |
