package ru.mail.jira.plugins.mrimsender.protocol;

import com.atlassian.query.Query;
import lombok.Getter;

@Getter
public class ChatState {
    private final boolean isWaitingForComment;
    private final boolean isWaitingForIssueKey;
    private final boolean isSearchResultsShowing;
    private final boolean isWaitingForJqlClause;
    private final String issueKey;
    private final Query currentSearchJqlClause;
    private final Integer currentSearchResultsPage;

    private ChatState(boolean isWaitingForComment,
                      boolean isWaitingForIssueKey,
                      boolean isSearchResultsShowing,
                      boolean isWaitingForJqlClause,
                      String issueKey,
                      Query currentSearchJqlClause,
                      Integer currentSearchResultsPage) {
        this.isWaitingForComment = isWaitingForComment;
        this.isWaitingForIssueKey = isWaitingForIssueKey;
        this.isSearchResultsShowing = isSearchResultsShowing;
        this.isWaitingForJqlClause = isWaitingForJqlClause;
        this.issueKey = issueKey;
        this.currentSearchJqlClause = currentSearchJqlClause;
        this.currentSearchResultsPage = currentSearchResultsPage;
    }

    private final static ChatState issueKeyWaitingState = new ChatState(false,
                                                                        true,
                                                                        false,
                                                                        false,
                                                                        null,
                                                                        null,
                                                                        null);
    private final static ChatState jqlClauseWaitingState = new ChatState(false,
                                                                         false,
                                                                         false,
                                                                         true,
                                                                         null,
                                                                         null,
                                                                         null);

    public static ChatState buildCommentWaitingState(String commentedIssueKey) {
        return new ChatState(true,
                             false,
                             false,
                             false,
                             commentedIssueKey,
                             null,
                             null);
    }

    public static ChatState jqlClauseWaitingState() { return jqlClauseWaitingState; }

    public static ChatState issueKeyWaitingState() {
        return issueKeyWaitingState;
    }

    public static ChatState buildSearchResultsWatchingState(Query currentSearchClause, Integer currentSearchResultsPage) {
        return new ChatState(false,
                             false,
                             true,
                             false,
                             null,
                             currentSearchClause,
                             currentSearchResultsPage);
    }

}
