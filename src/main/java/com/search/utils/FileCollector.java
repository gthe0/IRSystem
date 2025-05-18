package com.search.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class FileCollector {

    public static List<FileTriple> collectFileTriples(String vocDir, String postingDir, String docDir) {
        List<FileTriple> FileTriples = new ArrayList<>();

        // Get all vocabulary files and sort them by batch number
        File[] vocFiles = new File(vocDir).listFiles((dir, name) -> 
            name.startsWith("VocabularyFile_Batch_") && name.endsWith(".txt"));
        
        // Get all posting files and sort them by batch number
        File[] postFiles = new File(postingDir).listFiles((dir, name) -> 
            name.startsWith("PostingFile_Batch_") && name.endsWith(".txt"));

        // Get all posting files and sort them by batch number
        File[] docFiles = new File(docDir).listFiles((dir, name) -> 
            name.startsWith("DocumentFile_Batch_") && name.endsWith(".txt"));

        // Sort files by batch number to ensure correct pairing
        if (vocFiles != null && postFiles != null) {
            Arrays.sort(vocFiles, new BatchNumberComparator());
            Arrays.sort(postFiles, new BatchNumberComparator());
            Arrays.sort(docFiles, new BatchNumberComparator());

            // Pair them up
            for (int i = 0; i < Math.min(vocFiles.length, postFiles.length); i++) {
                FileTriples.add(new FileTriple( vocFiles[i].getAbsolutePath(), 
                                                postFiles[i].getAbsolutePath(),
                                                docFiles[i].getAbsolutePath()));
            }
        }

        return FileTriples;
    }

    // Helper class to represent a vocabulary/posting file pair
    public static class FileTriple {
        public final String vocabularyFilePath;
        public final String postingFilePath;
        public final String docFilePath;

        public FileTriple(String vocabularyFilePath, String postingFilePath, String docFilePath) {
            this.vocabularyFilePath = vocabularyFilePath;
            this.postingFilePath = postingFilePath;
            this.docFilePath = docFilePath;
        }
    }

    // Comparator to sort files by their batch number
    private static class BatchNumberComparator implements Comparator<File> {
        @Override
        public int compare(File f1, File f2) {
            int batch1 = extractBatchNumber(f1.getName());
            int batch2 = extractBatchNumber(f2.getName());
            return Integer.compare(batch1, batch2);
        }

        private int extractBatchNumber(String filename) {
            try {
                // Extract the number between "Batch_" and ".txt"
                String numberPart = filename.substring(filename.indexOf("Batch_") + 6, filename.lastIndexOf(".txt"));
                return Integer.parseInt(numberPart);
            } catch (Exception e) {
                return 0; // Default if parsing fails
            }
        }
    }
}