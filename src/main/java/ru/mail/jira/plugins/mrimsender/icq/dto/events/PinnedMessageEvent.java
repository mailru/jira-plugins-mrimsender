package ru.mail.jira.plugins.mrimsender.icq.dto.events;

import org.codehaus.jackson.map.annotate.JsonDeserialize;
import ru.mail.jira.plugins.mrimsender.icq.dto.Chat;
import ru.mail.jira.plugins.mrimsender.icq.dto.User;

@JsonDeserialize(as = PinnedMessageEvent.class)
public class PinnedMessageEvent extends Event<PinnedMessageEvent.Data> {

    public long getMsgId() {
        return this.getPayload().msgId;
    }

    public long getTimestamp() {
        return this.getPayload().timestamp;
    }

    public String getText() {
        return this.getPayload().text;
    }

    public Chat getChat() {
        return this.getPayload().chat;
    }

    public User getFrom() {
        return this.getPayload().from;
    }

    static class Data {
        private long msgId;
        private long timestamp;
        private String text;
        private Chat chat;
        private User from;

        @Override
        public String toString() {
            return "Data{" +
                    "msgId=" + msgId +
                    ", timestamp=" + timestamp +
                    ", text='" + text + '\'' +
                    ", chat=" + chat +
                    ", from=" + from +
                    '}';
        }
    }
}
