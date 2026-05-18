package org.example.reservations.guest;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GuestRepository extends JpaRepository<Guest, Long> {

    Optional<Guest> findByRestaurantIdAndPhone(Long restaurantId, String phone);
}
