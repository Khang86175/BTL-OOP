package com.example.movie.model;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
/** Physical/catalog data only; no cascade deletion and no booking state. */
@Entity
@Table(name="theaters")
public class Theater {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 private Long id;
 @NotBlank @Size(max=150) @Column(nullable=false,length=150)
 private String name;
 @NotBlank @Size(max=500) @Column(nullable=false,length=500)
 private String address;
 @Column(nullable=false)
 private boolean active = true;
 public Long getId(){return id;}
 public String getName(){return name;}
 public void setName(String value){this.name=value;}
 public String getAddress(){return address;}
 public void setAddress(String value){this.address=value;}
 public boolean isActive(){return active;}
 public void setActive(boolean value){this.active=value;}
}
