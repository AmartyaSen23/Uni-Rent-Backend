package com.campusshare.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
@Entity
@Table(name = "items") // This will create a new table called 'items' in Supabase
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String name; // e.g., "Scientific Calculator"

    @Column(nullable = false)
    private Double pricePerDay; // e.g., 20.0

    @Column(nullable = false)
    private String lenderEmail; // Who owns this item?

    private String description; // Optional details

    private boolean isAvailable = true; // Is it currently borrowed?

    private LocalDateTime postedAt;

    // --- CONSTRUCTORS ---
    public Item() {}

    public Item(String name, Double pricePerDay, String lenderEmail, String description) {
        this.name = name;
        this.pricePerDay = pricePerDay;
        this.lenderEmail = lenderEmail;
        this.description = description;
        this.postedAt = LocalDateTime.now();
        this.isAvailable = true;
    }

    // --- GETTERS AND SETTERS ---
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getPricePerDay() { return pricePerDay; }
    public void setPricePerDay(Double pricePerDay) { this.pricePerDay = pricePerDay; }

    public String getLenderEmail() { return lenderEmail; }
    public void setLenderEmail(String lenderEmail) { this.lenderEmail = lenderEmail; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { isAvailable = available; }
    // --- NEW FIELDS FOR BORROWING ---
    @Column(name = "borrower_email")
    private String borrowerEmail; // Who rented it?

    @Column(name = "borrowed_at")
    private LocalDateTime borrowedAt; // When did the meter start?

    // --- NEW GETTERS AND SETTERS ---
    public String getBorrowerEmail() { return borrowerEmail; }
    public void setBorrowerEmail(String borrowerEmail) { this.borrowerEmail = borrowerEmail; }

    public LocalDateTime getBorrowedAt() { return borrowedAt; }
    public void setBorrowedAt(LocalDateTime borrowedAt) { this.borrowedAt = borrowedAt; }
    // --- NEW FIELDS FOR SECURITY ---
    private String status = "AVAILABLE"; // Values: AVAILABLE, PENDING, RENTED
    
    private String passkey; // The secret code (visible only to Lender)

    // --- GETTERS AND SETTERS ---
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPasskey() { return passkey; }
    public void setPasskey(String passkey) { this.passkey = passkey; }
    // --- NEW FIELDS FOR BILLING & RETURN ---
    @Column(name = "last_payment_date")
    private java.time.LocalDate lastPaymentDate; // Which day did they pay up to?

    @Column(name = "return_passkey")
    private String returnPasskey; // The 2nd OTP needed to stop the meter

    // --- GETTERS AND SETTERS ---
    public java.time.LocalDate getLastPaymentDate() { return lastPaymentDate; }
    public void setLastPaymentDate(java.time.LocalDate lastPaymentDate) { this.lastPaymentDate = lastPaymentDate; }

    public String getReturnPasskey() { return returnPasskey; }
    public void setReturnPasskey(String returnPasskey) { this.returnPasskey = returnPasskey; }
    @jakarta.persistence.Column(columnDefinition = "LONGTEXT") // Allows large image strings
    private String imageBase64;

    // Getters and Setters
    public String getImageBase64() { return imageBase64; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }
    // ⭐ ITEM RATING (Quality of the product)
    private double itemRating = 5.0;
    private int itemRatingCount = 0;

    // Getters Setters
    public double getItemRating() { return itemRating; }
    public void setItemRating(double itemRating) { this.itemRating = itemRating; }
    public int getItemRatingCount() { return itemRatingCount; }
    public void setItemRatingCount(int itemRatingCount) { this.itemRatingCount = itemRatingCount; }
    // ... inside Item.java ...

    // 🛡️ SPAM PROTECTION FLAGS
    @Column(name = "item_rated")
    private boolean itemRated = false;

    @Column(name = "lender_rated")
    private boolean lenderRated = false;

    // Getters & Setters
    public boolean isItemRated() { return itemRated; }
    public void setItemRated(boolean itemRated) { this.itemRated = itemRated; }

    public boolean isLenderRated() { return lenderRated; }
    public void setLenderRated(boolean lenderRated) { this.lenderRated = lenderRated; }
    @Column(name = "lender_name")
    private String lenderName;

    @Column(name = "borrower_name")
    private String borrowerName;

    // Getters and Setters
    public String getLenderName() { return lenderName; }
    public void setLenderName(String lenderName) { this.lenderName = lenderName; }

    public String getBorrowerName() { return borrowerName; }
    public void setBorrowerName(String borrowerName) { this.borrowerName = borrowerName; }
    @Column(name = "lender_phone")
    private String lenderPhone;

    @Column(name = "borrower_phone")
    private String borrowerPhone;

    public String getLenderPhone() { return lenderPhone; }
    public void setLenderPhone(String lenderPhone) { this.lenderPhone = lenderPhone; }

    public String getBorrowerPhone() { return borrowerPhone; }
    public void setBorrowerPhone(String borrowerPhone) { this.borrowerPhone = borrowerPhone; }
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private java.time.LocalDateTime createdAt;

    // Getter & Setter
    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }
}