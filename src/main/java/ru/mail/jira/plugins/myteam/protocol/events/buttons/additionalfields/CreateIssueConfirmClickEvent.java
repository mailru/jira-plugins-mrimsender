/* (C)2021 */
package ru.mail.jira.plugins.myteam.protocol.events.buttons.additionalfields;

import lombok.Getter;
import ru.mail.jira.plugins.myteam.protocol.IssueCreationDto;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.ButtonClickEvent;

@Getter
public class CreateIssueConfirmClickEvent {
  private final String queryId;
  private final String chatId;
  private final String userId;
  private final IssueCreationDto issueCreationDto;

  public CreateIssueConfirmClickEvent(
      ButtonClickEvent buttonClickEvent, IssueCreationDto issueCreationDto) {
    queryId = buttonClickEvent.getQueryId();
    chatId = buttonClickEvent.getChatId();
    userId = buttonClickEvent.getUserId();
    this.issueCreationDto = issueCreationDto;
  }
}
