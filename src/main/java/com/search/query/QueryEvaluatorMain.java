package com.search.query;

import com.search.common.utils.FileManager;
import com.search.common.utils.MemoryMonitor;
import com.search.query.evaluation.QueryEvaluator;
import com.search.query.evaluation.VectorSpaceModel;
import com.search.query.model.Query;
import com.search.query.reader.QueryReader;
import java.io.File;
import java.util.List;
import java.util.Map;

public class QueryEvaluatorMain {
    private static QueryEvaluator evaluator;

    public static void main() {
        try {
            // Get collection index path
            MemoryMonitor memMonitor = new MemoryMonitor("Query Memory Estimator");
            String collectionPath = getCollectionPath();
            
            // Initialize components
            evaluator = new QueryEvaluator(collectionPath, new VectorSpaceModel());

            memMonitor.printStats();
            memMonitor.printUsage();

            // Process queries
            List<Query> queries = getQueries();
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

    private static String getCollectionPath() throws Exception {
        System.out.println("Select the Collection Index directory:");
        File dir = FileManager.showFileChooserForDirectory();
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
            File file = FileManager.showFileChooserForFile();
            
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
        
        for (Query query : queries) {
            System.out.println("\nProcessing query: " + query.getQuery());
            
            Map<Long, Double> results = evaluator.evaluate(query);
            System.out.println("\nTop " + MAX_RESULTS + " results:");
            evaluator.printResults(results, MAX_RESULTS);
            
            System.out.println("\n" + "-".repeat(80));
        }
    }
}