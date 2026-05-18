package org.example.reservations.user;

import lombok.RequiredArgsConstructor;
import org.example.reservations.exception.ResourceNotFoundException;
import org.example.reservations.restaurant.Restaurant;
import org.example.reservations.restaurant.RestaurantRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final AppUserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Get all users for a specific restaurant
     * Only accessible by OWNER of that restaurant
     */
    @Transactional(readOnly = true)
    public List<AppUserDto> getUsersByRestaurant(Long restaurantId) {
        return userRepository.findAll().stream()
                .filter(user -> user.getRestaurant().getId().equals(restaurantId))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get user by ID
     */
    @Transactional(readOnly = true)
    public AppUserDto getUserById(Long id) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return toDto(user);
    }

    /**
     * Get current authenticated user's profile
     */
    @Transactional(readOnly = true)
    public AppUserDto getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
        return toDto(user);
    }

    /**
     * OWNER creates a new staff member (MANAGER or STAFF)
     * SECURITY: Only OWNER can create users and assign roles
     */
    @Transactional
    public AppUserDto createUser(CreateUserRequest request) {
        Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        AppUser user = new AppUser();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setRestaurant(restaurant);

        user = userRepository.save(user);
        return toDto(user);
    }

    /**
     * User updates their OWN profile (name, email only)
     * SECURITY: Cannot change role or restaurant!
     */
    @Transactional
    public AppUserDto updateOwnProfile(UpdateUserProfileRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Only update safe fields
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());

        // Role and restaurant remain unchanged!
        user = userRepository.save(user);
        return toDto(user);
    }

    /**
     * User changes their password
     * SECURITY: Must provide current password to change
     */
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        // Set new password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    /**
     * OWNER deletes a user (staff member)
     */
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    /**
     * Convert entity to DTO (never expose password!)
     */
    private AppUserDto toDto(AppUser user) {
        return new AppUserDto(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().name(),
                user.getRestaurant().getId()
        );
    }
}
