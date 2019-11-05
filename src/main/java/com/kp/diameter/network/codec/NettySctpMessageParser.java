package com.kp.diameter.network.codec;

import com.kp.common.data.message.IMessageParser;
import com.kp.diameter.api.message.IDMessage;
import com.kp.diameter.api.message.impl.parser.impl.MessageParser;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.sctp.SctpMessage;

import java.nio.ByteBuffer;

public class NettySctpMessageParser extends MessageParser implements IMessageParser<SctpMessage, IDMessage> {
    @Override
    public SctpMessage encodeMessage(IDMessage message) throws Exception {
        ByteBuffer byteBuffer = encode(message);
        ByteBuf byteBuf = Unpooled.buffer(byteBuffer.capacity());
        byteBuf.writeBytes(byteBuffer);
        SctpMessage sctpMessage = new SctpMessage(0, 0, byteBuf);

        return sctpMessage;

    }

    @Override
    public IDMessage decodeMessage(SctpMessage sctpMessage) throws Exception {

        ByteBuf input = sctpMessage.content();
        if (input.readableBytes() < 20) {
            return null;
        }

        int tmp = input.getInt(input.readerIndex());
        short version = (short) (tmp >> 24);
        if (version != 1) {
            throw new Exception("Illegal value of version " + version);
        }
        int length = (tmp & 0x00FFFFFF);

        if (input.readableBytes() >= length) {
            byte[] databytes = new byte[length];
            input.readBytes(databytes);
            return createMessage(databytes);

        }


        return null;
    }

    @Override
    public void encodeMessage(IDMessage message, SctpMessage out) throws Exception {

    }
}
