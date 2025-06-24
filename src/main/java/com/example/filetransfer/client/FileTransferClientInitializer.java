package com.example.filetransfer.client;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;

/**
 * 客户端的ChannelInitializer，用于配置新连接的ChannelPipeline。
 */
public class FileTransferClientInitializer extends ChannelInitializer<SocketChannel> {

    private final String fileName;
    private final String savePath;

    /**
     * 构造函数。
     * @param fileName 要从服务器下载的文件名。
     * @param savePath 下载后保存到本地的路径。
     */
    public FileTransferClientInitializer(String fileName, String savePath) {
        this.fileName = fileName;
        this.savePath = savePath;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        // 1. Outbound: 将要发送的字符串（文件名）编码为字节。
        pipeline.addLast(new StringEncoder(CharsetUtil.UTF_8));

        // 2. Inbound: 添加我们自定义的核心业务处理Handler。
        pipeline.addLast(new FileTransferClientHandler(fileName, savePath));
    }
}