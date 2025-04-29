package com.search;

import com.search.utils.FileManager;
import com.search.utils.Indexer;
import com.search.utils.StopWordManager;
import com.search.index.Document;
import com.search.index.DocumentFactory;
import com.search.index.Vocabulary;

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

            // Create and use Indexer for processing
            Indexer indexer = new Indexer();
            Vocabulary vocabulary  = new Vocabulary();

            vocabulary.updateVocabulary(documents);
            indexer.indexDocuments(vocabulary);

            for (Document document : documents) {
                document.printTermFrequencies();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
