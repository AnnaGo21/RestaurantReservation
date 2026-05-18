package org.example.reservations.restaurant;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "restaurants")
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String phone;

    private String address;

    private String cuisineType;

    @Column(columnDefinition = "TEXT")
    private String openingHours;

    private String logoUrl;

    private String timezone = "UTC";

    private Integer defaultReservationMinutes = 90;

    private Integer gracePeriodMinutes = 15;
}
