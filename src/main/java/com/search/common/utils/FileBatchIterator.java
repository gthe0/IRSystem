package com.search.common.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;

public class FileBatchIterator implements Iterator<List<Path>> {
    private final Queue<Path> fileQueue; // Queue to store file paths
    private final int batchSize;

    public FileBatchIterator(String directory, int batchSize) throws IOException {
        Path dirPath = Paths.get(directory);
        if (!Files.isDirectory(dirPath)) {
            throw new IllegalArgumentException("Provided path is not a directory!");
        }

        this.fileQueue = new ArrayDeque<>();
        collectFiles(dirPath, fileQueue); // Collect files recursively
        this.batchSize = batchSize;
    }

    private void collectFiles(Path directory, Queue<Path> fileQueue) throws IOException {
        // Walk through the directory tree recursively
        Files.walk(directory)
                .filter(Files::isRegularFile) // Add only files (exclude directories)
                .forEach(fileQueue::offer); // Enqueue each file path
    }

    @Override
    public boolean hasNext() {
        return !fileQueue.isEmpty();
    }

    @Override
    public List<Path> next() {
        List<Path> batch = new ArrayList<>();
        for (int i = 0; i < batchSize && !fileQueue.isEmpty(); i++) {
            batch.add(fileQueue.poll()); // Dequeue file paths for the batch
        }
        return batch;
    }
}
