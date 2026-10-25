package com.example.movie.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class ShowtimeSeat {

    // Khóa chính tự tăng
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Quan hệ N-1: Nhiều ghế này cùng thuộc về 1 suất chiếu cụ thể
    @ManyToOne
    private Showtime showtime;

    // Quan hệ N-1: Liên kết với chiếc ghế vật lý (số ghế, hàng ghế) trong phòng chiếu
    @ManyToOne
    private Seat seat;

    // Lưu trạng thái ghế dưới dạng chữ (Ví dụ: "AVAILABLE", "BOOKED") vào database thay vì số 0, 1
    @Enumerated(EnumType.STRING)
    private SeatStatus status;



    public ShowtimeSeat() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Showtime getShowtime() {
        return showtime;
    }

    public void setShowtime(Showtime showtime) {
        this.showtime = showtime;
    }

    public Seat getSeat() {
        return seat;
    }

    public void setSeat(Seat seat) {
        this.seat = seat;
    }

    public SeatStatus getStatus() {
        return status;
    }

    public void setStatus(SeatStatus status) {
        this.status = status;
    }
}