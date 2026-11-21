package vn.edu.vlu.restaurant.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.vlu.restaurant.domain.Bill;
import vn.edu.vlu.restaurant.domain.BillStatus;
import vn.edu.vlu.restaurant.domain.PaymentMethod;
import vn.edu.vlu.restaurant.domain.Shift;
import vn.edu.vlu.restaurant.domain.SplitType;
import vn.edu.vlu.restaurant.domain.User;
import vn.edu.vlu.restaurant.dto.ShiftReportResponse;
import vn.edu.vlu.restaurant.dto.ShiftReportResponse.ReportBill;
import vn.edu.vlu.restaurant.dto.ShiftResponse;
import vn.edu.vlu.restaurant.exception.BusinessRuleException;
import vn.edu.vlu.restaurant.exception.NotFoundException;
import vn.edu.vlu.restaurant.repository.BillRepository;
import vn.edu.vlu.restaurant.repository.PaymentRepository;
import vn.edu.vlu.restaurant.repository.ShiftRepository;
import vn.edu.vlu.restaurant.repository.UserRepository;

/** Shifts of the Manager (UC-11, FR-27, BR-13) and the shift report (UC-15, FR-28, FR-29). */
@Service
@Transactional
public class ShiftService {

    private final ShiftRepository shiftRepository;
    private final BillRepository billRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public ShiftService(ShiftRepository shiftRepository, BillRepository billRepository,
                        PaymentRepository paymentRepository, UserRepository userRepository, Clock clock) {
        this.shiftRepository = shiftRepository;
        this.billRepository = billRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    /** BR-13: only one shift can be open. The unique key on shift.open_marker catches two clicks at once. */
    public ShiftResponse open(String username) {
        shiftRepository.findFirstByClosedAtIsNull().ifPresent(open -> {
            throw new BusinessRuleException("A shift is already open since " + open.getOpenedAt().toLocalTime().withNano(0)
                    + "; close it first (BR-13)");
        });
        Shift shift = shiftRepository.saveAndFlush(new Shift(user(username), LocalDateTime.now(clock)));
        return ShiftResponse.from(shift);
    }

    /** BR-13: a shift cannot close while a bill is unpaid; the message lists the tables (UC-11 alternative 3a). */
    public ShiftResponse close(Long shiftId, Long countedCash, String username) {
        Shift shift = shiftRepository.findOpenForUpdate()
                .filter(open -> open.getId().equals(shiftId))
                .orElseThrow(() -> shiftRepository.existsById(shiftId)
                        ? new BusinessRuleException("This shift is already closed")
                        : new NotFoundException("Shift not found"));
        List<Bill> unpaid = billRepository.findWithTableByStatus(BillStatus.OPEN);
        if (!unpaid.isEmpty()) {
            String tables = unpaid.stream().map(b -> "table " + b.getOrder().getTable().getNumber())
                    .collect(Collectors.joining(", "));
            throw new BusinessRuleException("The shift cannot be closed: unpaid bills at " + tables + " (BR-13)");
        }
        shift.close(user(username), LocalDateTime.now(clock), countedCash);
        return ShiftResponse.from(shift);
    }

    /** The open shift, or empty; the bill screen uses it to tell the waiter whether payments are possible. */
    @Transactional(readOnly = true)
    public ShiftResponse current() {
        return shiftRepository.findFirstByClosedAtIsNull().map(ShiftResponse::from).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<ShiftResponse> list() {
        return shiftRepository.findAllByOrderByOpenedAtDesc().stream().map(ShiftResponse::from).toList();
    }

    /**
     * FR-28 and FR-29. Two queries whatever the number of bills: the closed bills with their tables, and
     * the payment totals per method grouped by the database (NFR-04: 300 bills within 3 seconds).
     */
    @Transactional(readOnly = true)
    public ShiftReportResponse report(Long shiftId) {
        Shift shift = shiftRepository.findById(shiftId).orElseThrow(() -> new NotFoundException("Shift not found"));
        Map<PaymentMethod, Long> byMethod = new EnumMap<>(PaymentMethod.class);
        for (PaymentMethod method : PaymentMethod.values()) {
            byMethod.put(method, 0L);
        }
        int payments = 0;
        for (var total : paymentRepository.totalsByMethod(shiftId)) {
            byMethod.put(total.getMethod(), total.getAmount());
            payments += total.getPayments().intValue();
        }
        long revenue = byMethod.values().stream().mapToLong(Long::longValue).sum();
        List<ReportBill> bills = billRepository.findClosedInShift(shiftId).stream()
                .map(b -> new ReportBill(b.getId(), b.getOrder().getTable().getNumber(), b.getTotal(),
                        b.getSplitType(), b.getClosedAt()))
                .toList();
        int splitBills = (int) bills.stream().filter(b -> b.splitType() != SplitType.NONE).count();
        long expectedCash = byMethod.get(PaymentMethod.CASH);
        Long counted = shift.getCountedCash();
        return new ShiftReportResponse(ShiftResponse.from(shift), shift.isOpen(), bills.size(), revenue, byMethod,
                payments, splitBills, expectedCash, counted, counted == null ? null : counted - expectedCash, bills);
    }

    private User user(String username) {
        return userRepository.findByUsername(username).orElseThrow(() -> new NotFoundException("User not found"));
    }
}
