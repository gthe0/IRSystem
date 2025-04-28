package com.search.index;

import com.search.token.SimpleTokenStream;
import com.search.utils.StopWordManager;

import java.io.IOException;

public class FieldFactory {

    public static Field createField(FieldType type, String content) throws IOException {
        SimpleTokenStream tokenStream = new SimpleTokenStream(content, StopWordManager.getStopWords());
        return new Field(type, tokenStream);
    }
}
