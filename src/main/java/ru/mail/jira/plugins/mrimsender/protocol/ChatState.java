package ru.mail.jira.plugins.mrimsender.protocol;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
public class ChatState {
    private boolean isWaitingForComment;
    private boolean isWaitingIssueKey;
    private String issueKey;

    private ChatState(boolean isWaitingIssueKey) {
        this.isWaitingIssueKey = isWaitingIssueKey;
    }

    public static ChatState buildCommentWaitingState(String commentedIssueKey) {
        ChatState chatState = new ChatState();
        chatState.setWaitingForComment(true);
        chatState.setIssueKey(commentedIssueKey);
        return chatState;
    }

    private final static ChatState issueKeyWaitingState = new ChatState(true);

    public static ChatState issueKeyWaitingState() {
        return issueKeyWaitingState;
    }
}
