package com.kp.diameter.api.controller;

import com.kp.common.log.Loggable;
import com.kp.diameter.api.factory.NetworkFactory;
import com.kp.diameter.api.factory.PeerFactory;
import com.kp.diameter.api.message.IDMessage;
import com.kp.diameter.api.message.impl.ApplicationId;
import com.kp.diameter.api.message.impl.parser.impl.MessageParser;
import com.kp.diameter.config.LocalInfor;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class AbstractDiameterInstance implements DiameterInstance, Loggable {
    protected LocalInfor metadata;
    protected NetworkFactory networkFactory;
    protected PeerFactory peerFactory;
    protected AtomicBoolean isStopping = new AtomicBoolean(false);
    protected DefaultPeerListenerManager peerListenerManager;
    protected MessageProvider messageProvider;
    private ScheduledExecutorService scheduledExecutorService;
    private int threadCount;
    private int queueSize;
    private Lock lock = new ReentrantLock();
    private MessageParser messageParser = new MessageParser();
    private Map<String, Map<String, IPeer>> peerTable = new ConcurrentHashMap<>();

    public void setMessageProvider(MessageProvider messageProvider) {
        this.messageProvider = messageProvider;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    public LocalInfor getMetadata() {
        return metadata;
    }

    public void setMetadata(LocalInfor metadata) {
        this.metadata = metadata;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    protected abstract void _init() throws Exception;

    protected abstract void _destroy() throws Exception;

    protected synchronized void addPeer(String realm, String host, IPeer peer) throws Exception {
        if (isStopping.get()) {
            throw new IllegalStateException("com.kp.diameter is stopping");
        }

        try {
            Map<String, IPeer> peerInRealm = peerTable.get(realm);
            if (peerInRealm == null) {
                peerInRealm = new ConcurrentHashMap<>();

            }
            IPeer oldPeer = peerInRealm.get(host);

            if (oldPeer != null && oldPeer.hasValidConnection()) {
                throw new Exception("Peer is exist");
            } else {
                if (oldPeer != null) {
                    peerInRealm.remove(host);
                    oldPeer.stop();

                }
            }

            peerInRealm.put(host, peer);
            peerTable.put(realm, peerInRealm);
        } catch (Exception e) {
            getLogger().error("error when addPeer peer{} ", peer, e);
        }

    }


    protected synchronized IPeer getPeer(String realm, String host) {
        if (isStopping.get()) {
            throw new IllegalStateException("com.kp.diameter is stopping");
        }
        Map<String, IPeer> peerInRealm = peerTable.get(realm);
        if (peerInRealm == null) {
            return null;

        }
        IPeer oldPeer = peerInRealm.get(host);

        if (oldPeer != null && oldPeer.hasValidConnection()) {
            return oldPeer;
        } else {
            if (oldPeer != null) {
                peerInRealm.remove(host);
                try {
                    oldPeer.stop();
                } catch (Exception e) {
                    getLogger().error("stop peer is error ", e);
                }

            }
        }

        return null;
    }


    protected synchronized void removePeer(String realm, String host) throws Exception {
        try {
            if (isStopping.get()) {
                throw new IllegalStateException("com.kp.diameter is stopping");
            }

            Map<String, IPeer> peerInRealm = peerTable.get(realm);
            if (peerInRealm == null) {

                return;

            }

            IPeer peer = peerInRealm.remove(host);

            peerTable.put(realm, peerInRealm);
            if (peer != null) {
                try {
                    peer.stop();
                } catch (Exception e) {
                    getLogger().error("stop peer is error ", e);
                }
            }


        } catch (Exception e) {
            getLogger().error("error when addPeer host{} realm{} ", host, realm, e);
        }


    }

    @Override
    public IDMessage createRequest(int commandCode, ApplicationId appId, String destRealm, String sessionId) {
        if (isStopping.get()) {
            throw new IllegalStateException("com.kp.diameter is stopping");
        }
        IDMessage m = (IDMessage) this.messageParser.createEmptyMessage(IDMessage.class, commandCode, messageParser.getAppId(appId));
        m.setRequest(true);
        m.getAvps().addAvp(263, sessionId, true, false, false);
        messageParser.appendAppId(appId, m);
        if (destRealm != null) {
            m.getAvps().addAvp(283, destRealm, true, false, true);
        }

        messageParser.addOriginAvps(m, metadata);
        return m;


    }


    @Override
    public void init() throws Exception {

        try {
            if (isStopping.get()) {
                throw new IllegalStateException("com.kp.diameter is stopping");
            }
            lock.lock();


            Class networkFactoryClass = metadata.getExtension().get(NetworkFactory.class.getSimpleName());
            networkFactory = (NetworkFactory) networkFactoryClass.newInstance();
            Class peerFactoryClass = metadata.getExtension().get(PeerFactory.class.getSimpleName());
            peerFactory = (PeerFactory) peerFactoryClass.newInstance();

            peerListenerManager = new DefaultPeerListenerManager();
            peerListenerManager.init();
            peerListenerManager.add("readmessage", new PeerLisenter() {
                @Override
                public void peerClosed(IPeer peer) {
                    try {
                        String host = peer.getRemotePeerInfor().getUri().getFQDN();
                        String realm = peer.getRemotePeerInfor().getRealmName();
                        removePeer(realm, host);
                    } catch (Exception e) {
                        getLogger().error("Auto Remove peer error {} ", peer, e);
                    }
                }

                @Override
                public void messageReceeved(IPeer peer, IDMessage idMessage) {
                    if (messageProvider != null) {
                        messageProvider.messageReceived(idMessage, peer);
                    }
                }
            });
            _init();

            scheduledExecutorService = Executors.newScheduledThreadPool(1);
            scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    invalidatePeertable();
                }
            }, metadata.getTimeout(), metadata.getTimeout(), TimeUnit.MILLISECONDS);
        } finally {
            lock.unlock();
        }
    }


    private void invalidatePeertable() {
        Iterator<Map.Entry<String, Map<String, IPeer>>> peerTableId = peerTable.entrySet().iterator();


        while (peerTableId.hasNext()) {
            Map.Entry<String, Map<String, IPeer>> mapEntry = peerTableId.next();
            String realm = mapEntry.getKey();
            Map<String, IPeer> iPeerMap = mapEntry.getValue();
            Iterator<String> hosts = iPeerMap.keySet().iterator();
            while (hosts.hasNext()) {
                String host = hosts.next();
                IPeer peer = iPeerMap.get(host);
                try {
                    if (!peer.hasValidConnection()) {
                        removePeer(realm, host);
                    }

                } catch (Exception e) {
                    getLogger().error(" remove peer ({}; {}) has error  ", realm, host, e);
                }
            }


        }
    }

    private void clearPeertable() {
        Iterator<Map.Entry<String, Map<String, IPeer>>> peerTableId = peerTable.entrySet().iterator();


        while (peerTableId.hasNext()) {
            Map.Entry<String, Map<String, IPeer>> mapEntry = peerTableId.next();
            String realm = mapEntry.getKey();
            Map<String, IPeer> iPeerMap = mapEntry.getValue();
            Iterator<String> hosts = iPeerMap.keySet().iterator();
            while (hosts.hasNext()) {
                String host = hosts.next();

                try {
                    removePeer(realm, host);

                } catch (Exception e) {
                    getLogger().error(" remove peer ({}; {}) has error  ", realm, host, e);
                }
            }


        }
        peerTable.clear();

    }

    @Override
    public void destroy() throws Exception {
        try {

            isStopping.set(true);
            lock.lock();
            try {
                _destroy();
                clearPeertable();
                if (scheduledExecutorService != null) {
                    scheduledExecutorService.shutdown();
                }
                if (peerListenerManager != null) {
                    peerListenerManager.destroy();

                }
            } catch (Exception e) {
                getLogger().error("_destroy error", e);

            }


        } finally {
            lock.unlock();
        }
    }


}
