package com.example.movie.model;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
/** Physical/catalog data only; no cascade deletion and no booking state. */
@Entity
@org.hibernate.annotations.Check(name="chk_showtimes_values",constraints="base_price >= 0 and end_time > start_time")
@org.hibernate.annotations.Check(name="chk_showtimes_prices",constraints="(normal_price is null or normal_price > 0) and (vip_price is null or vip_price > 0)")
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
 // Nullable during migration only; every new/admin-edited schedule requires both prices.
 @DecimalMin(value="0",inclusive=false) @Digits(integer=10,fraction=2) @Column(name="normal_price",precision=12,scale=2)
 private BigDecimal normalPrice;
 @DecimalMin(value="0",inclusive=false) @Digits(integer=10,fraction=2) @Column(name="vip_price",precision=12,scale=2)
 private BigDecimal vipPrice;
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
   && getRoom() != null && getRoom().isOperational() && hasConfiguredPrices();
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
 /** Compatibility only. Booking must switch to priceFor/normalPrice/vipPrice. */
 @Deprecated public BigDecimal getBasePrice(){return basePrice;}
 @Deprecated public void setBasePrice(BigDecimal value){this.basePrice=value;}
 public BigDecimal getNormalPrice(){return normalPrice;}
 public void setNormalPrice(BigDecimal value){normalPrice=value;}
 public BigDecimal getVipPrice(){return vipPrice;}
 public void setVipPrice(BigDecimal value){vipPrice=value;}
 public boolean hasConfiguredPrices(){return normalPrice!=null&&normalPrice.signum()>0&&vipPrice!=null&&vipPrice.signum()>0;}
 /** Per-showtime final seat price, with no global surcharge or rounding rule. */
 public BigDecimal priceFor(SeatType type){
  if(type==null||!type.isSupported())throw new IllegalArgumentException("Chỉ hỗ trợ giá ghế Thường và VIP.");
  if(!hasConfiguredPrices())throw new IllegalStateException("Suất chiếu chưa được cấu hình đủ hai mức giá.");
  return type==SeatType.NORMAL?normalPrice:vipPrice;
 }
 public ShowtimeStatus getStatus(){return status;}
 public void setStatus(ShowtimeStatus value){this.status=value;}
}
