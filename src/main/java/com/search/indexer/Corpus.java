package com.search.indexer;

import java.util.TreeMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Corpus implements Iterable<Document> {
    private Vocabulary vocabulary;                
    private TreeMap<Integer, Document> documents; 

    // Constructor initializes the vocabulary and document map
    public Corpus() {
        this.vocabulary = new Vocabulary();
        this.documents = new TreeMap<>();
    }

    // Add a single document to the corpus
    public void addDocument(Document document) {
        documents.put(document.getPmcdId(), document);
        for (String term : document.getTf().keySet()) {
            vocabulary.addTerm(term, document.getPmcdId());
        }
    }
    
    // Add multiple documents to the corpus
    public void addDocuments(List<Document> documents) {
        for (Document doc : documents) {
            addDocument(doc);                        
        }
    }

    // Retrieve a document by PMCID
    public Document getDocument(int pmcdId) {
        return documents.get(pmcdId);
    }

    // Get all documents in the corpus
    public Map<Integer, Document> getDocuments() {
        return documents;
    }

    // Get the vocabulary
    public Vocabulary getVocabulary() {
        return vocabulary;
    }

    // Corpus size (number of documents)
    public int size() {
        return documents.size();
    }

    // Clear the corpus t o release memory
    public void clear() {
        documents.clear();
        vocabulary = new Vocabulary();
        System.gc();
    }

    // Make the corpus iterable (for-each loop)
    @Override
    public Iterator<Document> iterator() {
        return documents.values().iterator();
    }
}
