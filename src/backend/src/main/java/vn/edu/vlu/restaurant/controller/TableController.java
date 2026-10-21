package vn.edu.vlu.restaurant.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.vlu.restaurant.dto.SectionRequest;
import vn.edu.vlu.restaurant.dto.SectionResponse;
import vn.edu.vlu.restaurant.dto.TableRequest;
import vn.edu.vlu.restaurant.dto.TableResponse;
import vn.edu.vlu.restaurant.service.TableService;

/** Manage tables and sections (UC-02). Viewing is open to front-of-house staff; changes need a Manager. */
@RestController
@RequestMapping("/api")
public class TableController {

    private final TableService tableService;

    public TableController(TableService tableService) {
        this.tableService = tableService;
    }

    @GetMapping("/sections")
    @PreAuthorize(AccessRules.FRONT_OF_HOUSE)
    public List<SectionResponse> listSections() {
        return tableService.listSections();
    }

    @PostMapping("/sections")
    @PreAuthorize(AccessRules.MANAGER)
    @ResponseStatus(HttpStatus.CREATED)
    public SectionResponse createSection(@Valid @RequestBody SectionRequest request) {
        return tableService.createSection(request);
    }

    /** includeInactive=true is used by the Manager's table-management screen. */
    @GetMapping("/tables")
    @PreAuthorize(AccessRules.FRONT_OF_HOUSE)
    public List<TableResponse> listTables(@RequestParam(defaultValue = "false") boolean includeInactive) {
        return tableService.listTables(includeInactive);
    }

    @PostMapping("/tables")
    @PreAuthorize(AccessRules.MANAGER)
    @ResponseStatus(HttpStatus.CREATED)
    public TableResponse createTable(@Valid @RequestBody TableRequest request) {
        return tableService.createTable(request);
    }

    @PutMapping("/tables/{id}")
    @PreAuthorize(AccessRules.MANAGER)
    public TableResponse updateTable(@PathVariable Long id, @Valid @RequestBody TableRequest request) {
        return tableService.updateTable(id, request);
    }

    @PostMapping("/tables/{id}/deactivate")
    @PreAuthorize(AccessRules.MANAGER)
    public TableResponse deactivateTable(@PathVariable Long id) {
        return tableService.deactivateTable(id);
    }

    @PostMapping("/tables/{id}/activate")
    @PreAuthorize(AccessRules.MANAGER)
    public TableResponse activateTable(@PathVariable Long id) {
        return tableService.activateTable(id);
    }
}
