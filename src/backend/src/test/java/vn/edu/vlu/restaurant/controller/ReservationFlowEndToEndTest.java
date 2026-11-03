package vn.edu.vlu.restaurant.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import vn.edu.vlu.restaurant.domain.DiningTable;
import vn.edu.vlu.restaurant.domain.TableStatus;
import vn.edu.vlu.restaurant.repository.DiningTableRepository;

/**
 * Week 9 end-to-end test: one evening of the Table & Reservation module through the real API,
 * with a real login session instead of a mock user (the same calls the React pages make).
 */
class ReservationFlowEndToEndTest extends ApiTestSupport {

    @Autowired
    private DiningTableRepository tableRepository;
    @Autowired
    private Clock clock;

    @Test
    @DisplayName("TC-RES-28 Book, check in, guests leave, clean, then seat the next waiting party on the same table")
    void wholeVisitOnOneTable() throws Exception {
        MockHttpSession waiter = login("waiter", "Waiter@2026");
        DiningTable table = tableRepository.findAllByOrderByNumberAsc().stream()
                .filter(t -> t.getCapacity() == 4 && t.getStatus() == TableStatus.AVAILABLE)
                .findFirst().orElseThrow();
        String path = "/api/tables/" + table.getId();

        // 1. Booking 20 minutes from now: the table is held (Reserved)
        LocalDateTime start = LocalDateTime.now(clock).plusMinutes(20).truncatedTo(ChronoUnit.MINUTES);
        String created = mockMvc.perform(post("/api/reservations").session(waiter)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("tableId", table.getId(), "guestName", "Nguyen Thi Lan",
                                "phone", "0912345678", "partySize", 4,
                                "date", start.toLocalDate().toString(), "startTime", start.toLocalTime().toString()))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long reservationId = objectMapper.readTree(created).get("id").asLong();
        expectTableStatus(waiter, table, "RESERVED");

        // 2. A walk-in cannot take the held table
        mockMvc.perform(post(path + "/seat-walk-in").session(waiter)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"partySize\":2}"))
                .andExpect(status().isConflict());

        // 3. The guests arrive
        mockMvc.perform(post("/api/reservations/{id}/check-in", reservationId).session(waiter))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHECKED_IN"));
        expectTableStatus(waiter, table, "OCCUPIED");

        // 4. Meanwhile a walk-in party joins the waiting list
        mockMvc.perform(post("/api/waitlist").session(waiter).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("partyName", "Pham Thu Ha", "phone", "0938111222", "partySize", 3))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.position").value(1));

        // 5. The guests leave; the table cannot be given away before it is cleaned (BR-03)
        mockMvc.perform(post(path + "/needs-cleaning").session(waiter))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NEEDS_CLEANING"));
        mockMvc.perform(post("/api/waitlist/seat-next").session(waiter).param("tableId", table.getId().toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Table " + table.getNumber() + " must be cleaned before the guests sit down"));

        // 6. Cleaned, then the waiting party is seated and leaves the list
        mockMvc.perform(post(path + "/clean").session(waiter))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
        mockMvc.perform(post("/api/waitlist/seat-next").session(waiter).param("tableId", table.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.partyName").value("Pham Thu Ha"));
        expectTableStatus(waiter, table, "OCCUPIED");
        mockMvc.perform(get("/api/waitlist").session(waiter))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("TC-RES-29 After logout the same session can no longer change a table")
    void logoutEndsAccess() throws Exception {
        MockHttpSession waiter = login("waiter", "Waiter@2026");
        mockMvc.perform(post("/api/auth/logout").session(waiter)).andExpect(status().isNoContent());
        mockMvc.perform(post("/api/tables/1/clean").session(waiter)).andExpect(status().isUnauthorized());
    }

    private MockHttpSession login(String username, String password) throws Exception {
        return (MockHttpSession) mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
    }

    private void expectTableStatus(MockHttpSession session, DiningTable table, String expected) throws Exception {
        mockMvc.perform(get("/api/tables").session(session))
                .andExpect(jsonPath("$[?(@.id == " + table.getId() + ")].status").value(expected));
    }
}
