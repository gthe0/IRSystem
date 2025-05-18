package com.search.indexer.utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import com.search.common.utils.FileManager;

public class FileMerger {
    private static final String MERGED_VOCABULARY_FILE = "VocabularyFile_Merged.txt";
    private static final String MERGED_POSTING_FILE = "PostingFile_Merged.txt";
    private static final String MERGED_DOCUMENT_FILE = "DocumentFile_Merged.txt";

    public static void mergeFiles(String outputDir,
                                 List<String> vocabularyFilePaths,
                                 List<String> postingFilePaths,
                                 List<String> documentFilePaths) throws IOException {
        FileManager.ensureDirectoryExists(outputDir);

        Queue<FilePair> queue = new LinkedList<>();
        for (int i = 0; i < vocabularyFilePaths.size(); i++) {
            queue.add(new FilePair(
                new File(vocabularyFilePaths.get(i)),
                new File(postingFilePaths.get(i))
            ));
        }

        // Merge vocabulary and posting files pairwise
        int mergeCount = 0;
        while (queue.size() > 1) {
            FilePair pair1 = queue.poll();
            FilePair pair2 = queue.poll();

            File mergedVocab = new File(outputDir, "temp_vocab_" + mergeCount + ".txt");
            File mergedPost = new File(outputDir, "temp_post_" + mergeCount + ".txt");
            mergePair(pair1.vocabFile, pair1.postingFile,
                      pair2.vocabFile, pair2.postingFile,
                      mergedVocab, mergedPost);

            queue.add(new FilePair(mergedVocab, mergedPost));
            mergeCount++;
        }

        // Final merged files
        FilePair finalPair = queue.poll();
        File mergedVocabFile = new File(outputDir, MERGED_VOCABULARY_FILE);
        File mergedPostFile = new File(outputDir, MERGED_POSTING_FILE);
        finalPair.vocabFile.renameTo(mergedVocabFile);
        finalPair.postingFile.renameTo(mergedPostFile);

        // Merge document files
        mergeDocumentFiles(documentFilePaths, new File(outputDir, MERGED_DOCUMENT_FILE));

        System.out.println("Merged files successfully!");
    }

    private static void mergePair(File vocabFile1, File postingFile1,
                                 File vocabFile2, File postingFile2,
                                 File mergedVocabFile, File mergedPostingFile) throws IOException {
        try (BufferedReader reader1 = new BufferedReader(new FileReader(vocabFile1, StandardCharsets.UTF_8));
             BufferedReader reader2 = new BufferedReader(new FileReader(vocabFile2, StandardCharsets.UTF_8));
             BufferedWriter vocabWriter = new BufferedWriter(new FileWriter(mergedVocabFile, StandardCharsets.UTF_8));
             BufferedWriter postWriter = new BufferedWriter(new FileWriter(mergedPostingFile, StandardCharsets.UTF_8))) {

            String line1 = reader1.readLine();
            String line2 = reader2.readLine();
            long currentPointer = 0;

            while (line1 != null || line2 != null) {
                String term1 = getTerm(line1);
                String term2 = getTerm(line2);

                if (term1 == null) {
                    processTerm(line2, postingFile2, postWriter, vocabWriter, currentPointer);
                    currentPointer += getCharsWritten(line2, postingFile2);
                    line2 = reader2.readLine();
                } else if (term2 == null) {
                    processTerm(line1, postingFile1, postWriter, vocabWriter, currentPointer);
                    currentPointer += getCharsWritten(line1, postingFile1);
                    line1 = reader1.readLine();
                } else {
                    int cmp = term1.compareTo(term2);
                    if (cmp < 0) {
                        processTerm(line1, postingFile1, postWriter, vocabWriter, currentPointer);
                        currentPointer += getCharsWritten(line1, postingFile1);
                        line1 = reader1.readLine();
                    } else if (cmp > 0) {
                        processTerm(line2, postingFile2, postWriter, vocabWriter, currentPointer);
                        currentPointer += getCharsWritten(line2, postingFile2);
                        line2 = reader2.readLine();
                    } else {
                        currentPointer = mergeSameTerms(line1, postingFile1, line2, postingFile2,
                                postWriter, vocabWriter, currentPointer);
                        line1 = reader1.readLine();
                        line2 = reader2.readLine();
                    }
                }
            }
        }
    }

    private static String getTerm(String line) {
        return line == null ? null : line.split(" ")[0];
    }

    private static void processTerm(String line, File postingFile,
                                    BufferedWriter postWriter, BufferedWriter vocabWriter,
                                    long currentPointer) throws IOException {
        String[] parts = line.split(" ");
        String term = parts[0];
        int df = Integer.parseInt(parts[1]);
        long pointer = Long.parseLong(parts[2]);

        long charsWritten = copyPostings(postingFile, pointer, df, postWriter);
        vocabWriter.write(term + " " + df + " " + currentPointer);
        vocabWriter.newLine();
    }

    private static long getCharsWritten(String line, File postingFile) throws IOException {
        String[] parts = line.split(" ");
        int df = Integer.parseInt(parts[1]);
        long pointer = Long.parseLong(parts[2]);
        long totalChars = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(postingFile, StandardCharsets.UTF_8))) {
            reader.skip(pointer);
            for (int i = 0; i < df; i++) {
                String postLine = reader.readLine();
                if (postLine == null) break;
                totalChars += postLine.length() + 1;
            }
        }
        return totalChars;
    }

    private static long mergeSameTerms(String line1, File postFile1,
                                      String line2, File postFile2,
                                      BufferedWriter postWriter, BufferedWriter vocabWriter,
                                      long currentPointer) throws IOException {
        String[] parts1 = line1.split(" ");
        String[] parts2 = line2.split(" ");
        String term = parts1[0];
        int df1 = Integer.parseInt(parts1[1]);
        int df2 = Integer.parseInt(parts2[1]);
        long ptr1 = Long.parseLong(parts1[2]);
        long ptr2 = Long.parseLong(parts2[2]);

        long startPointer = currentPointer;
        long charsWritten = mergePostings(postFile1, ptr1, df1, postFile2, ptr2, df2, postWriter);
        vocabWriter.write(term + " " + (df1 + df2) + " " + startPointer);
        vocabWriter.newLine();

        return startPointer + charsWritten;
    }

    private static long copyPostings(File postingFile, long pointer, int df,
                                    BufferedWriter postWriter) throws IOException {
        long charsWritten = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(postingFile, StandardCharsets.UTF_8))) {
            reader.skip(pointer);
            for (int i = 0; i < df; i++) {
                String line = reader.readLine();
                if (line == null) break;
                postWriter.write(line);
                postWriter.newLine();
                charsWritten += line.length() + 1;
            }
        }
        return charsWritten;
    }

    private static long mergePostings(File postFile1, long ptr1, int df1,
                                     File postFile2, long ptr2, int df2,
                                     BufferedWriter mergedWriter) throws IOException {
        long charsWritten = 0;
        try (BufferedReader reader1 = new BufferedReader(new FileReader(postFile1, StandardCharsets.UTF_8));
             BufferedReader reader2 = new BufferedReader(new FileReader(postFile2, StandardCharsets.UTF_8))) {
            reader1.skip(ptr1);
            reader2.skip(ptr2);

            String line1 = readPostingLine(reader1, df1);
            String line2 = readPostingLine(reader2, df2);
            int count1 = 0, count2 = 0;

            while (line1 != null || line2 != null) {
                if (line1 == null) {
                    mergedWriter.write(line2);
                    mergedWriter.newLine();
                    charsWritten += line2.length() + 1;
                    line2 = readPostingLine(reader2, df2 - count2 - 1);
                    count2++;
                } else if (line2 == null) {
                    mergedWriter.write(line1);
                    mergedWriter.newLine();
                    charsWritten += line1.length() + 1;
                    line1 = readPostingLine(reader1, df1 - count1 - 1);
                    count1++;
                } else {
                    int docId1 = Integer.parseInt(line1.split(" ")[0]);
                    int docId2 = Integer.parseInt(line2.split(" ")[0]);
                    if (docId1 <= docId2) {
                        mergedWriter.write(line1);
                        mergedWriter.newLine();
                        charsWritten += line1.length() + 1;
                        line1 = readPostingLine(reader1, df1 - count1 - 1);
                        count1++;
                    } else {
                        mergedWriter.write(line2);
                        mergedWriter.newLine();
                        charsWritten += line2.length() + 1;
                        line2 = readPostingLine(reader2, df2 - count2 - 1);
                        count2++;
                    }
                }
            }
        }
        return charsWritten;
    }

    private static String readPostingLine(BufferedReader reader, int remaining) throws IOException {
        if (remaining <= 0) return null;
        return reader.readLine();
    }

    private static void mergeDocumentFiles(List<String> documentFilePaths, File mergedDocFile) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(mergedDocFile, StandardCharsets.UTF_8))) {
            for (String docFile : documentFilePaths) {
                try (BufferedReader reader = new BufferedReader(new FileReader(docFile, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        writer.write(line);
                        writer.newLine();
                    }
                }
            }
        }
    }

    private static class FilePair {
        File vocabFile;
        File postingFile;

        FilePair(File vocabFile, File postingFile) {
            this.vocabFile = vocabFile;
            this.postingFile = postingFile;
        }
    }
}