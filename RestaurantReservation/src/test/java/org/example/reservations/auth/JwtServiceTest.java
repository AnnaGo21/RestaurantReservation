package org.example.reservations.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.SignatureException;
import org.example.reservations.restaurant.Restaurant;
import org.example.reservations.user.AppUser;
import org.example.reservations.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-that-is-long-enough-for-hmac-sha256-signing!!";
    private static final long ONE_HOUR = 3_600_000L;

    private JwtService service;

    @BeforeEach
    void setUp() {
        service = new JwtService(SECRET, ONE_HOUR);
    }

    private AppUser userWithRestaurant(long restaurantId) {
        Restaurant r = new Restaurant();
        r.setId(restaurantId);
        AppUser u = new AppUser();
        u.setEmail("owner@bistro.ge");
        u.setRole(Role.OWNER);
        u.setRestaurant(r);
        return u;
    }

    @Test
    void tokenCarriesEmailRoleAndRestaurantId() {
        String token = service.generateToken(userWithRestaurant(77L));

        Claims claims = service.extractClaims(token);
        assertThat(claims.getSubject()).isEqualTo("owner@bistro.ge");
        assertThat(claims.get("role", String.class)).isEqualTo("OWNER");
        assertThat(service.extractEmail(token)).isEqualTo("owner@bistro.ge");
        assertThat(service.extractRestaurantId(token)).isEqualTo(77L);
    }

    @Test
    void restaurantIdIsAbsentWhenUserHasNoRestaurant() {
        AppUser u = new AppUser();
        u.setEmail("newuser@example.com");
        u.setRole(Role.OWNER);
        u.setRestaurant(null);

        String token = service.generateToken(u);

        assertThat(service.extractRestaurantId(token)).isNull();
    }

    @Test
    void tokenSignedWithDifferentSecretFailsVerification() {
        String token = service.generateToken(userWithRestaurant(77L));
        JwtService differentService = new JwtService(
                "another-secret-that-is-also-long-enough-for-hmac-sha256-!", ONE_HOUR);

        assertThatThrownBy(() -> differentService.extractEmail(token))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    void expiredTokenIsRejected() throws InterruptedException {
        JwtService shortLived = new JwtService(SECRET, 1L);
        String token = shortLived.generateToken(userWithRestaurant(77L));

        Thread.sleep(20);

        assertThatThrownBy(() -> shortLived.extractEmail(token))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }

    @Test
    void malformedTokenIsRejected() {
        assertThatThrownBy(() -> service.extractEmail("not-a-jwt"))
                .isInstanceOf(Exception.class);
    }
}
