package com.search.index;

public class Field implements IField {

    protected final String     name;
    protected Object fieldsData;

    Field(String name)
    {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public FieldProps getFieldProps() {
        return null;
    }

    @Override
    public String getStringValue() {
        return null;
    }

    @Override
    public Number getNumberValue() {
        return null;
    }

}
