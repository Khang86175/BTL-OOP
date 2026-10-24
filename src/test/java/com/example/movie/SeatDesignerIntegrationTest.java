package com.example.movie;

import com.example.movie.dto.*;
import com.example.movie.model.*;
import com.example.movie.repository.*;
import com.example.movie.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:person3_designer;MODE=MySQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc @ActiveProfiles("test") @WithMockUser(roles="ADMIN")
class SeatDesignerIntegrationTest {
 @Autowired SeatService seats; @Autowired TheaterService theaters; @Autowired RoomService rooms;
 @MockitoSpyBean SeatRepository repository;
 @Autowired RoomRepository roomRepository; @Autowired TheaterRepository theaterRepository;
 @Autowired PlatformTransactionManager txManager; @Autowired MockMvc mvc;
 Long roomId,theaterId,id;
 @BeforeEach void setup(){
  theaterId=theaters.create(new TheaterForm("Rạp Designer","Địa chỉ test",true)).getId();
  roomId=rooms.create(new RoomForm(theaterId,"Phòng A",RoomType.TWO_D,true)).getId();
  id=seats.create(new SeatForm(roomId,"A",1,SeatType.VIP,true)).getId();
 }
 @AfterEach void cleanup(){
  reset(repository);
  new TransactionTemplate(txManager).executeWithoutResult(tx->{
   repository.deleteAll();repository.flush();roomRepository.deleteAll();roomRepository.flush();theaterRepository.deleteAll();
  });
 }
 SeatLayoutSeatForm seat(Long id,String row,int n,SeatType type,boolean active){return new SeatLayoutSeatForm(id,row,n,type,active);}
 SeatLayoutSeatForm existing(){return seat(id,"A",1,SeatType.VIP,true);}
 SeatLayoutForm form(List<SeatLayoutSeatForm> desired,boolean confirm){return new SeatLayoutForm(roomId,seats.loadLayout(roomId).snapshot(),desired,confirm);}
 @Test void createsInitialAThroughEAndPreservesExistingA(){
  var desired=new ArrayList<SeatLayoutSeatForm>();desired.add(existing());
  for(String row:List.of("A","B","C","D","E"))for(int n=1;n<=12;n++){
   if(row.equals("A")&&n==1)continue;
   desired.add(seat(null,row,n,SeatType.NORMAL,true));
  }
  var result=seats.saveLayout(form(desired,false));
  assertThat(result.created()).isEqualTo(59);
  assertThat(seats.findByRoom(roomId)).hasSize(60);
  assertThat(seats.getById(id).seatType()).isEqualTo(SeatType.VIP);
 }
 @Test void editsExistingAWithoutDuplicateOrIdChange(){
  seats.saveLayout(form(List.of(seat(id,"A",1,SeatType.COUPLE,false)),true));
  assertThat(repository.count()).isEqualTo(1);
  assertThat(seats.getById(id).seatType()).isEqualTo(SeatType.COUPLE);
  assertThat(seats.getById(id).active()).isFalse();
 }
 @Test void mapsExistingPositionToOriginalIdEvenWhenClientOmitsId(){
  seats.saveLayout(form(List.of(seat(null," a ",1,SeatType.VIP,true)),false));
  assertThat(repository.count()).isEqualTo(1);
  assertThat(seats.findByRoom(roomId).getFirst().id()).isEqualTo(id);
 }
 @Test void increasingRowAddsOnlyNewPositions(){
  seats.saveLayout(form(List.of(existing(),seat(null,"A",2,SeatType.NORMAL,true),seat(null,"A",3,SeatType.NORMAL,true)),false));
  assertThat(repository.count()).isEqualTo(3);
  assertThat(seats.getById(id).seatType()).isEqualTo(SeatType.VIP);
 }
 @Test void shrinkingDeactivatesOmittedSeatsAndPreservesRemainingIds(){
  var second=seats.create(new SeatForm(roomId,"A",2,SeatType.NORMAL,true));
  seats.saveLayout(form(List.of(existing()),true));
  assertThat(repository.count()).isEqualTo(2);
  assertThat(seats.getById(id).active()).isTrue();
  assertThat(seats.getById(second.getId()).active()).isFalse();
 }
 @Test void deletingWholeSavedRowOnlyDeactivates(){
  seats.saveLayout(form(List.of(),true));
  assertThat(repository.count()).isEqualTo(1);
  assertThat(seats.getById(id).active()).isFalse();
 }
 @Test void emptyUnsavedLayoutCanBeSavedWithoutRowMinimum(){
  var other=rooms.create(new RoomForm(theaterId,"Empty",RoomType.TWO_D,true));
  var state=seats.loadLayout(other.getId());
  seats.saveLayout(new SeatLayoutForm(other.getId(),state.snapshot(),List.of(),false));
  assertThat(seats.findByRoom(other.getId())).isEmpty();
 }
 @Test void requiresConfirmationBeforeChangingOldCustomizations(){
  assertThatThrownBy(()->seats.saveLayout(form(List.of(seat(id,"A",1,SeatType.NORMAL,true)),false)))
   .isInstanceOf(ScheduleValidationException.class).hasMessageContaining("xác nhận");
  assertThat(seats.getById(id).seatType()).isEqualTo(SeatType.VIP);
 }
 @Test void requiresConfirmationBeforeOmittingOldSeat(){
  assertThatThrownBy(()->seats.saveLayout(form(List.of(),false))).isInstanceOf(ScheduleValidationException.class);
  assertThat(seats.getById(id).active()).isTrue();
 }
 @Test void staleDraftCannotOverwriteSingleSeatEdit(){
  var draft=form(List.of(existing(),seat(null,"B",1,SeatType.NORMAL,true)),true);
  seats.update(id,new SeatForm(roomId,"A",1,SeatType.COUPLE,true));
  assertThatThrownBy(()->seats.saveLayout(draft)).isInstanceOf(ScheduleValidationException.class).hasMessageContaining("nơi khác");
  assertThat(repository.count()).isEqualTo(1);assertThat(seats.getById(id).seatType()).isEqualTo(SeatType.COUPLE);
 }
 @Test void duplicatePositionsRejectAllChanges(){
  var draft=form(List.of(existing(),seat(null,"B",1,SeatType.NORMAL,true),seat(null," b ",1,SeatType.VIP,true)),true);
  assertThatThrownBy(()->seats.saveLayout(draft)).isInstanceOf(ScheduleValidationException.class);
  assertThat(repository.count()).isEqualTo(1);
 }
 @Test void foreignIdsAndMovingIdsAreRejected(){
  var other=rooms.create(new RoomForm(theaterId,"Other",RoomType.TWO_D,true));
  var foreign=seats.create(new SeatForm(other.getId(),"A",1,SeatType.NORMAL,true));
  assertThatThrownBy(()->seats.saveLayout(form(List.of(seat(foreign.getId(),"A",1,SeatType.NORMAL,true)),true))).isInstanceOf(ScheduleValidationException.class);
  assertThatThrownBy(()->seats.saveLayout(form(List.of(seat(id,"A",2,SeatType.NORMAL,true)),true))).isInstanceOf(ScheduleValidationException.class);
  assertThat(seats.getById(id).seatNumber()).isEqualTo(1);
 }
 @Test void failedInsertRollsBackOldSeatChangesToo(){
  doAnswer(call->{
   List<Seat> additions=call.getArgument(0);repository.saveAndFlush(additions.getFirst());
   throw new DataIntegrityViolationException("Simulated persistence failure");
  }).when(repository).saveAllAndFlush(anyList());
  assertThatThrownBy(()->seats.saveLayout(form(List.of(seat(id,"A",1,SeatType.NORMAL,false),seat(null,"B",1,SeatType.NORMAL,true)),true)))
   .isInstanceOf(DataIntegrityViolationException.class);
  assertThat(repository.count()).isEqualTo(1);
  assertThat(seats.getById(id).seatType()).isEqualTo(SeatType.VIP);
  assertThat(seats.getById(id).active()).isTrue();
 }
 @Test void aislesDoNotCreateFakeSeats(){
  seats.saveLayout(form(List.of(existing(),seat(null,"B",1,SeatType.NORMAL,true),seat(null,"B",3,SeatType.NORMAL,true)),false));
  assertThat(seats.findByRoom(roomId)).filteredOn(s->s.rowLabel().equals("B")).extracting(SeatSummary::seatNumber).containsExactly(1,3);
 }
 String body(String token,String positions,boolean confirm){
  return "{\"roomId\":"+roomId+",\"snapshot\":\""+token+"\",\"seats\":"+positions+",\"confirmExistingChanges\":"+confirm+"}";
 }
 String existingJson(){return "{\"id\":"+id+",\"rowLabel\":\"A\",\"seatNumber\":1,\"seatType\":\"VIP\",\"active\":true}";}
 @Test void designerRendersCurrentLayoutAndInteractivePanelsWithoutOpenInView() throws Exception {
  mvc.perform(get("/admin/seats").param("roomId",roomId.toString())).andExpect(status().isOk())
   .andExpect(model().attributeExists("layoutSnapshot","seats","rows"))
   .andExpect(content().string(containsString("Chỉnh sửa sơ đồ hiện có")))
   .andExpect(content().string(containsString("data-seat-id=\""+id+"\"")))
   .andExpect(content().string(containsString("row-panel")))
   .andExpect(content().string(containsString("seat-designer-model.js")))
   .andExpect(content().string(containsString("_csrf")));
 }
 @Test void jsonSaveWorksAndStaleRetryReturnsConflict() throws Exception {
  String token=seats.loadLayout(roomId).snapshot();
  String data=body(token,"["+existingJson()+",{\"rowLabel\":\"B\",\"seatNumber\":1,\"seatType\":\"NORMAL\",\"active\":true}]",false);
  mvc.perform(post("/admin/seats/designer").with(csrf()).contentType("application/json").content(data))
   .andExpect(status().isOk()).andExpect(jsonPath("$.created").value(1));
  mvc.perform(post("/admin/seats/designer").with(csrf()).contentType("application/json").content(data))
   .andExpect(status().isConflict()).andExpect(jsonPath("$.field").value("snapshot"));
 }
 @Test void invalidJsonLayoutReturnsBadRequestAndLeavesData() throws Exception {
  String token=seats.loadLayout(roomId).snapshot();
  mvc.perform(post("/admin/seats/designer").with(csrf()).contentType("application/json")
   .content(body(token,"[{\"rowLabel\":\"A\",\"seatNumber\":0,\"seatType\":\"NORMAL\",\"active\":true}]",true)))
   .andExpect(status().isBadRequest());
  assertThat(repository.count()).isEqualTo(1);
 }
 @Test void endpointStillRequiresAdminAndCsrf() throws Exception {
  String data=body(seats.loadLayout(roomId).snapshot(),"["+existingJson()+"]",false);
  mvc.perform(post("/admin/seats/designer").contentType("application/json").content(data)).andExpect(status().isForbidden());
  mvc.perform(post("/admin/seats/designer").with(user("customer").roles("CUSTOMER")).with(csrf()).contentType("application/json").content(data)).andExpect(status().isForbidden());
  mvc.perform(post("/admin/seats/designer").with(anonymous()).with(csrf()).contentType("application/json").content(data)).andExpect(status().is3xxRedirection());
 }
 @Test void directDesignerServiceCallsRequireAdmin(){
  var draft=form(List.of(existing()),false);
  org.springframework.security.core.context.SecurityContextHolder.clearContext();
  assertThatThrownBy(()->seats.loadLayout(roomId)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
  assertThatThrownBy(()->seats.saveLayout(draft)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
 }
 @Test void twoConcurrentDraftsCannotOverwriteEachOther() throws Exception {
  var draft=form(List.of(existing(),seat(null,"B",1,SeatType.NORMAL,true)),false);
  var pool=Executors.newFixedThreadPool(2);var ready=new CountDownLatch(2);var go=new CountDownLatch(1);
  Callable<Boolean> task=()->{
   org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("admin","",List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))));
   try{
    ready.countDown();if(!go.await(10,TimeUnit.SECONDS))throw new IllegalStateException("Timeout");
    try{seats.saveLayout(draft);return true;}catch(ScheduleValidationException ex){return false;}
   }finally{org.springframework.security.core.context.SecurityContextHolder.clearContext();}
  };
  try{
   var a=pool.submit(task);var b=pool.submit(task);assertThat(ready.await(10,TimeUnit.SECONDS)).isTrue();go.countDown();
   assertThat(List.of(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);
   assertThat(repository.count()).isEqualTo(2);
  }finally{go.countDown();pool.shutdownNow();}
 }
}
