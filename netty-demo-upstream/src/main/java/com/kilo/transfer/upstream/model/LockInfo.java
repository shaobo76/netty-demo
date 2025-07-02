package com.kilo.transfer.upstream.model;

public class LockInfo {
    private double progress;
    private long timestamp;
    private String owner;

    // Default constructor for JSON deserialization
    public LockInfo() {
    }

    public LockInfo(double progress, long timestamp, String owner) {
        this.progress = progress;
        this.timestamp = timestamp;
        this.owner = owner;
    }

    public double getProgress() {
        return progress;
    }

    public void setProgress(double progress) {
        this.progress = progress;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    @Override
    public String toString() {
        return "LockInfo{" +
                "progress=" + progress +
                ", timestamp=" + timestamp +
                ", owner='" + owner + '\'' +
                '}';
    }
}