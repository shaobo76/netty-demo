package com.kilo.transfer.upstream;

import com.kilo.transfer.common.codec.FileChunkDecoder;
import com.kilo.transfer.common.codec.FileChunkEncoder;
import com.kilo.transfer.upstream.handler.UpstreamHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.File;

public class UpstreamClient {

    private final String host;
    private final int port;
    private final File file;

    public UpstreamClient(String host, int port, File file) {
        this.host = host;
        this.port = port;
        this.file = file;
    }

    public void connect() throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();

        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(
                                    new FileChunkEncoder(),
                                    new FileChunkDecoder(),
                                    new UpstreamHandler(file, 1024 * 1024) // 1MB chunk size
                            );
                        }
                    });

            System.out.printf("Upstream Client connecting to %s:%d%n", host, port);
            ChannelFuture f = b.connect(host, port).sync();
            System.out.println("Upstream Client connected.");

            // Wait until the connection is closed by the handler.
            f.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully();
            System.out.println("Upstream Client shut down.");
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println("Usage: java " + UpstreamClient.class.getSimpleName() + " <host> <port> <file>");
            return;
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        File file = new File(args[2]);

        if (!file.exists()) {
            System.err.println("File not found: " + args[2]);
            return;
        }

        new UpstreamClient(host, port, file).connect();
    }
}