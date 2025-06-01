package com.search.query.reader;

import com.search.common.token.SimpleTokenStream;
import com.search.common.token.TokenStream;
import com.search.common.utils.StopWordManager;
import com.search.query.model.Query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public interface QueryReadingStrategy {
    List<Query> readQueries() throws Exception;

    default List<String> process(String text) {
        List<String> tokens = new ArrayList<>();
        text = text.trim();
        if (text.isEmpty()) return tokens;

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