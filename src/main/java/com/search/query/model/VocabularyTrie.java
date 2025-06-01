package com.search.query.model;

import java.util.HashMap;
import java.util.Map;

public class VocabularyTrie {
    private static class TrieNode {
        Map<Character, TrieNode> children = new HashMap<>();
        int df = -1;        // Document frequency
        long pointer = -1;  // File position pointer
    }
    
    private final TrieNode root = new TrieNode();
    
    public void insert(String term, int df, long pointer) {
        TrieNode current = root;
        for (char c : term.toCharArray()) {
            char normalized = normalizeChar(c);
            current = current.children.computeIfAbsent(normalized, k -> new TrieNode());
        }
        current.df = df;
        current.pointer = pointer;
    }
    
    public TermData search(String term) {
        TrieNode current = root;
        for (char c : term.toCharArray()) {
            char normalized = normalizeChar(c);
            TrieNode nextNode = current.children.get(normalized);
            if (nextNode == null) {
                return null;
            }
            current = nextNode;
        }
        return current.df != -1 ? new TermData(current.df, current.pointer) : null;
    }
    
    private char normalizeChar(char c) {
        if (Character.isLetter(c)) {
            return Character.toLowerCase(c); // Case-insensitive handling
        } else if (Character.isDigit(c)) {
            return c; // Digits remain unchanged
        }
        throw new IllegalArgumentException("Invalid term character: '" + c + 
                                           "' (only letters and digits allowed)");
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