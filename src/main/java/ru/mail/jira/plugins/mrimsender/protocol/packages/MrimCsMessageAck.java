package ru.mail.jira.plugins.mrimsender.protocol.packages;

class MrimCsMessageAck extends MrimCsHeader {
    private final int msgId;
    private final int flags;
    private final String fromEmail;
    private final String message;

    public MrimCsMessageAck(byte[] header, byte[] body) {
        super(header);
        this.msgId = convertULToInt(body, 0);
        this.flags = convertULToInt(body, 4);
        int fromEmailLength = convertULToInt(body, 8);
        fromEmail = convertBytesToString(body, 12, fromEmailLength, Consts.CP1251_MRIM_ENCODING);
        int messageLength = convertULToInt(body, 12 + fromEmailLength);
        message = convertBytesToString(body, 16 + fromEmailLength, messageLength, checkFlag(Consts.MESSAGE_FLAG_CP1251) ? Consts.CP1251_MRIM_ENCODING : Consts.UTF16_MRIM_ENCODING);
    }

    public int getMsgId() {
        return msgId;
    }

    public boolean checkFlag(int flag) {
        return (flags & flag) != 0;
    }

    public String getFromEmail() {
        return fromEmail;
    }

    public String getMessage() {
        return message;
    }
}
