package org.example.reservations.reservation;

import lombok.RequiredArgsConstructor;
import org.example.reservations.exception.ResourceNotFoundException;
import org.example.reservations.exception.TableUnavailableException;
import org.example.reservations.guest.Guest;
import org.example.reservations.guest.GuestRepository;
import org.example.reservations.notification.SmsService;
import org.example.reservations.restaurant.Restaurant;
import org.example.reservations.restaurant.RestaurantRepository;
import org.example.reservations.table.RestaurantTable;
import org.example.reservations.table.RestaurantTableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantTableRepository tableRepository;
    private final GuestRepository guestRepository;
    private final SmsService smsService;
    private final ReservationMapper reservationMapper;

    @Transactional(readOnly = true)
    public List<ReservationDto> getAllReservationsByRestaurant(Long restaurantId, LocalDateTime start, LocalDateTime end) {
        return reservationRepository.findByRestaurantIdAndStartTimeBetween(restaurantId, start, end).stream()
                .map(reservationMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReservationDto getReservationById(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));
        return reservationMapper.toDto(reservation);
    }

    @Transactional
    public ReservationDto createReservation(ReservationRequest request) {
        Restaurant restaurant = restaurantRepository.findById(request.restaurantId())
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));

        RestaurantTable table = tableRepository.findById(request.tableId())
                .orElseThrow(() -> new ResourceNotFoundException("Table not found"));

        if (!table.getRestaurant().getId().equals(restaurant.getId())) {
            throw new IllegalArgumentException("Table does not belong to restaurant");
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
        smsService.sendConfirmation(
                guest.getPhone(),
                guest.getFullName(),
                restaurant.getName(),
                formattedDateTime
        );

        return reservationMapper.toDto(reservation);
    }

    @Transactional
    public ReservationDto updateReservationStatus(Long id, ReservationStatus status) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));

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

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation = reservationRepository.save(reservation);
        return reservationMapper.toDto(reservation);
    }

    @Transactional(readOnly = true)
    public List<Long> getAvailableTables(Long restaurantId, LocalDateTime startTime, LocalDateTime endTime) {
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
