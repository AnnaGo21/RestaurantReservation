package org.example.reservations.restaurant;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantDto {
    private Long id;

    @NotBlank(message = "Restaurant name is required")
    private String name;

    private String phone;
    private String address;
    private String cuisineType;
    private String openingHours;
    private String logoUrl;
    private String timezone;
    private Integer defaultReservationMinutes;
    private Integer gracePeriodMinutes;
}
