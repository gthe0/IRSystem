package com.search.indexer.utils;

import java.io.*;
import java.util.*;

public class FileMerger {
    private static class TermEntry implements Comparable<TermEntry> {
        String term;
        int batchDf; // DF from this specific batch
        long pointer;
        BufferedReader vocabReader;
        String postingFilePath;

        public TermEntry(String term, int batchDf, long pointer, BufferedReader vocabReader, String postingFilePath) {
            this.term = term;
            this.batchDf = batchDf;
            this.pointer = pointer;
            this.vocabReader = vocabReader;
            this.postingFilePath = postingFilePath;
        }

        @Override
        public int compareTo(TermEntry other) {
            return this.term.compareTo(other.term);
        }
    }
    public static void merge(List<String> vocabFiles, List<String> postingFiles, List<String> documentFiles, String resultPath) throws IOException {
        mergeVocAndPost(vocabFiles, postingFiles, resultPath);        
        mergeDocumentFiles(documentFiles, resultPath);
    }

    public static void mergeVocAndPost(List<String> vocabFiles, List<String> postingFiles, String resultPath) throws IOException {
        String mergedVocabPath = resultPath + File.separator + "VocabularyFile.txt";
        String mergedPostingPath = resultPath + File.separator + "PostingFile.txt";

        PriorityQueue<TermEntry> queue = new PriorityQueue<>();
        Map<String, RandomAccessFile> postingFileHandles = new HashMap<>();

        // Open all posting files
        for (String postingFile : postingFiles) {
            postingFileHandles.put(postingFile, new RandomAccessFile(postingFile, "r"));
        }

        // Initialize queue with first term from each vocab file
        for (int i = 0; i < vocabFiles.size(); i++) {
            String vocabFile = vocabFiles.get(i);
            String postingFile = postingFiles.get(i);
            BufferedReader vocabReader = new BufferedReader(new FileReader(vocabFile));
            String line = vocabReader.readLine();
            if (line != null) {
                String[] parts = line.split(" ");
                String term = parts[0];
                int df = Integer.parseInt(parts[1]);
                long pointer = Long.parseLong(parts[2]);
                queue.add(new TermEntry(term, df, pointer, vocabReader, postingFile));
            }
        }

        try (BufferedWriter mergedVocabWriter = new BufferedWriter(new FileWriter(mergedVocabPath));
             BufferedWriter mergedPostingWriter = new BufferedWriter(new FileWriter(mergedPostingPath))) {

            long currentPostingPointer = 0;

            while (!queue.isEmpty()) {
                TermEntry current = queue.poll();
                String currentTerm = current.term;
                int totalDf = current.batchDf;
                List<TermEntry> batches = new ArrayList<>();
                batches.add(current);

                // Collect all entries for the current term
                while (!queue.isEmpty() && queue.peek().term.equals(currentTerm)) {
                    TermEntry duplicate = queue.poll();
                    totalDf += duplicate.batchDf;
                    batches.add(duplicate);
                }

                // Write merged term to vocabulary
                mergedVocabWriter.write(currentTerm + " " + totalDf + " " + currentPostingPointer + "\n");

                // Process postings for all batches
                for (TermEntry batch : batches) {
                    RandomAccessFile postingFile = postingFileHandles.get(batch.postingFilePath);
                    postingFile.seek(batch.pointer);
                    for (int i = 0; i < batch.batchDf; i++) {
                        String line = postingFile.readLine();
                        if (line == null) break; // Handle EOF
                        mergedPostingWriter.write(line + "\n");
                        currentPostingPointer += line.length() + 1; // +1 for newline
                    }
                }

                // Advance readers for all processed batches
                for (TermEntry batch : batches) {
                    advanceReader(batch.vocabReader, queue, batch.postingFilePath);
                }
            }
        }

        // Close all posting files
        for (RandomAccessFile file : postingFileHandles.values()) {
            file.close();
        }
    }

    private static void advanceReader(BufferedReader reader, PriorityQueue<TermEntry> queue, String postingFilePath) throws IOException {
        String line = reader.readLine();
        if (line != null) {
            String[] parts = line.split(" ");
            String term = parts[0];
            int df = Integer.parseInt(parts[1]);
            long pointer = Long.parseLong(parts[2]);
            queue.add(new TermEntry(term, df, pointer, reader, postingFilePath));
        }
    }

    public static void mergeDocumentFiles(List<String> documentFiles, String resultPath) throws IOException {
        String mergedDocPath = resultPath + File.separator + "DocumentFile.txt";
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(mergedDocPath))) {
            for (String docFile : documentFiles) {
                appendFileContents(docFile, writer);
            }
        }
    }

    private static void appendFileContents(String filePath, BufferedWriter writer) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error processing " + filePath + ": " + e.getMessage());
        }
    }
}