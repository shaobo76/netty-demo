package com.kilo.transfer.common.codec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kilo.transfer.common.message.FileChunkMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class JsonDecoder extends ByteToMessageDecoder {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() < 4) {
            return;
        }

        in.markReaderIndex();
        int length = in.readInt();

        if (in.readableBytes() < length) {
            in.resetReaderIndex();
            return;
        }

        byte[] bytes = new byte[length];
        in.readBytes(bytes);

        FileChunkMessage message = objectMapper.readValue(bytes, FileChunkMessage.class);
        out.add(message);
    }
}