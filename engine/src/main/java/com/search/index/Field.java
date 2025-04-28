package com.search.index;

public class Field implements IField {

    protected final String      name;
    protected final Object      data;

    public Field(String name)
    {
        this.data = null;
        this.name = name;
    }

    public Field(String name, Object data)
    {
        this.data = data;
        this.name = name;
    }

    @Override
    public Object getValue() {
        return data;
    }

    @Override
    public FieldType getType() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getType'");
    }

    @Override
    public boolean isIndexable() {
        return false;
    }

    @Override
    public String getName() {
        return name;
    }

}
