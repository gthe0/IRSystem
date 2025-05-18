package com.search.query.model;

import java.util.TreeMap;
import java.util.List;
import java.util.Map;

public class Query {
    private int                  id;
    private String               query;
    private Map<String, Double>  termFrequency; 

    // Constructor to initialize the Query object
    public Query(int id, String query, List<String> tokens) {
        this.id = id;
        this.query = query.toLowerCase().trim();
        this.termFrequency = new TreeMap<>();
        calculateTermFreq(tokens);
    }

    // Getter for the query ID
    public int getId() {
        return id;
    }

    public String getQuery() {
        return query;
    }

    public Map<String, Double> getTermFrequency() {
        return termFrequency;
    }

    // Method to calculate term frequencies and normalize them
    public boolean calculateTermFreq(List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return false;
        }
    
        // Count raw frequencies using provided tokens
        Map<String, Integer> rawTermFrequency = new TreeMap<>();
        for (String token : tokens) {
            if (!token.isEmpty()) {
                rawTermFrequency.put(token, rawTermFrequency.getOrDefault(token, 0) + 1);
            }
        }
    
        // Find the maximum frequency
        int maxFrequency = rawTermFrequency.values().stream()
                                        .max(Integer::compare)
                                        .orElse(1); // Safeguard against empty maps
    
        // Normalize frequencies using max frequency
        termFrequency.clear();
        for (Map.Entry<String, Integer> entry : rawTermFrequency.entrySet()) {
            termFrequency.put(entry.getKey(), entry.getValue() / (double) maxFrequency);
        }
    
        return true;
    }

    // Override toString for a clean representation
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Query ID: ").append(id).append("\nQuery: ").append(query).append("\nNormalized Term Frequencies:\n");
        for (Map.Entry<String, Double> entry : termFrequency.entrySet()) {
            sb.append("    ").append(entry.getKey()).append(": ").append(String.format("%.4f", entry.getValue())).append("\n");
        }
        return sb.toString();
    }
}
