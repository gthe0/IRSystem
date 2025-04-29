package com.search.utils;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import com.search.index.Corpus;
import com.search.index.Vocabulary;

public class Indexer {
    public static void indexDocuments(Corpus corpus) throws IOException {
        // Export the vocabulary using FileManager
        String outputFilePath = FileManager.RESULT_DIR + "CollectionIndex/VocabularyFile.txt";
        FileManager.ensureDirectoryExists(FileManager.RESULT_DIR + "CollectionIndex");
        exportVocabulary(corpus.getVocabulary(), outputFilePath);

        System.out.println("Vocabulary exported to: " + outputFilePath);
    }

    // Export the vocabulary to a file
    private static void exportVocabulary(Vocabulary vocabulary, String filePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), "UTF-8"))) { 
            for (String term : vocabulary.getSortedTerms()) {
                int documentFrequency = vocabulary.getDocumentFrequency(term);
                writer.write(term + " " + documentFrequency + "\n");
            }
        }
    }


}
