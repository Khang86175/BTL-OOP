package com.example.movie.model;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
/** Physical/catalog data only; no cascade deletion and no booking state. */
@Entity
@org.hibernate.annotations.Check(name="chk_showtimes_values",constraints="base_price >= 0 and end_time > start_time")
@Table(name="showtimes",indexes={@Index(name="idx_showtimes_room_time",columnList="room_id,start_time,end_time"),@Index(name="idx_showtimes_movie_time",columnList="movie_id,start_time")})
public class Showtime {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 private Long id;
 @NotNull @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="movie_id",nullable=false,foreignKey=@ForeignKey(name="fk_showtimes_movie"))
 private Movie movie;
 @NotNull @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="room_id",nullable=false,foreignKey=@ForeignKey(name="fk_showtimes_room"))
 private Room room;
 @NotNull @Column(nullable=false)
 private LocalDateTime startTime;
 @NotNull @Column(nullable=false)
 private LocalDateTime endTime;
 @NotNull @DecimalMin("0") @Digits(integer=10,fraction=2) @Column(nullable=false,precision=12,scale=2)
 private BigDecimal basePrice;
 @NotNull @Enumerated(EnumType.STRING) @Column(nullable=false,length=20,columnDefinition="varchar(20)")
 private ShowtimeStatus status;
 /** Evaluates physical/catalog availability, not booking or held-seat inventory.
  * now uses the cinema's Asia/Ho_Chi_Minh local time.
  */
 public boolean isBookableAt(LocalDateTime now) {
  java.util.Objects.requireNonNull(now, "now");
  return getStatus() == ShowtimeStatus.OPEN
   && getStartTime() != null && getStartTime().isAfter(now)
   && getMovie() != null && getMovie().isVisible()
   && getRoom() != null && getRoom().isOperational();
 }
 public Long getId(){return id;}
 public Movie getMovie(){return movie;}
 public void setMovie(Movie value){this.movie=value;}
 public Room getRoom(){return room;}
 public void setRoom(Room value){this.room=value;}
 public LocalDateTime getStartTime(){return startTime;}
 public void setStartTime(LocalDateTime value){this.startTime=value;}
 public LocalDateTime getEndTime(){return endTime;}
 public void setEndTime(LocalDateTime value){this.endTime=value;}
 public BigDecimal getBasePrice(){return basePrice;}
 public void setBasePrice(BigDecimal value){this.basePrice=value;}
 public ShowtimeStatus getStatus(){return status;}
 public void setStatus(ShowtimeStatus value){this.status=value;}
}
