package com.kp.diameter.api.controller;

import com.kp.diameter.api.message.IDMessage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultPeerListenerManager extends PeerListenerManager {

    private Map<String, PeerLisenter> listeners;


    @Override
    public void firePeerClose(IPeer peer) {
        listeners.forEach((s, tConnectionListener) -> {
            try {
                tConnectionListener.peerClosed(peer);
            } catch (Exception e) {
            }
        });
    }

    @Override
    public void fireMessageReceived(IPeer peer, IDMessage idMessage) {
        listeners.forEach((s, tConnectionListener) -> {
            try {
                tConnectionListener.messageReceeved(peer, idMessage);
            } catch (Exception e) {
            }
        });
    }

    @Override
    public void add(String id, PeerLisenter object) throws Exception {
        listeners.put(id, object);
    }

    @Override
    public PeerLisenter get(String id) throws Exception {
        return listeners.get(id);
    }

    @Override
    public boolean contains(String id) throws Exception {
        return listeners.containsKey(id);
    }

    @Override
    public boolean containsAndRemove(String id) {
        return false;
    }

    @Override
    public PeerLisenter remove(String id) throws Exception {
        return listeners.remove(id);
    }

    @Override
    public void destroy() throws Exception {
        removeAll();
        listeners = null;
    }

    @Override
    public void init() throws Exception {
        listeners = new ConcurrentHashMap<>();
    }

    @Override
    public void removeAll() throws Exception {
        listeners.clear();
    }
}
