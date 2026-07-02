package org.example.reservations.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityUtilsTest {

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsRestaurantIdWhenAuthenticationDetailsIsLong() {
        var auth = new UsernamePasswordAuthenticationToken("user@x", null, List.of());
        auth.setDetails(42L);
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(SecurityUtils.currentRestaurantId()).isEqualTo(42L);
    }

    @Test
    void throwsWhenNoAuthentication() {
        assertThatThrownBy(SecurityUtils::currentRestaurantId)
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void throwsWhenDetailsIsNull() {
        var auth = new UsernamePasswordAuthenticationToken("user@x", null, List.of());
        // details left null on purpose
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThatThrownBy(SecurityUtils::currentRestaurantId)
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void throwsWhenDetailsIsWrongType() {
        var auth = new UsernamePasswordAuthenticationToken("user@x", null, List.of());
        auth.setDetails("not-a-long");
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThatThrownBy(SecurityUtils::currentRestaurantId)
                .isInstanceOf(AccessDeniedException.class);
    }
}
