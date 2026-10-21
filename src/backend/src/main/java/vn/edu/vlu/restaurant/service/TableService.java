package vn.edu.vlu.restaurant.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.vlu.restaurant.domain.DiningTable;
import vn.edu.vlu.restaurant.domain.ReservationStatus;
import vn.edu.vlu.restaurant.domain.TableSection;
import vn.edu.vlu.restaurant.dto.SectionRequest;
import vn.edu.vlu.restaurant.dto.SectionResponse;
import vn.edu.vlu.restaurant.dto.TableRequest;
import vn.edu.vlu.restaurant.dto.TableResponse;
import vn.edu.vlu.restaurant.exception.BusinessRuleException;
import vn.edu.vlu.restaurant.exception.InvalidInputException;
import vn.edu.vlu.restaurant.exception.NotFoundException;
import vn.edu.vlu.restaurant.repository.DiningTableRepository;
import vn.edu.vlu.restaurant.repository.ReservationRepository;
import vn.edu.vlu.restaurant.repository.TableSectionRepository;

/** Tables and sections (FR-02, UC-02). */
@Service
@Transactional
public class TableService {

    private final DiningTableRepository tableRepository;
    private final TableSectionRepository sectionRepository;
    private final ReservationRepository reservationRepository;
    private final Clock clock;

    public TableService(DiningTableRepository tableRepository, TableSectionRepository sectionRepository,
                        ReservationRepository reservationRepository, Clock clock) {
        this.tableRepository = tableRepository;
        this.sectionRepository = sectionRepository;
        this.reservationRepository = reservationRepository;
        this.clock = clock;
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
