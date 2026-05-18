package org.example.reservations.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Get all users for a restaurant (OWNER only)
     */
    @GetMapping
    @PreAuthorize("hasRole('OWNER')")
    public List<AppUserDto> getUsersByRestaurant(@RequestParam Long restaurantId) {
        return userService.getUsersByRestaurant(restaurantId);
    }

    /**
     * Get user by ID (OWNER only)
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public AppUserDto getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    /**
     * Get current user's profile (any authenticated user)
     */
    @GetMapping("/me")
    public AppUserDto getCurrentUser() {
        return userService.getCurrentUser();
    }

    /**
     * OWNER creates a new staff member
     * SECURITY: Only OWNER can create users and assign roles
     */
    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public AppUserDto createUser(@Valid @RequestBody CreateUserRequest request) {
        return userService.createUser(request);
    }

    /**
     * User updates their OWN profile
     * SECURITY: Can only update name and email, NOT role or restaurant!
     */
    @PutMapping("/me")
    public AppUserDto updateOwnProfile(@Valid @RequestBody UpdateUserProfileRequest request) {
        return userService.updateOwnProfile(request);
    }

    /**
     * User changes their password
     * SECURITY: Must provide current password
     */
    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return ResponseEntity.ok().build();
    }

    /**
     * OWNER deletes a user (OWNER only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok().build();
    }
}
