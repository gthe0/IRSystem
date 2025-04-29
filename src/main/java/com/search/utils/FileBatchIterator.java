package com.search.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FileBatchIterator implements Iterator<List<File>> {
    private final List<File> allFiles; // List of all files (including from subdirectories)
    private final int batchSize;
    private int currentIndex;

    public FileBatchIterator(File directory, int batchSize) {
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("Provided file is not a directory!");
        }

        // Collect all files recursively
        this.allFiles = new ArrayList<>();
        collectFiles(directory, this.allFiles);

        this.batchSize = batchSize;
        this.currentIndex = 0;
    }

    private void collectFiles(File directory, List<File> fileList) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    fileList.add(file);
                } else if (file.isDirectory()) {
                    collectFiles(file, fileList); // Recursively explore subdirectories
                }
            }
        }
    }

    @Override
    public boolean hasNext() {
        return currentIndex < allFiles.size();
    }

    @Override
    public List<File> next() {
        int endIndex = Math.min(currentIndex + batchSize, allFiles.size());
        List<File> batch = new ArrayList<>(allFiles.subList(currentIndex, endIndex));
        currentIndex = endIndex;
        return batch;
    }
}
