package com.search.query;

import com.search.common.utils.FileManager;
import com.search.common.utils.MemoryMonitor;
import com.search.query.evaluation.QueryEvaluator;
import com.search.query.evaluation.IRetrievalModelFactory;
import com.search.query.model.Query;
import com.search.query.reader.QueryReader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class QueryEvaluatorMain {
    private static QueryEvaluator evaluator;

    public static void main(String[] args) {
        try {
            // Get collection index path
            MemoryMonitor memMonitor = new MemoryMonitor("Query Memory Estimator");
            String collectionPath = getCollectionPath();
            
            // Initialize components
            evaluator = new QueryEvaluator(collectionPath, IRetrievalModelFactory.getModel("OkapiBM25"));

            memMonitor.printStats();
            memMonitor.printUsage();

            // Process queries
            List<Query> queries = getQueries();
            processQueries(queries);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } 
    }

    private static String getCollectionPath() throws Exception {
        System.out.println("Select the Collection Index directory:");
        File dir = FileManager.showFileChooserForDirectory(FileManager.RESULT_DIR);
        if (dir == null) throw new Exception("No collection index selected");
        return dir.getAbsolutePath();
    }
    
    private static List<Query> getQueries() throws Exception {
        System.out.println("Choose input method:");
        System.out.println("1. Interactive console input");
        System.out.println("2. File input");
        System.out.print("Selection: ");

        int choice = Integer.parseInt(System.console().readLine());

        if (choice == 2) {
            System.out.println("Select query file:");
            File file = FileManager.showFileChooserForFile(FileManager.RESOURCE_DIR);

            if (file == null) {
                throw new Exception("No query file selected");
            }

            if (file.getName().toLowerCase().endsWith(".xml")) {
                // XML file detected - ask for source selection
                System.out.println("Choose query source:");
                System.out.println("1. Summary");
                System.out.println("2. Description");
                System.out.print("Selection: ");
                int sourceChoice = Integer.parseInt(System.console().readLine());

                boolean source = (sourceChoice == 1) 
                ? true 
                : false;

                return QueryReader.createForXMLFile(file, source).read();
            } else {
                return QueryReader.createForTextFile(file).read();
            }
        } else {
            return QueryReader.createForTerminal().read();
        }
    }

    private static void processQueries(List<Query> queries) {
        final int MAX_RESULTS = 1000;
        final String MODEL_NAME = evaluator.getModelName();
        final String OUTPUT_FILE = FileManager.RESULT_DIR + "query_results.tsv";
        FileManager.ensureDirectoryExists(FileManager.RESULT_DIR);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(OUTPUT_FILE))) {
            writer.write("QUERY_ID\tPLACE_HOLDER\tDOC_ID\tRANK\tSCORE\tMODEL_USED\n");

            for (Query query : queries) {
                System.out.println("\nProcessing query (ID: " + query.getId() + "): " + query.getQuery());

                long startTime = System.currentTimeMillis();
                Map<Long, Double> results = evaluator.evaluate(query);
                long duration = System.currentTimeMillis() - startTime;

                System.out.println("Evaluation took: " + duration/1000.0 + " seconds");
                System.out.println("Writing results to: " + OUTPUT_FILE);

                // Sort results by score descending
                List<Map.Entry<Long, Double>> sortedResults = new ArrayList<>(results.entrySet());
                sortedResults.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

                int rank = 0;
                double prevScore = Double.NaN;
                int count = 0; // Tracks total results written

                for (int i = 0; i < sortedResults.size() && count < MAX_RESULTS; i++) {
                    Map.Entry<Long, Double> entry = sortedResults.get(i);
                    double currentScore = entry.getValue();

                    // Update rank when score changes or at first entry
                    if (i == 0 || Double.compare(currentScore, prevScore) != 0) {
                        rank = i + 1; 
                    }
    
                    writer.write(String.format("%s\t0\t%d\t%d\t%.6f\t%s%n",
                        query.getId(),
                        entry.getKey(),
                        rank,
                        currentScore,
                        MODEL_NAME));

                    prevScore = currentScore;
                    count++; 
                }
                writer.flush();
            }
        } catch (IOException e) {
            System.err.println("Error writing results: " + e.getMessage());
        }
    }
}
