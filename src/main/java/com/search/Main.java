package com.search;

import com.search.utils.FileManager;
import com.search.utils.StopWordManager;
import com.search.index.DocumentFactory;
import com.search.index.Document;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        try {
            // Load stop words once at the start
            System.out.println("Select the directory containing stopword files:");
            String stopwordDirectory = FileManager.showFileChooser(false);
            StopWordManager.loadStopWords(stopwordDirectory);

            // Read documents from the selected directory
            System.out.println("Select the directory containing the XML documents:");
            String documentDirectory = FileManager.showFileChooser(false);
            List<String> xmlFiles = FileManager.getFilesInDirectory(documentDirectory);

            List<Document> documents = DocumentFactory.createDocuments(xmlFiles);

            // Print term frequencies for each document
            for (Document document : documents) {
                document.printTermFrequencies();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}