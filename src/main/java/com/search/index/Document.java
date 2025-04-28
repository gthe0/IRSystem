package com.search.index;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class Document {
    private HashMap<FieldType, Field> fieldMap;
    private String docPath;
    
    private int calculateTotalFrequency(TreeMap<FieldType, Integer> fieldMap) {
        return fieldMap.values().stream().mapToInt(Integer::intValue).sum();
    }

    private void printAggregatedTermFrequencies(TreeMap<String, TreeMap<FieldType, Integer>> aggregatedTF) {
        for (Map.Entry<String, TreeMap<FieldType, Integer>> entry : aggregatedTF.entrySet()) {
            String term = entry.getKey();
            TreeMap<FieldType, Integer> fieldMap = entry.getValue();

            int totalFrequency = calculateTotalFrequency(fieldMap);
            System.out.println(term + ": " + totalFrequency);

            for (Map.Entry<FieldType, Integer> fieldEntry : fieldMap.entrySet()) {
                System.out.println(" - Field: " + fieldEntry.getKey() + ", Frequency: " + fieldEntry.getValue());
            }
        }
    }

    
    // Private method to aggregate term frequencies
    private TreeMap<String, TreeMap<FieldType, Integer>> aggregateTermFrequencies() {
        TreeMap<String, TreeMap<FieldType, Integer>> aggregatedTF = new TreeMap<>();

        for (Map.Entry<FieldType, Field> entry : fieldMap.entrySet()) {
            FieldType fieldType = entry.getKey();
            Field field = entry.getValue();

            for (Map.Entry<String, Integer> termEntry : field.tf.entrySet()) {
                String term = termEntry.getKey();
                int frequency = termEntry.getValue();

                // Add term to the aggregation map
                aggregatedTF.putIfAbsent(term, new TreeMap<>());
                aggregatedTF.get(term).put(fieldType, frequency);
            }
        }
        return aggregatedTF;
    }

    public Document() {
        this.docPath = null;
        fieldMap = new HashMap<>();
    }

    public Document(String docPath) {
        this.docPath = docPath;
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Field field : fieldMap.values()) {
            sb.append(field.toString()).append("\n");
        }
        return sb.toString();
    }
  
    // Public method to print term frequencies
    public void printTermFrequencies() {
        System.out.println("Term Frequencies for Document: " + (docPath != null ? docPath : "Unknown"));

        TreeMap<String, TreeMap<FieldType, Integer>> aggregatedTF = aggregateTermFrequencies();
        printAggregatedTermFrequencies(aggregatedTF);
    }

}
