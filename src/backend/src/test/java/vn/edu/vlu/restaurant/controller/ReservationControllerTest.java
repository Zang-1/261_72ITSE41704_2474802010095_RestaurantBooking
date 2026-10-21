package vn.edu.vlu.restaurant.controller;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import vn.edu.vlu.restaurant.domain.DiningTable;
import vn.edu.vlu.restaurant.repository.DiningTableRepository;

/** UC-03 Create reservation and UC-04 Modify or cancel a reservation (FR-03 to FR-05). */
@WithMockUser(roles = "WAITER_HOST")
class ReservationControllerTest extends ApiTestSupport {

    @Autowired
    private DiningTableRepository tableRepository;

    @Test
    @DisplayName("TC-RES-09 A reservation for a past date is rejected")
    void reservationInThePast() throws Exception {
        Map<String, Object> body = validReservation(tableWithCapacity(4));
        body.put("date", LocalDate.now(RESTAURANT_ZONE).minusDays(1).toString());

        mockMvc.perform(post("/api/reservations").contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.date").value("Date cannot be in the past"));
    }

    @Test
    @DisplayName("TC-RES-10 A party larger than the chosen table is rejected (BR-02)")
    void partyLargerThanTable() throws Exception {
        DiningTable table = tableWithCapacity(2);
        Map<String, Object> body = validReservation(table);
        body.put("partySize", 5);

        mockMvc.perform(post("/api/reservations").contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.partySize")
                        .value("Table " + table.getNumber() + " seats only 2 guests"));
    }

    @Test
    @DisplayName("TC-RES-11 An invalid phone number is rejected")
    void invalidPhone() throws Exception {
        Map<String, Object> body = validReservation(tableWithCapacity(4));
        body.put("phone", "12345");

        mockMvc.perform(post("/api/reservations").contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.phone")
                        .value("Phone number must have 10 digits and start with 0"));
    }

    @Test
    @DisplayName("TC-RES-12 A valid reservation is saved as Confirmed with a reservation code")
    void createValidReservation() throws Exception {
        mockMvc.perform(post("/api/reservations").contentType(MediaType.APPLICATION_JSON)
                        .content(json(validReservation(tableWithCapacity(4)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.code").value(matchesPattern("R[A-Z2-9]{7}")))
                .andExpect(jsonPath("$.guestName").value("Tran Van Binh"));

        mockMvc.perform(get("/api/reservations").param("date", tomorrow().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].guestName").value("Tran Van Binh"));
    }

    @Test
    @DisplayName("TC-RES-13 Cancelling a future reservation sets it to Cancelled")
    void cancelReservation() throws Exception {
        String response = mockMvc.perform(post("/api/reservations").contentType(MediaType.APPLICATION_JSON)
                        .content(json(validReservation(tableWithCapacity(4)))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(post("/api/reservations/{id}/cancel", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // A cancelled reservation cannot be cancelled again
        mockMvc.perform(post("/api/reservations/{id}/cancel", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Only a confirmed reservation can be changed"));
    }

    @Test
    @DisplayName("TC-RES-14 Searching for a party larger than any table is rejected")
    void searchPartyLargerThanAnyTable() throws Exception {
        int largest = tableRepository.findLargestActiveCapacity().orElseThrow(); // 12 in the seed data
        mockMvc.perform(get("/api/reservations/available-tables")
                        .param("date", tomorrow().toString())
                        .param("startTime", "19:00")
                        .param("partySize", String.valueOf(largest + 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.partySize")
                        .value("No table seats more than " + largest + " guests"));
    }

    @Test
    @DisplayName("The availability search only returns tables that seat the party")
    void searchReturnsOnlyTablesThatFit() throws Exception {
        String response = mockMvc.perform(get("/api/reservations/available-tables")
                        .param("date", tomorrow().toString())
                        .param("startTime", "19:00")
                        .param("partySize", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].capacity", everyItem(greaterThanOrEqualTo(5))))
                .andReturn().getResponse().getContentAsString();
        JsonNode tables = objectMapper.readTree(response);
        org.junit.jupiter.api.Assertions.assertTrue(tables.size() > 0);
    }

    @Test
    @DisplayName("A badly formatted start time in the search gives a field message, not an error page")
    void searchWithInvalidTime() throws Exception {
        mockMvc.perform(get("/api/reservations/available-tables")
                        .param("date", tomorrow().toString())
                        .param("startTime", "7pm")
                        .param("partySize", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.startTime").value("Invalid value"));
    }

    @Test
    @DisplayName("Default timings match the restaurant interview: 180-minute dining, 15-minute turn-time (CR-02)")
    void defaultReservationSettings() throws Exception {
        mockMvc.perform(get("/api/settings/reservation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diningMinutes").value(180))
                .andExpect(jsonPath("$.turnMinutes").value(15));
    }

    private DiningTable tableWithCapacity(int capacity) {
        return tableRepository.findAllByOrderByNumberAsc().stream()
                .filter(t -> t.getCapacity() == capacity && t.isActive())
                .findFirst()
                .orElseThrow();
    }

    private Map<String, Object> validReservation(DiningTable table) {
        Map<String, Object> body = new HashMap<>();
        body.put("tableId", table.getId());
        body.put("guestName", "Tran Van Binh");
        body.put("phone", "0912345678");
        body.put("partySize", 2);
        body.put("date", tomorrow().toString());
        body.put("startTime", "19:00");
        body.put("note", "Birthday, window seat if possible");
        return body;
    }
}
