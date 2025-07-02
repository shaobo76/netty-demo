package com.kilo.transfer.common.message;

import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Represents a chunk of a file for transfer.
 * This object is sent from the Upstream Server to the Downstream Server.
 * This class is immutable to ensure thread safety in a concurrent environment like Netty.
 */
public final class FileChunkMessage {

    // File-level metadata
    private final String fileId;
    private final String fileHash;
    private final String hashAlgorithm;
    private final int totalChunks;

    // Chunk-level metadata
    private final int chunkIndex;
    private final String chunkHash;
    private final String chunkHashAlgorithm;
    private final byte[] data;
    private final boolean isLast;

    public FileChunkMessage(String fileId,
                            String fileHash,
                            String hashAlgorithm,
                            int totalChunks,
                            int chunkIndex,
                            String chunkHash,
                            String chunkHashAlgorithm,
                            byte[] data,
                            boolean isLast) {
        this.fileId = fileId;
        this.fileHash = fileHash;
        this.hashAlgorithm = hashAlgorithm;
        this.totalChunks = totalChunks;
        this.chunkIndex = chunkIndex;
        this.chunkHash = chunkHash;
        this.chunkHashAlgorithm = chunkHashAlgorithm;
        // Defensive copy to ensure immutability of the byte array
        this.data = data != null ? Arrays.copyOf(data, data.length) : null;
        this.isLast = isLast;
    }

    // Deserialization constructor
    public FileChunkMessage(ByteBuf in) {
        this.fileId = readString(in);
        this.fileHash = readString(in);
        this.hashAlgorithm = readString(in);
        this.totalChunks = in.readInt();
        this.chunkIndex = in.readInt();
        this.chunkHash = readString(in);
        this.chunkHashAlgorithm = readString(in);
        int dataLength = in.readInt();
        this.data = new byte[dataLength];
        in.readBytes(this.data);
        this.isLast = in.readBoolean();
    }

    // Serialization method
    public void toByteBuf(ByteBuf out) {
        writeString(out, fileId);
        writeString(out, fileHash);
        writeString(out, hashAlgorithm);
        out.writeInt(totalChunks);
        out.writeInt(chunkIndex);
        writeString(out, chunkHash);
        writeString(out, chunkHashAlgorithm);
        out.writeInt(data.length);
        out.writeBytes(data);
        out.writeBoolean(isLast);
    }

    private void writeString(ByteBuf out, String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.writeBytes(bytes);
    }

    private String readString(ByteBuf in) {
        int length = in.readInt();
        byte[] bytes = new byte[length];
        in.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    // Getters

    public String getFileId() {
        return fileId;
    }

    public String getFileHash() {
        return fileHash;
    }

    public String getHashAlgorithm() {
        return hashAlgorithm;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public String getChunkHash() {
        return chunkHash;
    }

    public String getChunkHashAlgorithm() {
        return chunkHashAlgorithm;
    }

    /**
     * Returns a copy of the data to maintain immutability.
     *
     * @return A copy of the chunk's byte data.
     */
    public byte[] getData() {
        // Return a copy to prevent external modification of the internal state
        return data != null ? Arrays.copyOf(data, data.length) : null;
    }

    public boolean isLast() {
        return isLast;
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