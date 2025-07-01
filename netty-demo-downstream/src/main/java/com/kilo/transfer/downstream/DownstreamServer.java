package com.kilo.transfer.downstream;

import com.kilo.transfer.downstream.handler.DownstreamHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;

public class DownstreamServer {

    private final int port;
    private final String storagePath;

    public DownstreamServer(int port, String storagePath) {
        this.port = port;
        this.storagePath = storagePath;
    }

    public void run() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        // Create a separate thread pool for business logic to avoid blocking I/O threads.
        EventExecutorGroup businessGroup = new DefaultEventExecutorGroup(16);

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(
                                    // Using Java serialization for simplicity.
                                    // For production, consider using Protobuf or other efficient serialization frameworks.
                                    new ObjectEncoder(),
                                    new ObjectDecoder(ClassResolvers.cacheDisabled(null))
                            ).addLast(businessGroup, new DownstreamHandler(storagePath));
                        }
                    });

            System.out.println("Downstream Server starting on port " + port);
            System.out.println("Storage path: " + storagePath);
            ChannelFuture f = b.bind(port).sync();
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
            businessGroup.shutdownGracefully();
            System.out.println("Downstream Server shut down.");
        }
    }

    public static void main(String[] args) throws Exception {
        // Default port, can be overridden by configuration.
        int port = 8080;
        String storagePath = "downloads"; // Default storage path

        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }
        if (args.length > 1) {
            storagePath = args[1];
        }
        new DownstreamServer(port, storagePath).run();
    }
}