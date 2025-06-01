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

public class VectorSpaceModel implements RetrievalModel {
  private static final int MAX_RESULTS = 1000;
    private static final int BUFFER_SIZE = 64 * 1024; // 64KB buffer

    @Override
    public Map<Long, Double> evaluate(Query query, EvaluationContext context) {
        PriorityQueue<Map.Entry<Long, Double>> topResults = 
            new PriorityQueue<>(MAX_RESULTS, Map.Entry.comparingByValue());
        
        Map<String, Double> queryWeights = calculateQueryWeights(query, context);
        double queryNorm = calculateQueryNorm(queryWeights);
        Map<Long, Double> accumulators = new HashMap<>(1024);

        try (FileChannel channel = FileChannel.open(context.getPostingsPath(), 
                                                  StandardOpenOption.READ)) {
            
            ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
            
            for (var entry : queryWeights.entrySet()) {
                String term = entry.getKey();
                double queryWeight = entry.getValue();
                TermData termData = context.getVocabulary().search(term);
                if (termData == null) continue;

                processTermPostings(channel, buffer, termData, queryWeight, accumulators);
            }
        } catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
            return Map.of();
        }

        // Normalize scores and collect top results
        for (var entry : accumulators.entrySet()) {
            long docId = entry.getKey();
            double score = entry.getValue();
            double docNorm = context.getDocumentNorms().getOrDefault(docId, 1.0);
            double normalized = score / (queryNorm * docNorm);
            
            if (topResults.size() < MAX_RESULTS) {
                topResults.offer(Map.entry(docId, normalized));
            } else if (normalized > topResults.peek().getValue()) {
                topResults.poll();
                topResults.offer(Map.entry(docId, normalized));
            }
        }

        // Convert to result map
        Map<Long, Double> results = new HashMap<>(topResults.size());
        for (var entry : topResults) {
            results.put(entry.getKey(), entry.getValue());
        }
        
        return results;
    }

    public String getModelName()
    {
        return "Vector Space Model";
    }

    private void processTermPostings(FileChannel channel, ByteBuffer buffer,
                                    TermData termData, double queryWeight,
                                    Map<Long, Double> accumulators) throws IOException {
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
                    processPostingLine(sb.toString(), queryWeight, accumulators);
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
            processPostingLine(sb.toString(), queryWeight, accumulators);
        }
    }

    private void processPostingLine(String line, double queryWeight, 
                                  Map<Long, Double> accumulators) {
        int firstSpace = line.indexOf(' ');
        if (firstSpace <= 0) return;
        
        try {
            long docId = Long.parseLong(line.substring(0, firstSpace));
            int secondSpace = line.indexOf(' ', firstSpace + 1);
            double docTf = Double.parseDouble(line.substring(firstSpace + 1, secondSpace));
            
            accumulators.merge(docId, queryWeight * docTf, Double::sum);
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            System.err.println("Invalid posting line: " + line);
        }
    }

    private Map<String, Double> calculateQueryWeights(Query query, EvaluationContext context) {
        Map<String, Double> weights = new HashMap<>();
        for (var entry : query.getTermFrequency().entrySet()) {
            String term = entry.getKey();
            TermData termData = context.getVocabulary().search(term);
            if (termData == null)
                continue;

            double idf = Math.log(context.getTotalDocuments() / (double) termData.df);
            weights.put(term, entry.getValue() * idf);
        }
        return weights;
    }

    private double calculateQueryNorm(Map<String, Double> queryWeights) {
        return Math.sqrt(queryWeights.values().stream()
                .mapToDouble(v -> v * v)
                .sum());
    }
}