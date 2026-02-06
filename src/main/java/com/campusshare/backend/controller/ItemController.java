package com.campusshare.backend.controller;

import com.campusshare.backend.model.Item;
import com.campusshare.backend.repository.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/items") // All item URLs start with /api/items
@CrossOrigin(origins = "*") // Allow the frontend to talk to this
public class ItemController {
    @Autowired
    private com.campusshare.backend.util.JwtUtil jwtUtil;

    @Autowired
    private ItemRepository itemRepository;
    
    @Autowired 
    private com.campusshare.backend.repository.UserRepository userRepository; // Ensure this exists

    // 1. 📝 ADD ITEM (Now saves Lender Name)
    @PostMapping("/add")
    public ResponseEntity<?> addItem(@RequestHeader("Authorization") String token, @RequestBody Item item) {
        String email = jwtUtil.extractEmail(token);
        if (email == null) return ResponseEntity.status(401).body("Invalid Token");

        // ✅ FETCH LENDER NAME
        com.campusshare.backend.model.User lender = userRepository.findByEmail(email).orElse(null);
        String displayName = (lender != null && lender.getUsername() != null) ? lender.getUsername() : "Student";
        String phone = (lender != null) ? lender.getPhoneNumber() : "";

        item.setLenderEmail(email);
        item.setLenderName(displayName); // Save Name
        item.setLenderPhone(phone); // Save  Phone
        
        Item savedItem = itemRepository.save(item);
        return ResponseEntity.ok(savedItem);
    }
    // 2. GET MY ITEMS API (For Lender Dashboard)
    // URL: /api/items/lender?email=...
    @GetMapping("/lender")
    public ResponseEntity<List<Item>> getLenderItems(@RequestParam String email) {
        // Clean the email just in case
        String cleanEmail = email.replace("\"", "").trim();
        List<Item> items = itemRepository.findByLenderEmail(cleanEmail);
        return ResponseEntity.ok(items);
    }
    // 3. GET AVAILABLE ITEMS (With Sorting & Limits)
    @GetMapping("/available")
    public ResponseEntity<List<Item>> getAllAvailableItems(
            @RequestParam(defaultValue = "newest") String sort) {
        
        Sort sortObj = Sort.unsorted();

        // 🔀 Sorting Logic
        switch (sort) {
            case "top_rated":
                sortObj = Sort.by("itemRating").descending(); 
                break;
            case "oldest":
                sortObj = Sort.by("createdAt").ascending();
                break;
            case "price_low":
                sortObj = Sort.by("pricePerDay").ascending();
                break;
            case "newest":
            default:
                sortObj = Sort.by("createdAt").descending(); // Default: Newest first
                break;
        }

        // Fetch top 50 items with the chosen sort
        List<Item> items = itemRepository.findByStatus("AVAILABLE", PageRequest.of(0, 50, sortObj));
        
        return ResponseEntity.ok(items);
    }

    // 4. DELETE ITEM (Safe Version)
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteItem(@PathVariable java.util.UUID id) {
        return itemRepository.findById(id).map(item -> {
            // 🛑 BLOCK if active
            if (!"AVAILABLE".equals(item.getStatus())) {
                return ResponseEntity.badRequest().body("Cannot delete! This item is currently in use or requested.");
            }
            
            itemRepository.deleteById(id);
            return ResponseEntity.ok("Item deleted successfully");
        }).orElse(ResponseEntity.status(404).body("Item not found"));
    }
    // 5. RENT AN ITEM (The "Buy" Button)
    @PostMapping("/rent/{itemId}")
    public ResponseEntity<String> rentItem(@PathVariable UUID itemId, @RequestBody String borrowerEmail) {
        Item item = itemRepository.findById(itemId).orElse(null);
        if (item == null) return ResponseEntity.badRequest().body("Item not found");

        if (!"AVAILABLE".equals(item.getStatus())) {
            return ResponseEntity.badRequest().body("Item is not available.");
        }

        // Generate a 4-digit random Passkey
        String secretCode = String.valueOf((int)(Math.random() * 9000) + 1000);

        item.setStatus("PENDING"); // Hide from others, but timer hasn't started
        item.setBorrowerEmail(borrowerEmail.replace("\"", ""));
        item.setPasskey(secretCode);
        item.setAvailable(false); // Remove from public list

        itemRepository.save(item);

        return ResponseEntity.ok("Request Sent! Ask the Lender for the Passkey.");
    }
    
    // 6. CONFIRM RENT (Start Timer & Generate Return Key)
    @PostMapping("/confirm/{itemId}")
    public ResponseEntity<String> confirmRent(@PathVariable java.util.UUID itemId, @RequestBody String userEnteredPasskey) {
        return itemRepository.findById(itemId).map(item -> {
            
            // 🛡️ SAFETY CHECK: Did the lender cancel it?
            if (item.getPasskey() == null) {
                return ResponseEntity.badRequest().body("Too late! The Lender cancelled this request.");
            }

            String inputCode = userEnteredPasskey.replace("\"", "").trim();
            if (!item.getPasskey().equals(inputCode)) {
                return ResponseEntity.status(401).body("Wrong Passkey!");
            }

            // START RENTAL
            item.setStatus("RENTED");
            item.setBorrowedAt(java.time.LocalDateTime.now());
            item.setLastPaymentDate(java.time.LocalDate.now().minusDays(1)); 
            item.setPasskey(null); // Clear pickup key

            // Generate RETURN Passkey
            String returnCode = String.valueOf((int)(Math.random() * 9000) + 1000);
            item.setReturnPasskey(returnCode);

            itemRepository.save(item);
            return ResponseEntity.ok("Confirmed! Timer Started.");
        }).orElse(ResponseEntity.status(404).body("Item not found"));
    }
    // 6. GET MY BORROWED ITEMS (The "My Borrowed" Page)
    
    @GetMapping("/borrowed/{email}")
    public ResponseEntity<List<Item>> getBorrowedItems(@PathVariable String email) {
        List<Item> items = itemRepository.findByBorrowerEmail(email);
        return ResponseEntity.ok(items);
    }
    // 7. GET DUE AMOUNT (Calculates Day-Wise Fee)
    @GetMapping("/due/{itemId}")
    public ResponseEntity<Double> getDueAmount(@PathVariable UUID itemId) {
        Item item = itemRepository.findById(itemId).orElse(null);
        if (item == null) return ResponseEntity.badRequest().body(0.0);

        if (item.getLastPaymentDate() == null) return ResponseEntity.ok(0.0);

        // Calculate days passed since last payment
        long daysUnpaid = java.time.temporal.ChronoUnit.DAYS.between(
            item.getLastPaymentDate(), 
            java.time.LocalDate.now()
        );

        // If daysUnpaid is 0, it means they paid today. If > 0, they owe money.
        if (daysUnpaid < 0) daysUnpaid = 0; 

        double due = daysUnpaid * item.getPricePerDay();
        return ResponseEntity.ok(due);
    }

    // 8. PAY FEES (Mock Payment Gateway)
    @PostMapping("/pay/{itemId}")
    public ResponseEntity<String> payFees(@PathVariable UUID itemId) {
        Item item = itemRepository.findById(itemId).orElse(null);
        if (item == null) return ResponseEntity.badRequest().body("Item not found");

        // Update Last Payment Date to TODAY
        item.setLastPaymentDate(java.time.LocalDate.now());
        itemRepository.save(item);

        return ResponseEntity.ok("Payment Successful! Balance cleared for today.");
    }

    // 9. RETURN ITEM (Stops Cycle - Requires OTP & Zero Balance)
    @PostMapping("/return/{itemId}")
    public ResponseEntity<String> returnItem(@PathVariable UUID itemId, @RequestBody String returnOtp) {
        Item item = itemRepository.findById(itemId).orElse(null);
        if (item == null) return ResponseEntity.badRequest().body("Item not found");

        // 1. Check Dues
        long daysUnpaid = java.time.temporal.ChronoUnit.DAYS.between(
            item.getLastPaymentDate(), java.time.LocalDate.now()
        );
        if (daysUnpaid > 0) {
            return ResponseEntity.badRequest().body("Payment Pending! You owe fees for " + daysUnpaid + " day(s). Pay first.");
        }

        // 2. Check OTP
        String inputCode = returnOtp.replace("\"", "").trim();
        if (!item.getReturnPasskey().equals(inputCode)) {
            return ResponseEntity.status(401).body("Wrong Return OTP!");
        }

        // 3. Close Deal
        item.setStatus("AVAILABLE");
        item.setBorrowerEmail(null);
        item.setBorrowedAt(null);
        item.setReturnPasskey(null);
        item.setLastPaymentDate(null);
        item.setAvailable(true); // Back to public list

        itemRepository.save(item);
        return ResponseEntity.ok("Item Returned Successfully! Cycle Closed.");
    }
    // ✏️ EDIT ITEM ENDPOINT
   @PutMapping("/update/{id}")
    public ResponseEntity<String> updateItem(@PathVariable java.util.UUID id, @RequestBody Item updatedItem) {
        return itemRepository.findById(id).map(item -> {
            item.setName(updatedItem.getName());
            item.setDescription(updatedItem.getDescription());
            item.setPricePerDay(updatedItem.getPricePerDay());
            
            // 📸 NEW: Update Image if provided
            if (updatedItem.getImageBase64() != null && !updatedItem.getImageBase64().isEmpty()) {
                item.setImageBase64(updatedItem.getImageBase64());
            }

            itemRepository.save(item);
            return ResponseEntity.ok("Item updated successfully!");
        }).orElse(ResponseEntity.status(404).body("Item not found"));
    }
    // 1. 🙋‍♂️ REQUEST ITEM (Now saves Borrower Name)
    // 1. 🙋‍♂️ REQUEST ITEM (Scalable + Robust)
    @PostMapping("/request/{itemId}")
    public ResponseEntity<String> requestItem(@PathVariable java.util.UUID itemId, @RequestBody String borrowerEmail) {
        
        // 1. Basic Validation (Check ID and Self-Renting)
        Item item = itemRepository.findById(itemId).orElse(null);
        if (item == null) return ResponseEntity.status(404).body("Item not found");
        
        String cleanEmail = borrowerEmail.replace("\"", "").trim();
        
        // 🛑 SELF-RENT CHECK
        if (cleanEmail.equalsIgnoreCase(item.getLenderEmail())) {
            return ResponseEntity.badRequest().body("You cannot rent your own item!");
        }

        // 2. Fetch Borrower Details (Your Robust Logic)
        com.campusshare.backend.model.User borrower = userRepository.findByEmail(cleanEmail).orElse(null);
        
        // ✅ Your Safer Null Check (Kept exactly as you wanted)
        String bName = (borrower != null && borrower.getUsername() != null) ? borrower.getUsername() : "Student";
        String bPhone = (borrower != null) ? borrower.getPhoneNumber() : "";

        // 3. 🛡️ ATOMIC UPDATE (The Scalability Fix)
        // We pass the calculated 'bName' and 'bPhone' into the atomic query.
        int rowsUpdated = itemRepository.requestItemAtomic(itemId, cleanEmail, bName, bPhone);

        if (rowsUpdated == 1) {
            return ResponseEntity.ok("Request Sent! You secured the spot.");
        } else {
            // If rowsUpdated is 0, it means status was NOT 'AVAILABLE' (someone else got it)
            return ResponseEntity.badRequest().body("Too slow! This item is no longer available.");
        }
    }

    // 2. ✅ APPROVE RENT (Lender Action) - NOW WITH OTP GENERATION
    @PostMapping("/approve/{itemId}")
    public ResponseEntity<String> approveRent(@PathVariable java.util.UUID itemId) {
        return itemRepository.findById(itemId).map(item -> {
            item.setStatus("PENDING"); // Moves to "Handshake" phase
            
            // 🔑 GENERATE THE MISSING OTP
            String secretCode = String.valueOf((int)(Math.random() * 9000) + 1000);
            item.setPasskey(secretCode);
            
            itemRepository.save(item);
            return ResponseEntity.ok("Approved! OTP Generated.");
        }).orElse(ResponseEntity.status(404).body("Item not found"));
    }

    // 3. ❌ REJECT RENT (Lender Action)
    @PostMapping("/reject/{itemId}")
    public ResponseEntity<String> rejectRent(@PathVariable java.util.UUID itemId) {
        return itemRepository.findById(itemId).map(item -> {
            item.setStatus("REJECTED"); // Special status so borrower sees the message
            itemRepository.save(item);
            return ResponseEntity.ok("Rejected.");
        }).orElse(ResponseEntity.status(404).body("Item not found"));
    }
    
    // 4. 👋 DISMISS REJECTION (Borrower Action - Resets Item)
    @PostMapping("/dismiss-rejection/{itemId}")
    public ResponseEntity<String> dismissRejection(@PathVariable java.util.UUID itemId) {
        return itemRepository.findById(itemId).map(item -> {
            item.setStatus("AVAILABLE");
            item.setBorrowerEmail(null); // Clear borrower
            itemRepository.save(item);
            return ResponseEntity.ok("Dismissed");
        }).orElse(ResponseEntity.status(404).body("Item not found"));
    }
    // 5. ⭐ RATE ITEM (Now with Spam Protection)
    @PostMapping("/rate-item/{itemId}")
    public ResponseEntity<String> rateItem(@PathVariable java.util.UUID itemId, @RequestBody int stars) {
        return itemRepository.findById(itemId).map(item -> {
            
            // 🛑 CHECK: Did they already rate?
            if (item.isItemRated()) {
                return ResponseEntity.badRequest().body("You already rated this item!");
            }

            double total = item.getItemRating() * item.getItemRatingCount();
            double newTotal = total + stars;
            int newCount = item.getItemRatingCount() + 1;
            
            item.setItemRating(newTotal / newCount);
            item.setItemRatingCount(newCount);
            
            // ✅ MARK AS RATED
            item.setItemRated(true); 
            
            itemRepository.save(item);
            return ResponseEntity.ok("Item Rated!");
        }).orElse(ResponseEntity.status(404).body("Item not found"));
    }
    // 🆕 RATE LENDER (Linked to this specific Rental)

    @PostMapping("/rate-lender-transaction/{itemId}")
    public ResponseEntity<String> rateLenderTransaction(@PathVariable java.util.UUID itemId, @RequestBody int stars) {
        return itemRepository.findById(itemId).map(item -> {

            // 🛑 CHECK: Did they already rate?
            if (item.isLenderRated()) {
                return ResponseEntity.badRequest().body("You already rated the lender for this transaction!");
            }

            // Fetch the Lender
            com.campusshare.backend.model.User lender = userRepository.findByEmail(item.getLenderEmail()).orElse(null);
            if (lender == null) return ResponseEntity.badRequest().body("Lender not found");

            // Update Lender's Score
            double total = lender.getRating() * lender.getRatingCount();
            double newTotal = total + stars;
            int newCount = lender.getRatingCount() + 1;
            
            lender.setRating(newTotal / newCount);
            lender.setRatingCount(newCount);
            userRepository.save(lender);

            // ✅ MARK AS RATED
            item.setLenderRated(true);
            itemRepository.save(item);

            return ResponseEntity.ok("Lender Rated Successfully!");
        }).orElse(ResponseEntity.status(404).body("Item not found"));
    }
    // 10. 🚫 BORROWER CANCELS PICKUP
    @PostMapping("/cancel-pickup/{itemId}")
    public ResponseEntity<String> cancelPickup(@PathVariable java.util.UUID itemId) {
        return itemRepository.findById(itemId).map(item -> {
            if (!"PENDING".equals(item.getStatus())) {
                return ResponseEntity.badRequest().body("Too late! You can't cancel now.");
            }
            // Set special status so Lender sees the alert
            item.setStatus("CANCELLED_BY_BORROWER");
            item.setPasskey(null); // Destroy the handshake code
            itemRepository.save(item);
            return ResponseEntity.ok("Pickup Cancelled.");
        }).orElse(ResponseEntity.status(404).body("Item not found"));
    }
    
    // 11. 🔄 FORCE RELIST (Fixes "Stuck" Items)
    @PostMapping("/relist/{itemId}")
    public ResponseEntity<String> relistItem(@PathVariable java.util.UUID itemId) {
        return itemRepository.findById(itemId).map(item -> {
            // 1. Reset Status
            item.setStatus("AVAILABLE");
            item.setAvailable(true);

            // 2. Wipe All Borrower Data
            item.setBorrowerEmail(null);
            item.setBorrowerName(null);  // ✅ Clear Name
            item.setBorrowerPhone(null); // ✅ Clear Phone
            
            // 3. Clear Security Codes
            item.setBorrowedAt(null);
            item.setPasskey(null);
            item.setReturnPasskey(null);
            item.setLastPaymentDate(null);

            itemRepository.save(item);
            return ResponseEntity.ok("Item is Live Again!");
        }).orElse(ResponseEntity.status(404).body("Item not found"));
    }
}