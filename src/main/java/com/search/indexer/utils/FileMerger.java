package com.search.indexer.utils;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.search.common.utils.FileManager;

public class FileMerger {

    public static void merge(List<String> vocabFiles, List<String> postingFiles, List<String> documentFiles, String resultPath) throws IOException {
        mergeVocAndPost(vocabFiles, postingFiles, resultPath);
        mergeDocumentFiles(documentFiles, resultPath);
    }

    public static void mergeVocAndPost(List<String> vocabFiles, List<String> postingFiles, String resultPath) throws IOException {
        if (vocabFiles.isEmpty()) return;

        // Create temp directory for intermediate files
        Path tempDir = Paths.get(resultPath, "temp_merge");
        Files.createDirectories(tempDir);
        
        // Initialize queues
        Queue<String> vocabQueue = new ConcurrentLinkedQueue<>(vocabFiles);
        Queue<String> postQueue = new ConcurrentLinkedQueue<>(postingFiles);
        
        // Thread pool setup
        int numThreads = Math.max(2, Runtime.getRuntime().availableProcessors());
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        AtomicInteger mergeCount = new AtomicInteger(0);
        
        try {
            while (vocabQueue.size() > 1) {
                int currentSize = vocabQueue.size();
                int numPairs = currentSize / 2;
                int leftover = currentSize % 2;
                
                // Process pairs concurrently
                List<Future<MergeResult>> futures = new ArrayList<>(numPairs);
                for (int i = 0; i < numPairs; i++) {
                    String vocab1 = vocabQueue.poll();
                    String vocab2 = vocabQueue.poll();
                    String post1 = postQueue.poll();
                    String post2 = postQueue.poll();
                    
                    String mergedVocab = tempDir.resolve("vocab_merged_" + mergeCount.get() + "_t" + i + ".txt").toString();
                    String mergedPost = tempDir.resolve("post_merged_" + mergeCount.get() + "_t" + i + ".txt").toString();
                    
                    futures.add(executor.submit(() -> {
                        mergeTwoFiles(vocab1, post1, vocab2, post2, mergedVocab, mergedPost);
                        return new MergeResult(mergedVocab, mergedPost);
                    }));
                }
                
                // Handle leftover file
                if (leftover > 0) {
                    vocabQueue.add(vocabQueue.poll());
                    postQueue.add(postQueue.poll());
                }
                
                // Collect results
                for (Future<MergeResult> future : futures) {
                    MergeResult result = future.get();
                    vocabQueue.add(result.mergedVocabPath);
                    postQueue.add(result.mergedPostPath);
                }
                mergeCount.incrementAndGet();
            }
            
            // Move final files to result location
            Path finalVocab = Paths.get(resultPath, "VocabularyFile.txt");
            Path finalPosting = Paths.get(resultPath, "PostingFile.txt");
            Files.move(Paths.get(vocabQueue.poll()), finalVocab, StandardCopyOption.REPLACE_EXISTING);
            Files.move(Paths.get(postQueue.poll()), finalPosting, StandardCopyOption.REPLACE_EXISTING);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Merge interrupted", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException) throw (IOException) cause;
            throw new IOException("Merge error", cause);
        } finally {
            executor.shutdownNow();
            FileManager.deleteDirectory(tempDir.toFile());
        }
    }

    private static class MergeResult {
        final String mergedVocabPath;
        final String mergedPostPath;

        MergeResult(String mergedVocabPath, String mergedPostPath) {
            this.mergedVocabPath = mergedVocabPath;
            this.mergedPostPath = mergedPostPath;
        }
    }

    private static void mergeTwoFiles(String vocabPath1, String postPath1, 
                                     String vocabPath2, String postPath2,
                                     String mergedVocab, String mergedPost) throws IOException {
        try (BufferedReader reader1 = new BufferedReader(new FileReader(vocabPath1));
             BufferedReader reader2 = new BufferedReader(new FileReader(vocabPath2));
             FileChannel postChannel1 = FileChannel.open(Paths.get(postPath1), StandardOpenOption.READ);
             FileChannel postChannel2 = FileChannel.open(Paths.get(postPath2), StandardOpenOption.READ);
             BufferedWriter vocabWriter = new BufferedWriter(new FileWriter(mergedVocab));
             FileChannel mergedPostChannel = FileChannel.open(Paths.get(mergedPost), 
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            
            // Initialize pointers
            String line1 = reader1.readLine();
            String line2 = reader2.readLine();
            long mergedPointer = 0;

            while (line1 != null && line2 != null) {
                // Parse vocab entries
                VocabEntry entry1 = parseVocabLine(line1);
                VocabEntry entry2 = parseVocabLine(line2);
                
                int cmp = entry1.term.compareTo(entry2.term);
                
                if (cmp < 0) {
                    mergedPointer = processTerm(entry1, postChannel1, reader1, vocabWriter, mergedPostChannel, mergedPointer);
                    line1 = reader1.readLine();
                } else if (cmp > 0) {
                    mergedPointer = processTerm(entry2, postChannel2, reader2, vocabWriter, mergedPostChannel, mergedPointer);
                    line2 = reader2.readLine();
                } else {
                    long newPointer = mergedPointer;

                    // Read first document ID to determine merge order
                    long docId1 = readFirstDocId(postChannel1, entry1.pointer);
                    long docId2 = readFirstDocId(postChannel2, entry2.pointer);

                    if (docId1 <= docId2) {
                        mergedPointer = copyPostingData(postChannel1, entry1.pointer, 
                                                       getNextPointer(reader1, entry1), 
                                                       mergedPostChannel, mergedPointer);
                        
                        mergedPointer = copyPostingData(postChannel2, entry2.pointer, 
                                                       getNextPointer(reader2, entry2), 
                                                       mergedPostChannel, mergedPointer);
                    } else {
                        mergedPointer = copyPostingData(postChannel2, entry2.pointer, 
                                                       getNextPointer(reader2, entry2), 
                                                       mergedPostChannel, mergedPointer);
                        
                        mergedPointer = copyPostingData(postChannel1, entry1.pointer, 
                                                       getNextPointer(reader1, entry1), 
                                                       mergedPostChannel, mergedPointer);
                    }
                    
                    vocabWriter.write(entry1.term + " " + (entry1.df + entry2.df) + " " + newPointer + "\n");
                    
                    line1 = reader1.readLine();
                    line2 = reader2.readLine();
                }
            }

            // Process remaining terms
            while (line1 != null) {
                VocabEntry entry = parseVocabLine(line1);
                mergedPointer = processTerm(entry, postChannel1, reader1, vocabWriter, mergedPostChannel, mergedPointer);
                line1 = reader1.readLine();
            }
            
            while (line2 != null) {
                VocabEntry entry = parseVocabLine(line2);
                mergedPointer = processTerm(entry, postChannel2, reader2, vocabWriter, mergedPostChannel, mergedPointer);
                line2 = reader2.readLine();
            }
        }
    }

    private static long readFirstDocId(FileChannel channel, long position) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        channel.read(buffer, position);
        buffer.flip();
        return buffer.getLong();
    }

    private static long processTerm(VocabEntry entry, 
                                    FileChannel sourceChannel,
                                    BufferedReader vocabReader,
                                    BufferedWriter vocabWriter,
                                    FileChannel mergedChannel,
                                    long currentPointer) throws IOException {
        long newPointer = currentPointer;
        long nextPointer = getNextPointer(vocabReader, entry);
        
        vocabWriter.write(entry.term + " " + entry.df + " " + newPointer + "\n");
        return copyPostingData(sourceChannel, entry.pointer, nextPointer, mergedChannel, currentPointer);
    }

    private static long getNextPointer(BufferedReader reader, VocabEntry current) throws IOException {
        reader.mark(8192);
        String nextLine = reader.readLine();
        reader.reset();
        return nextLine != null ? parseVocabLine(nextLine).pointer : -1;
    }

    private static long copyPostingData(FileChannel sourceChannel, 
                                       long startPointer, 
                                       long endPointer,
                                       FileChannel targetChannel,
                                       long targetPosition) throws IOException {
        long length = (endPointer == -1) ? sourceChannel.size() - startPointer : endPointer - startPointer;
        return copyBytes(sourceChannel, startPointer, length, targetChannel, targetPosition);
    }

    private static long copyBytes(FileChannel source, long sourcePos, long length,
                                 FileChannel target, long targetPos) throws IOException {
        long transferred = 0;
        while (transferred < length) {
            transferred += source.transferTo(sourcePos + transferred, length - transferred, target);
        }
        return targetPos + length;
    }

    private static VocabEntry parseVocabLine(String line) {
        String[] parts = line.split(" ");
        return new VocabEntry(
            parts[0],
            Integer.parseInt(parts[1]),
            Long.parseLong(parts[2])
        );
    }

    // Lightweight helper class
    private static class VocabEntry {
        final String term;
        final int df;
        final long pointer;

        VocabEntry(String term, int df, long pointer) {
            this.term = term;
            this.df = df;
            this.pointer = pointer;
        }
    }

    public static void mergeDocumentFiles(List<String> documentFiles, String resultPath) throws IOException {
        String mergedDocPath = resultPath + File.separator + "DocumentFile.txt";
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(mergedDocPath))) {
            for (String docFile : documentFiles) {
                appendFileContents(docFile, writer);
            }
        }
    }

    private static void appendFileContents(String filePath, BufferedWriter writer) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error processing " + filePath + ": " + e.getMessage());
        }
    }
}