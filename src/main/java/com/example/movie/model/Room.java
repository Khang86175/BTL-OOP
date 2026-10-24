package com.example.movie.model;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
/** Physical/catalog data only; no cascade deletion and no booking state. */
@Entity
@Table(name="rooms",uniqueConstraints=@UniqueConstraint(name="uk_rooms_theater_name",columnNames={"theater_id","name"}))
public class Room {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 private Long id;
 @NotNull @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="theater_id",nullable=false,foreignKey=@ForeignKey(name="fk_rooms_theater"))
 private Theater theater;
 @NotBlank @Size(max=80) @Column(nullable=false,length=80)
 private String name;
 @NotNull @Enumerated(EnumType.STRING) @Column(nullable=false,length=20,columnDefinition="varchar(20)")
 private RoomType roomType;
 @Column(nullable=false)
 private boolean active = true;
 /** A room can operate only while its theater is also active. */
 public boolean isOperational() {
  return isActive() && getTheater() != null && getTheater().isActive();
 }
 public Long getId(){return id;}
 public Theater getTheater(){return theater;}
 public void setTheater(Theater value){this.theater=value;}
 public String getName(){return name;}
 public void setName(String value){this.name=value;}
 public RoomType getRoomType(){return roomType;}
 public void setRoomType(RoomType value){this.roomType=value;}
 public boolean isActive(){return active;}
 public void setActive(boolean value){this.active=value;}
}
