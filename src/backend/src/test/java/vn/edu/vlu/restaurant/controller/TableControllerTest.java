package vn.edu.vlu.restaurant.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import vn.edu.vlu.restaurant.domain.DiningTable;
import vn.edu.vlu.restaurant.domain.Reservation;
import vn.edu.vlu.restaurant.repository.DiningTableRepository;
import vn.edu.vlu.restaurant.repository.ReservationRepository;

/** UC-02 Manage tables and sections (FR-02). */
@WithMockUser(roles = "MANAGER")
class TableControllerTest extends ApiTestSupport {

    @Autowired
    private DiningTableRepository tableRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Test
    @DisplayName("TC-RES-06 A table number that already exists is rejected")
    void duplicateTableNumber() throws Exception {
        DiningTable existing = tableRepository.findAllByOrderByNumberAsc().get(0);

        mockMvc.perform(post("/api/tables")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("number", existing.getNumber(), "capacity", 4,
                                "sectionId", existing.getSection().getId()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.number")
                        .value("Table number " + existing.getNumber() + " already exists"));
    }

    @Test
    @DisplayName("TC-RES-07 Capacity outside 1-20 is rejected")
    void capacityOutOfRange() throws Exception {
        mockMvc.perform(post("/api/tables")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("number", 40, "capacity", 25, "sectionId", sectionId()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.capacity").value("Capacity must be between 1 and 20"));
    }

    @Test
    @DisplayName("A Manager can add a new table, which starts Available and active")
    void createTable() throws Exception {
        mockMvc.perform(post("/api/tables")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("number", 41, "capacity", 6, "sectionId", sectionId()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.number").value(41))
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @DisplayName("TC-RES-08 A table with a future reservation cannot be deactivated")
    void deactivateTableWithFutureReservation() throws Exception {
        DiningTable table = tableRepository.findAllByOrderByNumberAsc().get(2);
        reservationRepository.save(new Reservation("RTEST001", table, "Nguyen Thi Mai", "0901234567", 2,
                tomorrow().atTime(19, 0), null));

        mockMvc.perform(post("/api/tables/{id}/deactivate", table.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Table " + table.getNumber()
                        + " has 1 future reservation(s) and cannot be deactivated"));
    }

    @Test
    @DisplayName("A table without future reservations can be deactivated")
    void deactivateFreeTable() throws Exception {
        DiningTable table = tableRepository.findAllByOrderByNumberAsc().get(3);

        mockMvc.perform(post("/api/tables/{id}/deactivate", table.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    private Long sectionId() {
        return tableRepository.findAllByOrderByNumberAsc().get(0).getSection().getId();
    }
}
