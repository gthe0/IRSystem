package com.search.query.evaluation;

import com.search.query.model.VocabularyTrie;

import java.util.List;
import java.util.Map;
import java.nio.file.Path;

public class EvaluationContext {
    private final VocabularyTrie vocabulary;
    private final List<String> documentEntries;
    private final Map<Long, Double> documentNorms;
    private final long totalDocuments;
    private final Path postingsFile;

    public EvaluationContext(VocabularyTrie vocabulary, 
                            Map<Long, Double> documentNorms,
                            List<String> documentEntries,
                            Path postingsFile) {
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
    public Path getPostingsPath() { return postingsFile; }
}