package com.kp.diameter.network.codec;

import com.kp.common.data.message.IMessageParser;
import com.kp.diameter.api.message.IDMessage;
import com.kp.diameter.api.message.impl.parser.impl.MessageParser;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.ByteBuffer;

public class NettyTcpMessageParser
        extends MessageParser implements IMessageParser<ByteBuf, IDMessage> {

    @Override
    public ByteBuf encodeMessage(IDMessage message) throws Exception {

        ByteBuffer byteBuffer = encode(message);
        ByteBuf byteBuf = Unpooled.buffer(byteBuffer.capacity());
        byteBuf.writeBytes(byteBuffer);
        return byteBuf;

    }

    @Override
    public IDMessage decodeMessage(ByteBuf input) throws Exception {


        if (input.readableBytes() < 20) {
            return null;
        }

        int tmp = input.getInt(input.readerIndex());
        short version = (short) (tmp >> 24);
        if (version != 1) {
            throw new Exception("Illegal value of version " + version);
        }
        int length = (tmp & 0x00FFFFFF);

        if (input.readableBytes() >= (tmp & 0x00FFFFFF)) {
            byte[] databytes = new byte[length];
            input.readBytes(databytes);
            return createMessage(databytes);

        }


        return null;


    }

    @Override
    public void encodeMessage(IDMessage message, ByteBuf out) throws Exception {
        ByteBuffer byteBuffer = encode(message);
        out.writeBytes(byteBuffer);
    }
}
