package com.search.index;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class Vocabulary {
    private HashMap<String, TreeSet<Integer>> termToDocIds; // Mapping of terms to PMCID (document IDs)

    private void updateVocabulary(Document document) {
        Integer pmcdId = document.getPmcdId();

        for (Map.Entry<String, Integer> entry : document.getTf().entrySet()) {
            String term = entry.getKey();
            addTerm(term, pmcdId);
        }
    }

    public Vocabulary() {
        termToDocIds = new HashMap<>();
    }

    public Vocabulary(List<Document> docs) {
        this();
        updateVocabulary(docs);
    }


    // Add a term and the document ID it was found in
    public void addTerm(String term, Integer pmcdId) {
        termToDocIds.putIfAbsent(term, new TreeSet<>());
        termToDocIds.get(term).add(pmcdId);
    }

    // Get the document frequency (df) for a term
    public int getDocumentFrequency(String term) {
        return termToDocIds.getOrDefault(term, new TreeSet<>()).size();
    }

    // Get all terms in lexicographic order
    public Set<String> getSortedTerms() {
        return new TreeSet<>(termToDocIds.keySet());
    }

    // Export the vocabulary to a file
    public void exportVocabulary(String filePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (String term : getSortedTerms()) {
                int documentFrequency = getDocumentFrequency(term);
                writer.write(term + " " + documentFrequency + "\n");
            }
        }
    }

    // Retrieve the set of document IDs for a given term
    public Set<Integer> getDocumentIds(String term) {
        return termToDocIds.getOrDefault(term, new TreeSet<>());
    }

    public void updateVocabulary(List<Document> documents) {
        for (Document document : documents) {
            updateVocabulary(document);
        }
    }

}
