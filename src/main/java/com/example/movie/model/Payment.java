package com.example.movie.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One payment attempt. A booking may have multiple attempts. */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(nullable = false, length = 30)
    private String method;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "transaction_ref", length = 100)
    private String transactionRef;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime paidAt;

    public Payment() {
    }

    public Payment(Booking booking, String method, BigDecimal amount, PaymentStatus status,
                   String transactionRef, LocalDateTime createdAt) {
        this.booking = booking;
        this.method = method;
        this.amount = amount;
        this.status = status;
        this.transactionRef = transactionRef;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Booking getBooking() { return booking; }
    public String getMethod() { return method; }
    public BigDecimal getAmount() { return amount; }
    public PaymentStatus getStatus() { return status; }
    public String getTransactionRef() { return transactionRef; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
}
