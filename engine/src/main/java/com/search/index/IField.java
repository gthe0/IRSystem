package com.search.index;

public interface IField {

    // Various getters 
    String      getName();
    Object      getValue();

    // Return null if they are not one of
    // the following values
    FieldType   getType();
    boolean     isIndexable();

    String      toString();
}
