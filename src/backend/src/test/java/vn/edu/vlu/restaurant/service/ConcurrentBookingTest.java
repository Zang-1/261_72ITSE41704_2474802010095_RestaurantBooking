package vn.edu.vlu.restaurant.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import vn.edu.vlu.restaurant.domain.DiningTable;
import vn.edu.vlu.restaurant.dto.ReservationRequest;
import vn.edu.vlu.restaurant.dto.ReservationResponse;
import vn.edu.vlu.restaurant.exception.BusinessRuleException;
import vn.edu.vlu.restaurant.repository.DiningTableRepository;
import vn.edu.vlu.restaurant.repository.ReservationRepository;

/**
 * NFR-06: two staff book the same table for the same time at the same moment; exactly one wins.
 * Not @Transactional, so each thread commits its own transaction like two real requests. The test cleans up after itself.
 */
@SpringBootTest
@ActiveProfiles("test")
class ConcurrentBookingTest {

    @Autowired
    private ReservationService reservationService;
    @Autowired
    private DiningTableRepository tableRepository;
    @Autowired
    private ReservationRepository reservationRepository;

    private final List<Long> created = new ArrayList<>();

    @AfterEach
    void removeBookings() {
        reservationRepository.deleteAllById(created);
    }

    @Test
    @DisplayName("TC-RES-27 Two simultaneous bookings of one table and time: one is saved, the other gets a clear message")
    void onlyOneOfTwoSimultaneousBookingsWins() throws Exception {
        DiningTable table = tableRepository.findAllByOrderByNumberAsc().stream()
                .filter(t -> t.getCapacity() == 4 && t.isActive()).findFirst().orElseThrow();
        LocalDate date = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(2);
        ReservationRequest request = new ReservationRequest(table.getId(), "Tran Van Binh", "0912345678",
                2, date, LocalTime.of(19, 0), null);

        CountDownLatch go = new CountDownLatch(1);
        Callable<ReservationResponse> booking = () -> {
            go.await();
            return reservationService.create(request);
        };
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Future<ReservationResponse> first = pool.submit(booking);
        Future<ReservationResponse> second = pool.submit(booking);
        go.countDown();

        int saved = 0;
        List<Throwable> refused = new ArrayList<>();
        for (Future<ReservationResponse> f : List.of(first, second)) {
            try {
                created.add(f.get().id());
                saved++;
            } catch (java.util.concurrent.ExecutionException e) {
                refused.add(e.getCause());
            }
        }
        pool.shutdown();

        assertEquals(1, saved);
        assertEquals(1, refused.size());
        assertInstanceOf(BusinessRuleException.class, refused.get(0));
        assertEquals("Table " + table.getNumber() + " is already booked at 19:00; choose another time or table",
                refused.get(0).getMessage());
    }
}
