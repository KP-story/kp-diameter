package com.kp.diameter.api.factory;

import com.kp.diameter.api.controller.IPeer;
import com.kp.diameter.config.LocalInfor;
import com.kp.diameter.config.RemotePeerInfor;

public interface PeerFactory {
    IPeer createRemotePeer(LocalInfor metadata, RemotePeerInfor remotePeerInfor) throws Exception;

    IPeer createLocalPeer(LocalInfor metadata, RemotePeerInfor remotePeerInfor) throws Exception;

}
