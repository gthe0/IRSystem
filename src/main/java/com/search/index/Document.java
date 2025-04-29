package com.search.index;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class Document {
    private HashMap<FieldType, Field> fieldMap;
    private TreeMap<String, Integer> tf; // Term frequencies passed in directly
    private String docPath;
    private Integer pmcdId;

    public Document(Integer pmcdId) {
        this(pmcdId, null, new TreeMap<>());
    }

    public Document(Integer pmcdId, String docPath) {
        this(pmcdId, docPath, new TreeMap<>());
    }
    
    // Constructor that initializes with document path and term frequencies
    public Document(Integer pmcdId, String docPath, TreeMap<String, Integer> docTf) {
        this.docPath = docPath;
        this.pmcdId = pmcdId;
        this.tf = docTf != null ? docTf : new TreeMap<>();
        fieldMap = new HashMap<>();
    }

    public void addField(Field field) {
        fieldMap.put(field.getType(), field);
    }

    public Field getField(FieldType fieldType) {
        return fieldMap.get(fieldType); // Faster lookup by field type
    }

    public String getDocPath() {
        return docPath;
    }

    public Integer getPmcdId() {
        return pmcdId;
    }

    public TreeMap<String, Integer> getTf() {
        return tf;
    }

    // Print global frequency of each term, followed by its frequency in each field
    public void printTermFrequencies() {
        System.out.println("Term Frequencies for Document: " + (docPath != null ? docPath : "Unknown"));

        for (Map.Entry<String, Integer> entry : tf.entrySet()) {
            String term = entry.getKey();
            int globalFrequency = entry.getValue();

            System.out.println(term + ": " + globalFrequency); // Global frequency
            System.out.println("  Frequencies by Field:");

            for (Map.Entry<FieldType, Field> fieldEntry : fieldMap.entrySet()) {
                FieldType fieldType = fieldEntry.getKey();
                Field field = fieldEntry.getValue();

                int fieldFrequency = field.tf.getOrDefault(term, 0);
                System.out.println("    - Field Type: " + fieldType + ", Frequency: " + fieldFrequency);
            }
        }
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        // Print the docPath and pmcdId
        sb.append("Document Path: ").append(docPath != null ? docPath : "Unknown").append("\n");
        sb.append("PMCID: ").append(pmcdId != null ? pmcdId : "Unknown").append("\n");
        
        // Print details for each field
        sb.append("Fields:\n");
        for (Field field : fieldMap.values()) {
            sb.append(field.toString()).append("\n");
        }
        
        return sb.toString();
    }
    
}
