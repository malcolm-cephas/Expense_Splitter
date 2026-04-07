package com.malcolm.expensesplitter.services;

import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class CategoryDetectionService {

    private final Map<String, String> keywordMap = new HashMap<>();

    public CategoryDetectionService() {
        // Default mappings
        keywordMap.put("uber", "Travel");
        keywordMap.put("ola", "Travel");
        keywordMap.put("lyft", "Travel");
        keywordMap.put("taxi", "Travel");
        keywordMap.put("pizza", "Food");
        keywordMap.put("restaurant", "Food");
        keywordMap.put("zomato", "Food");
        keywordMap.put("swiggy", "Food");
        keywordMap.put("amazon", "Shopping");
        keywordMap.put("flipkart", "Shopping");
        keywordMap.put("fuel", "Transport");
        keywordMap.put("petrol", "Transport");
        keywordMap.put("diesel", "Transport");
        keywordMap.put("rent", "Rent");
        keywordMap.put("electricity", "Utilities");
        keywordMap.put("water", "Utilities");
        keywordMap.put("internet", "Utilities");
        keywordMap.put("grocery", "Groceries");
        keywordMap.put("supermarket", "Groceries");
        keywordMap.put("netflix", "Entertainment");
        keywordMap.put("movie", "Entertainment");
        keywordMap.put("cinema", "Entertainment");
    }

    public String detectCategory(String description) {
        if (description == null || description.isEmpty()) {
            return "Other";
        }

        String lowerDesc = description.toLowerCase();
        for (Map.Entry<String, String> entry : keywordMap.entrySet()) {
            if (lowerDesc.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return "Other";
    }

    public void addKeywordMapping(String keyword, String category) {
        keywordMap.put(keyword.toLowerCase(), category);
    }
}
