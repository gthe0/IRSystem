package com.search.indexer;

public enum FieldType {
    TITLE(0),
    ABSTRACT(1),
    BODY(2),
    AUTHOR(3),
    CATEGORY(4),
    JOURNAL(5),
    PUBLISHER(6);

    private final int fieldCode;

    FieldType(int fieldCode) {
        this.fieldCode = fieldCode;
    }

    public int getFieldCode() {
        return fieldCode;
    }
}
