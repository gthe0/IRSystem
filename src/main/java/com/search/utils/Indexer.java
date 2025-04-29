package com.search.utils;

import java.io.*;
import java.util.*;

import com.search.index.Corpus;
import com.search.index.Vocabulary;

public class Indexer {

    private static final String batchFilePath = FileManager.RESULT_DIR + "CollectionIndex/";

    public static void indexDocuments(Corpus corpus, int batchNumber) throws IOException {
        String batchFile = batchFilePath + "/VocabularyFile_Batch_" + batchNumber + ".txt";
        FileManager.ensureDirectoryExists(batchFilePath);
        exportVocabulary(corpus.getVocabulary(), batchFilePath);

        System.out.println("Vocabulary for batch " + batchNumber + " exported to: " + batchFile);
    }

    // Export the vocabulary to a file
    private static void exportVocabulary(Vocabulary vocabulary, String filePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), "UTF-8"))) {
            for (String term : vocabulary.getSortedTerms()) {
                int documentFrequency = vocabulary.getDocumentFrequency(term);
                writer.write(term + " " + documentFrequency + "\n");
            }
        }
    }

    // Merge all Vocabulary Files into a single file
    public static void mergeVocabularyFiles() throws IOException {

        FileManager.ensureDirectoryExists(batchFilePath);
        String mergedFilePath = batchFilePath + "/VocabularyFile.txt";

        File[] batchFiles = new File(batchFilePath).listFiles((dir, name) -> name.startsWith("VocabularyFile_Batch_") && name.endsWith(".txt"));
        Map<String, Integer> mergedVocabulary = new TreeMap<>(); // Use TreeMap for lexicographic ordering

        // Read each batch file and merge the terms
        if (batchFiles != null) {
            for (File batchFile : batchFiles) {
                try (BufferedReader reader = new BufferedReader(new FileReader(batchFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split(" ");
                        String term = parts[0];
                        int df = Integer.parseInt(parts[1]);

                        mergedVocabulary.put(term, mergedVocabulary.getOrDefault(term, 0) + df);
                    }
                }
            }
        }

        // Write the merged vocabulary to the final file
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(mergedFilePath), "UTF-8"))) {
            for (Map.Entry<String, Integer> entry : mergedVocabulary.entrySet()) {
                writer.write(entry.getKey() + " " + entry.getValue() + "\n");
            }
        }

        System.out.println("Merged Vocabulary File created at: " + mergedFilePath);
    }
}
