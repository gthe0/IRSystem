package com.search.query.expansion;

import com.search.common.document.Document;
import com.search.common.document.DocumentFactory;
import com.search.common.token.SimpleTokenStream;
import com.search.common.token.TokenStream;
import com.search.common.utils.StopWordManager;
import com.search.query.evaluation.EvaluationContext;
import com.search.query.model.Query;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class QueryExpander {

    private static final int TERMS_TO_KEEP = 10;
    private static final int FILES_TO_SCAN = 10;

    // Static method to expand a query using evaluation results
    public static Query expand(Query query, EvaluationContext context, Map<Long, Double> evaluationResults) {

        // Sort results by score descending
        List<Map.Entry<Long, Double>> sortedResults = new ArrayList<>(evaluationResults.entrySet());
        sortedResults.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        Map<Long, String> documentPaths = context.getDocumentPaths();
        List<Document> documentList = new ArrayList<>();

        for (int i = 0; i < sortedResults.size() && i < FILES_TO_SCAN; i++) {
            try {
                documentList.add(
                        DocumentFactory.createDocument(new File(documentPaths.get(sortedResults.get(i).getKey()))));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Map<String, Integer> termFrequencies = new HashMap<>();
        for (Document doc : documentList) {
            for (Map.Entry<String, Integer> entry : doc.getTf().entrySet()) {
                String term = entry.getKey();

                // Exclude terms that contain only numbers
                if (!term.matches("\\d+")) {
                    termFrequencies.put(term, termFrequencies.getOrDefault(term, 0) + entry.getValue());
                }
            }
        }

        List<Map.Entry<String, Integer>> sortedTerms = new ArrayList<>(termFrequencies.entrySet());
        sortedTerms.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        String newQuery = query.getQuery() + " " + sortedTerms.stream()
                .limit(TERMS_TO_KEEP)
                .map(Map.Entry::getKey)
                .collect(Collectors.joining(" "));

        List<String> queryList = process(newQuery);
        return new Query(query.getId(), newQuery, queryList);
    }

    private static List<String> process(String text) {
        List<String> tokens = new ArrayList<>();
        text = text.trim();
        if (text.isEmpty())
            return tokens;

        try (TokenStream ts = new SimpleTokenStream(text, StopWordManager.getStopWords())) {
            String token;
            while ((token = ts.getNext()) != null) {
                tokens.add(token);
            }
        } catch (IOException e) {
            System.err.println("Error processing text: " + e.getMessage());
        }
        return tokens;
    }
}
