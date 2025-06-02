package com.search.evaluator;

import com.search.common.utils.FileManager;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class IREvaluatorMain {

    public static void main(String[] args) {
        try {
            // Select qrels file
            System.out.println("Select qrels file (relevance judgments)");
            File qrelsFile = FileManager.showFileChooserForFile();
            if (qrelsFile == null) {
                JOptionPane.showMessageDialog(null, "No qrels file selected. Exiting.");
                return;
            }

            // Select result files
            List<File> resultFiles = new ArrayList<>();
            int selection = JOptionPane.YES_OPTION;
            
            while (selection == JOptionPane.YES_OPTION) {
                System.out.println("Select a result file");
                File resultFile = FileManager.showFileChooserForFile();
                
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

            // Convert to paths
            List<String> resultPaths = new ArrayList<>();
            for (File file : resultFiles) {
                resultPaths.add(file.getAbsolutePath());
            }

            FileManager.ensureDirectoryExists(FileManager.RESULT_DIR);

            // Run evaluation
            ResultEvaluator evaluator = new ResultEvaluator(qrelsFile.getAbsolutePath());
            evaluator.evaluate(resultPaths, FileManager.RESULT_DIR + File.separator + "eval_results.txt");
            
            JOptionPane.showMessageDialog(null, 
                    "Evaluation complete!\nResults saved to eval_results.txt", 
                    "Success", 
                    JOptionPane.INFORMATION_MESSAGE);
            
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, 
                    "Error during evaluation: " + e.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
}