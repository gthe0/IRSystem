package com.search.index;

public interface IField {

    // Various getters 
    String     getName();
    FieldProps getFieldProps();

    // Return null if they are not one of
    // the following values
    String getStringValue();
    Number getNumberValue();

}
