package com.search.evaluator;

import com.search.common.utils.FileManager;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class IREvaluatorMain {

    public static void main(String[] args) {

        String evalDirectory = FileManager.RESULT_DIR + File.separator + "evals"; 
        FileManager.ensureDirectoryExists(evalDirectory);
        
        try {
            // Select qrels file
            JOptionPane.showMessageDialog(null, "Select qrels file (relevance judgments)", "File Selection", JOptionPane.INFORMATION_MESSAGE);
            File qrelsFile = FileManager.showFileChooserForFile(FileManager.RESOURCE_DIR);
            if (qrelsFile == null) {
                JOptionPane.showMessageDialog(null, "No qrels file selected. Exiting.");
                return;
            }

            // Select result files
            List<File> resultFiles = new ArrayList<>();
            int selection = JOptionPane.YES_OPTION;
            
            while (selection == JOptionPane.YES_OPTION) {
                JOptionPane.showMessageDialog(null, "Select a result file", "File Selection", JOptionPane.INFORMATION_MESSAGE);
                File resultFile = FileManager.showFileChooserForFile(FileManager.RESULT_DIR);
                
                if (resultFile != null) {
                    resultFiles.add(resultFile);
                    selection = JOptionPane.showConfirmDialog(null, 
                            "Add another result file?", 
                            "More Files", 
                            JOptionPane.YES_NO_OPTION);
                } else {
                    break;
                }
            }

            if (resultFiles.isEmpty()) {
                JOptionPane.showMessageDialog(null, "No result files selected. Exiting.");
                return;
            }

            // Initialize evaluator
            ResultEvaluator evaluator = new ResultEvaluator(qrelsFile.getAbsolutePath());

            // Process each result file separately
            for (File resultFile : resultFiles) {
                String modelName = extractModelName(resultFile);
                if (modelName == null || modelName.isEmpty()) {
                    modelName = "UnknownModel";
                }                
                
                String outputFilePath = evalDirectory + File.separator + "eval_results_" + modelName + ".txt";

                evaluator.evaluate(List.of(resultFile.getAbsolutePath()), outputFilePath);

                JOptionPane.showMessageDialog(null, 
                        "Evaluation complete!\nResults saved to: " + outputFilePath, 
                        "Success", 
                        JOptionPane.INFORMATION_MESSAGE);
            }
            
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, 
                    "Error during evaluation: " + e.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private static String extractModelName(File file) throws IOException {
        List<String> lines = Files.readAllLines(file.toPath());
        for (String line : lines) {
            if (!line.startsWith("QUERY_ID")) { 
                String[] parts = line.split("\\t"); 
                if (parts.length >= 6) {
                    return parts[5]; 
                }
            }
        }
        return null;
    }
}
