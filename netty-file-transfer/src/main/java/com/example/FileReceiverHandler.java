package com.example;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Handles incoming file data and saves it.
 */
public class FileReceiverHandler extends ChannelInboundHandlerAdapter {

    private final String outputDirectory;
    private BufferedOutputStream bos;
    private File receivedFile;
    private long fileSize = -1;
    private long receivedBytes = 0;
    private boolean receivingFileSize = true;


    /**
     * Constructs a FileReceiverHandler.
     *
     * @param outputDirectory The directory to save received files.
     */
    public FileReceiverHandler(String outputDirectory) {
        this.outputDirectory = outputDirectory;
        File dir = new File(outputDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("Client connected: " + ctx.channel().remoteAddress());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws IOException {
        ByteBuf byteBuf = (ByteBuf) msg;
        try {
            if (receivingFileSize) {
                if (byteBuf.readableBytes() >= 8) { // long is 8 bytes
                    fileSize = byteBuf.readLong();
                    receivingFileSize = false;
                    receivedBytes = 0; // Reset for file content

                    // For simplicity, using a fixed name or generating one.
                    // A more robust solution would involve receiving the filename from the sender.
                    String fileName = "received_file_" + System.currentTimeMillis();
                    this.receivedFile = new File(outputDirectory, fileName);
                    this.bos = new BufferedOutputStream(new FileOutputStream(this.receivedFile));
                    System.out.println("Receiving file: " + this.receivedFile.getName() + ", Size: " + fileSize + " bytes");

                    if (byteBuf.readableBytes() > 0) {
                        // If there's more data in the current buffer, process it as file content
                        processFileContent(ctx, byteBuf);
                    }
                } else {
                    // Not enough data for file size, wait for more.
                    // This simple example assumes fileSize comes in one go.
                    // A more robust handler would buffer until 8 bytes are available.
                    return;
                }
            } else {
                 processFileContent(ctx, byteBuf);
            }
        } finally {
            byteBuf.release(); // Release the ByteBuf
        }
    }

    private void processFileContent(ChannelHandlerContext ctx, ByteBuf byteBuf) throws IOException {
        int readableBytes = byteBuf.readableBytes();
        byteBuf.readBytes(bos, readableBytes);
        receivedBytes += readableBytes;

        System.out.println("Received " + receivedBytes + " of " + fileSize + " bytes.");

        if (receivedBytes >= fileSize) {
            bos.flush();
            bos.close();
            bos = null;
            System.out.println("File reception completed: " + receivedFile.getAbsolutePath());
            // Reset for next file if the connection is kept alive
            receivingFileSize = true;
            fileSize = -1;
            receivedBytes = 0;
            // Optionally close the connection or wait for more files
            // ctx.close();
        }
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        if (bos != null) {
            try {
                bos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        ctx.close();
    }
}
