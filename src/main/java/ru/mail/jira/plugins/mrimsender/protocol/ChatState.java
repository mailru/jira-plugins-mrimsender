package ru.mail.jira.plugins.mrimsender.protocol;

import com.atlassian.query.Query;
import lombok.Getter;


@Getter
public class ChatState {
    private final boolean isWaitingForComment;
    private final boolean isWaitingForIssueKey;
    private final boolean isIssueSearchResultsShowing;
    private final boolean isWaitingForJqlClause;
    private final boolean isIssueCreationState;
    private final String issueKey;
    private final Query currentSearchJqlClause;
    private final Integer currentSelectListPage;
    private final IssueCreationState issueCreationState;

    private ChatState(boolean isWaitingForComment,
                      boolean isWaitingForIssueKey,
                      boolean isIssueSearchResultsShowing,
                      boolean isWaitingForJqlClause,
                      boolean isIssueCreationState,
                      String issueKey,
                      Query currentSearchJqlClause,
                      Integer currentSelectListPage,
                      IssueCreationState issueCreationState) {
        this.isWaitingForComment = isWaitingForComment;
        this.isWaitingForIssueKey = isWaitingForIssueKey;
        this.isIssueSearchResultsShowing = isIssueSearchResultsShowing;
        this.isWaitingForJqlClause = isWaitingForJqlClause;
        this.isIssueCreationState = isIssueCreationState;
        this.issueKey = issueKey;
        this.currentSearchJqlClause = currentSearchJqlClause;
        this.currentSelectListPage = currentSelectListPage;
        this.issueCreationState = issueCreationState;
    }

    private final static ChatState issueKeyWaitingState = new ChatState(false,
                                                                        true,
                                                                        false,
                                                                        false,
                                                                        false,
                                                                        null,
                                                                        null,
                                                                        null,
                                                                        null);
    private final static ChatState jqlClauseWaitingState = new ChatState(false,
                                                                         false,
                                                                         false,
                                                                         true,
                                                                         false,
                                                                         null,
                                                                         null,
                                                                         null,
                                                                         null);

    public static ChatState jqlClauseWaitingState() {
        return jqlClauseWaitingState;
    }

    public static ChatState issueKeyWaitingState() {
        return issueKeyWaitingState;
    }

    public static ChatState buildCommentWaitingState(String commentedIssueKey) {
        return new ChatState(true,
                             false,
                             false,
                             false,
                             false,
                             commentedIssueKey,
                             null,
                             null,
                             null);
    }

    public static ChatState buildIssueSearchResultsWatchingState(Query currentSearchClause, Integer currentIssuesListPage) {
        return new ChatState(false,
                             false,
                             true,
                             false,
                             false,
                             null,
                             currentSearchClause,
                             currentIssuesListPage,
                             null);
    }

    public static ChatState buildProjectSelectWaitingState(Integer currentProjectsListPage) {
        return new ChatState(false,
                             false,
                             false,
                             false,
                             true,
                             null,
                             null,
                             currentProjectsListPage,
                             IssueCreationState.projectSelectWaitingState);
    }

    public static ChatState buildIssueTypeSelectWaitingState(String selectedProjectKey, Integer currentIssueTypesListPage) {
        return new ChatState(false,
                             false,
                             false,
                             false,
                             true,
                             null,
                             null,
                             currentIssueTypesListPage,
                             IssueCreationState.buildIssueTypeSelectWaitingState(selectedProjectKey));
    }

    public boolean isWaitingForProjectSelect() {
        return (issueCreationState != null) ? issueCreationState.isWaitingForProjectSelect : false;
    }

    public boolean isWaitingForIssueTypeSelect() {
        return (issueCreationState != null) ? issueCreationState.isWaitingForIssueTypeSelect : false;
    }

    public String getSelectedProjectKey() {
        return issueCreationState.getIssueCreationDto().getProjectKey();
    }

    public IssueCreationDto getIssueCreationDto() {
        return issueCreationState.getIssueCreationDto();
    }

    @Getter
    private static class IssueCreationState {
        private final boolean isWaitingForProjectSelect;
        private final boolean isWaitingForIssueTypeSelect;
        private final IssueCreationDto issueCreationDto;

        public static IssueCreationState projectSelectWaitingState = new IssueCreationState(true, false);

        public static IssueCreationState buildIssueTypeSelectWaitingState(String selectedProjectKey) {
            return new IssueCreationState(false, true, selectedProjectKey);
        }

        private IssueCreationState(boolean isWaitingForProjectSelect, boolean isWaitingForIssueTypeSelect) {
            this.isWaitingForProjectSelect = isWaitingForProjectSelect;
            this.isWaitingForIssueTypeSelect = isWaitingForIssueTypeSelect;
            this.issueCreationDto = null;
        }

        private IssueCreationState(boolean isWaitingForProjectSelect, boolean isWaitingForIssueTypeSelect, String selectedProjectKey) {
            this.isWaitingForProjectSelect = isWaitingForProjectSelect;
            this.isWaitingForIssueTypeSelect = isWaitingForIssueTypeSelect;
            issueCreationDto = new IssueCreationDto(selectedProjectKey);
        }
    }
}