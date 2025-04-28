package com.search.index;

import java.io.IOException;
import java.util.TreeMap;

import com.search.token.TokenStream;

public class Field {

    protected final TreeMap<String, Integer> tf; // Term Frequency Map
    protected final FieldType type;              // Field type

    // Constructor that accepts an external TokenStream
    public Field(FieldType type, TokenStream tokenStream) throws IOException {
        this.tf = new TreeMap<>();
        this.type = type;

        tokenizeContent(tokenStream);
    }

    // Common logic to process tokens using a TokenStream
    private void tokenizeContent(TokenStream tokenStream) throws IOException {
        String token;
        while ((token = tokenStream.getNext()) != null) {
            tf.put(token, tf.getOrDefault(token, 0) + 1);
        }
    }

    public FieldType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "FieldType: " + type + ", Term Frequencies: " + tf.toString();
    }
}
