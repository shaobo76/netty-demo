package com.kilo.transfer.downstream.handler;

import com.kilo.transfer.common.message.FileChunkMessage;
import com.kilo.transfer.common.utils.HashUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.File;
import java.nio.file.StandardOpenOption;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ConcurrentHashMap;

public class DownstreamHandler extends SimpleChannelInboundHandler<FileChunkMessage> {

    private final String storagePath;
    private final ConcurrentHashMap<String, FileChannel> openFiles = new ConcurrentHashMap<>();

    public DownstreamHandler(String storagePath) {
        this.storagePath = storagePath;
        File storageDir = new File(storagePath);
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FileChunkMessage msg) throws Exception {
        // FR-5a: Chunk-level validation
        if (!validateChunk(msg)) {
            System.err.printf("Chunk validation failed for file %s, chunk %d. Discarding.%n", msg.getFileId(), msg.getChunkIndex());
            // TODO: Send retransmission request to upstream.
            return;
        }

        FileChannel channel = getFileChannel(msg.getFileId());
        channel.write(java.nio.ByteBuffer.wrap(msg.getData()));

        System.out.printf("Received and validated chunk %d/%d for file %s%n", msg.getChunkIndex() + 1, msg.getTotalChunks(), msg.getFileId());

        if (msg.isLast()) {
            System.out.printf("Finished receiving all chunks for file %s.%n", msg.getFileId());
            closeFileChannel(msg.getFileId());

            // FR-5b: File-level validation
            handleFileCompletion(ctx, msg);
        }
    }

    private boolean validateChunk(FileChunkMessage msg) {
        if (msg.getChunkHash() == null || msg.getChunkHash().isEmpty()) {
            return true; // No hash provided, skipping validation.
        }
        String actualHash = HashUtil.crc32(msg.getData());
        return actualHash.equalsIgnoreCase(msg.getChunkHash());
    }

    private void handleFileCompletion(ChannelHandlerContext ctx, FileChunkMessage msg) throws Exception {
        Path tempPath = Paths.get(storagePath, msg.getFileId() + ".tmp");
        Path finalPath = Paths.get(storagePath, msg.getFileId());

        String actualFileHash = HashUtil.sha256(tempPath);
        System.out.printf("Validating final file. Expected SHA-256: %s, Actual: %s%n", msg.getFileHash(), actualFileHash);

        if (actualFileHash.equalsIgnoreCase(msg.getFileHash())) {
            Files.move(tempPath, finalPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.printf("File %s successfully transferred and verified.%n", msg.getFileId());
            ctx.writeAndFlush("OK");
        } else {
            Files.delete(tempPath);
            System.err.printf("File validation failed for %s. Deleted temporary file.%n", msg.getFileId());
            ctx.writeAndFlush("FAIL");
        }
    }

    private FileChannel getFileChannel(String fileId) throws IOException {
        return openFiles.computeIfAbsent(fileId, key -> {
            try {
                // Store file with a temporary extension until fully validated.
                File tempFile = new File(storagePath, key + ".tmp");
                return FileChannel.open(tempFile.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                throw new RuntimeException("Failed to open file channel for " + key, e);
            }
        });
    }

    private void closeFileChannel(String fileId) throws IOException {
        FileChannel channel = openFiles.remove(fileId);
        if (channel != null && channel.isOpen()) {
            channel.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        // Clean up resources associated with the connection on error.
        openFiles.values().forEach(channel -> {
            if (channel.isOpen()) {
                try {
                    channel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        openFiles.clear();
        ctx.close();
    }
}