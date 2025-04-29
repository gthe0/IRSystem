package com.search.index;

import com.search.utils.FileManager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class FileBuilder {
    private static final String POSTING_FILE_NAME = "PostingFile_Batch_";
    private static final String VOCABULARY_FILE_NAME = "VocabularyFile_Batch_";

    public static final String POSTING_DIR = FileManager.RESULT_DIR + File.separator + "tempPost" + File.separator;
    public static final String VOC_DIR = FileManager.RESULT_DIR + File.separator + "tempVoc" + File.separator;
    
    private final int batchNumber;

    public FileBuilder(int batchNumber) {
        this.batchNumber = batchNumber;
    }

    public void createBatchFiles(Corpus corpus) throws IOException {
        String postingFilePath =  POSTING_DIR + File.separator + POSTING_FILE_NAME + batchNumber + ".txt";
        String vocabularyFilePath =  VOC_DIR + File.separator + VOCABULARY_FILE_NAME + batchNumber + ".txt";

        FileManager.ensureDirectoryExists(POSTING_DIR);
        FileManager.ensureDirectoryExists(VOC_DIR);

        try (
            RandomAccessFile postingFile = new RandomAccessFile(postingFilePath, "rw");
            BufferedWriter vocabWriter = new BufferedWriter(new FileWriter(vocabularyFilePath, StandardCharsets.UTF_8))
        ) {
            Vocabulary vocabulary = corpus.getVocabulary(); 
            long pointer = 0;

            for (String term : vocabulary.getSortedTerms()) {
                Set<Integer> docIds = vocabulary.getDocumentIds(term); // Get all document IDs for this term
                int df = docIds.size(); 

                // Write postings for the term to the Posting File
                for (Integer docId : docIds) {
                    Document document = corpus.getDocument(docId);

                    // Get term frequency for the document
                    int tf = document.getTf().get(term);

                    // Build positional information for the term
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

                // Write term, df, and pointer to Vocabulary File
                vocabWriter.write(term + " " + df + " " + pointer + "\n");
                pointer = postingFile.getFilePointer();
            }
        }

        System.out.println("Posting File and Vocabulary File created for batch " + batchNumber + ".");
        corpus.clear();
    }
}
