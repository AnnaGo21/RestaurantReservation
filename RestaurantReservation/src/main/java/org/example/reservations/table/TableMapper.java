package org.example.reservations.table;

import org.springframework.stereotype.Component;

@Component
public class TableMapper {

    public TableDto toDto(RestaurantTable table) {
        return new TableDto(
                table.getId(),
                table.getLabel(),
                table.getCapacity(),
                table.getStatus(),
                table.getPositionX(),
                table.getPositionY(),
                table.getRestaurant().getId()
        );
    }
}
