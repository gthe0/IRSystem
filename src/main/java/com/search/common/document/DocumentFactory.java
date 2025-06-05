package com.search.common.document;

import gr.uoc.csd.hy463.NXMLFileReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import com.search.common.token.SimpleTokenStream;
import com.search.common.token.TokenStream;
import com.search.common.utils.StopWordManager;

public class DocumentFactory {
    
    // Scan a single document
    public static Document createDocument(File file) throws IOException {
        NXMLFileReader xmlFile = new NXMLFileReader(file);

        TreeMap<String, Integer> documentTf = new TreeMap<>();
        Document                 document   = new Document(Integer.parseInt(xmlFile.getPMCID()), file.getAbsolutePath(), documentTf);

        // Add fields to the document
        document.addField( new Field( FieldType.TITLE,     tokenizeContent(new SimpleTokenStream(xmlFile.getTitle(), StopWordManager.getStopWords()), documentTf)));
        document.addField( new Field( FieldType.ABSTRACT,  tokenizeContent(new SimpleTokenStream(xmlFile.getAbstr(), StopWordManager.getStopWords()), documentTf)));
        document.addField( new Field( FieldType.BODY,      tokenizeContent(new SimpleTokenStream(xmlFile.getBody(),  StopWordManager.getStopWords()), documentTf)));
        document.addField( new Field( FieldType.JOURNAL,   tokenizeContent(new SimpleTokenStream(xmlFile.getJournal(), StopWordManager.getStopWords()), documentTf)));
        document.addField( new Field( FieldType.PUBLISHER, tokenizeContent(new SimpleTokenStream(xmlFile.getPublisher(), StopWordManager.getStopWords()), documentTf)));
        document.addField( new Field( FieldType.AUTHOR,    tokenizeContent(new SimpleTokenStream(xmlFile.getAuthors().toString(), StopWordManager.getStopWords()), documentTf)));
        document.addField( new Field( FieldType.CATEGORY,  tokenizeContent(new SimpleTokenStream(xmlFile.getCategories().toString(), StopWordManager.getStopWords()), documentTf)));

        document.calcDocumentLength();
        document.calcMaxFrequency();

        return document;
    }


    public static List<Document> createDocuments(List<Path> filePaths) throws IOException {
        ArrayList<Document> docs = new ArrayList<>();
        for (Path path : filePaths) {
            docs.add(createDocument(path.toFile())); // Convert Path to File
        }
        return docs;
    }

    // Common logic to process tokens using a TokenStream
    private static HashMap<String, TreeSet<Integer>> tokenizeContent(
        TokenStream tokenStream, 
        TreeMap<String, Integer> docTf) throws IOException
    {
        String token;
        HashMap<String, TreeSet<Integer>> termPositions = new HashMap<>();

        Integer position = 0; 
        while ((token = tokenStream.getNext()) != null) {
            token = token.trim();
            if (!token.isEmpty()) {
                termPositions.putIfAbsent(token, new TreeSet<>());
                termPositions.get(token).add(position);
                docTf.put(token, docTf.getOrDefault(token, 0) + 1);
                position++;
            }
        }
        
        return termPositions;
    }
}
