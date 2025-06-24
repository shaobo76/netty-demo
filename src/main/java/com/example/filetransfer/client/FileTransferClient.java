package com.example.filetransfer.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * 文件传输客户端启动类。
 */
public class FileTransferClient {

    private final String host;
    private final int port;
    private final String fileName;
    private final String savePath;

    public FileTransferClient(String host, int port, String fileName, String savePath) {
        this.host = host;
        this.port = port;
        this.fileName = fileName;
        this.savePath = savePath;
    }

    public void run() throws Exception {
        // 配置客户端
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
             .channel(NioSocketChannel.class)
             .handler(new FileTransferClientInitializer(fileName, savePath));

            // 连接到服务器
            ChannelFuture f = b.connect(host, port).sync();
            System.out.println("Connecting to " + host + ":" + port + "...");

            // 等待连接关闭
            f.channel().closeFuture().sync();
        } finally {
            // 优雅地关闭EventLoopGroup
            group.shutdownGracefully();
        }
    }

    public static void start(String host, int port, String fileName, String savePath) throws Exception {
        new FileTransferClient(host, port, fileName, savePath).run();
    }
}