package org.example.reservations.table;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.reservations.auth.SecurityUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tables")
@RequiredArgsConstructor
public class RestaurantTableController {

    private final TableService tableService;

    @GetMapping
    public List<TableDto> getTablesByRestaurant() {
        return tableService.getTablesByRestaurantId(SecurityUtils.currentRestaurantId());
    }

    @GetMapping("/{id}")
    public TableDto getTableById(@PathVariable Long id) {
        return tableService.getTableById(id);
    }

    @PostMapping
    public TableDto createTable(@Valid @RequestBody TableDto dto) {
        return tableService.createTable(dto);
    }

    @PutMapping("/{id}")
    public TableDto updateTable(@PathVariable Long id, @Valid @RequestBody TableDto dto) {
        return tableService.updateTable(id, dto);
    }

    @DeleteMapping("/{id}")
    public void deleteTable(@PathVariable Long id) {
        tableService.deleteTable(id);
    }
}
