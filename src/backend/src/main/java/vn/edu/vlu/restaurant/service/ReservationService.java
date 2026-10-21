package vn.edu.vlu.restaurant.service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.vlu.restaurant.domain.DiningTable;
import vn.edu.vlu.restaurant.domain.Reservation;
import vn.edu.vlu.restaurant.domain.ReservationSettings;
import vn.edu.vlu.restaurant.dto.AvailabilityQuery;
import vn.edu.vlu.restaurant.dto.ReservationRequest;
import vn.edu.vlu.restaurant.dto.ReservationResponse;
import vn.edu.vlu.restaurant.dto.ReservationSettingsDto;
import vn.edu.vlu.restaurant.dto.TableResponse;
import vn.edu.vlu.restaurant.exception.BusinessRuleException;
import vn.edu.vlu.restaurant.exception.InvalidInputException;
import vn.edu.vlu.restaurant.exception.NotFoundException;
import vn.edu.vlu.restaurant.repository.DiningTableRepository;
import vn.edu.vlu.restaurant.repository.ReservationRepository;
import vn.edu.vlu.restaurant.repository.ReservationSettingsRepository;

/**
 * Reservations (FR-03 to FR-05, UC-03, UC-04).
 * Week 5-6 scope: CRUD, input validation and capacity fit (BR-02).
 * Week 7 adds the no-double-booking rule BR-01 (with a pessimistic lock, NFR-06), check-in and no-show.
 */
@Service
@Transactional
public class ReservationService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 7;

    private final ReservationRepository reservationRepository;
    private final DiningTableRepository tableRepository;
    private final ReservationSettingsRepository settingsRepository;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public ReservationService(ReservationRepository reservationRepository, DiningTableRepository tableRepository,
                              ReservationSettingsRepository settingsRepository, Clock clock) {
        this.reservationRepository = reservationRepository;
        this.tableRepository = tableRepository;
        this.settingsRepository = settingsRepository;
        this.clock = clock;
    }

    /** Active tables that seat the party (FR-03). The overlap filter of BR-01 is added in Week 7. */
    @Transactional(readOnly = true)
    public List<TableResponse> searchAvailableTables(AvailabilityQuery query) {
        validateStart(query.date(), query.startTime());
        validatePartySizeFitsSomeTable(query.partySize());
        return tableRepository
                .findByActiveTrueAndCapacityGreaterThanEqualOrderByCapacityAscNumberAsc(query.partySize())
                .stream().map(TableResponse::from).toList();
    }

    public ReservationResponse create(ReservationRequest request) {
        LocalDateTime start = validateStart(request.date(), request.startTime());
        DiningTable table = findUsableTable(request.tableId(), request.partySize());
        Reservation reservation = new Reservation(newCode(), table, request.guestName().trim(),
                request.phone(), request.partySize(), start, normaliseNote(request.note()));
        return ReservationResponse.from(reservationRepository.save(reservation));
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> listForDate(LocalDate date) {
        return reservationRepository
                .findByStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeAsc(
                        date.atStartOfDay(), date.plusDays(1).atStartOfDay())
                .stream().map(ReservationResponse::from).toList();
    }

    /** Modify a confirmed reservation before its start time (FR-05, UC-04 step 2). */
    public ReservationResponse modify(Long id, ReservationRequest request) {
        Reservation reservation = findReservation(id);
        LocalDateTime start = validateStart(request.date(), request.startTime());
        DiningTable table = findUsableTable(request.tableId(), request.partySize());
        reservation.modify(table, request.guestName().trim(), request.phone(), request.partySize(),
                start, normaliseNote(request.note()), now());
        return ReservationResponse.from(reservation);
    }

    /** Cancel a confirmed reservation before its start time; the slot is released (FR-05, UC-04 step 3). */
    public ReservationResponse cancel(Long id) {
        Reservation reservation = findReservation(id);
        reservation.cancel(now());
        return ReservationResponse.from(reservation);
    }

    @Transactional(readOnly = true)
    public ReservationSettingsDto getSettings() {
        return ReservationSettingsDto.from(loadSettings());
    }

    public ReservationSettingsDto updateSettings(ReservationSettingsDto request) {
        ReservationSettings settings = loadSettings();
        settings.update(request.diningMinutes(), request.turnMinutes());
        return ReservationSettingsDto.from(settings);
    }

    private LocalDateTime validateStart(LocalDate date, LocalTime time) {
        LocalDateTime now = now();
        if (date.isBefore(now.toLocalDate())) {
            throw new InvalidInputException("date", "Date cannot be in the past");
        }
        LocalDateTime start = date.atTime(time);
        if (!start.isAfter(now)) {
            throw new InvalidInputException("startTime", "Start time must be later than now");
        }
        return start;
    }

    private void validatePartySizeFitsSomeTable(int partySize) {
        int largest = tableRepository.findLargestActiveCapacity()
                .orElseThrow(() -> new BusinessRuleException("There are no active tables"));
        if (partySize > largest) {
            throw new InvalidInputException("partySize", "No table seats more than " + largest + " guests");
        }
    }

    private DiningTable findUsableTable(Long tableId, int partySize) {
        DiningTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> new InvalidInputException("tableId", "Table not found"));
        if (!table.isActive()) {
            throw new InvalidInputException("tableId", "Table " + table.getNumber() + " is not in use");
        }
        if (!table.canSeat(partySize)) {
            throw new InvalidInputException("partySize",
                    "Table " + table.getNumber() + " seats only " + table.getCapacity() + " guests");
        }
        return table;
    }

    private Reservation findReservation(Long id) {
        return reservationRepository.findById(id).orElseThrow(() -> new NotFoundException("Reservation not found"));
    }

    private ReservationSettings loadSettings() {
        return settingsRepository.findById(ReservationSettings.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("reservation_settings row is missing"));
    }

    /** Short code read out to the guest, e.g. R7KQ2MXA. Ambiguous characters (0, O, 1, I) are left out. */
    private String newCode() {
        String code;
        do {
            StringBuilder builder = new StringBuilder("R");
            for (int i = 0; i < CODE_LENGTH; i++) {
                builder.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
            }
            code = builder.toString();
        } while (reservationRepository.existsByCode(code));
        return code;
    }

    private String normaliseNote(String note) {
        return note == null || note.isBlank() ? null : note.trim();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
