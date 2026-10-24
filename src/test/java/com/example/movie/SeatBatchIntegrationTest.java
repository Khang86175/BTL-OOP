package com.example.movie;

import com.example.movie.dto.*;
import com.example.movie.model.*;
import com.example.movie.repository.*;
import com.example.movie.service.*;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
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

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:person3_batch;MODE=MySQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc @ActiveProfiles("test") @WithMockUser(roles="ADMIN")
class SeatBatchIntegrationTest {
 @Autowired SeatService seats;
 @Autowired RoomService rooms;
 @Autowired TheaterService theaters;
 @MockitoSpyBean SeatRepository seatRepository;
 @Autowired RoomRepository roomRepository;
 @Autowired TheaterRepository theaterRepository;
 @Autowired PlatformTransactionManager txManager;
 @Autowired MockMvc mvc;
 @jakarta.persistence.PersistenceContext jakarta.persistence.EntityManager entityManager;
 Long theaterId,roomId,oldId;

 @BeforeEach void setup(){
  theaterId=theaters.create(new TheaterForm("Rạp Batch","Địa chỉ test",true)).getId();
  roomId=rooms.create(new RoomForm(theaterId,"Phòng A",RoomType.TWO_D,true)).getId();
  oldId=seats.create(new SeatForm(roomId,"A",1,SeatType.VIP,true)).getId();
 }
 @AfterEach void cleanup(){
  reset(seatRepository);
  new TransactionTemplate(txManager).executeWithoutResult(tx->{
   seatRepository.deleteAll();seatRepository.flush();
   roomRepository.deleteAll();roomRepository.flush();theaterRepository.deleteAll();
  });
 }
 void admin(){
  org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
   new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("admin","",
    List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))));
 }
 SeatBatchForm batch(SeatRowForm... rows){
  var f=new SeatBatchForm();f.setRoomId(roomId);f.setRows(new ArrayList<>(List.of(rows)));return f;
 }
 SeatRowForm row(String label,int first,int last,SeatType type){return new SeatRowForm(label,first,last,type,true);}
 @Test void createsMultipleRowsAndNormalizesLabelsWithoutTouchingOldSeat(){
  var created=seats.createBatch(batch(row(" b ",1,12,SeatType.NORMAL),row("C",1,6,SeatType.VIP),row("D",1,4,SeatType.COUPLE)));
  assertThat(created).hasSize(22);
  assertThat(created).filteredOn(s->s.rowLabel().equals("B")).hasSize(12);
  assertThat(created).extracting(SeatSummary::id).doesNotHaveDuplicates().doesNotContainNull();
  assertThat(seats.getById(oldId).seatType()).isEqualTo(SeatType.VIP);
  assertThat(seatRepository.count()).isEqualTo(23);
 }
 @Test void existingPositionRejectsWholeBatchWithoutOverwrite(){
  assertThatThrownBy(()->seats.createBatch(batch(row("B",1,5,SeatType.NORMAL),row("a",1,2,SeatType.NORMAL))))
   .isInstanceOf(ScheduleValidationException.class).hasMessageContaining("A1");
  assertThat(seatRepository.count()).isEqualTo(1);
  assertThat(seats.getById(oldId).seatType()).isEqualTo(SeatType.VIP);
 }
 @Test void duplicateWithinRequestRejectsWholeBatch(){
  assertThatThrownBy(()->seats.createBatch(batch(row("B",1,3,SeatType.NORMAL),row(" b ",3,5,SeatType.VIP))))
   .isInstanceOf(ScheduleValidationException.class).hasMessageContaining("B3");
  assertThat(seatRepository.count()).isEqualTo(1);
 }
 @Test void reversedRangeIsRejected(){
  assertThatThrownBy(()->seats.createBatch(batch(row("B",5,1,SeatType.NORMAL)))).isInstanceOf(ScheduleValidationException.class);
  assertThat(seatRepository.count()).isEqualTo(1);
 }
 @Test void nestedValidationRejectsInvalidAndOversizedRows(){
  assertThatThrownBy(()->seats.createBatch(batch(row(" ",1,12,SeatType.NORMAL)))).isInstanceOf(ConstraintViolationException.class);
  assertThatThrownBy(()->seats.createBatch(batch(row("B",0,201,SeatType.NORMAL)))).isInstanceOf(ConstraintViolationException.class);
  var f=batch();f.setRows(Collections.nCopies(31,row("B",1,1,SeatType.NORMAL)));
  assertThatThrownBy(()->seats.createBatch(f)).isInstanceOf(ConstraintViolationException.class);
  assertThat(seatRepository.count()).isEqualTo(1);
 }
 @Test void totalLimitIsCheckedBeforeAnyInsert(){
  var rows=new ArrayList<SeatRowForm>();
  for(String name:List.of("B","C","D","E","F","G"))rows.add(row(name,1,200,SeatType.NORMAL));
  var f=batch();f.setRows(rows);
  assertThatThrownBy(()->seats.createBatch(f)).isInstanceOf(ScheduleValidationException.class).hasMessageContaining("1.000");
  assertThat(seatRepository.count()).isEqualTo(1);
 }
 @Test void previewIsReadOnlyAndIncludesExistingSeatIds(){
  var preview=seats.previewBatch(batch(row("B",1,3,SeatType.NORMAL)));
  assertThat(preview).hasSize(4);
  assertThat(preview).filteredOn(s->s.id()!=null).extracting(SeatSummary::id).containsExactly(oldId);
  assertThat(preview).filteredOn(s->s.id()==null).hasSize(3);
  assertThat(seatRepository.count()).isEqualTo(1);
 }
 @Test void rowSegmentsLeaveAisleWithoutFakeSeats(){
  seats.createBatch(batch(row("A",2,3,SeatType.NORMAL),row("A",5,6,SeatType.NORMAL)));
  assertThat(seats.findByRoom(roomId)).extracting(SeatSummary::seatNumber).containsExactly(1,2,3,5,6);
 }
 @Test void bulkUpdatesKeepAllIdsPositionsAndCount(){
  var created=seats.createBatch(batch(row("B",1,3,SeatType.NORMAL)));
  var ids=new ArrayList<Long>();ids.add(oldId);created.forEach(s->ids.add(s.id()));
  assertThat(seats.updateBatch(new SeatBulkUpdateForm(roomId,ids,SeatType.COUPLE,false))).isEqualTo(4);
  assertThat(seats.findByRoom(roomId)).extracting(SeatSummary::id).containsExactlyElementsOf(ids);
  assertThat(seats.findByRoom(roomId)).allMatch(s->s.seatType()==SeatType.COUPLE&&!s.active());
  assertThat(seatRepository.count()).isEqualTo(4);
 }
 @Test void bulkUpdateCanChangeOnlyOneProperty(){
  seats.updateBatch(new SeatBulkUpdateForm(roomId,List.of(oldId),null,false));
  assertThat(seats.getById(oldId).seatType()).isEqualTo(SeatType.VIP);
  seats.updateBatch(new SeatBulkUpdateForm(roomId,List.of(oldId),SeatType.NORMAL,null));
  assertThat(seats.getById(oldId).active()).isFalse();
 }
 @Test void foreignRoomSeatRejectsAllUpdates(){
  var other=rooms.create(new RoomForm(theaterId,"B",RoomType.TWO_D,true));
  var foreign=seats.create(new SeatForm(other.getId(),"A",1,SeatType.NORMAL,true));
  assertThatThrownBy(()->seats.updateBatch(new SeatBulkUpdateForm(roomId,List.of(oldId,foreign.getId()),SeatType.COUPLE,false)))
   .isInstanceOf(ScheduleValidationException.class);
  assertThat(seats.getById(oldId).seatType()).isEqualTo(SeatType.VIP);
  assertThat(seats.getById(oldId).active()).isTrue();
 }
 @Test void unknownOrRepeatedIdRejectsAllUpdates(){
  assertThatThrownBy(()->seats.updateBatch(new SeatBulkUpdateForm(roomId,List.of(oldId,Long.MAX_VALUE),SeatType.NORMAL,false)))
   .isInstanceOf(ScheduleValidationException.class);
  assertThatThrownBy(()->seats.updateBatch(new SeatBulkUpdateForm(roomId,List.of(oldId,oldId),SeatType.NORMAL,false)))
   .isInstanceOf(ScheduleValidationException.class);
  assertThat(seats.getById(oldId).active()).isTrue();
 }
 @Test void emptySelectionOrNoChangesIsRejected(){
  assertThatThrownBy(()->seats.updateBatch(new SeatBulkUpdateForm(roomId,List.of(),SeatType.NORMAL,true)))
   .isInstanceOf(ConstraintViolationException.class);
  assertThatThrownBy(()->seats.updateBatch(new SeatBulkUpdateForm(roomId,List.of(oldId),null,null)))
   .isInstanceOf(ScheduleValidationException.class);
 }
 @Test void databaseFailureAfterFirstInsertRollsBackEntireTransaction(){
  doAnswer(call->{
   List<Seat> values=call.getArgument(0);
   seatRepository.saveAndFlush(values.getFirst());
   throw new DataIntegrityViolationException("Simulated failure after a real insert");
  }).when(seatRepository).saveAllAndFlush(anyList());
  assertThatThrownBy(()->seats.createBatch(batch(row("B",1,3,SeatType.NORMAL)))).isInstanceOf(DataIntegrityViolationException.class);
  assertThat(seatRepository.count()).isEqualTo(1);
  assertThat(seats.getById(oldId).active()).isTrue();
 }
 @Test void databaseFailureAfterBulkUpdateRollsBackAllChanges(){
  var added=seats.createBatch(batch(row("B",1,2,SeatType.NORMAL)));
  doAnswer(call->{
   entityManager.flush();
   throw new DataIntegrityViolationException("Simulated failure after real updates");
  }).when(seatRepository).flush();
  var ids=List.of(oldId,added.getFirst().id());
  assertThatThrownBy(()->seats.updateBatch(new SeatBulkUpdateForm(roomId,ids,SeatType.COUPLE,false)))
   .isInstanceOf(DataIntegrityViolationException.class);
  assertThat(seats.getById(oldId).seatType()).isEqualTo(SeatType.VIP);
  assertThat(seats.getById(oldId).active()).isTrue();
  assertThat(seats.getById(added.getFirst().id()).seatType()).isEqualTo(SeatType.NORMAL);
  assertThat(seats.getById(added.getFirst().id()).active()).isTrue();
 }
 @Test void previewDoesNotReservePositionsAndCreateRevalidates(){
  var form=batch(row("B",1,3,SeatType.NORMAL));
  seats.previewBatch(form);
  var intervening=seats.create(new SeatForm(roomId,"B",2,SeatType.VIP,true));
  assertThatThrownBy(()->seats.createBatch(form)).isInstanceOf(ScheduleValidationException.class).hasMessageContaining("B2");
  assertThat(seatRepository.count()).isEqualTo(2);
  assertThat(seats.getById(intervening.getId()).seatType()).isEqualTo(SeatType.VIP);
 }
 MockHttpServletRequestBuilder batchPost(String path){
  return post(path).with(csrf()).param("roomId",roomId.toString())
   .param("rows[0].rowLabel","B").param("rows[0].firstNumber","1").param("rows[0].lastNumber","12")
   .param("rows[0].seatType","NORMAL").param("_rows[0].active","on").param("rows[0].active","true");
 }
 @Test void batchPageAndPreviewRenderWithoutOpenInView() throws Exception {
  mvc.perform(get("/admin/seats/batch").param("roomId",roomId.toString())).andExpect(status().isOk())
   .andExpect(content().string(containsString("rows[0].rowLabel"))).andExpect(content().string(containsString("_csrf")));
  mvc.perform(batchPost("/admin/seats/batch/preview")).andExpect(status().isOk())
   .andExpect(model().attributeExists("previewRows","previewCount")).andExpect(content().string(containsString("B12")));
  assertThat(seatRepository.count()).isEqualTo(1);
 }
 @Test void webCreatesMultipleRowsAndHonorsUncheckedActive() throws Exception {
  mvc.perform(batchPost("/admin/seats/batch")
   .param("rows[1].rowLabel","C").param("rows[1].firstNumber","1").param("rows[1].lastNumber","2")
   .param("rows[1].seatType","COUPLE").param("_rows[1].active","on"))
   .andExpect(status().is3xxRedirection());
  assertThat(seatRepository.count()).isEqualTo(15);
  assertThat(seats.findByRoom(roomId)).filteredOn(s->s.rowLabel().equals("C")).allMatch(s->!s.active()&&s.seatType()==SeatType.COUPLE);
 }
 @Test void webDuplicateAndInvalidRangesRenderErrorsWithoutWrites() throws Exception {
  mvc.perform(post("/admin/seats/batch").with(csrf()).param("roomId",roomId.toString())
   .param("rows[0].rowLabel","A").param("rows[0].firstNumber","1").param("rows[0].lastNumber","3").param("rows[0].seatType","NORMAL"))
   .andExpect(status().isOk()).andExpect(model().attributeHasErrors("form")).andExpect(content().string(containsString("A1")));
  mvc.perform(post("/admin/seats/batch/preview").with(csrf()).param("roomId",roomId.toString())
   .param("rows[0].rowLabel","B").param("rows[0].firstNumber","abc").param("rows[0].lastNumber","3").param("rows[0].seatType","NORMAL"))
   .andExpect(status().isOk()).andExpect(model().attributeHasErrors("form"));
  assertThat(seatRepository.count()).isEqualTo(1);
 }
 @Test void webBulkUpdateAndSelectionErrors() throws Exception {
  mvc.perform(post("/admin/seats/batch-update").with(csrf()).param("roomId",roomId.toString()).param("seatIds",oldId.toString()).param("seatType","COUPLE").param("active","false"))
   .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("success"));
  admin();assertThat(seats.getById(oldId).seatType()).isEqualTo(SeatType.COUPLE);
  mvc.perform(post("/admin/seats/batch-update").with(csrf()).param("roomId",roomId.toString()).param("seatType","VIP"))
   .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("failure"));
 }
 @Test void batchRoutesRequireAdminAndCsrf() throws Exception {
  var customer=user("customer").roles("CUSTOMER");
  mvc.perform(get("/admin/seats/batch").with(customer).param("roomId",roomId.toString())).andExpect(status().isForbidden());
  for(String path:List.of("/admin/seats/batch","/admin/seats/batch/preview","/admin/seats/batch-update")){
   mvc.perform(post(path)).andExpect(status().isForbidden());
   mvc.perform(post(path).with(customer).with(csrf())).andExpect(status().isForbidden());
   mvc.perform(post(path).with(anonymous()).with(csrf())).andExpect(status().is3xxRedirection());
  }
 }
 @Test void directServiceCallsRequireAdmin(){
  org.springframework.security.core.context.SecurityContextHolder.clearContext();
  assertThatThrownBy(()->seats.previewBatch(batch(row("B",1,3,SeatType.NORMAL)))).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
  assertThatThrownBy(()->seats.createBatch(batch(row("B",1,3,SeatType.NORMAL)))).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
  assertThatThrownBy(()->seats.updateBatch(new SeatBulkUpdateForm(roomId,List.of(oldId),SeatType.NORMAL,false))).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
 }
 @Test void simultaneousBatchesCannotCreateDuplicatePositions() throws Exception {
  var pool=Executors.newFixedThreadPool(2);var ready=new CountDownLatch(2);var go=new CountDownLatch(1);
  Callable<Boolean> task=()->{
   admin();
   try{
    ready.countDown();if(!go.await(10,TimeUnit.SECONDS))throw new IllegalStateException("Timeout");
    try{seats.createBatch(batch(row("B",1,12,SeatType.NORMAL)));return true;}
    catch(ScheduleValidationException ex){return false;}
   }finally{org.springframework.security.core.context.SecurityContextHolder.clearContext();}
  };
  try{
   var a=pool.submit(task);var b=pool.submit(task);assertThat(ready.await(10,TimeUnit.SECONDS)).isTrue();go.countDown();
   assertThat(List.of(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);
   assertThat(seatRepository.count()).isEqualTo(13);
  }finally{go.countDown();pool.shutdownNow();}
 }
}
