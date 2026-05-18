package org.example.reservations.guest;

import lombok.RequiredArgsConstructor;
import org.example.reservations.exception.ResourceNotFoundException;
import org.example.reservations.restaurant.Restaurant;
import org.example.reservations.restaurant.RestaurantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GuestService {

    private final GuestRepository guestRepository;
    private final RestaurantRepository restaurantRepository;

    @Transactional(readOnly = true)
    public List<GuestDto> getGuestsByRestaurantId(Long restaurantId) {
        List<Guest> guests = guestRepository.findAll().stream()
                .filter(g -> g.getRestaurant().getId().equals(restaurantId))
                .collect(Collectors.toList());
        return guests.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GuestDto getGuestById(Long id) {
        Guest guest = guestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Guest not found with id: " + id));
        return toDto(guest);
    }

    @Transactional(readOnly = true)
    public Optional<GuestDto> findByRestaurantAndPhone(Long restaurantId, String phone) {
        return guestRepository.findByRestaurantIdAndPhone(restaurantId, phone)
                .map(this::toDto);
    }

    @Transactional
    public GuestDto createGuest(GuestDto dto) {
        Restaurant restaurant = restaurantRepository.findById(dto.getRestaurantId())
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + dto.getRestaurantId()));

        Optional<Guest> existing = guestRepository.findByRestaurantIdAndPhone(dto.getRestaurantId(), dto.getPhone());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Guest with this phone already exists for this restaurant");
        }

        Guest guest = new Guest();
        guest.setRestaurant(restaurant);
        guest.setFullName(dto.getFullName());
        guest.setPhone(dto.getPhone());
        guest.setEmail(dto.getEmail());
        guest.setTotalVisits(0);
        guest.setNoShowCount(0);

        guest = guestRepository.save(guest);
        return toDto(guest);
    }

    @Transactional
    public GuestDto updateGuest(Long id, GuestDto dto) {
        Guest guest = guestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Guest not found with id: " + id));

        guest.setFullName(dto.getFullName());
        guest.setPhone(dto.getPhone());
        guest.setEmail(dto.getEmail());

        guest = guestRepository.save(guest);
        return toDto(guest);
    }

    @Transactional
    public void deleteGuest(Long id) {
        if (!guestRepository.existsById(id)) {
            throw new ResourceNotFoundException("Guest not found with id: " + id);
        }
        guestRepository.deleteById(id);
    }

    @Transactional
    public void incrementVisits(Long guestId) {
        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new ResourceNotFoundException("Guest not found with id: " + guestId));
        guest.setTotalVisits(guest.getTotalVisits() + 1);
        guestRepository.save(guest);
    }

    @Transactional
    public void incrementNoShows(Long guestId) {
        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new ResourceNotFoundException("Guest not found with id: " + guestId));
        guest.setNoShowCount(guest.getNoShowCount() + 1);
        guestRepository.save(guest);
    }

    private GuestDto toDto(Guest guest) {
        return new GuestDto(
                guest.getId(),
                guest.getFullName(),
                guest.getPhone(),
                guest.getEmail(),
                guest.getTotalVisits(),
                guest.getNoShowCount(),
                guest.getRestaurant().getId()
        );
    }
}
