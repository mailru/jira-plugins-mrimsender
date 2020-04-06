package ru.mail.jira.plugins.mrimsender.icq.dto.events;

import org.codehaus.jackson.map.annotate.JsonDeserialize;
import ru.mail.jira.plugins.mrimsender.icq.dto.Chat;
import ru.mail.jira.plugins.mrimsender.icq.dto.User;

@JsonDeserialize(as = CallbackQueryEvent.class)
public class CallbackQueryEvent extends Event<CallbackQueryEvent.Data> {
    public long getQueryId() {
        return this.getPayload().queryId;
    }

    public Chat getChat() {
        return this.getPayload().chat;
    }

    public User getFrom() {
        return this.getPayload().from;
    }

    public String getCallbackData() {
        return this.getPayload().callbackData;
    }

    public NewMessageEvent getCallbackMessage() {
        return this.getPayload().message;
    }

    static class Data {
        public long queryId;
        public Chat chat;
        public User from;
        public String callbackData;
        public NewMessageEvent message;

        @Override
        public String toString() {
            return "Data{" +
                    "queryId=" + queryId +
                    ", chat=" + chat +
                    ", from=" + from +
                    ", callbackData='" + callbackData + '\'' +
                    ", message=" + message +
                    '}';
        }
    }
}
