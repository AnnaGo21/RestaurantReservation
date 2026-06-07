package org.example.reservations.restaurant;

import org.springframework.stereotype.Component;

@Component
public class RestaurantMapper {

    public RestaurantDto toDto(Restaurant restaurant) {
        return new RestaurantDto(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getPhone(),
                restaurant.getAddress(),
                restaurant.getCuisineType(),
                restaurant.getOpeningHours(),
                restaurant.getLogoUrl(),
                restaurant.getTimezone(),
                restaurant.getDefaultReservationMinutes(),
                restaurant.getGracePeriodMinutes()
        );
    }
}
