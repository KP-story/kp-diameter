package com.kp.diameter.config;

import com.kp.diameter.api.message.URI;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class RemotePeerInfor {
    protected long vendorID;
    protected String productName;
    protected int firmWare;
    SocketAddress address;
    private URI uri;
    private String realmName;

    @Override
    public String toString() {
        return "RemotePeerInfor{" +
                "address=" + address +
                ", uri=" + uri +
                ", realmName='" + realmName + '\'' +
                ", vendorID=" + vendorID +
                ", productName='" + productName + '\'' +
                ", firmWare=" + firmWare +
                '}';
    }

    public SocketAddress getAddress() {
        if (address == null) {
            address = new InetSocketAddress(uri.getFQDN(), uri.getPort());
        }
        return address;
    }

    public void setAddress(SocketAddress address) {
        this.address = address;
    }

    public long getVendorID() {
        return vendorID;
    }

    public void setVendorID(long vendorID) {
        this.vendorID = vendorID;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getFirmWare() {
        return firmWare;
    }

    public void setFirmWare(int firmWare) {
        this.firmWare = firmWare;
    }

    public String getRealmName() {
        return realmName;
    }

    public void setRealmName(String realmName) {
        this.realmName = realmName;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

}
