package com.search.query.evaluation;

import java.util.HashMap;
import java.util.Map;

public class IRetrievalModelFactory {
    private static final Map<String, RetrievalModel> modelMap = new HashMap<>();

    static {
        modelMap.put("VSM", new VectorSpaceModel());
        modelMap.put("OkapiBM25", new OkapiBM25());
    }

    public static RetrievalModel getModel(String modelName) {
        RetrievalModel model = modelMap.get(modelName);
    
        if (model == null) {
            System.err.println("Unsupported encoder type: " + modelName + ".\n" +
                               "Defaulting to Vector Space Model");

            model = modelMap.get("VSM");
        }

        return model;
    }
}
