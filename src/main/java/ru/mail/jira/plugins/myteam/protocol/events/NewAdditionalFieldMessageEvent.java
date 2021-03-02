/* (C)2021 */
package ru.mail.jira.plugins.myteam.protocol.events;

import com.atlassian.jira.issue.fields.Field;
import lombok.Getter;
import ru.mail.jira.plugins.myteam.protocol.IssueCreationDto;

@Getter
public class NewAdditionalFieldMessageEvent {
  private final String userId;
  private final String chatId;
  private final String fieldValue;
  private final Field field;
  private final IssueCreationDto issueCreationDto;

  public NewAdditionalFieldMessageEvent(
      ChatMessageEvent chatMessageEvent, IssueCreationDto issueCreationDto, Field field) {
    userId = chatMessageEvent.getUserId();
    chatId = chatMessageEvent.getChatId();
    fieldValue = chatMessageEvent.getMessage().trim();
    this.field = field;
    this.issueCreationDto = issueCreationDto;
  }
}
