package com.example.filetransfer.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.CharsetUtil;

/**
 * 服务器端的ChannelInitializer，用于配置新连接的ChannelPipeline。
 */
public class FileTransferServerInitializer extends ChannelInitializer<SocketChannel> {

    private final String fileRoot;

    /**
     * 构造函数。
     * @param fileRoot 服务器存放文件的根目录。
     */
    public FileTransferServerInitializer(String fileRoot) {
        this.fileRoot = fileRoot;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        // 1. Inbound: 将接收到的字节解码为字符串（文件名）。
        // 使用换行符作为分隔符。
        pipeline.addLast(new StringDecoder(CharsetUtil.UTF_8));

        // 2. Outbound: 将要发送的字符串（错误信息）编码为字节。
        pipeline.addLast(new StringEncoder(CharsetUtil.UTF_8));

        // 3. Outbound: 添加ChunkedWriteHandler以支持异步发送大的数据流（如大文件）。
        // 这是实现大文件传输不占内存的关键。
        pipeline.addLast(new ChunkedWriteHandler());

        // 4. Inbound: 添加我们自定义的核心业务处理Handler。
        pipeline.addLast(new FileTransferServerHandler(fileRoot));
    }
}