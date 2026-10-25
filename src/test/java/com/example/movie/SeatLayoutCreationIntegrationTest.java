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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:person3_simple_creation;MODE=MySQL;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test") @WithMockUser(roles="ADMIN")
class SeatLayoutCreationIntegrationTest {
 @Autowired SeatService seats; @Autowired RoomService rooms; @Autowired TheaterService theaters;
 @MockitoSpyBean SeatRepository repository;
 @Autowired RoomRepository roomRepository; @Autowired TheaterRepository theaterRepository;
 Long roomId,theaterId;
 @BeforeEach void setup(){
  theaterId=theaters.create(new TheaterForm("Rạp test","Địa chỉ",true)).getId();
  roomId=rooms.create(new RoomForm(theaterId,"Phòng mới",RoomType.TWO_D,true)).getId();
 }
 @AfterEach void cleanup(){reset(repository);repository.deleteAll();roomRepository.deleteAll();theaterRepository.deleteAll();}
 List<SeatSummary> create(int rows,int count){return seats.createLayout(new SeatLayoutCreateForm(roomId,rows,count));}
 Seat legacy(String row,int number,SeatType type,boolean active){
  var s=new Seat();s.setRoom(roomRepository.findById(roomId).orElseThrow());s.setRowLabel(row);s.setSeatNumber(number);s.setSeatType(type);s.setActive(active);
  return repository.saveAndFlush(s);
 }
 @Test void fiveRowsOfEightCreateExactlyFortyUniquePositions(){
  var result=create(5,8);assertThat(result).hasSize(40);
  var expected=new ArrayList<String>();for(char row='A';row<='E';row++)for(int n=1;n<=8;n++)expected.add(""+row+n);
  assertThat(result).extracting(s->s.rowLabel()+s.seatNumber()).containsExactlyElementsOf(expected);
  assertThat(result).extracting(SeatSummary::id).doesNotHaveDuplicates().doesNotContainNull();
 }
 @Test void everyNewSeatIsActiveNormal(){assertThat(create(5,8)).allMatch(s->s.active()&&s.seatType()==SeatType.NORMAL);}
 @Test void readingRoomNeverCreatesSeats(){assertThat(seats.findByRoom(roomId)).isEmpty();assertThat(seats.findByRoom(roomId)).isEmpty();assertThat(repository.count()).isZero();}
 @Test void repeatCreationCannotResizeOrOverwrite(){
  var original=create(5,8);
  assertThatThrownBy(()->create(6,12)).isInstanceOf(ScheduleValidationException.class);
  assertThat(seats.findByRoom(roomId)).containsExactlyElementsOf(original);
 }
 @Test void inactiveOnlyRoomCannotBeRecreated(){
  var s=create(1,1).getFirst();seats.update(s.id(),new SeatForm(roomId,"A",1,SeatType.NORMAL,false));
  assertThatThrownBy(()->create(5,8)).isInstanceOf(ScheduleValidationException.class);
  assertThat(seats.findByRoom(roomId)).containsExactly(new SeatSummary(s.id(),roomId,"A",1,SeatType.NORMAL,false));
 }
 @Test void legacyNumberingAndHistoricalOverflowRemainUnchanged(){
  var a11=legacy("A",11,SeatType.VIP,false);var a12=legacy("A",12,SeatType.NORMAL,true);var odd=legacy("Z9",212,SeatType.NORMAL,false);
  var original=seats.findByRoom(roomId);
  assertThatThrownBy(()->create(5,8)).isInstanceOf(ScheduleValidationException.class);
  assertThat(seats.findByRoom(roomId)).containsExactlyElementsOf(original);
  seats.update(odd.getId(),new SeatForm(roomId,"Z9",212,SeatType.VIP,false));
  assertThat(seats.getById(odd.getId()).active()).isFalse();
  assertThat(seats.getById(a11.getId()).active()).isFalse();assertThat(seats.getById(a12.getId()).active()).isTrue();
 }
 @Test void normalVipNormalRoundTripPreservesIdentity(){
  var s=create(1,1).getFirst();seats.update(s.id(),new SeatForm(roomId,"A",1,SeatType.VIP,true));
  assertThat(seats.getById(s.id()).seatType()).isEqualTo(SeatType.VIP);
  seats.update(s.id(),new SeatForm(roomId,"A",1,SeatType.NORMAL,true));assertThat(seats.getById(s.id())).isEqualTo(s);
 }
 @Test void deactivateAndReactivateOnlySelectedSeat(){
  var original=create(1,2);var second=original.get(1);
  seats.update(second.id(),new SeatForm(roomId,"A",2,SeatType.NORMAL,false));
  assertThat(rooms.capacity(roomId)).isEqualTo(1);assertThat(seats.getById(original.getFirst().id())).isEqualTo(original.getFirst());
  seats.update(second.id(),new SeatForm(roomId,"A",2,SeatType.NORMAL,true));assertThat(seats.findByRoom(roomId)).containsExactlyElementsOf(original);
 }
 @Test void seatCannotMoveToDifferentRowOrNumber(){
  var s=create(1,1).getFirst();
  for(var form:List.of(new SeatForm(roomId,"B",1,SeatType.VIP,true),new SeatForm(roomId,"A",2,SeatType.VIP,true)))
   assertThatThrownBy(()->seats.update(s.id(),form)).isInstanceOf(ScheduleValidationException.class);
  assertThat(seats.getById(s.id())).isEqualTo(s);
 }
 @Test void seatCannotMoveToAnotherRoom(){
  var s=create(1,1).getFirst();var other=rooms.create(new RoomForm(theaterId,"Other",RoomType.TWO_D,true));
  assertThatThrownBy(()->seats.update(s.id(),new SeatForm(other.getId(),"A",1,SeatType.VIP,true))).isInstanceOf(ScheduleValidationException.class);
  assertThat(seats.getById(s.id())).isEqualTo(s);
 }
 @Test void legacyTypeReadableUntilExplicitConversionWithoutIdChanges(){
  var old=legacy("A",11,SeatType.COUPLE,false);
  assertThat(seats.getById(old.getId()).seatType()).isEqualTo(SeatType.COUPLE);
  assertThatThrownBy(()->seats.update(old.getId(),new SeatForm(roomId,"A",11,SeatType.COUPLE,true))).isInstanceOf(ScheduleValidationException.class);
  seats.update(old.getId(),new SeatForm(roomId,"A",11,SeatType.NORMAL,false));
  assertThat(seats.getById(old.getId())).isEqualTo(new SeatSummary(old.getId(),roomId,"A",11,SeatType.NORMAL,false));
 }
 @Test void assigningCoupleToSupportedSeatRejected(){
  var s=create(1,1).getFirst();assertThatThrownBy(()->seats.update(s.id(),new SeatForm(roomId,"A",1,SeatType.COUPLE,true))).isInstanceOf(ScheduleValidationException.class);
  assertThat(seats.getById(s.id())).isEqualTo(s);
 }
 @Test void invalidDimensionsRejectedBeforeAnyInsert(){
  for(var f:List.of(new SeatLayoutCreateForm(roomId,0,8),new SeatLayoutCreateForm(roomId,31,8),new SeatLayoutCreateForm(roomId,5,0),new SeatLayoutCreateForm(roomId,5,201)))
   assertThatThrownBy(()->seats.createLayout(f)).isInstanceOf(ConstraintViolationException.class);
  assertThat(repository.count()).isZero();
 }
 @Test void totalAboveThousandRejected(){assertThatThrownBy(()->create(6,200)).isInstanceOf(ScheduleValidationException.class);assertThat(repository.count()).isZero();}
 @Test void allowsExactlyThousand(){assertThat(create(5,200)).hasSize(1000);}
 @Test void rowLabelsContinueAfterZ(){assertThat(create(28,1)).extracting(SeatSummary::rowLabel).containsSubsequence("Z","AA","AB");}
 @Test void nonexistentRoomOrSeatRejected(){
  assertThatThrownBy(()->seats.createLayout(new SeatLayoutCreateForm(Long.MAX_VALUE,5,8))).isInstanceOf(ScheduleValidationException.class);
  assertThatThrownBy(()->seats.update(Long.MAX_VALUE,new SeatForm(roomId,"A",1,SeatType.NORMAL,true))).isInstanceOf(ScheduleValidationException.class);
 }
 @Test void databaseUniquenessPreventsDuplicatePhysicalPosition(){
  create(1,1);assertThatThrownBy(()->legacy("A",1,SeatType.VIP,true)).isInstanceOf(DataIntegrityViolationException.class);
  assertThat(repository.count()).isEqualTo(1);
 }
 @Test void failedInsertRollsBackWholeInitialLayout(){
  doAnswer(call->{List<Seat> batch=call.getArgument(0);repository.saveAndFlush(batch.getFirst());throw new DataIntegrityViolationException("Simulated failure");}).when(repository).saveAllAndFlush(anyList());
  assertThatThrownBy(()->create(5,8)).isInstanceOf(DataIntegrityViolationException.class);assertThat(repository.count()).isZero();
 }
 @Test void customerCannotInitialize(){
  SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("customer","",List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
  assertThatThrownBy(()->create(5,8)).isInstanceOf(AccessDeniedException.class);
 }
 @Test void directUpdateRequiresAdmin(){
  var s=create(1,1).getFirst();SecurityContextHolder.clearContext();
  assertThatThrownBy(()->seats.update(s.id(),new SeatForm(roomId,"A",1,SeatType.VIP,true))).isInstanceOf(AccessDeniedException.class);
 }
 @Test void concurrentInitializationsCreateOnlyOneLayout() throws Exception {
  var pool=Executors.newFixedThreadPool(2);var ready=new CountDownLatch(2);var start=new CountDownLatch(1);
  Callable<Boolean> task=()->{
   SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin","",List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
   try{ready.countDown();if(!start.await(10,TimeUnit.SECONDS))throw new IllegalStateException("Timeout");try{create(5,8);return true;}catch(ScheduleValidationException ex){return false;}}
   finally{SecurityContextHolder.clearContext();}
  };
  try{var a=pool.submit(task);var b=pool.submit(task);assertThat(ready.await(10,TimeUnit.SECONDS)).isTrue();start.countDown();assertThat(List.of(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);assertThat(repository.count()).isEqualTo(40);}
  finally{start.countDown();pool.shutdownNow();}
 }
}
