package com.search.utils;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileManager {

    private static final String PARENT_DIR    = System.getProperty("user.dir") + File.separator;
    private static final String RESOURCE_DIR  = PARENT_DIR + "src" + File.separator + "main" + File.separator + "resources" + File.separator;
    
    private static final int    DEFAULT_DEPTH = 10;
    
    public static String showFileChooser(boolean selectFile) {
        JFileChooser fileChooser = new JFileChooser();
    
        // Set the initial directory to resources, or fall back to the current working directory
        ensureDirectoryExists(RESOURCE_DIR);
        File initialDirectory = new File(RESOURCE_DIR);
        if (!initialDirectory.exists()) {
            System.out.println("Resources directory not found. Defaulting to current working directory.");
            initialDirectory = new File(System.getProperty("user.dir"));
        }
        fileChooser.setCurrentDirectory(initialDirectory);
    
        fileChooser.setFileSelectionMode(selectFile ? JFileChooser.FILES_ONLY : JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle(selectFile ? "Select a File" : "Select a Directory");
        fileChooser.setAcceptAllFileFilterUsed(false);
    
        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedDirectory = fileChooser.getSelectedFile();
            return selectedDirectory.getAbsolutePath();
        }
    
        System.out.println("No directory selected. Returning default path.");
        return RESOURCE_DIR;
    }
    

    // Search up to a certain depth
    public static List<String> getFilesInDirectory(String directoryPath) {
        String input = JOptionPane.showInputDialog("Enter the depth of subdirectories to search:");
        int depth = DEFAULT_DEPTH;

        if (input != null && !input.trim().isEmpty()) 
        {
            try {
                depth = Integer.parseInt(input.trim());
            } catch (NumberFormatException e) {
                depth = DEFAULT_DEPTH; 
            }
        }

        List<String> fileList = new ArrayList<>();
        exploreDirectory(new File(directoryPath), depth, 0, fileList);
        return fileList;
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

    private static boolean ensureDirectoryExists(String directoryPath) {
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
}
