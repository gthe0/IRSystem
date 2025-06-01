package com.search.query.evaluation;

import com.search.query.model.Query;
import com.search.query.model.VocabularyTrie.TermData;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;

public class VectorSpaceModel implements RetrievalModel {

    @Override
    public Map<Long, Double> evaluate(Query query, EvaluationContext context) {
        Map<Long, Double> scores = new HashMap<>();
        Map<String, Double> queryWeights = calculateQueryWeights(query, context);
        double queryNorm = calculateQueryNorm(queryWeights);

        queryWeights.forEach((term, queryWeight) -> {
            TermData termData = context.getVocabulary().search(term);
            if (termData == null) return;

            try {
                processPostings(termData.pointer, termData.df, queryWeight, context, scores);
            } catch (IOException e) {
                System.err.println("Error processing term: " + term);
            }
        });

        normalizeScores(scores, queryNorm, context);
        return scores;
    }

    private void processPostings(long pointer, int df, double queryWeight,
            EvaluationContext context, Map<Long, Double> scores) throws IOException {
        context.getPostingsFile().seek(pointer);

        for (int i = 0; i < df; i++) {
            String line = context.getPostingsFile().readLine();
            if (line == null)
                break;

            String[] parts = line.split(" ", 3);
            long docId = Long.parseLong(parts[0]);
            double docTf = Double.parseDouble(parts[1]);

            scores.merge(docId, queryWeight * docTf, Double::sum);
        }
    }


    private Map<String, Double> calculateQueryWeights(Query query, EvaluationContext context) {
        Map<String, Double> weights = new HashMap<>();
        for (var entry : query.getTermFrequency().entrySet()) {
            String term = entry.getKey();
            TermData termData = context.getVocabulary().search(term);
            if (termData == null)
                continue;

            double idf = Math.log(context.getTotalDocuments() / (double) termData.df);
            weights.put(term, entry.getValue() * idf);
        }
        return weights;
    }

    private double calculateQueryNorm(Map<String, Double> queryWeights) {
        return Math.sqrt(queryWeights.values().stream()
                .mapToDouble(v -> v * v)
                .sum());
    }

    private void normalizeScores(Map<Long, Double> scores, double queryNorm, EvaluationContext context) {
        scores.replaceAll((docId, score) -> score / (queryNorm * context.getDocumentNorms().getOrDefault(docId, 1.0)));
    }

}