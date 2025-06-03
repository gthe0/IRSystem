package com.search.query.evaluation;

import com.search.query.model.Query;
import com.search.query.model.VocabularyTrie.TermData;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.HashMap;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;

public class OkapiBM25 implements RetrievalModel {
    private static final int MAX_RESULTS = 1000;
    private static final int BUFFER_SIZE = 64 * 1024; // 64KB buffer
    private static final double K1 = 1.2;
    private static final double B = 0.75;
    
    @Override
    public Map<Long, Double> evaluate(Query query, EvaluationContext context) {
        PriorityQueue<Map.Entry<Long, Double>> topResults = 
            new PriorityQueue<>(MAX_RESULTS, Map.Entry.comparingByValue());
        
        // Precompute collection statistics
        double avgDocLength = calculateAverageDocLength(context);
        Map<String, Double> idfCache = new HashMap<>();
        Map<Long, Double> accumulators = new HashMap<>(1024);

        try (FileChannel channel = FileChannel.open(context.getPostingsPath(), 
                                                  StandardOpenOption.READ)) {
            
            ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
            
            for (String term : query.getTermFrequency().keySet()) {
                TermData termData = context.getVocabulary().search(term);
                if (termData == null) continue;

                // Compute IDF if not cached
                double idf = idfCache.computeIfAbsent(term, 
                    k -> calculateIDF(termData.df, context.getTotalDocuments()));
                
                processTermPostings(channel, buffer, termData, idf, accumulators, 
                                   context, avgDocLength);
            }
        } catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
            return Map.of();
        }

        // Collect top results
        for (var entry : accumulators.entrySet()) {
            double score = entry.getValue();
            
            if (topResults.size() < MAX_RESULTS) {
                topResults.offer(Map.entry(entry.getKey(), score));
            } else if (score > topResults.peek().getValue()) {
                topResults.poll();
                topResults.offer(Map.entry(entry.getKey(), score));
            }
        }

        // Convert to result map
        Map<Long, Double> results = new HashMap<>(topResults.size());
        for (var entry : topResults) {
            results.put(entry.getKey(), entry.getValue());
        }
        
        return results;
    }

    public String getModelName() {
        return "OkapiBM25";
    }

    private double calculateAverageDocLength(EvaluationContext context) {
        return context.getDocumentLengths().values().stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(1.0);
    }

    private double calculateIDF(int docFreq, long totalDocs) {
        return Math.log(1 + (totalDocs - docFreq + 0.5) / (docFreq + 0.5));
    }

    private void processTermPostings(FileChannel channel, ByteBuffer buffer,
                                    TermData termData, double idf,
                                    Map<Long, Double> accumulators,
                                    EvaluationContext context, 
                                    double avgDocLength) throws IOException {
        long position = termData.pointer;
        int remaining = termData.df;
        StringBuilder sb = new StringBuilder(128);
        
        channel.position(position);
        buffer.clear();
        
        while (remaining > 0 && channel.read(buffer) != -1) {
            buffer.flip();
            
            while (buffer.hasRemaining() && remaining > 0) {
                char c = (char) buffer.get();
                if (c == '\n') {
                    processPostingLine(sb.toString(), idf, accumulators, context, avgDocLength);
                    sb.setLength(0);
                    remaining--;
                } else {
                    sb.append(c);
                }
            }
            
            buffer.compact();
        }
        
        // Process last line if any
        if (sb.length() > 0 && remaining > 0) {
            processPostingLine(sb.toString(), idf, accumulators, context, avgDocLength);
        }
    }

    private void processPostingLine(String line, double idf, 
                                    Map<Long, Double> accumulators,
                                    EvaluationContext context,
                                    double avgDocLength) {
        int firstSpace = line.indexOf(' ');
        if (firstSpace <= 0) return;
        
        try {
            long docId = Long.parseLong(line.substring(0, firstSpace));
            int secondSpace = line.indexOf(' ', firstSpace + 1);
            double termFreq = Double.parseDouble(line.substring(firstSpace + 1, secondSpace));
            
            // Get document length
            double docLength = context.getDocumentLengths().getOrDefault(docId, avgDocLength);
            
            // Calculate BM25 component
            double numerator = termFreq * (K1 + 1);
            double denominator = termFreq + K1 * (1 - B + B * (docLength / avgDocLength));
            double termScore = idf * (numerator / denominator);
            
            accumulators.merge(docId, termScore, Double::sum);
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            System.err.println("Invalid posting line: " + line);
        }
    }
}