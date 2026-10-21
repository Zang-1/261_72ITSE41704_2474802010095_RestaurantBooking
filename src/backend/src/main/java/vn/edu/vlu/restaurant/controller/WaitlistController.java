package vn.edu.vlu.restaurant.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.vlu.restaurant.dto.WaitlistEntryResponse;
import vn.edu.vlu.restaurant.dto.WaitlistRequest;
import vn.edu.vlu.restaurant.service.WaitlistService;

/** Walk-in waiting list (UC-05). */
@RestController
@RequestMapping("/api/waitlist")
@PreAuthorize(AccessRules.FRONT_OF_HOUSE)
public class WaitlistController {

    private final WaitlistService waitlistService;

    public WaitlistController(WaitlistService waitlistService) {
        this.waitlistService = waitlistService;
    }

    @GetMapping
    public List<WaitlistEntryResponse> listWaiting() {
        return waitlistService.listWaiting();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WaitlistEntryResponse add(@Valid @RequestBody WaitlistRequest request) {
        return waitlistService.add(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long id) {
        waitlistService.remove(id);
    }
}
