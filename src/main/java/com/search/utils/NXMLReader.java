package com.search.utils;

import gr.uoc.csd.hy463.NXMLFileReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.search.index.Document;
import com.search.index.FieldFactory;
import com.search.index.FieldType;

public class NXMLReader {

    // Scan a single document
    public static Document createDocument(String filePath) throws IOException {
        File file = new File(filePath);
        NXMLFileReader xmlFile = new NXMLFileReader(file);

        Document document = new Document();

        // Add fields to the document
        document.addField( FieldFactory.createField(FieldType.TITLE,     xmlFile.getTitle()));
        document.addField( FieldFactory.createField(FieldType.ABSTRACT,  xmlFile.getAbstr()));
        document.addField( FieldFactory.createField(FieldType.BODY,      xmlFile.getBody()));
        document.addField( FieldFactory.createField(FieldType.JOURNAL,   xmlFile.getJournal()));
        document.addField( FieldFactory.createField(FieldType.PUBLISHER, xmlFile.getPublisher()));
        document.addField( FieldFactory.createField(FieldType.PMCID,     xmlFile.getPMCID()));
        document.addField( FieldFactory.createField(FieldType.AUTHOR,    xmlFile.getAuthors().toString()));
        document.addField( FieldFactory.createField(FieldType.CATEGORY,  xmlFile.getCategories().toString()));

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
