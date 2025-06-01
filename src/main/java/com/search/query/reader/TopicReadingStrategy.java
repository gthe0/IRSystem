package com.search.query.reader;

import com.search.query.model.Query;
import gr.uoc.csd.hy463.Topic;
import gr.uoc.csd.hy463.TopicsReader;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TopicReadingStrategy implements QueryReadingStrategy {
    private final File      xmlFile;
    private final boolean   readSummary;
    
    public TopicReadingStrategy(File xmlFile, boolean readSummary) {
        this.xmlFile = xmlFile;
        this.readSummary = readSummary;
    }

    @Override
    public List<Query> readQueries() throws Exception {
        List<Query> queries = new ArrayList<>();
        List<Topic> topics = TopicsReader.readTopics(xmlFile.getAbsolutePath());
        
        for (Topic topic : topics) {
            String text = readSummary ? topic.getSummary() : topic.getDescription();
            
            if (text == null || text.isBlank()) continue;
            
            List<String> tokens = process(text);
            if (!tokens.isEmpty()) {
                queries.add(new Query(topic.getNumber(), text, tokens));
            }
        }
        return queries;
    }
}