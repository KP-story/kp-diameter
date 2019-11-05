package com.kp.diameter.api.factory.impl;

import com.kp.diameter.api.controller.IPeer;
import com.kp.diameter.api.controller.impl.ClientPeerImpl;
import com.kp.diameter.api.controller.impl.RemotePeerImpl;
import com.kp.diameter.api.factory.PeerFactory;
import com.kp.diameter.config.LocalInfor;
import com.kp.diameter.config.RemotePeerInfor;

public class PeerFactoryImpl implements PeerFactory {
    @Override
    public IPeer createRemotePeer(LocalInfor metadata, RemotePeerInfor remotePeerInfor) throws Exception {
        return new RemotePeerImpl(metadata, remotePeerInfor);
    }

    @Override
    public IPeer createLocalPeer(LocalInfor metadata, RemotePeerInfor remotePeerInfor) throws Exception {
        return new ClientPeerImpl(metadata, remotePeerInfor);
    }
}
