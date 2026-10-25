package com.example.movie;
import com.example.movie.config.ShowtimeSeedData;
import com.example.movie.model.*;
import com.example.movie.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:person3_seed;MODE=MySQL;DB_CLOSE_DELAY=-1","showtime.seed.enabled=true"})
@ActiveProfiles("demo")
class ShowtimeSeedSmokeTest {
 @Autowired ShowtimeSeedData seed;
 @Autowired TheaterRepository theaters;@Autowired RoomRepository rooms;@Autowired SeatRepository seats;
 @Autowired ShowtimeRepository schedules;@Autowired MovieRepository movies;
 @Test void seedsAfterCatalogAndRerunsWithoutDuplicatesOrChangingMovies(){
  assertThat(movies.count()).isEqualTo(6);
  assertThat(theaters.count()).isEqualTo(1);assertThat(rooms.count()).isEqualTo(1);
  assertThat(seats.count()).isZero();assertThat(schedules.count()).isEqualTo(2);
  var ids=seats.findAll().stream().map(Seat::getId).toList();
  seed.seed();
  assertThat(seats.findAll()).extracting(Seat::getId).containsExactlyElementsOf(ids);
  assertThat(theaters.count()).isEqualTo(1);assertThat(schedules.count()).isEqualTo(2);assertThat(movies.count()).isEqualTo(6);
  Seat manual=new Seat();manual.setRoom(rooms.findAll().getFirst());manual.setRowLabel("X");manual.setSeatNumber(1);
  manual.setSeatType(SeatType.VIP);manual.setActive(true);manual=seats.saveAndFlush(manual);
  seed.seed();
  assertThat(seats.findAll()).extracting(Seat::getId).containsExactly(manual.getId());
  assertThat(seats.findById(manual.getId()).orElseThrow().getSeatType()).isEqualTo(SeatType.VIP);
 }
}
