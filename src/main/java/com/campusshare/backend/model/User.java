package com.campusshare.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users") // This tells Spring to look at the 'users' table in Supabase
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password; // We will store the HASHED password here, not plain text

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // --- CONSTRUCTORS ---
    public User() {
    }

    public User(String email, String password) {
        this.email = email;
        this.password = password;
        this.createdAt = LocalDateTime.now();
    }

    // --- GETTERS AND SETTERS (Needed for Spring to read/write data) ---
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    // --- NEW FIELDS FOR PAYOUTS ---
    @Column(name = "upi_id")
    private String upiId; // e.g., "amartya@oksbi"

    @Column(name = "razorpay_account_id")
    private String razorpayAccountId; // We will need this later for Route

    // --- GETTERS AND SETTERS ---
    public String getUpiId() { return upiId; }
    public void setUpiId(String upiId) { this.upiId = upiId; }

    public String getRazorpayAccountId() { return razorpayAccountId; }
    public void setRazorpayAccountId(String razorpayAccountId) { this.razorpayAccountId = razorpayAccountId; }
    // --- MISSING FIELD! ADD THIS ---
    @Column(columnDefinition = "DOUBLE PRECISION DEFAULT 0.0")
    private Double earnings = 0.0; // Total money earned

    public Double getEarnings() { return earnings; }
    public void setEarnings(Double earnings) { this.earnings = earnings; }
    // --- OTP FIELDS ---
    @Column(name = "otp")
    private String otp;

    @Column(name = "otp_generated_time")
    private LocalDateTime otpGeneratedTime;

    // Getters and Setters
    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }

    public LocalDateTime getOtpGeneratedTime() { return otpGeneratedTime; }
    public void setOtpGeneratedTime(LocalDateTime otpGeneratedTime) { this.otpGeneratedTime = otpGeneratedTime; }
    private double rating = 5.0; // Default start at 5 stars
    private int ratingCount = 0;

    // Getters and Setters
    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }
    
    public int getRatingCount() { return ratingCount; }
    public void setRatingCount(int ratingCount) { this.ratingCount = ratingCount; }
    @Column(name = "username")
    private String username;

    // Getter and Setter
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    @Column(name = "phone_number")
    private String phoneNumber;

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
}