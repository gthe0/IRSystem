package com.search;

import com.search.utils.FileManager;
import com.search.utils.StopWordManager;
import com.search.index.Corpus;
import com.search.index.Document;
import com.search.index.DocumentFactory;
import com.search.index.FileMerger;
import com.search.index.FileBuilder;
import com.search.utils.FileBatchIterator;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class Main {

    private static final int BATCH_SIZE = 100; 

    public static void main(String[] args) {
        try {
            // Load stop words once at the start
            System.out.println("Select the directory containing stopword files:");
            File stopwordDirectory = FileManager.showFileChooserForDirectory();
            StopWordManager.loadStopWords(stopwordDirectory);

            // Select the directory containing the XML documents
            System.out.println("Select the directory containing the XML documents:");
            File documentDirectory = FileManager.showFileChooserForDirectory();

            // Define the batch size
            FileBatchIterator fileBatchIterator = FileManager.getFileBatchIterator(documentDirectory, BATCH_SIZE);
            int batchNo = 0;
            while (fileBatchIterator.hasNext()) {
                List<Path> xmlFiles = fileBatchIterator.next();
                List<Document> documents = DocumentFactory.createDocuments(xmlFiles);

                Corpus corpus = new Corpus();
                corpus.addDocuments(documents);

                FileBuilder postingFileBuilder = new FileBuilder(batchNo);
                postingFileBuilder.createBatchFiles(corpus);
                System.out.println("Batch of " + xmlFiles.size() + " files processed successfully.");

                batchNo++;
            }
            
            System.out.println("All batches processed successfully.");

            FileMerger fileMerger = new FileMerger();
            fileMerger.mergeFiles(FileBuilder.VOC_DIR, FileBuilder.POSTING_DIR, FileManager.RESULT_DIR + File.separator + "CollectionIndex");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
