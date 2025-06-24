package com.example.filetransfer.server;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.ssl.SslHandler;

import java.io.File;
import java.io.RandomAccessFile;

/**
 * 服务器端的核心业务逻辑处理器。
 * 负责处理文件名请求，并使用零拷贝技术发送文件。
 */
public class FileTransferServerHandler extends SimpleChannelInboundHandler<String> {

    private final String fileRoot;

    public FileTransferServerHandler(String fileRoot) {
        this.fileRoot = fileRoot;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        // 移除文件名中的换行符
        String fileName = msg.trim();
        File file = new File(fileRoot, fileName);

        if (!file.exists() || file.isDirectory()) {
            // 文件不存在或是一个目录
            // 根据协议，发送 -1L 表示失败
            ctx.writeAndFlush(-1L);
            // 发送错误信息
            ctx.writeAndFlush("ERROR: File not found or is a directory.\n").addListener(ChannelFutureListener.CLOSE);
            System.err.println("File not found: " + file.getPath());
            return;
        }

        // 使用RandomAccessFile以只读模式打开文件
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        long fileLength = raf.length();

        // 1. 发送文件长度
        ctx.write(fileLength);

        // 2. 使用DefaultFileRegion实现零拷贝文件传输
        // 注意：如果使用了SSL/TLS，零拷贝可能不起作用，因为数据需要被加密和解密。
        // Netty的SslHandler会处理这种情况，但性能会受到影响。
        // 在这个例子中，我们没有使用SSL。
        DefaultFileRegion region = new DefaultFileRegion(raf.getChannel(), 0, fileLength);
        
        // 3. 发送文件内容
        // writeAndFlush会触发ChunkedWriteHandler将文件分块发送
        ctx.writeAndFlush(region).addListener(future -> {
            if (!future.isSuccess()) {
                System.err.println("File transfer failed: " + future.cause());
            } else {
                System.out.println("File transfer successful: " + file.getName());
            }
            // 传输完成后关闭RandomAccessFile
            raf.close();
            // 传输完成后关闭连接
            ctx.close();
        });

        System.out.println("Serving file: " + file.getName() + " (" + fileLength + " bytes)");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // 异常处理
        cause.printStackTrace();
        if (ctx.channel().isActive()) {
            ctx.writeAndFlush("ERROR: " + cause.getClass().getSimpleName() + ": " + cause.getMessage() + '\n')
                    .addListener(ChannelFutureListener.CLOSE);
        }
    }
}