package com.example.movie;

import com.example.movie.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.*;

/** Pure domain tests: no Spring context or database. */
class ScheduleDomainTest {
 private final LocalDateTime now = LocalDateTime.of(2026, 10, 24, 12, 0);
 private Theater theater;
 private Room room;
 private Movie movie;
 private Showtime showtime;

 @BeforeEach
 void setup() {
  theater = new Theater();
  room = new Room();
  room.setTheater(theater);
  movie = new Movie();
  movie.setVisible(true);
  showtime = new Showtime();
  showtime.setMovie(movie);
  showtime.setRoom(room);
  showtime.setStatus(ShowtimeStatus.OPEN);
  showtime.setStartTime(now.plusHours(1));
 }

 @Test void operationalRequiresBothRoomAndTheaterActive() {
  for (boolean roomActive : new boolean[]{true, false}) {
   for (boolean theaterActive : new boolean[]{true, false}) {
    room.setActive(roomActive);
    theater.setActive(theaterActive);
    assertThat(room.isOperational()).isEqualTo(roomActive && theaterActive);
   }
  }
 }

 @Test void roomWithoutTheaterIsNotOperational() {
  room.setTheater(null);
  assertThat(room.isOperational()).isFalse();
 }

 @Test void eachStatusProvidesItsOwnOccupancyBehavior() {
  assertThat(ShowtimeStatus.OPEN.occupiesRoom()).isTrue();
  assertThat(ShowtimeStatus.CLOSED.occupiesRoom()).isTrue();
  assertThat(ShowtimeStatus.CANCELLED.occupiesRoom()).isFalse();
 }

 @Test void onlyOpenShowtimeIsBookable() {
  for (ShowtimeStatus status : ShowtimeStatus.values()) {
   showtime.setStatus(status);
   assertThat(showtime.isBookableAt(now)).isEqualTo(status == ShowtimeStatus.OPEN);
  }
 }

 @Test void bookingClosesExactlyAtStartTime() {
  assertThat(showtime.isBookableAt(now)).isTrue();
  assertThat(showtime.isBookableAt(showtime.getStartTime().minusNanos(1))).isTrue();
  assertThat(showtime.isBookableAt(showtime.getStartTime())).isFalse();
  assertThat(showtime.isBookableAt(showtime.getStartTime().plusNanos(1))).isFalse();
 }

 @Test void hiddenMovieIsNotBookable() {
  movie.setVisible(false);
  assertThat(showtime.isBookableAt(now)).isFalse();
 }

 @Test void inactiveRoomOrTheaterIsNotBookable() {
  room.setActive(false);
  assertThat(showtime.isBookableAt(now)).isFalse();
  room.setActive(true);
  theater.setActive(false);
  assertThat(showtime.isBookableAt(now)).isFalse();
 }

 @Test void incompleteShowtimeIsNotBookableAndNowIsRequired() {
  assertThat(new Showtime().isBookableAt(now)).isFalse();
  showtime.setMovie(null);
  assertThat(showtime.isBookableAt(now)).isFalse();
  showtime.setMovie(movie);
  showtime.setRoom(null);
  assertThat(showtime.isBookableAt(now)).isFalse();
  showtime.setRoom(room);
  showtime.setStartTime(null);
  assertThat(showtime.isBookableAt(now)).isFalse();
  assertThatNullPointerException().isThrownBy(() -> showtime.isBookableAt(null));
 }
}
