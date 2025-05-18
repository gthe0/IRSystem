package com.search.utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import com.search.index.Corpus;
import com.search.index.Document;
import com.search.index.Field;
import com.search.index.FieldType;
import com.search.index.Vocabulary;

public class FileBuilder {
    private static final String POSTING_FILE_NAME = "PostingFile_Batch_";
    private static final String VOCABULARY_FILE_NAME = "VocabularyFile_Batch_";
    private static final String DOCUMENT_FILE_NAME = "DocumentFile_Batch_";

    public static final String POSTING_DIR = FileManager.RESULT_DIR + File.separator + "tempPost" + File.separator;
    public static final String VOC_DIR = FileManager.RESULT_DIR + File.separator + "tempVoc" + File.separator;
    public static final String DOC_DIR = FileManager.RESULT_DIR + File.separator + "tempDoc" + File.separator;
    
    private final int batchNumber;

    public FileBuilder(int batchNumber) {
        this.batchNumber = batchNumber;
    }

    public void createBatchFiles(Corpus corpus, long totalDocs) throws IOException {
        // Ensure all directories exist
        FileManager.ensureDirectoryExists(POSTING_DIR);
        FileManager.ensureDirectoryExists(VOC_DIR);
        FileManager.ensureDirectoryExists(DOC_DIR);

        // Create file paths
        String postingFilePath = POSTING_DIR + POSTING_FILE_NAME + batchNumber + ".txt";
        String vocabularyFilePath = VOC_DIR + VOCABULARY_FILE_NAME + batchNumber + ".txt";
        String documentFilePath = DOC_DIR + DOCUMENT_FILE_NAME + batchNumber + ".txt";

        try (
            // Open all files for writing
            RandomAccessFile postingFile = new RandomAccessFile(postingFilePath, "rw");
            BufferedWriter vocabWriter = new BufferedWriter(new FileWriter(vocabularyFilePath, StandardCharsets.UTF_8));
            BufferedWriter docWriter = new BufferedWriter(new FileWriter(documentFilePath, StandardCharsets.UTF_8))
        ) {
            Vocabulary vocabulary = corpus.getVocabulary(); 
            long pointer = 0;

            // First write all document metadata
            for (Document document : corpus) {
                docWriter.write(document.getPmcdId() + " " + document.getDocPath() + "\n");
            }

            // Then process vocabulary and postings
            for (String term : vocabulary.getSortedTerms()) {
                Set<Integer> docIds = vocabulary.getDocumentIds(term);
                int df = docIds.size(); 

                // Write postings for the term
                for (Integer docId : docIds) {
                    Document document = corpus.getDocument(docId);
                    int tf = document.getTf().get(term);

                    // Build positional information
                    StringBuilder positions = new StringBuilder("[");
                    for (FieldType fieldType : FieldType.values()) {
                        Field field = document.getField(fieldType);
                        if (field != null && field.getTermPositions().containsKey(term)) {
                            TreeSet<Integer> termPositions = field.getTermPositions().get(term);
                            for (Integer position : termPositions) {
                                positions.append(fieldType.getFieldCode()).append(":").append(position).append(",");
                            }
                        }
                    }

                    if (positions.length() > 1) {
                        positions.setLength(positions.length() - 1); // Remove trailing comma
                    }
                    positions.append("]");
                    postingFile.writeBytes(docId + " " + tf + " " + positions.toString() + "\n");
                }

                // Write vocabulary entry
                vocabWriter.write(term + " " + df + " " + pointer + "\n");
                pointer = postingFile.getFilePointer();
            }
        }

        System.out.println("Created files for batch " + batchNumber + ":");
        System.out.println("- Posting file: " + postingFilePath);
        System.out.println("- Vocabulary file: " + vocabularyFilePath);
        System.out.println("- Document file: " + documentFilePath);
        
        corpus.clear();
    }
}