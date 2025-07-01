package com.kilo.transfer.upstream.handler;

import com.kilo.transfer.common.message.FileChunkMessage;
import com.kilo.transfer.common.utils.HashUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;

public class UpstreamHandler extends ChannelInboundHandlerAdapter {

    private final File file;
    private final int chunkSize;

    public UpstreamHandler(File file, int chunkSize) {
        this.file = file;
        this.chunkSize = chunkSize;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.printf("Connection active. Starting transfer of file: %s%n", file.getName());
        
        Path filePath = file.toPath();
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

                FileChunkMessage chunk = new FileChunkMessage();
                chunk.setFileId(file.getName());
                chunk.setFileHash(fileHash);
                chunk.setHashAlgorithm("SHA-256");
                chunk.setTotalChunks(totalChunks);
                chunk.setChunkIndex(i);
                chunk.setData(data);
                chunk.setChunkHash(HashUtil.crc32(data));
                chunk.setChunkHashAlgorithm("CRC32");
                chunk.setLast(i == totalChunks - 1);
                
                System.out.printf("Sending chunk %d/%d for file %s%n", i + 1, totalChunks, file.getName());
                ctx.writeAndFlush(chunk).sync(); // Ensure chunks are sent sequentially.
            }
        }

        System.out.printf("Finished sending all chunks for file %s.%n", file.getName());
        // TODO: Wait for confirmation from downstream before closing.
        // For now, we close the channel after sending is done.
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}