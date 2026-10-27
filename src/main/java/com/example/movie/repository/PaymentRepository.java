package com.example.movie.repository;

import com.example.movie.model.Payment;
import com.example.movie.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import com.example.movie.model.BookingStatus;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByBookingIdOrderByCreatedAtDesc(Long bookingId);
    Optional<Payment> findFirstByBookingIdAndStatusOrderByCreatedAtDesc(Long bookingId, PaymentStatus status);
    List<Payment> findByBookingUserIdOrderByCreatedAtDesc(Long userId);

    @org.springframework.data.jpa.repository.Query("select coalesce(sum(p.amount), 0) from Payment p join p.booking b where p.status = :status and b.status = :bookingStatus and p.paidAt >= :from and p.paidAt < :to")
    BigDecimal sumSuccessfulBetween(@org.springframework.data.repository.query.Param("status") PaymentStatus status,
                                    @org.springframework.data.repository.query.Param("bookingStatus") BookingStatus bookingStatus,
                                    @org.springframework.data.repository.query.Param("from") LocalDateTime from,
                                    @org.springframework.data.repository.query.Param("to") LocalDateTime to);

    @org.springframework.data.jpa.repository.Query("select s.movie.title, sum(p.amount) from Payment p join p.booking b join b.showtime s where p.status = :status and b.status = :bookingStatus and p.paidAt >= :from and p.paidAt < :to group by s.movie.id, s.movie.title order by sum(p.amount) desc")
    List<Object[]> revenueByMovie(@org.springframework.data.repository.query.Param("status") PaymentStatus status,
                                  @org.springframework.data.repository.query.Param("bookingStatus") BookingStatus bookingStatus,
                                  @org.springframework.data.repository.query.Param("from") LocalDateTime from,
                                  @org.springframework.data.repository.query.Param("to") LocalDateTime to);

    @org.springframework.data.jpa.repository.Query("select s.room.theater.name, sum(p.amount) from Payment p join p.booking b join b.showtime s where p.status = :status and b.status = :bookingStatus and p.paidAt >= :from and p.paidAt < :to group by s.room.theater.id, s.room.theater.name order by sum(p.amount) desc")
    List<Object[]> revenueByTheater(@org.springframework.data.repository.query.Param("status") PaymentStatus status,
                                    @org.springframework.data.repository.query.Param("bookingStatus") BookingStatus bookingStatus,
                                    @org.springframework.data.repository.query.Param("from") LocalDateTime from,
                                    @org.springframework.data.repository.query.Param("to") LocalDateTime to);
}
