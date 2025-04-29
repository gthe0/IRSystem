package com.search.index;

import java.io.IOException;
import java.util.TreeMap;

public class Field {

    protected final TreeMap<String, Integer> tf; // Term Frequency Map
    protected final FieldType type;              // Field type

    // Constructor that accepts an external TokenStream
    public Field(FieldType type, TreeMap<String, Integer> tf) throws IOException {
        this.tf = tf;
        this.type = type;
    }

    public FieldType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "FieldType: " + type + ", Term Frequencies: " + tf.toString();
    }
}
