package com.kilo.transfer.common.codec;

import com.kilo.transfer.common.message.FileChunkMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class FileChunkDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // The serialization format is self-contained, so we can deserialize directly.
        // We assume a full message is always available in the buffer.
        // For TCP streams, you would need a more robust framing mechanism,
        // like prepending the message length.
        out.add(new FileChunkMessage(in));
    }
}