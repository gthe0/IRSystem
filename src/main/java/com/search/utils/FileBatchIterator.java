package com.search.utils;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FileBatchIterator implements Iterator<List<Path>> {
    private final DirectoryStream<Path> stream;
    private final Iterator<Path> fileIterator;
    private final int batchSize;

    public FileBatchIterator(String directory, int batchSize) throws IOException {
        Path dirPath = Paths.get(directory);
        if (!Files.isDirectory(dirPath)) {
            throw new IllegalArgumentException("Provided path is not a directory!");
        }

        this.stream = Files.newDirectoryStream(dirPath);
        this.fileIterator = stream.iterator();
        this.batchSize = batchSize;
    }

    @Override
    public boolean hasNext() {
        return fileIterator.hasNext();
    }

    @Override
    public List<Path> next() {
        List<Path> batch = new ArrayList<>();
        for (int i = 0; i < batchSize && fileIterator.hasNext(); i++) {
            batch.add(fileIterator.next());
        }
        return batch;
    }
}
