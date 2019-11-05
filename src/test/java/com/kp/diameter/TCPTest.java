package com.kp.diameter;

import com.kp.common.data.message.IMessageParser;
import com.kp.diameter.api.message.Answer;
import com.kp.diameter.api.message.IDMessage;
import com.kp.diameter.api.message.Request;
import com.kp.diameter.api.message.ResultCode;
import com.kp.diameter.api.message.impl.parser.IDMessageParser;
import com.kp.diameter.network.client.netty.tcp.DiameterTcpClient;
import com.kp.diameter.network.codec.NettyTcpMessageParser;
import com.kp.diameter.network.server.netty.tcp.DiameterTcpServer;
import com.kp.network.DefaultFutureManager;
import com.kp.network.FutureManager;
import com.kp.network.connection.DefaultConnectionManager;
import com.kp.network.connection.IConnection;
import com.kp.network.connection.IConnectionManager;
import com.kp.network.event.impl.ConnectionListener;
import com.kp.network.event.impl.DefaultConnectionListenerManager;
import com.kp.network.event.impl.IConnectionListenerManager;
import com.kp.network.utilities.IPConverter;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import static com.kp.diameter.api.message.impl.parser.Avp.*;

public class TCPTest {
    public static void main(String[] args) throws Exception {
        createServer();
        createClient();


    }


    public static void createClient() throws Exception {

        FutureManager<String, IDMessage> futureManager = new DefaultFutureManager<>();
        IMessageParser<ByteBuf, IDMessage> messageParser = new NettyTcpMessageParser();
        IConnectionListenerManager<IDMessage> connectionListenerManager = new DefaultConnectionListenerManager<>();
        DiameterTcpClient client = new DiameterTcpClient(connectionListenerManager, messageParser);
        client.setTimeout(20000);
        client.addRemoteAddress(new InetSocketAddress("localhost", 3868));
//        client.addLocalAddress(new InetSocketAddress("localhost",9022));

        client.setNWorker(12);
        connectionListenerManager.init();
        client.connect();
        client.addConnectionListener("khanhlv", new ConnectionListener<IDMessage>() {
            @Override
            public void connectionOpened(IConnection connection) {
                System.out.println("open");
            }

            @Override
            public void connectionClosed(IConnection connection) {
                System.out.println("close");

            }

            @Override
            public void messageReceived(IConnection connection, IDMessage message) {
                System.out.println("message den {}" + message.toString());

            }

            @Override
            public void internalError(IConnection connection, IDMessage message, Throwable cause) {

            }

        });
        while (true) {
            client.send(dwrMessage(messageParser));
            Thread.sleep(500);
        }

    }


    public static IDMessage dwrMessage(IMessageParser parser) {
        IDMessageParser idMessageParser = (IDMessageParser) parser;
        IDMessage message = idMessageParser.createEmptyMessage(IDMessage.DEVICE_WATCHDOG_REQUEST, 0);
        message.setRequest(true);
        message.setHopByHopIdentifier(message.getEndToEndIdentifier());
        // Set content
        message.getAvps().addAvp(ORIGIN_HOST, "khanhlv", true, false, true);
        message.getAvps().addAvp(ORIGIN_REALM, "hehehe", true, false, true);
        message.getAvps().addAvp(ORIGIN_STATE_ID, 1, true, false, true);
        // Remove trash avp
        message.getAvps().removeAvp(DESTINATION_HOST);
        message.getAvps().removeAvp(DESTINATION_REALM);
        // Send
        return message;
    }

    public static IDMessage dwaMessage(IDMessage dwr, int resultCode, String errorMessage) {

        // Send
        Request request = (Request) dwr;

        Answer answer = request.createAnswer(ResultCode.SUCCESS);
        answer.getAvps().addAvp(ORIGIN_HOST, "khanhlv", true, false, true);
        answer.getAvps().addAvp(ORIGIN_REALM, "hehehe", true, false, true);

        return answer;
    }


    public static void createServer() throws Exception {


        IConnectionManager<IDMessage, Channel> connectionManager = new DefaultConnectionManager<>();
        IConnectionListenerManager<IDMessage> connectionListenerManager = new DefaultConnectionListenerManager<>();
        IMessageParser<ByteBuf, IDMessage> messageParser = new NettyTcpMessageParser();


        DiameterTcpServer server = new DiameterTcpServer(connectionManager, connectionListenerManager, messageParser);
        InetAddress inetAddress = IPConverter.InetAddressByIPv4("localhost");
        server.addLocalAddress(inetAddress);
        server.addLocalPort(3868);
        server.setNboot(1);
        server.setNworker(1);
        server.setRcvbuf(10);
        server.setTimeout(12000);
        server.addConnectionListener("khanhlv", new ConnectionListener<IDMessage>() {
            @Override
            public void connectionOpened(IConnection connection) {
                System.out.println("connect" + connection);
            }

            @Override
            public void connectionClosed(IConnection connection) {
                System.out.println("disconnect" + connection);

            }

            @Override
            public void messageReceived(IConnection connection, IDMessage message) {
                System.out.println("message vao {}" + message.toString());

                try {
                    IDMessage idMessage = dwaMessage(message, 0, null);
                    connection.send(idMessage);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void internalError(IConnection connection, IDMessage message, Throwable cause) {


            }
        });

        server.init();

    }


}
