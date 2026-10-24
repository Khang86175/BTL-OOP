package com.example.movie;
import com.example.movie.dto.*;
import com.example.movie.model.*;
import com.example.movie.service.*;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.access.AccessDeniedException;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:person3_test;MODE=MySQL;DB_CLOSE_DELAY=-1") @ActiveProfiles("test") @Transactional
@WithMockUser(roles="ADMIN")
class TheaterRoomSeatIntegrationTest {
 @Autowired TheaterService theaters; @Autowired RoomService rooms; @Autowired SeatService seats;
 Theater theater(){return theaters.create(new TheaterForm("Rạp A","Hà Nội",true));}
 Room room(Long t,String name){return rooms.create(new RoomForm(t,name,RoomType.TWO_D,true));}
 @Test void createAndUpdateTheater(){
  var t=theater();var edited=theaters.update(t.getId(),new TheaterForm("Rạp B","Địa chỉ mới",false));
  assertThat(edited.getId()).isEqualTo(t.getId());assertThat(edited.getName()).isEqualTo("Rạp B");assertThat(edited.isActive()).isFalse();
 }
 @Test void createAndUpdateRoom(){
  var t=theater();var r=room(t.getId(),"1");rooms.update(r.getId(),new RoomForm(t.getId(),"2",RoomType.IMAX,false));
  assertThat(rooms.getById(r.getId()).getRoomType()).isEqualTo(RoomType.IMAX);
  assertThat(rooms.findActive(t.getId())).isEmpty();
 }
 @Test void duplicateRoomInSameTheaterRejected(){
  var t=theater();room(t.getId(),"A");
  assertThatThrownBy(()->room(t.getId()," a ")).isInstanceOf(ScheduleValidationException.class);
 }
 @Test void sameRoomNameInDifferentTheatersAllowed(){
  room(theater().getId(),"A");room(theater().getId(),"A");
 }
 @Test void seatUpdateKeepsIdAndCapacityUsesActiveSeats(){
  var r=room(theater().getId(),"A");
  var s=seats.create(new SeatForm(r.getId()," a ",1,SeatType.NORMAL,true));
  var changed=seats.update(s.getId(),new SeatForm(r.getId(),"B",2,SeatType.VIP,false));
  assertThat(changed.getId()).isEqualTo(s.getId());assertThat(rooms.capacity(r.getId())).isZero();
  assertThat(seats.findByRoom(r.getId())).hasSize(1);
 }
 @Test void duplicateSeatRejected(){
  var r=room(theater().getId(),"A");seats.create(new SeatForm(r.getId(),"A",1,SeatType.NORMAL,true));
  assertThatThrownBy(()->seats.create(new SeatForm(r.getId(),"a",1,SeatType.VIP,true))).isInstanceOf(ScheduleValidationException.class);
 }
 @Test void cannotMoveSeatToAnotherRoom(){
  var t=theater();var a=room(t.getId(),"A");var b=room(t.getId(),"B");
  var s=seats.create(new SeatForm(a.getId(),"A",1,SeatType.NORMAL,true));
  assertThatThrownBy(()->seats.update(s.getId(),new SeatForm(b.getId(),"A",1,SeatType.NORMAL,true))).isInstanceOf(ScheduleValidationException.class);
 }
 @Test void serviceValidatesForms(){
  assertThatThrownBy(()->theaters.create(new TheaterForm(" ","",true))).isInstanceOf(ConstraintViolationException.class);
 }
 @Test @WithMockUser(roles="CUSTOMER") void customerCannotManage(){
  assertThatThrownBy(()->theater()).isInstanceOf(AccessDeniedException.class);
 }
}
