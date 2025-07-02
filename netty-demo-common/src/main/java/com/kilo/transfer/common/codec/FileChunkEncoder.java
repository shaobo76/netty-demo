package com.kilo.transfer.common.codec;

import com.kilo.transfer.common.message.FileChunkMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class FileChunkEncoder extends MessageToByteEncoder<FileChunkMessage> {

    @Override
    protected void encode(ChannelHandlerContext ctx, FileChunkMessage msg, ByteBuf out) throws Exception {
        msg.toByteBuf(out);
    }
}