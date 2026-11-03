package vn.edu.vlu.restaurant.service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.vlu.restaurant.domain.DiningTable;
import vn.edu.vlu.restaurant.domain.Reservation;
import vn.edu.vlu.restaurant.domain.ReservationSettings;
import vn.edu.vlu.restaurant.domain.ReservationStatus;
import vn.edu.vlu.restaurant.domain.TableStatus;
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
 * Reservations (FR-03 to FR-06, UC-03, UC-04): CRUD with input validation, capacity fit (BR-02),
 * no double booking (BR-01, with a row lock for NFR-06), check-in and no-show (BR-05, CR-01).
 */
@Service
@Transactional
public class ReservationService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 7;
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

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

    /** Active tables that seat the party and have no overlapping booking (FR-03, BR-01, BR-02). */
    @Transactional(readOnly = true)
    public List<TableResponse> searchAvailableTables(AvailabilityQuery query) {
        LocalDateTime start = validateStart(query.date(), query.startTime());
        validatePartySizeFitsSomeTable(query.partySize());
        ReservationSettings settings = loadSettings();
        long block = settings.getDiningMinutes() + settings.getTurnMinutes();
        Set<Long> booked = reservationRepository.findByStatusAndStartTimeGreaterThanAndStartTimeLessThan(
                        ReservationStatus.CONFIRMED, start.minusMinutes(block), start.plusMinutes(block))
                .stream().map(r -> r.getTable().getId()).collect(Collectors.toSet());
        return tableRepository
                .findByActiveTrueAndCapacityGreaterThanEqualOrderByCapacityAscNumberAsc(query.partySize())
                .stream().filter(t -> !booked.contains(t.getId())).map(TableResponse::from).toList();
    }

    public ReservationResponse create(ReservationRequest request) {
        LocalDateTime start = validateStart(request.date(), request.startTime());
        DiningTable table = findUsableTable(request.tableId(), request.partySize());
        requireNoOverlap(table, start, null);
        Reservation reservation = reservationRepository.save(new Reservation(newCode(), table, request.guestName().trim(),
                request.phone(), request.partySize(), start, normaliseNote(request.note())));
        holdIfSoon(table, start);
        return ReservationResponse.from(reservation);
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
        DiningTable oldTable = reservation.getTable();
        LocalDateTime start = validateStart(request.date(), request.startTime());
        DiningTable table = findUsableTable(request.tableId(), request.partySize());
        requireNoOverlap(table, start, reservation);
        reservation.modify(table, request.guestName().trim(), request.phone(), request.partySize(),
                start, normaliseNote(request.note()), now());
        releaseIfNotHeld(oldTable);   // BR-05: a moved booking returns the old table to Available
        holdIfSoon(table, start);
        return ReservationResponse.from(reservation);
    }

    /** Cancel a confirmed reservation before its start time; the slot is released (FR-05, UC-04 step 3). */
    public ReservationResponse cancel(Long id) {
        Reservation reservation = findReservation(id);
        reservation.cancel(now());
        releaseIfNotHeld(reservation.getTable());
        return ReservationResponse.from(reservation);
    }

    /** The guests arrived: the table becomes Occupied (UC-04 step 4, BR-03, BR-05). */
    public ReservationResponse checkIn(Long id) {
        Reservation reservation = findReservation(id);
        DiningTable table = lockTable(reservation.getTable().getId());
        if (table.getStatus() == TableStatus.NEEDS_CLEANING) {
            throw new BusinessRuleException("Table " + table.getNumber() + " must be cleaned before the guests sit down");
        }
        if (table.getStatus() == TableStatus.OCCUPIED) {
            throw new BusinessRuleException("Table " + table.getNumber() + " is still occupied");
        }
        reservation.checkIn(now());
        table.changeStatus(TableStatus.OCCUPIED);
        return ReservationResponse.from(reservation);
    }

    /**
     * Time-driven part of BR-05 and FR-06, run every minute by TableStatusScheduler: bookings 45 minutes late
     * become No-show and free their table; a table turns Reserved 30 minutes before its next booking.
     * Returns how many reservations were looked at.
     */
    public int refreshStatuses(LocalDateTime now) {
        List<Reservation> due = reservationRepository.findByStatusAndStartTimeBetweenOrderByStartTimeAsc(
                ReservationStatus.CONFIRMED, now.minusDays(1), now.plusMinutes(Reservation.RESERVED_BEFORE_MINUTES));
        for (Reservation r : due) {
            if (r.isNoShow(now)) {
                r.markNoShow(now);
                releaseIfNotHeld(r.getTable(), now);
            } else if (r.getTable().getStatus() == TableStatus.AVAILABLE) {
                r.getTable().changeStatus(TableStatus.RESERVED);
            }
        }
        return due.size();
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

    /** BR-01 on the locked table row, so a booking made at the same moment is seen here (NFR-06). */
    private void requireNoOverlap(DiningTable table, LocalDateTime start, Reservation self) {
        ReservationSettings settings = loadSettings();
        for (Reservation other : reservationRepository.findByTableIdAndStatusOrderByStartTimeAsc(table.getId(), ReservationStatus.CONFIRMED)) {
            if ((self == null || !other.getId().equals(self.getId())) && Reservation.overlap(other.getStartTime(), start, settings)) {
                throw new BusinessRuleException("Table " + table.getNumber() + " is already booked at "
                        + other.getStartTime().format(HH_MM) + "; choose another time or table");
            }
        }
    }

    private void holdIfSoon(DiningTable table, LocalDateTime start) {
        if (table.getStatus() == TableStatus.AVAILABLE
                && !now().isBefore(start.minusMinutes(Reservation.RESERVED_BEFORE_MINUTES))) {
            table.changeStatus(TableStatus.RESERVED);
        }
    }

    private void releaseIfNotHeld(DiningTable table) {
        releaseIfNotHeld(table, now());
    }

    /** A Reserved table goes back to Available unless another booking is due within its window. */
    private void releaseIfNotHeld(DiningTable table, LocalDateTime now) {
        if (table.getStatus() != TableStatus.RESERVED) {
            return;
        }
        boolean held = reservationRepository.findByTableIdAndStatusOrderByStartTimeAsc(table.getId(), ReservationStatus.CONFIRMED)
                .stream().anyMatch(r -> !now.isBefore(r.getStartTime().minusMinutes(Reservation.RESERVED_BEFORE_MINUTES)) && !r.isNoShow(now));
        if (!held) {
            table.changeStatus(TableStatus.AVAILABLE);
        }
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
        DiningTable table = tableRepository.findByIdForUpdate(tableId)
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

    private DiningTable lockTable(Long id) {
        return tableRepository.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("Table not found"));
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
