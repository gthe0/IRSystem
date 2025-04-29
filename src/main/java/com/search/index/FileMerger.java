package com.search.index;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import com.search.utils.FileManager;

public class FileMerger {

    private static final String MERGED_VOCABULARY_FILE_NAME = "VocabularyFile_Merged.txt";
    private static final String MERGED_POSTING_FILE_NAME = "PostingFile_Merged.txt";

    public void mergeFiles(String vocabDir, String postingDir, String outputDirectory) throws IOException {
        FileManager.ensureDirectoryExists(outputDirectory);

        String mergedVocabularyFilePath = outputDirectory + File.separator + MERGED_VOCABULARY_FILE_NAME;
        String mergedPostingFilePath = outputDirectory + File.separator + MERGED_POSTING_FILE_NAME;

        Map<String, MergedEntry> vocabMap = new TreeMap<>();

        // Step 1: Collect all Vocabulary and Posting files from the directories
        List<String> vocabularyFiles = getFilesInDirectory(vocabDir, "VocabularyFile_Batch_");
        List<String> postingFiles = getFilesInDirectory(postingDir, "PostingFile_Batch_");

        // Handle single file edge case for vocabulary and posting files
        if (vocabularyFiles.size() == 1 && postingFiles.size() == 1) {
            copySingleFile(vocabularyFiles.get(0), mergedVocabularyFilePath);
            copySingleFile(postingFiles.get(0), mergedPostingFilePath);
            System.out.println("Single file detected. Merging completed by copying.");
            return;
        }

        processVocabularyFiles(vocabularyFiles, vocabMap);
        mergePostingFiles(vocabMap, postingFiles, mergedPostingFilePath);
        writeMergedVocabularyFile(vocabMap, mergedVocabularyFilePath);

        System.out.println("Files merged successfully. Output:");
        System.out.println("Vocabulary File: " + mergedVocabularyFilePath);
        System.out.println("Posting File: " + mergedPostingFilePath);
    }

    private List<String> getFilesInDirectory(String directoryPath, String filePrefix) {
        File dir = new File(directoryPath);

        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("Provided path is not a directory: " + directoryPath);
        }

        List<String> filePaths = new ArrayList<>();
        File[] files = dir.listFiles((dir1, name) -> name.startsWith(filePrefix));

        if (files != null) {
            for (File file : files) {
                filePaths.add(file.getAbsolutePath());
            }
        }
        return filePaths;
    }

    private void processVocabularyFiles(List<String> vocabularyFiles, Map<String, MergedEntry> vocabMap) throws IOException {
        for (String vocabFile : vocabularyFiles) {
            try (BufferedReader reader = new BufferedReader(new FileReader(vocabFile, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(" ");
                    String term = parts[0];
                    int df = Integer.parseInt(parts[1]);
                    long pointer = Long.parseLong(parts[2]);

                    // If term exists, aggregate df; otherwise, add new entry
                    vocabMap.putIfAbsent(term, new MergedEntry(term, 0));
                    MergedEntry entry = vocabMap.get(term);
                    entry.addDf(df);
                    entry.addPointer(pointer, vocabFile); // Track pointer and source file
                }
            }
        }
    }

    private void mergePostingFiles(Map<String, MergedEntry> vocabMap, List<String> postingFiles, String mergedPostingFilePath) throws IOException {
        try (RandomAccessFile mergedPostingFile = new RandomAccessFile(mergedPostingFilePath, "rw")) {
            long pointer = 0; // Byte offset for new postings in the merged file

            for (MergedEntry entry : vocabMap.values()) {
                List<Posting> postings = new ArrayList<>();

                for (PointerData pointerData : entry.getPointers()) {
                    String postingFile = pointerData.getSourceFile();
                    long postingPointer = pointerData.getPointer();

                    try (RandomAccessFile postingReader = new RandomAccessFile(postingFile, "r")) {
                        postingReader.seek(postingPointer); // Jump to the pointer in the Posting File
                        String line;

                        while ((line = postingReader.readLine()) != null) {
                            String[] parts = line.split(" ");
                            int docId = Integer.parseInt(parts[0]);
                            int tf = Integer.parseInt(parts[1]);
                            String positions = parts[2]; // Positional information

                            postings.add(new Posting(docId, tf, positions));

                            // Stop reading if the next posting is for a different term
                            if (postingReader.getFilePointer() >= postingReader.length()) break;
                        }
                    }
                }

                postings.sort(Comparator.comparingInt(Posting::getDocId));

                for (Posting posting : postings) {
                    mergedPostingFile.writeBytes(posting.getDocId() + " " + posting.getTf() + " " + posting.getPositions() + "\n");
                }

                entry.setPointer(pointer);

                pointer = mergedPostingFile.getFilePointer();
            }
        }
    }

    private void writeMergedVocabularyFile(Map<String, MergedEntry> vocabMap, String filePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, StandardCharsets.UTF_8))) {
            for (MergedEntry entry : vocabMap.values()) {
                writer.write(entry.getTerm() + " " + entry.getDf() + " " + entry.getPointer() + "\n");
            }
        }
    }

    private void copySingleFile(String sourceFilePath, String destinationFilePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(sourceFilePath, StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new FileWriter(destinationFilePath, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.newLine();
            }
        }
    }

    // Helper class to hold vocabulary entry data during merging
    private static class MergedEntry {
        private final String term;
        private int df; // Document frequency
        private long pointer; // Pointer in the Posting File
        private final List<PointerData> pointers; // Track pointers from each batch

        public MergedEntry(String term, int df) {
            this.term = term;
            this.df = df;
            this.pointers = new ArrayList<>();
        }

        public void addDf(int df) {
            this.df += df;
        }

        public void addPointer(long pointer, String sourceFile) {
            this.pointers.add(new PointerData(pointer, sourceFile));
        }

        public void setPointer(long pointer) {
            this.pointer = pointer;
        }

        public String getTerm() {
            return term;
        }

        public int getDf() {
            return df;
        }

        public long getPointer() {
            return pointer;
        }

        public List<PointerData> getPointers() {
            return pointers;
        }
    }

    private static class PointerData {
        private final long pointer;
        private final String sourceFile;

        public PointerData(long pointer, String sourceFile) {
            this.pointer = pointer;
            this.sourceFile = sourceFile;
        }

        public long getPointer() {
            return pointer;
        }

        public String getSourceFile() {
            return sourceFile;
        }
    }

    private static class Posting {
        private final int docId;
        private final int tf;
        private final String positions;

        public Posting(int docId, int tf, String positions) {
            this.docId = docId;
            this.tf = tf;
            this.positions = positions;
        }

        public int getDocId() {
            return docId;
        }

        public int getTf() {
            return tf;
        }

        public String getPositions() {
            return positions;
        }
    }
}
