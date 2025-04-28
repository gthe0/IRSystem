package com.search.utils;

import gr.uoc.csd.hy463.NXMLFileReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.search.index.Document;
import com.search.index.Field;

public class NXMLReader {

    // Scan a single document
    public static Document createDocument(String filePath) throws IOException {
        File file = new File(filePath);
        NXMLFileReader xmlFile = new NXMLFileReader(file);

        Document document = new Document();

        // Add fields to the document
        document.addField(new Field("AA", xmlFile.getTitle()));
        document.addField(new Field("AA", xmlFile.getAbstr()));
        document.addField(new Field("AA", xmlFile.getBody()));
        document.addField(new Field("AA", xmlFile.getJournal()));
        document.addField(new Field("AA", xmlFile.getPublisher()));
        document.addField(new Field("AA", xmlFile.getPMCID()));
        document.addField(new Field("AA", xmlFile.getAuthors()));
        document.addField(new Field("AA", xmlFile.getCategories()));

        return document;
    }

    // Create a list of documents
    public static List<Document> createDocument(List<String> filepaths) throws IOException {
        ArrayList<Document> docs = new ArrayList<>();
        for (String filepath : filepaths) {
            docs.add(createDocument(filepath));
        }

        return docs;
    }
}
