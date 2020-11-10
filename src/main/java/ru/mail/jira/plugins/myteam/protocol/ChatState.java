/* (C)2020 */
package ru.mail.jira.plugins.myteam.protocol;

import com.atlassian.query.Query;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatState {
  private final boolean isWaitingForComment;
  private final boolean isWaitingForIssueKey;
  private final boolean isIssueSearchResultsShowing;
  private final boolean isIssueCommentsShowing;
  private final boolean isWaitingForJqlClause;
  private final boolean isWaitingForProjectSelect;
  private final boolean isWaitingForIssueTypeSelect;
  private final boolean isNewIssueFieldsFillingState;
  private final boolean isWaitingForNewIssueButtonFillingState;
  private final String issueKey;
  private final Query currentSearchJqlClause;
  private final Integer currentSelectListPage;
  private final Integer currentFillingFieldNum;
  private final IssueCreationDto issueCreationDto;

  public boolean isWaiting() { // if bot is waiting for user action "Cancel" button works
    return isWaitingForComment
        || isWaitingForIssueKey
        || isWaitingForIssueTypeSelect
        || isWaitingForJqlClause
        || isWaitingForProjectSelect
        || isWaitingForNewIssueButtonFillingState
        || isNewIssueFieldsFillingState;
  }

  public static final ChatState issueKeyWaitingState = builder().isWaitingForIssueKey(true).build();
  public static final ChatState jqlClauseWaitingState =
      builder().isWaitingForJqlClause(true).build();

  public static ChatState buildCommentWaitingState(String commentedIssueKey) {
    return builder().isWaitingForComment(true).issueKey(commentedIssueKey).build();
  }

  public static ChatState buildIssueSearchResultsWatchingState(
      Query currentSearchClause, Integer currentIssuesListPage) {
    return builder()
        .isIssueSearchResultsShowing(true)
        .currentSearchJqlClause(currentSearchClause)
        .currentSelectListPage(currentIssuesListPage)
        .build();
  }

  public static ChatState buildIssueCommentsWatchingState(
      String issueKey, Integer currentCommentsListPage) {
    return builder()
        .isIssueCommentsShowing(true)
        .issueKey(issueKey)
        .currentSelectListPage(currentCommentsListPage)
        .build();
  }

  public static ChatState buildProjectSelectWaitingState(
      Integer currentProjectsListPage, IssueCreationDto issueCreationDto) {
    return builder()
        .isWaitingForProjectSelect(true)
        .currentSelectListPage(currentProjectsListPage)
        .issueCreationDto(issueCreationDto)
        .build();
  }

  public static ChatState buildIssueTypeSelectWaitingState(IssueCreationDto issueCreationDto) {
    return builder().isWaitingForIssueTypeSelect(true).issueCreationDto(issueCreationDto).build();
  }

  public static ChatState buildNewIssueButtonFieldsWaitingState(
      Integer currentFillingFieldNum, IssueCreationDto issueCreationDto) {
    return builder()
        .isWaitingForNewIssueButtonFillingState(true)
        .currentFillingFieldNum(currentFillingFieldNum)
        .issueCreationDto(issueCreationDto)
        .build();
  }

  public static ChatState buildNewIssueFieldsFillingState(
      Integer currentFillingFieldNum, IssueCreationDto issueCreationDto) {
    return builder()
        .isNewIssueFieldsFillingState(true)
        .currentFillingFieldNum(currentFillingFieldNum)
        .issueCreationDto(issueCreationDto)
        .build();
  }
}
