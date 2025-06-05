package com.search.query.evaluation;

import com.search.query.model.VocabularyTrie;

import java.util.Map;
import java.nio.file.Path;

public class EvaluationContext {
    private final VocabularyTrie vocabulary;
    private final Map<Long, String> documentPaths;
    private final Map<Long, Double> documentNorms;
    private final Map<Long, Double> documentLengths;
    private final Map<Long, Double> documentMaxFreqs;
    private final long totalDocuments;
    private final Path postingsFile;

    public EvaluationContext(VocabularyTrie vocabulary, 
                            Map<Long, Double> documentNorms,
                            Map<Long, Double> documentLengths,
                            Map<Long, Double> documentMaxFreqs,
                            Map<Long, String> documentPaths,
                            Path postingsFile) {
        this.vocabulary = vocabulary;
        this.documentNorms = documentNorms;
        this.documentLengths = documentLengths;
        this.documentMaxFreqs = documentMaxFreqs;
        this.documentPaths = documentPaths;
        this.totalDocuments = documentPaths.size();
        this.postingsFile = postingsFile;
    }

    // Getters
    public VocabularyTrie getVocabulary() { return vocabulary; }
    public Map<Long, String> getDocumentPaths() { return documentPaths; }
    public Map<Long, Double> getDocumentNorms() { return documentNorms; }
    public Map<Long, Double> getDocumentLengths() { return documentLengths; }
    public Map<Long, Double> getDocumentMaxFrequenc() { return documentMaxFreqs; }
    public long getTotalDocuments() { return totalDocuments; }
    public Path getPostingsPath() { return postingsFile; }
}