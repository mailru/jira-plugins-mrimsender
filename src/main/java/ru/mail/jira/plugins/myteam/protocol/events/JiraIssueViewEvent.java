/* (C)2021 */
package ru.mail.jira.plugins.myteam.protocol.events;

import com.atlassian.jira.user.ApplicationUser;
import lombok.Getter;

@Getter
public class JiraIssueViewEvent extends MyteamEvent {
  private final String issueKey;
  private final ApplicationUser initiator;
  private final boolean isGroupChat;

  public JiraIssueViewEvent(
      String chatId, String issueKey, ApplicationUser initiator, boolean isGroupChat) {
    this.chatId = chatId;
    this.issueKey = issueKey;
    this.initiator = initiator;
    this.isGroupChat = isGroupChat;
  }
}
