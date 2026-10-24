package com.example.movie.service;
import com.example.movie.dto.RoomForm;
import com.example.movie.model.Room;
import com.example.movie.repository.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service @Validated @Transactional(readOnly=true)
public class RoomService {
 private final RoomRepository rooms; private final TheaterService theaters; private final SeatRepository seats;
 public RoomService(RoomRepository rooms,TheaterService theaters,SeatRepository seats){this.rooms=rooms;this.theaters=theaters;this.seats=seats;}
 public Room getById(Long id){return rooms.findById(id).orElseThrow(()->new ScheduleValidationException("roomId","Không tìm thấy phòng."));}
 public List<com.example.movie.dto.RoomSummary> findForAdmin(Long theaterId){
  ScheduleAccess.admin();
  return rooms.findAll(org.springframework.data.domain.Sort.by("name","id")).stream()
   .filter(r->theaterId==null||r.getTheater().getId().equals(theaterId))
   .map(com.example.movie.dto.RoomSummary::from).toList();
 }
 public com.example.movie.dto.RoomSummary getSummary(Long id){ScheduleAccess.admin();return com.example.movie.dto.RoomSummary.from(getById(id));}
 @Transactional public void setActive(Long id,boolean active){
  ScheduleAccess.admin();
  rooms.findLockedById(id).orElseThrow(()->new ScheduleValidationException("roomId","Không tìm thấy phòng.")).setActive(active);
 }
 public List<Room> findActive(Long theaterId){return rooms.findByTheaterIdAndActiveTrueAndTheaterActiveTrueOrderByNameAsc(theaterId);}
 public long capacity(Long roomId){getById(roomId);return seats.countByRoomIdAndActiveTrue(roomId);}
 @Transactional public Room create(@NotNull @Valid RoomForm f){ScheduleAccess.admin();return save(new Room(),f);}
 @Transactional public Room update(Long id,@NotNull @Valid RoomForm f){
  ScheduleAccess.admin(); Room r=rooms.findLockedById(id).orElseThrow(()->new ScheduleValidationException("roomId","Không tìm thấy phòng."));
  if(!r.getTheater().getId().equals(f.theaterId()))throw new ScheduleValidationException("theaterId","Không chuyển phòng sang rạp khác.");
  return save(r,f);
 }
 private Room save(Room r,RoomForm f){
  var t=theaters.getById(f.theaterId()); String name=f.name().strip().replaceAll("\\s+"," ");
  if(rooms.existsByTheaterIdAndNameIgnoreCaseAndIdNot(t.getId(),name,r.getId()==null?-1L:r.getId()))
   throw new ScheduleValidationException("name","Tên phòng đã tồn tại trong rạp.");
  r.setTheater(t);r.setName(name);r.setRoomType(f.roomType());r.setActive(f.active());return rooms.saveAndFlush(r);
 }
}
