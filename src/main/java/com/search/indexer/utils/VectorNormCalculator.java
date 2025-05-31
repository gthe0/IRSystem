package com.search.indexer.utils;

import java.io.*;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.Map;

public class VectorNormCalculator {
    private final Path resultDir;
    private final Map<Long, Double> vectorNorms;
    private long totalDocuments;

    public VectorNormCalculator(String resultDirPath) {
        this.resultDir = Paths.get(resultDirPath);
        this.vectorNorms = new ConcurrentHashMap<>();
    }

    public void calculateAndUpdateNorms() throws IOException, InterruptedException {
        // 1. Count documents
        totalDocuments = countDocuments();
        
        // 2. Process with thread-local storage
        processInvertedIndexParallel();
        
        // 3. Update document file
        writeUpdatedDocumentFile();
    }

    private long countDocuments() throws IOException {
        Path docFile = resultDir.resolve("DocumentFile.txt");
        try (BufferedReader reader = Files.newBufferedReader(docFile)) {
            return reader.lines().count();
        }
    }

    private void processInvertedIndexParallel() throws IOException, InterruptedException {
        final Path vocabFile = resultDir.resolve("VocabularyFile.txt");
        final Path postingsFile = resultDir.resolve("PostingFile.txt");
        
        // Create worker threads
        int numThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        
        try (BufferedReader vocabReader = Files.newBufferedReader(vocabFile)) {
            // Process vocabulary line-by-line
            String line;
            while ((line = vocabReader.readLine()) != null) {
                final String vocabLine = line;
                executor.execute(() -> {
                    try (RandomAccessFile raf = new RandomAccessFile(postingsFile.toFile(), "r")) {
                        processVocabEntry(raf, vocabLine);
                    } catch (IOException e) {
                        throw new CompletionException(e);
                    }
                });
            }
        } finally {
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.HOURS);
        }
    }

    private void processVocabEntry(RandomAccessFile raf, String vocabLine) throws IOException {
        String[] parts = vocabLine.split(" ");
        int df = Integer.parseInt(parts[1]);
        long pointer = Long.parseLong(parts[2]);
        double idf = Math.log(totalDocuments / (double) df);
        
        raf.seek(pointer);
        for (int i = 0; i < df; i++) {
            String postingLine = raf.readLine();
            if (postingLine == null) break;
            
            String[] tokens = postingLine.split(" ", 3);
            long docId = Long.parseLong(tokens[0]);
            int tf = Integer.parseInt(tokens[1]);
            
            double contribution = Math.pow(tf * idf, 2);
            vectorNorms.merge(docId, contribution, Double::sum);
        }
    }

    private void writeUpdatedDocumentFile() throws IOException {
        Path docFile = resultDir.resolve("DocumentFile.txt");
        Path tempFile = resultDir.resolve("DocumentFile.tmp");
        
        try (BufferedReader reader = Files.newBufferedReader(docFile);
             BufferedWriter writer = Files.newBufferedWriter(tempFile)) {
             
            String line;
            while ((line = reader.readLine()) != null) {
                long docId = getDocId(line);
                double norm = Math.sqrt(vectorNorms.getOrDefault(docId, 0.0));
                writer.write(line + " " + norm + "\n");
            }
        }
        
        Files.move(tempFile, docFile, StandardCopyOption.REPLACE_EXISTING);
    }

    private long getDocId(String documentLine) {
        return Long.parseLong(documentLine.split(" ")[0]);
    }
}