package com.search.index;

import gr.uoc.csd.hy463.NXMLFileReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import com.search.token.SimpleTokenStream;
import com.search.token.TokenStream;

public class DocumentFactory {
    
    // Scan a single document
    public static Document createDocument(File file) throws IOException {
        NXMLFileReader xmlFile = new NXMLFileReader(file);

        TreeMap<String, Integer> documentTf = new TreeMap<>();
        Document                 document   = new Document(Integer.parseInt(xmlFile.getPMCID()), file.getAbsolutePath(), documentTf);

        // Add fields to the document
        document.addField( new Field( FieldType.TITLE,     tokenizeContent(new SimpleTokenStream(xmlFile.getTitle()), documentTf)));
        document.addField( new Field( FieldType.ABSTRACT,  tokenizeContent(new SimpleTokenStream(xmlFile.getAbstr()), documentTf)));
        document.addField( new Field( FieldType.BODY,      tokenizeContent(new SimpleTokenStream(xmlFile.getBody()), documentTf)));
        document.addField( new Field( FieldType.JOURNAL,   tokenizeContent(new SimpleTokenStream(xmlFile.getJournal()), documentTf)));
        document.addField( new Field( FieldType.PUBLISHER, tokenizeContent(new SimpleTokenStream(xmlFile.getPublisher()), documentTf)));
        document.addField( new Field( FieldType.AUTHOR,    tokenizeContent(new SimpleTokenStream(xmlFile.getAuthors().toString()), documentTf)));
        document.addField( new Field( FieldType.CATEGORY,  tokenizeContent(new SimpleTokenStream(xmlFile.getCategories().toString()), documentTf)));

        return document;
    }


    // Create a list of documents
    public static List<Document> createDocuments(List<File> files) throws IOException {
        ArrayList<Document> docs = new ArrayList<>();
        for (File file: files) {
            docs.add(createDocument(file));
        }
        return docs;
    }

    // Common logic to process tokens using a TokenStream
    private static TreeMap<String, Integer> tokenizeContent(
        TokenStream tokenStream, 
        TreeMap<String, Integer> docTf) throws IOException
    {
        String token;
        TreeMap<String, Integer> tf = new TreeMap<>();

        while ((token = tokenStream.getNext()) != null) {
            tf.put(token, tf.getOrDefault(token, 0) + 1);
            docTf.put(token, docTf.getOrDefault(token, 0) + 1);
        }

        return tf;
    }
}
