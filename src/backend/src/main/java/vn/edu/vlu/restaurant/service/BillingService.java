package vn.edu.vlu.restaurant.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.vlu.restaurant.domain.Bill;
import vn.edu.vlu.restaurant.domain.BillLine;
import vn.edu.vlu.restaurant.domain.BillShare;
import vn.edu.vlu.restaurant.domain.BillStatus;
import vn.edu.vlu.restaurant.domain.CustomerOrder;
import vn.edu.vlu.restaurant.domain.Shift;
import vn.edu.vlu.restaurant.domain.User;
import vn.edu.vlu.restaurant.dto.BillResponse;
import vn.edu.vlu.restaurant.dto.PaymentRequest;
import vn.edu.vlu.restaurant.exception.BusinessRuleException;
import vn.edu.vlu.restaurant.exception.InvalidInputException;
import vn.edu.vlu.restaurant.exception.NotFoundException;
import vn.edu.vlu.restaurant.repository.BillRepository;
import vn.edu.vlu.restaurant.repository.CustomerOrderRepository;
import vn.edu.vlu.restaurant.repository.ShiftRepository;
import vn.edu.vlu.restaurant.repository.UserRepository;

/**
 * Bills of the Waiter/Host: generate (UC-12), split (UC-13), pay and close (UC-14).
 * Locks are always taken in the same order, open shift → order or bill → table, so two requests cannot
 * deadlock. Each method is one transaction: the last payment, closing the bill, closing the order and
 * freeing the table are saved together or not at all (NFR-08).
 */
@Service
@Transactional
public class BillingService {

    private final BillRepository billRepository;
    private final CustomerOrderRepository orderRepository;
    private final ShiftRepository shiftRepository;
    private final UserRepository userRepository;
    private final TableService tableService;
    private final Clock clock;

    public BillingService(BillRepository billRepository, CustomerOrderRepository orderRepository,
                          ShiftRepository shiftRepository, UserRepository userRepository,
                          TableService tableService, Clock clock) {
        this.billRepository = billRepository;
        this.orderRepository = orderRepository;
        this.shiftRepository = shiftRepository;
        this.userRepository = userRepository;
        this.tableService = tableService;
        this.clock = clock;
    }

    /**
     * FR-21: generates the bill of an order once the kitchen is done (BR-09). A shift must be open
     * (UC-12 precondition); the shift row is locked so the Manager cannot close it at the same moment.
     */
    public BillResponse generate(Long orderId) {
        requireOpenShift();
        CustomerOrder order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new InvalidInputException("orderId", "Order not found"));
        if (billRepository.existsByOrderIdAndStatusIn(orderId, EnumSet.of(BillStatus.OPEN, BillStatus.CLOSED))) {
            throw new BusinessRuleException("Table " + order.getTable().getNumber() + " already has a bill");
        }
        Bill bill = billRepository.save(Bill.generate(order, LocalDateTime.now(clock)));
        billRepository.flush();  // gives the lines their ids for the response
        return BillResponse.from(bill);
    }

    /** The unpaid bill of a table, for the bill screen; empty when there is none. */
    @Transactional(readOnly = true)
    public List<BillResponse> listOpenBills(Long tableId) {
        return billRepository.findByOrderTableIdAndStatusOrderByCreatedAtDesc(tableId, BillStatus.OPEN).stream()
                .map(BillResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public BillResponse getBill(Long id) {
        return BillResponse.from(billRepository.findById(id).orElseThrow(() -> new NotFoundException("Bill not found")));
    }

    /** UC-12 alternative 4a: cancel an unpaid bill so the guests can order more (BR-09). */
    public BillResponse cancel(Long billId) {
        Bill bill = findForUpdate(billId);
        bill.cancel();
        return BillResponse.from(bill);
    }

    /** FR-23, BR-10. */
    public BillResponse splitEvenly(Long billId, int shares) {
        Bill bill = findForUpdate(billId);
        dropOldSplit(bill);
        bill.splitEvenly(shares);
        billRepository.flush();
        return BillResponse.from(bill);
    }

    /** FR-24: groups.get(i) holds the ids of the bill lines that share i + 1 pays for. */
    public BillResponse splitByItem(Long billId, List<List<Long>> groups) {
        Bill bill = findForUpdate(billId);
        List<List<BillLine>> lines = groups.stream()
                .map(group -> group.stream()
                        .map(id -> bill.findLine(id)
                                .orElseThrow(() -> new InvalidInputException("shares", "A dish does not belong to this bill")))
                        .toList())
                .toList();
        dropOldSplit(bill);
        bill.splitByItem(lines);
        billRepository.flush();
        return BillResponse.from(bill);
    }

    public BillResponse removeSplit(Long billId) {
        Bill bill = findForUpdate(billId);
        bill.removeSplit();
        return BillResponse.from(bill);
    }

    /**
     * FR-25, FR-26: records a payment and, when it completes the bill, closes the bill, the order and
     * sets the table to Needs cleaning, all in this one transaction (NFR-08). The open shift, the bill
     * and the table rows are locked in that order (BR-13: the shift cannot close in between).
     */
    public BillResponse pay(Long billId, PaymentRequest request, String username) {
        Shift shift = shiftRepository.findOpenForUpdate().orElse(null);
        Bill bill = findForUpdate(billId);
        tableService.lockTable(bill.getOrder().getTable().getId());
        BillShare share = null;
        if (request.shareId() != null) {
            share = bill.findShare(request.shareId())
                    .orElseThrow(() -> new InvalidInputException("shareId", "The share does not belong to this bill"));
        }
        User user = userRepository.findByUsername(username).orElseThrow(() -> new NotFoundException("User not found"));
        bill.pay(share, request.method(), request.amount(), shift, user, LocalDateTime.now(clock));
        billRepository.flush();  // a database error here rolls everything back and is reported, never half saved
        return BillResponse.from(bill);
    }

    /**
     * Shares are numbered 1..N and unique per bill. Hibernate inserts before it deletes, so the old
     * shares are deleted first, otherwise a new share 1 would clash with the old share 1.
     */
    private void dropOldSplit(Bill bill) {
        if (bill.isSplit()) {
            bill.removeSplit();
            billRepository.flush();
        }
    }

    private void requireOpenShift() {
        shiftRepository.findOpenForUpdate().orElseThrow(() -> new BusinessRuleException(
                "No shift is open; ask the Manager to open a shift first (BR-13)"));
    }

    private Bill findForUpdate(Long id) {
        return billRepository.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("Bill not found"));
    }
}
