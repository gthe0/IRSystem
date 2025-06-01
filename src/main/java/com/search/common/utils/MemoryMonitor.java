package com.search.common.utils;

public class MemoryMonitor {
    private static final long MEGABYTE = 1024 * 1024;
    private final Runtime runtime;
    private long initialMemory;
    private long peakMemory;
    private final String monitorName;

    public MemoryMonitor(String name) {
        this.runtime = Runtime.getRuntime();
        this.monitorName = name;
        reset();
    }

    public void reset() {
        runtime.gc();
        runtime.runFinalization();
        initialMemory = getUsedMemory();
        peakMemory = initialMemory;
    }

    public void recordPeak() {
        long current = getUsedMemory();
        if (current > peakMemory) {
            peakMemory = current;
        }
    }

    public void printUsage() {
        printUsage("");
    }

    public void printUsage(String context) {
        long current = getUsedMemory();
        long total = runtime.totalMemory();
        long max = runtime.maxMemory();

        System.out.printf("[%s] Memory Usage%s:%n", monitorName, context.isEmpty() ? "" : " (" + context + ")");
        System.out.printf("  Current:  %6.2f MB%n", bytesToMB(current));
        System.out.printf("  Initial:  %6.2f MB%n", bytesToMB(initialMemory));
        System.out.printf("  Net:      %6.2f MB%n", bytesToMB(current - initialMemory));
        System.out.printf("  Peak:     %6.2f MB%n", bytesToMB(peakMemory));
        System.out.printf("  Total:    %6.2f MB%n", bytesToMB(total));
        System.out.printf("  Max:      %6.2f MB%n", bytesToMB(max));
        System.out.printf("  Free:     %6.2f MB%n", bytesToMB(runtime.freeMemory()));
        System.out.println();
    }

    public void printStats() {
        System.out.printf("[%s] Memory Statistics:%n", monitorName);
        System.out.printf("  Peak Usage:    %6.2f MB%n", bytesToMB(peakMemory));
        System.out.printf("  Net Allocation:%6.2f MB%n", bytesToMB(peakMemory - initialMemory));
        System.out.println();
    }

    private long getUsedMemory() {
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private double bytesToMB(long bytes) {
        return bytes / (double) MEGABYTE;
    }
}