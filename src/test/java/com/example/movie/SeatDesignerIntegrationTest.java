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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:person3_simple_web;MODE=MySQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc @ActiveProfiles("test") @WithMockUser(roles="ADMIN")
class SeatDesignerIntegrationTest {
 @Autowired MockMvc mvc; @Autowired SeatService seats; @Autowired RoomService rooms; @Autowired TheaterService theaters;
 @Autowired SeatRepository repository; @Autowired RoomRepository roomRepository; @Autowired TheaterRepository theaterRepository;
 @Autowired PlatformTransactionManager txManager;
 @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
 Long roomId;
 @BeforeEach void setup(){var t=theaters.create(new TheaterForm("Rạp đơn giản","Địa chỉ",true));roomId=rooms.create(new RoomForm(t.getId(),"A",RoomType.TWO_D,true)).getId();}
 @AfterEach void cleanup(){repository.deleteAll();roomRepository.deleteAll();theaterRepository.deleteAll();}
 org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder create(){return post("/admin/seats/layout").with(csrf()).param("roomId",roomId.toString()).param("rowCount","5").param("seatsPerRow","8");}
 org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder update(Long id,String type,String active){return post("/admin/seats/"+id).with(csrf()).param("roomId",roomId.toString()).param("rowLabel","A").param("seatNumber","1").param("seatType",type).param("active",active);}
 @Test void emptyRoomShowsOnlyInitialDimensionsAndGetNeverWrites() throws Exception {
  for(int i=0;i<2;i++)mvc.perform(get("/admin/seats").param("roomId",roomId.toString())).andExpect(status().isOk())
   .andExpect(content().string(containsString("id=\"create-layout-form\""))).andExpect(content().string(containsString("Số hàng ghế"))).andExpect(content().string(containsString("Số ghế mỗi hàng"))).andExpect(content().string(containsString("_csrf")));
  assertThat(repository.count()).isZero();
 }
 @Test void createsFortyAndSubsequentGetKeepsSizeControlsWithoutAddingSeats() throws Exception {
  mvc.perform(create()).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/seats?roomId="+roomId));
  var before=seats.findByRoom(roomId);assertThat(before).hasSize(40);
  for(int i=0;i<2;i++)mvc.perform(get("/admin/seats").param("roomId",roomId.toString())).andExpect(status().isOk())
   .andExpect(content().string(containsString("id=\"create-layout-form\""))).andExpect(content().string(containsString("Cập nhật sơ đồ"))).andExpect(content().string(containsString("E8")));
  assertThat(seats.findByRoom(roomId)).containsExactlyElementsOf(before);
 }
 @Test void existingRoomCanGrowPreservingSavedSeats() throws Exception {
  var before=seats.createLayout(new SeatLayoutCreateForm(roomId,1,2));
  mvc.perform(create()).andExpect(status().is3xxRedirection());
  assertThat(seats.findByRoom(roomId)).hasSize(40).containsAll(before);
 }
 @Test void invalidDimensionsRenderVietnameseValidation() throws Exception {
  mvc.perform(post("/admin/seats/layout").with(csrf()).param("roomId",roomId.toString()).param("rowCount","0").param("seatsPerRow","8")).andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("createForm","rowCount"));assertThat(repository.count()).isZero();
 }
 @Test void invalidNumericDimensionsNeverCreateSeats() throws Exception {
  mvc.perform(post("/admin/seats/layout").with(csrf()).param("roomId",roomId.toString()).param("rowCount","abc").param("seatsPerRow","8"))
   .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("createForm","rowCount"));assertThat(repository.count()).isZero();
 }
 @Test void noRetiredControlsOrCoupleOptionsRendered() throws Exception {
  var s=seats.createLayout(new SeatLayoutCreateForm(roomId,1,2)).getFirst();
  for(String url:List.of("/admin/seats?roomId="+roomId,"/admin/seats/"+s.id()+"/edit")){
   var response=mvc.perform(get(url)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
   for(String removed:List.of("value=\"COUPLE\"","Ghế đôi","row-gaps","row-first","row-panel","selection-type","batch-update","generate-layout","type=\"checkbox\" name=\"seatIds\""))assertThat(response).doesNotContain(removed);
  }
 }
 @Test void updatesVipThenNormalWithoutChangingId() throws Exception {
  var s=seats.createLayout(new SeatLayoutCreateForm(roomId,1,1)).getFirst();
  mvc.perform(update(s.id(),"VIP","true")).andExpect(status().is3xxRedirection());assertThat(repository.findById(s.id()).orElseThrow().getSeatType()).isEqualTo(SeatType.VIP);
  mvc.perform(update(s.id(),"NORMAL","true")).andExpect(status().is3xxRedirection());assertThat(seats.findByRoom(roomId)).containsExactly(s);
 }
 @Test void updateUncheckedActiveAndExplicitReactivate() throws Exception {
  var s=seats.createLayout(new SeatLayoutCreateForm(roomId,1,1)).getFirst();
  mvc.perform(post("/admin/seats/"+s.id()).with(csrf()).param("roomId",roomId.toString()).param("rowLabel","A").param("seatNumber","1").param("seatType","NORMAL").param("_active","on"))
   .andExpect(status().is3xxRedirection());assertThat(repository.findById(s.id()).orElseThrow().isActive()).isFalse();
  mvc.perform(update(s.id(),"NORMAL","true")).andExpect(status().is3xxRedirection());assertThat(repository.findById(s.id()).orElseThrow().isActive()).isTrue();
 }
 @Test void positionTamperingRejected() throws Exception {
  var s=seats.createLayout(new SeatLayoutCreateForm(roomId,1,1)).getFirst();
  mvc.perform(post("/admin/seats/"+s.id()).with(csrf()).param("roomId",roomId.toString()).param("rowLabel","B").param("seatNumber","1").param("seatType","VIP").param("active","true")).andExpect(status().isOk()).andExpect(model().attributeHasErrors("form"));assertThat(seats.findByRoom(roomId)).containsExactly(s);
 }
 @Test void coupleAndUnknownTypeRejected() throws Exception {
  var s=seats.createLayout(new SeatLayoutCreateForm(roomId,1,1)).getFirst();
  for(String type:List.of("COUPLE","UNKNOWN"))mvc.perform(update(s.id(),type,"true")).andExpect(status().isOk()).andExpect(model().attributeHasErrors("form"));
  assertThat(seats.findByRoom(roomId)).containsExactly(s);
 }
 @Test void removedEndpointsCannotMutateData() throws Exception {
  var original=seats.createLayout(new SeatLayoutCreateForm(roomId,1,2));
  for(String path:List.of("/admin/seats","/admin/seats/batch","/admin/seats/batch/preview","/admin/seats/batch-update","/admin/seats/designer"))
   mvc.perform(post(path).with(csrf()).param("roomId",roomId.toString())).andExpect(status().is4xxClientError());
  for(String path:List.of("/admin/seats/new","/admin/seats/batch"))mvc.perform(get(path).param("roomId",roomId.toString())).andExpect(status().is4xxClientError());
  assertThat(seats.findByRoom(roomId)).containsExactlyElementsOf(original);
 }
 @Test void adminAndCsrfEnforcedForInitializationAndSingleUpdate() throws Exception {
  var s=seats.createLayout(new SeatLayoutCreateForm(roomId,1,1)).getFirst();
  for(String path:List.of("/admin/seats/layout","/admin/seats/"+s.id())){
   mvc.perform(post(path)).andExpect(status().isForbidden());
   mvc.perform(post(path).with(user("customer").roles("CUSTOMER")).with(csrf())).andExpect(status().isForbidden());
   mvc.perform(post(path).with(anonymous()).with(csrf())).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
  }
  mvc.perform(get("/admin/seats").param("roomId",roomId.toString()).with(user("customer").roles("CUSTOMER"))).andExpect(status().isForbidden());
  assertThat(seats.findByRoom(roomId)).containsExactly(s);
 }
 @Test void legacyIrregularLayoutLoadsWithoutRebuildingOrReactivation() throws Exception {
  var old=new Seat();old.setRoom(roomRepository.findById(roomId).orElseThrow());old.setRowLabel("Z9");old.setSeatNumber(212);old.setSeatType(SeatType.COUPLE);old.setActive(false);old=repository.saveAndFlush(old);
  mvc.perform(get("/admin/seats").param("roomId",roomId.toString())).andExpect(status().isOk()).andExpect(content().string(containsString("Z9212")))
   .andExpect(content().string(containsString("Có ghế dùng loại cũ"))).andExpect(content().string(containsString("id=\"create-layout-form\"")));
  var stored=repository.findById(old.getId()).orElseThrow();assertThat(stored.isActive()).isFalse();assertThat(stored.getSeatType()).isEqualTo(SeatType.COUPLE);assertThat(repository.count()).isEqualTo(1);
 }
 @Test void emptyLegacyMetadataTableIsNotPartOfJpaModel(){
  assertThat(jdbc.queryForObject("select count(*) from information_schema.tables where table_name='SEAT_ROW_LAYOUTS'",Integer.class)).isZero();
 }
 @Test void shrinkWithoutConfirmationRendersErrorAndKeepsData() throws Exception {
  var original=seats.createLayout(new SeatLayoutCreateForm(roomId,6,10));
  mvc.perform(create()).andExpect(status().isOk()).andExpect(model().attributeHasErrors("createForm"))
   .andExpect(content().string(containsString("Cần xác nhận")));
  assertThat(seats.findByRoom(roomId)).containsExactlyElementsOf(original);
 }
 @Test void singleAndWholeDeletionRequireConfirmationAndCsrf() throws Exception {
  var original=seats.createLayout(new SeatLayoutCreateForm(roomId,1,2));var first=original.getFirst();
  mvc.perform(post("/admin/seats/"+first.id()+"/delete").with(csrf()).param("roomId",roomId.toString())).andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("failure"));
  assertThat(repository.count()).isEqualTo(2);
  mvc.perform(post("/admin/seats/"+first.id()+"/delete").with(csrf()).param("roomId",roomId.toString()).param("confirmed","true")).andExpect(status().is3xxRedirection());
  assertThat(repository.existsById(first.id())).isFalse();assertThat(repository.count()).isEqualTo(1);
  mvc.perform(post("/admin/seats/layout/delete").with(csrf()).param("roomId",roomId.toString()).param("confirmed","true")).andExpect(status().is3xxRedirection());
  assertThat(repository.count()).isZero();
 }
 @Test void customerAndAnonymousCannotDeleteEvenWithConfirmation() throws Exception {
  var original=seats.createLayout(new SeatLayoutCreateForm(roomId,1,2));
  for(String path:List.of("/admin/seats/"+original.getFirst().id()+"/delete","/admin/seats/layout/delete")){
   mvc.perform(post(path).param("roomId",roomId.toString()).param("confirmed","true")).andExpect(status().isForbidden());
   mvc.perform(post(path).with(user("customer").roles("CUSTOMER")).with(csrf()).param("roomId",roomId.toString()).param("confirmed","true")).andExpect(status().isForbidden());
   mvc.perform(post(path).with(anonymous()).with(csrf()).param("roomId",roomId.toString()).param("confirmed","true")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
  }
  assertThat(seats.findByRoom(roomId)).containsExactlyElementsOf(original);
 }
}
