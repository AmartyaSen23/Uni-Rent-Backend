package com.campusshare.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling; // 1. Add Import

@SpringBootApplication
@EnableScheduling // 2. Add Annotation
public class CampusshareBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(CampusshareBackendApplication.class, args);
    }
}