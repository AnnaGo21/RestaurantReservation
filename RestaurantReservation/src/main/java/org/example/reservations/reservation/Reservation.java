package org.example.reservations.reservation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.example.reservations.guest.Guest;
import org.example.reservations.restaurant.Restaurant;
import org.example.reservations.table.RestaurantTable;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Restaurant restaurant;

    @ManyToOne(optional = false)
    private RestaurantTable restaurantTable;

    @ManyToOne(optional = false)
    private Guest guest;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Column(nullable = false)
    private Integer partySize;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status = ReservationStatus.CONFIRMED;

    private Boolean reminderSent = false;

    private LocalDateTime checkedInAt;
}
