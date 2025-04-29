package com.search;

import com.search.utils.FileManager;
import com.search.utils.StopWordManager;
import com.search.index.*;
import com.search.utils.FileBatchIterator;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {

    private static final int BATCH_SIZE = 0x200;
    private static final int NUM_PROCESSING_THREADS = Runtime.getRuntime().availableProcessors();
    private static final ExecutorService processingExecutor = Executors.newFixedThreadPool(NUM_PROCESSING_THREADS);
    private static final ExecutorService fileWritingExecutor = Executors.newSingleThreadExecutor();
    
    // Synchronization lock for document creation
    private static final Object documentCreationLock = new Object();

    public static void main(String[] args) {
        try {
            System.out.println("Select the directory containing stopword files:");
            File stopwordDirectory = FileManager.showFileChooserForDirectory();
            StopWordManager.loadStopWords(stopwordDirectory);

            System.out.println("Select the directory containing the XML documents:");
            File documentDirectory = FileManager.showFileChooserForDirectory();

            // Create a completion service for processing batches
            CompletionService<BatchResult> completionService = 
                new ExecutorCompletionService<>(processingExecutor);

            FileBatchIterator fileBatchIterator = FileManager.getFileBatchIterator(documentDirectory, BATCH_SIZE);
            AtomicInteger batchNo = new AtomicInteger(0);
            int totalBatches = 0;

            // Submit all batches for processing
            while (fileBatchIterator.hasNext()) {
                final List<Path> xmlFiles = fileBatchIterator.next();
                final int currentBatchNo = batchNo.getAndIncrement();
                
                completionService.submit(() -> {
                    try {
                        List<Document> documents = new ArrayList<>();
                        
                        for (Path xmlFile : xmlFiles) {
                            synchronized (documentCreationLock) {
                                try {
                                    Document doc = DocumentFactory.createDocument(xmlFile.toFile());
                                    documents.add(doc);
                                } catch (Exception e) {
                                    System.err.println("Error processing file in batch " + currentBatchNo + 
                                                     ": " + xmlFile);
                                    e.printStackTrace();
                                }
                            }
                        }
                        
                        if (!documents.isEmpty()) {
                            Corpus corpus = new Corpus();
                            corpus.addDocuments(documents);
                            return new BatchResult(currentBatchNo, corpus, documents.size());
                        }
                        return null;
                    } catch (Exception e) {
                        System.err.println("Error processing batch " + currentBatchNo);
                        e.printStackTrace();
                        return null;
                    }
                });
                totalBatches++;
            }

            // Process results as they complete and submit for file writing
            List<Future<?>> fileWriteFutures = new ArrayList<>();
            for (int i = 0; i < totalBatches; i++) {
                try {
                    Future<BatchResult> future = completionService.take();
                    BatchResult result = future.get();
                    if (result != null) {
                        // Submit file writing to single-threaded executor
                        fileWriteFutures.add(fileWritingExecutor.submit(() -> {
                            try {
                                FileBuilder postingFileBuilder = new FileBuilder(result.batchNo);
                                postingFileBuilder.createBatchFiles(result.corpus);
                                System.out.println("Batch " + result.batchNo + " (" + 
                                                 result.fileCount + " files) processed successfully.");
                            } catch (Exception e) {
                                System.err.println("Error writing batch " + result.batchNo);
                                e.printStackTrace();
                            }
                        }));
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }

            // Wait for all file writing to complete
            for (Future<?> future : fileWriteFutures) {
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }

            System.out.println("All batches processed successfully.");

            // Final merging (single-threaded)
            List<FileCollector.FilePair> filePairs = FileCollector.collectFilePairs(
                FileBuilder.VOC_DIR, 
                FileBuilder.POSTING_DIR
            );

            List<String> vocabularyFiles = new ArrayList<>();
            List<String> postingFiles = new ArrayList<>();

            for (FileCollector.FilePair pair : filePairs) {
                vocabularyFiles.add(pair.vocabularyFilePath);
                postingFiles.add(pair.postingFilePath);
            }

            FileMerger.mergeFiles(FileManager.RESULT_DIR + File.separator + "CollectionIndex", 
                                 vocabularyFiles, postingFiles);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
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
                processingExecutor.shutdownNow();
                fileWritingExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private static class BatchResult {
        final int batchNo;
        final Corpus corpus;
        final int fileCount;

        BatchResult(int batchNo, Corpus corpus, int fileCount) {
            this.batchNo = batchNo;
            this.corpus = corpus;
            this.fileCount = fileCount;
        }
    }
}