package com.search.query;

import com.search.common.utils.FileManager;
import com.search.common.utils.StopWordManager;
import com.search.query.evaluation.QueryEvaluator;
import com.search.query.expansion.QueryExpander;
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

import javax.swing.JOptionPane;

public class QueryEvaluatorMain {
    private static QueryEvaluator evaluator;

    public static void main(String[] args) {
        try {
            String collectionPath = getCollectionPath();

            // load stopwords
            JOptionPane.showMessageDialog(null, "Select the StopWords directory", "Stop Words Path", JOptionPane.INFORMATION_MESSAGE);
            File file = FileManager.showFileChooserForDirectory(FileManager.RESOURCE_DIR);
            StopWordManager.loadStopWords(file);

            String[] options = {"OkapiBM25", "VSM"};
            int choice = JOptionPane.showOptionDialog(
                null,
                "Choose Retrieval model:",
                "Retrieval Model",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
            );

            evaluator = new QueryEvaluator(collectionPath, IRetrievalModelFactory.getModel(options[choice]));

            // Process queries
            List<Query> queries = getQueries();
            processQueries(queries);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private static String getCollectionPath() throws Exception {
        JOptionPane.showMessageDialog(null, "Select the Collection Index directory", "Collection Path", JOptionPane.INFORMATION_MESSAGE);
        FileManager.ensureDirectoryExists(FileManager.RESULT_DIR);
        File dir = FileManager.showFileChooserForDirectory(FileManager.RESULT_DIR);
        if (dir == null) throw new Exception("No collection index selected");
        return dir.getAbsolutePath();
    }

    private static List<Query> getQueries() throws Exception {
        // Choose input method via pop-up dialog
        String[] options = {"Interactive console input", "File input"};
        int choice = JOptionPane.showOptionDialog(
                null,
                "Choose input method:",
                "Query Input",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );

        if (choice == 1) {
            // Select query file via file chooser pop-up
            JOptionPane.showMessageDialog(null, "Select query file", "File Selection", JOptionPane.INFORMATION_MESSAGE);
            File file = FileManager.showFileChooserForFile(FileManager.RESOURCE_DIR);

            if (file == null) throw new Exception("No query file selected");

            if (file.getName().toLowerCase().endsWith(".xml")) {
                // Ask for source selection via pop-up dialog
                String[] sourceOptions = {"Summary", "Description"};
                int sourceChoice = JOptionPane.showOptionDialog(
                        null,
                        "Choose query source:",
                        "Query Source",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        sourceOptions,
                        sourceOptions[0]
                );

                boolean source = (sourceChoice == 0); // Summary selected if 0
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

        final String EVALS_FILE  = FileManager.RESULT_DIR +File.separator + "retrieval";
        final String OUTPUT_FILE = EVALS_FILE + File.separator + "query_results_" + MODEL_NAME + ".tsv";
        FileManager.ensureDirectoryExists(EVALS_FILE);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(OUTPUT_FILE))) {
            writer.write("QUERY_ID\tPLACE_HOLDER\tDOC_ID\tRANK\tSCORE\tMODEL_USED\n");

            for (Query query : queries) {
                System.out.println("\nProcessing query (ID: " + query.getId() + "): " + query.getQuery());

                long startTime = System.currentTimeMillis();
                Map<Long, Double> results = evaluator.evaluate(query);

                query = QueryExpander.expand(query, evaluator.getEvaluationContext(), results);
                results = evaluator.evaluate(query);
                
                long duration = System.currentTimeMillis() - startTime;
                System.out.println("Evaluation took: " + duration/1000.0 + " seconds");
                System.out.println("Writing results to: " + OUTPUT_FILE);

                // Sort results by score descending
                List<Map.Entry<Long, Double>> sortedResults = new ArrayList<>(results.entrySet());
                sortedResults.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

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
}
