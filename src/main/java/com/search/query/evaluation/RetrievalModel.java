package com.search.query.evaluation;

import com.search.query.model.Query;
import java.util.Map;

public interface RetrievalModel {
    Map<Long, Double> evaluate(Query query, EvaluationContext context);
    String getModelName();
}