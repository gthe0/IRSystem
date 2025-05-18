package com.search.common.token;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class SimpleTokenStream extends TokenStream {

    public SimpleTokenStream(BufferedReader reader) {
        super(reader);
    }

    public SimpleTokenStream(BufferedReader reader, HashSet<String> stopWords) {
        super(reader, stopWords);
    }
    
    public SimpleTokenStream(String content) {
        super(new BufferedReader(new StringReader(content)));
    }

    public SimpleTokenStream(String content, HashSet<String> stopWords) {
        super(new BufferedReader(new StringReader(content)), stopWords);
    }

    public SimpleTokenStream(File file) throws IOException {
        super(new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8")));
    }

    @Override
    protected String[] tokenize(String line) {
        // Remove punctuation and split the line into tokens
        String sanitizedLine = line.replaceAll("[\\p{Punct}]+", " ")
                                   .replaceAll("\\s+", " ")
                                   .toLowerCase();

        String[] strArray = sanitizedLine.split(" ");

        // Filter out stop words if the set exists
        List<String> filteredTokens = new ArrayList<>();
        for (String token : strArray) {
            token = token.trim();
            if (isValidUtf8(token)) { 
                filteredTokens.add(token);
            }
        }

        return filteredTokens.toArray(new String[0]);
    }

    // Check that the term is utf8
    private boolean isValidUtf8(String token) {
        return !token.isEmpty() && token.matches("\\A\\p{ASCII}*|\\p{L}+\\z") && !stopWords.contains(token);
    }
}
