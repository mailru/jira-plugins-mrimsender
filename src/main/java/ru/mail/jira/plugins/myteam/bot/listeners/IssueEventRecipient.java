/* (C)2023 */
package ru.mail.jira.plugins.myteam.bot.listeners;

import com.atlassian.jira.user.ApplicationUser;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class IssueEventRecipient {

  private final ApplicationUser recipient;
  private final boolean mentioned;

  public static IssueEventRecipient of(final ApplicationUser recipient, final boolean mentioned) {
    return new IssueEventRecipient(recipient, mentioned);
  }
}
