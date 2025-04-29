package com.search.index;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import com.search.utils.FileManager;

public class FileMerger {
    private static final String MERGED_POSTING_FILE_NAME = "PostingFile_Merged_";
    private static final String MERGED_VOCABULARY_FILE_NAME = "VocabularyFile_Merged_";
    
    public static void mergeFiles(String outputDir, List<String> vocabularyFilePaths, List<String> postingFilePaths) throws IOException {
        // Ensure output directories exist
        FileManager.ensureDirectoryExists(outputDir);
        
        String timestamp = String.valueOf(System.currentTimeMillis());
        String mergedPostingFilePath = outputDir + File.separator + MERGED_POSTING_FILE_NAME + timestamp + ".txt";
        String mergedVocabularyFilePath = outputDir + File.separator + MERGED_VOCABULARY_FILE_NAME + timestamp + ".txt";
        
        TreeMap<String, VocabularyEntry> mergedVocabulary = new TreeMap<>();
        List<String> mergedPostings = new ArrayList<>();
        
        for (int i = 0; i < vocabularyFilePaths.size(); i++) {
            String vocabFilePath = vocabularyFilePaths.get(i);
            String postingFilePath = postingFilePaths.get(i);
            
            Map<String, VocabularyEntry> currentVocabulary = readVocabularyFile(vocabFilePath);
            List<String> currentPostings = readPostingFile(postingFilePath);
            
            mergeData(mergedVocabulary, mergedPostings, currentVocabulary, currentPostings);
        }
        
        // Write the merged files
        writeMergedFiles(mergedVocabulary, mergedPostings, mergedVocabularyFilePath, mergedPostingFilePath);
    }
    
    private static Map<String, VocabularyEntry> readVocabularyFile(String filePath) throws IOException {
        Map<String, VocabularyEntry> vocabulary = new HashMap<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ");
                if (parts.length >= 3) {
                    String term = parts[0];
                    int df = Integer.parseInt(parts[1]);
                    long pointer = Long.parseLong(parts[2]);
                    vocabulary.put(term, new VocabularyEntry(df, pointer));
                }
            }
        }
        
        return vocabulary;
    }
    
    private static List<String> readPostingFile(String filePath) throws IOException {
        List<String> postings = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                postings.add(line);
            }
        }
        
        return postings;
    }
    
    private static void mergeData(
            TreeMap<String, VocabularyEntry> mergedVocabulary,
            List<String> mergedPostings,
            Map<String, VocabularyEntry> currentVocabulary,
            List<String> currentPostings) {
        
        // Calculate offset for pointers (current size of merged postings)
        long pointerOffset = mergedPostings.size() == 0 ? 0 : 
            mergedPostings.get(mergedPostings.size() - 1).length() + 1; // +1 for newline
        
        // Add all postings from current file to merged postings
        mergedPostings.addAll(currentPostings);
        
        // Update vocabulary entries with new pointers
        for (Map.Entry<String, VocabularyEntry> entry : currentVocabulary.entrySet()) {
            String term = entry.getKey();
            VocabularyEntry vocabEntry = entry.getValue();
            
            // Adjust pointer by adding the offset
            long newPointer = vocabEntry.pointer + pointerOffset;
            
            // If term already exists in merged vocabulary, combine the DFs
            if (mergedVocabulary.containsKey(term)) {
                VocabularyEntry existingEntry = mergedVocabulary.get(term);
                existingEntry.df += vocabEntry.df;
            } else {
                mergedVocabulary.put(term, new VocabularyEntry(vocabEntry.df, newPointer));
            }
        }
    }
    
    private static void writeMergedFiles(
            TreeMap<String, VocabularyEntry> mergedVocabulary,
            List<String> mergedPostings,
            String mergedVocabularyFilePath,
            String mergedPostingFilePath) throws IOException {
        
        // Write merged posting file
        try (BufferedWriter postingWriter = new BufferedWriter(new FileWriter(mergedPostingFilePath, StandardCharsets.UTF_8))) {
            for (String posting : mergedPostings) {
                postingWriter.write(posting);
                postingWriter.newLine();
            }
        }
        
        // Write merged vocabulary file
        try (BufferedWriter vocabWriter = new BufferedWriter(new FileWriter(mergedVocabularyFilePath, StandardCharsets.UTF_8))) {
            for (Map.Entry<String, VocabularyEntry> entry : mergedVocabulary.entrySet()) {
                String term = entry.getKey();
                VocabularyEntry vocabEntry = entry.getValue();
                vocabWriter.write(term + " " + vocabEntry.df + " " + vocabEntry.pointer);
                vocabWriter.newLine();
            }
        }
    }
    
    // Helper class to hold vocabulary entry data
    private static class VocabularyEntry {
        int df;
        long pointer;
        
        VocabularyEntry(int df, long pointer) {
            this.df = df;
            this.pointer = pointer;
        }
    }
}