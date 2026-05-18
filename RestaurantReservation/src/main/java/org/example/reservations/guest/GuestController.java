package org.example.reservations.guest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/guests")
@RequiredArgsConstructor
public class GuestController {

    private final GuestService guestService;

    @GetMapping
    public List<GuestDto> getGuestsByRestaurant(@RequestParam Long restaurantId) {
        return guestService.getGuestsByRestaurantId(restaurantId);
    }

    @GetMapping("/{id}")
    public GuestDto getGuestById(@PathVariable Long id) {
        return guestService.getGuestById(id);
    }

    @GetMapping("/search")
    public GuestDto findByPhone(@RequestParam Long restaurantId, @RequestParam String phone) {
        return guestService.findByRestaurantAndPhone(restaurantId, phone)
                .orElseThrow(() -> new IllegalArgumentException("Guest not found with phone: " + phone));
    }

    @PostMapping
    public GuestDto createGuest(@Valid @RequestBody GuestDto dto) {
        return guestService.createGuest(dto);
    }

    @PutMapping("/{id}")
    public GuestDto updateGuest(@PathVariable Long id, @Valid @RequestBody GuestDto dto) {
        return guestService.updateGuest(id, dto);
    }

    @DeleteMapping("/{id}")
    public void deleteGuest(@PathVariable Long id) {
        guestService.deleteGuest(id);
    }
}
