package vn.edu.vlu.restaurant.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** Guest website: booking without login, linked to the staff reservations (BR-01, BR-02 apply). */
class PublicControllerTest extends ApiTestSupport {

    @Test
    @DisplayName("TC-RES-32 A guest books online without logging in; staff see the booking with an Online booking note")
    void guestBookingShowsUpForStaff() throws Exception {
        book(2, "19:00", "Window seat")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").isNotEmpty())
                .andExpect(jsonPath("$.guestName").value("Hoang Thi Mai"));

        mockMvc.perform(get("/api/reservations").param("date", tomorrow().toString())
                        .with(user("waiter").roles("WAITER_HOST")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].guestName").value("Hoang Thi Mai"))
                .andExpect(jsonPath("$[0].note").value("Online booking: Window seat"))
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("TC-RES-33 The online booking takes the smallest free table that fits, and refuses when none is left (BR-01, BR-02)")
    void smallestTableAndNoDoubleBooking() throws Exception {
        // the seed has a single table for 12 guests
        book(12, "19:00", null).andExpect(status().isCreated());
        book(12, "19:00", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("No table is free for this party at that time. Please choose another time."));

        mockMvc.perform(get("/api/public/slots").param("date", tomorrow().toString()).param("partySize", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.time == '19:00:00')].available").value(false))
                .andExpect(jsonPath("$[?(@.time == '17:00:00')].available").value(false))   // inside the 195-minute block
                .andExpect(jsonPath("$[?(@.time == '15:30:00')].available").value(true));   // 210 minutes earlier
    }

    @Test
    @DisplayName("TC-RES-34 Online booking input is validated like the staff form")
    void invalidInput() throws Exception {
        Map<String, Object> body = body(2, "19:00", null);
        body.put("phone", "12345");
        mockMvc.perform(post("/api/public/reservations").contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.phone").value("Phone number must have 10 digits and start with 0"));

        mockMvc.perform(get("/api/public/slots").param("date", tomorrow().toString()).param("partySize", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("The public menu needs no login, while staff data still does")
    void publicMenuOnly() throws Exception {
        mockMvc.perform(get("/api/public/menu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").isNotEmpty());
        mockMvc.perform(get("/api/reservations").param("date", tomorrow().toString()))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions book(int partySize, String time, String note) throws Exception {
        return mockMvc.perform(post("/api/public/reservations").contentType(MediaType.APPLICATION_JSON)
                .content(json(body(partySize, time, note))));
    }

    private Map<String, Object> body(int partySize, String time, String note) {
        Map<String, Object> body = new HashMap<>();
        body.put("guestName", "Hoang Thi Mai");
        body.put("phone", "0912345678");
        body.put("partySize", partySize);
        body.put("date", tomorrow().toString());
        body.put("startTime", time);
        body.put("note", note);
        return body;
    }
}
