package vn.edu.vlu.restaurant.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import vn.edu.vlu.restaurant.domain.CustomerOrder;
import vn.edu.vlu.restaurant.domain.DiningTable;
import vn.edu.vlu.restaurant.domain.Reservation;
import vn.edu.vlu.restaurant.domain.ReservationStatus;
import vn.edu.vlu.restaurant.domain.TableStatus;
import vn.edu.vlu.restaurant.repository.CustomerOrderRepository;
import vn.edu.vlu.restaurant.repository.DiningTableRepository;
import vn.edu.vlu.restaurant.repository.ReservationRepository;
import vn.edu.vlu.restaurant.repository.UserRepository;
import vn.edu.vlu.restaurant.service.ReservationService;

/** Week 7 business rules: BR-01 no double booking, BR-03 clean before reuse, BR-04 waiting list, BR-05 lifecycle. */
@WithMockUser(roles = "WAITER_HOST")
class TableLifecycleTest extends ApiTestSupport {

    @Autowired
    private DiningTableRepository tableRepository;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private CustomerOrderRepository orderRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ReservationService reservationService;
    @Autowired
    private Clock clock;

    @Test
    @DisplayName("TC-RES-17 A second booking inside the 195-minute block of the same table is refused (BR-01)")
    void noDoubleBooking() throws Exception {
        DiningTable table = tableWithCapacity(4);
        book(table, tomorrowAt("19:00")).andExpect(status().isCreated());

        book(table, tomorrowAt("22:14"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Table " + table.getNumber()
                        + " is already booked at 19:00; choose another time or table"));

        // 19:00 + 180 dining + 15 turn = 22:15: the next booking may start exactly then
        book(table, tomorrowAt("22:15")).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("TC-RES-18 The availability search leaves out a table that is already booked at that time")
    void searchSkipsBookedTable() throws Exception {
        DiningTable table = tableWithCapacity(4);
        book(table, tomorrowAt("19:00")).andExpect(status().isCreated());

        String response = mockMvc.perform(get("/api/reservations/available-tables")
                        .param("date", tomorrow().toString()).param("startTime", "20:00").param("partySize", "2"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        for (JsonNode t : objectMapper.readTree(response)) {
            org.junit.jupiter.api.Assertions.assertNotEquals(table.getId().longValue(), t.get("id").asLong());
        }
    }

    @Test
    @DisplayName("TC-RES-19 A booking due within 30 minutes holds the table; check-in makes it Occupied (BR-05)")
    void checkInOccupiesTable() throws Exception {
        DiningTable table = tableWithCapacity(4);
        long id = createdId(book(table, soon(20)));
        assertEquals(TableStatus.RESERVED, table.getStatus());

        mockMvc.perform(post("/api/reservations/{id}/check-in", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHECKED_IN"));
        assertEquals(TableStatus.OCCUPIED, table.getStatus());
    }

    @Test
    @DisplayName("TC-RES-20 Guests cannot be seated at a table that still needs cleaning (BR-03)")
    void checkInRefusedWhileTableNeedsCleaning() throws Exception {
        DiningTable table = tableWithCapacity(4);
        long id = createdId(book(table, soon(20)));
        table.changeStatus(TableStatus.OCCUPIED);
        table.changeStatus(TableStatus.NEEDS_CLEANING);

        mockMvc.perform(post("/api/reservations/{id}/check-in", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Table " + table.getNumber() + " must be cleaned before the guests sit down"));
    }

    @Test
    @DisplayName("TC-RES-21 A walk-in is refused at a table booked within the next 195 minutes (BR-01)")
    void walkInRefusedBeforeABooking() throws Exception {
        DiningTable table = tableWithCapacity(4);
        LocalDateTime start = soon(60);
        book(table, start).andExpect(status().isCreated());

        mockMvc.perform(post("/api/tables/{id}/seat-walk-in", table.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"partySize\":2}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Table " + table.getNumber() + " has a reservation at "
                        + start.toLocalTime() + "; a walk-in would overlap it"));
    }

    @Test
    @DisplayName("TC-RES-22 Seat next gives the table to the earliest waiting party that fits (BR-04)")
    void seatNextTakesEarliestPartyThatFits() throws Exception {
        DiningTable table = tableWithCapacity(4);
        addParty("Large Group", 6);
        addParty("Vo Thanh Tam", 2);
        addParty("Dang Kim Lien", 3);

        mockMvc.perform(post("/api/waitlist/seat-next").param("tableId", table.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.partyName").value("Vo Thanh Tam"))
                .andExpect(jsonPath("$.tableNumber").value(table.getNumber()));
        assertEquals(TableStatus.OCCUPIED, table.getStatus());

        mockMvc.perform(get("/api/waitlist"))
                .andExpect(jsonPath("$[0].partyName").value("Large Group"))
                .andExpect(jsonPath("$[1].partyName").value("Dang Kim Lien"))
                .andExpect(jsonPath("$[1].position").value(2));
    }

    @Test
    @DisplayName("TC-RES-23 A table with an open order cannot be marked Needs cleaning; after the bill it can, then Available")
    void cleaningCycle() throws Exception {
        DiningTable table = tableWithCapacity(2);
        mockMvc.perform(post("/api/tables/{id}/seat-walk-in", table.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"partySize\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OCCUPIED"));
        CustomerOrder order = orderRepository.save(new CustomerOrder(table,
                userRepository.findByUsername("waiter").orElseThrow(), LocalDateTime.now(clock)));

        mockMvc.perform(post("/api/tables/{id}/needs-cleaning", table.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Table " + table.getNumber() + " still has an open order; close its bill first"));

        order.close();
        mockMvc.perform(post("/api/tables/{id}/needs-cleaning", table.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NEEDS_CLEANING"));
        mockMvc.perform(post("/api/tables/{id}/clean", table.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    @DisplayName("TC-RES-24 An Occupied table cannot skip cleaning and go straight to Available (BR-05)")
    void occupiedCannotBeMarkedClean() throws Exception {
        DiningTable table = tableWithCapacity(2);
        table.changeStatus(TableStatus.OCCUPIED);

        mockMvc.perform(post("/api/tables/{id}/clean", table.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Table " + table.getNumber() + " cannot go from Occupied to Available"));
    }

    @Test
    @WithMockUser(roles = "KITCHEN_STAFF")
    @DisplayName("TC-RES-25 Kitchen Staff cannot check guests in or seat walk-ins (NFR-03)")
    void kitchenStaffIsForbidden() throws Exception {
        DiningTable table = tableWithCapacity(2);
        mockMvc.perform(post("/api/reservations/{id}/check-in", 1)).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/tables/{id}/seat-walk-in", table.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"partySize\":2}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC-RES-26 The scheduler marks a booking 45 minutes late as No-show and frees its table (FR-06)")
    void refreshMarksNoShowAndReservesUpcoming() {
        LocalDateTime now = LocalDateTime.of(2030, 1, 15, 19, 0);
        DiningTable late = tableWithCapacity(4);
        DiningTable upcoming = tableWithCapacity(2);
        Reservation lateBooking = reservationRepository.save(new Reservation("RLATE234", late, "Bui Quang Huy",
                "0901112233", 2, now.minusMinutes(45), null));
        late.changeStatus(TableStatus.RESERVED);
        Reservation upcomingBooking = reservationRepository.save(new Reservation("RSOON234", upcoming, "Ho Ngoc Mai",
                "0904445566", 2, now.plusMinutes(30), null));

        reservationService.refreshStatuses(now);

        assertEquals(ReservationStatus.NO_SHOW, lateBooking.getStatus());
        assertEquals(TableStatus.AVAILABLE, late.getStatus());
        assertEquals(ReservationStatus.CONFIRMED, upcomingBooking.getStatus());
        assertEquals(TableStatus.RESERVED, upcoming.getStatus());
    }

    private DiningTable tableWithCapacity(int capacity) {
        return tableRepository.findAllByOrderByNumberAsc().stream()
                .filter(t -> t.getCapacity() == capacity && t.isActive() && t.getStatus() == TableStatus.AVAILABLE)
                .findFirst()
                .orElseThrow();
    }

    private LocalDateTime tomorrowAt(String time) {
        return tomorrow().atTime(java.time.LocalTime.parse(time));
    }

    /** A start time the given number of minutes from now, on whole minutes. */
    private LocalDateTime soon(int minutes) {
        return LocalDateTime.now(clock).plusMinutes(minutes).truncatedTo(ChronoUnit.MINUTES);
    }

    private org.springframework.test.web.servlet.ResultActions book(DiningTable table, LocalDateTime start) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("tableId", table.getId());
        body.put("guestName", "Tran Van Binh");
        body.put("phone", "0912345678");
        body.put("partySize", 2);
        body.put("date", start.toLocalDate().toString());
        body.put("startTime", start.toLocalTime().toString());
        return mockMvc.perform(post("/api/reservations").contentType(MediaType.APPLICATION_JSON).content(json(body)));
    }

    private long createdId(org.springframework.test.web.servlet.ResultActions result) throws Exception {
        String response = result.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private void addParty(String name, int size) throws Exception {
        mockMvc.perform(post("/api/waitlist").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("partyName", name, "phone", "0987654321", "partySize", size))))
                .andExpect(status().isCreated());
    }
}
