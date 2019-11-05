/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, TeleStax Inc. and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 *   JBoss, Home of Professional Open Source
 *   Copyright 2007-2011, Red Hat, Inc. and individual contributors
 *   by the @authors tag. See the copyright.txt in the distribution for a
 *   full listing of individual contributors.
 *
 *   This is free software; you can redistribute it and/or modify it
 *   under the terms of the GNU Lesser General Public License as
 *   published by the Free Software Foundation; either version 2.1 of
 *   the License, or (at your option) any later version.
 *
 *   This software is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *   Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 *   License along with this software; if not, write to the Free
 *   Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *   02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package com.kp.diameter.api.controller;

import com.kp.diameter.api.message.IDMessage;
import com.kp.diameter.config.RemotePeerInfor;
import com.kp.network.connection.IConnection;

import java.io.IOException;
import java.util.concurrent.Future;

/**
 * This interface provide additional methods for Peer interface
 *
 * @author erick.svenson@yahoo.com
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 * @author <a href="mailto:baranowb@gmail.com"> Bartosz Baranowski </a>
 */
public interface IPeer {
    String getId();

    void setLocal(boolean local);

    RemotePeerInfor getRemotePeerInfor();

    void start(IConnection connection) throws Exception;


    void stop() throws Exception;

    public void setPeerListenerManager(PeerListenerManager peerListenerManager);

    void notifyMessage(IDMessage message) throws Exception;

    Future<IDMessage> sendMessage(IDMessage message) throws Exception;

    int processCerMessage(IDMessage message);

    public boolean processCeaMessage(IDMessage message);

    IDMessage buildCerMessage() throws IOException;

    IDMessage buildCeaMessage(int resultCode, IDMessage cer, String errMessage) throws IOException;

    IDMessage buildDwrMessage() throws IOException;

    IDMessage buildDwaMessage(IDMessage dwr, int resultCode, String errorMessage) throws IOException;

    IDMessage buildDprMessage(int disconnectCause) throws IOException;

    IDMessage buildDpaMessage(IDMessage dpr, int resultCode, String errorMessage) throws IOException;

    /**
     * Return true if connection should be restored
     * Look AttemptToConnect property of peer
     *
     * @return true if connection should be restored
     */
    void disconnect(int disconnectCause) throws IOException, InterruptedException;

    void handleMessage(IDMessage idMessage, IConnection connection);

    /**
     * Return new hop by hop id for new message
     *
     * @return new hop by hop id
     */
    long getHopByHopIdentifier();


    /**
     * Return true if peer has valid connection
     *
     * @return true if peer has valid connection
     */
    boolean hasValidConnection();

    boolean isConnected();
}
