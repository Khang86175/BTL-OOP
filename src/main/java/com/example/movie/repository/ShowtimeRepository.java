package com.example.movie.repository;
import com.example.movie.model.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.*;
public interface ShowtimeRepository extends JpaRepository<Showtime,Long> {
 @Query("select s.room.id from Showtime s where s.id=:id")
 Optional<Long> findRoomIdById(@Param("id") Long id);
 // Derive the database filter from the same polymorphic rule used by the service.
 default long countOverlaps(Long roomId, Long excludedId, LocalDateTime start, LocalDateTime end) {
  List<ShowtimeStatus> statuses = Arrays.stream(ShowtimeStatus.values())
   .filter(ShowtimeStatus::occupiesRoom).toList();
  return statuses.isEmpty() ? 0 : countOverlapsWithStatuses(roomId, excludedId, start, end, statuses);
 }
 @Query("""
 select count(s) from Showtime s where s.room.id=:roomId and s.id<>:excludedId
 and s.status in :statuses
 and s.startTime<:end and s.endTime>:start
 """)
 long countOverlapsWithStatuses(@Param("roomId") Long roomId,@Param("excludedId") Long excludedId,
 @Param("start") LocalDateTime start,@Param("end") LocalDateTime end,
 @Param("statuses") Collection<ShowtimeStatus> statuses);
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select s from Showtime s where s.id=:id")
 Optional<Showtime> findLockedById(@Param("id") Long id);
 @EntityGraph(attributePaths={"movie","room","room.theater"})
 @Query("""
 select s from Showtime s where (:movieId is null or s.movie.id=:movieId)
 and (:theaterId is null or s.room.theater.id=:theaterId)
 and s.startTime>=:from and s.startTime<:until order by s.startTime,s.id
 """)
 List<Showtime> findSchedule(@Param("movieId") Long movieId,@Param("theaterId") Long theaterId,
 @Param("from") LocalDateTime from,@Param("until") LocalDateTime until);
}
