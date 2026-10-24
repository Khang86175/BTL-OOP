package com.example.movie.repository;
import com.example.movie.model.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SeatRepository extends JpaRepository<Seat,Long> {
 List<Seat> findByRoomIdOrderByRowLabelAscSeatNumberAsc(Long roomId);
 List<Seat> findByRoomIdAndActiveTrueOrderByRowLabelAscSeatNumberAsc(Long roomId);
 boolean existsByRoomIdAndRowLabelIgnoreCaseAndSeatNumberAndIdNot(Long roomId,String rowLabel,Integer seatNumber,Long excludedId);
 long countByRoomIdAndActiveTrue(Long roomId);
}
