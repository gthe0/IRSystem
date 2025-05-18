package com.search.common.utils;

import javax.swing.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FileManager {

    public static final String PARENT_DIR = System.getProperty("user.dir") + File.separator;
    public static final String RESOURCE_DIR = PARENT_DIR + "src" + File.separator + "main" + File.separator + "resources" + File.separator;
    public static final String RESULT_DIR = PARENT_DIR + "src" + File.separator + "main" + File.separator + "results" + File.separator;

    // Shows a file or directory chooser based on mode
    private static File showFileChooser(int selectionMode, String dialogTitle) {
        JFileChooser fileChooser = new JFileChooser();

        // Set initial directory
        File initialDirectory = new File(RESOURCE_DIR);
        if (!ensureDirectoryExists(initialDirectory.getAbsolutePath())) {
            System.out.println("Resources directory not found. Defaulting to current working directory.");
            initialDirectory = new File(PARENT_DIR);
        }

        fileChooser.setCurrentDirectory(initialDirectory);
        fileChooser.setFileSelectionMode(selectionMode);
        fileChooser.setDialogTitle(dialogTitle);
        fileChooser.setAcceptAllFileFilterUsed(false);

        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile();
        }

        System.out.println("No selection made.");
        return null;
    }

    // Public method for file chooser
    public static File showFileChooserForFile() {
        return showFileChooser(JFileChooser.FILES_ONLY, "Select a File");
    }

    // Public method for directory chooser
    public static File showFileChooserForDirectory() {
        return showFileChooser(JFileChooser.DIRECTORIES_ONLY, "Select a Directory");
    }

    // Get a batch iterator for a directory
    public static FileBatchIterator getFileBatchIterator(File directory, int batchSize) throws IOException {
        return new FileBatchIterator(directory.getAbsolutePath() , batchSize);
    }

    // Ensures a directory exists or creates it
    public static boolean ensureDirectoryExists(String directoryPath) {
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            if (directory.mkdirs()) {
                System.out.println("Created directory: " + directoryPath);
                return true; // Directory created successfully
            } else {
                System.err.println("Failed to create directory: " + directoryPath);
                return false; // Directory creation failed
            }
        }
        return true; // Directory already exists
    }

    // Creates a file and writes content to it
    public static boolean createFile(File file, String content) {
        try {
            ensureDirectoryExists(file.getParent()); // Ensure parent directory exists
            if (file.createNewFile() || file.exists()) {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                    writer.write(content);
                    System.out.println("File created and content written: " + file.getAbsolutePath());
                }
                return true;
            } else {
                System.err.println("Failed to create file: " + file.getAbsolutePath());
                return false;
            }
        } catch (IOException e) {
            System.err.println("Error creating file: " + e.getMessage());
            return false;
        }
    }

    public static long countFilesInDir(Path startPath)
    {
        long count = 0;
        
        try {
            count = Files.walk(startPath)
                        .filter(Files::isRegularFile)
                        .count();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return count;
    }


    public static class LineResult {
        private final String line;
        private final long nextPosition;
        
        public LineResult(String line, long nextPosition) {
            this.line = line;
            this.nextPosition = nextPosition;
        }
        
        public String getLine() { return line; }
        public long getNextPosition() { return nextPosition; }
    }

    public static String readLineFromPosition(RandomAccessFile file, long position) throws IOException
    {
        return readLineFrom(file, position).getLine();
    }

    /**
     * Reads a single UTF-8 encoded line starting at the specified file position
     * @param file Opened RandomAccessFile instance
     * @param position Starting byte position in file
     * @return LineResult containing line text and next read position
     */
    private static LineResult readLineFrom(RandomAccessFile file, long position) 
        throws IOException {
        
        file.seek(position);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        long currentPosition = position;
        boolean foundCR = false;

        while (true) {
            int byteRead = file.read();
            if (byteRead == -1) break; // EOF
            
            currentPosition = file.getFilePointer();

            if (byteRead == '\n') { 
                return buildResult(buffer, foundCR, currentPosition);
            } else if (foundCR) {
                handleCRLF(file, buffer, byteRead, currentPosition);
                return buildResult(buffer, true, currentPosition);
            } else if (byteRead == '\r') { 
                foundCR = true;
                continue;
            }
            
            buffer.write(byteRead);
            foundCR = false;
        }

        return handleEOF(buffer, foundCR, currentPosition);
    }

    /**
     * Reads multiple UTF-8 encoded lines starting at specified position
     * @param file Opened RandomAccessFile instance
     * @param position Starting byte position
     * @param maxLines Maximum number of lines to read
     * @return List of read lines (may be shorter than maxLines if EOF reached)
     */
    public static List<String> readLinesFromPosition(RandomAccessFile file, long position, int maxLines) 
        throws IOException {
        
        List<String> results = new ArrayList<>();
        long currentPosition = position;
        int linesRead = 0;

        while (linesRead < maxLines) {
            LineResult result = readLineFrom(file, currentPosition);
            if (result == null) break;
            
            results.add(result.getLine());
            currentPosition = result.getNextPosition();
            linesRead++;
        }

        return results;
    }

    // Helper methods
    private static LineResult buildResult(ByteArrayOutputStream buffer, boolean foundCR, long pos) {
        if (foundCR) pos--; // Adjust for CR-only endings
        String line = buffer.toString(StandardCharsets.UTF_8);
        return new LineResult(line, pos);
    }

    private static void handleCRLF(RandomAccessFile file, ByteArrayOutputStream buffer, 
                                  int byteRead, long currentPos) throws IOException {
        if (byteRead != '\n') {
            file.seek(currentPos - 1); // Rewind for non-LF following CR
        }
    }

    private static LineResult handleEOF(ByteArrayOutputStream buffer, boolean foundCR, long pos) {
        if (buffer.size() == 0 && !foundCR) return null;
        if (foundCR) pos--; // Adjust for final CR
        return new LineResult(buffer.toString(StandardCharsets.UTF_8), pos);
    }
}
