package com.example.movie.repository;

import com.example.movie.model.ShowtimeSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShowtimeSeatRepository extends JpaRepository<ShowtimeSeat, Long> {

    // 1. Lấy toàn bộ danh sách ghế của 1 suất chiếu để hiển thị lên sơ đồ
    List<ShowtimeSeat> findByShowtimeId(Long showtimeId);

    // 2. Tìm đúng 1 ghế trong 1 suất chiếu cụ thể
    Optional<ShowtimeSeat> findByShowtimeIdAndSeatId(Long showtimeId, Long seatId);
}