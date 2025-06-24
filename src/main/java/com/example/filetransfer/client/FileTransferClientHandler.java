package com.example.filetransfer.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 客户端的核心业务逻辑处理器。
 * 负责发送文件名请求，并接收文件内容写入本地磁盘。
 */
public class FileTransferClientHandler extends ChannelInboundHandlerAdapter {

    private final String fileName;
    private final String savePath;

    private FileOutputStream fos;
    private long fileLength;
    private long receivedLength;
    private State state = State.WAITING_METADATA;

    private enum State {
        WAITING_METADATA,
        RECEIVING_FILE,
        ERROR
    }

    public FileTransferClientHandler(String fileName, String savePath) {
        this.fileName = fileName;
        this.savePath = savePath;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        // 连接建立后，立即发送文件名请求
        // 我们在文件名后添加换行符，以匹配服务器端的StringDecoder配置
        System.out.println("Connected to server. Requesting file: " + fileName);
        ctx.writeAndFlush(fileName + "\n");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;

        switch (state) {
            case WAITING_METADATA:
                if (buf.readableBytes() < 8) {
                    // 等待直到接收到完整的long（8字节）
                    return;
                }

                fileLength = buf.readLong();
                if (fileLength == -1) {
                    // 服务器返回错误
                    state = State.ERROR;
                    System.err.println("Server returned an error: File not found or access denied.");
                    // 后续的字节可能是错误信息，这里我们简单地关闭连接
                    ctx.close();
                    return;
                }

                System.out.println("File size received: " + fileLength + " bytes. Starting download...");
                File file = new File(savePath);
                // 确保父目录存在
                if (!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }
                fos = new FileOutputStream(file);
                state = State.RECEIVING_FILE;
                // fall-through to process the rest of the buffer
            
            case RECEIVING_FILE:
                int readableBytes = buf.readableBytes();
                receivedLength += readableBytes;
                
                // 将接收到的字节写入文件
                buf.readBytes(fos, readableBytes);

                // 打印下载进度
                int progress = (int) (receivedLength * 100 / fileLength);
                System.out.print("Download progress: " + progress + "%\r");

                if (receivedLength >= fileLength) {
                    System.out.println("\nFile download completed successfully. Saved to: " + savePath);
                    ctx.close();
                }
                break;

            case ERROR:
                // 在错误状态下，忽略所有后续数据
                buf.release();
                return;
        }
        // 释放ByteBuf
        // buf.release(); // SimpleChannelInboundHandler会自动释放，但我们用的是Adapter，需要手动释放
        // 在这个逻辑中，我们通过readBytes消费了buf，所以不需要手动释放
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        closeResources();
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 连接关闭时，确保资源被释放
        closeResources();
        super.channelInactive(ctx);
    }

    private void closeResources() {
        if (fos != null) {
            try {
                fos.close();
                fos = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}