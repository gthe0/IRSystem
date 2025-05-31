package com.search.query;

import com.search.common.utils.FileManager;
import com.search.query.evaluation.QueryEvaluator;
import com.search.query.model.Query;
import com.search.query.reader.QueryReader;
import java.io.File;
import java.util.List;
import java.util.Map;

public class QueryEvaluatorMain {
    private static QueryEvaluator evaluator;
    private static QueryReader queryReader;

    public static void main(String[] args) {
        try {
            // Get collection index path
            String collectionPath = getCollectionPath(args);
            
            // Initialize components
            evaluator = new QueryEvaluator(collectionPath);
            queryReader = new QueryReader();

            // Process queries
            List<Query> queries = getQueries(args);
            processQueries(queries);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (evaluator != null) {
                evaluator.close();
            }
        }
    }

    private static String getCollectionPath(String[] args) throws Exception {
        if (args.length > 0) {
            return args[0];
        }
        System.out.println("Select the Collection Index directory:");
        File dir = FileManager.showFileChooserForDirectory();
        if (dir == null) throw new Exception("No collection index selected");
        return dir.getAbsolutePath();
    }

    private static List<Query> getQueries(String[] args) throws Exception {
        if (args.length > 1) {
            // Read queries from file
            File queryFile = new File(args[1]);
            return queryReader.readFromFile(queryFile);
        }
        
        // Choose input mode
        System.out.println("Choose input method:");
        System.out.println("1. Interactive console input");
        System.out.println("2. File input");
        System.out.print("Selection: ");
        
        int choice = Integer.parseInt(System.console().readLine());
        if (choice == 2) {
            System.out.println("Select query file:");
            File file = FileManager.showFileChooserForFile();
            return queryReader.readFromFile(file);
        }
        
        // Default to console input
        return queryReader.readFromConsole();
    }

    private static void processQueries(List<Query> queries) {
        final int MAX_RESULTS = 1000;
        
        for (Query query : queries) {
            System.out.println("\nProcessing query: " + query.getQuery());
            
            Map<Long, Double> results = evaluator.evaluate(query);
            System.out.println("\nTop " + MAX_RESULTS + " results:");
            evaluator.printResults(results, MAX_RESULTS);
            
            System.out.println("\n" + "-".repeat(80));
        }
    }
}