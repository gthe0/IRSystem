package com.search.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;

public class StopWordManager {
    private static HashSet<String> stopWords = new HashSet<>();

    // Load stop words from files
    public static void loadStopWords(String directoryPath) throws IOException {
        File stopwordDir = new File(directoryPath);
        File[] files = stopwordDir.listFiles((dir, name) -> name.endsWith(".txt")); // Filter only .txt files

        if (files != null) {
            for (File file : files) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stopWords.add(line.trim().toLowerCase()); // Add stopword in lowercase
                    }
                }
            }
        } else {
            throw new IOException("No stopword files found in the directory: " + directoryPath);
        }
    }

    public static HashSet<String> getStopWords() {
        return stopWords;
    }
}
