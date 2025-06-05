package com.search.indexer.model;

import java.util.HashMap;
import java.util.TreeSet;

public class Field {

    protected final HashMap<String, TreeSet<Integer>> termPositions; // Map of term to positions
    protected final FieldType type; // Field type

    // Constructor that initializes term frequencies and positions
    public Field(FieldType type, HashMap<String, TreeSet<Integer>> termPositions) {
        this.termPositions = termPositions;
        this.type = type;
    }

    // Retrieve term positions
    public HashMap<String, TreeSet<Integer>> getTermPositions() {
        return termPositions;
    }

    public FieldType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "FieldType: " + type + ", Term Positions: " + termPositions.toString();
    }
}
