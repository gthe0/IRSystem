package com.search.query.model;

public class VocabularyTrie {
    private static final int ALPHABET_SIZE = 36; // a-z (26) + 0-9 (10)
    
    private static class TrieNode {
        TrieNode[] children = new TrieNode[ALPHABET_SIZE];
        int df = -1;        // Document frequency
        long pointer = -1;  // File position pointer
    }
    
    private final TrieNode root = new TrieNode();
    
    public void insert(String term, int df, long pointer) {
        TrieNode current = root;
        for (char c : term.toCharArray()) {
            int index = charToIndex(c);
            if (index < 0 || index >= ALPHABET_SIZE) {
                throw new IllegalArgumentException("Invalid term character: " + c + 
                                                  " (only a-z and 0-9 allowed)");
            }
            if (current.children[index] == null) {
                current.children[index] = new TrieNode();
            }
            current = current.children[index];
        }
        current.df = df;
        current.pointer = pointer;
    }
    
    public TermData search(String term) {
        TrieNode current = root;
        for (char c : term.toCharArray()) {
            int index = charToIndex(c);
            if (index < 0 || index >= ALPHABET_SIZE || current.children[index] == null) {
                return null;
            }
            current = current.children[index];
        }
        return current.df != -1 ? new TermData(current.df, current.pointer) : null;
    }
    
    // Handles both letters and digits
    private int charToIndex(char c) {
        if (Character.isLetter(c)) {
            return Character.toLowerCase(c) - 'a'; // a-z: 0-25
        } else if (Character.isDigit(c)) {
            return 26 + (c - '0'); // 0-9: 26-35
        }
        return -1; // Invalid character
    }
    
    public static class TermData {
        public final int df;
        public final long pointer;
        
        public TermData(int df, long pointer) {
            this.df = df;
            this.pointer = pointer;
        }
    }
}