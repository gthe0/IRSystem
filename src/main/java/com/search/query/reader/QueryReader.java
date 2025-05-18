package com.search.query.reader;

import com.search.common.token.SimpleTokenStream;
import com.search.common.token.TokenStream;
import com.search.common.utils.StopWordManager;
import com.search.query.model.Query;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class QueryReader {

    // Read queries from file (one query per line)
    public List<Query> readFromFile(File file) throws IOException {
        List<Query> queries = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                processLine(line, queries);
            }
        }
        return queries;
    }

    // Read interactive input from command line
    public List<Query> readFromConsole() {
        List<Query> queries = new ArrayList<>();
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("Enter queries (Type "+ "s" +" to stop):");
            int queryCount = 1;
            while (true) {
                System.out.print("Query #" + queryCount + ": ");
                String input = scanner.nextLine().trim();
                if (input.equalsIgnoreCase("s")) break;
                processLine(input, queries);
                queryCount++;
            }
        }
        return queries;
    }

    // Read from a single string (one query)
    public Query readFromString(String queryText) {
        List<String> tokens = processLine(queryText);
        return !tokens.isEmpty() ? new Query(1, queryText, tokens) : null;
    }

    // Core processing logic using TokenStream
    private void processLine(String line, List<Query> queries) {
        List<String> tokens = processLine(line);
        if (!tokens.isEmpty()) {
            queries.add(new Query(queries.size() + 1, line ,tokens));
        }
    }

    private List<String> processLine(String line) {
        List<String> tokens = new ArrayList<>();
        line = line.trim();
        if (line.isEmpty()) return tokens;

        try (TokenStream ts = new SimpleTokenStream(line, StopWordManager.getStopWords())) {
            String token;
            while ((token = ts.getNext()) != null) {
                tokens.add(token);
            }
        } catch (IOException e) {
            System.err.println("Error processing query: " + e.getMessage());
        }
        return tokens;
    }

    // Batch processing from multiple strings
    public List<Query> readFromList(List<String> queryTexts) {
        List<Query> queries = new ArrayList<>();
        for (String text : queryTexts) {
            List<String> tokens = processLine(text);
            if (!tokens.isEmpty()) {
                queries.add(new Query(queries.size() + 1, text , tokens));
            }
        }
        return queries;
    }
}