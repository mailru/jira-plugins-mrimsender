package ru.mail.jira.plugins.mrimsender.protocol.packages;

class MrimCsHelloAck extends MrimCsHeader {
    private final int timeout;

    public MrimCsHelloAck(byte[] header, byte[] body) {
        super(header);
        this.timeout = convertULToInt(body, 0);
    }

    public int getTimeout() {
        return timeout;
    }
}
