package com.search.query.reader;

import com.search.query.model.Query;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class TextFileReadingStrategy implements QueryReadingStrategy {
    private final File file;
    
    public TextFileReadingStrategy(File file) {
        this.file = file;
    }

    @Override
    public List<Query> readQueries() throws Exception {
        List<Query> queries = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                List<String> tokens = process(line);
                if (!tokens.isEmpty()) {
                    queries.add(new Query(queries.size() + 1, line, tokens));
                }
            }
        }
        return queries;
    }
}