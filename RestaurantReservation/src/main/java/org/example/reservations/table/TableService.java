package org.example.reservations.table;

import lombok.RequiredArgsConstructor;
import org.example.reservations.exception.ResourceNotFoundException;
import org.example.reservations.restaurant.Restaurant;
import org.example.reservations.restaurant.RestaurantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TableService {

    private final RestaurantTableRepository tableRepository;
    private final RestaurantRepository restaurantRepository;
    private final TableMapper tableMapper;

    @Transactional(readOnly = true)
    public List<TableDto> getTablesByRestaurantId(Long restaurantId) {
        return tableRepository.findByRestaurantId(restaurantId).stream()
                .map(tableMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TableDto getTableById(Long id) {
        RestaurantTable table = tableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Table not found with id: " + id));
        return tableMapper.toDto(table);
    }

    @Transactional
    public TableDto createTable(TableDto dto) {
        Restaurant restaurant = restaurantRepository.findById(dto.getRestaurantId())
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + dto.getRestaurantId()));

        RestaurantTable table = new RestaurantTable();
        table.setLabel(dto.getLabel());
        table.setCapacity(dto.getCapacity());
        table.setStatus(dto.getStatus() != null ? dto.getStatus() : TableStatus.AVAILABLE);
        table.setPositionX(dto.getPositionX());
        table.setPositionY(dto.getPositionY());
        table.setRestaurant(restaurant);

        table = tableRepository.save(table);
        return tableMapper.toDto(table);
    }

    @Transactional
    public TableDto updateTable(Long id, TableDto dto) {
        RestaurantTable table = tableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Table not found with id: " + id));

        table.setLabel(dto.getLabel());
        table.setCapacity(dto.getCapacity());
        table.setStatus(dto.getStatus());
        table.setPositionX(dto.getPositionX());
        table.setPositionY(dto.getPositionY());

        table = tableRepository.save(table);
        return tableMapper.toDto(table);
    }

    @Transactional
    public void deleteTable(Long id) {
        if (!tableRepository.existsById(id)) {
            throw new ResourceNotFoundException("Table not found with id: " + id);
        }
        tableRepository.deleteById(id);
    }
}
