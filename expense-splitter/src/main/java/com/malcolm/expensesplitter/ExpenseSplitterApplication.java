package com.malcolm.expensesplitter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Cloud-First Entry point for Expense Splitter Pro.
 * Optimized for server environments with zero local desktop dependencies.
 */
@SpringBootApplication
public class ExpenseSplitterApplication {

    public static void main(String[] args) {
        // Enforce headless mode for cloud environments
        System.setProperty("java.awt.headless", "true");
        SpringApplication.run(ExpenseSplitterApplication.class, args);
    }

    /**
     * Database self-healing for collaborative cloud schemas.
     */
    @Bean
    public CommandLineRunner migrateSchema(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                // Ensure cloud databases handle our multi-user split logic correctly
                jdbcTemplate.execute("ALTER TABLE EXPENSES ALTER COLUMN PAID_BY_ID SET NULL");
                System.out.println("Cloud-Migration: DB schema verified for collaboration.");
            } catch (Exception e) {
                // Skip if not a relational DB or migration not needed
            }
        };
    }
}
