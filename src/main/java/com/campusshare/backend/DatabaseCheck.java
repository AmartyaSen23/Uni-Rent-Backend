package com.campusshare.backend;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.sql.Connection;

@Component
public class DatabaseCheck implements CommandLineRunner {

    private final DataSource dataSource;

    public DatabaseCheck(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Testing Database Connection...");
        try (Connection connection = dataSource.getConnection()) {
            System.out.println("✅ SUCCESS: Connected to Supabase Database: " + connection.getCatalog());
        } catch (Exception e) {
            System.out.println("❌ FAILURE: Could not connect to Database!");
            e.printStackTrace();
        }
    }
}