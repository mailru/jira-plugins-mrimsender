package ru.mail.jira.plugins.mrimsender.protocol;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class ChatState {
    private boolean isWaitingForComment;
    private boolean isWaitingIssueKey;
    private Object stateData;

    public static ChatState buildCommentWaitingState(String commentedIssueKey) {
        ChatState chatState = new ChatState();
        chatState.setWaitingForComment(true);
        chatState.setStateData(commentedIssueKey);
        return chatState;
    }

    public static ChatState buildIssueKeyWaitingState() {
        ChatState chatState = new ChatState();
        chatState.setWaitingIssueKey(true);
        return chatState;
    }
}
