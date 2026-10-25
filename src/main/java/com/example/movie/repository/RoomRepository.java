package com.example.movie.repository;
import com.example.movie.model.Room;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface RoomRepository extends JpaRepository<Room,Long> {
 List<Room> findByTheaterIdAndActiveTrueAndTheaterActiveTrueOrderByNameAsc(Long theaterId);
 boolean existsByTheaterIdAndNameIgnoreCaseAndIdNot(Long theaterId,String name,Long excludedId);
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select r from Room r where r.id=:id")
 Optional<Room> findLockedById(@Param("id") Long id);
}
