package com.campusshare.backend.controller;

import com.campusshare.backend.model.User;
import com.campusshare.backend.model.Item;
import com.campusshare.backend.repository.ItemRepository;
import com.campusshare.backend.repository.UserRepository;
import com.campusshare.backend.service.EmailService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;
import java.util.Random;
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private EmailService emailService;

    @GetMapping("/earnings/{email}")
    public ResponseEntity<Double> getEarnings(@PathVariable String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body(0.0);
        
        return ResponseEntity.ok(user.getEarnings() == null ? 0.0 : user.getEarnings());
    }
    // SAVE PAYOUT METHOD
    @PostMapping("/payout-method/{email}")
    public ResponseEntity<String> updatePayoutMethod(@PathVariable String email, @RequestBody String upiId) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body("User not found");

        // Clean up the input (remove quotes)
        String cleanUpi = upiId.replace("\"", "").trim();
        
        user.setUpiId(cleanUpi);
        
        // In the future: We will call Razorpay API here to create a "Linked Account"
        // user.setRazorpayAccountId( createRazorpayLinkedAccount(cleanUpi) ); 
        
        userRepository.save(user);

        return ResponseEntity.ok("UPI ID Saved! Future payments will be routed here.");
    }
    
    // GET PAYOUT METHOD (To show in dashboard)
    @GetMapping("/payout-method/{email}")
    public ResponseEntity<String> getPayoutMethod(@PathVariable String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body("");
        
        return ResponseEntity.ok(user.getUpiId() == null ? "" : user.getUpiId());
    }

    // 1. REQUEST DELETE OTP
    @PostMapping("/request-delete-otp/{email}")
    public ResponseEntity<String> requestDeleteOtp(@PathVariable String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body("User not found");

        // Generate OTP
        String otp = String.format("%06d", new Random().nextInt(999999));
        user.setOtp(otp); // reusing the OTP field is fine
        user.setOtpGeneratedTime(LocalDateTime.now());
        userRepository.save(user);

        // Send SCARY Email
        emailService.sendDeleteWarning(email, otp);

        return ResponseEntity.ok("OTP Sent");
    }

    // 2. CONFIRM DELETION (Requires OTP)
    @PostMapping("/confirm-delete")
    @Transactional // Important: Ensures all deletes happen together or not at all
    public ResponseEntity<String> confirmDelete(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String otp = payload.get("otp");

        // 1. Basic User Checks
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body("User not found");

        if (user.getOtp() == null || !user.getOtp().equals(otp)) {
            return ResponseEntity.status(401).body("Invalid OTP.");
        }
        if (user.getOtpGeneratedTime().plusMinutes(5).isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(401).body("OTP Expired.");
        }

        // 🛡️ BLOCKER 1: Are you a BORROWER holding active items?
        List<Item> borrowedItems = itemRepository.findByBorrowerEmail(email);
        for (Item item : borrowedItems) {
            // If you have an item RENTED or waiting for pickup (PENDING) -> STOP.
            if ("RENTED".equals(item.getStatus()) || "PENDING".equals(item.getStatus())) {
                return ResponseEntity.badRequest().body("❌ Cannot delete: You currently have borrowed items. Please return them first.");
            }
        }

        // 🛡️ BLOCKER 2: Are you a LENDER with items currently out?
        List<Item> lentItems = itemRepository.findByLenderEmail(email);
        for (Item item : lentItems) {
            // If someone is using your item (RENTED) or coming to pick it up (PENDING) -> STOP.
            if ("RENTED".equals(item.getStatus()) || "PENDING".equals(item.getStatus())) {
                return ResponseEntity.badRequest().body("❌ Cannot delete: Your items are currently in use by others. Wait for them to be returned.");
            }
        }

        // 🧹 CLEANUP 1: Remove "Requests" this user made (Anti-Ghosting)
        // If this user simply 'REQUESTED' items but deleted account, cancel those requests so Lenders aren't stuck.
        for (Item item : borrowedItems) {
            if ("REQUESTED".equals(item.getStatus())) {
                item.setStatus("AVAILABLE");
                item.setBorrowerEmail(null);
                item.setBorrowerName(null);
                item.setBorrowerPhone(null);
                itemRepository.save(item);
            }
        }

        // 🧹 CLEANUP 2: Delete Lender's Inventory
        // Since we passed Blocker 2, we know all their items are AVAILABLE or REJECTED (safe to delete).
        itemRepository.deleteByLenderEmail(email);

        // 🔥 FINAL EXECUTION: Delete User
        userRepository.delete(user);

        return ResponseEntity.ok("Account Deleted Successfully. Goodbye!");
    }
    // ⭐ RATE A USER
    @PostMapping("/rate")
    public ResponseEntity<String> rateUser(@RequestBody Map<String, Object> payload) {
        String email = (String) payload.get("email");
        int stars = (int) payload.get("stars");

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body("User not found");

        // Calculate New Average
        double currentTotal = user.getRating() * user.getRatingCount();
        double newTotal = currentTotal + stars;
        int newCount = user.getRatingCount() + 1;
        
        user.setRating(newTotal / newCount);
        user.setRatingCount(newCount);
        
        userRepository.save(user);
        return ResponseEntity.ok("Rating submitted!");
    }
    
    // GET RATING
    @GetMapping("/rating/{email}")
    public ResponseEntity<Double> getRating(@PathVariable String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if(user == null) return ResponseEntity.ok(0.0);
        return ResponseEntity.ok(user.getRating());
    }
}