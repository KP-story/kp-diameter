package com.kp.diameter.api.controller;

import com.kp.diameter.api.message.IDMessage;

public interface MessageProvider {
    void messageReceived(IDMessage idMessage, IPeer iPeer);
}
