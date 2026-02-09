package com.campusshare.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    public void sendOtpEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        
        // 🟢 USE THE VARIABLE, NOT HARDCODED STRING
        message.setFrom(senderEmail); 
        
        message.setTo(toEmail);
        message.setSubject("CampusShare Login OTP");
        message.setText("Hello Student,\n\nYour OTP for CampusShare login is: " + otp + "\n\nThis OTP is valid for 5 minutes.\n\n- CampusShare Team");

        mailSender.send(message);
        System.out.println("📧 OTP Sent from " + senderEmail + " to " + toEmail);
    }
    // 🛑 DANGER EMAIL
    public void sendDeleteWarning(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        
        // 🟢 USE THE VARIABLE HERE TOO
        message.setFrom(senderEmail);
        
        message.setTo(toEmail);
        message.setSubject("⚠️ SECURITY WARNING: Account Deletion Request");
        message.setText("Hello,\n\nWe received a request to PERMANENTLY DELETE your CampusShare account.\n\n"
                + "Your Deletion OTP is: " + otp + "\n\n"
                + "If you proceed, all your earnings, history, and listed items will be wiped immediately.\n"
                + "If you did not request this, please ignore this email.\n\n"
                + "- CampusShare Security Team");

        mailSender.send(message);
        System.out.println("🔥 Delete OTP Sent to " + toEmail);
    }
}

