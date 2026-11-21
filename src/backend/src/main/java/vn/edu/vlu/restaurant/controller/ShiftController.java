package vn.edu.vlu.restaurant.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.vlu.restaurant.dto.CloseShiftRequest;
import vn.edu.vlu.restaurant.dto.ShiftReportResponse;
import vn.edu.vlu.restaurant.dto.ShiftResponse;
import vn.edu.vlu.restaurant.service.ShiftService;

/** UC-11 Open and close shift (FR-27, FR-29) and UC-15 Generate shift report (FR-28). Manager only, except "current". */
@RestController
@RequestMapping("/api/shifts")
public class ShiftController {

    private final ShiftService shiftService;

    public ShiftController(ShiftService shiftService) {
        this.shiftService = shiftService;
    }

    @GetMapping
    @PreAuthorize(AccessRules.MANAGER)
    public List<ShiftResponse> list() {
        return shiftService.list();
    }

    /** The open shift, or 204 when none is open. Waiters need it to know whether they can take payments. */
    @GetMapping("/current")
    @PreAuthorize(AccessRules.FRONT_OF_HOUSE)
    public ResponseEntity<ShiftResponse> current() {
        ShiftResponse current = shiftService.current();
        return current == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(current);
    }

    @PostMapping
    @PreAuthorize(AccessRules.MANAGER)
    @ResponseStatus(HttpStatus.CREATED)
    public ShiftResponse open(Authentication authentication) {
        return shiftService.open(authentication.getName());
    }

    /** The body is optional; countedCash is the cash counted in the till (FR-29). */
    @PostMapping("/{id}/close")
    @PreAuthorize(AccessRules.MANAGER)
    public ShiftResponse close(@PathVariable Long id, @Valid @RequestBody(required = false) CloseShiftRequest request,
                               Authentication authentication) {
        return shiftService.close(id, request == null ? null : request.countedCash(), authentication.getName());
    }

    @GetMapping("/{id}/report")
    @PreAuthorize(AccessRules.MANAGER)
    public ShiftReportResponse report(@PathVariable Long id) {
        return shiftService.report(id);
    }
}
