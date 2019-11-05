package com.kp.diameter.api.controller.impl;

import com.kp.diameter.api.controller.AbstractDiameterInstance;
import com.kp.diameter.api.controller.IPeer;
import com.kp.diameter.api.message.AvpDataException;
import com.kp.diameter.api.message.IDMessage;
import com.kp.diameter.api.message.impl.parser.Avp;
import com.kp.diameter.config.RemotePeerInfor;
import com.kp.network.SocketServer;
import com.kp.network.connection.IConnection;
import com.kp.network.event.impl.ConnectionListener;

import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class DiameterServer extends AbstractDiameterInstance {
    private Map<String, RemotePeerInfor> predefinedPeer = new ConcurrentHashMap<>();
    private SocketServer<IDMessage> socketServer;
    private int nboot = 2;
    private Object lockPeerInit = new Object();


    public int getNboot() {
        return nboot;
    }

    public void setNboot(int nboot) {
        this.nboot = nboot;
    }

    @Override
    protected void _init() throws Exception {
        socketServer = networkFactory.createServer();
        for (InetAddress ia : metadata.getAddress()) {

            socketServer.addLocalAddress(ia);

        }
        socketServer.addLocalPort(metadata.getUri().getPort());


        socketServer.setTimeout(metadata.getTimeout());
        socketServer.setNworker(getThreadCount());
        socketServer.setRcvbuf(getQueueSize());
        socketServer.setNboot(getNboot());
        socketServer.addConnectionListener("main", new ConnectionListener<IDMessage>() {
            @Override
            public void connectionOpened(IConnection connection) {
                synchronized (lockPeerInit) {
                    try {
                        connection.addConnectionListener("init", new ConnectionListener<IDMessage>() {
                            @Override
                            public void connectionOpened(IConnection connection) {

                            }

                            @Override
                            public void connectionClosed(IConnection connection) {

                            }

                            @Override
                            public void messageReceived(IConnection connection, IDMessage message) {

                                if (message.isRequest() && message.getCommandCode() == IDMessage.CAPABILITIES_EXCHANGE_REQUEST) {

                                    IPeer peer = null;
                                    String host;
                                    try {
                                        host = message.getAvps().getAvp(Avp.ORIGIN_HOST).getDiameterIdentity();
                                        getLogger().debug("Origin-Host in new received message is [{}]", host);
                                    } catch (AvpDataException e) {
                                        getLogger().warn("Unable to retrieve find Origin-Host AVP in CER", e);
                                        disconnect(connection);
                                        return;
                                    }
                                    String realm;
                                    try {
                                        realm = message.getAvps().getAvp(Avp.ORIGIN_REALM).getDiameterIdentity();
                                        getLogger().debug("Origin-Realm in new received message is [{}]", realm);
                                    } catch (AvpDataException e) {
                                        getLogger().warn("Unable to retrieve find Origin-Realm AVP in CER", e);
                                        disconnect(connection);

                                        return;
                                    }
                                    String key = host + realm;

                                    RemotePeerInfor remotePeerInfor = predefinedPeer.get(key);
                                    if (remotePeerInfor == null) {
                                        getLogger().error("Unknown host {} and realm {} . ", host, realm);
                                        disconnect(connection);
                                    }
                                    peer = getPeer(realm, host);
                                    if (peer != null && peer.hasValidConnection()) {
                                        getLogger().error("Peer is exist host {} and realm {}. ", host, realm, peer.getId());
                                        disconnect(connection);
                                    } else {

                                        try {
                                            peer = peerFactory.createRemotePeer(metadata, remotePeerInfor);
                                            peer.setPeerListenerManager(peerListenerManager);
                                            try {
                                                connection.remAllConnectionListener();
                                            } catch (Exception e) {
                                                getLogger().error("connection.remAllConnectionListener ", e);
                                            }

                                            peer.start(connection);
                                            int result = peer.processCerMessage(message);
                                            peer.notifyMessage(peer.buildCeaMessage(result, message, null));
                                            addPeer(realm, host, peer);

                                        } catch (Exception e) {
                                            disconnect(connection);
                                            getLogger().error("Peer is exist host {} and realm {}. ", host, realm, peer.getId());
                                        }
                                    }

                                } else {

                                    getLogger().error("Illigal message {}", message);
                                    try {
                                        connection.remAllConnectionListener();
                                    } catch (Exception e) {
                                        getLogger().error("connection.remAllConnectionListener ", e);
                                    }
                                    disconnect(connection);

                                }
                            }

                            @Override
                            public void internalError(IConnection connection, IDMessage message, Throwable cause) {
                                disconnect(connection);
                            }
                        });
                    } catch (Exception e) {
                        getLogger().error("error when add connection listener ", e);
                        disconnect(connection);
                    }
                }

            }

            @Override
            public void connectionClosed(IConnection connection) {

            }

            @Override
            public void messageReceived(IConnection connection, IDMessage message) {


            }

            @Override
            public void internalError(IConnection connection, IDMessage message, Throwable cause) {

            }
        });
        socketServer.init().get(metadata.getTimeout(), TimeUnit.MILLISECONDS);
        metadata.updateLocalHostStateId();
    }


    private void disconnect(IConnection iConnection) {
        try {
            iConnection.disconnect();

        } catch (Exception e) {
            getLogger().error("disconnect connection has error ", e);
        }


    }

    @Override
    protected void _destroy() throws Exception {
        try {
            if (socketServer != null) {
                socketServer.destroy().get(metadata.getTimeout(), TimeUnit.MILLISECONDS);
            }

        } catch (Exception e) {
            getLogger().error("Destroy server has error ", e);

        }


        predefinedPeer.clear();
    }

    @Override
    public boolean isAtive() {
        if (socketServer != null && socketServer.isActive()) {
            return true;
        }
        return false;
    }

    @Override
    public void addPredefinedPeer(RemotePeerInfor remotePeerInfor) {
        predefinedPeer.put(remotePeerInfor.getUri().getFQDN() + remotePeerInfor.getRealmName(), remotePeerInfor);

    }

    @Override
    public void notifyToClient(IDMessage message) throws Exception {


    }

    @Override
    public Future<IDMessage> sendMessage(IDMessage message) throws Exception {
        return null;
    }
}
