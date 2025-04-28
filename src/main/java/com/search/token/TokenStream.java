package com.search.token;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.util.HashSet;

public abstract class TokenStream implements Closeable {
    protected BufferedReader reader; 
    private String currentLine;      
    private int tokenIndex;          
    private String[] tokens;
    
    protected HashSet<String> stopWords;

    // Constructor initializes the BufferedReader
    public TokenStream(BufferedReader reader) {
        this.reader = reader;
        this.currentLine = null;
        this.stopWords = new HashSet<>();
        this.tokenIndex = 0;
        this.tokens = new String[0];
    }

    // Constructor initializes the BufferedReader and loads stop words
    public TokenStream(BufferedReader reader, HashSet<String> stopWords) {
        this(reader); 
        this.stopWords = stopWords;
    }

    // Abstract method to customize tokenization logic
    protected abstract String[] tokenize(String line);

    // Returns the next term/token in the file
    public String getNext() throws IOException {
        while (tokenIndex >= tokens.length) {
            currentLine = reader.readLine(); 
            if (currentLine == null) {
                return null; 
            }
            tokens = tokenize(currentLine);
            tokenIndex = 0; 
        }
        return tokens[tokenIndex++]; // Return the next token
    }

    // Closes the reader to release resources
    @Override
    public void close() throws IOException {
        if (reader != null) {
            reader.close();
        }
    }
}
