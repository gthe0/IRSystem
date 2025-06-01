package com.search.query.reader;

import com.search.query.model.Query;

import java.io.File;
import java.util.List;

public class QueryReader {
    private QueryReadingStrategy strategy;
    
    public QueryReader(QueryReadingStrategy strategy) {
        this.strategy = strategy;
    }
    
    public void setStrategy(QueryReadingStrategy strategy) {
        this.strategy = strategy;
    }
    
    public List<Query> read() throws Exception {
        return strategy.readQueries();
    }
    
    // Factory method for convenience
    public static QueryReader createForTerminal() {
        return new QueryReader(new TerminalReadingStrategy());
    }
    
    public static QueryReader createForTextFile(File file) {
        return new QueryReader(new TextFileReadingStrategy(file));
    }
    
    public static QueryReader createForXMLFile(File file, boolean source) {
        return new QueryReader(new TopicReadingStrategy(file, source));
    }
}