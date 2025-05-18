package com.search.query.reader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import com.search.query.model.VocabularyTrie;

public class VocabularyReader {
    
    public void loadVocabulary(File vocabFile, VocabularyTrie trie) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(vocabFile))) {
            String line;
            int lineCount = 0;
            
            while ((line = br.readLine()) != null) {
                lineCount++;
                processLine(line, trie, lineCount);
            }
        }
    }

    private void processLine(String line, VocabularyTrie trie, int lineNumber) {
        String[] parts = line.trim().split("\\s+");
        
        if (parts.length != 3) {
            System.err.println("Invalid format at line " + lineNumber + ": " + line);
            return;
        }

        String term = parts[0].toLowerCase();  // Ensure lowercase
        int df;
        long pointer;

        try {
            df = Integer.parseInt(parts[1]);
            pointer = Long.parseLong(parts[2]);
        } catch (NumberFormatException e) {
            System.err.println("Number format error at line " + lineNumber + ": " + line);
            return;
        }

        try {
            trie.insert(term, df, pointer);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid term at line " + lineNumber + ": " + term);
        }
    }

    // Alternative method for bulk loading from memory
    public void loadFromMemory(String[] entries, VocabularyTrie trie) {
        for (int i = 0; i < entries.length; i++) {
            processLine(entries[i], trie, i+1);
        }
    }
}