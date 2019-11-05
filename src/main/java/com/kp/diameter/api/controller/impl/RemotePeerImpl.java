package com.kp.diameter.api.controller.impl;

import com.kp.diameter.config.LocalInfor;
import com.kp.diameter.config.RemotePeerInfor;
import com.kp.network.connection.IConnection;

public class RemotePeerImpl extends AbstactPeerImpl {
    public RemotePeerImpl(LocalInfor localInfor, RemotePeerInfor remotePeerInfor) throws Exception {
        super(localInfor, remotePeerInfor);
        setLocal(false);
    }

    @Override
    public void _start(IConnection connection) throws Exception {
        if (connection.isConnected()) {
            established.complete(this);
        }
    }
}
