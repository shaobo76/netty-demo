package com.example;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * Receives files from a client.
 */
public class FileReceiver {

    private final int port;
    private final String outputDirectory;

    /**
     * Constructs a FileReceiver.
     *
     * @param port            The port to listen on.
     * @param outputDirectory The directory to save received files.
     */
    public FileReceiver(int port, String outputDirectory) {
        this.port = port;
        this.outputDirectory = outputDirectory;
    }

    /**
     * Runs the file receiver server.
     *
     * @throws InterruptedException If the server is interrupted.
     */
    public void run() throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 100)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();
                            p.addLast(new FileReceiverHandler(outputDirectory));
                        }
                    });

            ChannelFuture f = b.bind(port).sync();
            System.out.println("File receiver server started on port " + port +
                               ", saving files to " + outputDirectory);
            f.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: FileReceiver <port> <output_directory>");
            return;
        }
        int port = Integer.parseInt(args[0]);
        String outputDir = args[1];
        new FileReceiver(port, outputDir).run();
    }
}
