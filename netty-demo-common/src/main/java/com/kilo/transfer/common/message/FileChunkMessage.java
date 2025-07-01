package com.kilo.transfer.common.message;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Represents a chunk of a file for transfer.
 * This object is sent from the Upstream Server to the Downstream Server.
 */
public class FileChunkMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    // File-level metadata
    private String fileId;
    private String fileHash;
    private String hashAlgorithm;
    private int totalChunks;

    // Chunk-level metadata
    private int chunkIndex;
    private String chunkHash;
    private String chunkHashAlgorithm;
    private byte[] data;
    private boolean isLast;

    // Getters and Setters

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    public String getHashAlgorithm() {
        return hashAlgorithm;
    }

    public void setHashAlgorithm(String hashAlgorithm) {
        this.hashAlgorithm = hashAlgorithm;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public void setTotalChunks(int totalChunks) {
        this.totalChunks = totalChunks;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(int chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public String getChunkHash() {
        return chunkHash;
    }

    public void setChunkHash(String chunkHash) {
        this.chunkHash = chunkHash;
    }

    public String getChunkHashAlgorithm() {
        return chunkHashAlgorithm;
    }

    public void setChunkHashAlgorithm(String chunkHashAlgorithm) {
        this.chunkHashAlgorithm = chunkHashAlgorithm;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public boolean isLast() {
        return isLast;
    }

    public void setLast(boolean last) {
        isLast = last;
    }

    @Override
    public String toString() {
        return "FileChunkMessage{" +
                "fileId='" + fileId + '\'' +
                ", totalChunks=" + totalChunks +
                ", chunkIndex=" + chunkIndex +
                ", data.length=" + (data != null ? data.length : 0) +
                ", isLast=" + isLast +
                '}';
    }
}