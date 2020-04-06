package ru.mail.jira.plugins.mrimsender.icq.dto.events;

import org.codehaus.jackson.map.annotate.JsonDeserialize;
import ru.mail.jira.plugins.mrimsender.icq.dto.Chat;

@JsonDeserialize(as = UnpinnedMessageEvent.class)
public class UnpinnedMessageEvent extends Event<UnpinnedMessageEvent.Data> {

    public long getMsgId() {
        return this.getPayload().msgId;
    }

    public long getTimestamp() {
        return this.getPayload().timestamp;
    }

    public Chat getChat() {
        return this.getPayload().chat;
    }

    static class Data {
        private long msgId;
        private long timestamp;
        private Chat chat;

        @Override
        public String toString() {
            return "Data{" +
                    "msgId=" + msgId +
                    ", timestamp=" + timestamp +
                    ", chat=" + chat +
                    '}';
        }
    }
}