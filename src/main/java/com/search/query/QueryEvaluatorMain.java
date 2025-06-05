package com.search.query;

import com.search.common.document.Document;
import com.search.common.document.DocumentFactory;
import com.search.common.token.SimpleTokenStream;
import com.search.common.token.TokenStream;
import com.search.common.utils.FileManager;
import com.search.common.utils.MemoryMonitor;
import com.search.common.utils.StopWordManager;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class QueryEvaluatorMain {
    private static QueryEvaluator evaluator;

    public static void main(String[] args) {
        try {
            // Get collection index path
            MemoryMonitor memMonitor = new MemoryMonitor("Query Memory Estimator");
            String collectionPath = getCollectionPath();

            File file = FileManager.showFileChooserForDirectory(FileManager.RESOURCE_DIR);
            StopWordManager.loadStopWords(file);
            
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

                System.out.println("Writing results to: " + OUTPUT_FILE);

                // Sort results by score descending
                List<Map.Entry<Long, Double>> sortedResults = new ArrayList<>(results.entrySet());
                sortedResults.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

                Map<Long, String> documentPaths = evaluator.getEvaluationContext().getDocumentPaths();
                List<Document>    documentList  = new ArrayList<>();

                for (int i = 0; i < sortedResults.size() && i < 10; i++) {
                    documentList.add(DocumentFactory.createDocument(new File(documentPaths.get(sortedResults.get(i).getKey()))));
                }

                Map<String, Integer> termFrequencies = new HashMap<>();
                for (Document doc : documentList) {
                    for (Map.Entry<String, Integer> entry : doc.getTf().entrySet()) {
                        termFrequencies.put(entry.getKey(), termFrequencies.getOrDefault(entry.getKey(), 0) + entry.getValue());
                    }
                }

                List<Map.Entry<String, Integer>> sortedTerms = new ArrayList<>(termFrequencies.entrySet());
                sortedTerms.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
                
                String newQuery = query.getQuery() + " " + sortedTerms.stream()
                                        .limit(10)
                                        .map(Map.Entry::getKey) 
                                        .collect(Collectors.joining(" ")); 


                System.out.println(newQuery);
                List<String> queryList = process(newQuery);
                query = new Query(query.getId(),newQuery,queryList);
                results = evaluator.evaluate(query);

                long duration = System.currentTimeMillis() - startTime;
                System.out.println("Evaluation took: " + duration/1000.0 + " seconds");

                sortedResults = new ArrayList<>(results.entrySet());
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

    private static  List<String> process(String text) {
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
