Mỗi use case theo cấu trúc Cockburn đề xuất [7]: tác nhân, điều kiện trước, luồng chính, các luồng phụ đánh số theo bước mà chúng rẽ nhánh, và điều kiện sau.

### UC-01: Đăng nhập và đăng xuất
- **Tác nhân:** Phục vụ/Lễ tân, Nhân viên bếp, Quản lý
- **Điều kiện trước:** Người dùng có tài khoản được tạo sẵn.
- **Luồng chính:** 1. Người dùng mở ứng dụng và thấy màn hình đăng nhập. 2. Người dùng nhập tên đăng nhập và mật khẩu. 3. Hệ thống so mật khẩu với giá trị băm đã lưu. 4. Hệ thống mở màn hình chính theo vai trò: tổng quan khu phục vụ (Phục vụ/Lễ tân, Quản lý) hoặc màn hình bếp (Nhân viên bếp). 5. Khi người dùng bấm Đăng xuất, phiên kết thúc và màn hình đăng nhập được hiển thị.
- **Luồng phụ:** 3a. Sai tên đăng nhập hoặc mật khẩu: hệ thống báo "Invalid username or password" mà không nói ô nào sai. 2a. Một ô bị để trống: hệ thống hiện thông báo kiểm tra dữ liệu.
- **Điều kiện sau:** Người dùng đã đăng nhập và chỉ dùng được các chức năng của vai trò mình.

### UC-02: Quản lý bàn và khu vực
- **Tác nhân:** Quản lý
- **Điều kiện trước:** Quản lý đã đăng nhập.
- **Luồng chính:** 1. Quản lý mở Quản lý bàn. 2. Quản lý thêm hoặc sửa bàn: số bàn, sức chứa, khu vực. 3. Hệ thống kiểm tra dữ liệu và lưu. 4. Quản lý có thể ngừng sử dụng một bàn. 5. Quản lý có thể đổi thời gian dùng bữa và thời gian dọn bàn mặc định mà BR-01 sử dụng (ReservationSettings).
- **Luồng phụ:** 3a. Số bàn đã tồn tại, hoặc sức chứa ngoài khoảng 1–20: hệ thống báo lỗi và không lưu. 4a. Bàn còn lượt đặt trong tương lai: hệ thống từ chối ngừng sử dụng và cho biết có bao nhiêu lượt đặt.
- **Điều kiện sau:** Danh sách bàn được cập nhật và được dùng khi tìm bàn trống.

### UC-03: Tạo lượt đặt bàn
- **Tác nhân:** Phục vụ/Lễ tân
- **Điều kiện trước:** Phục vụ/Lễ tân đã đăng nhập; có ít nhất một bàn đang hoạt động.
- **Luồng chính:** 1. Phục vụ/Lễ tân mở màn hình Đặt bàn. 2. Nhập ngày, giờ bắt đầu và số khách rồi bấm Tìm. 3. Hệ thống kiểm tra dữ liệu và liệt kê các bàn vừa với nhóm khách và không có lượt đặt trùng giờ (BR-01, BR-02). 4. Phục vụ/Lễ tân chọn một bàn. 5. Nhập tên khách, số điện thoại và ghi chú tùy chọn, rồi bấm Xác nhận. 6. Hệ thống kiểm tra lại khung giờ, lưu lượt đặt và hiện mã đặt bàn.
- **Luồng phụ:** 3a. Dữ liệu sai (ngày trong quá khứ, số khách ≤ 0 hoặc lớn hơn mọi bàn): thông báo hiện cạnh ô nhập. 3b. Không có bàn trống: hệ thống gợi ý giờ khác hoặc danh sách chờ (UC-05). 6a. Khung giờ vừa bị người dùng khác đặt: lượt đặt bị từ chối với thông báo xung đột và danh sách được làm mới.
- **Điều kiện sau:** Lượt đặt ở trạng thái Đã xác nhận và khung giờ không còn trống.

### UC-04: Sửa, hủy hoặc nhận bàn cho lượt đặt
- **Tác nhân:** Phục vụ/Lễ tân
- **Điều kiện trước:** Có một lượt đặt Đã xác nhận.
- **Luồng chính:** 1. Phục vụ/Lễ tân mở danh sách lượt đặt của một ngày và chọn một lượt đặt. 2a. **Sửa:** đổi giờ, số khách hoặc bàn; hệ thống áp dụng lại BR-01 và BR-02 rồi lưu. 2b. **Hủy:** xác nhận; lượt đặt chuyển sang Đã hủy và khung giờ được trả lại. 2c. **Nhận bàn:** khách đã đến; bàn chuyển sang Có khách (BR-05).
- **Luồng phụ:** 2a-1. Thay đổi làm trùng giờ với lượt đặt khác: bị từ chối và giữ nguyên lượt đặt cũ. 2x. Đã qua giờ bắt đầu: không được sửa hay hủy. 2c-1. Bàn vẫn ở trạng thái Cần dọn: từ chối nhận bàn đến khi bàn được đánh dấu đã dọn. 2c-2. Trễ quá 15 phút: lượt đặt đã là Không đến; hệ thống gợi ý xếp nhóm khách như khách vãng lai (UC-05). (Sau buổi phỏng vấn, giới hạn này là 45 phút, CR-01.)
- **Điều kiện sau:** Lượt đặt và trạng thái bàn được cập nhật.

### UC-05: Quản lý danh sách chờ khách vãng lai
- **Tác nhân:** Phục vụ/Lễ tân
- **Điều kiện trước:** Phục vụ/Lễ tân đã đăng nhập.
- **Luồng chính:** 1. Một nhóm khách vãng lai đến và không có bàn phù hợp đang trống. 2. Phục vụ/Lễ tân nhập tên, số khách và số điện thoại, rồi bấm Thêm vào danh sách chờ. 3. Hệ thống lưu mục chờ với giờ đến và hiện vị trí. 4. Khi một bàn chuyển sang Trống, hệ thống làm nổi bật nhóm chờ sớm nhất vừa với bàn (BR-04). 5. Phục vụ/Lễ tân bấm Xếp bàn; bàn chuyển sang Có khách và nhóm khách rời danh sách.
- **Luồng phụ:** 2a. Thiếu tên hoặc số khách ≤ 0: thông báo kiểm tra dữ liệu. 4a. Không có nhóm chờ nào vừa: không có gì được làm nổi bật. 5a. Nhóm khách đã bỏ đi: Phục vụ/Lễ tân xóa mục chờ và các nhóm sau được đẩy lên. 5b. Bàn đang Cần dọn hoặc có lượt đặt sắp bắt đầu (BR-01): từ chối xếp bàn kèm lý do.
- **Điều kiện sau:** Nhóm khách đang chờ hoặc đã được xếp bàn.

### UC-06: Cập nhật trạng thái bàn
- **Tác nhân:** Phục vụ/Lễ tân
- **Điều kiện trước:** Phục vụ/Lễ tân đã đăng nhập và đang xem tổng quan khu phục vụ.
- **Luồng chính:** 1. Phục vụ/Lễ tân chọn một bàn Có khách mà khách đã rời đi và bấm Cần dọn. 2. Sau khi dọn, Phục vụ/Lễ tân chọn bàn và bấm Đã dọn. 3. Bàn chuyển sang Trống và, nếu có nhóm chờ vừa, nhóm đó được làm nổi bật (UC-05).
- **Luồng phụ:** 1a. Bàn vẫn còn đơn đang mở hoặc hóa đơn chưa trả: hệ thống từ chối và yêu cầu đóng hóa đơn trước (BR-05). 1b. Bước chuyển không được BR-05 cho phép thì không bao giờ được hiển thị.
- **Điều kiện sau:** Trạng thái bàn đi theo đúng vòng đời.

### UC-07: Quản lý thực đơn
- **Tác nhân:** Quản lý
- **Điều kiện trước:** Quản lý đã đăng nhập.
- **Luồng chính:** 1. Quản lý mở Quản lý thực đơn. 2. Thêm hoặc sửa món: tên, danh mục, lượt món, giá, mô tả. 3. Hệ thống kiểm tra dữ liệu và lưu. 4. Quản lý có thể ẩn một món.
- **Luồng phụ:** 3a. Tên trống hoặc giá ≤ 0: thông báo kiểm tra dữ liệu. 4a. Quản lý định xóa món đã có trong đơn cũ: hệ thống từ chối và đề nghị ẩn món, để lịch sử vẫn đúng.
- **Điều kiện sau:** Thực đơn trên màn hình gọi món được cập nhật.

### UC-08: Tạo đơn và gửi bếp
- **Tác nhân:** Phục vụ/Lễ tân (phụ: Nhân viên bếp)
- **Điều kiện trước:** Bàn ở trạng thái Có khách; Phục vụ/Lễ tân đã đăng nhập.
- **Luồng chính:** 1. Phục vụ/Lễ tân chọn bàn. 2. Hệ thống hiện thực đơn theo danh mục, không có món hết hàng. 3. Phục vụ/Lễ tân thêm món kèm số lượng và ghi chú. 4. Phục vụ/Lễ tân bấm Gửi. 5. Hệ thống tạo phiếu bếp theo đúng thứ tự lượt món (BR-06). 6. Phiếu xuất hiện trên màn hình bếp. 7. Hệ thống xác nhận đơn đã được gửi.
- **Luồng phụ:** 3a. Một món hết hàng ngay khi đang thêm: món bị gỡ kèm thông báo trước khi gửi (BR-07). 4a. Lưu thất bại: đơn vẫn chưa gửi, Phục vụ/Lễ tân được thông báo, và gửi lại không tạo phiếu trùng (NFR-07). 7a. Khách gọi thêm: món mới được gửi bằng phiếu mới; món đã Đang làm thì không được sửa (BR-08). 7b. Hóa đơn đã được tạo: đơn bị khóa (BR-09).
- **Điều kiện sau:** Đơn được lưu và phiếu của đơn nằm trên màn hình bếp theo thứ tự lượt món.

### UC-09: Xử lý phiếu bếp
- **Tác nhân:** Nhân viên bếp (phụ: Phục vụ/Lễ tân)
- **Điều kiện trước:** Nhân viên bếp đã đăng nhập; có phiếu đang chờ.
- **Luồng chính:** 1. Nhân viên bếp thấy các phiếu sắp theo lượt món và giờ gửi. 2. Chọn một món và chuyển sang Đang làm; từ lúc này món bị khóa với phục vụ (BR-08). 3. Khi món xong, chuyển sang Sẵn sàng. 4. Phục vụ/Lễ tân thấy món đã sẵn sàng.
- **Luồng phụ:** 3a. Món bị trả lại: Nhân viên bếp chuyển món về Đang làm kèm lý do; lịch sử trạng thái được lưu (ItemStatusChange).
- **Điều kiện sau:** Trạng thái món được cập nhật trên màn hình bếp và màn hình gọi món.

### UC-10: Đánh dấu món hết hàng hoặc có hàng trở lại
- **Tác nhân:** Nhân viên bếp, Quản lý
- **Điều kiện trước:** Người dùng đã đăng nhập.
- **Luồng chính:** 1. Người dùng chọn một món và bấm Hết hàng. 2. Hệ thống lưu thay đổi và trong vòng 5 giây gỡ món khỏi mọi màn hình gọi món đang mở (NFR-03). 3. Sau đó, người dùng bấm Có hàng trở lại và món xuất hiện lại.
- **Luồng phụ:** 2a. Món đã nằm trong các phiếu đã gửi: các phiếu đó không bị ảnh hưởng (BR-07). 2b. Một phục vụ định gửi món đó cùng lúc: dòng món đó bị từ chối kèm thông báo.
- **Điều kiện sau:** Chỉ món còn hàng mới được gọi.

### UC-11: Mở và đóng ca
- **Tác nhân:** Quản lý
- **Điều kiện trước:** Quản lý đã đăng nhập.
- **Luồng chính:** 1. Khi bắt đầu phục vụ, Quản lý bấm Mở ca; hệ thống ghi giờ bắt đầu. 2. Khi kết thúc phục vụ, Quản lý bấm Đóng ca. 3. Hệ thống kiểm tra không còn hóa đơn chưa trả, ghi giờ kết thúc và đưa ra báo cáo ca (UC-15).
- **Luồng phụ:** 1a. Đã có ca đang mở: từ chối mở ca (BR-13). 3a. Còn hóa đơn chưa trả: từ chối đóng ca và liệt kê các hóa đơn chưa trả.
- **Điều kiện sau:** Trong giờ phục vụ có đúng một ca mở, hoặc không có ca nào sau khi đóng.

### UC-12: Tạo hóa đơn
- **Tác nhân:** Phục vụ/Lễ tân
- **Điều kiện trước:** Bàn ở trạng thái Có khách và có đơn gọi món; có ca đang mở.
- **Luồng chính:** 1. Phục vụ/Lễ tân chọn bàn và bấm Tạo hóa đơn. 2. Hệ thống kiểm tra không còn món nào Mới hoặc Đang làm (BR-09). 3. Hệ thống sao chép các dòng đơn thành dòng hóa đơn, tính tổng và khóa đơn. 4. Hóa đơn được hiển thị với tổng tiền, số đã trả và số còn lại.
- **Luồng phụ:** 2a. Còn món đang chế biến: hệ thống từ chối và liệt kê các món đó. 1a. Bàn không có đơn: hệ thống từ chối. 4a. Khách muốn gọi thêm: Phục vụ/Lễ tân hủy hóa đơn chưa trả (hóa đơn được giữ lại với trạng thái Đã hủy), việc này chỉ được phép khi chưa ghi nhận khoản thanh toán nào, và đơn được mở khóa.
- **Điều kiện sau:** Bàn có một hóa đơn đang mở.

### UC-13: Chia hóa đơn
- **Tác nhân:** Phục vụ/Lễ tân
- **Điều kiện trước:** Hóa đơn đang mở và chưa có khoản thanh toán nào.
- **Luồng chính:** 1. Phục vụ/Lễ tân mở hóa đơn và bấm Chia. 2a. **Chia đều:** nhập số phần; hệ thống tính các phần theo BR-10. 2b. **Theo món:** gán mỗi dòng hóa đơn vào một phần. 3. Hệ thống hiện số tiền của từng phần; tổng các phần luôn bằng tổng hóa đơn.
- **Luồng phụ:** 2a-1. Số phần ngoài khoảng 2–20: thông báo kiểm tra dữ liệu. 2b-1. Có dòng chưa được gán hoặc bị gán hai lần: không thể xác nhận cách chia. 1a. Đã ghi nhận khoản thanh toán: từ chối chia hóa đơn.
- **Điều kiện sau:** Hóa đơn được chia thành các phần có thể thanh toán riêng.

### UC-14: Ghi nhận thanh toán và đóng hóa đơn
- **Tác nhân:** Phục vụ/Lễ tân
- **Điều kiện trước:** Hóa đơn (hoặc phần) đang mở; có ca đang mở.
- **Luồng chính:** 1. Phục vụ/Lễ tân mở hóa đơn hoặc phần. 2. Chọn phương thức (tiền mặt, thẻ, chuyển khoản) và nhập số tiền. 3. Hệ thống kiểm tra (BR-11) và, với tiền mặt, hiện tiền thối. 4. Hệ thống ghi nhận khoản thanh toán. 5. Khi các khoản thanh toán đủ tổng tiền, hệ thống đóng hóa đơn trong cùng giao dịch, chuyển hóa đơn sang chỉ xem (BR-12) và chuyển bàn sang Cần dọn (BR-05).
- **Luồng phụ:** 3a. Số tiền ≤ 0, hoặc số tiền thẻ hay chuyển khoản lớn hơn số còn lại: thông báo lỗi. 5a. Số tiền nhỏ hơn số còn lại: khoản thanh toán được ghi nhận và hóa đơn vẫn mở với số còn lại mới. 1a. Hóa đơn đã đóng: không thể thêm thanh toán. 4a. Lưu vào cơ sở dữ liệu thất bại: không có gì được ghi nhận và người dùng có thể thử lại (NFR-08). 1b. Không có ca đang mở: từ chối thanh toán (BR-13). (Sau buổi phỏng vấn, thêm ví điện tử, CR-04.)
- **Điều kiện sau:** Các khoản thanh toán được lưu; hóa đơn đã trả đủ được đóng.

### UC-15: Tạo báo cáo ca
- **Tác nhân:** Quản lý
- **Điều kiện trước:** Quản lý đã đăng nhập; có ít nhất một ca.
- **Luồng chính:** 1. Quản lý mở Báo cáo ca. 2. Chọn một ca. 3. Hệ thống tập hợp các hóa đơn và khoản thanh toán của ca. 4. Hệ thống hiển thị số hóa đơn đã đóng, tổng doanh thu, doanh thu theo phương thức thanh toán và số hóa đơn đã chia.
- **Luồng phụ:** 4a. Ca không có hóa đơn: báo cáo hiển thị doanh thu bằng không. 2a. Ca được chọn vẫn đang mở: báo cáo được ghi là tạm tính.
- **Điều kiện sau:** Quản lý thấy các số liệu của ca.
