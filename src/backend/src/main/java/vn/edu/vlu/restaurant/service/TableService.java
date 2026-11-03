package vn.edu.vlu.restaurant.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.vlu.restaurant.domain.DiningTable;
import vn.edu.vlu.restaurant.domain.OrderStatus;
import vn.edu.vlu.restaurant.domain.Reservation;
import vn.edu.vlu.restaurant.domain.ReservationSettings;
import vn.edu.vlu.restaurant.domain.ReservationStatus;
import vn.edu.vlu.restaurant.domain.TableSection;
import vn.edu.vlu.restaurant.domain.TableStatus;
import vn.edu.vlu.restaurant.dto.SectionRequest;
import vn.edu.vlu.restaurant.dto.SectionResponse;
import vn.edu.vlu.restaurant.dto.TableRequest;
import vn.edu.vlu.restaurant.dto.TableResponse;
import vn.edu.vlu.restaurant.exception.BusinessRuleException;
import vn.edu.vlu.restaurant.exception.InvalidInputException;
import vn.edu.vlu.restaurant.exception.NotFoundException;
import vn.edu.vlu.restaurant.repository.CustomerOrderRepository;
import vn.edu.vlu.restaurant.repository.DiningTableRepository;
import vn.edu.vlu.restaurant.repository.ReservationRepository;
import vn.edu.vlu.restaurant.repository.ReservationSettingsRepository;
import vn.edu.vlu.restaurant.repository.TableSectionRepository;

/** Tables and sections (FR-02, UC-02) and the manual steps of the table lifecycle (FR-09, UC-06, BR-01 to BR-05). */
@Service
@Transactional
public class TableService {

    private final DiningTableRepository tableRepository;
    private final TableSectionRepository sectionRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationSettingsRepository settingsRepository;
    private final CustomerOrderRepository orderRepository;
    private final Clock clock;

    public TableService(DiningTableRepository tableRepository, TableSectionRepository sectionRepository,
                        ReservationRepository reservationRepository, ReservationSettingsRepository settingsRepository,
                        CustomerOrderRepository orderRepository, Clock clock) {
        this.tableRepository = tableRepository;
        this.sectionRepository = sectionRepository;
        this.reservationRepository = reservationRepository;
        this.settingsRepository = settingsRepository;
        this.orderRepository = orderRepository;
        this.clock = clock;
    }

    /** The guests left: Occupied → Needs cleaning, only when the table has no open order (BR-05). */
    public TableResponse markNeedsCleaning(Long id) {
        DiningTable table = lockTable(id);
        boolean openOrder = !orderRepository.findByTableIdAndStatus(id, OrderStatus.OPEN).isEmpty()
                || !orderRepository.findByTableIdAndStatus(id, OrderStatus.LOCKED).isEmpty();
        if (openOrder) {
            throw new BusinessRuleException("Table " + table.getNumber() + " still has an open order; close its bill first");
        }
        table.changeStatus(TableStatus.NEEDS_CLEANING);
        return TableResponse.from(table);
    }

    /**
     * Needs cleaning → Available (BR-03).
     * shortcut: a booking due within 30 minutes turns the table Reserved at the next scheduler run (≤ 1 minute later).
     */
    public TableResponse markClean(Long id) {
        DiningTable table = lockTable(id);
        table.changeStatus(TableStatus.AVAILABLE);
        return TableResponse.from(table);
    }

    public TableResponse seatWalkIn(Long id, int partySize) {
        DiningTable table = lockTable(id);
        seatParty(table, partySize);
        return TableResponse.from(table);
    }

    /** Seats a party at a free table; shared by walk-ins and the waiting list (BR-01, BR-02, BR-03, BR-05). */
    public void seatParty(DiningTable table, int partySize) {
        if (!table.isActive()) {
            throw new BusinessRuleException("Table " + table.getNumber() + " is not in use");
        }
        if (table.getStatus() == TableStatus.NEEDS_CLEANING) {
            throw new BusinessRuleException("Table " + table.getNumber() + " must be cleaned before the guests sit down");
        }
        if (table.getStatus() != TableStatus.AVAILABLE) {
            throw new BusinessRuleException("Table " + table.getNumber() + " is not available");
        }
        if (!table.canSeat(partySize)) {
            throw new InvalidInputException("partySize", "Table " + table.getNumber() + " seats only " + table.getCapacity() + " guests");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        ReservationSettings settings = settingsRepository.findById(ReservationSettings.SINGLETON_ID).orElseThrow();
        for (Reservation r : reservationRepository.findByTableIdAndStatusOrderByStartTimeAsc(table.getId(), ReservationStatus.CONFIRMED)) {
            if (Reservation.overlap(r.getStartTime(), now, settings)) {
                throw new BusinessRuleException("Table " + table.getNumber() + " has a reservation at "
                        + r.getStartTime().toLocalTime() + "; a walk-in would overlap it");
            }
        }
        table.changeStatus(TableStatus.OCCUPIED);
    }

    public DiningTable lockTable(Long id) {
        return tableRepository.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("Table not found"));
    }

    @Transactional(readOnly = true)
    public List<SectionResponse> listSections() {
        return sectionRepository.findAllByOrderByNameAsc().stream().map(SectionResponse::from).toList();
    }

    public SectionResponse createSection(SectionRequest request) {
        String name = request.name().trim();
        if (sectionRepository.existsByNameIgnoreCase(name)) {
            throw new InvalidInputException("name", "Section \"" + name + "\" already exists");
        }
        return SectionResponse.from(sectionRepository.save(new TableSection(name)));
    }

    @Transactional(readOnly = true)
    public List<TableResponse> listTables(boolean includeInactive) {
        List<DiningTable> tables = includeInactive
                ? tableRepository.findAllByOrderByNumberAsc()
                : tableRepository.findByActiveTrueOrderByNumberAsc();
        return tables.stream().map(TableResponse::from).toList();
    }

    public TableResponse createTable(TableRequest request) {
        if (tableRepository.existsByNumber(request.number())) {
            throw duplicateNumber(request.number());
        }
        TableSection section = findSection(request.sectionId());
        DiningTable table = new DiningTable(request.number(), request.capacity(), section);
        return TableResponse.from(tableRepository.save(table));
    }

    public TableResponse updateTable(Long id, TableRequest request) {
        DiningTable table = findTable(id);
        if (tableRepository.existsByNumberAndIdNot(request.number(), id)) {
            throw duplicateNumber(request.number());
        }
        table.updateDetails(request.number(), request.capacity(), findSection(request.sectionId()));
        return TableResponse.from(table);
    }

    /** A table that still has future reservations cannot be deactivated (FR-02, UC-02 alternative 4a). */
    public TableResponse deactivateTable(Long id) {
        DiningTable table = findTable(id);
        long futureReservations = reservationRepository.countByTableIdAndStatusAndStartTimeAfter(
                id, ReservationStatus.CONFIRMED, LocalDateTime.now(clock));
        if (futureReservations > 0) {
            throw new BusinessRuleException("Table " + table.getNumber() + " has " + futureReservations
                    + " future reservation(s) and cannot be deactivated");
        }
        table.deactivate();
        return TableResponse.from(table);
    }

    public TableResponse activateTable(Long id) {
        DiningTable table = findTable(id);
        table.activate();
        return TableResponse.from(table);
    }

    private DiningTable findTable(Long id) {
        return tableRepository.findById(id).orElseThrow(() -> new NotFoundException("Table not found"));
    }

    private TableSection findSection(Long id) {
        return sectionRepository.findById(id)
                .orElseThrow(() -> new InvalidInputException("sectionId", "Section not found"));
    }

    private InvalidInputException duplicateNumber(int number) {
        return new InvalidInputException("number", "Table number " + number + " already exists");
    }
}
