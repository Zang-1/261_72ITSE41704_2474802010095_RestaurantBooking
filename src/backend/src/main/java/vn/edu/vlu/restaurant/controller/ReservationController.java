package vn.edu.vlu.restaurant.controller;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.vlu.restaurant.dto.AvailabilityQuery;
import vn.edu.vlu.restaurant.dto.ReservationRequest;
import vn.edu.vlu.restaurant.dto.ReservationResponse;
import vn.edu.vlu.restaurant.dto.ReservationSettingsDto;
import vn.edu.vlu.restaurant.dto.TableResponse;
import vn.edu.vlu.restaurant.service.ReservationService;

/** Create, list, modify and cancel reservations (UC-03, UC-04) and the reservation settings (UC-02). */
@RestController
@RequestMapping("/api")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    /** GET /api/reservations/available-tables?date=2026-10-24&startTime=19:00&partySize=4 */
    @GetMapping("/reservations/available-tables")
    @PreAuthorize(AccessRules.FRONT_OF_HOUSE)
    public List<TableResponse> searchAvailableTables(@Valid AvailabilityQuery query) {
        return reservationService.searchAvailableTables(query);
    }

    @GetMapping("/reservations")
    @PreAuthorize(AccessRules.FRONT_OF_HOUSE)
    public List<ReservationResponse> listForDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return reservationService.listForDate(date);
    }

    @PostMapping("/reservations")
    @PreAuthorize(AccessRules.FRONT_OF_HOUSE)
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse create(@Valid @RequestBody ReservationRequest request) {
        return reservationService.create(request);
    }

    @PutMapping("/reservations/{id}")
    @PreAuthorize(AccessRules.FRONT_OF_HOUSE)
    public ReservationResponse modify(@PathVariable Long id, @Valid @RequestBody ReservationRequest request) {
        return reservationService.modify(id, request);
    }

    @PostMapping("/reservations/{id}/cancel")
    @PreAuthorize(AccessRules.FRONT_OF_HOUSE)
    public ReservationResponse cancel(@PathVariable Long id) {
        return reservationService.cancel(id);
    }

    @GetMapping("/settings/reservation")
    @PreAuthorize(AccessRules.FRONT_OF_HOUSE)
    public ReservationSettingsDto getSettings() {
        return reservationService.getSettings();
    }

    @PutMapping("/settings/reservation")
    @PreAuthorize(AccessRules.MANAGER)
    public ReservationSettingsDto updateSettings(@Valid @RequestBody ReservationSettingsDto request) {
        return reservationService.updateSettings(request);
    }
}
