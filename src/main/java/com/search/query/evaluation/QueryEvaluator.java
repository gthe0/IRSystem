package com.search.query.evaluation;

import com.search.query.model.Query;
import com.search.query.model.VocabularyTrie;
import com.search.query.reader.VocabularyReader;
import java.io.*;
import java.util.*;

public class QueryEvaluator {
    private final VocabularyTrie vocabulary;
    private final Map<Long, Double> documentNorms;
    private final List<String> documentEntries;
    private final RandomAccessFile postingsFile;
    private final long totalDocuments;

    public QueryEvaluator(String collectionIndexPath) throws IOException {
        // Load vocabulary
        this.vocabulary = new VocabularyTrie();
        VocabularyReader vocabReader = new VocabularyReader();
        File vocabFile = new File(collectionIndexPath, "VocabularyFile.txt");
        vocabReader.loadVocabulary(vocabFile, vocabulary);

        // Load document norms
        this.documentEntries = new ArrayList<>();
        this.documentNorms = new HashMap<>();
        loadDocumentNorms(new File(collectionIndexPath, "DocumentFile.txt"));

        // Open postings file
        this.postingsFile = new RandomAccessFile(
            new File(collectionIndexPath, "PostingFile.txt"), "r"
        );
        
        this.totalDocuments = documentEntries.size();
    }

    private void loadDocumentNorms(File docFile) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(docFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                documentEntries.add(line);
                String[] parts = line.split(" ");
                long docId = Long.parseLong(parts[0]);
                double norm = Double.parseDouble(parts[2]);
                documentNorms.put(docId, norm);
            }
        }
    }

    public Map<Long, Double> evaluate(Query query) {
        Map<Long, Double> scores = new HashMap<>();
        Map<String, Double> queryWeights = calculateQueryWeights(query);
        double queryNorm = calculateQueryNorm(queryWeights);

        for (Map.Entry<String, Double> entry : queryWeights.entrySet()) {
            String term = entry.getKey();
            double queryWeight = entry.getValue();
            
            VocabularyTrie.TermData termData = vocabulary.search(term);
            if (termData == null) continue;

            try {
                processPostings(termData.pointer, termData.df, queryWeight, scores);
            } catch (IOException e) {
                System.err.println("Error processing postings for term: " + term);
            }
        }

        // Normalize scores
        for (Map.Entry<Long, Double> entry : scores.entrySet()) {
            double docNorm = documentNorms.getOrDefault(entry.getKey(), 1.0);
            entry.setValue(entry.getValue() / (queryNorm * docNorm));
        }

        return scores;
    }

    private Map<String, Double> calculateQueryWeights(Query query) {
        Map<String, Double> weights = new HashMap<>();
        for (Map.Entry<String, Double> entry : query.getTermFrequency().entrySet()) {
            String term = entry.getKey();
            VocabularyTrie.TermData termData = vocabulary.search(term);
            if (termData == null) continue;

            double idf = Math.log(totalDocuments / (double) termData.df);
            weights.put(term, entry.getValue() * idf);
        }
        return weights;
    }

    private double calculateQueryNorm(Map<String, Double> queryWeights) {
        double sum = queryWeights.values().stream()
            .mapToDouble(v -> Math.pow(v, 2))
            .sum();
        return Math.sqrt(sum);
    }

    private void processPostings(long pointer, int df, double queryWeight, 
                                Map<Long, Double> scores) throws IOException {
        postingsFile.seek(pointer);
        for (int i = 0; i < df; i++) {
            String line = postingsFile.readLine();
            if (line == null) break;

            String[] parts = line.split(" ", 3);
            long docId = Long.parseLong(parts[0]);
            double docTf = Double.parseDouble(parts[1]);

            scores.merge(docId, queryWeight * docTf, Double::sum);
        }
    }

    public void printResults(Map<Long, Double> results, int maxResults) {
        results.entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .limit(maxResults)
            .forEachOrdered(entry -> {
                String docPath = documentEntries.stream()
                    .filter(e -> e.startsWith(entry.getKey().toString()))
                    .findFirst()
                    .map(e -> e.split(" ")[1])
                    .orElse("Unknown Document");
                
                System.out.printf("DocID: %-10d Score: %-8.4f Path: %s%n",
                                entry.getKey(), entry.getValue(), docPath);
            });
    }

    public void close() {
        try {
            postingsFile.close();
        } catch (IOException e) {
            System.err.println("Error closing postings file: " + e.getMessage());
        }
    }
}