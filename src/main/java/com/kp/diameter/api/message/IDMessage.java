package com.kp.diameter.api.message;

import com.kp.common.data.message.IMessage;
import com.kp.diameter.api.message.impl.ApplicationId;
import com.kp.diameter.api.message.impl.parser.AvpSet;

import java.util.List;

public interface IDMessage extends IMessage {

    /**
     * The Abort-Session-Request message code
     */
    int ABORT_SESSION_REQUEST = 274;

    /**
     * The Abort-Session-Answer message code
     */
    int ABORT_SESSION_ANSWER = 274;

    /**
     * The Accounting-Request message code
     */
    int ACCOUNTING_REQUEST = 271;

    /**
     * The Accounting-Answer message code
     */
    int ACCOUNTING_ANSWER = 271;

    /**
     * The Capabilities-Exchange-Request message code
     */
    int CAPABILITIES_EXCHANGE_REQUEST = 257;

    /**
     * The Capabilities-Exchange-Answer message code
     */
    int CAPABILITIES_EXCHANGE_ANSWER = 257;

    /**
     * The Device-Watchdog-Request message code
     */
    int DEVICE_WATCHDOG_REQUEST = 280;

    /**
     * The Device-Watchdog-Answer message code
     */
    int DEVICE_WATCHDOG_ANSWER = 280;

    /**
     * The Disconnect-Peer-Request message code
     */
    int DISCONNECT_PEER_REQUEST = 282;

    /**
     * The Disconnect-Peer-Answer message code
     */
    int DISCONNECT_PEER_ANSWER = 282;

    /**
     * The Re-Auth-Request message code
     */
    int RE_AUTH_REQUEST = 258;

    /**
     * The Re-Auth-Answer message code
     */
    int RE_AUTH_ANSWER = 258;

    /**
     * The Session-Termination-Request message code
     */
    int SESSION_TERMINATION_REQUEST = 275;

    /**
     * The Session-Termination-Answer message code
     */
    int SESSION_TERMINATION_ANSWER = 275;


    /**
     * The message is not sent to the network
     */
    int STATE_NOT_SENT = 0;

    /**
     * The message has been sent to the network
     */
    int STATE_SENT = 1;

    /**
     * The message is buffered ( not use yet )
     */
    int STATE_BUFFERED = 2;

    /**
     * Stack received answer to this message
     */
    int STATE_ANSWERED = 3;

    /**
     * Return state of message
     *
     * @return state of message
     */
    int getState();

    /**
     * Set new state
     *
     * @param newState new state value
     */
    void setState(int newState);

    /**
     * Return header applicationId
     *
     * @return header applicationId
     */
    long getHeaderApplicationId();

    /**
     * Set header message application id
     *
     * @param applicationId header message application id
     */
    void setHeaderApplicationId(long applicationId);

    /**
     * Return flags as inteher
     *
     * @return flags as inteher
     */
    int getFlags();

    /**
     * Return application id
     *
     * @return application id
     */
    ApplicationId getSingleApplicationId();

    /**
     * Return application id
     *
     * @return application id
     */
    ApplicationId getSingleApplicationId(long id);

    /**
     * Create clone object
     *
     * @return clone
     */
    Object clone();

    /**
     * @return version of message (version filed in header)
     */
    byte getVersion();

    /**
     * @return value of R bit from header of message
     */
    boolean isRequest();

    /**
     * Set 1 or 0 to R bit field of header
     *
     * @param value true == 1 or false = 0
     */
    void setRequest(boolean value);

    /**
     * @return value of P bit from header of message
     */
    boolean isProxiable();

    /**
     * Set 1 or 0 to P bit field of header
     *
     * @param value true == 1 or false = 0
     */
    void setProxiable(boolean value);

    /**
     * @return value of E bit from header of message
     */
    boolean isError();

    /**
     * Set 1 or 0 to E bit field of header
     *
     * @param value true == 1 or false = 0
     */
    void setError(boolean value);

    /**
     * @return value of T bit from header of message
     */
    boolean isReTransmitted();

    /**
     * Set 1 or 0 to T bit field of header
     *
     * @param value true == 1 or false = 0
     */
    void setReTransmitted(boolean value);

    /**
     * @return command code from header of message
     */
    int getCommandCode();

    /**
     * Return message Session Id avp Value (null if avp not set)
     *
     * @return session id avp of message
     */
    String getSessionId();

    /**
     * Return ApplicationId value from message header
     *
     * @return ApplicationId value from message header
     */
    long getApplicationId();

    /**
     * Returns ordered list of Application-Id avps (Auth-Application-Id, Acc-Appplication-Id and Vendor-Specific-Application-Id avps) from message
     *
     * @return list of Application-Id avps
     */
    List<ApplicationId> getApplicationIdAvps();

    /**
     * The Hop-by-Hop Identifier is an unsigned 32-bit integer field (in
     * network byte order) and aids in matching requests and replies. The
     * sender MUST ensure that the Hop-by-Hop identifier in a request is
     * unique on a given connection at any given time, and MAY attempt to
     * ensure that the number is unique across reboots.
     *
     * @return hop by hop identifier from header of message
     */
    long getHopByHopIdentifier();

    /**
     * Set hop by hop id
     *
     * @param hopByHopId hopByHopId value
     */
    void setHopByHopIdentifier(long hopByHopId);

    /**
     * The End-to-End Identifier is an unsigned 32-bit integer field (in
     * network byte order) and is used to detect duplicate messages. Upon
     * reboot implementations MAY set the high order 12 bits to contain
     * the low order 12 bits of current time, and the low order 20 bits
     * to a random value. Senders of request messages MUST insert a
     * unique identifier on each message.
     *
     * @return end to end identifier from header of message
     */
    long getEndToEndIdentifier();

    /**
     * Set end by end id
     *
     * @param endByEndId endByEndId value
     */
    void setEndToEndIdentifier(long endByEndId);

    /**
     * @return Set of message Avps
     */
    AvpSet getAvps();


}
