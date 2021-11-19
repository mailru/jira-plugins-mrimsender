/* (C)2020 */
package ru.mail.jira.plugins.myteam.protocol.events.buttons;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import ru.mail.jira.plugins.myteam.protocol.IssueCreationDto;
import ru.mail.jira.plugins.myteam.protocol.events.NewIssueValueEvent;

@Getter
@AllArgsConstructor
public class NewIssueFieldValueButtonClickEvent implements NewIssueValueEvent {
  private final String queryId;

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

  public NewIssueFieldValueButtonClickEvent(
      ButtonClickEvent buttonClickEvent,
      IssueCreationDto issueCreationDto,
      Integer currentFieldNum) {
    this.queryId = buttonClickEvent.getQueryId();
    this.chatId = buttonClickEvent.getChatId();
    this.userId = buttonClickEvent.getUserId();
    this.fieldValue = StringUtils.substringAfter(buttonClickEvent.getCallbackData(), "-");
    this.issueCreationDto = issueCreationDto;
    this.currentFieldNum = currentFieldNum;
  }
}
