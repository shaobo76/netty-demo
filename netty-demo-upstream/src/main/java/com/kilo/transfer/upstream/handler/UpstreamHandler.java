package com.kilo.transfer.upstream.handler;

import com.kilo.transfer.common.message.FileChunkMessage;
import com.kilo.transfer.common.utils.HashUtil;
import com.kilo.transfer.upstream.service.FileLockService;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

public class UpstreamHandler extends SimpleChannelInboundHandler<String> {

    private static final Logger logger = LoggerFactory.getLogger(UpstreamHandler.class);

    private final File file;
    private final int chunkSize;
    private final FileLockService fileLockService;
    private final AtomicInteger progress = new AtomicInteger(0);

    public UpstreamHandler(File file, int chunkSize, FileLockService fileLockService) {
        this.file = file;
        this.chunkSize = chunkSize;
        this.fileLockService = fileLockService;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info("Connection active. Starting transfer of file: {}", file.getName());
        Path filePath = file.toPath();

        fileLockService.scheduleLockUpdate(filePath, () -> {
            fileLockService.updateLock(filePath, progress.get());
        });

        String fileHash = HashUtil.sha256(filePath);
        long fileLength = Files.size(filePath);
        int totalChunks = (int) Math.ceil((double) fileLength / chunkSize);

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            for (int i = 0; i < totalChunks; i++) {
                long offset = (long) i * chunkSize;
                long length = Math.min(chunkSize, fileLength - offset);
                byte[] data = new byte[(int) length];
                raf.seek(offset);
                raf.readFully(data);

                FileChunkMessage chunk = new FileChunkMessage(
                    file.getName(),
                    fileHash,
                    "SHA-256",
                    totalChunks,
                    i,
                    HashUtil.crc32(data),
                    "CRC32",
                    data,
                    i == totalChunks - 1
                );

                double currentProgress = ((double) (i + 1) / totalChunks) * 100;
                progress.set((int) currentProgress);

                logger.info("Sending chunk {}/{} for file {} ({}%)", i + 1, totalChunks, file.getName(), String.format("%.2f", currentProgress));
                ctx.writeAndFlush(chunk).sync();
            }
        }
        logger.info("Finished sending all chunks for file {}. Waiting for confirmation...", file.getName());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        logger.info("Received confirmation from downstream: {}", msg);
        if ("OK".equalsIgnoreCase(msg)) {
            fileLockService.releaseLock(file.toPath(), true);
            logger.info("File transfer successful and lock released for: {}", file.getName());
        } else {
            fileLockService.releaseLock(file.toPath(), false);
            logger.warn("Received non-OK confirmation: {}. Lock released, file preserved.", msg);
        }
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Exception during file transfer for {}", file.getName(), cause);
        fileLockService.releaseLock(file.toPath(), false);
        ctx.close();
    }
}