package com.kilo.transfer.common.codec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kilo.transfer.common.message.FileChunkMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class JsonEncoder extends MessageToByteEncoder<FileChunkMessage> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void encode(ChannelHandlerContext ctx, FileChunkMessage msg, ByteBuf out) throws Exception {
        byte[] bytes = objectMapper.writeValueAsBytes(msg);
        out.writeInt(bytes.length);
        out.writeBytes(bytes);
    }
}