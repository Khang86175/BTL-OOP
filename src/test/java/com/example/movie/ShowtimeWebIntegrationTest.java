package com.example.movie;

import com.example.movie.dto.*;
import com.example.movie.model.*;
import com.example.movie.repository.*;
import com.example.movie.service.*;
import com.example.movie.config.ShowtimeSeedData;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:person3_web;MODE=MySQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc @ActiveProfiles("test") @WithMockUser(roles="ADMIN")
class ShowtimeWebIntegrationTest {
 @Autowired MockMvc mvc;
 @Autowired TheaterService theaters; @Autowired RoomService rooms; @Autowired SeatService seats; @Autowired ShowtimeService schedules;
 @Autowired TheaterRepository theaterRepo; @Autowired RoomRepository roomRepo; @Autowired SeatRepository seatRepo;
 @Autowired ShowtimeRepository showRepo; @Autowired MovieRepository movieRepo;
 @Autowired ShowtimeSeatRepository showtimeSeatRepo; @Autowired BookingRepository bookingRepo; @Autowired UserRepository userRepo;
 @Autowired PlatformTransactionManager txManager; @Autowired ApplicationContext context;
 Long theaterId,roomId,seatId,movieId,showtimeId;
 LocalDateTime start;
 void admin(){
  org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
   new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("admin","",
    List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))));
 }

 @BeforeEach void fixture(){
  theaterId=theaters.create(new TheaterForm("Rạp Web","Địa chỉ thử",true)).getId();
  roomId=rooms.create(new RoomForm(theaterId,"Phòng Web",RoomType.IMAX,true)).getId();
  seatId=seats.createLayout(new SeatLayoutCreateForm(roomId,1,1)).getFirst().id();
  seats.update(seatId,new SeatForm(roomId,"A",1,SeatType.VIP,true));
  Movie m=new Movie();m.setTitle("Phim Web");m.setDirector("Đạo diễn");m.setDurationMinutes(100);
  m.setReleaseDate(LocalDate.now(Movie.CINEMA_ZONE));m.setAgeRating(AgeRating.P);m.setSynopsis("Nội dung");
  movieId=movieRepo.saveAndFlush(m).getId();
  start=LocalDate.now(Movie.CINEMA_ZONE).plusDays(3).atTime(19,0);
  showtimeId=schedules.create(new ShowtimeForm(movieId,roomId,start,new BigDecimal("80000"),new BigDecimal("100000"),ShowtimeStatus.OPEN)).id();
 }
 @AfterEach void cleanup(){
  new TransactionTemplate(txManager).executeWithoutResult(tx->{
   bookingRepo.deleteAll();showtimeSeatRepo.deleteAll();showRepo.deleteAll();showRepo.flush();seatRepo.deleteAll();seatRepo.flush();
   roomRepo.deleteAll();roomRepo.flush();theaterRepo.deleteAll();movieRepo.deleteAll();
   userRepo.findByEmail("user").ifPresent(userRepo::delete);
  });
 }
 @Test void customerCanOpenSeatMapAndBookAtConfiguredVipPrice() throws Exception {
  User user=new User();user.setFullName("Khách Web");user.setEmail("user");user.setPasswordHash("unused");userRepo.saveAndFlush(user);

  mvc.perform(get("/showtimes/"+showtimeId))
   .andExpect(status().isOk()).andExpect(content().string(containsString("Chọn ghế &amp; Đặt vé")));
  mvc.perform(get("/bookings/showtimes/"+showtimeId))
   .andExpect(status().isOk()).andExpect(content().string(containsString("A1")))
   .andExpect(content().string(containsString("Khám phá phim")));
  mvc.perform(post("/bookings").with(csrf()).param("showtimeId",showtimeId.toString()).param("seatIds",seatId.toString()))
   .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/bookings/confirmation"))
   .andExpect(flash().attributeExists("booking"));

  assertThat(bookingRepo.findAll()).singleElement().satisfies(booking ->
   assertThat(booking.getTotalAmount()).isEqualByComparingTo("100000"));
 }
 @Test void bookingConfirmationShowsSeatPriceAndPaymentNotice() throws Exception {
  BookingResponse booking=new BookingResponse(12L,showtimeId,"1",new BigDecimal("100000"),
   "CONFIRMED",LocalDateTime.now(Movie.CINEMA_ZONE),List.of("A1","A2"));
  mvc.perform(get("/bookings/confirmation").flashAttr("booking",booking))
   .andExpect(status().isOk())
   .andExpect(content().string(containsString("A1")))
   .andExpect(content().string(containsString("A2")))
   .andExpect(content().string(containsString("100,000.00 đ")))
   .andExpect(content().string(containsString("Thanh toán")))
   .andExpect(content().string(containsString("chưa được tích hợp")));
  mvc.perform(get("/bookings/confirmation")).andExpect(status().is3xxRedirection())
   .andExpect(redirectedUrl("/showtimes"));
 }
 @Test void adminListsAndAllFormsRenderWithoutOpenInView() throws Exception {
  for(String path:List.of("/admin/theaters","/admin/theaters/new","/admin/theaters/"+theaterId+"/edit",
   "/admin/rooms","/admin/rooms/new?theaterId="+theaterId,"/admin/rooms/"+roomId+"/edit",
   "/admin/seats?roomId="+roomId,"/admin/seats/"+seatId+"/edit",
   "/admin/showtimes","/admin/showtimes/new","/admin/showtimes/"+showtimeId+"/edit"))
   mvc.perform(get(path)).andExpect(status().isOk());
  mvc.perform(get("/admin/showtimes/new")).andExpect(content().string(containsString("_csrf")));
 }
 @Test void theaterCreateEditAndToggle() throws Exception {
  mvc.perform(post("/admin/theaters").with(csrf()).param("name","Rạp mới").param("address","Địa chỉ").param("active","true")).andExpect(status().is3xxRedirection());
  mvc.perform(post("/admin/theaters/"+theaterId).with(csrf()).param("name","Đổi tên").param("address","Mới").param("active","true")).andExpect(status().is3xxRedirection());
  mvc.perform(post("/admin/theaters/"+theaterId+"/active").with(csrf()).param("active","false")).andExpect(status().is3xxRedirection());
  assertThat(theaters.getById(theaterId).isActive()).isFalse();
 }
 @Test void invalidTheaterFormRendersVietnameseError() throws Exception {
  mvc.perform(post("/admin/theaters").with(csrf()).param("name"," ").param("address","").param("_active","on"))
   .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form","name","address"))
   .andExpect(content().string(containsString("Vui lòng nhập tên rạp.")));
 }
 @Test void duplicateRoomRendersBusinessError() throws Exception {
  mvc.perform(post("/admin/rooms").with(csrf()).param("theaterId",theaterId.toString()).param("name","Phòng Web").param("roomType","TWO_D").param("active","true"))
   .andExpect(status().isOk()).andExpect(model().attributeHasErrors("form")).andExpect(content().string(containsString("Tên phòng đã tồn tại")));
 }
 @Test void roomsCanBeCreatedEditedFilteredAndDisabled() throws Exception {
  mvc.perform(post("/admin/rooms").with(csrf()).param("theaterId",theaterId.toString()).param("name","B").param("roomType","THREE_D").param("active","true")).andExpect(status().is3xxRedirection());
  mvc.perform(post("/admin/rooms/"+roomId).with(csrf()).param("theaterId",theaterId.toString()).param("name","Đổi phòng").param("roomType","TWO_D").param("active","true")).andExpect(status().is3xxRedirection());
  mvc.perform(get("/admin/rooms").param("theaterId",theaterId.toString())).andExpect(model().attribute("rooms",hasSize(2)));
  mvc.perform(post("/admin/rooms/"+roomId+"/active").with(csrf()).param("active","false")).andExpect(status().is3xxRedirection());
  admin();assertThat(rooms.getSummary(roomId).active()).isFalse();
 }
 @Test void seatEditKeepsIdAndShowsInactiveLabel() throws Exception {
  mvc.perform(post("/admin/seats/"+seatId).with(csrf()).param("roomId",roomId.toString()).param("rowLabel","A").param("seatNumber","1").param("seatType","NORMAL").param("active","false")).andExpect(status().is3xxRedirection());
  admin();assertThat(seats.getById(seatId).seatType()).isEqualTo(SeatType.NORMAL);
  mvc.perform(get("/admin/seats").param("roomId",roomId.toString())).andExpect(content().string(containsString("Ngừng dùng"))).andExpect(content().string(containsString("Thường")));
  assertThat(seatRepo.count()).isEqualTo(1);
 }
 @Test void existingLayoutCanGrowWithoutChangingOriginalSeat() throws Exception {
  mvc.perform(post("/admin/seats/layout").with(csrf()).param("roomId",roomId.toString()).param("rowCount","5").param("seatsPerRow","8"))
   .andExpect(status().is3xxRedirection());
  mvc.perform(post("/admin/seats").with(csrf()).param("roomId",roomId.toString()).param("rowLabel","A").param("seatNumber","2").param("seatType","NORMAL").param("active","true")).andExpect(status().is4xxClientError());
  assertThat(seatRepo.count()).isEqualTo(40);
  assertThat(seatRepo.findById(seatId).orElseThrow().getSeatType()).isEqualTo(SeatType.VIP);
 }
 org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder showPost(String url,LocalDateTime time){
  return post(url).with(csrf()).param("movieId",movieId.toString()).param("roomId",roomId.toString())
   .param("startTime",time.toString()).param("normalPrice","95000").param("vipPrice","115000").param("status","OPEN");
 }
 @Test void showtimeCreateEditAndStatusTransitions() throws Exception {
  mvc.perform(showPost("/admin/showtimes",start.plusHours(3))).andExpect(status().is3xxRedirection());
  mvc.perform(showPost("/admin/showtimes/"+showtimeId,start.plusMinutes(10))).andExpect(status().is3xxRedirection());
  mvc.perform(post("/admin/showtimes/"+showtimeId+"/status").with(csrf()).param("status","CLOSED")).andExpect(status().is3xxRedirection());
  admin();assertThat(schedules.getForAdmin(showtimeId).status()).isEqualTo(ShowtimeStatus.CLOSED);
  mvc.perform(post("/admin/showtimes/"+showtimeId+"/status").with(csrf()).param("status","CANCELLED")).andExpect(status().is3xxRedirection());
  admin();assertThat(schedules.getForAdmin(showtimeId).status()).isEqualTo(ShowtimeStatus.CANCELLED);
 }
 @Test void overlapPastPriceAndBadDateAreRejectedAndRender() throws Exception {
  mvc.perform(showPost("/admin/showtimes",start.plusMinutes(30))).andExpect(status().isOk()).andExpect(model().attributeHasErrors("form")).andExpect(content().string(containsString("Lịch chiếu trùng")));
  mvc.perform(showPost("/admin/showtimes",start.minusDays(5))).andExpect(status().isOk()).andExpect(model().attributeHasErrors("form"));
  mvc.perform(post("/admin/showtimes").with(csrf()).param("movieId",movieId.toString()).param("roomId",roomId.toString()).param("startTime",start.plusHours(3).toString()).param("status","OPEN").param("normalPrice","-1").param("vipPrice","100000")).andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form","normalPrice"));
  mvc.perform(post("/admin/showtimes").with(csrf()).param("startTime","khong-phai-ngay").param("normalPrice","abc").param("vipPrice","xyz"))
   .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form","startTime","normalPrice","vipPrice"));
 }
 @Test void hiddenMovieAndInactiveRoomReturnFormErrors() throws Exception {
  rooms.setActive(roomId,false);
  mvc.perform(showPost("/admin/showtimes",start.plusHours(3))).andExpect(status().isOk()).andExpect(model().attributeHasErrors("form"));
  admin();rooms.setActive(roomId,true);
  new TransactionTemplate(txManager).executeWithoutResult(tx->movieRepo.findById(movieId).orElseThrow().setVisible(false));
  mvc.perform(showPost("/admin/showtimes",start.plusHours(3))).andExpect(status().isOk()).andExpect(model().attributeHasErrors("form"));
 }
 @Test void guestPagesFilterAndRender() throws Exception {
  mvc.perform(get("/showtimes").param("movieId",movieId.toString()).param("theaterId",theaterId.toString()).param("date",start.toLocalDate().toString()))
   .andExpect(status().isOk()).andExpect(model().attribute("showtimes",hasSize(1))).andExpect(content().string(containsString("Phim Web")));
  mvc.perform(get("/showtimes/"+showtimeId)).andExpect(status().isOk()).andExpect(content().string(containsString("80,000")));
  mvc.perform(get("/showtimes").param("date",start.toLocalDate().plusDays(1).toString())).andExpect(model().attribute("showtimes",hasSize(0)));
  mvc.perform(get("/showtimes").param("movieId","999999").param("date",start.toLocalDate().toString())).andExpect(model().attribute("showtimes",hasSize(0)));
  mvc.perform(get("/showtimes").param("theaterId","999999").param("date",start.toLocalDate().toString())).andExpect(model().attribute("showtimes",hasSize(0)));
 }
 @Test void closedCancelledAndHiddenShowtimesAreNotSelectable() throws Exception {
  schedules.changeStatus(showtimeId,ShowtimeStatus.CLOSED);
  mvc.perform(get("/showtimes/"+showtimeId)).andExpect(status().isNotFound());
  mvc.perform(get("/showtimes").param("date",start.toLocalDate().toString())).andExpect(model().attribute("showtimes",hasSize(0)));
  admin();schedules.changeStatus(showtimeId,ShowtimeStatus.CANCELLED);
  mvc.perform(get("/showtimes/"+showtimeId)).andExpect(status().isNotFound());
 }
 @Test void malformedFiltersAndUnknownIdsReturnClientErrors() throws Exception {
  mvc.perform(get("/showtimes").param("date","not-a-date")).andExpect(status().isBadRequest());
  mvc.perform(get("/showtimes/999999")).andExpect(status().isNotFound());
  mvc.perform(get("/admin/seats/999999/edit")).andExpect(status().isNotFound());
 }
 @Test void allAdminMutationsRequireCsrf() throws Exception {
  for(String path:List.of("/admin/theaters","/admin/theaters/"+theaterId,"/admin/theaters/"+theaterId+"/active",
    "/admin/rooms","/admin/rooms/"+roomId,"/admin/rooms/"+roomId+"/active","/admin/seats","/admin/seats/"+seatId,
    "/admin/showtimes","/admin/showtimes/"+showtimeId,"/admin/showtimes/"+showtimeId+"/status"))
   mvc.perform(post(path)).andExpect(status().isForbidden());
 }
 @Test void customerCanBrowseButCannotManage() throws Exception {
  // Override authentication per request; fixture remains authorized Admin.
  var customer=org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("customer").roles("CUSTOMER");
  mvc.perform(get("/showtimes").with(customer)).andExpect(status().isOk());
  for(String path:List.of("/admin/theaters","/admin/rooms","/admin/seats?roomId="+roomId,"/admin/showtimes"))
   mvc.perform(get(path).with(customer)).andExpect(status().isForbidden());
  for(String path:List.of("/admin/theaters","/admin/rooms","/admin/seats","/admin/showtimes","/admin/showtimes/"+showtimeId+"/status"))
   mvc.perform(post(path).with(customer).with(csrf())).andExpect(status().isForbidden());
 }
 @Test void anonymousCanBrowseShowtimesButAdminStillRequiresLogin() throws Exception {
  var anonymous=org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous();
  mvc.perform(get("/showtimes").with(anonymous).param("date",start.toLocalDate().toString()))
   .andExpect(status().isOk()).andExpect(model().attribute("showtimes",hasSize(1)));
  mvc.perform(get("/showtimes/"+showtimeId).with(anonymous)).andExpect(status().isOk());
  for(String path:List.of("/admin/theaters","/admin/rooms","/admin/seats?roomId="+roomId,"/admin/showtimes"))
   mvc.perform(get(path).with(anonymous)).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
 }
 @Test void anonymousMovieDetailLinksToItsOwnShowtimesAndKeepsMovieContent() throws Exception {
  var anonymous=org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous();
  mvc.perform(get("/movies/"+movieId).with(anonymous))
   .andExpect(status().isOk())
   .andExpect(content().string(containsString("href=\"/showtimes?movieId="+movieId+"\"")))
   .andExpect(content().string(containsString("Xem lịch chiếu")))
   .andExpect(content().string(containsString("Phim Web")))
   .andExpect(content().string(containsString("100 phút")))
   .andExpect(content().string(containsString("Nội dung")));
 }
 @Test void navbarShowsPublicLinkAndAdminItemsOnlyForAdmin() throws Exception {
  var anonymous=org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous();
  var customer=org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("customer").roles("CUSTOMER");
  var adminUser=org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN");
  for(var visitor:List.of(anonymous,customer)){
   mvc.perform(get("/movies/"+movieId).with(visitor))
    .andExpect(content().string(containsString("href=\"/showtimes\"")))
    .andExpect(content().string(not(containsString("href=\"/admin/theaters\""))))
    .andExpect(content().string(not(containsString("href=\"/admin/rooms\""))))
    .andExpect(content().string(not(containsString("href=\"/admin/showtimes\""))))
    .andExpect(content().string(not(containsString("href=\"/admin/movies\""))));
  }
  mvc.perform(get("/movies/"+movieId).with(adminUser))
   .andExpect(content().string(containsString("href=\"/admin/theaters\"")))
   .andExpect(content().string(containsString("href=\"/admin/rooms\"")))
   .andExpect(content().string(containsString("href=\"/admin/showtimes\"")))
   .andExpect(content().string(containsString("href=\"/admin/movies\"")))
   .andExpect(content().string(containsString("href=\"/admin/categories\"")));
 }
 @Test void publicGetMatcherDoesNotOpenOtherMethodsOrSimilarPaths() throws Exception {
  var anonymous=org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous();
  for(String path:List.of("/showtimes","/showtimes/"+showtimeId)){
   mvc.perform(post(path).with(anonymous)).andExpect(status().isForbidden());
   mvc.perform(post(path).with(anonymous).with(csrf())).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
   mvc.perform(put(path).with(anonymous).with(csrf())).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
   mvc.perform(delete(path).with(anonymous).with(csrf())).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
  }
  mvc.perform(get("/showtimes-other").with(anonymous)).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
 }
 @Test void anonymousCannotMutateAdminEvenWithCsrf() throws Exception {
  var anonymous=org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous();
  for(String path:List.of("/admin/theaters","/admin/rooms","/admin/seats","/admin/showtimes","/admin/showtimes/"+showtimeId+"/status")){
   mvc.perform(post(path).with(anonymous)).andExpect(status().isForbidden());
   mvc.perform(post(path).with(anonymous).with(csrf())).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
  }
 }
 @Test void seedIsDisabledByDefaultOutsideDemo(){assertThat(context.getBeansOfType(ShowtimeSeedData.class)).isEmpty();}
 @Test void postedEndTimeIsIgnored() throws Exception {
  mvc.perform(showPost("/admin/showtimes",start.plusHours(3)).param("endTime","2099-01-01T23:59")).andExpect(status().is3xxRedirection());
  admin();var created=schedules.findForAdmin().stream().filter(s->!s.id().equals(showtimeId)).findFirst().orElseThrow();
  assertThat(created.endTime()).isEqualTo(created.startTime().plusMinutes(100));
 }
 @Test void cancelledCannotBeClosedWithoutConflictValidation() throws Exception {
  schedules.changeStatus(showtimeId,ShowtimeStatus.CANCELLED);
  mvc.perform(post("/admin/showtimes/"+showtimeId+"/status").with(csrf()).param("status","CLOSED"))
   .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("failure"));
  admin();assertThat(schedules.getForAdmin(showtimeId).status()).isEqualTo(ShowtimeStatus.CANCELLED);
 }
 @Test void uncheckedActiveCheckboxCreatesInactiveTheater() throws Exception {
  mvc.perform(post("/admin/theaters").with(csrf()).param("name","Rạp tắt").param("address","Thử").param("_active","on"))
   .andExpect(status().is3xxRedirection());
  admin();assertThat(theaters.findForAdmin().stream().filter(t->t.getName().equals("Rạp tắt")).findFirst().orElseThrow().isActive()).isFalse();
 }
 @Test void oldHiddenMovieScheduleCanStillBeCancelled() throws Exception {
  new TransactionTemplate(txManager).executeWithoutResult(tx->{
   var s=showRepo.findById(showtimeId).orElseThrow();
   s.setStartTime(start.minusDays(10));s.setEndTime(start.minusDays(10).plusMinutes(100));
   movieRepo.findById(movieId).orElseThrow().setVisible(false);
  });
  mvc.perform(post("/admin/showtimes/"+showtimeId+"/status").with(csrf()).param("status","CANCELLED"))
   .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("success"));
  admin();assertThat(schedules.getForAdmin(showtimeId).status()).isEqualTo(ShowtimeStatus.CANCELLED);
 }
 @Test void userContentIsEscapedInNewTemplates() throws Exception {
  theaters.update(theaterId,new TheaterForm("<script>alert(1)</script>","Địa chỉ",true));
  mvc.perform(get("/admin/theaters")).andExpect(content().string(not(containsString("<script>alert(1)</script>"))))
   .andExpect(content().string(containsString("&lt;script&gt;")));
 }
 org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder pricePost(String url,String normal,String vip){
  var request=post(url).with(csrf()).param("movieId",movieId.toString()).param("roomId",roomId.toString()).param("startTime",start.toString()).param("status","OPEN");
  if(normal!=null)request.param("normalPrice",normal);if(vip!=null)request.param("vipPrice",vip);return request;
 }
 @Test void showtimeFormsExposeTwoRequiredPositivePriceInputsOnly() throws Exception {
  for(String path:List.of("/admin/showtimes/new","/admin/showtimes/"+showtimeId+"/edit")){
   mvc.perform(get(path)).andExpect(status().isOk()).andExpect(content().string(containsString("Giá vé thường (VNĐ)")))
    .andExpect(content().string(containsString("Giá vé VIP (VNĐ)"))).andExpect(content().string(containsString("name=\"normalPrice\"")))
    .andExpect(content().string(containsString("name=\"vipPrice\""))).andExpect(content().string(not(containsString("name=\"basePrice\""))));
  }
 }
 @Test void normalAndVipCanBeEditedIndependentlyOverHttp() throws Exception {
  mvc.perform(pricePost("/admin/showtimes/"+showtimeId,"85000","100000")).andExpect(status().is3xxRedirection());
  assertThat(showRepo.findById(showtimeId).orElseThrow().getNormalPrice()).isEqualByComparingTo("85000");assertThat(showRepo.findById(showtimeId).orElseThrow().getVipPrice()).isEqualByComparingTo("100000");
  mvc.perform(pricePost("/admin/showtimes/"+showtimeId,"85000","70000")).andExpect(status().is3xxRedirection());
  mvc.perform(get("/admin/showtimes/"+showtimeId+"/edit")).andExpect(status().isOk()).andExpect(content().string(containsString("85000"))).andExpect(content().string(containsString("70000")));
  assertThat(showRepo.findById(showtimeId).orElseThrow().getBasePrice()).isEqualByComparingTo("80000");
 }
 @Test void missingEitherPriceOrLegacyOnlySubmissionIsRejected() throws Exception {
  mvc.perform(pricePost("/admin/showtimes/"+showtimeId,null,"100000")).andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form","normalPrice"));
  mvc.perform(pricePost("/admin/showtimes/"+showtimeId,"80000",null)).andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form","vipPrice"));
  mvc.perform(pricePost("/admin/showtimes/"+showtimeId,null,null).param("basePrice","99000")).andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form","normalPrice","vipPrice"));
  assertThat(showRepo.findById(showtimeId).orElseThrow().getNormalPrice()).isEqualByComparingTo("80000");
 }
 @Test void malformedAndNonPositivePricesRejectedWithoutChangingStoredValues() throws Exception {
  for(String value:List.of("0","-1","abc","NaN","Infinity","0.001","10000000000")){
   mvc.perform(pricePost("/admin/showtimes/"+showtimeId,value,"100000")).andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form","normalPrice"));
   mvc.perform(pricePost("/admin/showtimes/"+showtimeId,"80000",value)).andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form","vipPrice"));
  }
  assertThat(showRepo.findById(showtimeId).orElseThrow().getNormalPrice()).isEqualByComparingTo("80000");assertThat(showRepo.findById(showtimeId).orElseThrow().getVipPrice()).isEqualByComparingTo("100000");
 }
 @Test void anonymousListAndDetailShowBothPrices() throws Exception {
  mvc.perform(get("/showtimes").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous()).param("date",start.toLocalDate().toString()))
   .andExpect(status().isOk()).andExpect(content().string(containsString("80,000.00"))).andExpect(content().string(containsString("100,000.00")));
  mvc.perform(get("/showtimes/"+showtimeId).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous()))
   .andExpect(status().isOk()).andExpect(content().string(containsString("Giá vé thường"))).andExpect(content().string(containsString("Giá vé VIP")))
   .andExpect(content().string(containsString("80,000.00"))).andExpect(content().string(containsString("100,000.00")));
 }
 @Test void legacyPricesRemainVisibleToAdminAndRequireExplicitConfiguration() throws Exception {
  var old=new Showtime();old.setMovie(movieRepo.findById(movieId).orElseThrow());old.setRoom(roomRepo.findById(roomId).orElseThrow());old.setStartTime(start.plusHours(3));old.setEndTime(start.plusHours(3).plusMinutes(100));old.setBasePrice(new BigDecimal("75000"));old.setStatus(ShowtimeStatus.OPEN);old=showRepo.saveAndFlush(old);
  mvc.perform(get("/admin/showtimes")).andExpect(status().isOk()).andExpect(content().string(containsString("Chưa cập nhật")));
  mvc.perform(get("/admin/showtimes/"+old.getId()+"/edit")).andExpect(status().isOk()).andExpect(content().string(containsString("Lịch cũ chưa có đủ hai mức giá")));
  mvc.perform(get("/showtimes/"+old.getId()).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous())).andExpect(status().isNotFound());
  assertThat(showRepo.findById(old.getId()).orElseThrow().getBasePrice()).isEqualByComparingTo("75000");
 }
}
