package com.example.filetransfer.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.io.File;

/**
 * 文件传输服务器启动类。
 */
public class FileTransferServer {

    private final int port;
    private final String fileRoot;

    public FileTransferServer(int port, String fileRoot) {
        this.port = port;
        this.fileRoot = fileRoot;
    }

    public void run() throws Exception {
        // 校验文件根目录是否存在且是目录
        File rootDir = new File(fileRoot);
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            System.err.println("Error: File root directory does not exist or is not a directory: " + fileRoot);
            return;
        }

        // 配置服务器
        // 1. 创建两个EventLoopGroup。bossGroup用于接受连接，workerGroup用于处理连接的I/O。
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class) // 使用NIO的ServerSocketChannel
             .option(ChannelOption.SO_BACKLOG, 128) // 设置TCP连接的backlog队列大小
             .handler(new LoggingHandler(LogLevel.INFO)) // 添加日志处理器
             .childHandler(new FileTransferServerInitializer(fileRoot)); // 设置workerGroup的处理器

            // 绑定端口并启动服务器
            ChannelFuture f = b.bind(port).sync();
            System.out.println("Server started, listening on " + port + ". File root is " + fileRoot);

            // 等待服务器socket关闭
            f.channel().closeFuture().sync();
        } finally {
            // 优雅地关闭EventLoopGroup
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public static void start(int port, String fileRoot) throws Exception {
        new FileTransferServer(port, fileRoot).run();
    }
}