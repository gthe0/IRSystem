package com.search.utils;

import javax.swing.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileManager {

    public static final String PARENT_DIR   = System.getProperty("user.dir") + File.separator;
    public static final String RESOURCE_DIR = PARENT_DIR + "src" + File.separator + "main" + File.separator + "resources" + File.separator;
    public static final String RESULT_DIR   = PARENT_DIR + "src" + File.separator + "main" + File.separator + "results" + File.separator;
    public static final int DEFAULT_DEPTH   = 10;

    // Show file or directory chooser based on mode
    private static String showFileChooser(int selectionMode, String dialogTitle) {
        JFileChooser fileChooser = new JFileChooser();

        // Set up initial directory
        File initialDirectory = new File(RESOURCE_DIR);
        ensureDirectoryExists(initialDirectory.getAbsolutePath());
        if (!initialDirectory.exists()) {
            System.out.println("Resources directory not found. Defaulting to current working directory.");
            initialDirectory = new File(PARENT_DIR);
        }

        fileChooser.setCurrentDirectory(initialDirectory);
        fileChooser.setFileSelectionMode(selectionMode);
        fileChooser.setDialogTitle(dialogTitle);
        fileChooser.setAcceptAllFileFilterUsed(false);

        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile().getAbsolutePath();
        }

        System.out.println("No selection made. Returning default path.");
        return RESOURCE_DIR;
    }

    // Public method for file chooser
    public static String showFileChooserForFile() {
        return showFileChooser(JFileChooser.FILES_ONLY, "Select a File");
    }

    // Public method for directory chooser
    public static String showFileChooserForDirectory() {
        return showFileChooser(JFileChooser.DIRECTORIES_ONLY, "Select a Directory");
    }

    // Get all files in a directory, up to a specified depth
    public static List<String> getFilesInDirectory(String directoryPath) {
        int depth = getDirectoryDepth();
        List<String> fileList = new ArrayList<>();
        exploreDirectory(new File(directoryPath), depth, 0, fileList);
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

    private static void exploreDirectory(File directory, int maxDepth, int currentDepth, List<String> fileList) {
        if (currentDepth > maxDepth || !directory.isDirectory()) {
            return;
        }

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    fileList.add(file.getAbsolutePath());
                } else if (file.isDirectory()) {
                    exploreDirectory(file, maxDepth, currentDepth + 1, fileList);
                }
            }
        }
    }

    // Ensure a directory exists or create it
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

    // Create a file and write content to it
    public static boolean createFile(String filePath, String content) {
        try {
            File file = new File(filePath);
            ensureDirectoryExists(file.getParent()); // Ensure parent directory exists
            if (file.createNewFile() || file.exists()) {
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(content);
                    System.out.println("File created and content written: " + filePath);
                }
                return true;
            } else {
                System.err.println("Failed to create file: " + filePath);
                return false;
            }
        } catch (IOException e) {
            System.err.println("Error creating file: " + e.getMessage());
            return false;
        }
    }
}
