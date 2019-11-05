package com.kp.diameter.api.controller.impl;

import com.kp.diameter.api.message.IDMessage;
import com.kp.diameter.config.LocalInfor;
import com.kp.diameter.config.RemotePeerInfor;
import com.kp.network.connection.IConnection;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ClientPeerImpl extends AbstactPeerImpl {
    public ClientPeerImpl(LocalInfor localInfor, RemotePeerInfor remotePeerInfor) throws Exception {
        super(localInfor, remotePeerInfor);
        setLocal(true);
    }

    @Override
    public void _start(IConnection connection) throws Exception {


        if (isLocal) {
            connection.connect();
            if (!hasValidConnection()) {
                throw new Exception("Peer is not established");
            }
        }

        scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    IDMessage idMessage = buildDwrMessage();
                    notifyMessage(idMessage);
                } catch (Exception e) {
                    try {
                        stop();
                    } catch (Exception e1) {
                        getLogger().error("stop peer error ", e);
                    }
                    getLogger().error("send dWR error ", e);
                }

            }
        }, localInfor.getTimeout(), localInfor.getTimeout(), TimeUnit.MILLISECONDS);

    }

}
