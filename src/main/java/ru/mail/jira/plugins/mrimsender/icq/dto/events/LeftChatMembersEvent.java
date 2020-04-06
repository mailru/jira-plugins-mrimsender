package ru.mail.jira.plugins.mrimsender.icq.dto.events;

import org.codehaus.jackson.map.annotate.JsonDeserialize;
import ru.mail.jira.plugins.mrimsender.icq.dto.Chat;
import ru.mail.jira.plugins.mrimsender.icq.dto.User;

import java.util.List;

@JsonDeserialize(as = LeftChatMembersEvent.class)
public class LeftChatMembersEvent extends Event<LeftChatMembersEvent.Data> {
    public Chat getChat() {
        return this.getPayload().chat;
    }

    public List<User> getLeftMembers() {
        return this.getPayload().leftMembers;
    }

    public User getRemovedBy() {
        return this.getPayload().removedBy;
    }

    static class Data {
        private Chat chat;
        private List<User> leftMembers;
        private User removedBy;

        @Override
        public String toString() {
            return "Data{" +
                    "chat=" + chat +
                    ", leftMembers=" + leftMembers +
                    ", removedBy=" + removedBy +
                    '}';
        }
    }
}
