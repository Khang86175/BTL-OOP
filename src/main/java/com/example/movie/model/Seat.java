package com.example.movie.model;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
/** Physical/catalog data only; no cascade deletion and no booking state. */
@Entity
@org.hibernate.annotations.Check(name="chk_seats_number",constraints="seat_number > 0")
@Table(name="seats",uniqueConstraints=@UniqueConstraint(name="uk_seats_position",columnNames={"room_id","row_label","seat_number"}))
public class Seat {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 private Long id;
 @NotNull @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="room_id",nullable=false,foreignKey=@ForeignKey(name="fk_seats_room"))
 private Room room;
 @NotBlank @Size(max=10) @Column(nullable=false,length=10)
 private String rowLabel;
 @NotNull @Min(1) @Column(nullable=false)
 private Integer seatNumber;
 @NotNull @Enumerated(EnumType.STRING) @Column(nullable=false,length=20,columnDefinition="varchar(20)")
 private SeatType seatType;
 @Column(nullable=false)
 private boolean active = true;
 public Long getId(){return id;}
 public Room getRoom(){return room;}
 public void setRoom(Room value){this.room=value;}
 public String getRowLabel(){return rowLabel;}
 public void setRowLabel(String value){this.rowLabel=value;}
 public Integer getSeatNumber(){return seatNumber;}
 public void setSeatNumber(Integer value){this.seatNumber=value;}
 public SeatType getSeatType(){return seatType;}
 public void setSeatType(SeatType value){this.seatType=value;}
 public boolean isActive(){return active;}
 public void setActive(boolean value){this.active=value;}
}
