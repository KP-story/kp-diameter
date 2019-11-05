package com.kp.diameter.config;

import com.kp.diameter.api.factory.NetworkFactory;
import com.kp.diameter.api.factory.PeerFactory;
import com.kp.diameter.api.factory.impl.PeerFactoryImpl;
import com.kp.diameter.api.factory.impl.SctpNettyNetworkFactory;
import com.kp.diameter.api.message.URI;
import com.kp.diameter.api.message.impl.ApplicationId;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LocalInfor {
    private boolean useUriAsFQDN = false; // Use URI as origin host name into CER command
    private List<InetAddress> addresses = new LinkedList<>();
    private URI uri;
    private long state;
    private long vendorId;
    private String productName;
    private int firmware;
    private int timeout;
    private Map<String, Class> extension = new ConcurrentHashMap<>();
    private Set<ApplicationId> commonApplications = new HashSet<ApplicationId>();
    private String realmName;

    public LocalInfor() {
        extension.put(NetworkFactory.class.getSimpleName(), SctpNettyNetworkFactory.class);
        extension.put(PeerFactory.class.getSimpleName(), PeerFactoryImpl.class);


    }

    @Override
    public String toString() {
        return "LocalInfor{" +
                "useUriAsFQDN=" + useUriAsFQDN +
                ", addresses=" + addresses +
                ", uri=" + uri +
                ", state=" + state +
                ", vendorId=" + vendorId +
                ", productName='" + productName + '\'' +
                ", firmware=" + firmware +
                ", timeout=" + timeout +
                ", extension=" + extension +
                ", commonApplications=" + commonApplications +
                ", realmName='" + realmName + '\'' +
                '}';
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public List<InetAddress> getAddress() {

        return addresses;
    }

    public void addAddress(InetAddress socketAddress) {
        addresses.add(socketAddress);
    }

    public Map<String, Class> getExtension() {
        return extension;
    }

    public void addExtension(String name, Class cl) {
        this.extension.put(name, cl);
    }

    public boolean isUseUriAsFQDN() {
        return useUriAsFQDN;
    }

    public void setUseUriAsFQDN(boolean useUriAsFQDN) {
        this.useUriAsFQDN = useUriAsFQDN;
    }

    public Set<ApplicationId> getCommonApplications() {
        return commonApplications;
    }

    public void addCommonApplications(ApplicationId commonApplication) {
        this.commonApplications.add(commonApplication);
    }

    public URI getUri() {

        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public long getVendorId() {
        return vendorId;
    }

    public void setVendorId(long vendorId) {
        this.vendorId = vendorId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public void updateLocalHostStateId() {
        state = System.currentTimeMillis();
    }

    public int getFirmware() {
        return firmware;
    }

    public void setFirmware(int firmware) {
        this.firmware = firmware;
    }

    public long getLocalHostStateId() {
        return state;
    }

    public String getRealmName() {
        return realmName;
    }

    public void setRealmName(String realmName) {
        this.realmName = realmName;
    }

}
