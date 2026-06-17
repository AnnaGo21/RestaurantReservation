package org.example.reservations.reservation;

import lombok.RequiredArgsConstructor;
import org.example.reservations.auth.SecurityUtils;
import org.example.reservations.exception.ResourceNotFoundException;
import org.example.reservations.exception.TableUnavailableException;
import org.example.reservations.guest.Guest;
import org.example.reservations.guest.GuestRepository;
import org.example.reservations.restaurant.Restaurant;
import org.example.reservations.restaurant.RestaurantRepository;
import org.example.reservations.table.RestaurantTable;
import org.example.reservations.table.RestaurantTableRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantTableRepository tableRepository;
    private final GuestRepository guestRepository;
    private final ReservationMapper reservationMapper;
    private final ApplicationEventPublisher events;

    @Transactional(readOnly = true)
    public List<ReservationDto> getAllReservationsByRestaurant(Long restaurantId, LocalDateTime start, LocalDateTime end) {
        Long currentRestaurantId = SecurityUtils.currentRestaurantId();
        if (!currentRestaurantId.equals(restaurantId)) {
            throw new AccessDeniedException("Cross-restaurant access denied");
        }
        return reservationRepository.findByRestaurantIdAndStartTimeBetween(restaurantId, start, end).stream()
                .map(reservationMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReservationDto getReservationById(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));
        if (!reservation.getRestaurant().getId().equals(SecurityUtils.currentRestaurantId())) {
            throw new AccessDeniedException("Cross-restaurant access denied");
        }
        return reservationMapper.toDto(reservation);
    }

    @Transactional
    public ReservationDto createReservation(ReservationRequest request) {
        Long restaurantId = SecurityUtils.currentRestaurantId();

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));

        // Pessimistic lock on the table serializes concurrent reservation creates for it.
        RestaurantTable table = tableRepository.findByIdForUpdate(request.tableId())
                .orElseThrow(() -> new ResourceNotFoundException("Table not found"));

        if (!table.getRestaurant().getId().equals(restaurantId)) {
            throw new AccessDeniedException("Table does not belong to your restaurant");
        }

        if (request.partySize() > table.getCapacity()) {
            throw new IllegalArgumentException("Party size exceeds table capacity");
        }

        int slotMinutes = restaurant.getDefaultReservationMinutes() != null
                ? restaurant.getDefaultReservationMinutes()
                : 90;

        LocalDateTime endTime = request.startTime().plusMinutes(slotMinutes);

        List<Reservation> conflicts = reservationRepository.findConflicts(
                table.getId(),
                request.startTime(),
                endTime
        );

        if (!conflicts.isEmpty()) {
            throw new TableUnavailableException("Table is not available at the requested time");
        }

        Guest guest = guestRepository
                .findByRestaurantIdAndPhone(restaurant.getId(), request.guestPhone())
                .orElseGet(() -> {
                    Guest newGuest = new Guest();
                    newGuest.setRestaurant(restaurant);
                    newGuest.setFullName(request.guestName());
                    newGuest.setPhone(request.guestPhone());
                    newGuest.setEmail(request.guestEmail());
                    newGuest.setTotalVisits(0);
                    newGuest.setNoShowCount(0);
                    return guestRepository.save(newGuest);
                });

        Reservation reservation = new Reservation();
        reservation.setRestaurant(restaurant);
        reservation.setRestaurantTable(table);
        reservation.setGuest(guest);
        reservation.setStartTime(request.startTime());
        reservation.setEndTime(endTime);
        reservation.setPartySize(request.partySize());
        reservation.setNotes(request.notes());
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setReminderSent(false);

        reservation = reservationRepository.save(reservation);

        String formattedDateTime = reservation.getStartTime()
                .format(DateTimeFormatter.ofPattern("MMM dd 'at' HH:mm"));

        events.publishEvent(new ReservationConfirmedEvent(
                guest.getPhone(),
                guest.getFullName(),
                restaurant.getName(),
                formattedDateTime
        ));

        return reservationMapper.toDto(reservation);
    }

    @Transactional
    public ReservationDto updateReservationStatus(Long id, ReservationStatus status) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));

        if (!reservation.getRestaurant().getId().equals(SecurityUtils.currentRestaurantId())) {
            throw new AccessDeniedException("Cross-restaurant access denied");
        }

        // Idempotency: no-op if already in target state — prevents double-counting visits/no-shows.
        if (reservation.getStatus() == status) {
            return reservationMapper.toDto(reservation);
        }

        if (!reservation.getStatus().canTransitionTo(status)) {
            throw new IllegalStateException(
                    "Invalid transition: " + reservation.getStatus() + " -> " + status);
        }

        reservation.setStatus(status);

        if (status == ReservationStatus.COMPLETED) {
            Guest guest = reservation.getGuest();
            guest.setTotalVisits(guest.getTotalVisits() + 1);
            guestRepository.save(guest);
        } else if (status == ReservationStatus.NO_SHOW) {
            Guest guest = reservation.getGuest();
            guest.setNoShowCount(guest.getNoShowCount() + 1);
            guestRepository.save(guest);
        }

        reservation = reservationRepository.save(reservation);
        return reservationMapper.toDto(reservation);
    }

    @Transactional
    public ReservationDto cancelReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));

        if (!reservation.getRestaurant().getId().equals(SecurityUtils.currentRestaurantId())) {
            throw new AccessDeniedException("Cross-restaurant access denied");
        }
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            return reservationMapper.toDto(reservation);
        }
        if (!reservation.getStatus().canTransitionTo(ReservationStatus.CANCELLED)) {
            throw new IllegalStateException(
                    "Cannot cancel reservation in state " + reservation.getStatus());
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation = reservationRepository.save(reservation);
        return reservationMapper.toDto(reservation);
    }

    @Transactional
    public ReservationDto createWalkIn(WalkInRequest request) {
        Long restaurantId = SecurityUtils.currentRestaurantId();

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));

        RestaurantTable table = tableRepository.findByIdForUpdate(request.tableId())
                .orElseThrow(() -> new ResourceNotFoundException("Table not found"));

        if (!table.getRestaurant().getId().equals(restaurantId)) {
            throw new AccessDeniedException("Table does not belong to your restaurant");
        }
        if (request.partySize() > table.getCapacity()) {
            throw new IllegalArgumentException("Party size exceeds table capacity");
        }

        LocalDateTime startTime = LocalDateTime.now();
        int slotMinutes = restaurant.getDefaultReservationMinutes() != null
                ? restaurant.getDefaultReservationMinutes() : 90;
        LocalDateTime endTime = startTime.plusMinutes(slotMinutes);

        List<Reservation> conflicts = reservationRepository.findConflicts(table.getId(), startTime, endTime);
        if (!conflicts.isEmpty()) {
            throw new TableUnavailableException("Table is occupied right now");
        }

        // Phone is optional for walk-ins; synthesize a unique placeholder so guest
        // dedup by (restaurant, phone) doesn't merge anonymous walk-ins together.
        String phone = (request.guestPhone() == null || request.guestPhone().isBlank())
                ? "walkin-" + UUID.randomUUID()
                : request.guestPhone();

        Guest guest = guestRepository
                .findByRestaurantIdAndPhone(restaurant.getId(), phone)
                .orElseGet(() -> {
                    Guest g = new Guest();
                    g.setRestaurant(restaurant);
                    g.setFullName(request.guestName());
                    g.setPhone(phone);
                    g.setTotalVisits(0);
                    g.setNoShowCount(0);
                    return guestRepository.save(g);
                });

        Reservation reservation = new Reservation();
        reservation.setRestaurant(restaurant);
        reservation.setRestaurantTable(table);
        reservation.setGuest(guest);
        reservation.setStartTime(startTime);
        reservation.setEndTime(endTime);
        reservation.setPartySize(request.partySize());
        reservation.setNotes(request.notes());
        reservation.setStatus(ReservationStatus.SEATED);
        reservation.setCheckedInAt(startTime);
        reservation.setReminderSent(true); // never send reminder for someone already seated

        reservation = reservationRepository.save(reservation);
        return reservationMapper.toDto(reservation);
    }

    @Transactional
    public ReservationDto moveReservation(Long id, MoveReservationRequest request) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));

        Long restaurantId = SecurityUtils.currentRestaurantId();
        if (!reservation.getRestaurant().getId().equals(restaurantId)) {
            throw new AccessDeniedException("Cross-restaurant access denied");
        }

        ReservationStatus s = reservation.getStatus();
        if (s == ReservationStatus.COMPLETED || s == ReservationStatus.CANCELLED || s == ReservationStatus.NO_SHOW) {
            throw new IllegalStateException("Cannot move reservation in state " + s);
        }

        Long newTableId = request.tableId() != null ? request.tableId() : reservation.getRestaurantTable().getId();
        LocalDateTime newStart = request.startTime() != null ? request.startTime() : reservation.getStartTime();

        RestaurantTable table = tableRepository.findByIdForUpdate(newTableId)
                .orElseThrow(() -> new ResourceNotFoundException("Table not found"));

        if (!table.getRestaurant().getId().equals(restaurantId)) {
            throw new AccessDeniedException("Table does not belong to your restaurant");
        }
        if (reservation.getPartySize() > table.getCapacity()) {
            throw new IllegalArgumentException("Party size exceeds table capacity");
        }

        int slotMinutes = reservation.getRestaurant().getDefaultReservationMinutes() != null
                ? reservation.getRestaurant().getDefaultReservationMinutes() : 90;
        LocalDateTime newEnd = newStart.plusMinutes(slotMinutes);

        List<Reservation> conflicts = reservationRepository.findConflictsExcluding(
                table.getId(), newStart, newEnd, reservation.getId());
        if (!conflicts.isEmpty()) {
            throw new TableUnavailableException("Table is not available at the requested time");
        }

        reservation.setRestaurantTable(table);
        reservation.setStartTime(newStart);
        reservation.setEndTime(newEnd);
        reservation = reservationRepository.save(reservation);
        return reservationMapper.toDto(reservation);
    }

    @Transactional
    public ReservationDto checkIn(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));

        if (!reservation.getRestaurant().getId().equals(SecurityUtils.currentRestaurantId())) {
            throw new AccessDeniedException("Cross-restaurant access denied");
        }
        if (reservation.getStatus() == ReservationStatus.SEATED) {
            return reservationMapper.toDto(reservation); // idempotent
        }
        if (!reservation.getStatus().canTransitionTo(ReservationStatus.SEATED)) {
            throw new IllegalStateException(
                    "Cannot check in reservation in state " + reservation.getStatus());
        }

        reservation.setStatus(ReservationStatus.SEATED);
        reservation.setCheckedInAt(LocalDateTime.now());
        reservation = reservationRepository.save(reservation);
        return reservationMapper.toDto(reservation);
    }

    @Transactional(readOnly = true)
    public List<Long> getAvailableTables(Long restaurantId, LocalDateTime startTime, LocalDateTime endTime) {
        Long currentRestaurantId = SecurityUtils.currentRestaurantId();
        if (!currentRestaurantId.equals(restaurantId)) {
            throw new AccessDeniedException("Cross-restaurant access denied");
        }
        List<RestaurantTable> allTables = tableRepository.findByRestaurantId(restaurantId);

        return allTables.stream()
                .map(RestaurantTable::getId)
                .filter(id -> {
                    List<Reservation> conflicts = reservationRepository.findConflicts(
                            id,
                            startTime,
                            endTime
                    );
                    return conflicts.isEmpty();
                })
                .collect(Collectors.toList());
    }
}
