package com.search.utils;

import java.io.IOException;

import com.search.index.Vocabulary;

public class Indexer {
    public void indexDocuments(Vocabulary vocabulary) throws IOException {
        // Export the vocabulary using FileManager
        String outputFilePath = FileManager.RESULT_DIR + "CollectionIndex/VocabularyFile.txt";
        FileManager.ensureDirectoryExists(FileManager.RESULT_DIR + "CollectionIndex");
        vocabulary.exportVocabulary(outputFilePath);

        System.out.println("Vocabulary exported to: " + outputFilePath);
    }
}
