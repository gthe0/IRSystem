package com.search.indexer.utils;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;
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
        totalDocuments = countDocuments();
        processInvertedIndexParallel();
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
        long fileSize = Files.size(postingsFile);
        
        // Use multiple buffers for files >2GB
        List<ByteBuffer> buffers = new ArrayList<>();
        try (FileChannel channel = FileChannel.open(postingsFile, StandardOpenOption.READ)) {
            long offset = 0;
            while (offset < fileSize) {
                long chunkSize = Math.min(Integer.MAX_VALUE, fileSize - offset);
                MappedByteBuffer buf = channel.map(FileChannel.MapMode.READ_ONLY, offset, chunkSize);
                buffers.add(buf);
                offset += chunkSize;
            }
        }
        
        int numThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        
        try (BufferedReader vocabReader = Files.newBufferedReader(vocabFile)) {
            String line;
            while ((line = vocabReader.readLine()) != null) {
                final String vocabLine = line;
                executor.execute(() -> {
                    try {
                        processVocabEntry(buffers, vocabLine);
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                });
            }
        } finally {
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.HOURS);
        }
    }

    private void processVocabEntry(List<ByteBuffer> buffers, String vocabLine) {
        String[] parts = vocabLine.split(" ");
        int df = Integer.parseInt(parts[1]);
        long pointer = Long.parseLong(parts[2]);
        double idf = Math.log(totalDocuments / (double) df);
        
        // Get buffer and position for this pointer
        int bufIndex = (int) (pointer / Integer.MAX_VALUE);
        int bufOffset = (int) (pointer % Integer.MAX_VALUE);
        ByteBuffer buffer = buffers.get(bufIndex).duplicate();
        buffer.position(bufOffset);
        
        for (int i = 0; i < df; i++) {
            // Parse docID (read until space)
            long docId = 0;
            byte b;
            while (buffer.hasRemaining() && (b = buffer.get()) != ' ') {
                if (b >= '0' && b <= '9') {
                    docId = docId * 10 + (b - '0');
                }
            }
            
            // Parse term frequency (read until space or newline)
            int tf = 0;
            while (buffer.hasRemaining()) {
                b = buffer.get();
                if (b == ' ' || b == '\n') break;
                if (b >= '0' && b <= '9') {
                    tf = tf * 10 + (b - '0');
                }
            }
            
            // Skip rest of line
            while (buffer.hasRemaining() && buffer.get() != '\n') {}
            
            // Update norm contribution
            double contribution = Math.pow(tf * idf, 2);
            vectorNorms.merge(docId, contribution, Double::sum);
            
            // Handle buffer boundaries
            if (!buffer.hasRemaining() && bufIndex < buffers.size() - 1) {
                bufIndex++;
                buffer = buffers.get(bufIndex).duplicate();
            }
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
                long docMaxFreq = getDocMaxFreq(line);

                double norm = docMaxFreq != 0 ?  Math.sqrt(vectorNorms.getOrDefault(docId, 0.0))/docMaxFreq : 0.0;
                writer.write(line + " " + norm + "\n");
            }
        }
        
        Files.move(tempFile, docFile, StandardCopyOption.REPLACE_EXISTING);
    }

    private long getDocId(String documentLine) {
        return Long.parseLong(documentLine.split(" ")[0]);
    }

    private long getDocMaxFreq(String documentLine) {
        return Long.parseLong(documentLine.split(" ")[2]);
    }
}