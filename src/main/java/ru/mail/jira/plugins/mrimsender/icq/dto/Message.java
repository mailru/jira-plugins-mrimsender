package ru.mail.jira.plugins.mrimsender.icq.dto;

public class Message {
    private User from;
    private String msgId;
    private String text;
    private long timestamp;

    public User getFrom() {
        return from;
    }

    public String getMsgId() {
        return msgId;
    }

    public String getText() {
        return text;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "Message{" +
                "from=" + from +
                ", msgId='" + msgId + '\'' +
                ", text='" + text + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
