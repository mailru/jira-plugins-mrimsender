package ru.mail.jira.plugins.mrimsender.protocol.events;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.user.ApplicationUser;
import com.google.common.eventbus.Subscribe;
import com.mashape.unirest.http.exceptions.UnirestException;
import lombok.Getter;
import lombok.Setter;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.NewMessageEvent;

import java.io.IOException;

@Setter
@Getter
public class IssueKeyMessageEvent {
    private final String chatId;
    private final String message;
    private final String userId;

    public IssueKeyMessageEvent(NewMessageEvent newMessageEvent) {
        this.chatId = newMessageEvent.getChat().getChatId();
        this.message = newMessageEvent.getText().trim();
        this.userId = newMessageEvent.getFrom().getUserId();
    }
}
