package com.example.movie.repository;

import com.example.movie.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import com.example.movie.model.BookingStatus;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByIdAndUserId(Long id, Long userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :id and b.user.id = :userId")
    Optional<Booking> findForUpdate(@Param("id") Long id, @Param("userId") Long userId);
    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Booking> findByStatusAndHoldExpiresAtBefore(BookingStatus status, LocalDateTime time);
}
