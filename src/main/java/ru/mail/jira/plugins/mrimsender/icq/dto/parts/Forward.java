package ru.mail.jira.plugins.mrimsender.icq.dto.parts;

import org.codehaus.jackson.map.annotate.JsonDeserialize;
import ru.mail.jira.plugins.mrimsender.icq.dto.Message;

@JsonDeserialize(as = Forward.class)
public class Forward extends Part<Forward.Data> {
    public static class Data {
        public Message message;

        @Override
        public String toString() {
            return "Data{" +
                    "message=" + message +
                    '}';
        }
    }

    public Message getMessage() {
        return this.getPayload().message;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
