package ru.mail.jira.plugins.mrimsender.icq.dto.events;


import org.codehaus.jackson.map.annotate.JsonDeserialize;
import ru.mail.jira.plugins.mrimsender.icq.dto.Chat;
import ru.mail.jira.plugins.mrimsender.icq.dto.User;
import ru.mail.jira.plugins.mrimsender.icq.dto.parts.Part;

import java.util.List;

@JsonDeserialize(as = NewMessageEvent.class)
public class NewMessageEvent extends Event<NewMessageEvent.Data> {
    public long getMsgId() {
        return  this.getPayload().msgId;
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

    public List<Part<?>> getParts() {
        return this.getPayload().parts;
    }


    static class Data {
        public long msgId;
        public long timestamp;
        public String text;
        public Chat chat;
        public User from;
        public List<Part<?>> parts;

        @Override
        public String toString() {
            return "Data{" +
                    "msgId=" + msgId +
                    ", timestamp=" + timestamp +
                    ", text='" + text + '\'' +
                    ", chat=" + chat +
                    ", from=" + from +
                    ", parts=" + parts +
                    '}';
        }
    }
}
