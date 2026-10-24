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
 private final SeatRepository seats; private final RoomRepository rooms;
 public SeatService(SeatRepository seats,RoomRepository rooms){this.seats=seats;this.rooms=rooms;}
 public List<SeatSummary> findByRoom(Long roomId){return seats.findByRoomIdOrderByRowLabelAscSeatNumberAsc(roomId).stream().map(SeatSummary::from).toList();}
 public SeatSummary getById(Long id){
  ScheduleAccess.admin();
  return SeatSummary.from(seats.findById(id).orElseThrow(()->new ScheduleValidationException("seatId","Không tìm thấy ghế.")));
 }
 @Transactional public Seat create(@NotNull @Valid SeatForm f){ScheduleAccess.admin();return save(null,f);}
 @Transactional public Seat update(Long id,@NotNull @Valid SeatForm f){ScheduleAccess.admin();return save(id,f);}
 public SeatLayoutState loadLayout(Long roomId){
  ScheduleAccess.admin();
  rooms.findById(roomId).orElseThrow(()->new ScheduleValidationException("roomId","Không tìm thấy phòng."));
  var all=seats.findByRoomIdOrderByRowLabelAscSeatNumberAsc(roomId);
  return new SeatLayoutState(all.stream().map(SeatSummary::from).toList(),snapshot(roomId,all));
 }
 /** Merge a full draft while preserving every persisted seat identity and position. */
 @Transactional
 public SeatLayoutResult saveLayout(@NotNull @Valid SeatLayoutForm f){
  ScheduleAccess.admin();
  var room=rooms.findLockedById(f.roomId()).orElseThrow(()->new ScheduleValidationException("roomId","Không tìm thấy phòng."));
  var existing=seats.findByRoomIdOrderByRowLabelAscSeatNumberAsc(room.getId());
  if(!snapshot(room.getId(),existing).equals(f.snapshot()))
   throw new ScheduleValidationException("snapshot","Sơ đồ đã được thay đổi ở nơi khác. Chưa lưu; hãy tải lại trang và kiểm tra.");
  var byId=new HashMap<Long,Seat>();
  var byPosition=new HashMap<String,Seat>();
  for(var s:existing){byId.put(s.getId(),s);byPosition.put(position(s.getRowLabel(),s.getSeatNumber()),s);}
  var positions=new HashSet<String>();
  var matchedIds=new HashSet<Long>();
  var changes=new LinkedHashMap<Seat,SeatLayoutSeatForm>();
  var additions=new ArrayList<Seat>();
  int updated=0,deactivated=0;
  for(var p:f.seats()){
   String row=p.rowLabel().strip().toUpperCase(Locale.ROOT);
   if(row.length()>10)throw new ScheduleValidationException("seats","Tên hàng sau chuẩn hóa tối đa 10 ký tự.");
   String key=position(row,p.seatNumber());
   if(!positions.add(key))throw new ScheduleValidationException("seats","Vị trí "+row+p.seatNumber()+" bị lặp trong sơ đồ.");
   Seat target=p.id()==null?byPosition.get(key):byId.get(p.id());
   if(p.id()!=null&&(target==null||!position(target.getRowLabel(),target.getSeatNumber()).equals(key)))
    throw new ScheduleValidationException("seats","ID ghế không thuộc đúng phòng/vị trí. Không được di chuyển ID trong trình thiết kế.");
   if(target==null){
    var s=new Seat();s.setRoom(room);s.setRowLabel(row);s.setSeatNumber(p.seatNumber());
    s.setSeatType(p.seatType());s.setActive(p.active());additions.add(s);
   }else{
    if(!matchedIds.add(target.getId()))throw new ScheduleValidationException("seats","ID ghế bị lặp.");
    changes.put(target,p);
    if(target.getSeatType()!=p.seatType()||target.isActive()!=p.active())updated++;
   }
  }
  for(var s:existing)if(!matchedIds.contains(s.getId())&&s.isActive())deactivated++;
  if((updated>0||deactivated>0)&&!f.confirmExistingChanges())
   throw new ScheduleValidationException("confirmExistingChanges","Hãy xác nhận thay đổi ghế đã lưu. Ghế bị bỏ chỉ ngừng hoạt động, không bị xóa.");
  // Apply only after the entire draft and the confirmation have been validated.
  for(var entry:changes.entrySet()){
   entry.getKey().setSeatType(entry.getValue().seatType());
   entry.getKey().setActive(entry.getValue().active());
  }
  for(var s:existing)if(!matchedIds.contains(s.getId()))s.setActive(false);
  seats.saveAllAndFlush(additions);
  return new SeatLayoutResult(additions.size(),updated,deactivated);
 }
 private static String position(String row,int number){return row.strip().toUpperCase(Locale.ROOT)+":"+number;}
 private static String snapshot(Long roomId,List<Seat> all){
  var text=new StringBuilder(roomId.toString()).append('\n');
  all.stream().sorted(Comparator.comparing(Seat::getId)).forEach(s->text.append(s.getId()).append('|')
   .append(s.getRowLabel().length()).append(':').append(s.getRowLabel()).append('|').append(s.getSeatNumber())
   .append('|').append(s.getSeatType()).append('|').append(s.isActive()).append('\n'));
  try{
   return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
    .digest(text.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
  }catch(java.security.NoSuchAlgorithmException ex){throw new IllegalStateException(ex);}
 }
 /** Pure read preview: combines current seats and proposed positions without allocating IDs. */
 public List<SeatSummary> previewBatch(@NotNull @Valid SeatBatchForm f){
  ScheduleAccess.admin();
  rooms.findById(f.getRoomId()).orElseThrow(()->new ScheduleValidationException("roomId","Không tìm thấy phòng."));
  var existing=seats.findByRoomIdOrderByRowLabelAscSeatNumberAsc(f.getRoomId());
  var proposed=plan(f,existing);
  var combined=new ArrayList<SeatSummary>();
  existing.stream().map(SeatSummary::from).forEach(combined::add);
  combined.addAll(proposed);
  combined.sort(Comparator.comparing(SeatSummary::rowLabel).thenComparing(SeatSummary::seatNumber));
  return List.copyOf(combined);
 }
 @Transactional
 public List<SeatSummary> createBatch(@NotNull @Valid SeatBatchForm f){
  ScheduleAccess.admin();
  var room=rooms.findLockedById(f.getRoomId()).orElseThrow(()->new ScheduleValidationException("roomId","Không tìm thấy phòng."));
  // Validate every segment before writing; the room lock also serializes single-seat writes.
  var proposed=plan(f,seats.findByRoomIdOrderByRowLabelAscSeatNumberAsc(room.getId()));
  var created=new ArrayList<Seat>();
  for(var p:proposed){
   Seat s=new Seat();s.setRoom(room);s.setRowLabel(p.rowLabel());s.setSeatNumber(p.seatNumber());
   s.setSeatType(p.seatType());s.setActive(p.active());created.add(s);
  }
  return seats.saveAllAndFlush(created).stream().map(SeatSummary::from).toList();
 }
 @Transactional
 public int updateBatch(@NotNull @Valid SeatBulkUpdateForm f){
  ScheduleAccess.admin();
  if(f.seatType()==null&&f.active()==null)
   throw new ScheduleValidationException("seatType","Chọn loại ghế hoặc trạng thái cần thay đổi.");
  var ids=new HashSet<>(f.seatIds());
  if(ids.size()!=f.seatIds().size())throw new ScheduleValidationException("seatIds","Danh sách ID ghế bị trùng.");
  rooms.findLockedById(f.roomId()).orElseThrow(()->new ScheduleValidationException("roomId","Không tìm thấy phòng."));
  var selected=seats.findAllById(ids);
  if(selected.size()!=ids.size())throw new ScheduleValidationException("seatIds","Có ghế không tồn tại; chưa cập nhật ghế nào.");
  for(var s:selected){
   if(!s.getRoom().getId().equals(f.roomId()))
    throw new ScheduleValidationException("seatIds","Có ghế thuộc phòng khác; chưa cập nhật ghế nào.");
  }
  for(var s:selected){
   if(f.seatType()!=null)s.setSeatType(f.seatType());
   if(f.active()!=null)s.setActive(f.active());
  }
  seats.flush();
  return selected.size();
 }
 private List<SeatSummary> plan(SeatBatchForm f,List<Seat> existing){
  var positions=new HashSet<String>();
  for(var s:existing)positions.add(s.getRowLabel().strip().toUpperCase(Locale.ROOT)+":"+s.getSeatNumber());
  var result=new ArrayList<SeatSummary>();
  for(var row:f.getRows()){
   String label=row.getRowLabel().strip().toUpperCase(Locale.ROOT);
   if(label.length()>10)throw new ScheduleValidationException("rows","Tên hàng sau chuẩn hóa tối đa 10 ký tự.");
   if(row.getFirstNumber()>row.getLastNumber())
    throw new ScheduleValidationException("rows","Hàng "+label+": số bắt đầu phải nhỏ hơn hoặc bằng số kết thúc.");
   for(int n=row.getFirstNumber();n<=row.getLastNumber();n++){
    if(!positions.add(label+":"+n))
     throw new ScheduleValidationException("rows","Vị trí "+label+n+" đã tồn tại hoặc bị lặp trong yêu cầu. Chưa tạo ghế nào.");
    if(result.size()>=1000)throw new ScheduleValidationException("rows","Mỗi lần tạo tối đa 1.000 ghế.");
    result.add(new SeatSummary(null,f.getRoomId(),label,n,row.getSeatType(),row.isActive()));
   }
  }
  return List.copyOf(result);
 }
 private Seat save(Long id,SeatForm f){
  Room r=rooms.findLockedById(f.roomId()).orElseThrow(()->new ScheduleValidationException("roomId","Không tìm thấy phòng."));
  Seat s=id==null?new Seat():seats.findById(id).orElseThrow(()->new ScheduleValidationException("seatId","Không tìm thấy ghế."));
  if(id!=null&&!s.getRoom().getId().equals(r.getId()))throw new ScheduleValidationException("roomId","Không chuyển ghế sang phòng khác.");
  String row=f.rowLabel().strip().toUpperCase(Locale.ROOT);
  if(seats.existsByRoomIdAndRowLabelIgnoreCaseAndSeatNumberAndIdNot(r.getId(),row,f.seatNumber(),id==null?-1L:id))
   throw new ScheduleValidationException("seatNumber","Vị trí ghế đã tồn tại.");
  s.setRoom(r);s.setRowLabel(row);s.setSeatNumber(f.seatNumber());s.setSeatType(f.seatType());s.setActive(f.active());
  return seats.saveAndFlush(s);
 }
}
