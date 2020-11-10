/* (C)2020 */
package ru.mail.jira.plugins.myteam.protocol.events.buttons;

import lombok.Getter;
import ru.mail.jira.plugins.myteam.protocol.IssueCreationDto;

@Getter
public class NextProjectsPageClickEvent extends NextPageClickEvent {
  private final IssueCreationDto issueCreationDto;

  public NextProjectsPageClickEvent(
      ButtonClickEvent chatButtonClickEvent, int currentPage, IssueCreationDto issueCreationDto) {
    super(chatButtonClickEvent, currentPage);
    this.issueCreationDto = issueCreationDto;
  }
}
