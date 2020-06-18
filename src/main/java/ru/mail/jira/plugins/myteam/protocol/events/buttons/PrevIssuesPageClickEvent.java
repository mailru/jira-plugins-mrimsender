package ru.mail.jira.plugins.myteam.protocol.events.buttons;

import com.atlassian.query.Query;
import lombok.Getter;

@Getter
public class PrevIssuesPageClickEvent extends PrevPageClickEvent {
    private final Query currentJqlQueryClause;

    public PrevIssuesPageClickEvent(ButtonClickEvent chatButtonClickEvent, int currentPage, Query currentJqlQueryClause) {
        super(chatButtonClickEvent, currentPage);
        this.currentJqlQueryClause = currentJqlQueryClause;
    }
}
