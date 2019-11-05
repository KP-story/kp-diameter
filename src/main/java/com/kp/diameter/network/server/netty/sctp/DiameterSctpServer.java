package com.kp.diameter.network.server.netty.sctp;

import com.kp.common.data.message.IMessageParser;
import com.kp.diameter.api.message.IDMessage;
import com.kp.network.DefaultFutureManager;
import com.kp.network.FutureManager;
import com.kp.network.connection.IConnectionManager;
import com.kp.network.event.impl.DefaultConnectionListenerManager;
import com.kp.network.event.impl.IConnectionListenerManager;
import com.kp.network.netty.server.sctp.NettySctpServer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.sctp.SctpMessage;

public class DiameterSctpServer extends NettySctpServer<IDMessage> {


    public DiameterSctpServer(IConnectionManager<IDMessage, Channel> connectionManager, IConnectionListenerManager<IDMessage> connectionListenerManager, IMessageParser<SctpMessage, IDMessage> messageParser) throws Exception {
        super(connectionManager, connectionListenerManager, messageParser);
    }

    @Override
    protected IConnectionListenerManager<IDMessage> createEntryConnectionListenerManager() {
        return new DefaultConnectionListenerManager<>();
    }

    @Override
    public void applyOptions(ServerBootstrap b) {

    }

    @Override
    protected FutureManager<String, IDMessage> newFutureManager() {
        return new DefaultFutureManager<>();
    }
}
