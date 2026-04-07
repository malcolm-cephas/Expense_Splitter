package com.malcolm.expensesplitter.services;

import org.springframework.stereotype.Service;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class ReceiptService {

    private static final String RECEIPT_DIR = "data/receipts/";

    /**
     * Copies a selected file to the local data/receipts directory and returns the relative path.
     * 
     * @param sourceFile The file selected by the user.
     * @return The path to the stored file, or null if storage failed.
     */
    public String saveReceipt(File sourceFile) {
        if (sourceFile == null) return null;

        try {
            // Ensure the receipts directory exists
            Path directory = Paths.get(RECEIPT_DIR);
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }

            // Generate a unique filename to prevent overwrites
            String extension = "";
            int i = sourceFile.getName().lastIndexOf('.');
            if (i > 0) {
                extension = sourceFile.getName().substring(i);
            }
            
            String fileName = UUID.randomUUID().toString() + extension;
            Path targetPath = directory.resolve(fileName);

            // Copy the file
            Files.copy(sourceFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            return targetPath.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Checks if a receipt file exists for a given path.
     */
    public boolean receiptExists(String path) {
        if (path == null || path.isEmpty()) return false;
        return Files.exists(Paths.get(path));
    }
}
