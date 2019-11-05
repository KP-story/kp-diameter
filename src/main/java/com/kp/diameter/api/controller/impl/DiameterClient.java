package com.kp.diameter.api.controller.impl;

import com.kp.diameter.api.controller.AbstractDiameterInstance;
import com.kp.diameter.api.controller.IPeer;
import com.kp.diameter.api.message.IDMessage;
import com.kp.diameter.config.RemotePeerInfor;
import com.kp.network.connection.IConnection;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Future;


public class DiameterClient extends AbstractDiameterInstance {
    private IConnection connection;
    private IPeer clientPeer;
    private RemotePeerInfor serverInfor;

    @Override
    public boolean isAtive() {
        if (clientPeer != null) {
            return clientPeer.hasValidConnection();
        }
        return false;
    }

    @Override
    public void _init() throws Exception {
        connection = networkFactory.createClient();
        for (InetAddress ia : metadata.getAddress()) {
            SocketAddress socketAddress = new InetSocketAddress(ia.getHostAddress(), metadata.getUri().getPort());
            connection.addLocalAddress(socketAddress);

        }

        connection.addRemoteAddress(serverInfor.getAddress());
        connection.setTimeout(metadata.getTimeout());
        connection.setNWorker(getThreadCount());
        clientPeer = peerFactory.createLocalPeer(metadata, serverInfor);
        clientPeer.setPeerListenerManager(peerListenerManager);
        metadata.updateLocalHostStateId();
        clientPeer.start(connection);

    }

    @Override
    public void _destroy() throws Exception {


        if (clientPeer != null) {
            clientPeer.stop();
        }
        if (peerListenerManager != null) {
            peerListenerManager.destroy();

        }

    }


    @Override
    public void addPredefinedPeer(RemotePeerInfor remotePeerInfor) {
        serverInfor = remotePeerInfor;

    }

    @Override
    public void notifyToClient(IDMessage message) throws Exception {
        if (isStopping.get()) {
            throw new IllegalStateException("com.kp.diameter is stopping");
        }
        if (clientPeer != null) {
            clientPeer.notifyMessage(message);
        } else {
            throw new IOException("peer is not established");
        }
    }

    @Override
    public Future<IDMessage> sendMessage(IDMessage message) throws Exception {
        if (isStopping.get()) {
            throw new IllegalStateException("com.kp.diameter is stopping");
        }
        if (clientPeer != null) {
            return clientPeer.sendMessage(message);
        } else {
            throw new IOException("peer is not established");
        }
    }


}
