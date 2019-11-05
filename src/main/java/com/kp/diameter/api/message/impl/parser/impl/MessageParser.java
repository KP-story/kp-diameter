/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, TeleStax Inc. and individual contributors
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

package com.kp.diameter.api.message.impl.parser.impl;


import com.kp.common.log.Loggable;
import com.kp.common.utilities.DataTypeConverter;
import com.kp.diameter.api.message.AvpDataException;
import com.kp.diameter.api.message.IDMessage;
import com.kp.diameter.api.message.impl.ApplicationId;
import com.kp.diameter.api.message.impl.MessageImpl;
import com.kp.diameter.api.message.impl.parser.Avp;
import com.kp.diameter.api.message.impl.parser.AvpSet;
import com.kp.diameter.api.message.impl.parser.IDMessageParser;
import com.kp.diameter.config.LocalInfor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;

import static com.kp.diameter.api.message.impl.parser.Avp.*;


/**
 * @author erick.svenson@yahoo.com
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 * @author <a href="mailto:baranowb@gmail.com"> Bartosz Baranowski </a>
 */
public class MessageParser extends ElementParser implements IDMessageParser, Loggable {


    private AtomicLong endtoend = new AtomicLong(0);

    public MessageParser() {

    }

    public static void addOriginAvps(IDMessage m, LocalInfor md) {
        AvpSet set = m.getAvps();
        if (set.getAvp(264) == null) {
            m.getAvps().addAvp(264, md.getUri().getFQDN(), true, false, true);
        }

        if (set.getAvp(296) == null) {
            m.getAvps().addAvp(296, md.getRealmName(), true, false, true);
        }

    }

    public void appendAppId(ApplicationId appId, IDMessage m) {
        if (appId != null) {
            Iterator var3 = m.getAvps().iterator();

            int code;
            do {
                if (!var3.hasNext()) {
                    if (appId.getVendorId() == 0L) {
                        if (appId.getAcctAppId() != 0L) {
                            m.getAvps().addAvp(259, appId.getAcctAppId(), true, false, true);
                        }

                        if (appId.getAuthAppId() != 0L) {
                            m.getAvps().addAvp(258, appId.getAuthAppId(), true, false, true);
                        }
                    } else {
                        AvpSet avp = m.getAvps().addGroupedAvp(260, true, false);
                        avp.addAvp(266, appId.getVendorId(), true, false, true);
                        if (appId.getAuthAppId() != 0L) {
                            avp.addAvp(258, appId.getAuthAppId(), true, false, true);
                        }

                        if (appId.getAcctAppId() != 0L) {
                            avp.addAvp(259, appId.getAcctAppId(), true, false, true);
                        }
                    }

                    return;
                }

                Avp avp = (Avp) var3.next();
                code = avp.getCode();
            } while (code != 259 && code != 258 && code != 260);

        }
    }

    public long getAppId(ApplicationId appId) {
        if (appId == null) {
            return 0L;
        } else if (appId.getAcctAppId() != 0L) {
            return appId.getAcctAppId();
        } else {
            return appId.getAuthAppId() != 0L ? appId.getAuthAppId() : appId.getVendorId();
        }
    }

    @Override
    public IDMessage createMessage(byte[] message) throws AvpDataException {
        // Read header
        try {
            long tmp;
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));
            tmp = in.readInt();
            short version = (short) (tmp >> 24);
            if (version != 1) {
                throw new Exception("Illegal value of version " + version);
            }

            if (message.length != (tmp & 0x00FFFFFF)) {
                //throw new ParseException("Wrong length of data: " + (tmp & 0x00FFFFFF));
                throw new Exception("Wrong length of data: " + (tmp & 0x00FFFFFF));
            }

            tmp = in.readInt();
            short flags = (short) ((tmp >> 24) & 0xFF);
            int commandCode = (int) (tmp & 0xFFFFFF);
            long applicationId = ((long) in.readInt() << 32) >>> 32;
            long hopByHopId = ((long) in.readInt() << 32) >>> 32;
            long endToEndId = ((long) in.readInt() << 32) >>> 32;

            AvpSetImpl avpSet = decodeAvpSet(message, 20);

            return new MessageImpl(commandCode, applicationId, flags, hopByHopId, endToEndId, avpSet);
        } catch (Exception exc) {
            throw new AvpDataException(exc);
        }
    }

    @Override
    public IDMessage createMessage(ByteBuffer data) throws AvpDataException {
        byte[] message = data.array();
        return createMessage(message);
    }

    @Override
    public <T> T createMessage(Class<?> iface, ByteBuffer data) throws AvpDataException {
        if (iface == IDMessage.class) {
            return (T) createMessage(data);
        }
        return null;
    }

    @Override
    public <T> T createEmptyMessage(Class<?> iface, IDMessage parentMessage) {

        return (T) createEmptyMessage(parentMessage, parentMessage.getCommandCode());


    }

    @Override
    public IDMessage createEmptyMessage(IDMessage prnMessage) {
        return createEmptyMessage(prnMessage, prnMessage.getCommandCode());
    }

    @Override
    public IDMessage createEmptyMessage(IDMessage prnMessage, int commandCode) {
        //
        MessageImpl newMessage = new MessageImpl(
                commandCode,
                prnMessage.getHeaderApplicationId(),
                (short) prnMessage.getFlags(),
                prnMessage.getHopByHopIdentifier(),
                getNextEndToEndId(),
                null
        );
        copyBasicAvps(newMessage, prnMessage, false);

        return newMessage;
    }

    public void copyBasicAvps(IDMessage newMessage, IDMessage prnMessage, boolean invertPoints) {
        //left it here, but
        Avp avp;
        // Copy session id's information
        {
            avp = prnMessage.getAvps().getAvp(Avp.SESSION_ID);
            if (avp != null) {
                newMessage.getAvps().addAvp(new AvpImpl(avp));
            }
            avp = prnMessage.getAvps().getAvp(Avp.ACC_SESSION_ID);
            if (avp != null) {
                newMessage.getAvps().addAvp(new AvpImpl(avp));
            }
            avp = prnMessage.getAvps().getAvp(Avp.ACC_SUB_SESSION_ID);
            if (avp != null) {
                newMessage.getAvps().addAvp(new AvpImpl(avp));
            }
            avp = prnMessage.getAvps().getAvp(Avp.ACC_MULTI_SESSION_ID);
            if (avp != null) {
                newMessage.getAvps().addAvp(new AvpImpl(avp));
            }
        }
        // Copy Applicatio id's information
        {
            avp = prnMessage.getAvps().getAvp(VENDOR_SPECIFIC_APPLICATION_ID);
            if (avp != null) {
                newMessage.getAvps().addAvp(new AvpImpl(avp));
            }
            avp = prnMessage.getAvps().getAvp(ACCT_APPLICATION_ID);
            if (avp != null) {
                newMessage.getAvps().addAvp(new AvpImpl(avp));
            }
            avp = prnMessage.getAvps().getAvp(AUTH_APPLICATION_ID);
            if (avp != null) {
                newMessage.getAvps().addAvp(new AvpImpl(avp));
            }
        }
        // Copy proxy information
        {
            AvpSet avps = prnMessage.getAvps().getAvps(Avp.PROXY_INFO);
            for (Avp piAvp : avps) {
                newMessage.getAvps().addAvp(new AvpImpl(piAvp));
            }
        }
        // Copy route information
        {
            if (newMessage.isRequest()) {
                if (invertPoints) {
                    // set Dest host
                    avp = prnMessage.getAvps().getAvp(Avp.ORIGIN_HOST);
                    if (avp != null) {
                        newMessage.getAvps().addAvp(new AvpImpl(Avp.DESTINATION_HOST, avp));
                    }
                    // set Dest realm
                    avp = prnMessage.getAvps().getAvp(Avp.ORIGIN_REALM);
                    if (avp != null) {
                        newMessage.getAvps().addAvp(new AvpImpl(Avp.DESTINATION_REALM, avp));
                    }
                } else {
                    // set Dest host
                    avp = prnMessage.getAvps().getAvp(Avp.DESTINATION_HOST);
                    if (avp != null) {
                        newMessage.getAvps().addAvp(avp);
                    }
                    // set Dest realm
                    avp = prnMessage.getAvps().getAvp(Avp.DESTINATION_REALM);
                    if (avp != null) {
                        newMessage.getAvps().addAvp(avp);
                    }
                }
            }
            //      // set Orig host and realm
            //      try {
            //        newMessage.getAvps().addAvp(Avp.ORIGIN_HOST, metadata.getLocalPeer().getUri().getFQDN(), true, false, true);
            //        newMessage.getAvps().addAvp(Avp.ORIGIN_REALM, metadata.getLocalPeer().getRealmName(), true, false, true);
            //      }
            //      catch (Exception e) {
            //        logger.debug("Error copying Origin-Host/Realm AVPs", e);
            //      }
        }
    }

    @Override
    public ByteBuffer encode(IDMessage message) throws ParseException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            byte[] rawData = encodeAvpSet(message.getAvps());
            DataOutputStream data = new DataOutputStream(out);
            // Wasting processor time, are we ?
            // int tmp = (1 << 24) & 0xFF000000;
            int tmp = (1 << 24);
            tmp += 20 + rawData.length;
            data.writeInt(tmp);
            // Again, unneeded operation ?
            // tmp = (message.getFlags() << 24) & 0xFF000000;
            tmp = (message.getFlags() << 24);
            tmp += message.getCommandCode();
            data.writeInt(tmp);
            data.write(DataTypeConverter.longToBytes(message.getHeaderApplicationId()));
            data.write(DataTypeConverter.longToBytes(message.getHopByHopIdentifier()));
            data.write(DataTypeConverter.longToBytes(message.getEndToEndIdentifier()));
            data.write(rawData);
        } catch (Exception e) {
            //logger.debug("Error during encode message", e);
            throw new ParseException("Failed to encode message.", e);
        }
        try {
            return prepareBuffer(out.toByteArray(), out.size());
        } catch (AvpDataException ade) {
            throw new ParseException(ade);
        }
    }

    @Override
    public IDMessage createEmptyMessage(int commandCode, long headerAppId) {
        return new MessageImpl(commandCode, headerAppId);
    }

    @Override
    public <T> T createEmptyMessage(Class<?> iface, int commandCode, long headerAppId) {
        return (T) new MessageImpl(commandCode, headerAppId);
    }


    public long getNextEndToEndId() {
        long nextId = endtoend.updateAndGet(current -> current == Integer.MAX_VALUE ? 0 : current + 1);
        return nextId;
    }
}
