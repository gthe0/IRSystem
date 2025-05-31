package com.search.indexer.utils;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

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
        
        ConcurrentLinkedQueue<String> vocabQueue = new ConcurrentLinkedQueue<>(vocabFiles);
        ConcurrentLinkedQueue<String> postQueue = new ConcurrentLinkedQueue<>(postingFiles);
        int mergeCount = 0;

        // Pairwise merge until only one file remains
        while (vocabQueue.size() > 1) {
            String vocab1 = vocabQueue.poll();
            String vocab2 = vocabQueue.poll();
            String post1  = postQueue.poll();
            String post2  = postQueue.poll();

            String mergedVocab = tempDir.resolve("vocab_merged_" + mergeCount + ".txt").toString();
            String mergedPost = tempDir.resolve("post_merged_" + mergeCount + ".txt").toString();
            mergeCount++;

            mergeTwoFiles(vocab1, post1, vocab2, post2, mergedVocab, mergedPost);
            
            vocabQueue.add(mergedVocab);
            postQueue.add(mergedPost);
        }

        // Move final files to result location
        Path finalVocab = Paths.get(resultPath, "VocabularyFile.txt");
        Path finalPosting = Paths.get(resultPath, "PostingFile.txt");
        Files.move(Paths.get(vocabQueue.poll()), finalVocab, StandardCopyOption.REPLACE_EXISTING);
        Files.move(Paths.get(postQueue.poll()), finalPosting, StandardCopyOption.REPLACE_EXISTING);

        // Cleanup temp directory
        FileManager.deleteDirectory(tempDir.toFile());
    }

    private static void mergeTwoFiles(String vocabPath1, String postPath1, 
                                     String vocabPath2, String postPath2,
                                     String mergedVocab, String mergedPost) throws IOException {
        try (BufferedReader reader1 = new BufferedReader(new FileReader(vocabPath1));
             BufferedReader reader2 = new BufferedReader(new FileReader(vocabPath2));
             RandomAccessFile raf1 = new RandomAccessFile(postPath1, "r");
             RandomAccessFile raf2 = new RandomAccessFile(postPath2, "r");
             BufferedWriter vocabWriter = new BufferedWriter(new FileWriter(mergedVocab));
             RandomAccessFile mergedRAF = new RandomAccessFile(mergedPost, "rw")) {
            
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
                    mergedPointer = processTerm(entry1, raf1, reader1, vocabWriter, mergedRAF, mergedPointer);
                    line1 = reader1.readLine();
                } else if (cmp > 0) {
                    mergedPointer = processTerm(entry2, raf2, reader2,vocabWriter, mergedRAF, mergedPointer);
                    line2 = reader2.readLine();
                } else {
                    long newPointer = mergedPointer;

                    // Read the number at the current pointers
                    raf1.seek(entry1.pointer);
                    raf2.seek(entry2.pointer);

                    long num1 = raf1.readLong();
                    long num2 = raf2.readLong();

                    if(num1 <= num2)
                    {
                        mergedPointer = copyPostingData(raf1, entry1.pointer, 
                                                      getNextPointer(reader1, entry1), 
                                                      mergedRAF, mergedPointer);
                        
                        mergedPointer = copyPostingData(raf2, entry2.pointer, 
                                                      getNextPointer(reader2, entry2), 
                                                      mergedRAF, mergedPointer);
                    }
                    else
                    {
                        mergedPointer = copyPostingData(raf2, entry2.pointer, 
                                                      getNextPointer(reader2, entry2), 
                                                      mergedRAF, mergedPointer);

                        mergedPointer = copyPostingData(raf1, entry1.pointer, 
                                                      getNextPointer(reader1, entry1), 
                                                      mergedRAF, mergedPointer);
                    }
                    
                    vocabWriter.write(entry1.term + " " + (entry1.df + entry2.df) + " " + newPointer + "\n");
                    
                    line1 = reader1.readLine();
                    line2 = reader2.readLine();
                }
            }

            // Process remaining terms
            while (line1 != null) {
                VocabEntry entry = parseVocabLine(line1);
                mergedPointer = processTerm(entry, raf1, reader1, vocabWriter, mergedRAF, mergedPointer);
                line1 = reader1.readLine();
            }
            
            while (line2 != null) {
                VocabEntry entry = parseVocabLine(line2);
                mergedPointer = processTerm(entry, raf2, reader2, vocabWriter, mergedRAF, mergedPointer);
                line2 = reader2.readLine();
            }
        }
    }

    private static long processTerm(VocabEntry entry, 
                                    RandomAccessFile sourceRAF,
                                    BufferedReader vocabReader,
                                    BufferedWriter vocabWriter,
                                    RandomAccessFile mergedRAF,
                                    long currentPointer) throws IOException {
        long newPointer = currentPointer;
        long nextPointer = getNextPointer(vocabReader, entry);
        
        vocabWriter.write(entry.term + " " + entry.df + " " + newPointer + "\n");
        return copyPostingData(sourceRAF, entry.pointer, nextPointer, mergedRAF, currentPointer);
    }

    private static long getNextPointer(BufferedReader reader, VocabEntry current) throws IOException {
        reader.mark(8192);
        String nextLine = reader.readLine();
        reader.reset();
        return nextLine != null ? parseVocabLine(nextLine).pointer : -1;
    }

    private static long copyPostingData(RandomAccessFile sourceRAF, 
                                       long startPointer, 
                                       long endPointer,
                                       RandomAccessFile targetRAF,
                                       long targetPosition) throws IOException {
        if (endPointer == -1) {
            long length = sourceRAF.length() - startPointer;
            return copyBytes(sourceRAF, startPointer, length, targetRAF, targetPosition);
        }

        long byteCount = endPointer - startPointer;
        return copyBytes(sourceRAF, startPointer, byteCount, targetRAF, targetPosition);
    }

    private static long copyBytes(RandomAccessFile source, long sourcePos, long length,
                                 RandomAccessFile target, long targetPos) throws IOException {
        source.seek(sourcePos);
        target.seek(targetPos);
        
        byte[] buffer = new byte[8192];
        long bytesRemaining = length;
        
        while (bytesRemaining > 0) {
            int bytesToRead = (int) Math.min(buffer.length, bytesRemaining);
            int bytesRead = source.read(buffer, 0, bytesToRead);
            if (bytesRead < 0) break;
            
            target.write(buffer, 0, bytesRead);
            bytesRemaining -= bytesRead;
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