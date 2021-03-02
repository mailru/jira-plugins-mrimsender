/* (C)2021 */
package ru.mail.jira.plugins.myteam.protocol.events.buttons.additionalfields;

import lombok.Getter;
import ru.mail.jira.plugins.myteam.protocol.IssueCreationDto;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.PrevPageClickEvent;

@Getter
public class NextAdditionalFieldPageClickEvent extends PrevPageClickEvent {
  private final IssueCreationDto issueCreationDto;

  public NextAdditionalFieldPageClickEvent(
      ButtonClickEvent chatButtonClickEvent, int currentPage, IssueCreationDto issueCreationDto) {
    super(chatButtonClickEvent, currentPage);
    this.issueCreationDto = issueCreationDto;
  }
}
