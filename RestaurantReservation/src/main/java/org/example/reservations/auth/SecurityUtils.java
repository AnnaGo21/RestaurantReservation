package org.example.reservations.auth;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static Long currentRestaurantId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getDetails() instanceof Long restaurantId)) {
            throw new AccessDeniedException("No restaurant context on current user");
        }
        return restaurantId;
    }
}
