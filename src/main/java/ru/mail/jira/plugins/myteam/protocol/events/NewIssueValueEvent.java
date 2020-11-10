/* (C)2020 */
package ru.mail.jira.plugins.myteam.protocol.events;

import ru.mail.jira.plugins.myteam.protocol.IssueCreationDto;

public interface NewIssueValueEvent {
  String getUserId();

  String getChatId();

  String getFieldValue();

  IssueCreationDto getIssueCreationDto();

  Integer getCurrentFieldNum();
}
