package org.example.reservations.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("""
        select r from Reservation r
        where r.restaurantTable.id = :tableId
          and r.status not in ('CANCELLED', 'NO_SHOW', 'COMPLETED')
          and r.startTime < :endTime
          and r.endTime > :startTime
    """)
    List<Reservation> findConflicts(Long tableId, LocalDateTime startTime, LocalDateTime endTime);

    @Query("""
        select r from Reservation r
        where r.restaurantTable.id = :tableId
          and r.id <> :excludeId
          and r.status not in ('CANCELLED', 'NO_SHOW', 'COMPLETED')
          and r.startTime < :endTime
          and r.endTime > :startTime
    """)
    List<Reservation> findConflictsExcluding(Long tableId, LocalDateTime startTime, LocalDateTime endTime, Long excludeId);

    List<Reservation> findByRestaurantIdAndStartTimeBetween(
            Long restaurantId,
            LocalDateTime start,
            LocalDateTime end
    );

    long countByRestaurantId(Long restaurantId);

    long countByRestaurantIdAndStatus(Long restaurantId, ReservationStatus status);

    // Used by SmsReminderScheduler: CONFIRMED reservations starting between `from` and `until` with no reminder sent yet
    List<Reservation> findByStatusAndReminderSentFalseAndStartTimeBetween(
            ReservationStatus status, LocalDateTime from, LocalDateTime until);
}
