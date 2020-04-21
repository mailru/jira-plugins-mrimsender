package ru.mail.jira.plugins.mrimsender.protocol.state;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WaitForCommentState implements ChatState {
    private String issueKey;

    public WaitForCommentState(String issueKey) {
        this.issueKey = issueKey;
    }
}
