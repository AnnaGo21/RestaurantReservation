package org.example.reservations.restaurant;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;

    @GetMapping
    public List<RestaurantDto> getAllRestaurants() {
        return restaurantService.getAllRestaurants();
    }

    @GetMapping("/{id}")
    public RestaurantDto getRestaurantById(@PathVariable Long id) {
        return restaurantService.getRestaurantById(id);
    }

    @PostMapping
    public RestaurantDto createRestaurant(@Valid @RequestBody RestaurantDto dto) {
        return restaurantService.createRestaurant(dto);
    }

    @PutMapping("/{id}")
    public RestaurantDto updateRestaurant(@PathVariable Long id, @Valid @RequestBody RestaurantDto dto) {
        return restaurantService.updateRestaurant(id, dto);
    }

    @DeleteMapping("/{id}")
    public void deleteRestaurant(@PathVariable Long id) {
        restaurantService.deleteRestaurant(id);
    }
}
