package com.example.movie.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tickets", uniqueConstraints = @UniqueConstraint(name = "uk_tickets_code", columnNames = "code"))
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String code = "TKT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "showtime_id", nullable = false)
    private Showtime showtime;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @Column(name = "showtime_start", nullable = false)
    private LocalDateTime showtimeStart;

    @Column(nullable = false)
    private LocalDateTime issuedAt = LocalDateTime.now(Movie.CINEMA_ZONE);

    public Ticket() { }
    public Ticket(Booking booking, Showtime showtime, Seat seat) {
        this.booking = booking;
        this.showtime = showtime;
        this.seat = seat;
        this.movie = showtime.getMovie();
        this.showtimeStart = showtime.getStartTime();
    }
    public Long getId() { return id; }
    public String getCode() { return code; }
    public Booking getBooking() { return booking; }
    public Showtime getShowtime() { return showtime; }
    public Seat getSeat() { return seat; }
    public Movie getMovie() { return movie; }
    public LocalDateTime getShowtimeStart() { return showtimeStart; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
}
