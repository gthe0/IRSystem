package com.search.query.evaluation;

import com.search.query.model.VocabularyTrie;

import java.util.List;
import java.util.Map;
import java.io.RandomAccessFile;

public class EvaluationContext {
    private final VocabularyTrie vocabulary;
    private final List<String> documentEntries;
    private final Map<Long, Double> documentNorms;
    private final long totalDocuments;
    private final RandomAccessFile postingsFile;

    public EvaluationContext(VocabularyTrie vocabulary, 
                            Map<Long, Double> documentNorms,
                            List<String> documentEntries,
                            RandomAccessFile postingsFile) {
        this.vocabulary = vocabulary;
        this.documentNorms = documentNorms;
        this.documentEntries = documentEntries;
        this.totalDocuments = documentEntries.size();
        this.postingsFile = postingsFile;
    }

    // Getters
    public VocabularyTrie getVocabulary() { return vocabulary; }
    public List<String> getDocumentEntries() { return documentEntries; }
    public Map<Long, Double> getDocumentNorms() { return documentNorms; }
    public long getTotalDocuments() { return totalDocuments; }
    public RandomAccessFile getPostingsFile() { return postingsFile; }
}