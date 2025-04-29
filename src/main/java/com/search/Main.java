package com.search;

import com.search.utils.FileManager;
import com.search.utils.Indexer;
import com.search.utils.StopWordManager;
import com.search.index.Corpus;
import com.search.index.Document;
import com.search.index.DocumentFactory;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        try {
            // Load stop words once at the start
            System.out.println("Select the directory containing stopword files:");
            String stopwordDirectory = FileManager.showFileChooserForDirectory();
            StopWordManager.loadStopWords(stopwordDirectory);

            // Read documents from the selected directory
            System.out.println("Select the directory containing the XML documents:");
            String documentDirectory = FileManager.showFileChooserForDirectory();
            List<String> xmlFiles = FileManager.getFilesInDirectory(documentDirectory);
            List<Document> documents = DocumentFactory.createDocuments(xmlFiles);

            // Create a corpus and add documents to it
            Corpus corpus = new Corpus();
            corpus.addDocuments(documents);

            // Use Indexer to process the corpus and export vocabulary
            Indexer.indexDocuments(corpus);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
