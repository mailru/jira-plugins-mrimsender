package ru.mail.jira.plugins.mrimsender.protocol.packages;

class Consts {
    public static final int MRIM_CS_HELLO = 0x1001;
    public static final int MRIM_CS_HELLO_ACK = 0x1002;
    public static final int MRIM_CS_LOGIN_ACK = 0x1004;
    public static final int MRIM_CS_LOGIN_REJ = 0x1005;
    public static final int MRIM_CS_PING = 0x1006;
    public static final int MRIM_CS_MESSAGE = 0x1008;
    public static final int MRIM_CS_MESSAGE_ACK = 0x1009;
    public static final int MRIM_CS_MESSAGE_RECV = 0x1011;
    public static final int MRIM_CS_AUTHORIZE = 0x1020;
    public static final int MRIM_CS_LOGIN2 = 0x1038;
    public static final int MRIM_CS_OFFLINE_MESSAGE_ACK = 0x101D;
    public static final int MRIM_CS_DELETE_OFFLINE_MESSAGE = 0x101E;

    public static final int CS_MAGIC = 0xDEADBEEF;
    public static final int PROTO_VERSION = (1 << 16) | 16;
    public static final int STATUS_ONLINE = 0x00000001;
    public static final String USER_AGENT = "client=\"JiraClient\" version=\"0.01\"";
    public static final String RU_LOCALE = "ru";
    public static final int MESSAGE_FLAG_OFFLINE = 0x00000001;
    public static final int MESSAGE_FLAG_NORECV = 0x00000004;
    public static final int MESSAGE_FLAG_AUTHORIZE = 0x00000008;
    public static final int MESSAGE_FLAG_NOTIFY = 0x00000400;
    public static final int MESSAGE_FLAG_CP1251 = 0x00200000;
    public static final String EMPTY_STRING = " ";
    public static final String CP1251_MRIM_ENCODING = "cp1251";
    public static final String UTF16_MRIM_ENCODING = "UTF-16LE";
}
