package com.example.movie.repository;

import com.example.movie.model.BookingSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {

    // Tìm tất cả các ghế chi tiết nằm trong 1 đơn booking
    List<BookingSeat> findByBookingId(Long bookingId);
}