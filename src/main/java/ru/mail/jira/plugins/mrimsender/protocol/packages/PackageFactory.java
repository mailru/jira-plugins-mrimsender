package ru.mail.jira.plugins.mrimsender.protocol.packages;

import org.apache.commons.lang.StringUtils;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

class PackageFactory {
    private static byte[] convertIntToUL(int i) {
        ByteBuffer bb = ByteBuffer.allocate(Integer.SIZE / Byte.SIZE);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(i);
        return bb.array();
    }

    private static byte[] convertStringToLPS(String s, String encoding) {
        try {
            if (StringUtils.isEmpty(s))
                s = Consts.EMPTY_STRING;

            byte[] sBuff = s.getBytes(encoding);
            byte[] sLenBuff = convertIntToUL(sBuff.length);

            ByteBuffer bb = ByteBuffer.allocate(sLenBuff.length + sBuff.length);
            bb.put(sLenBuff);
            bb.put(sBuff);
            return bb.array();
        } catch (UnsupportedEncodingException ignored) {
            return null;
        }
    }

    private static byte[] getMrimCsHeader(int seq, int type, int size) {
        ByteBuffer bb = ByteBuffer.allocate(MrimCsHeader.SIZE);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(Consts.CS_MAGIC);
        bb.putInt(Consts.PROTO_VERSION);
        bb.putInt(seq);
        bb.putInt(type);
        bb.putInt(size);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        return bb.array();
    }

    public static byte[] getMrimCsHello() {
        return getMrimCsHeader(0, Consts.MRIM_CS_HELLO, 0);
    }

    public static byte[] getMrimCsPing(int seq) {
        return getMrimCsHeader(seq, Consts.MRIM_CS_PING, 0);
    }

    public static byte[] getMrimCsMessage(int seq, int flag, String to, String message) {
        byte[] flagBytes = convertIntToUL(Consts.MESSAGE_FLAG_CP1251 | flag);
        byte[] toBytes = convertStringToLPS(to, Consts.CP1251_MRIM_ENCODING);
        byte[] messageBytes = convertStringToLPS(message, Consts.CP1251_MRIM_ENCODING);
        byte[] rtf_message = convertIntToUL(0);

        int dlen = flagBytes.length + toBytes.length + messageBytes.length + rtf_message.length;

        ByteBuffer bb = ByteBuffer.allocate(MrimCsHeader.SIZE + dlen);
        bb.put(getMrimCsHeader(seq, Consts.MRIM_CS_MESSAGE, dlen));
        bb.put(flagBytes);
        bb.put(toBytes);
        bb.put(messageBytes);
        bb.put(rtf_message);
        return bb.array();
    }

    public static byte[] getMrimCsMessageRecv(int seq, String to, int msgId) {
        byte[] toBytes = convertStringToLPS(to, Consts.CP1251_MRIM_ENCODING);
        byte[] msgIdBytes = convertIntToUL(msgId);

        int dlen = toBytes.length + msgIdBytes.length;

        ByteBuffer bb = ByteBuffer.allocate(MrimCsHeader.SIZE + dlen);
        bb.put(getMrimCsHeader(seq, Consts.MRIM_CS_MESSAGE_RECV, dlen));
        bb.put(toBytes);
        bb.put(msgIdBytes);
        return bb.array();
    }

    public static byte[] getMrimCsAuthorize(int seq, String email) {
        byte[] user = convertStringToLPS(email, Consts.CP1251_MRIM_ENCODING);

        int dlen = user.length;

        ByteBuffer bb = ByteBuffer.allocate(MrimCsHeader.SIZE + dlen);
        bb.put(getMrimCsHeader(seq, Consts.MRIM_CS_AUTHORIZE, dlen));
        bb.put(user);
        return bb.array();
    }

    public static byte[] getMrimCsLogin2(int seq, String login, String password) {
        byte[] loginBytes = convertStringToLPS(login, Consts.CP1251_MRIM_ENCODING);
        byte[] passwordBytes = convertStringToLPS(password, Consts.CP1251_MRIM_ENCODING);
        byte[] status = convertIntToUL(Consts.STATUS_ONLINE);
        byte[] spec_status_uri = convertIntToUL(0);
        byte[] status_title = convertIntToUL(0);
        byte[] status_desc = convertIntToUL(0);
        byte[] features = convertIntToUL(0);
        byte[] status_user_agent = convertStringToLPS(Consts.USER_AGENT, Consts.CP1251_MRIM_ENCODING);
        byte[] lang = convertStringToLPS(Consts.RU_LOCALE, Consts.CP1251_MRIM_ENCODING);
        byte[] client_description = convertStringToLPS("", Consts.CP1251_MRIM_ENCODING);

        int dlen = loginBytes.length + passwordBytes.length + status.length + spec_status_uri.length + status_title.length +
                status_desc.length + features.length + status_user_agent.length + lang.length + client_description.length;

        ByteBuffer bb = ByteBuffer.allocate(MrimCsHeader.SIZE + dlen);
        bb.put(getMrimCsHeader(seq, Consts.MRIM_CS_LOGIN2, dlen));
        bb.put(loginBytes);
        bb.put(passwordBytes);
        bb.put(status);
        bb.put(spec_status_uri);
        bb.put(status_title);
        bb.put(status_desc);
        bb.put(features);
        bb.put(status_user_agent);
        bb.put(lang);
        bb.put(client_description);
        return bb.array();
    }

    public static byte[] getMrimCsDeleteOfflineMessage(int seq, byte[] uidl) {
        int dlen = uidl.length;

        ByteBuffer bb = ByteBuffer.allocate(MrimCsHeader.SIZE + dlen);
        bb.put(getMrimCsHeader(seq, Consts.MRIM_CS_DELETE_OFFLINE_MESSAGE, dlen));
        bb.put(uidl);
        return bb.array();
    }
}
