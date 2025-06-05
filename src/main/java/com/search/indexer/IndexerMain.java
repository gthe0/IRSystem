package com.search.indexer;

import com.search.common.utils.FileBatchIterator;
import com.search.common.utils.FileManager;
import com.search.common.utils.StopWordManager;
import com.search.common.utils.Timer;
import com.search.indexer.utils.FileBuilder;
import com.search.indexer.utils.FileMerger;
import com.search.indexer.utils.VectorNormCalculator;
import com.search.indexer.model.Corpus;
import com.search.indexer.model.Document;
import com.search.indexer.model.DocumentFactory;
import com.search.indexer.utils.FileBatchCollector;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;


public class IndexerMain {

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

            FileBatchIterator fileBatchIterator = FileManager.getFileBatchIterator(documentDirectory, BATCH_SIZE);
            List<Future<?>> futures = new ArrayList<>();

            FileBatchCollector fileBatchCollector = new FileBatchCollector();
            Timer timer  = new Timer();

            timer.start();
            
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
                                    fileBatchCollector.add(postingFileBuilder.createBatchFiles(corpus));
                                    System.out.println("Batch " + currentBatchNo + " written successfully");
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

            // Wait for processing tasks to complete
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }

            // Wait for all file writing tasks to finish
            fileWritingExecutor.shutdown(); // Prevent new tasks

            timer.stop();

            
            try {
                // Wait indefinitely for existing tasks to complete
                if (!fileWritingExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
                    System.err.println("File writing tasks did not complete!");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            System.out.println("Partial indexing of " + documentDirectory + " is done in " + timer.getElapsedTimeSeconds() + " sec");

            timer.start();
            String resultDir = FileManager.RESULT_DIR + File.separator + "CollectionIndex";
            FileManager.ensureDirectoryExists(resultDir);
            FileMerger.merge(
                fileBatchCollector.getVocabPaths(),
                fileBatchCollector.getPostingsPaths(), 
                fileBatchCollector.getDocPaths(), 
                resultDir
            );
            timer.stop();
            System.out.println("Merging of partial files was done in " + timer.getElapsedTimeSeconds() + " sec");

            timer.start();
            VectorNormCalculator vec = new VectorNormCalculator(resultDir);
            vec.calculateAndUpdateNorms();
            timer.stop();

            FileManager.deleteDirectory(new File(FileBuilder.POSTING_DIR));
            FileManager.deleteDirectory(new File(FileBuilder.VOC_DIR)); 
            FileManager.deleteDirectory(new File(FileBuilder.DOC_DIR)); 

            System.out.println("Vector Norm caclulation was done in " + timer.getElapsedTimeSeconds() + " sec");
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
