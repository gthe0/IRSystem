package com.search.query.evaluation;

import com.search.query.model.Query;
import com.search.query.model.VocabularyTrie;
import com.search.query.reader.VocabularyReader;
import java.io.*;
import java.nio.file.Path;
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
        Map<Long, Double> documentNorms = new HashMap<>();
        Map<Long, String> documentPaths = new HashMap<>();
        Map<Long, Double> documentLengths = new HashMap<>();
        Map<Long, Double> documentMaxFreqs = new HashMap<>();

        loadDocumentInfo(new File(collectionIndexPath, "DocumentFile.txt"), documentPaths, documentNorms, documentLengths, documentMaxFreqs);

        // Open postings file
        Path postingsFile = Path.of(collectionIndexPath + File.separator +"PostingFile.txt");

        this.context = new EvaluationContext(vocabulary, documentNorms, documentLengths, documentMaxFreqs, documentPaths, postingsFile);

        this.retrievalModel = retrievalModel;

    }

    private void loadDocumentInfo(File docFile, Map<Long, String> documentPaths, Map<Long, Double> documentNorms, Map<Long, Double> documentLengths, Map<Long, Double> documentMaxFreqs)
            throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(docFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ");
                
                long docId      = Long.parseLong(parts[0]);
                
                double maxfreq  = Double.parseDouble(parts[2]);
                double length   = Double.parseDouble(parts[3]);
                double norm     = Double.parseDouble(parts[4]);
                
                documentNorms.put(docId, norm);
                documentPaths.put(docId, parts[1]);
                documentLengths.put(docId, length);
                documentMaxFreqs.put(docId,maxfreq);
            }
        }
    }

    public String getModelName()
    {
        return retrievalModel.getModelName();
    }

    public void setRetrievalModel(RetrievalModel model) {
        this.retrievalModel = model;
    }

    public Map<Long, Double> evaluate(Query query) {
        return retrievalModel.evaluate(query, context);
    }
}