package ru.mail.jira.plugins.mrimsender.icq.dto.events;

import org.codehaus.jackson.map.annotate.JsonDeserialize;
import ru.mail.jira.plugins.mrimsender.icq.dto.Chat;
import ru.mail.jira.plugins.mrimsender.icq.dto.User;

import java.util.List;

@JsonDeserialize( as = NewChatMembersEvent.class)
public class NewChatMembersEvent extends Event<NewChatMembersEvent.Data> {
    public Chat getChat() {
        return this.getPayload().chat;
    }

    public List<User> getNewMembers() {
        return  this.getPayload().newMembers;
    }

    public User getAddedBy() {
        return this.getPayload().addedBy;
    }

    static class Data {
        private Chat chat;
        private List<User> newMembers;
        private User addedBy;

        @Override
        public String toString() {
            return "Data{" +
                    "chat=" + chat +
                    ", newMembers=" + newMembers +
                    ", addedBy=" + addedBy +
                    '}';
        }
    }
}
