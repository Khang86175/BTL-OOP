package com.example.movie;
import com.example.movie.dto.*;
import com.example.movie.model.*;
import com.example.movie.repository.*;
import com.example.movie.service.*;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:person3_test;MODE=MySQL;DB_CLOSE_DELAY=-1") @ActiveProfiles("test") @WithMockUser(roles="ADMIN")
class ShowtimeServiceIntegrationTest {
 @Autowired TheaterService theaters; @Autowired RoomService rooms; @Autowired SeatService seats;
 @Autowired ShowtimeService schedules; @Autowired MovieRepository movies;
 @Autowired ShowtimeRepository repository; @Autowired PlatformTransactionManager transactionManager;
 @Autowired SeatRepository seatRepository; @Autowired RoomRepository roomRepository; @Autowired TheaterRepository theaterRepository;
 Long movieId,theaterId,roomId; LocalDateTime start;
 @AfterEach void cleanup(){
  // Only fixtures owned by this test, in the H2 test database.
  new TransactionTemplate(transactionManager).executeWithoutResult(tx->{
   repository.findAll().stream().filter(s->s.getMovie().getId().equals(movieId)).forEach(repository::delete);
   repository.flush();
   roomRepository.findAll().stream().filter(r->r.getTheater().getId().equals(theaterId)).forEach(r->{
    seatRepository.deleteAll(seatRepository.findByRoomIdOrderByRowLabelAscSeatNumberAsc(r.getId()));
    seatRepository.flush();roomRepository.delete(r);
   });
   roomRepository.flush();theaterRepository.deleteById(theaterId);movies.deleteById(movieId);
  });
 }
 @BeforeEach void setup(){
  var t=theaters.create(new TheaterForm("Test","Địa chỉ",true));theaterId=t.getId();
  roomId=rooms.create(new RoomForm(theaterId,"A",RoomType.TWO_D,true)).getId();
  start=LocalDate.now(Movie.CINEMA_ZONE).plusDays(10).atTime(20,0);
  Movie m=new Movie();m.setTitle("Test");m.setDirector("Test");m.setDurationMinutes(120);
  m.setReleaseDate(start.toLocalDate().minusDays(1));m.setAgeRating(AgeRating.P);m.setSynopsis("Test");m.setVisible(true);
  movieId=movies.saveAndFlush(m).getId();
 }
 ShowtimeForm form(Long room,LocalDateTime time,ShowtimeStatus status){return new ShowtimeForm(movieId,room,time,new BigDecimal("90000"),new BigDecimal("110000"),status);}
 ShowtimeSummary create(LocalDateTime time){return schedules.create(form(roomId,time,ShowtimeStatus.OPEN));}
 @Test void calculatesEndTime(){assertThat(create(start).endTime()).isEqualTo(start.plusHours(2));}
 @Test void rejectsPast(){assertThatThrownBy(()->create(LocalDateTime.now(Movie.CINEMA_ZONE).minusHours(1))).isInstanceOf(ScheduleValidationException.class);}
 @Test void rejectsHiddenMovie(){
  new TransactionTemplate(transactionManager).executeWithoutResult(tx->movies.findById(movieId).orElseThrow().setVisible(false));
  assertThatThrownBy(()->create(start)).isInstanceOf(ResponseStatusException.class);
 }
 @Test void rejectsInactiveRoom(){
  rooms.update(roomId,new RoomForm(theaterId,"A",RoomType.TWO_D,false));
  assertThatThrownBy(()->create(start)).isInstanceOf(ScheduleValidationException.class);
 }
 @Test void rejectsInactiveTheater(){
  theaters.update(theaterId,new TheaterForm("Test","Địa chỉ",false));
  assertThatThrownBy(()->create(start)).isInstanceOf(ScheduleValidationException.class);
 }
 @Test void rejectsBeforeRelease(){
  assertThatThrownBy(()->create(start.minusDays(2))).isInstanceOf(ScheduleValidationException.class);
 }
 @Test void rejectsNegativePrice(){
  assertThatThrownBy(()->schedules.create(new ShowtimeForm(movieId,roomId,start,new BigDecimal("-1"),new BigDecimal("110000"),ShowtimeStatus.OPEN))).isInstanceOf(ConstraintViolationException.class);
 }
 @Test void rejectsAllOverlapShapes(){
  create(start);
  for(var time:List.of(start,start.minusMinutes(30),start.plusMinutes(30)))
   assertThatThrownBy(()->create(time)).isInstanceOf(ScheduleValidationException.class);
 }
 @Test void allowsAdjacent(){create(start);assertThat(create(start.plusHours(2))).isNotNull();assertThat(create(start.minusHours(2))).isNotNull();}
 @Test void allowsDifferentRooms(){
  var other=rooms.create(new RoomForm(theaterId,"B",RoomType.IMAX,true));
  create(start);assertThat(schedules.create(form(other.getId(),start,ShowtimeStatus.OPEN))).isNotNull();
 }
 @Test void handlesMidnight(){
  var time=start.withHour(23);assertThat(create(time).endTime()).isEqualTo(time.plusHours(2));
  assertThatThrownBy(()->create(time.plusHours(1))).isInstanceOf(ScheduleValidationException.class);
 }
 @Test void updateExcludesItself(){
  var s=create(start);assertThat(schedules.update(s.id(),form(roomId,start.plusMinutes(10),ShowtimeStatus.OPEN)).id()).isEqualTo(s.id());
 }
 @Test void updateCannotOverlapAnotherSchedule(){
  var s=create(start);create(start.plusHours(3));
  assertThatThrownBy(()->schedules.update(s.id(),form(roomId,start.plusHours(2),ShowtimeStatus.OPEN))).isInstanceOf(ScheduleValidationException.class);
 }
 @Test void hidingMovieAfterScheduleRemovesBookingAccess(){
  var s=create(start);
  new TransactionTemplate(transactionManager).executeWithoutResult(tx->movies.findById(movieId).orElseThrow().setVisible(false));
  assertThat(schedules.findOpenShowtimes(movieId,theaterId,start.toLocalDate())).isEmpty();
  assertThatThrownBy(()->schedules.findActiveSeats(s.id())).isInstanceOf(ScheduleValidationException.class);
 }
 @Test void directServiceCallRequiresAdmin(){
  var context=SecurityContextHolder.getContext();
  var previous=context.getAuthentication();
  try{
   context.setAuthentication(new UsernamePasswordAuthenticationToken("customer","",List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
   assertThatThrownBy(()->create(start)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
  }finally{context.setAuthentication(previous);}
 }
 @Test void closedStillOccupiesRoom(){
  schedules.create(form(roomId,start,ShowtimeStatus.CLOSED));
  assertThatThrownBy(()->create(start)).isInstanceOf(ScheduleValidationException.class);
 }
 @Test void cancelledDoesNotOccupyRoom(){
  schedules.create(form(roomId,start,ShowtimeStatus.CANCELLED));assertThat(create(start)).isNotNull();
 }
 @Test void cancelledCannotReopenOverAnotherSchedule(){
  var s=schedules.create(form(roomId,start,ShowtimeStatus.CANCELLED));create(start);
  assertThatThrownBy(()->schedules.update(s.id(),form(roomId,start,ShowtimeStatus.OPEN))).isInstanceOf(ScheduleValidationException.class);
 }
 @Test void bookingContractFiltersAndReturnsOnlyPhysicalActiveSeats(){
  var s=create(start);var layout=seats.createLayout(new SeatLayoutCreateForm(roomId,1,2));seats.update(layout.get(1).id(),new SeatForm(roomId,"A",2,SeatType.VIP,false));
  assertThat(schedules.findOpenShowtimes(movieId,theaterId,start.toLocalDate())).extracting(ShowtimeSummary::id).containsExactly(s.id());
  assertThat(schedules.findOpenShowtimes(movieId,theaterId,start.toLocalDate().plusDays(1))).isEmpty();
  assertThat(schedules.getBookableShowtime(s.id()).roomId()).isEqualTo(roomId);
  assertThat(schedules.findActiveSeats(s.id())).hasSize(1);
 }
 @Test void closedScheduleNotBookable(){
  var s=schedules.create(form(roomId,start,ShowtimeStatus.CLOSED));
  assertThatThrownBy(()->schedules.getBookableShowtime(s.id())).isInstanceOf(ScheduleValidationException.class);
 }
 @Test void bookingContractExcludesLegacyTypesWithoutChangingPricesOrIds(){
  var show=create(start);
  var initial=seats.createLayout(new SeatLayoutCreateForm(roomId,1,3));var normal=initial.get(0);
  var vip=initial.get(1);seats.update(vip.id(),new SeatForm(roomId,"A",2,SeatType.VIP,true));
  var legacy=initial.get(2);
  new TransactionTemplate(transactionManager).executeWithoutResult(tx->seatRepository.findById(legacy.id()).orElseThrow().setSeatType(SeatType.COUPLE));
  assertThat(schedules.findActiveSeats(show.id())).extracting(SeatSummary::id).containsExactly(normal.id(),vip.id());
  assertThat(schedules.getBookableShowtime(show.id()).basePrice()).isEqualByComparingTo("90000");
  assertThat(seats.findByRoom(roomId)).hasSize(3);
 }
 @Test void repositoryOccupancyMatchesEveryEnumImplementation(){
  int offset=0;
  for(var status:ShowtimeStatus.values()){
   var time=start.plusHours(offset);
   var s=schedules.create(form(roomId,time,status));
   assertThat(repository.countOverlaps(roomId,-1L,time,time.plusHours(2)))
    .as("occupancy for %s",status).isEqualTo(status.occupiesRoom()?1L:0L);
   assertThat(repository.countOverlaps(roomId,s.id(),time,time.plusHours(2))).isZero();
   assertThat(repository.countOverlaps(roomId,-1L,time.plusHours(2),time.plusHours(3))).isZero();
   offset+=4;
  }
 }
 @Test void concurrentRequestsCreateOnlyOneSchedule() throws Exception {
  var pool=Executors.newFixedThreadPool(2);var ready=new CountDownLatch(2);var go=new CountDownLatch(1);
  Callable<Boolean> task=()->{
   SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin","",List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
   try {ready.countDown();if(!go.await(10,TimeUnit.SECONDS))throw new IllegalStateException("Timeout");
    try{create(start);return true;}catch(ScheduleValidationException ex){return false;}
   }finally{SecurityContextHolder.clearContext();}
  };
  try{
   var a=pool.submit(task);var b=pool.submit(task);
   assertThat(ready.await(10,TimeUnit.SECONDS)).isTrue();go.countDown();
   assertThat(List.of(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);
   assertThat(repository.findSchedule(movieId,theaterId,start.toLocalDate().atStartOfDay(),start.toLocalDate().plusDays(1).atStartOfDay())).hasSize(1);
  }finally{go.countDown();pool.shutdownNow();}
 }
}
