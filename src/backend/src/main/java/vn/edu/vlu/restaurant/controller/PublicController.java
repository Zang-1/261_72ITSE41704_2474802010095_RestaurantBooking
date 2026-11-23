package vn.edu.vlu.restaurant.controller;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.vlu.restaurant.dto.MenuItemResponse;
import vn.edu.vlu.restaurant.dto.OnlineBookingRequest;
import vn.edu.vlu.restaurant.dto.OnlineBookingResponse;
import vn.edu.vlu.restaurant.service.MenuService;
import vn.edu.vlu.restaurant.service.OnlineBookingService;
import vn.edu.vlu.restaurant.service.OnlineBookingService.Slot;

/** The guest website: no login (SecurityConfig permits /api/public/**). Only reads the menu and makes bookings. */
@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final MenuService menuService;
    private final OnlineBookingService bookingService;

    public PublicController(MenuService menuService, OnlineBookingService bookingService) {
        this.menuService = menuService;
        this.bookingService = bookingService;
    }

    /** Dishes on the menu and in stock. */
    @GetMapping("/menu")
    public List<MenuItemResponse> menu() {
        return menuService.listItems(false, true);
    }

    /** GET /api/public/slots?date=2026-11-25&partySize=4 */
    @GetMapping("/slots")
    public List<Slot> slots(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                            @RequestParam int partySize) {
        return bookingService.slots(date, partySize);
    }

    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public OnlineBookingResponse book(@Valid @RequestBody OnlineBookingRequest request) {
        return bookingService.book(request);
    }
}
