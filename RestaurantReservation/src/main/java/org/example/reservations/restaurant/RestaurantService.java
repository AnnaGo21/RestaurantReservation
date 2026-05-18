package org.example.reservations.restaurant;

import lombok.RequiredArgsConstructor;
import org.example.reservations.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;

    @Transactional(readOnly = true)
    public List<RestaurantDto> getAllRestaurants() {
        return restaurantRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RestaurantDto getRestaurantById(Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + id));
        return toDto(restaurant);
    }

    @Transactional
    public RestaurantDto createRestaurant(RestaurantDto dto) {
        Restaurant restaurant = new Restaurant();
        restaurant.setName(dto.getName());
        restaurant.setPhone(dto.getPhone());
        restaurant.setAddress(dto.getAddress());
        restaurant.setCuisineType(dto.getCuisineType());
        restaurant.setOpeningHours(dto.getOpeningHours());
        restaurant.setLogoUrl(dto.getLogoUrl());
        restaurant.setTimezone(dto.getTimezone() != null ? dto.getTimezone() : "UTC");
        restaurant.setDefaultReservationMinutes(dto.getDefaultReservationMinutes() != null ? dto.getDefaultReservationMinutes() : 90);
        restaurant.setGracePeriodMinutes(dto.getGracePeriodMinutes() != null ? dto.getGracePeriodMinutes() : 15);

        restaurant = restaurantRepository.save(restaurant);
        return toDto(restaurant);
    }

    @Transactional
    public RestaurantDto updateRestaurant(Long id, RestaurantDto dto) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + id));

        restaurant.setName(dto.getName());
        restaurant.setPhone(dto.getPhone());
        restaurant.setAddress(dto.getAddress());
        restaurant.setCuisineType(dto.getCuisineType());
        restaurant.setOpeningHours(dto.getOpeningHours());
        restaurant.setLogoUrl(dto.getLogoUrl());
        restaurant.setTimezone(dto.getTimezone());
        restaurant.setDefaultReservationMinutes(dto.getDefaultReservationMinutes());
        restaurant.setGracePeriodMinutes(dto.getGracePeriodMinutes());

        restaurant = restaurantRepository.save(restaurant);
        return toDto(restaurant);
    }

    @Transactional
    public void deleteRestaurant(Long id) {
        if (!restaurantRepository.existsById(id)) {
            throw new ResourceNotFoundException("Restaurant not found with id: " + id);
        }
        restaurantRepository.deleteById(id);
    }

    private RestaurantDto toDto(Restaurant restaurant) {
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
