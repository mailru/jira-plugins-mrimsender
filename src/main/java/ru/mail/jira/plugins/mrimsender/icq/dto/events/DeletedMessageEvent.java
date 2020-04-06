package ru.mail.jira.plugins.mrimsender.icq.dto.events;

import org.codehaus.jackson.map.annotate.JsonDeserialize;
import ru.mail.jira.plugins.mrimsender.icq.dto.Chat;

@JsonDeserialize(as = DeletedMessageEvent.class)
public class DeletedMessageEvent extends Event<DeletedMessageEvent.Data>{
    public long getMsgId() {
        return  this.getPayload().msgId;
    }

    public long getTimestamp() {
        return this.getPayload().timestamp;
    }

    public Chat getChat() {
        return this.getPayload().chat;
    }


    static class Data {
        public long msgId;
        public long timestamp;
        public Chat chat;

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
