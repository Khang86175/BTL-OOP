package com.example.movie.repository;

import com.example.movie.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    @Query("select t from Ticket t join fetch t.movie join fetch t.seat where t.booking.id = :bookingId order by t.id asc")
    List<Ticket> findByBookingIdOrderByIdAsc(@Param("bookingId") Long bookingId);
    boolean existsByBookingId(Long bookingId);
}
