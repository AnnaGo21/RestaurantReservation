package org.example.reservations.guest;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GuestDto {
    private Long id;

    @NotBlank(message = "Guest name is required")
    private String fullName;

    @NotBlank(message = "Phone number is required")
    private String phone;

    private String email;
    private Integer totalVisits;
    private Integer noShowCount;
    private Long restaurantId;
}
