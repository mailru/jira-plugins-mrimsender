package ru.mail.jira.plugins.mrimsender.icq.dto;


public class MessageResponse {
    private long msgId;

    private boolean ok;


    public long getMsgId() {
        return msgId;
    }

    public void setMsgId(long msgId) {
        this.msgId = msgId;
    }

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    @Override
    public String toString() {
        return "MessageResponse{" +
                "msgId=" + msgId +
                ", ok=" + ok +
                '}';
    }
}
