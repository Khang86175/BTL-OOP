package com.example.movie.repository;

import com.example.movie.model.ShowtimeSeat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShowtimeSeatRepository extends JpaRepository<ShowtimeSeat, Long> {


    @Query("SELECT ss FROM ShowtimeSeat ss JOIN FETCH ss.seat WHERE ss.showtime.id = :showtimeId")
    List<ShowtimeSeat> findByShowtimeId(@Param("showtimeId") Long showtimeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<ShowtimeSeat> findByShowtimeIdAndSeatIdIn(Long showtimeId, List<Long> seatIds);
}