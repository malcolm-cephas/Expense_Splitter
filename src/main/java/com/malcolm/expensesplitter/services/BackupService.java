package com.malcolm.expensesplitter.services;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class BackupService {

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @PreDestroy
    public void backupDatabaseOnShutdown() {
        System.out.println("Initiating database backup before shutdown...");
        
        // Extract file path from JDBC URL (e.g., jdbc:h2:file:./data/expense_splitter_db)
        if (dbUrl != null && dbUrl.startsWith("jdbc:h2:file:")) {
            try {
                String dbFilePathStr = dbUrl.substring("jdbc:h2:file:".length()).split(";")[0] + ".mv.db";
                Path sourcePath = Paths.get(dbFilePathStr);
                
                if (Files.exists(sourcePath)) {
                    Path backupDir = Paths.get("backups");
                    if (!Files.exists(backupDir)) {
                        Files.createDirectories(backupDir);
                    }
                    
                    Path targetPath = backupDir.resolve("expense_splitter_db_backup.mv.db");
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("Database successfully backed up to: " + targetPath.toAbsolutePath());
                } else {
                    System.err.println("Database file not found at expected path: " + sourcePath);
                }
            } catch (Exception e) {
                System.err.println("Failed to backup database: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
