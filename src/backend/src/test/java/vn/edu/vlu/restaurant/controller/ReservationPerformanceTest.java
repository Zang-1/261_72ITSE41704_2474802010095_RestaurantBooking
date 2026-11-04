package vn.edu.vlu.restaurant.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import vn.edu.vlu.restaurant.domain.DiningTable;
import vn.edu.vlu.restaurant.domain.Reservation;
import vn.edu.vlu.restaurant.domain.TableSection;
import vn.edu.vlu.restaurant.repository.DiningTableRepository;
import vn.edu.vlu.restaurant.repository.ReservationRepository;
import vn.edu.vlu.restaurant.repository.TableSectionRepository;

/** NFR-01: the availability search answers within 2 seconds for 50 tables and 500 reservations in a week. */
@WithMockUser(roles = "WAITER_HOST")
class ReservationPerformanceTest extends ApiTestSupport {

    @Autowired
    private DiningTableRepository tableRepository;
    @Autowired
    private TableSectionRepository sectionRepository;
    @Autowired
    private ReservationRepository reservationRepository;

    @Test
    @DisplayName("TC-RES-30 Availability search with 50 tables and 500 reservations in a week takes under 2 seconds (NFR-01)")
    void searchIsFastEnough() throws Exception {
        TableSection extra = sectionRepository.save(new TableSection("Garden Room"));
        for (int number = 28; number <= 50; number++) {
            tableRepository.save(new DiningTable(number, 4, extra));
        }
        List<DiningTable> tables = tableRepository.findByActiveTrueOrderByNumberAsc();
        assertTrue(tables.size() >= 50);

        // 500 bookings over the next 7 days: 10 per table, at 11:00, 14:30 and 18:00, never overlapping
        LocalDate firstDay = tomorrow();
        int[][] slots = {{11, 0}, {14, 30}, {18, 0}};
        for (int i = 0; i < 500; i++) {
            DiningTable table = tables.get(i % 50);
            int round = i / 50;
            LocalDateTime start = firstDay.plusDays(round % 7).atTime(slots[round / 7 % 3][0], slots[round / 7 % 3][1]);
            reservationRepository.save(new Reservation(String.format("P%07d", i), table, "Guest " + i,
                    "0900000000", 2, start, null));
        }

        // The first call warms up the JIT and the query plans; the next five are timed
        search();
        long slowest = 0;
        for (int i = 0; i < 5; i++) {
            long begin = System.nanoTime();
            search();
            slowest = Math.max(slowest, (System.nanoTime() - begin) / 1_000_000);
        }
        System.out.println("NFR-01 slowest availability search: " + slowest + " ms");
        assertTrue(slowest < 2000, "Slowest search took " + slowest + " ms");
    }

    private void search() throws Exception {
        mockMvc.perform(get("/api/reservations/available-tables")
                        .param("date", tomorrow().plusDays(3).toString())
                        .param("startTime", "19:00")
                        .param("partySize", "2"))
                .andExpect(status().isOk());
    }
}
