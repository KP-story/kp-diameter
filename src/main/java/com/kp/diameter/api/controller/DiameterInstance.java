package com.kp.diameter.api.controller;

import com.kp.diameter.api.message.IDMessage;
import com.kp.diameter.api.message.impl.ApplicationId;
import com.kp.diameter.config.RemotePeerInfor;

import java.util.concurrent.Future;

public interface DiameterInstance {
    void init() throws Exception;

    void destroy() throws Exception;

    boolean isAtive();


    void addPredefinedPeer(RemotePeerInfor remotePeerInfor);

    void notifyToClient(IDMessage message) throws Exception;

    Future<IDMessage> sendMessage(IDMessage message) throws Exception;

    IDMessage createRequest(int commandCode, ApplicationId appId, String destRealm, String sessionId);

}
