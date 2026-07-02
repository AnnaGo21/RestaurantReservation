package org.example.reservations.auth;

import jakarta.servlet.FilterChain;
import org.example.reservations.restaurant.Restaurant;
import org.example.reservations.user.AppUser;
import org.example.reservations.user.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * JwtService is instantiated with a real HMAC key rather than mocked because
 * Mockito's inline mock maker cannot retransform it under JDK 25.
 * Using the real service also verifies the whole sign→parse→extract path.
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String SECRET = "test-secret-that-is-long-enough-for-hmac-sha256-signing!!";

    @Mock UserDetailsService userDetailsService;
    @Mock FilterChain filterChain;

    JwtService jwtService;
    JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 3_600_000L);
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static UserDetails userDetails() {
        return User.builder().username("u@x").password("p").roles("OWNER").build();
    }

    private String validTokenFor(String email, long restaurantId) {
        Restaurant r = new Restaurant();
        r.setId(restaurantId);
        AppUser u = new AppUser();
        u.setEmail(email);
        u.setRole(Role.OWNER);
        u.setRestaurant(r);
        return jwtService.generateToken(u);
    }

    @Test
    void validTokenPopulatesAuthenticationWithRestaurantIdInDetails() throws Exception {
        String token = validTokenFor("u@x", 42L);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(userDetailsService.loadUserByUsername("u@x")).thenReturn(userDetails());

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getDetails()).isEqualTo(42L);
        assertThat(auth.isAuthenticated()).isTrue();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void requestWithoutAuthorizationHeaderPassesThroughUnauthenticated() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void authorizationHeaderWithoutBearerPrefixIsIgnored() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void invalidTokenIsSwallowedAndChainContinues() throws Exception {
        // Filter must NOT throw on bad tokens — security chain returns 401.
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer garbage-not-a-jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void tokenSignedWithWrongSecretIsRejected() throws Exception {
        JwtService attacker = new JwtService(
                "another-secret-that-is-also-long-enough-for-hmac-sha256!!!", 3_600_000L);
        AppUser u = new AppUser();
        u.setEmail("attacker@x");
        u.setRole(Role.OWNER);
        Restaurant r = new Restaurant();
        r.setId(1L);
        u.setRestaurant(r);
        String forged = attacker.generateToken(u);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + forged);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void doesNotReauthenticateWhenContextAlreadyHasAuthentication() throws Exception {
        String token = validTokenFor("u@x", 42L);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        var preExisting = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "someone-else", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(preExisting);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isSameAs(preExisting);
        verifyNoInteractions(userDetailsService);
    }
}
