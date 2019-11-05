package com.kp.diameter.api.message.impl.parser;

import com.kp.common.utilities.TBCDUtil;
import com.kp.diameter.api.message.AvpDataException;
import com.kp.diameter.api.message.IDMessage;
import com.kp.diameter.api.validation.AvpRepresentation;
import com.kp.diameter.api.validation.MessageRepresentation;
import com.kp.diameter.api.validation.impl.DictionaryImpl;
import org.apache.commons.codec.binary.Hex;

import java.util.Iterator;


public class MessageUtil {
    public static final int UTF8_STRING = 1;
    public static final int UNSIGNED32 = 2;
    public static final int OCTET_STRING = 3;
    public static final int DIAM_IDENT = 4;
    public static final int ENUMERATED = 5;
    public static final int UNSIGNED64 = 6;
    /**
     * @param message IMessage
     * @return String
     * @throws AvpDataException
     */
    private static final String ENTER = "       ";
    private static final String IDENT = "  ";

    /**
     * @param message IMessage
     * @return String
     * @throws AvpDataException
     */

    public static String toString(IDMessage message) {
        StringBuilder buff = new StringBuilder();

        try {


            MessageRepresentation mr = DictionaryImpl.INSTANCE.getMessage(message.getCommandCode(), message.getApplicationId(),
                    message.isRequest()
            );

            if (mr != null) {
                buff.append("Command(").append(message.getCommandCode()).append(") ").append(mr.getName()).append((message.
                        getFlags() & 0x80) != 0 ? " Request" :
                        " Answer").append(ENTER);
            } else {
                buff.append("Command Code:").append(message.getCommandCode()).append((message.getFlags() & 0x80) != 0 ?
                        " Request" :
                        " Answer").append(ENTER);
            }
            buff.append("Application id:").append(message.getApplicationId()).append(ENTER);
            buff.append("HopByHop id:0x").append(Long.toHexString(message.getHopByHopIdentifier())).append(ENTER);
            buff.append("EndToEnd id:0x").append(Long.toHexString(message.getEndToEndIdentifier())).append(ENTER);
            buff.append("AVPs:").append(ENTER);
            Iterator<Avp> it = message.getAvps().iterator();
            while (it.hasNext()) {
                Avp avp = it.next();
                try {
                    buff.append(avpToString(avp, ""));
                } catch (Exception e) {
                    e.printStackTrace();
                    try {
                        buff.append(Hex.encodeHex(avp.getRaw()));
                    } catch (AvpDataException e1) {
                        buff.append("decode failed");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return buff.toString();

    }

    public static String messageToString(IDMessage message) throws AvpDataException {
        StringBuilder buff = new StringBuilder();
        MessageRepresentation mr = DictionaryImpl.INSTANCE.getMessage(message.getCommandCode(), message.getApplicationId(),
                message.isRequest()
        );
        if (mr != null) {
            buff.append("Command(").append(message.getCommandCode()).append(") ").append(mr.getName()).append((message.
                    getFlags() & 0x80) != 0 ? " Request" :
                    " Answer").append(ENTER);
        } else {
            buff.append("Command Code:").append(message.getCommandCode()).append((message.getFlags() & 0x80) != 0 ?
                    " Request" :
                    " Answer").append(ENTER);
        }
        buff.append("Application id:").append(message.getApplicationId()).append(ENTER);
        buff.append("HopByHop id:0x").append(Long.toHexString(message.getHopByHopIdentifier())).append(ENTER);
        buff.append("EndToEnd id:0x").append(Long.toHexString(message.getEndToEndIdentifier())).append(ENTER);
        buff.append("AVPs:").append(ENTER);
        Iterator<Avp> it = message.getAvps().iterator();
        while (it.hasNext()) {
            Avp avp = it.next();
            try {
                buff.append(avpToString(avp, ""));
            } catch (Exception e) {
                e.printStackTrace();
                buff.append(Hex.encodeHex(avp.getRaw()));
            }
        }
        return buff.toString();
    }

    /**
     * @param avp Avp
     * @return String
     * @throws AvpDataException
     */
    public static String avpToString(Avp avp, String ident) throws AvpDataException {
        StringBuilder buff = new StringBuilder();
        int iCode = avp.getCode();
        long lVendorId = avp.getVendorId();
        AvpRepresentation present = DictionaryImpl.INSTANCE.getAvp(iCode, lVendorId);
        if (present == null) {
            buff.append("Unknown AVP").append("(").append(avp.getCode()).append("):");
            byte[] raw = avp.getRaw();
            buff.append("Hex:").append(Hex.encodeHex(raw));
            buff.append("- Char:").append(new String(raw));
            buff.append(ENTER);
        } else if (present.isGrouped()) {
            AvpSet avpSet = avp.getGrouped();
            Iterator<Avp> it = avpSet.iterator();
            buff.append(ident).append("(").append(avp.getCode()).append("):");
            buff.append(ident).append("{");
            buff.append(ENTER);
            while (it.hasNext()) {
                Avp av = it.next();
                buff.append(avpToString(av, ident + IDENT));
            }
            buff.append(ident).append("}").append(ENTER);
        } else {
            int iType = -1;
            String[] strTypes = {"Unsigned32", "Unsigned64", "Integer32",
                    "Integer64", "OctetString", "UTF8String", "DiameterURI",
                    "Enumerated", "Time", "IPAddress"};
            for (int i = 0; i < strTypes.length; i++) {
                if (strTypes[i].equals(present.getType())) {
                    iType = i;
                    break;
                }
            }
            buff.append(ident).append("(").append(avp.getCode()).append("):");
            try {
                if (present.getCode() == Avp.MSISDN) {
                    buff.append(TBCDUtil.toTBCD(avp.getRaw()));
                    if (present.getDescription() != null) {
                        String str = present.getDescription();
                        if (str != null) {
                            buff.append("(").append(str).append(")");
                        }
                    }
                } else {
                    switch (iType) {
                        case 0:
                            buff.append(avp.getUnsigned32());
                            if (present.getDescription() != null) {
                                String str = present.getDescription();
                                if (str != null) {
                                    buff.append("(").append(str).append(")");
                                }
                            }
                            break;
                        case 1:
                            buff.append(avp.getUnsigned64());
                            if (present.getDescription() != null) {
                                String str = present.getDescription();
                                if (str != null) {
                                    buff.append("(").append(str).append(")");
                                }
                            }
                            break;
                        case 2:
                            buff.append(avp.getInteger32());
                            if (present.getDescription() != null) {
                                String str = present.getDescription();
                                if (str != null) {
                                    buff.append("(").append(str).append(")");
                                }
                            }
                            break;
                        case 3:
                            buff.append(avp.getInteger64());
                            if (present.getDescription() != null) {
                                String str = present.getDescription();
                                if (str != null) {
                                    buff.append("(").append(str).append(")");
                                }
                            }
                            break;
                        case 4:
                            buff.append(new String(avp.getOctetString()));
                            break;
                        case 5:
                            buff.append(avp.getUTF8String());
                            break;
                        case 6:
                            buff.append(avp.getDiameterURI());
                            break;
                        case 7:
                            buff.append(avp.getInteger32());
                            if (present.getDescription() != null) {
                                String str = present.getDescription();
                                if (str != null) {
                                    buff.append("(").append(str).append(")");
                                }
                            }
                            break;
                        case 8:
                            buff.append(avp.getTime());
                            break;
                        case 9:
                            buff.append(avp.getAddress());
                            break;
                        default:
                            buff.append("Hex:").append(Hex.encodeHex(avp.getRaw()));
                            buff.append("- Char:").append(new String(avp.getRaw()));
                    }
                }
            } catch (Exception e) {
                buff.append("Hex:").append(Hex.encodeHex(avp.getRaw()));
                buff.append("- Char:").append(new String(avp.getRaw()));
            }
            buff.append(ENTER);
        }
        return buff.toString();
    }

    /**
     * @param avp Avp
     * @return String
     * @throws AvpDataException
     */
    public static String getAvpValue(Avp avp) throws AvpDataException {
        int iCode = avp.getCode();
        long lVendorId = avp.getVendorId();
        AvpRepresentation present = DictionaryImpl.INSTANCE.getAvp(iCode, lVendorId);
        if (present == null) {
            return new String(Hex.encodeHex(avp.getRaw()));
        } else if (present.isGrouped()) {
            return "(grouped)";
        } else {
            int iType = -1;
            String[] strTypes = {"Unsigned32", "Unsigned64", "Integer32",
                    "Integer64", "OctetString", "UTF8String", "DiameterURI",
                    "Enumerated", "Time", "IPAddress"};
            for (int i = 0; i < strTypes.length; i++) {
                if (strTypes[i].equals(present.getType())) {
                    iType = i;
                    break;
                }
            }

            try {
                switch (iType) {
                    case 0:
                        return String.valueOf(avp.getUnsigned32());
                    case 1:
                        return String.valueOf(avp.getUnsigned64());
                    case 2:
                        return String.valueOf(avp.getInteger32());
                    case 3:
                        return String.valueOf(avp.getInteger64());
                    case 4:
                        return String.valueOf(avp.getOctetString());
                    case 5:
                        return String.valueOf(avp.getUTF8String());
                    case 6:
                        return String.valueOf(avp.getDiameterURI());
                    case 7:
                        return String.valueOf(avp.getInteger32());
                    case 8:
                        return String.valueOf(avp.getTime());
                    case 9:
                        return String.valueOf(avp.getAddress());
                    default:
                        return new String(Hex.encodeHex(avp.getRaw()));
                }
            } catch (Exception e) {
                return new String(Hex.encodeHex(avp.getRaw()));
            }
        }
    }
}
