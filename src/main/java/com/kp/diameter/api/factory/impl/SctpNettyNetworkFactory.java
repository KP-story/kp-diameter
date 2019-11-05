package com.kp.diameter.api.factory.impl;

import com.kp.common.data.message.IMessageParser;
import com.kp.diameter.api.factory.NetworkFactory;
import com.kp.diameter.api.message.IDMessage;
import com.kp.diameter.network.client.netty.sctp.DiameterSctpClient;
import com.kp.diameter.network.codec.NettySctpMessageParser;
import com.kp.diameter.network.server.netty.sctp.DiameterSctpServer;
import com.kp.network.DefaultFutureManager;
import com.kp.network.FutureManager;
import com.kp.network.SocketServer;
import com.kp.network.connection.DefaultConnectionManager;
import com.kp.network.connection.IConnection;
import com.kp.network.connection.IConnectionManager;
import com.kp.network.event.impl.DefaultConnectionListenerManager;
import com.kp.network.event.impl.IConnectionListenerManager;
import io.netty.channel.sctp.SctpMessage;

import java.nio.channels.Channel;

public class SctpNettyNetworkFactory implements NetworkFactory<Channel> {
    @Override
    public SocketServer<IDMessage> createServer() throws Exception {
        IConnectionManager<IDMessage, io.netty.channel.Channel> connectionManager = new DefaultConnectionManager<>();
        IConnectionListenerManager<IDMessage> connectionListenerManager = new DefaultConnectionListenerManager<>();
        connectionListenerManager.init();
        connectionManager.init();
        IMessageParser<SctpMessage, IDMessage> messageParser = new NettySctpMessageParser();
        DiameterSctpServer sctpServer = new DiameterSctpServer(connectionManager, connectionListenerManager, messageParser);
        return sctpServer;
    }

    @Override
    public IConnection createClient() throws Exception {
        FutureManager<String, IDMessage> futureManager = new DefaultFutureManager<>();
        IMessageParser<SctpMessage, IDMessage> messageParser = new NettySctpMessageParser();
        IConnectionListenerManager<IDMessage> connectionListenerManager = new DefaultConnectionListenerManager<>();
        futureManager.init();
        connectionListenerManager.init();
        DiameterSctpClient client = new DiameterSctpClient(connectionListenerManager, messageParser);
        return client;
    }
}
