package com.search.query.evaluation;

import com.search.query.model.Query;
import com.search.query.model.VocabularyTrie;
import com.search.query.reader.VocabularyReader;
import java.io.*;
import java.util.*;

public class QueryEvaluator {

    private final EvaluationContext context;
    private RetrievalModel retrievalModel;

    public QueryEvaluator(String collectionIndexPath, RetrievalModel retrievalModel) throws IOException {
        // Load vocabulary
        VocabularyTrie vocabulary = new VocabularyTrie();
        VocabularyReader vocabReader = new VocabularyReader();

        File vocabFile = new File(collectionIndexPath, "VocabularyFile.txt");
        vocabReader.loadVocabulary(vocabFile, vocabulary);

        // Load document norms
        List<String> documentEntries = new ArrayList<>();
        Map<Long, Double> documentNorms = new HashMap<>();
        loadDocumentInfo(new File(collectionIndexPath, "DocumentFile.txt"), documentEntries, documentNorms);

        // Open postings file
        RandomAccessFile postingsFile = new RandomAccessFile(
                new File(collectionIndexPath, "PostingFile.txt"), "r");

        this.context = new EvaluationContext(vocabulary, documentNorms, documentEntries, postingsFile);

        this.retrievalModel = retrievalModel;

    }

    private void loadDocumentInfo(File docFile, List<String> documentEntries, Map<Long, Double> documentNorms)
            throws IOException {
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

    public void setRetrievalModel(RetrievalModel model) {
        this.retrievalModel = model;
    }

    public Map<Long, Double> evaluate(Query query) {
        return retrievalModel.evaluate(query, context);
    }

    public void printResults(Map<Long, Double> results, int maxResults) {
        results.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(maxResults)
                .forEachOrdered(entry -> {
                    String docPath = context.getDocumentEntries().stream()
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
            context.getPostingsFile().close();
        } catch (IOException e) {
            System.err.println("Error closing postings file: " + e.getMessage());
        }
    }
}