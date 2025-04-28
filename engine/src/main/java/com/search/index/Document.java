package com.search.index;
import java.util.HashMap;

public class Document {
    private HashMap<FieldType, Field> fieldMap;
    private String docpath;

    public Document() {
        fieldMap = new HashMap<>();
    }

    public void addField(Field field) {
        fieldMap.put(field.getType(), field);
    }

    public Field getField(FieldType fieldType) {
        return fieldMap.get(fieldType); // Faster lookup by field type
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Field field : fieldMap.values()) {
            sb.append(field.toString()).append("\n");
        }
        return sb.toString();
    }
}
