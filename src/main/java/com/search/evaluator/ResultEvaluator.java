package com.search.evaluator;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class ResultEvaluator {
    // Stores qrels: query -> (docID -> relevance)
    private final Map<Integer, Map<String, Integer>> qrels = new HashMap<>();
    
    public ResultEvaluator(String qrelsPath) throws IOException {
        readQrels(qrelsPath);
    }

    private void readQrels(String path) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                
                String[] tokens = line.split("\t");
                if (tokens.length < 4) continue;
                
                try {
                    int queryId = Integer.parseInt(tokens[0].trim());
                    String docId = tokens[2].trim();
                    int relevance = Integer.parseInt(tokens[3].trim());
                    
                    qrels.computeIfAbsent(queryId, k -> new HashMap<>())
                         .put(docId, relevance);
                } catch (NumberFormatException e) {
                    System.err.println("Skipping invalid qrels line: " + line);
                }
            }
        }
    }

    public void evaluate(List<String> resultPaths, String outputPath) throws IOException {
        // Read and combine all result files
        Map<Integer, List<ResultEntry>> results = readResults(resultPaths);
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {
            writer.println("QUERY_NO\tBPREF_VALUE\tAVEP_VALUE\tNDCG_VALUE");
            
            for (int queryId : results.keySet()) {
                if (!qrels.containsKey(queryId)) {
                    System.err.println("Skipping query " + queryId + " (missing qrels)");
                    continue;
                }
                
                List<ResultEntry> rankedResults = results.get(queryId);
                Map<String, Integer> queryQrels = qrels.get(queryId);
                
                double bpref = calculateBPREF(rankedResults, queryQrels);
                double avep = calculateAVEP(rankedResults, queryQrels);
                double ndcg = calculateNDCG(rankedResults, queryQrels);
                
                writer.printf("%d\t%.6f\t%.6f\t%.6f\n", queryId, bpref, avep, ndcg);
            }
        }
    }

    private Map<Integer, List<ResultEntry>> readResults(List<String> paths) throws IOException {
        Map<Integer, List<ResultEntry>> results = new HashMap<>();
        
        for (String path : paths) {
            try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("QUERY_ID")) continue;  // Skip header
                    
                    String[] tokens = line.split("\t");
                    if (tokens.length < 4) continue;
                    
                    try {
                        int queryId = Integer.parseInt(tokens[0].trim());
                        String docId = tokens[2].trim();
                        int rank = Integer.parseInt(tokens[3].trim());
                        
                        results.computeIfAbsent(queryId, k -> new ArrayList<>())
                               .add(new ResultEntry(docId, rank));
                    } catch (NumberFormatException e) {
                        System.err.println("Skipping invalid result line: " + line);
                    }
                }
            }
        }
        
        // Sort results by rank for each query
        for (List<ResultEntry> entries : results.values()) {
            entries.sort(Comparator.comparingInt(e -> e.rank));
        }
        
        return results;
    }

    private double calculateBPREF(List<ResultEntry> results, Map<String, Integer> qrels) {
        int R = (int) qrels.values().stream().filter(rel -> rel >= 1).count();
        if (R == 0) return 0.0;
        
        int nonrelCount = 0;
        double bprefSum = 0.0;
        
        for (ResultEntry entry : results) {
            Integer relevance = qrels.get(entry.docId);
            if (relevance == null) continue;  // Skip unjudged
            
            if (relevance >= 1) {
                bprefSum += 1.0 - (Math.min(nonrelCount, R) / (double) R);
            } else if (relevance == 0) {
                nonrelCount++;
            }
        }
        
        return bprefSum / R;
    }

    private double calculateAVEP(List<ResultEntry> results, Map<String, Integer> qrels) {
        int R = (int) qrels.values().stream().filter(rel -> rel >= 1).count();
        if (R == 0) return 0.0;
        
        int relevantCount = 0;
        double precisionSum = 0.0;
        int rank = 0;
        
        for (ResultEntry entry : results) {
            rank++;
            Integer relevance = qrels.get(entry.docId);
            if (relevance == null || relevance < 1) continue;
            
            relevantCount++;
            precisionSum += (double) relevantCount / rank;
        }
        
        return precisionSum / R;
    }

    private double calculateNDCG(List<ResultEntry> results, Map<String, Integer> qrels) {
        final int k = 10;
        double dcg = 0.0;
        
        // Calculate DCG@10
        int size = Math.min(k, results.size());
        for (int i = 0; i < size; i++) {
            int rank = i + 1;
            String docId = results.get(i).docId;
            int rel = qrels.getOrDefault(docId, 0);
            dcg += rel * (Math.log(2) / Math.log(rank + 1));
        }
        
        // Calculate IDCG@10
        List<Integer> gains = qrels.values().stream()
            .sorted(Comparator.reverseOrder())
            .limit(k)
            .collect(Collectors.toList());
        
        double idcg = 0.0;
        for (int i = 0; i < gains.size(); i++) {
            int rank = i + 1;
            idcg += gains.get(i) * (Math.log(2) / Math.log(rank + 1));
        }
        
        return (idcg > 0) ? dcg / idcg : 0.0;
    }

    private static class ResultEntry {
        final String docId;
        final int rank;
        
        ResultEntry(String docId, int rank) {
            this.docId = docId;
            this.rank = rank;
        }
    }
}