package com.campusshare.backend.controller;

import com.campusshare.backend.model.User;
import com.campusshare.backend.repository.UserRepository;
import com.campusshare.backend.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.Random;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private com.campusshare.backend.util.JwtUtil jwtUtil;
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;
    
    // 1. LOGIN (BCrypt Check)
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        User user = userRepository.findByEmail(request.email).orElse(null);

        if (user == null || !passwordEncoder.matches(request.password, user.getPassword())) {
             return ResponseEntity.status(401).body("Invalid Email or Password");
        }

        // ✅ GENERATE TOKEN
        String token = jwtUtil.generateToken(user.getEmail());
        
        // ✅ NEW (Now includes Username)
        return ResponseEntity.ok(Map.of(
            "message", "Login Successful",
            "token", token,
            "email", user.getEmail(),
            "username", user.getUsername() // <--- ADD THIS LINE
        ));
    }

    // 2. SIGNUP STEP 1: Send OTP (Smart Check)
    @PostMapping("/send-signup-otp")
    public ResponseEntity<String> sendSignupOtp(@RequestBody Map<String, String> payload) {
        String cleanEmail = payload.get("email");
        if (cleanEmail == null || cleanEmail.isEmpty()) {
            return ResponseEntity.badRequest().body("Email is required");
        }
        User existingUser = userRepository.findByEmail(cleanEmail).orElse(null);
        
        // 🛑 SMART CHECK: Block ONLY if they have a REAL password
        if (existingUser != null) {
            String currentPass = existingUser.getPassword();
            // If password is NOT null AND NOT "temp_pending", they are a real user.
            if (currentPass != null && !currentPass.equals("PENDING_VERIFICATION")) {
                return ResponseEntity.badRequest().body("User already exists! Please Login.");
            }
            // If password IS "temp_pending", we allow them to proceed (Resend OTP logic)
        }

        // Create temp user if not exists
        // Create temp user if not exists
        if (existingUser == null) {
            existingUser = new User();
            existingUser.setEmail(cleanEmail);
            // 🟢 FIX: Fill required fields with DUMMY data to prevent DB crash
            existingUser.setPassword("PENDING_VERIFICATION"); 
            existingUser.setUsername("Guest_" + System.currentTimeMillis()); 
            existingUser.setPhoneNumber("0000000000"); 
        }

        // Generate OTP
        String otp = String.format("%06d", new Random().nextInt(999999));
        existingUser.setOtp(otp);
        existingUser.setOtpGeneratedTime(LocalDateTime.now());
        userRepository.save(existingUser);

        // Send Email
        try {
            emailService.sendOtpEmail(cleanEmail, otp);
            return ResponseEntity.ok("OTP Sent to " + cleanEmail);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error sending email");
        }
    }
    
    // 3. SIGNUP STEP 2: Verify OTP & Encrypt Password
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        User user = userRepository.findByEmail(request.email).orElse(null);

        if (user == null) return ResponseEntity.badRequest().body("User not found");

        if (user.getOtp() == null || !user.getOtp().equals(request.otp)) {
            return ResponseEntity.status(401).body("Invalid OTP");
        }

        // 🔒 ENCRYPT PASSWORD BEFORE SAVING
        user.setPassword(passwordEncoder.encode(request.password));
        user.setPhoneNumber(request.phoneNumber);
        user.setUsername(request.username);
        user.setOtp(null); 
        userRepository.save(user);

        return ResponseEntity.ok("Account Created! You can now Login.");
    }
    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    // DTO Helper Classes
    static class LoginRequest { public String email; public String password; }
    static class RegisterRequest { public String email; public String otp; public String password; public String username; public String phoneNumber; }
    // 4. FORGOT PASSWORD STEP 1: Request OTP
    @PostMapping("/forgot-password-request")
    public ResponseEntity<String> forgotPasswordRequest(@RequestBody Map<String, String> payload) {
        // 2. Extract the email
        String cleanEmail = payload.get("email");
        if (cleanEmail == null || cleanEmail.isEmpty()) {
            return ResponseEntity.badRequest().body("Email is required");
        }
        // 1. Check if user exists (Real user, not temp)
        User user = userRepository.findByEmail(cleanEmail).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body("User not found with this email.");
        }
        
        // 2. Generate OTP (Re-using the existing OTP field)
        String otp = String.format("%06d", new Random().nextInt(999999));
        user.setOtp(otp);
        user.setOtpGeneratedTime(LocalDateTime.now());
        userRepository.save(user);

        // 3. Send Email
        try {
            emailService.sendOtpEmail(cleanEmail, otp);
            return ResponseEntity.ok("OTP Sent! Check your email.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error sending email.");
        }
    }

    // 5. FORGOT PASSWORD STEP 2: Set New Password
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetRequest request) {
        User user = userRepository.findByEmail(request.email).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body("User not found");

        // 1. Verify OTP
        if (user.getOtp() == null || !user.getOtp().equals(request.otp)) {
            return ResponseEntity.status(401).body("Invalid OTP");
        }

        // 2. Encrypt & Save New Password
        user.setPassword(passwordEncoder.encode(request.newPassword));
        user.setOtp(null); // Clear OTP so it can't be reused
        userRepository.save(user);

        return ResponseEntity.ok("Password Reset Successful! Login with new password.");
    }

    // Add this DTO at the bottom with your other classes
    static class ResetRequest { public String email; public String otp; public String newPassword; }

}


