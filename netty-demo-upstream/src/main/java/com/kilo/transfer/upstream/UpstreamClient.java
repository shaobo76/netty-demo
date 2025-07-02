package com.kilo.transfer.upstream;

import com.kilo.transfer.common.codec.FileChunkDecoder;
import com.kilo.transfer.common.codec.FileChunkEncoder;
import com.kilo.transfer.upstream.handler.UpstreamHandler;
import com.kilo.transfer.upstream.service.FileLockService;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class UpstreamClient {

    private static final Logger logger = LoggerFactory.getLogger(UpstreamClient.class);

    private final String host;
    private final int port;
    private final String scanDirectory;
    private final FileLockService fileLockService;
    private final ScheduledExecutorService scannerScheduler;

    public UpstreamClient(String host, int port, String scanDirectory) {
        this.host = host;
        this.port = port;
        this.scanDirectory = scanDirectory;
        this.fileLockService = new FileLockService(scanDirectory);
        this.scannerScheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void start() {
        scannerScheduler.scheduleAtFixedRate(this::scanAndProcessFiles, 0, 10, TimeUnit.SECONDS);
        logger.info("Upstream client started, scanning directory: {}", scanDirectory);
    }

    public void stop() {
        scannerScheduler.shutdown();
        try {
            if (!scannerScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scannerScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scannerScheduler.shutdownNow();
        }
        fileLockService.stop();
        logger.info("Upstream client stopped.");
    }

    private void scanAndProcessFiles() {
        logger.debug("Scanning for files in {}", scanDirectory);
        try (Stream<Path> paths = Files.walk(Paths.get(scanDirectory))) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> !path.toString().endsWith(".loc"))
                    .forEach(this::processFile);
        } catch (Exception e) {
            logger.error("Error scanning directory: {}", scanDirectory, e);
        }
    }

    private void processFile(Path filePath) {
        if (fileLockService.acquireLock(filePath)) {
            logger.info("Acquired lock for file: {}, starting transfer.", filePath);
            transferFile(filePath.toFile());
        }
    }

    public void transferFile(File file) {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(
                                    new StringDecoder(),
                                    new FileChunkEncoder(),
                                    new FileChunkDecoder(),
                                    new UpstreamHandler(file, 1024 * 1024, fileLockService) // 1MB chunk size
                            );
                        }
                    });

            logger.info("Connecting to {}:{} to transfer file {}", host, port, file.getName());
            ChannelFuture f = b.connect(host, port).sync();
            f.channel().closeFuture().sync();
        } catch (Exception e) {
            logger.error("Failed to transfer file: {}", file.getName(), e);
            fileLockService.releaseLock(file.toPath(), false); // Do not delete file on error
        } finally {
            group.shutdownGracefully();
            logger.info("Finished transfer attempt for file: {}", file.getName());
        }
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: java " + UpstreamClient.class.getSimpleName() + " <host> <port> <scanDirectory>");
            return;
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String scanDirectory = args[2];

        File dir = new File(scanDirectory);
        if (!dir.exists() || !dir.isDirectory()) {
            System.err.println("Directory not found or not a directory: " + scanDirectory);
            return;
        }

        UpstreamClient client = new UpstreamClient(host, port, scanDirectory);
        client.start();

        Runtime.getRuntime().addShutdownHook(new Thread(client::stop));
    }
}