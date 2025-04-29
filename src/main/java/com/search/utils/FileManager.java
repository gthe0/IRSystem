package com.search.utils;

import javax.swing.*;
import java.io.*;

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
    public static FileBatchIterator getFileBatchIterator(File directory, int batchSize) {
        return new FileBatchIterator(directory, batchSize);
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
}
