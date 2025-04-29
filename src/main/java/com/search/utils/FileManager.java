package com.search.utils;

import javax.swing.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileManager {

    public static final String PARENT_DIR = System.getProperty("user.dir") + File.separator;
    public static final String RESOURCE_DIR = PARENT_DIR + "src" + File.separator + "main" + File.separator + "resources" + File.separator;
    public static final String RESULT_DIR = PARENT_DIR + "src" + File.separator + "main" + File.separator + "results" + File.separator;
    public static final int DEFAULT_DEPTH = 10;

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

    // Get all files in a directory up to a specified depth
    public static List<File> getFilesInDirectory(File directory) {
        int depth = getDirectoryDepth();
        List<File> fileList = new ArrayList<>();
        exploreDirectory(directory, depth, 0, fileList);
        return fileList;
    }

    // Prompt user for directory depth
    private static int getDirectoryDepth() {
        String input = JOptionPane.showInputDialog("Enter the depth of subdirectories to search:");
        int depth = DEFAULT_DEPTH;

        if (input != null && !input.trim().isEmpty()) {
            try {
                depth = Integer.parseInt(input.trim());
            } catch (NumberFormatException e) {
                System.err.println("Invalid input for depth. Using default value: " + DEFAULT_DEPTH);
            }
        }
        return depth;
    }

    // Recursive method to explore the directory and collect files
    private static void exploreDirectory(File directory, int maxDepth, int currentDepth, List<File> fileList) {
        if (currentDepth > maxDepth || !directory.isDirectory()) {
            return;
        }

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    fileList.add(file); // Add File directly
                } else if (file.isDirectory()) {
                    exploreDirectory(file, maxDepth, currentDepth + 1, fileList);
                }
            }
        }
    }

    // Ensures a directory exists or creates it
    public static boolean ensureDirectoryExists(String directoryPath) {
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            if (directory.mkdirs()) {
                System.out.println("Created directory: " + directoryPath);
                return true;
            } else {
                System.err.println("Failed to create directory: " + directoryPath);
                return false;
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
