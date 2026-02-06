package com.campusshare.backend.dto;

public class SignupRequest {
    private String email;
    private String password;

    // Getters and Setters (needed for Spring to put JSON data into this box)
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
}