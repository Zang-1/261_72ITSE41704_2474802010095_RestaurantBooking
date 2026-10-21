package vn.edu.vlu.restaurant.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

/** UC-05 Manage walk-in waiting list (FR-07). */
@WithMockUser(roles = "WAITER_HOST")
class WaitlistControllerTest extends ApiTestSupport {

    @Test
    @DisplayName("TC-RES-15 Walk-in parties are listed in arrival order with their positions")
    void partiesKeepArrivalOrder() throws Exception {
        long firstId = addParty("Le Hoang Nam", 4);
        addParty("Pham Thu Ha", 2);

        mockMvc.perform(get("/api/waitlist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].partyName").value("Le Hoang Nam"))
                .andExpect(jsonPath("$[0].position").value(1))
                .andExpect(jsonPath("$[1].partyName").value("Pham Thu Ha"))
                .andExpect(jsonPath("$[1].position").value(2));

        // The first party leaves: the second one moves up (UC-05 alternative 5a)
        mockMvc.perform(delete("/api/waitlist/{id}", firstId)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/waitlist"))
                .andExpect(jsonPath("$[0].partyName").value("Pham Thu Ha"))
                .andExpect(jsonPath("$[0].position").value(1));
    }

    @Test
    @DisplayName("TC-RES-16 A walk-in without a name or with party size 0 is rejected")
    void invalidWalkIn() throws Exception {
        mockMvc.perform(post("/api/waitlist").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("partyName", " ", "phone", "0987654321", "partySize", 0))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.partyName").value("Party name is required"))
                .andExpect(jsonPath("$.fieldErrors.partySize").value("Party size must be at least 1"));
    }

    private long addParty(String name, int size) throws Exception {
        String response = mockMvc.perform(post("/api/waitlist").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("partyName", name, "phone", "0987654321", "partySize", size))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }
}
