package com.campusshare.backend.controller;

import com.campusshare.backend.model.Item;
import com.campusshare.backend.model.User;
import com.campusshare.backend.repository.ItemRepository;
import com.campusshare.backend.repository.UserRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = "*")
public class PaymentController {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserRepository userRepository;

    // 🔑 YOUR KEYS (Hardcoded for safety right now)
    private final String KEY_ID = "rzp_test_SA4W8wVPxIcCrC"; 
    private final String KEY_SECRET = "lRhY2vzdZ4XESJinjPFjfcjc";

    // 1. CREATE ORDER
    @PostMapping("/create-order/{itemId}")
    public ResponseEntity<String> createOrder(@PathVariable UUID itemId) {
        try {
            Item item = itemRepository.findById(itemId).orElse(null);
            if (item == null) return ResponseEntity.badRequest().body("Item not found");

            // 🛡️ FIX: Handle Null Date (Crash Prevention)
            LocalDate startDate = item.getLastPaymentDate();
            if (startDate == null) {
                // If null (old data), assume the start date is TODAY to prevent crash
                startDate = LocalDate.now();
                // Optionally verify with borrowedAt if you have it, but this is safest
            }

            // Calculate Days
            long daysUnpaid = ChronoUnit.DAYS.between(startDate, LocalDate.now());
            if (daysUnpaid <= 0) daysUnpaid = 1; // Minimum 1 day charge

            double amount = daysUnpaid * item.getPricePerDay();
            int amountInPaise = (int) (amount * 100);

            RazorpayClient client = new RazorpayClient(KEY_ID, KEY_SECRET);
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "txn_" + System.currentTimeMillis());

            Order order = client.orders.create(orderRequest);

            JSONObject response = new JSONObject();
            response.put("orderId", order.get("id").toString());
            response.put("amount", amountInPaise);
            response.put("keyId", KEY_ID);
            
            return ResponseEntity.ok(response.toString());

        } catch (Exception e) {
            e.printStackTrace(); // Print error to console so you can see it!
            return ResponseEntity.status(500).body("Error creating order: " + e.getMessage());
        }
    }

    // 2. VERIFY PAYMENT
    @PostMapping("/verify-payment/{itemId}")
    public ResponseEntity<String> verifyPayment(@PathVariable UUID itemId) {
        Item item = itemRepository.findById(itemId).orElse(null);
        if (item == null) return ResponseEntity.badRequest().body("Error");

        // 🛡️ FIX: Handle Null Date Here Too
        LocalDate startDate = item.getLastPaymentDate();
        if (startDate == null) startDate = LocalDate.now();

        long daysUnpaid = ChronoUnit.DAYS.between(startDate, LocalDate.now());
        if (daysUnpaid <= 0) daysUnpaid = 1; 
        
        double amountPaid = daysUnpaid * item.getPricePerDay();

        // Credit Lender
        User lender = userRepository.findByEmail(item.getLenderEmail()).orElse(null);
        if (lender != null) {
            double currentBalance = (lender.getEarnings() == null) ? 0.0 : lender.getEarnings();
            double platformFee = amountPaid * 0.10; 
            double lenderShare = amountPaid - platformFee; 
            
            lender.setEarnings(currentBalance + lenderShare);
            userRepository.save(lender);
        }

        // Clear Debt
        item.setLastPaymentDate(LocalDate.now());
        itemRepository.save(item);

        return ResponseEntity.ok("Payment Verified.");
    }
}