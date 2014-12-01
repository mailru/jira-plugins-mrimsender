package ru.mail.jira.plugins.mrimsender.protocol.packages;

import java.util.Arrays;

class MrimCsOfflineMessageAck extends MrimCsHeader {
    private final byte[] uidl;

    public MrimCsOfflineMessageAck(byte[] header, byte[] body) {
        super(header);
        this.uidl = Arrays.copyOfRange(body, 0, 8);
    }

    public byte[] getUidl() {
        return uidl;
    }
}
