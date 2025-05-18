package com.search;

import com.search.utils.FileManager;
import com.search.utils.FileMerger;
import com.search.utils.StopWordManager;
import com.search.index.*;
import com.search.utils.FileBatchIterator;
import com.search.utils.FileBuilder;
import com.search.utils.FileCollector;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Main {

    // Conservative batch size for 2GB heap
    private static final int BATCH_SIZE = 0x200; 
    
    // Exactly two threads - one for processing, one for writing
    private static final ExecutorService processingExecutor = Executors.newSingleThreadExecutor();
    private static final ExecutorService fileWritingExecutor = Executors.newSingleThreadExecutor();
    
    // Track active batches to prevent memory overload
    private static final Semaphore memorySemaphore = new Semaphore(2); // Allow 2 batches in memory

    public static void main(String[] args) {
        try {
            System.out.println("JVM Memory: " + Runtime.getRuntime().maxMemory() / (1024 * 1024) + "MB");
            System.out.println("Using conservative batch size: " + BATCH_SIZE);
            
            System.out.println("Select the directory containing stopword files:");
            File stopwordDirectory = FileManager.showFileChooserForDirectory();
            StopWordManager.loadStopWords(stopwordDirectory);

            System.out.println("Select the directory containing the XML documents:");
            File documentDirectory = FileManager.showFileChooserForDirectory();

            long totalDocs = FileManager.countFilesInDir(documentDirectory.toPath());

            FileBatchIterator fileBatchIterator = FileManager.getFileBatchIterator(documentDirectory, BATCH_SIZE);
            List<Future<?>> futures = new ArrayList<>();

            while (fileBatchIterator.hasNext()) {
                final List<Path> xmlFiles = fileBatchIterator.next();
                final int currentBatchNo = futures.size();
                
                // Wait for memory availability
                memorySemaphore.acquire();
                
                futures.add(processingExecutor.submit(() -> {
                    try {
                        System.out.println("Processing batch " + currentBatchNo + " (" + xmlFiles.size() + " files)");
                        
                        // Process documents sequentially (stemmer limitation)
                        List<Document> documents = new ArrayList<>();
                        for (Path xmlFile : xmlFiles) {
                            try {
                                Document doc = DocumentFactory.createDocument(xmlFile.toFile());
                                documents.add(doc);
                            } catch (Exception e) {
                                System.err.println("Error processing file in batch " + currentBatchNo + ": " + xmlFile);
                                e.printStackTrace();
                            }
                        }
                        
                        if (!documents.isEmpty()) {
                            Corpus corpus = new Corpus();
                            corpus.addDocuments(documents);
                            
                            // Submit writing task
                            fileWritingExecutor.submit(() -> {
                                try {
                                    FileBuilder postingFileBuilder = new FileBuilder(currentBatchNo);
                                    postingFileBuilder.createBatchFiles(corpus, totalDocs);
                                    System.out.println("Batch " + currentBatchNo + " written successfully");
                                    
                                    // Clear memory
                                    corpus.clear();
                                } catch (Exception e) {
                                    System.err.println("Error writing batch " + currentBatchNo);
                                    e.printStackTrace();
                                } finally {
                                    memorySemaphore.release();
                                }
                            });
                        } else {
                            memorySemaphore.release();
                        }
                    } catch (Exception e) {
                        System.err.println("Error processing batch " + currentBatchNo);
                        e.printStackTrace();
                        memorySemaphore.release();
                    }
                }));
            }

            // Wait for completion
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }

            // Final merging
            System.out.println("Starting final merge...");
            List<FileCollector.FileTriple> fileTriples = FileCollector.collectFileTriples(
                FileBuilder.VOC_DIR, 
                FileBuilder.POSTING_DIR,
                FileBuilder.DOC_DIR
            );

            List<String> vocabularyFiles = new ArrayList<>();
            List<String> postingFiles = new ArrayList<>();
            List<String> docFiles = new ArrayList<>();

            for (FileCollector.FileTriple triple : fileTriples) {
                vocabularyFiles.add(triple.vocabularyFilePath);
                postingFiles.add(triple.postingFilePath);
                docFiles.add(triple.docFilePath);
            }

            FileMerger.mergeFiles(FileManager.RESULT_DIR + File.separator + "CollectionIndex", 
                                 vocabularyFiles, postingFiles);

            System.out.println("Processing complete!");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            shutdownExecutors();
        }
    }

    private static void shutdownExecutors() {
        processingExecutor.shutdown();
        fileWritingExecutor.shutdown();
        try {
            if (!processingExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                processingExecutor.shutdownNow();
            }
            if (!fileWritingExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                fileWritingExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}