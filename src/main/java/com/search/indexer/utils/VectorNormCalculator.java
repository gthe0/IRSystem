package com.search.indexer.utils;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class VectorNormCalculator {
    private final Path resultDir;
    private final Map<String, Double> vectorNorms = new HashMap<>();
    private final List<String> documentEntries = new ArrayList<>();
    private long totalDocuments;

    public VectorNormCalculator(String resultDirPath) {
        this.resultDir = Paths.get(resultDirPath);
    }

    public void calculateAndUpdateNorms() throws IOException {
        // 1. Read document file and initialize data
        loadDocumentFile();
        
        // 2. Process vocabulary and postings
        processInvertedIndex();

        // 3. Calculate final norms and update file
        writeUpdatedDocumentFile();
    }

    private void loadDocumentFile() throws IOException {
        Path docFile = resultDir.resolve("DocumentFile.txt");
        try (BufferedReader reader = new BufferedReader(new FileReader(docFile.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                documentEntries.add(line);
                vectorNorms.put(getDocId(line), 0.0);
            }
            totalDocuments = documentEntries.size();
        }
    }

    private void processInvertedIndex() throws IOException {
        Path vocabFile = resultDir.resolve("VocabularyFile.txt");
        Path postingsFile = resultDir.resolve("PostingFile.txt");

        try (BufferedReader vocabReader = new BufferedReader(new FileReader(vocabFile.toFile()));
             RandomAccessFile postingsRAF = new RandomAccessFile(postingsFile.toFile(), "r")) {
            
            String vocabLine;
            while ((vocabLine = vocabReader.readLine()) != null) {
                String[] parts = vocabLine.split(" ");
                int df = Integer.parseInt(parts[1]);
                long pointer = Long.parseLong(parts[2]);

                double idf = Math.log(totalDocuments / (double) df);
                
                processPostings(postingsRAF, pointer, df, idf);
            }
        }
    }

    private void processPostings(RandomAccessFile postingsRAF, long pointer, int df, double idf) throws IOException {
        postingsRAF.seek(pointer);
        for (int i = 0; i < df; i++) {
            String postingLine = postingsRAF.readLine();
            if (postingLine == null) break;

            String[] parts = postingLine.split(" ", 3);
            String docId = parts[0];
            int tf = Integer.parseInt(parts[1]);

            double tfidf = tf * idf;
            vectorNorms.compute(docId, (k, v) -> v + Math.pow(tfidf, 2));
        }
    }

    private void writeUpdatedDocumentFile() throws IOException {
        Path tempFile = resultDir.resolve("DocumentFile.tmp");
        Path finalFile = resultDir.resolve("DocumentFile.txt");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile.toFile()))) {
            for (String entry : documentEntries) {
                String docId = getDocId(entry);
                double norm = Math.sqrt(vectorNorms.get(docId));
                writer.write(entry + " " + norm + "\n");
            }
        }

        // Atomic replace
        FileUtils.replaceFile(tempFile, finalFile);
    }

    private String getDocId(String documentLine) {
        return documentLine.split(" ")[0];
    }

    // File replacement utility
    private static class FileUtils {
        static void replaceFile(Path temp, Path target) throws IOException {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}