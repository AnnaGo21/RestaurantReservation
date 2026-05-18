package org.example.reservations.guest;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.example.reservations.restaurant.Restaurant;

@Getter
@Setter
@Entity
@Table(name = "guests")
public class Guest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Restaurant restaurant;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String phone;

    private String email;

    private Integer totalVisits = 0;

    private Integer noShowCount = 0;
}
