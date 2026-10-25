package com.example.movie.repository;

import com.example.movie.model.ShowtimeSeat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShowtimeSeatRepository extends JpaRepository<ShowtimeSeat, Long> {

    List<ShowtimeSeat> findByShowtimeId(Long showtimeId);

    // Khóa bi quan chặn race condition khi 2 người cùng bấm đặt 1 ghế
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ShowtimeSeat s WHERE s.showtime.id = :showtimeId AND s.seat.id IN :seatIds")
    List<ShowtimeSeat> findSeatsForBookingWithLock(@Param("showtimeId") Long showtimeId, @Param("seatIds") List<Long> seatIds);
}