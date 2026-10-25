package com.example.movie.repository;
import com.example.movie.model.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SeatRepository extends JpaRepository<Seat,Long> {
 boolean existsByRoomId(Long roomId);
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @org.springframework.data.jpa.repository.Query("select s from Seat s where s.room.id=:roomId order by s.id")
 List<Seat> findLockedByRoomId(@org.springframework.data.repository.query.Param("roomId") Long roomId);
 List<Seat> findByRoomIdOrderByRowLabelAscSeatNumberAsc(Long roomId);
 List<Seat> findByRoomIdAndActiveTrueOrderByRowLabelAscSeatNumberAsc(Long roomId);
 boolean existsByRoomIdAndRowLabelIgnoreCaseAndSeatNumberAndIdNot(Long roomId,String rowLabel,Integer seatNumber,Long excludedId);
 long countByRoomIdAndActiveTrue(Long roomId);
}
