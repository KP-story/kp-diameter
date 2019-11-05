package com.kp.diameter.network.client.netty.tcp;

import com.kp.common.data.message.IMessageParser;
import com.kp.diameter.api.message.IDMessage;
import com.kp.network.DefaultFutureManager;
import com.kp.network.FutureManager;
import com.kp.network.event.impl.IConnectionListenerManager;
import com.kp.network.netty.client.tcp.NettyTcpClient;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.handler.timeout.ReadTimeoutHandler;

public class DiameterTcpClient extends NettyTcpClient<IDMessage> {


    public DiameterTcpClient(IConnectionListenerManager connectionListenerManager, IMessageParser<ByteBuf, IDMessage> messageParser) {
        super(connectionListenerManager, messageParser);
    }

    @Override
    public void applyOptions(Bootstrap b) {

    }

    @Override
    public void applyChannelHandler(Channel ch) {
        ch.pipeline().addLast(new ReadTimeoutHandler(timeout));

    }

    @Override
    protected FutureManager<String, IDMessage> createFutureManager() {
        return new DefaultFutureManager<>();
    }
}
