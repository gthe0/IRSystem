package com.search.common.utils;

public class Timer {
    private long startTime;
    private long endTime;
    private boolean running;

    // Starts the timer
    public void start() {
        startTime = System.nanoTime();
        running = true;
    }

    // Stops the timer
    public void stop() {
        if (running) {
            endTime = System.nanoTime();
            running = false;
        } else {
            System.out.println("Timer was not running.");
        }
    }

    // Returns elapsed time in milliseconds
    public long getElapsedTimeMillis() {
        return (endTime - startTime) / 1_000_000;
    }

    // Returns elapsed time in seconds
    public double getElapsedTimeSeconds() {
        return (endTime - startTime) / 1_000_000_000.0;
    }
}
