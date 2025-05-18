package com.search.indexer.utils;

import java.util.ArrayList;
import java.util.List;

public class FileBatchCollector {
    private final List<String> docPaths = new ArrayList<>();
    private final List<String> vocabPaths = new ArrayList<>();
    private final List<String> postingsPaths = new ArrayList<>();
    
    private final Object lock = new Object();

    /**
     * Adds a pair of file paths to the collector
     * @param vocabPath Path to vocabulary file
     * @param postingsPath Path to corresponding postings file
     */
    public void add(String vocabPath, String postingsPath, String docPath) {
        synchronized (lock) {
            vocabPaths.add(vocabPath);
            postingsPaths.add(postingsPath);
            docPaths.add(docPath);
        }
    }

    public void add(List<String> lStrings) {
        synchronized (lock) {
            vocabPaths.add(lStrings.get(0));
            postingsPaths.add(lStrings.get(1));
            docPaths.add(lStrings.get(2));
        }
    }

    /**
     * @return Unmodifiable list of collected vocabulary file paths
     */
    public List<String> getVocabPaths() {
        return (vocabPaths);
    }

    /**
     * @return Unmodifiable list of collected postings file paths
     */
    public List<String> getPostingsPaths() {
        return ((postingsPaths));
    }

    /**
     * @return Unmodifiable list of collected document file paths
     */
    public List<String> getDocPaths() {
        return docPaths;
    }
}