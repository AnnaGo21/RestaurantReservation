package org.example.reservations.reservation;

import org.example.reservations.exception.ResourceNotFoundException;
import org.example.reservations.exception.TableUnavailableException;
import org.example.reservations.guest.Guest;
import org.example.reservations.guest.GuestRepository;
import org.example.reservations.restaurant.Restaurant;
import org.example.reservations.restaurant.RestaurantRepository;
import org.example.reservations.table.RestaurantTable;
import org.example.reservations.table.RestaurantTableRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    private static final Long TENANT_ID = 100L;
    private static final Long OTHER_TENANT_ID = 999L;

    @Mock ReservationRepository reservationRepository;
    @Mock RestaurantRepository restaurantRepository;
    @Mock RestaurantTableRepository tableRepository;
    @Mock GuestRepository guestRepository;
    @Mock ApplicationEventPublisher events;

    // Use the real mapper — trivial pure function; mocking it hides bugs and adds noise.
    private final ReservationMapper reservationMapper = new ReservationMapper();

    ReservationService service;

    Restaurant restaurant;
    RestaurantTable table;

    @BeforeEach
    void setUp() {
        service = new ReservationService(
                reservationRepository,
                restaurantRepository,
                tableRepository,
                guestRepository,
                reservationMapper,
                events
        );

        restaurant = new Restaurant();
        restaurant.setId(TENANT_ID);
        restaurant.setName("Test Bistro");
        restaurant.setDefaultReservationMinutes(90);
        restaurant.setGracePeriodMinutes(15);

        table = new RestaurantTable();
        table.setId(10L);
        table.setCapacity(4);
        table.setRestaurant(restaurant);

        authenticateAs(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static void authenticateAs(Long restaurantId) {
        var auth = new UsernamePasswordAuthenticationToken("user@example.com", null, List.of());
        auth.setDetails(restaurantId);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private ReservationRequest sampleRequest() {
        return new ReservationRequest(
                table.getId(),
                "John Doe",
                "+995555111222",
                "john@example.com",
                LocalDateTime.of(2026, 7, 5, 19, 0),
                2,
                "Window seat"
        );
    }

    private Guest existingGuest() {
        Guest g = new Guest();
        g.setId(50L);
        g.setRestaurant(restaurant);
        g.setFullName("John Doe");
        g.setPhone("+995555111222");
        g.setTotalVisits(3);
        g.setNoShowCount(1);
        return g;
    }

    private Reservation savedReservation(Guest guest, ReservationStatus status) {
        Reservation r = new Reservation();
        r.setId(500L);
        r.setRestaurant(restaurant);
        r.setRestaurantTable(table);
        r.setGuest(guest);
        r.setStartTime(LocalDateTime.of(2026, 7, 5, 19, 0));
        r.setEndTime(LocalDateTime.of(2026, 7, 5, 20, 30));
        r.setPartySize(2);
        r.setStatus(status);
        r.setReminderSent(false);
        return r;
    }

    // -------------------- createReservation --------------------

    @Nested
    @DisplayName("createReservation")
    class CreateReservation {

        @Test
        @DisplayName("creates CONFIRMED reservation, saves guest, and publishes confirmation event")
        void createsReservationSuccessfully() {
            when(restaurantRepository.findById(TENANT_ID)).thenReturn(Optional.of(restaurant));
            when(tableRepository.findByIdForUpdate(table.getId())).thenReturn(Optional.of(table));
            when(reservationRepository.findConflicts(eq(table.getId()), any(), any())).thenReturn(List.of());

            Guest guest = existingGuest();
            when(guestRepository.findByRestaurantIdAndPhone(TENANT_ID, "+995555111222"))
                    .thenReturn(Optional.of(guest));

            ArgumentCaptor<Reservation> saved = ArgumentCaptor.forClass(Reservation.class);
            when(reservationRepository.save(saved.capture())).thenAnswer(inv -> {
                Reservation r = inv.getArgument(0);
                r.setId(500L);
                return r;
            });

            ReservationDto dto = service.createReservation(sampleRequest());

            Reservation persisted = saved.getValue();
            assertThat(persisted.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
            assertThat(persisted.getStartTime()).isEqualTo(LocalDateTime.of(2026, 7, 5, 19, 0));
            // endTime = start + defaultReservationMinutes (90)
            assertThat(persisted.getEndTime()).isEqualTo(LocalDateTime.of(2026, 7, 5, 20, 30));
            assertThat(persisted.getReminderSent()).isFalse();
            assertThat(dto.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);

            verify(events).publishEvent(any(ReservationConfirmedEvent.class));
        }

        @Test
        @DisplayName("creates a new guest row when phone is not on file")
        void createsGuestOnFirstBooking() {
            when(restaurantRepository.findById(TENANT_ID)).thenReturn(Optional.of(restaurant));
            when(tableRepository.findByIdForUpdate(table.getId())).thenReturn(Optional.of(table));
            when(reservationRepository.findConflicts(anyLong(), any(), any())).thenReturn(List.of());
            when(guestRepository.findByRestaurantIdAndPhone(TENANT_ID, "+995555111222"))
                    .thenReturn(Optional.empty());

            ArgumentCaptor<Guest> guestCap = ArgumentCaptor.forClass(Guest.class);
            when(guestRepository.save(guestCap.capture())).thenAnswer(inv -> {
                Guest g = inv.getArgument(0);
                g.setId(51L);
                return g;
            });
            when(reservationRepository.save(any())).thenAnswer(inv -> {
                Reservation r = inv.getArgument(0);
                r.setId(500L);
                return r;
            });

            service.createReservation(sampleRequest());

            Guest newGuest = guestCap.getValue();
            assertThat(newGuest.getPhone()).isEqualTo("+995555111222");
            assertThat(newGuest.getRestaurant().getId()).isEqualTo(TENANT_ID);
            assertThat(newGuest.getTotalVisits()).isZero();
            assertThat(newGuest.getNoShowCount()).isZero();
        }

        @Test
        @DisplayName("uses pessimistic lock on the target table")
        void locksTableForUpdate() {
            when(restaurantRepository.findById(TENANT_ID)).thenReturn(Optional.of(restaurant));
            when(tableRepository.findByIdForUpdate(table.getId())).thenReturn(Optional.of(table));
            when(reservationRepository.findConflicts(anyLong(), any(), any())).thenReturn(List.of());
            when(guestRepository.findByRestaurantIdAndPhone(anyLong(), anyString()))
                    .thenReturn(Optional.of(existingGuest()));
            when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.createReservation(sampleRequest());

            verify(tableRepository).findByIdForUpdate(table.getId());
            verify(tableRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("rejects overlapping reservation with TableUnavailableException")
        void rejectsOverlappingReservation() {
            when(restaurantRepository.findById(TENANT_ID)).thenReturn(Optional.of(restaurant));
            when(tableRepository.findByIdForUpdate(table.getId())).thenReturn(Optional.of(table));
            when(reservationRepository.findConflicts(anyLong(), any(), any()))
                    .thenReturn(List.of(new Reservation()));

            assertThatThrownBy(() -> service.createReservation(sampleRequest()))
                    .isInstanceOf(TableUnavailableException.class);

            verify(reservationRepository, never()).save(any());
            verify(events, never()).publishEvent(any());
        }

        @Test
        @DisplayName("rejects when party size exceeds table capacity")
        void rejectsWhenPartyTooLarge() {
            table.setCapacity(2);
            when(restaurantRepository.findById(TENANT_ID)).thenReturn(Optional.of(restaurant));
            when(tableRepository.findByIdForUpdate(table.getId())).thenReturn(Optional.of(table));

            ReservationRequest req = new ReservationRequest(
                    table.getId(), "John", "+9955", null,
                    LocalDateTime.of(2026, 7, 5, 19, 0),
                    5, null
            );

            assertThatThrownBy(() -> service.createReservation(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("capacity");

            verify(reservationRepository, never()).findConflicts(anyLong(), any(), any());
            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejects when the table belongs to another restaurant (cross-tenant guard)")
        void rejectsCrossTenantTable() {
            Restaurant otherRestaurant = new Restaurant();
            otherRestaurant.setId(OTHER_TENANT_ID);
            table.setRestaurant(otherRestaurant);

            when(restaurantRepository.findById(TENANT_ID)).thenReturn(Optional.of(restaurant));
            when(tableRepository.findByIdForUpdate(table.getId())).thenReturn(Optional.of(table));

            assertThatThrownBy(() -> service.createReservation(sampleRequest()))
                    .isInstanceOf(AccessDeniedException.class);

            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("does not send SMS synchronously — publishes event instead")
        void doesNotCallSmsInline() {
            when(restaurantRepository.findById(TENANT_ID)).thenReturn(Optional.of(restaurant));
            when(tableRepository.findByIdForUpdate(table.getId())).thenReturn(Optional.of(table));
            when(reservationRepository.findConflicts(anyLong(), any(), any())).thenReturn(List.of());
            when(guestRepository.findByRestaurantIdAndPhone(anyLong(), anyString()))
                    .thenReturn(Optional.of(existingGuest()));
            when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.createReservation(sampleRequest());

            // The Twilio call must happen after commit via listener, not during the transaction.
            verify(events, times(1)).publishEvent(any(ReservationConfirmedEvent.class));
        }
    }

    // -------------------- walk-in --------------------

    @Nested
    @DisplayName("createWalkIn")
    class WalkIn {

        @Test
        @DisplayName("creates SEATED reservation with checkedInAt set and reminderSent=true")
        void createsSeatedWalkIn() {
            when(restaurantRepository.findById(TENANT_ID)).thenReturn(Optional.of(restaurant));
            when(tableRepository.findByIdForUpdate(table.getId())).thenReturn(Optional.of(table));
            when(reservationRepository.findConflicts(anyLong(), any(), any())).thenReturn(List.of());
            when(guestRepository.findByRestaurantIdAndPhone(anyLong(), anyString())).thenReturn(Optional.of(existingGuest()));

            ArgumentCaptor<Reservation> cap = ArgumentCaptor.forClass(Reservation.class);
            when(reservationRepository.save(cap.capture())).thenAnswer(inv -> {
                Reservation r = inv.getArgument(0);
                r.setId(600L);
                return r;
            });

            service.createWalkIn(new WalkInRequest(table.getId(), "Ana", "+995111", 2, null));

            Reservation r = cap.getValue();
            assertThat(r.getStatus()).isEqualTo(ReservationStatus.SEATED);
            assertThat(r.getCheckedInAt()).isNotNull();
            assertThat(r.getReminderSent()).isTrue();
            // No confirmation event for walk-ins (guest is already at the table).
            verify(events, never()).publishEvent(any());
        }

        @Test
        @DisplayName("synthesizes a unique walkin phone when guestPhone is blank so anonymous walk-ins do not merge")
        void anonymousWalkInGetsSyntheticPhone() {
            when(restaurantRepository.findById(TENANT_ID)).thenReturn(Optional.of(restaurant));
            when(tableRepository.findByIdForUpdate(table.getId())).thenReturn(Optional.of(table));
            when(reservationRepository.findConflicts(anyLong(), any(), any())).thenReturn(List.of());
            when(guestRepository.findByRestaurantIdAndPhone(anyLong(), anyString())).thenReturn(Optional.empty());

            ArgumentCaptor<Guest> guestCap = ArgumentCaptor.forClass(Guest.class);
            when(guestRepository.save(guestCap.capture())).thenAnswer(inv -> {
                Guest g = inv.getArgument(0);
                g.setId(77L);
                return g;
            });
            when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.createWalkIn(new WalkInRequest(table.getId(), "Walk-in", "  ", 2, null));

            assertThat(guestCap.getValue().getPhone()).startsWith("walkin-");
        }

        @Test
        @DisplayName("rejects walk-in when table is currently occupied")
        void rejectsWhenTableOccupied() {
            when(restaurantRepository.findById(TENANT_ID)).thenReturn(Optional.of(restaurant));
            when(tableRepository.findByIdForUpdate(table.getId())).thenReturn(Optional.of(table));
            when(reservationRepository.findConflicts(anyLong(), any(), any()))
                    .thenReturn(List.of(new Reservation()));

            assertThatThrownBy(() -> service.createWalkIn(
                    new WalkInRequest(table.getId(), "Ana", "+995111", 2, null)))
                    .isInstanceOf(TableUnavailableException.class);
        }
    }

    // -------------------- move / reschedule --------------------

    @Nested
    @DisplayName("moveReservation")
    class Move {

        @Test
        @DisplayName("moves reservation to a new table and updates window")
        void movesToNewTable() {
            Reservation existing = savedReservation(existingGuest(), ReservationStatus.CONFIRMED);
            when(reservationRepository.findById(500L)).thenReturn(Optional.of(existing));

            RestaurantTable newTable = new RestaurantTable();
            newTable.setId(11L);
            newTable.setCapacity(4);
            newTable.setRestaurant(restaurant);

            when(tableRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(newTable));
            when(reservationRepository.findConflictsExcluding(eq(11L), any(), any(), eq(500L)))
                    .thenReturn(List.of());
            when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            LocalDateTime newStart = LocalDateTime.of(2026, 7, 5, 20, 0);
            service.moveReservation(500L, new MoveReservationRequest(11L, newStart));

            assertThat(existing.getRestaurantTable().getId()).isEqualTo(11L);
            assertThat(existing.getStartTime()).isEqualTo(newStart);
            assertThat(existing.getEndTime()).isEqualTo(newStart.plusMinutes(90));
        }

        @Test
        @DisplayName("uses findConflictsExcluding so the reservation does not conflict with itself")
        void excludesSelfFromConflictCheck() {
            Reservation existing = savedReservation(existingGuest(), ReservationStatus.CONFIRMED);
            when(reservationRepository.findById(500L)).thenReturn(Optional.of(existing));
            when(tableRepository.findByIdForUpdate(anyLong())).thenReturn(Optional.of(table));
            when(reservationRepository.findConflictsExcluding(anyLong(), any(), any(), eq(500L)))
                    .thenReturn(List.of());
            when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.moveReservation(500L, new MoveReservationRequest(null,
                    LocalDateTime.of(2026, 7, 5, 20, 0)));

            verify(reservationRepository).findConflictsExcluding(anyLong(), any(), any(), eq(500L));
            verify(reservationRepository, never()).findConflicts(anyLong(), any(), any());
        }

        @Test
        @DisplayName("rejects moving a terminal reservation")
        void rejectsTerminal() {
            Reservation completed = savedReservation(existingGuest(), ReservationStatus.COMPLETED);
            when(reservationRepository.findById(500L)).thenReturn(Optional.of(completed));

            assertThatThrownBy(() -> service.moveReservation(500L,
                    new MoveReservationRequest(11L, LocalDateTime.now())))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("rejects when new table would conflict with another reservation")
        void rejectsWhenConflict() {
            Reservation existing = savedReservation(existingGuest(), ReservationStatus.CONFIRMED);
            when(reservationRepository.findById(500L)).thenReturn(Optional.of(existing));
            when(tableRepository.findByIdForUpdate(anyLong())).thenReturn(Optional.of(table));
            when(reservationRepository.findConflictsExcluding(anyLong(), any(), any(), anyLong()))
                    .thenReturn(List.of(new Reservation()));

            assertThatThrownBy(() -> service.moveReservation(500L,
                    new MoveReservationRequest(null, LocalDateTime.of(2026, 7, 5, 21, 0))))
                    .isInstanceOf(TableUnavailableException.class);
        }
    }

    // -------------------- status updates / lifecycle --------------------

    @Nested
    @DisplayName("updateReservationStatus")
    class UpdateStatus {

        @Test
        @DisplayName("CONFIRMED -> COMPLETED increments guest.totalVisits exactly once")
        void completingIncrementsVisits() {
            Guest guest = existingGuest();
            int visitsBefore = guest.getTotalVisits();
            Reservation r = savedReservation(guest, ReservationStatus.CONFIRMED);

            when(reservationRepository.findById(500L)).thenReturn(Optional.of(r));
            when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.updateReservationStatus(500L, ReservationStatus.COMPLETED);

            assertThat(guest.getTotalVisits()).isEqualTo(visitsBefore + 1);
            verify(guestRepository).save(guest);
        }

        @Test
        @DisplayName("CONFIRMED -> NO_SHOW increments guest.noShowCount exactly once")
        void noShowIncrementsNoShows() {
            Guest guest = existingGuest();
            int noShowsBefore = guest.getNoShowCount();
            Reservation r = savedReservation(guest, ReservationStatus.CONFIRMED);

            when(reservationRepository.findById(500L)).thenReturn(Optional.of(r));
            when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.updateReservationStatus(500L, ReservationStatus.NO_SHOW);

            assertThat(guest.getNoShowCount()).isEqualTo(noShowsBefore + 1);
            verify(guestRepository).save(guest);
        }

        @Test
        @DisplayName("idempotent: re-applying the same COMPLETED status does not double-count visits")
        void idempotentCompleted() {
            Guest guest = existingGuest();
            int visitsBefore = guest.getTotalVisits();
            Reservation r = savedReservation(guest, ReservationStatus.COMPLETED);

            when(reservationRepository.findById(500L)).thenReturn(Optional.of(r));

            service.updateReservationStatus(500L, ReservationStatus.COMPLETED);

            assertThat(guest.getTotalVisits()).isEqualTo(visitsBefore);
            verify(guestRepository, never()).save(any());
            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("idempotent: re-applying the same NO_SHOW status does not double-count no-shows")
        void idempotentNoShow() {
            Guest guest = existingGuest();
            int noShowsBefore = guest.getNoShowCount();
            Reservation r = savedReservation(guest, ReservationStatus.NO_SHOW);

            when(reservationRepository.findById(500L)).thenReturn(Optional.of(r));

            service.updateReservationStatus(500L, ReservationStatus.NO_SHOW);

            assertThat(guest.getNoShowCount()).isEqualTo(noShowsBefore);
            verify(guestRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejects invalid transitions with IllegalStateException (→ HTTP 409)")
        void rejectsInvalidTransition() {
            Reservation r = savedReservation(existingGuest(), ReservationStatus.COMPLETED);
            when(reservationRepository.findById(500L)).thenReturn(Optional.of(r));

            assertThatThrownBy(() -> service.updateReservationStatus(500L, ReservationStatus.SEATED))
                    .isInstanceOf(IllegalStateException.class);

            verify(reservationRepository, never()).save(any());
            verify(guestRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws AccessDenied when reservation belongs to another restaurant")
        void rejectsCrossTenant() {
            Restaurant other = new Restaurant();
            other.setId(OTHER_TENANT_ID);
            Reservation r = savedReservation(existingGuest(), ReservationStatus.CONFIRMED);
            r.setRestaurant(other);

            when(reservationRepository.findById(500L)).thenReturn(Optional.of(r));

            assertThatThrownBy(() -> service.updateReservationStatus(500L, ReservationStatus.COMPLETED))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("throws ResourceNotFound when reservation id is unknown")
        void notFoundBubbles() {
            when(reservationRepository.findById(anyLong())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateReservationStatus(9999L, ReservationStatus.COMPLETED))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // -------------------- cancel --------------------

    @Nested
    @DisplayName("cancelReservation")
    class Cancel {

        @Test
        @DisplayName("cancels a CONFIRMED reservation")
        void cancelsConfirmed() {
            Reservation r = savedReservation(existingGuest(), ReservationStatus.CONFIRMED);
            when(reservationRepository.findById(500L)).thenReturn(Optional.of(r));
            when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ReservationDto dto = service.cancelReservation(500L);

            assertThat(dto.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        }

        @Test
        @DisplayName("idempotent: cancelling an already-cancelled reservation is a no-op")
        void cancelIdempotent() {
            Reservation r = savedReservation(existingGuest(), ReservationStatus.CANCELLED);
            when(reservationRepository.findById(500L)).thenReturn(Optional.of(r));

            service.cancelReservation(500L);

            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("cannot cancel a COMPLETED reservation")
        void cannotCancelCompleted() {
            Reservation r = savedReservation(existingGuest(), ReservationStatus.COMPLETED);
            when(reservationRepository.findById(500L)).thenReturn(Optional.of(r));

            assertThatThrownBy(() -> service.cancelReservation(500L))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // -------------------- check-in --------------------

    @Nested
    @DisplayName("checkIn")
    class CheckIn {

        @Test
        @DisplayName("sets SEATED and stamps checkedInAt")
        void checksIn() {
            Reservation r = savedReservation(existingGuest(), ReservationStatus.CONFIRMED);
            when(reservationRepository.findById(500L)).thenReturn(Optional.of(r));
            when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            LocalDateTime before = LocalDateTime.now();
            service.checkIn(500L);

            assertThat(r.getStatus()).isEqualTo(ReservationStatus.SEATED);
            assertThat(r.getCheckedInAt()).isNotNull();
            assertThat(r.getCheckedInAt()).isAfterOrEqualTo(before);
        }

        @Test
        @DisplayName("idempotent: second check-in does not overwrite checkedInAt or resave")
        void checkInIdempotent() {
            Reservation r = savedReservation(existingGuest(), ReservationStatus.SEATED);
            LocalDateTime original = LocalDateTime.of(2026, 7, 5, 19, 0);
            r.setCheckedInAt(original);
            when(reservationRepository.findById(500L)).thenReturn(Optional.of(r));

            service.checkIn(500L);

            assertThat(r.getCheckedInAt()).isEqualTo(original);
            verify(reservationRepository, never()).save(any());
        }
    }

    // -------------------- reads / cross-tenant --------------------

    @Nested
    @DisplayName("read-side tenant guards")
    class ReadGuards {

        @Test
        @DisplayName("getReservationById rejects cross-restaurant access")
        void getByIdRejectsCrossTenant() {
            Restaurant other = new Restaurant();
            other.setId(OTHER_TENANT_ID);
            Reservation r = savedReservation(existingGuest(), ReservationStatus.CONFIRMED);
            r.setRestaurant(other);

            when(reservationRepository.findById(500L)).thenReturn(Optional.of(r));

            assertThatThrownBy(() -> service.getReservationById(500L))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("getAllReservationsByRestaurant rejects mismatched restaurantId param")
        void listRejectsMismatchedTenant() {
            assertThatThrownBy(() -> service.getAllReservationsByRestaurant(
                    OTHER_TENANT_ID,
                    LocalDateTime.now(),
                    LocalDateTime.now().plusHours(1)))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("getAvailableTables rejects mismatched restaurantId param")
        void availableTablesRejectsMismatchedTenant() {
            assertThatThrownBy(() -> service.getAvailableTables(
                    OTHER_TENANT_ID,
                    LocalDateTime.now(),
                    LocalDateTime.now().plusHours(1)))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("getAvailableTables returns only tables with no conflicts in the window")
        void availableTablesFiltersOutBusy() {
            RestaurantTable t1 = new RestaurantTable();
            t1.setId(10L);
            t1.setRestaurant(restaurant);
            RestaurantTable t2 = new RestaurantTable();
            t2.setId(11L);
            t2.setRestaurant(restaurant);

            when(tableRepository.findByRestaurantId(TENANT_ID)).thenReturn(List.of(t1, t2));
            when(reservationRepository.findConflicts(eq(10L), any(), any())).thenReturn(List.of());
            when(reservationRepository.findConflicts(eq(11L), any(), any()))
                    .thenReturn(List.of(new Reservation()));

            List<Long> ids = service.getAvailableTables(TENANT_ID,
                    LocalDateTime.of(2026, 7, 5, 19, 0),
                    LocalDateTime.of(2026, 7, 5, 20, 30));

            assertThat(ids).containsExactly(10L);
        }
    }
}
