package com.search.indexer.utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class VectorNormCalculator {

    private static final String MERGED_DOCUMENT_FILE = "DocumentFile_Merged.txt";
    private static final String MERGED_VOCABULARY_FILE = "VocabularyFile_Merged.txt";
    private static final String MERGED_POSTING_FILE = "PostingFile_Merged.txt";
    private static final String OUTPUT_DOCUMENT_FILE = "DocumentFile_With_VectorNorms.txt";

    public static void computeVectorNorms(String outputDir) throws IOException {
        File documentFile = new File(outputDir, MERGED_DOCUMENT_FILE);
        File vocabFile = new File(outputDir, MERGED_VOCABULARY_FILE);
        File postingFile = new File(outputDir, MERGED_POSTING_FILE);
        File outputFile = new File(outputDir, OUTPUT_DOCUMENT_FILE);

        Map<Integer, Double> docNorms = countDocumentsAndInitializeMap(documentFile);
        processVocabularyAndPostings(docNorms, vocabFile, postingFile);
        writeFinalDocumentFile(documentFile, outputFile, docNorms);
    }

    private static Map<Integer, Double> countDocumentsAndInitializeMap(File documentFile) throws IOException {
        Map<Integer, Double> docNorms = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(documentFile, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ");
                int docID = Integer.parseInt(parts[0]);
                docNorms.put(docID, 0.0); // Initialize norms to 0
            }
        }
        return docNorms;
    }

    private static void processVocabularyAndPostings(Map<Integer, Double> docNorms, File vocabFile, File postingFile) throws IOException {
        try (BufferedReader vocabReader = new BufferedReader(new FileReader(vocabFile, StandardCharsets.UTF_8));
             BufferedReader postingReader = new BufferedReader(new FileReader(postingFile, StandardCharsets.UTF_8))) {

            String vocabLine;
            while ((vocabLine = vocabReader.readLine()) != null) {
                String[] vocabParts = vocabLine.split(" ");

                int df = Integer.parseInt(vocabParts[1]);
                long pointer = Long.parseLong(vocabParts[2]);

                // Calculate IDF
                double idf = Math.log(docNorms.size() / (double) df);

                // Move to correct posting position
                postingReader.skip(pointer);

                for (int i = 0; i < df; i++) {
                    String postingLine = postingReader.readLine();
                    if (postingLine != null) {
                        String[] postingParts = postingLine.split(" ");
                        int docID = Integer.parseInt(postingParts[0]);
                        int tf = Integer.parseInt(postingParts[1]);

                        double tfIdf = tf * idf;
                        docNorms.put(docID, docNorms.get(docID) + Math.pow(tfIdf, 2));
                    }
                }
            }
        }
    }

    private static void writeFinalDocumentFile(File documentFile, File outputFile, Map<Integer, Double> docNorms) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(documentFile, StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ");
                int docID = Integer.parseInt(parts[0]);
                String docPath = parts[1];

                double vectorNorm = Math.sqrt(docNorms.get(docID));

                writer.write(docID + " " + docPath + " " + vectorNorm);
                writer.newLine();
            }
        }

        System.out.println("Vector norms computed and saved successfully!");
    }
}
