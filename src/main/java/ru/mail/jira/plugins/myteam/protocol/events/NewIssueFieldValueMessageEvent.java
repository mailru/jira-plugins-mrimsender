/* (C)2020 */
package ru.mail.jira.plugins.myteam.protocol.events;

import lombok.Getter;
import ru.mail.jira.plugins.myteam.protocol.IssueCreationDto;

@Getter
public class NewIssueFieldValueMessageEvent implements NewIssueValueEvent {
  @Getter(onMethod_ = {@Override})
  private final String userId;

  @Getter(onMethod_ = {@Override})
  private final String chatId;

  @Getter(onMethod_ = {@Override})
  private final String fieldValue;

  @Getter(onMethod_ = {@Override})
  private final IssueCreationDto issueCreationDto;

  @Getter(onMethod_ = {@Override})
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
