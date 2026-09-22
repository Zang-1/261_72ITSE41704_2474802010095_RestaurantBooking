# Module Order & Bếp - FR/NFR và Use Case (Lâm)

2026-09-22 · @Someone

## Bối cảnh và vấn đề của quy trình order/bếp hiện tại

Trong mô hình vận hành truyền thống tại nhiều nhà hàng vừa và nhỏ, quy trình ghi order và chuyển thông tin xuống bếp vẫn phụ thuộc nhiều vào giấy tờ hoặc trao đổi miệng giữa waiter và bếp. Cách làm này dẫn đến một số vấn đề thường gặp:

- Order bị thất lạc hoặc ghi sai món khi quán đông khách.
- Bếp không nắm được thứ tự ưu tiên giữa các món khai vị và món chính, khiến món ra không đúng trình tự.
- Khi một nguyên liệu hoặc món hết hàng, thông tin không được cập nhật kịp thời tới waiter, dẫn đến việc nhận order cho món không còn khả năng phục vụ.
- Việc sửa đổi order sau khi bếp đã bắt đầu chế biến thường gây lãng phí nguyên liệu và tranh cãi giữa các bộ phận.

Module Order & Bếp của hệ thống được xây dựng nhằm số hóa quy trình này: cho phép waiter tạo và gửi order trực tiếp xuống màn hình bếp theo thời gian thực, tự động sắp xếp thứ tự món theo course, đồng bộ trạng thái hết món giữa các màn hình, và khóa order lại khi kitchen ticket đã bắt đầu xử lý để đảm bảo tính nhất quán giữa những gì khách gọi và những gì bếp thực hiện.

## Danh sách FR/NFR&#32;

### Functional Requirements

| Mã | Yêu cầu | Ghi chú |
| --- | --- | --- |
| FR-O1 | Waiter tạo order mới gắn với một bàn đang có khách, chọn món từ menu theo danh mục (MenuCategory). | Nền tảng |
| FR-O2 | Waiter thêm, sửa số lượng, xóa món và ghi chú đặc biệt cho từng dòng món (OrderLine, ví dụ "không hành", "ít cay") trước khi gửi bếp. |  |
| FR-O3 | Mỗi món được gán một Course (khai vị, món chính, tráng miệng). Hệ thống gửi bếp theo thứ tự course, khai vị trước món chính. | Rule Tuần 7 |
| FR-O4 | Khi waiter gửi order, hệ thống tự tạo KitchenTicket gồm các KitchenTicketItem và hiển thị trên màn hình bếp (Kitchen Display). | Cốt lõi |
| FR-O5 | Kitchen Staff cập nhật trạng thái từng món/ticket (Mới → Đang chế biến → Sẵn sàng), waiter nhìn thấy trạng thái này. |  |
| FR-O6 | Manager quản lý menu: thêm, sửa, xóa/ẩn món, đổi giá, gán danh mục và course. | CRUD Tuần 5 |
| FR-O7 | Kitchen Staff hoặc Manager đánh dấu món hết hàng; món đó biến mất khỏi màn hình order ngay lập tức. | Rule Tuần 7 |
| FR-O8 | Hệ thống không cho sửa hoặc hủy món sau khi kitchen ticket đã bắt đầu chế biến. | Liên quan cả phần Khoa |
| FR-O9 | Waiter gộp hoặc chuyển order sang bàn khác. | Mở rộng |
| FR-O10 | Waiter xem lại lịch sử order của một bàn trong ca hiện tại. | Mở rộng |

### Non-Functional Requirements (chọn 2-3)

| Mã | Yêu cầu |
| --- | --- |
| NFR-O1 | **Hiệu năng:** Order gửi xuống bếp hiển thị trên màn hình bếp trong vòng 2-3 giây. |
| NFR-O2 | **Tính nhất quán dữ liệu:** Khi món bị đánh dấu hết, mọi màn hình order đang mở phản ánh thay đổi trong vòng vài giây. |
| NFR-O3 | **Khả dụng (Usability):** Waiter hoàn thành một order tiêu chuẩn (5 món) trong không quá khoảng 1 phút. |
| NFR-O4 | **Độ tin cậy:** Nếu gửi bếp thất bại (lỗi DB/mạng), order không bị mất và không tạo ticket trùng lặp. |
| NFR-O5 | **Phân quyền/Bảo mật:** Chỉ Manager được sửa menu và giá; Waiter chỉ tạo và sửa order của mình; Kitchen Staff chỉ đổi trạng thái ticket. |

## Use case 1: Waiter tạo order và gửi xuống bếp

**Actor chính:** Waiter

**Actor phụ:** Kitchen Staff (nhận ticket)

**Tiền điều kiện:** Bàn đã có khách (trạng thái occupied); waiter đã đăng nhập.

**Luồng chính:**

1. Waiter chọn bàn cần lên order.
2. Hệ thống hiển thị menu theo danh mục (MenuCategory), ẩn các món đang hết hàng.
3. Waiter chọn món, nhập số lượng và ghi chú đặc biệt cho từng OrderLine.
4. Waiter xác nhận gửi order xuống bếp.
5. Hệ thống tạo KitchenTicket, sắp xếp các món theo thứ tự Course (khai vị trước món chính).
6. Ticket hiển thị trên màn hình bếp (Kitchen Display) cho Kitchen Staff.
7. Hệ thống xác nhận với waiter rằng order đã được gửi thành công.

**Luồng phụ:**

- **3a. Món vừa chọn hết hàng giữa lúc order:** hệ thống báo ngay và loại món đó khỏi order trước khi waiter xác nhận gửi.
- **4a. Gửi bếp lỗi (mất kết nối/DB lỗi):** hệ thống giữ nguyên order ở trạng thái nháp, báo lỗi cho waiter, không tạo ticket trùng lặp; waiter có thể thử gửi lại.
- **7a. Waiter cần thêm món sau khi đã gửi:** nếu kitchen ticket của order đó chưa bắt đầu chế biến, hệ thống cho phép thêm món mới (tạo bổ sung vào ticket); nếu đã bắt đầu chế biến, hệ thống từ chối và yêu cầu tạo order mới.

**Hậu điều kiện:** Order được lưu, KitchenTicket tương ứng hiển thị đúng trên màn hình bếp theo đúng thứ tự course.

## Use case 2: Kitchen Staff xử lý ticket và đánh dấu hết món

**Actor chính:** Kitchen Staff

**Actor phụ:** Waiter (nhận cập nhật trạng thái), Manager (cũng có thể đánh dấu hết món)

**Tiền điều kiện:** Kitchen Staff đã đăng nhập, màn hình bếp đang mở với các ticket đang chờ.

**Luồng chính:**

1. Kitchen Staff xem danh sách ticket đang chờ trên Kitchen Display, sắp xếp theo thứ tự course và thời gian gửi.
2. Kitchen Staff chọn một ticket/món và chuyển trạng thái từ "Mới" sang "Đang chế biến".
3. Hệ thống khóa order tương ứng, không cho waiter sửa hoặc hủy các món trong ticket đó nữa.
4. Kitchen Staff hoàn tất món, chuyển trạng thái sang "Sẵn sàng".
5. Waiter nhận thông báo món đã sẵn sàng để phục vụ.

**Luồng phụ:**

- **1a. Kitchen Staff phát hiện một món trong menu đã hết nguyên liệu:** đánh dấu món đó là hết hàng; hệ thống lập tức ẩn món khỏi màn hình order của mọi waiter, các ticket đã gửi trước đó không bị ảnh hưởng.
- **4a. Món bị trả lại do lỗi chế biến:** Kitchen Staff chuyển trạng thái về "Đang chế biến", ghi chú lý do; hệ thống lưu lại lịch sử thay đổi trạng thái của ticket.
- **1b. Món hết hàng giữa lúc đang có ticket chờ chứa món đó:** hệ thống vẫn cho phép hoàn tất các ticket đã tồn tại trước khi món bị đánh dấu hết, chỉ chặn các order mới.

**Hậu điều kiện:** Trạng thái ticket và trạng thái hết món được cập nhật đồng bộ trên mọi màn hình order và màn hình bếp.

## Business rules (Tuần 7 - Iteration 2)

Đây là tuần quan trọng nhất trong các tuần không chấm điểm - toàn bộ business rule cốt lõi của module Order & Bếp nằm ở đây. Mỗi rule cần có test case chứng minh, kể cả edge case.

| Mã | Business rule | Edge case cần test |
| --- | --- | --- |
| BR-O1 | **Thứ tự món (course sequencing):** món khai vị phải được gửi xuống bếp trước món chính; hệ thống tự sắp xếp thứ tự gửi theo Course, waiter không thể gửi món chính trước khi khai vị đã gửi. | Order chỉ có 1 course; order có nhiều món cùng course; món tráng miệng gửi trước khi món chính hoàn tất. |
| BR-O2 | **Món hết hàng biến mất ngay lập tức:** khi Kitchen Staff hoặc Manager đánh dấu một món hết hàng, món đó phải biến mất khỏi màn hình order của mọi waiter ngay lập tức, không cần tải lại trang. | Món vừa hết đúng lúc một waiter khác đang thêm món đó vào order; món hết hàng nhưng đã nằm trong ticket đã gửi trước đó (ticket cũ vẫn giữ nguyên). |
| BR-O3 | **Khóa order sau khi ticket bắt đầu chế biến:** ngay khi Kitchen Staff chuyển trạng thái ticket sang "Đang chế biến", order tương ứng bị khóa — waiter không thể sửa số lượng, xóa món hay đổi ghi chú của các món trong ticket đó. | Waiter cố sửa order đúng lúc Kitchen Staff vừa bấm "bắt đầu chế biến" (race condition); thêm món MỚI vào order đã có ticket đang chế biến (rule BR-O3 chỉ khóa các món đã có trong ticket, không chặn thêm ticket phụ). |

**Ghi chú triển khai:** viết test case cho từng rule ở Tuần 7, bao gồm ít nhất 1 edge case mỗi rule, và nộp cùng Appendix D (self-check) cuối tuần.
