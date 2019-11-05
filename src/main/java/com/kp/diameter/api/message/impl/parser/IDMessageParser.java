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

package com.kp.diameter.api.message.impl.parser;


import com.kp.diameter.api.message.AvpDataException;
import com.kp.diameter.api.message.IDMessage;
import com.kp.diameter.api.message.impl.parser.impl.ParseException;

import java.nio.ByteBuffer;

/**
 * Basic interface for com.kp.diameter message parsers.
 *
 * @author erick.svenson@yahoo.com
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 * @author <a href="mailto:baranowb@gmail.com"> Bartosz Baranowski </a>
 */
public interface IDMessageParser {

    /**
     * Create message from bytebuffer
     *
     * @param data message bytebuffer
     * @return instance of message
     * @throws AvpDataException
     */
    IDMessage createMessage(ByteBuffer data) throws AvpDataException;

    /**
     * Create message from byte array
     *
     * @param data message byte array
     * @return instance of message
     * @throws AvpDataException
     */
    IDMessage createMessage(byte[] message) throws AvpDataException;

    /**
     * Created specified type of message
     *
     * @param iface type of message
     * @param data  message bytebuffer
     * @return instance of message
     * @throws AvpDataException
     */
    <T> T createMessage(Class<?> iface, ByteBuffer data) throws AvpDataException;

    /**
     * Created empty message
     *
     * @param commandCode message command code
     * @param headerAppId header applicatio id
     * @return instance of message
     */
    IDMessage createEmptyMessage(int commandCode, long headerAppId);

    /**
     * Created specified type of message
     *
     * @param iface       type of message
     * @param commandCode message command code
     * @param headerAppId header applicatio id
     * @return instance of message
     */
    <T> T createEmptyMessage(Class<?> iface, int commandCode, long headerAppId);

    /**
     * Created new message with copied of header of parent message
     *
     * @param parentMessage parent message
     * @return instance of message
     */
    IDMessage createEmptyMessage(IDMessage parentMessage);

    /**
     * Created new message with copied of header of parent message
     *
     * @param parentMessage parent message
     * @param commandCode   new command code value
     * @return instance of message
     */
    IDMessage createEmptyMessage(IDMessage parentMessage, int commandCode);

    /**
     * Created new message with copied of header of parent message
     *
     * @param iface         type of message
     * @param parentMessage parent message
     * @return instance of message
     */
    <T> T createEmptyMessage(Class<?> iface, IDMessage parentMessage);

    /**
     * Encode message to ByteBuffer
     *
     * @param message com.kp.diameter message
     * @return instance of message
     * @throws ParseException
     */
    ByteBuffer encode(IDMessage message) throws ParseException;

}
