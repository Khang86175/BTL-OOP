package com.example.movie.service;

import com.example.movie.dto.*;
import com.example.movie.model.*;
import com.example.movie.repository.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @Validated @Transactional(readOnly=true)
public class SeatService {
 private final SeatRepository seats;
 private final RoomRepository rooms;
 private final SeatReferenceGuard references;
 public SeatService(SeatRepository seats,RoomRepository rooms,SeatReferenceGuard references){this.seats=seats;this.rooms=rooms;this.references=references;}

 public List<SeatSummary> findByRoom(Long roomId){
  return seats.findByRoomIdOrderByRowLabelAscSeatNumberAsc(roomId).stream()
   .sorted(Comparator.comparingInt((Seat s)->rowOrder(s.getRowLabel())).thenComparing(Seat::getRowLabel).thenComparingInt(Seat::getSeatNumber))
   .map(SeatSummary::from).toList();
 }
 public SeatSummary getById(Long id){
  ScheduleAccess.admin();
  return SeatSummary.from(requireSeat(id));
 }

 /** Strict initialization remains available to existing Java callers. The designer uses saveLayout. */
 @Transactional
 public List<SeatSummary> createLayout(@NotNull @Valid SeatLayoutCreateForm form){
  ScheduleAccess.admin();
  if((long)form.rowCount()*form.seatsPerRow()>1000)
   throw new ScheduleValidationException("rowCount","Mỗi phòng tối đa 1.000 ghế khi tạo sơ đồ.");
  var room=rooms.findLockedById(form.roomId()).orElseThrow(()->new ScheduleValidationException("roomId","Không tìm thấy phòng."));
  if(seats.existsByRoomId(room.getId()))
   throw new ScheduleValidationException("roomId","Phòng đã có ghế. Chỉ được sửa loại và trạng thái từng ghế, không tạo lại sơ đồ.");
  return saveLayout(form);
 }

 /** Resize directly; unchanged positions keep their ID/type/state. Nothing is written until validation completes. */
 @Transactional
 public List<SeatSummary> saveLayout(@NotNull @Valid SeatLayoutCreateForm form){
  ScheduleAccess.admin();
  if((long)form.rowCount()*form.seatsPerRow()>1000)throw new ScheduleValidationException("rowCount","Mỗi phòng tối đa 1.000 ghế.");
  var room=lockRoom(form.roomId());var existing=seats.findLockedByRoomId(room.getId());
  var desired=new LinkedHashSet<String>();
  for(int r=1;r<=form.rowCount();r++)for(int n=1;n<=form.seatsPerRow();n++)desired.add(position(rowLabel(r),n));
  var occupied=new HashMap<String,Seat>();
  for(var seat:existing)if(occupied.put(position(seat.getRowLabel(),seat.getSeatNumber()),seat)!=null)
   throw new ScheduleValidationException("seats","Dữ liệu vị trí ghế cũ bị trùng sau chuẩn hóa; chưa thay đổi sơ đồ.");
  var removed=existing.stream().filter(s->!desired.contains(position(s.getRowLabel(),s.getSeatNumber()))).toList();
  requireRemovalConfirmation(removed,Boolean.TRUE.equals(form.confirmRemoval()));references.assertUnreferenced(removed);
  var additions=new ArrayList<Seat>();
  for(int r=1;r<=form.rowCount();r++)for(int n=1;n<=form.seatsPerRow();n++)if(!occupied.containsKey(position(rowLabel(r),n))){
   var seat=new Seat();seat.setRoom(room);seat.setRowLabel(rowLabel(r));seat.setSeatNumber(n);seat.setSeatType(SeatType.NORMAL);seat.setActive(true);additions.add(seat);
  }
  seats.deleteAll(removed);seats.flush();seats.saveAllAndFlush(additions);
  return findByRoom(room.getId());
 }
 @Transactional
 public void deleteSeat(Long roomId,Long seatId,boolean confirmed){
  ScheduleAccess.admin();lockRoom(roomId);
  var selected=seats.findLockedByRoomId(roomId).stream().filter(s->s.getId().equals(seatId)).findFirst()
   .orElseThrow(()->new ScheduleValidationException("seatId","Ghế không tồn tại trong phòng này."));
  var removed=List.of(selected);requireRemovalConfirmation(removed,confirmed);references.assertUnreferenced(removed);
  seats.delete(selected);seats.flush();
 }
 @Transactional
 public void deleteLayout(Long roomId,boolean confirmed){
  ScheduleAccess.admin();lockRoom(roomId);var removed=seats.findLockedByRoomId(roomId);
  requireRemovalConfirmation(removed,confirmed);references.assertUnreferenced(removed);
  seats.deleteAll(removed);seats.flush();
 }
 private Room lockRoom(Long roomId){return rooms.findLockedById(roomId).orElseThrow(()->new ScheduleValidationException("roomId","Không tìm thấy phòng."));}
 private static void requireRemovalConfirmation(List<Seat> removed,boolean confirmed){
  if(!removed.isEmpty()&&!confirmed)throw new ScheduleValidationException("confirmRemoval","Cần xác nhận thao tác xóa ghế đã lưu. Chưa thay đổi dữ liệu.");
 }
 private static String position(String row,int number){return row.strip().toUpperCase(Locale.ROOT)+":"+number;}

 /** Only type/state may change, including for irregular legacy layouts. */
 @Transactional
 public Seat update(Long id,@NotNull @Valid SeatForm form){
  ScheduleAccess.admin();
  if(!form.seatType().isSupported())
   throw new ScheduleValidationException("seatType","Chỉ hỗ trợ ghế Thường và VIP. Hãy chọn rõ loại ghế để chuyển đổi dữ liệu cũ.");
  rooms.findLockedById(form.roomId()).orElseThrow(()->new ScheduleValidationException("roomId","Không tìm thấy phòng."));
  var seat=requireSeat(id);
  if(!seat.getRoom().getId().equals(form.roomId())||!seat.getRowLabel().equals(form.rowLabel())||!seat.getSeatNumber().equals(form.seatNumber()))
   throw new ScheduleValidationException("seatId","Không được đổi phòng, hàng hoặc số ghế đã lưu.");
  seat.setSeatType(form.seatType());seat.setActive(form.active());
  return seats.saveAndFlush(seat);
 }
 private Seat requireSeat(Long id){
  return seats.findById(id).orElseThrow(()->new ScheduleValidationException("seatId","Không tìm thấy ghế."));
 }
 private static String rowLabel(int number){
  var label=new StringBuilder();
  while(number>0){number--;label.insert(0,(char)('A'+number%26));number/=26;}
  return label.toString();
 }
 private static int rowOrder(String row){
  String label=row.strip().toUpperCase(Locale.ROOT);if(!label.matches("[A-Z]{1,3}"))return Integer.MAX_VALUE;
  int number=0;for(char c:label.toCharArray())number=number*26+c-'A'+1;return number;
 }
}
