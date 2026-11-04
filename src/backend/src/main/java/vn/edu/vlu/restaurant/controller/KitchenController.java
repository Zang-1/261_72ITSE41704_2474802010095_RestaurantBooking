package vn.edu.vlu.restaurant.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.vlu.restaurant.dto.KitchenTicketItemResponse;
import vn.edu.vlu.restaurant.dto.KitchenTicketResponse;
import vn.edu.vlu.restaurant.dto.SendBackRequest;
import vn.edu.vlu.restaurant.service.KitchenService;

/**
 * UC-09 Process kitchen ticket (FR-15). The kitchen display polls GET /tickets every 2 seconds, so a
 * sent order appears within 3 seconds (NFR-02).
 */
@RestController
@RequestMapping("/api/kitchen")
public class KitchenController {

    private final KitchenService kitchenService;

    public KitchenController(KitchenService kitchenService) {
        this.kitchenService = kitchenService;
    }

    @GetMapping("/tickets")
    @PreAuthorize(AccessRules.KITCHEN)
    public List<KitchenTicketResponse> listActiveTickets() {
        return kitchenService.listActiveTickets();
    }

    @PostMapping("/items/{id}/start")
    @PreAuthorize(AccessRules.KITCHEN)
    public KitchenTicketItemResponse start(@PathVariable Long id) {
        return kitchenService.start(id);
    }

    @PostMapping("/items/{id}/ready")
    @PreAuthorize(AccessRules.KITCHEN)
    public KitchenTicketItemResponse markReady(@PathVariable Long id) {
        return kitchenService.markReady(id);
    }

    @PostMapping("/items/{id}/send-back")
    @PreAuthorize(AccessRules.KITCHEN)
    public KitchenTicketItemResponse sendBack(@PathVariable Long id, @Valid @RequestBody SendBackRequest request) {
        return kitchenService.sendBack(id, request.reason());
    }
}
