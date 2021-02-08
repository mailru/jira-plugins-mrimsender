/* (C)2020 */
package ru.mail.jira.plugins.myteam.protocol.events;

import lombok.Getter;
import ru.mail.jira.plugins.myteam.protocol.IssueCreationDto;

@Getter
public class NewIssueFieldValueMessageEvent implements NewIssueValueEvent {
  private final String userId;
  private final String chatId;
  private final String fieldValue;
  private final IssueCreationDto issueCreationDto;
  private final Integer currentFieldNum;

  public NewIssueFieldValueMessageEvent(
      ChatMessageEvent chatMessageEvent,
      IssueCreationDto issueCreationDto,
      Integer currentFieldNum) {
    userId = chatMessageEvent.getUserId();
    chatId = chatMessageEvent.getChatId();
    fieldValue = chatMessageEvent.getMessage().trim();
    this.issueCreationDto = issueCreationDto;
    this.currentFieldNum = currentFieldNum;
  }
}
