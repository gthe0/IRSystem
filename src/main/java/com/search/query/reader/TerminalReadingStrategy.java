package com.search.query.reader;

import com.search.query.model.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class TerminalReadingStrategy implements QueryReadingStrategy {
    @Override
    public List<Query> readQueries() throws Exception {
        List<Query> queries = new ArrayList<>();
        try (Scanner scanner = new Scanner(System.in, "UTF-8")) {
            System.out.println("Enter queries (Press \"s\" to stop):");
            int queryCount = 1;
            while (true) {
                System.out.print("Query #" + queryCount + ": ");
                String input = scanner.nextLine().trim();
                if (input.equalsIgnoreCase("s")) break;
                
                List<String> tokens = process(input);
                if (!tokens.isEmpty()) {
                    queries.add(new Query(queryCount, input, tokens));
                    queryCount++;
                }
            }
        }
        return queries;
    }
}