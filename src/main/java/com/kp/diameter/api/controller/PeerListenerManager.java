package com.kp.diameter.api.controller;

import com.kp.diameter.api.message.IDMessage;
import com.kp.network.IObjectManager;

public abstract class PeerListenerManager implements IObjectManager<String, PeerLisenter> {

    public abstract void firePeerClose(IPeer peer);

    public abstract void fireMessageReceived(IPeer peer, IDMessage idMessage);

}
