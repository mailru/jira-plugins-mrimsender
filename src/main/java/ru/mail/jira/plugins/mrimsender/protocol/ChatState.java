package ru.mail.jira.plugins.mrimsender.protocol;

import com.atlassian.query.Query;
import lombok.Getter;


@Getter
public class ChatState {
    private final boolean isWaitingForComment;
    private final boolean isWaitingForIssueKey;
    private final boolean isIssueSearchResultsShowing;
    private final boolean isWaitingForJqlClause;
    private final boolean isWaitingForProjectSelect;
    private final boolean isWaitingForIssueTypeSelect;
    private final boolean isNewIssueFieldsFillingState;
    private final String issueKey;
    private final Query currentSearchJqlClause;
    private final Integer currentSelectListPage;
    private final Integer currentFillingFieldNum;
    private final IssueCreationDto issueCreationDto;

    private ChatState(boolean isWaitingForComment,
                      boolean isWaitingForIssueKey,
                      boolean isIssueSearchResultsShowing,
                      boolean isWaitingForJqlClause,
                      boolean isWaitingForProjectSelect,
                      boolean isWaitingForIssueTypeSelect,
                      boolean isNewIssueFieldsFillingState,
                      String issueKey,
                      Query currentSearchJqlClause,
                      Integer currentSelectListPage,
                      Integer currentFillingFieldNum,
                      IssueCreationDto issueCreationDto) {
        this.isWaitingForComment = isWaitingForComment;
        this.isWaitingForIssueKey = isWaitingForIssueKey;
        this.isIssueSearchResultsShowing = isIssueSearchResultsShowing;
        this.isWaitingForJqlClause = isWaitingForJqlClause;
        this.isWaitingForProjectSelect = isWaitingForProjectSelect;
        this.isWaitingForIssueTypeSelect = isWaitingForIssueTypeSelect;
        this.isNewIssueFieldsFillingState = isNewIssueFieldsFillingState;
        this.issueKey = issueKey;
        this.currentSearchJqlClause = currentSearchJqlClause;
        this.currentSelectListPage = currentSelectListPage;
        this.currentFillingFieldNum = currentFillingFieldNum;
        this.issueCreationDto = issueCreationDto;
    }

    private final static ChatState issueKeyWaitingState = new ChatState(false,
                                                                        true,
                                                                        false,
                                                                        false,
                                                                        false,
                                                                        false,
                                                                        false,
                                                                        null,
                                                                        null,
                                                                        null,
                                                                        null,
                                                                        null);
    private final static ChatState jqlClauseWaitingState = new ChatState(false,
                                                                         false,
                                                                         false,
                                                                         true,
                                                                         false,
                                                                         false,
                                                                         false,
                                                                         null,
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
                             false,
                             false,
                             commentedIssueKey,
                             null,
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
                             false,
                             false,
                             null,
                             currentSearchClause,
                             currentIssuesListPage,
                             null,
                             null);
    }

    public static ChatState buildProjectSelectWaitingState(Integer currentProjectsListPage, IssueCreationDto issueCreationDto) {
        return new ChatState(false,
                             false,
                             false,
                             false,
                             true,
                             false,
                             false,
                             null,
                             null,
                             currentProjectsListPage,
                             null,
                             issueCreationDto);
    }

    public static ChatState buildIssueTypeSelectWaitingState(Integer currentIssueTypesListPage, IssueCreationDto issueCreationDto) {
        return new ChatState(false,
                             false,
                             false,
                             false,
                             false,
                             true,
                             false,
                             null,
                             null,
                             currentIssueTypesListPage,
                             null,
                             issueCreationDto);
    }

    public static ChatState buildNewIssueFieldsFillingState(Integer currentFillingFieldNum, IssueCreationDto issueCreationDto) {
        return new ChatState(false,
                             false,
                             false,
                             false,
                             false,
                             false,
                             true,
                             null,
                             null,
                             null,
                             currentFillingFieldNum,
                             issueCreationDto);
    }

}