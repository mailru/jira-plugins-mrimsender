package ru.mail.jira.plugins.mrimsender.icq.dto.events;


import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import ru.mail.jira.plugins.mrimsender.icq.dto.Message;
import ru.mail.jira.plugins.mrimsender.icq.dto.User;

public class CallbackQueryEvent extends Event<CallbackQueryEvent.Data> {
    public String getQueryId() {
        return this.getPayload().queryId;
    }

    public User getFrom() {
        return this.getPayload().from;
    }

    public String getCallbackData() {
        return this.getPayload().callbackData;
    }

    public Message getMessage() {
        return this.getPayload().message;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Data {
        public String queryId;
        public User from;
        public String callbackData;
        public Message message;

        @Override
        public String toString() {
            return "Data{" +
                    "queryId=" + queryId +
                    ", from=" + from +
                    ", callbackData='" + callbackData + '\'' +
                    ", message=" + message +
                    '}';
        }
    }
}
