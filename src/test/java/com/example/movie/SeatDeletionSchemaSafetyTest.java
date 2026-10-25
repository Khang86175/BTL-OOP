package com.example.movie;

import com.example.movie.dto.*;
import com.example.movie.model.*;
import com.example.movie.repository.*;
import com.example.movie.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:person3_missing_fk;MODE=MySQL;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test") @WithMockUser(roles="ADMIN")
class SeatDeletionSchemaSafetyTest {
 @Autowired SeatService seats; @Autowired RoomService rooms; @Autowired TheaterService theaters;
 @Autowired SeatRepository repository; @Autowired RoomRepository roomRepository; @Autowired TheaterRepository theaterRepository; @Autowired JdbcTemplate jdbc;
 Long roomId;
 @BeforeEach void setup(){
  var theater=theaters.create(new TheaterForm("Missing FK","Test",true));roomId=rooms.create(new RoomForm(theater.getId(),"A",RoomType.TWO_D,true)).getId();
  jdbc.execute("create table if not exists booking_seats(id bigint primary key, seat_id bigint)");
 }
 @AfterEach void cleanup(){repository.deleteAll();roomRepository.deleteAll();theaterRepository.deleteAll();}
 @Test void missingBookingForeignKeyBlocksDestructiveOperations(){
  var original=seats.saveLayout(new SeatLayoutCreateForm(roomId,5,8));
  assertThatThrownBy(()->seats.saveLayout(new SeatLayoutCreateForm(roomId,4,8,true))).isInstanceOf(ScheduleValidationException.class).hasMessageContaining("khóa ngoại");
  assertThatThrownBy(()->seats.deleteSeat(roomId,original.getFirst().id(),true)).isInstanceOf(ScheduleValidationException.class);
  assertThatThrownBy(()->seats.deleteLayout(roomId,true)).isInstanceOf(ScheduleValidationException.class);
  assertThat(seats.findByRoom(roomId)).containsExactlyElementsOf(original);
 }
 @Test void safeExpansionStillAllowedWithoutDeletion(){
  seats.saveLayout(new SeatLayoutCreateForm(roomId,5,8));assertThat(seats.saveLayout(new SeatLayoutCreateForm(roomId,6,8))).hasSize(48);
 }
}
