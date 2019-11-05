package com.kp.diameter.api.controller;

import com.kp.diameter.api.message.IDMessage;

public interface PeerLisenter {
    void peerClosed(IPeer peer);

    void messageReceeved(IPeer peer, IDMessage idMessage);

}
