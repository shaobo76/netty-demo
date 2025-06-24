package com.example;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.stream.ChunkedFile;
import io.netty.handler.stream.ChunkedWriteHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Sends a file to a specified server.
 */
public class FileSender {

    private final String host;
    private final int port;
    private final String filePath;

    /**
     * Constructs a FileSender.
     *
     * @param host     The host of the server.
     * @param port     The port of the server.
     * @param filePath The path to the file to send.
     */
    public FileSender(String host, int port, String filePath) {
        this.host = host;
        this.port = port;
        this.filePath = filePath;
    }

    /**
     * Runs the file sender.
     *
     * @throws InterruptedException If the connection is interrupted.
     * @throws IOException          If an I/O error occurs.
     */
    public void run() throws InterruptedException, IOException {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();
                            p.addLast(new ChunkedWriteHandler());
                            // Add other handlers if needed, e.g., for progress or completion
                        }
                    });

            ChannelFuture f = b.connect(host, port).sync();
            Channel channel = f.channel();

            File file = new File(filePath);
            if (!file.exists()) {
                System.err.println("File not found: " + filePath);
                return;
            }
            if (!file.isFile()) {
                System.err.println("Not a file: " + filePath);
                return;
            }
            if (!file.canRead()) {
                System.err.println("File not readable: " + filePath);
                return;
            }

            RandomAccessFile raf = null;
            try {
                raf = new RandomAccessFile(file, "r");
                // Send the file name first (optional, but good practice)
                // For simplicity, this example directly sends the file content.
                // You might want to implement a protocol to send metadata like filename and size.

                // Send file length first
                channel.writeAndFlush(raf.length()).sync();

                // Send file content
                channel.writeAndFlush(new ChunkedFile(raf, 0, raf.length(), 8192)).sync();
                System.out.println("File '" + file.getName() + "' sent successfully.");
            } catch (FileNotFoundException e) {
                System.err.println("File not found: " + e.getMessage());
            } catch (IOException e) {
                System.err.println("Error reading file: " + e.getMessage());
            } finally {
                if (raf != null) {
                    try {
                        raf.close();
                    } catch (IOException e) {
                        // Log or handle closing error
                    }
                }
            }

            channel.closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println("Usage: FileSender <host> <port> <file>");
            return;
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String filePath = args[2];
        new FileSender(host, port, filePath).run();
    }
}
