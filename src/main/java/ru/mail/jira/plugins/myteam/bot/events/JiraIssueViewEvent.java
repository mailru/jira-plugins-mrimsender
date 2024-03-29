/* (C)2021 */
package ru.mail.jira.plugins.myteam.bot.events;

import com.atlassian.jira.user.ApplicationUser;
import lombok.Getter;
import ru.mail.jira.plugins.myteam.myteam.dto.ChatType;

@Getter
public class JiraIssueViewEvent extends MyteamEvent {
  private final String issueKey;
  private final ApplicationUser initiator;
  private final boolean isGroupChat;

  public JiraIssueViewEvent(
      String chatId, String issueKey, ApplicationUser initiator, boolean isGroupChat) {
    super(chatId, initiator.getEmailAddress(), ChatType.GROUP);
    this.chatId = chatId;
    this.issueKey = issueKey;
    this.initiator = initiator;
    this.isGroupChat = isGroupChat;
  }
}
