package com.example.movie.repository;

import com.example.movie.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Lấy toàn bộ vé của 1 khách hàng theo userId
    List<Booking> findByUserId(Long userId);
}