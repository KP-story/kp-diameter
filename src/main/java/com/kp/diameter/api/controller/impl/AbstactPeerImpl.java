package com.kp.diameter.api.controller.impl;

import com.kp.common.log.Loggable;
import com.kp.diameter.api.controller.DisconnectCause;
import com.kp.diameter.api.controller.IPeer;
import com.kp.diameter.api.controller.PeerListenerManager;
import com.kp.diameter.api.message.AvpDataException;
import com.kp.diameter.api.message.IDMessage;
import com.kp.diameter.api.message.ResultCode;
import com.kp.diameter.api.message.impl.ApplicationId;
import com.kp.diameter.api.message.impl.parser.Avp;
import com.kp.diameter.api.message.impl.parser.AvpSet;
import com.kp.diameter.api.message.impl.parser.impl.MessageParser;
import com.kp.diameter.config.LocalInfor;
import com.kp.diameter.config.RemotePeerInfor;
import com.kp.network.connection.IConnection;
import com.kp.network.event.impl.ConnectionListener;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.kp.diameter.api.message.IDMessage.*;
import static com.kp.diameter.api.message.ResultCode.SUCCESS;
import static com.kp.diameter.api.message.impl.parser.Avp.*;

public abstract class AbstactPeerImpl implements IPeer, Loggable {
    public static final int INT_COMMON_APP_ID = 0xffffffff;
    //    protected final Dictionary dictionary = DictionarySingleton.getDictionary();
    protected LocalInfor localInfor;
    protected AtomicLong hopByHopId = new AtomicLong(0);
    protected AtomicBoolean stopping = new AtomicBoolean(false);
    protected MessageParser parser = new MessageParser();
    protected IConnection connection;
    protected CompletableFuture<AbstactPeerImpl> established;
    protected boolean isLocal = true;
    protected InetAddress[] addresses;
    protected RemotePeerInfor remotePeerInfor;
    protected Set<ApplicationId> commonApplications = new HashSet<ApplicationId>();
    protected ScheduledExecutorService scheduledExecutorService;
    protected Lock lock = new ReentrantLock();


    protected PeerListenerManager peerListenerManager;


    public AbstactPeerImpl(LocalInfor localInfor, RemotePeerInfor remotePeerInfor) throws Exception {
        if (localInfor == null || remotePeerInfor == null) {
            throw new NullPointerException("both localInfor and remotePeerInfor must be # null");
        }
        this.localInfor = localInfor;
        this.remotePeerInfor = remotePeerInfor;

    }

    @Override
    public String toString() {
        return "AbstactPeerImpl{" +
                "localInfor=" + localInfor +
                ", remotePeerInfor=" + remotePeerInfor +
                '}';
    }

    public PeerListenerManager getPeerListenerManager() {
        return peerListenerManager;
    }

    @Override
    public void setPeerListenerManager(PeerListenerManager peerListenerManager) {
        this.peerListenerManager = peerListenerManager;
    }

    protected abstract void _start(IConnection connection) throws Exception;


    @Override
    public void start(IConnection connection) throws Exception {
        try {

            lock.lock();

            this.connection = connection;
            established = new CompletableFuture<>();
            connection.addConnectionListener(getId(), new ConnectionListener<IDMessage>() {

                @Override
                public void connectionOpened(IConnection connection) {
                    if (isLocal)
                        try {


                            if (connection.isConnected()) {
                                IDMessage cerMessage = buildCerMessage();
                                _notifyToClient(cerMessage);

                            } else {
                                throw new IOException("Connection is inactive");
                            }
                        } catch (Exception e) {
                            established.cancel(true);
                            getLogger().error("establish peer error ", e);
                            try {
                                connection.disconnect();
                            } catch (Exception e1) {
                                getLogger().error("close connection error ", e1);
                            }

                        }


                }

                @Override
                public void connectionClosed(IConnection connection) {

                    try {
                        stop();
                    } catch (Exception e) {
                        getLogger().error("stop peer ", e);
                    }
                    try {

                    } catch (Exception e) {
                        getLogger().error(" peerListenerManager.firePeerClose ", e);

                    }
                }

                @Override
                public void messageReceived(IConnection connection, IDMessage message) {
                    handleMessage(message, connection);

                }


                @Override
                public void internalError(IConnection connection, IDMessage message, Throwable cause) {
                    getLogger().error("internalError connection {} message {} error ", connection, message, cause);
                }
            });
            _start(connection);

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
        } finally {
            lock.unlock();
        }
    }

    public InetAddress[] getAddresses() {
        return addresses;
    }

    public void setAddresses(InetAddress[] addresses) {
        this.addresses = addresses;
    }


    public Set<ApplicationId> getCommonApplications() {
        return commonApplications;
    }

    public void setCommonApplications(Set<ApplicationId> commonApplications) {
        this.commonApplications = commonApplications;
    }

    @Override
    public String getId() {
        return hashCode() + "";
    }

    @Override
    public void stop() throws Exception {
        try {
            lock.lock();
            try {
                disconnect(DisconnectCause.REBOOTING);
            } catch (Exception e) {
                getLogger().error("disconnect error ", e);

            }

            try {
                if (scheduledExecutorService != null) {
                    scheduledExecutorService.shutdown();
                }

            } catch (Exception e) {
                getLogger().error("shutdown scheduledExecutorService error ", e);
            }


        } finally {
            lock.unlock();
        }
    }

    @Override
    public RemotePeerInfor getRemotePeerInfor() {
        return remotePeerInfor;
    }

    @Override
    public int processCerMessage(IDMessage message) {

        int resultCode = ResultCode.SUCCESS;
        try {
            if (connection == null || !connection.isConnected()) {
                getLogger().debug("Connection is null or not connected. Looking for one in incConnections with key [{}]. Here are the incConnections :");

            }
            // Process cer
            Set<ApplicationId> newAppId = getCommonApplicationIds(message);
            if (newAppId.isEmpty()) {
                getLogger().warn("Processing CER failed, no com.kp.common.common application. Message AppIds [{}]", message.getApplicationIdAvps());

                return ResultCode.NO_COMMON_APPLICATION;
            }


            if (resultCode == ResultCode.SUCCESS) {
                commonApplications.clear();
                commonApplications.addAll(newAppId);
                fillIPAddressTable(message);
            }
        } catch (Exception exc) {
            getLogger().debug("Can not process CER", exc);
        }
        getLogger().debug("CER result [{}]", resultCode);

        return resultCode;
    }

    protected Set<ApplicationId> getCommonApplicationIds(IDMessage message) {
        //it does not include application Ids for which listeners register - and on this  basis it consume message!
        Set<ApplicationId> newAppId = new HashSet<ApplicationId>();
        Set<ApplicationId> locAppId = localInfor.getCommonApplications();
        List<ApplicationId> remAppId = message.getApplicationIdAvps();
        getLogger().debug("Checking com.kp.common.common applications. Remote applications: {}. Local applications: {}", remAppId, locAppId);
        // check com.kp.common.common application
        for (ApplicationId l : locAppId) {
            for (ApplicationId r : remAppId) {
                if (l.equals(r)) {
                    newAppId.add(l);
                } else if (r.getAcctAppId() == INT_COMMON_APP_ID || r.getAuthAppId() == INT_COMMON_APP_ID ||
                        l.getAcctAppId() == INT_COMMON_APP_ID || l.getAuthAppId() == INT_COMMON_APP_ID) {
                    newAppId.add(r);
                }
            }
        }
        return newAppId;
    }


    protected void fillIPAddressTable(IDMessage message) {
        AvpSet avps = message.getAvps().getAvps(HOST_IP_ADDRESS);
        if (avps != null) {
            ArrayList<InetAddress> t = new ArrayList<InetAddress>();
            for (int i = 0; i < avps.size(); i++) {
                try {
                    t.add(avps.getAvpByIndex(i).getAddress());
                } catch (AvpDataException e) {
                    getLogger().warn("Unable to retrieve IP Address from Host-IP-Address AVP");
                }
            }
            addresses = t.toArray(new InetAddress[t.size()]);
        }
    }

    @Override
    public boolean processCeaMessage(IDMessage message) {
        boolean rc = true;
        try {
            Avp origHost = message.getAvps().getAvp(ORIGIN_HOST);
            Avp origRealm = message.getAvps().getAvp(ORIGIN_REALM);
            Avp vendorId = message.getAvps().getAvp(VENDOR_ID);
            Avp prdName = message.getAvps().getAvp(PRODUCT_NAME);
            Avp resCode = message.getAvps().getAvp(RESULT_CODE);
            Avp frmId = message.getAvps().getAvp(FIRMWARE_REVISION);
            if (origHost == null || origRealm == null || vendorId == null) {
                getLogger().warn("Incorrect CEA message (missing mandatory AVPs)");
            } else {
                if (remotePeerInfor.getUri() == null) {
                    remotePeerInfor.setUri(origHost.getDiameterURI());
                }
                if (remotePeerInfor.getRealmName() == null) {
                    remotePeerInfor.setRealmName(origRealm.getDiameterIdentity());
                }
                if (remotePeerInfor.getVendorID() == 0) {
                    remotePeerInfor.setVendorID(vendorId.getUnsigned32());
                }
                fillIPAddressTable(message);
                if (remotePeerInfor.getProductName() == null && prdName != null) {
                    remotePeerInfor.setProductName(prdName.getUTF8String());
                }
                if (resCode != null) {
                    int mrc = resCode.getInteger32();
                    if (mrc != ResultCode.SUCCESS) {
                        getLogger().debug("Result code value {}", mrc);
                        return false;
                    }
                }
                Set<ApplicationId> cai = getCommonApplicationIds(message);
                if (cai.size() > 0) {
                    commonApplications.clear();
                    commonApplications.addAll(cai);
                } else {
                    getLogger().debug("CEA did not contained appId, therefore set local appids to com.kp.common.common-appid field");
                    commonApplications.clear();
                    commonApplications.addAll(localInfor.getCommonApplications());
                }

                if (remotePeerInfor.getFirmWare() == 0 && frmId != null) {
                    remotePeerInfor.setFirmWare(frmId.getInteger32());
                }
            }
        } catch (Exception exc) {
            getLogger().debug("Incorrect CEA message", exc);
            rc = false;
        }
        return rc;
    }


    @Override
    public IDMessage buildCerMessage() throws IOException {

        IDMessage message = parser.createEmptyMessage(CAPABILITIES_EXCHANGE_REQUEST, 0);
        message.setRequest(true);
        message.setHopByHopIdentifier(getHopByHopIdentifier());

        if (localInfor.isUseUriAsFQDN()) {
            message.getAvps().addAvp(ORIGIN_HOST, localInfor.getUri().toString(), true, false, true);
        } else {
            message.getAvps().addAvp(ORIGIN_HOST, localInfor.getUri().getFQDN(), true, false, true);
        }

        message.getAvps().addAvp(ORIGIN_REALM, localInfor.getRealmName(), true, false, true);
        for (InetAddress ia : localInfor.getAddress()) {
            message.getAvps().addAvp(HOST_IP_ADDRESS, ia, true, false);
        }
        message.getAvps().addAvp(VENDOR_ID, localInfor.getVendorId(), true, false, true);
        message.getAvps().addAvp(PRODUCT_NAME, localInfor.getProductName(), false);
        for (ApplicationId appId : localInfor.getCommonApplications()) {
            addAppId(appId, message);
        }
        message.getAvps().addAvp(FIRMWARE_REVISION, localInfor.getFirmware(), true);
        message.getAvps().addAvp(ORIGIN_STATE_ID, localInfor.getLocalHostStateId(), true, false, true);

        return message;

    }

    @Override
    public IDMessage buildCeaMessage(int resultCode, IDMessage cer, String errMessage) throws IOException {
        IDMessage message = parser.createEmptyMessage(IDMessage.CAPABILITIES_EXCHANGE_ANSWER, 0);
        message.setRequest(false);
        message.setHopByHopIdentifier(cer.getHopByHopIdentifier());
        message.setEndToEndIdentifier(cer.getEndToEndIdentifier());
        message.getAvps().addAvp(Avp.ORIGIN_HOST, localInfor.getUri().getFQDN(), true, false, true);
        message.getAvps().addAvp(Avp.ORIGIN_REALM, localInfor.getRealmName(), true, false, true);
        for (InetAddress ia : localInfor.getAddress()) {
            message.getAvps().addAvp(Avp.HOST_IP_ADDRESS, ia, true, false);
        }
        message.getAvps().addAvp(Avp.VENDOR_ID, localInfor.getVendorId(), true, false, true);

        for (ApplicationId appId : localInfor.getCommonApplications()) {
            addAppId(appId, message);
        }

        message.getAvps().addAvp(Avp.PRODUCT_NAME, localInfor.getProductName(), false);
        message.getAvps().addAvp(Avp.RESULT_CODE, resultCode, true, false, true);
        message.getAvps().addAvp(Avp.FIRMWARE_REVISION, localInfor.getFirmware(), true);
        if (errMessage != null) {
            message.getAvps().addAvp(Avp.ERROR_MESSAGE, errMessage, false);
        }
        return message;

    }

    @Override
    public IDMessage buildDwrMessage() throws IOException {
        IDMessage message = parser.createEmptyMessage(DEVICE_WATCHDOG_REQUEST, 0);
        message.setRequest(true);
        message.setHopByHopIdentifier(getHopByHopIdentifier());
        // Set content
        message.getAvps().addAvp(ORIGIN_HOST, localInfor.getUri().getFQDN(), true, false, true);
        message.getAvps().addAvp(ORIGIN_REALM, localInfor.getRealmName(), true, false, true);
        message.getAvps().addAvp(ORIGIN_STATE_ID, localInfor.getLocalHostStateId(), true, false, true);
        // Remove trash avp
        message.getAvps().removeAvp(DESTINATION_HOST);
        message.getAvps().removeAvp(DESTINATION_REALM);
        return message;
    }

    @Override
    public IDMessage buildDwaMessage(IDMessage dwr, int resultCode, String errorMessage) throws IOException {
        IDMessage message = parser.createEmptyMessage(dwr);
        message.setRequest(false);
        message.setHopByHopIdentifier(dwr.getHopByHopIdentifier());
        message.setEndToEndIdentifier(dwr.getEndToEndIdentifier());
        // Set content
        message.getAvps().addAvp(RESULT_CODE, resultCode, true, false, true);
        message.getAvps().addAvp(ORIGIN_HOST, localInfor.getUri().getFQDN(), true, false, true);
        message.getAvps().addAvp(ORIGIN_REALM, localInfor.getRealmName(), true, false, true);
        if (errorMessage != null) {
            message.getAvps().addAvp(ERROR_MESSAGE, errorMessage, false);
        }
        // Remove trash avp
        message.getAvps().removeAvp(DESTINATION_HOST);
        message.getAvps().removeAvp(DESTINATION_REALM);
        return message;
    }

    @Override
    public IDMessage buildDprMessage(int disconnectCause) throws IOException {
        IDMessage message = parser.createEmptyMessage(DISCONNECT_PEER_REQUEST, 0);
        message.setRequest(true);
        message.setHopByHopIdentifier(getHopByHopIdentifier());
        message.getAvps().addAvp(ORIGIN_HOST, localInfor.getUri().getFQDN(), true, false, true);
        message.getAvps().addAvp(ORIGIN_REALM, localInfor.getRealmName(), true, false, true);
        message.getAvps().addAvp(DISCONNECT_CAUSE, disconnectCause, true, false);
        return message;

    }

    @Override
    public void setLocal(boolean local) {
        isLocal = local;
    }

    @Override
    public IDMessage buildDpaMessage(IDMessage dpr, int resultCode, String errorMessage) throws IOException {
        IDMessage message = parser.createEmptyMessage(dpr);
        message.setRequest(false);
        message.setHopByHopIdentifier(dpr.getHopByHopIdentifier());
        message.setEndToEndIdentifier(dpr.getEndToEndIdentifier());
        message.getAvps().addAvp(RESULT_CODE, resultCode, true, false, true);
        message.getAvps().addAvp(ORIGIN_HOST, localInfor.getUri().getFQDN(), true, false, true);
        message.getAvps().addAvp(ORIGIN_REALM, localInfor.getRealmName(), true, false, true);
        if (errorMessage != null) {
            message.getAvps().addAvp(ERROR_MESSAGE, errorMessage, false);
        }
        return message;

    }

    public IConnection getConnection() {
        return connection;
    }

    @Override
    public void disconnect(int disconnectCause) {
        try {
            if (hasValidConnection()) {
                IDMessage idMessage = buildDprMessage(disconnectCause);
                Future future = _sendMessage(idMessage);

            } else {
                established.cancel(true);

            }
        } catch (Exception e) {
            getLogger().error("disconnect ", e);

        } finally {
            if (connection.isConnected()) {
                try {
                    connection.disconnect();
                } catch (Exception e) {
                    getLogger().error("Connection close ", e);

                }

            }
            try {
                peerListenerManager.firePeerClose(this);

            } catch (Exception e) {
                getLogger().error(" peerListenerManager.firePeerClose ", e);

            }
        }

    }

    private void onMessageReceived(IDMessage message) {

        try {

            IDMessage response;
            int cmd = message.getCommandCode();
            switch (cmd) {

                case IDMessage.DEVICE_WATCHDOG_REQUEST:
                    try {
                        response = buildDwaMessage(message, ResultCode.SUCCESS, null);
                        _notifyToClient(response);

                    } catch (Throwable e) {
                        getLogger().warn("Can not send DPA", e);
                    }
                    break;
                case IDMessage.DISCONNECT_PEER_REQUEST:
                    if (message.isRequest()) {
                        try {
                            response = buildDpaMessage(message, ResultCode.SUCCESS, null);
                            _notifyToClient(response);

                        } catch (Throwable e) {
                            getLogger().warn("Can not send DPA", e);
                        }
                    }
                    try {
                        connection.disconnect();
                    } catch (Exception e) {
                        getLogger().warn("Disconnect connection error ", e);

                    }
                    break;
                default:
                    peerListenerManager.fireMessageReceived(AbstactPeerImpl.this, message);
                    break;


            }


        } catch (Exception e) {
            getLogger().error(" peerListenerManager.fireMessageReceived ", e);

        }


    }

    @Override
    public void handleMessage(IDMessage message, IConnection connection) {

        int cmd = message.getCommandCode();
        if (message.isRequest()) {
            if (cmd == CAPABILITIES_EXCHANGE_REQUEST) {

                try {
                    int result = processCerMessage(message);
                    notifyMessage(buildCeaMessage(result, message, null));
                    established.complete(AbstactPeerImpl.this);
                    if (result != SUCCESS) {
                        throw new Exception("cer not valid");
                    }

                } catch (Throwable e) {

                    try {
                        connection.disconnect();
                    } catch (Exception e1) {
                        getLogger().error("connection.disconnect ", e1);
                    }
                    getLogger().warn("Can not send CEA", e);
                }


            } else if (hasValidConnection()) {
                onMessageReceived(message);


            } else {
                getLogger().error("peer is not establish {}", message);

            }

        } else {

            if (message.getCommandCode() == CAPABILITIES_EXCHANGE_ANSWER) {
                if (processCeaMessage(message)) {
                    established.complete(AbstactPeerImpl.this);
                } else {
                    try {
                        connection.disconnect();
                    } catch (Exception e) {
                        getLogger().error("DISCONNECT_PEER ", e);

                    }
                    established.cancel(true);
                }

            } else if (cmd != DEVICE_WATCHDOG_REQUEST) {
                if (hasValidConnection()) {
                    onMessageReceived(message);
                } else {
                    getLogger().error("peer is not establish {}", message);

                }
            }


        }
    }

    @Override
    public long getHopByHopIdentifier() {
        long nextId = hopByHopId.updateAndGet(current -> current == Integer.MAX_VALUE ? 0 : current + 1);
        return nextId;

    }

    protected Future<IDMessage> _sendMessage(IDMessage message) throws Exception {

        return connection.sendAsync(message);
    }

    protected void _notifyToClient(IDMessage message) throws Exception {

        connection.sendNotify(message);


    }

    @Override
    public void notifyMessage(IDMessage message) throws Exception {
        if (hasValidConnection()) {

            connection.sendNotify(message);

        } else {
            throw new IOException("peer is not established");
        }

    }

    @Override
    public Future<IDMessage> sendMessage(IDMessage message) throws Exception {
        if (hasValidConnection()) {
            if (message.isRequest()) {
                message.setHopByHopIdentifier(getHopByHopIdentifier());
            }
            return connection.sendAsync(message);
        } else {
            throw new IOException("peer is not established");
        }
    }


    @Override
    public boolean hasValidConnection() {
        if (isConnected()) {
            try {
                established.get(localInfor.getTimeout(), TimeUnit.MILLISECONDS);

            } catch (Exception e) {
                return false;
            }
            return true;

        }
        return false;
    }


    protected void addAppId(ApplicationId appId, IDMessage message) {
        if (appId.getVendorId() == 0) {
            if (appId.getAuthAppId() != 0) {
                message.getAvps().addAvp(AUTH_APPLICATION_ID, appId.getAuthAppId(), true, false, true);
            } else if (appId.getAcctAppId() != 0) {
                message.getAvps().addAvp(ACCT_APPLICATION_ID, appId.getAcctAppId(), true, false, true);
            }
        } else {
            // Avoid duplicates
            boolean vendorIdPresent = false;
            for (Avp avp : message.getAvps().getAvps(SUPPORTED_VENDOR_ID)) {
                try {
                    if (avp.getUnsigned32() == appId.getVendorId()) {
                        vendorIdPresent = true;
                        break;
                    }
                } catch (Exception e) {
                    getLogger().warn("Failed to read Supported-Vendor-Id.", e);
                }
            }
            if (!vendorIdPresent) {
                message.getAvps().addAvp(SUPPORTED_VENDOR_ID, appId.getVendorId(), true, false, true);
            }
            AvpSet vendorApp = message.getAvps().addGroupedAvp(VENDOR_SPECIFIC_APPLICATION_ID, true, false);
            vendorApp.addAvp(VENDOR_ID, appId.getVendorId(), true, false, true);
            if (appId.getAuthAppId() != 0) {
                vendorApp.addAvp(AUTH_APPLICATION_ID, appId.getAuthAppId(), true, false, true);
            }
            if (appId.getAcctAppId() != 0) {
                vendorApp.addAvp(ACCT_APPLICATION_ID, appId.getAcctAppId(), true, false, true);
            }
        }
    }

    @Override
    public boolean isConnected() {
        return connection.isConnected();
    }
}
