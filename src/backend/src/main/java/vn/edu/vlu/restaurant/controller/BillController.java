package vn.edu.vlu.restaurant.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.vlu.restaurant.dto.BillRequest;
import vn.edu.vlu.restaurant.dto.BillResponse;
import vn.edu.vlu.restaurant.dto.EvenSplitRequest;
import vn.edu.vlu.restaurant.dto.ItemSplitRequest;
import vn.edu.vlu.restaurant.dto.PaymentRequest;
import vn.edu.vlu.restaurant.service.BillingService;

/** UC-12 Generate bill, UC-13 Split bill, UC-14 Record payment and close bill (FR-21 to FR-26). */
@RestController
@RequestMapping("/api/bills")
public class BillController {

    private final BillingService billingService;

    public BillController(BillingService billingService) {
        this.billingService = billingService;
    }

    @PostMapping
    @PreAuthorize(AccessRules.FRONT_OF_HOUSE)
    @ResponseStatus(HttpStatus.CREATED)
    public BillResponse generate(@Valid @RequestBody BillRequest request) {
        return billingService.generate(request.orderId());
    }

    /** The unpaid bill of a table; empty when it has none. */
    @GetMapping
    @PreAuthorize(AccessRules.FRONT_OF_HOUSE)
    public List<BillResponse> listOpenBills(@RequestParam Long tableId) {
        return billingService.listOpenBills(tableId);
    }

    @GetMapping("/{id}")
    @PreAuthorize(AccessRules.FRONT_OF_HOUSE)
    public BillResponse getBill(@PathVariable Long id) {
        return billingService.getBill(id);
    }

    /** UC-12 alternative 4a: only while nothing is paid (BR-09). */
    @PostMapping("/{id}/cancel")
    @PreAuthorize(AccessRules.FRONT_OF_HOUSE)
    public BillResponse cancel(@PathVariable Long id) {
        return billingService.cancel(id);
    }

    @PostMapping("/{id}/split/even")
    @PreAuthorize(AccessRules.FRONT_OF_HOUSE)
    public BillResponse splitEvenly(@PathVariable Long id, @Valid @RequestBody EvenSplitRequest request) {
        return billingService.splitEvenly(id, request.shares());
    }

    @PostMapping("/{id}/split/by-item")
    @PreAuthorize(AccessRules.FRONT_OF_HOUSE)
    public BillResponse splitByItem(@PathVariable Long id, @Valid @RequestBody ItemSplitRequest request) {
        return billingService.splitByItem(id, request.shares());
    }

    @DeleteMapping("/{id}/split")
    @PreAuthorize(AccessRules.FRONT_OF_HOUSE)
    public BillResponse removeSplit(@PathVariable Long id) {
        return billingService.removeSplit(id);
    }

    @PostMapping("/{id}/payments")
    @PreAuthorize(AccessRules.FRONT_OF_HOUSE)
    @ResponseStatus(HttpStatus.CREATED)
    public BillResponse pay(@PathVariable Long id, @Valid @RequestBody PaymentRequest request,
                            Authentication authentication) {
        return billingService.pay(id, request, authentication.getName());
    }
}
