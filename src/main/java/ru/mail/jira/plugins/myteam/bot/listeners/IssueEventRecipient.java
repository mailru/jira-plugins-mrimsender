/* (C)2023 */
package ru.mail.jira.plugins.myteam.bot.listeners;

import com.atlassian.jira.user.ApplicationUser;
import com.google.common.base.Objects;
import lombok.Getter;
import ru.mail.jira.plugins.myteam.component.event.EventRecipient;

@Getter
public class IssueEventRecipient extends EventRecipient {
  private final boolean mentioned;

  public IssueEventRecipient(final ApplicationUser recipient, final boolean mentioned) {
    super(recipient);
    this.mentioned = mentioned;
  }

  public static IssueEventRecipient of(final ApplicationUser recipient, final boolean mentioned) {
    return new IssueEventRecipient(recipient, mentioned);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof IssueEventRecipient)) {
      return false;
    }

    IssueEventRecipient that = (IssueEventRecipient) o;
    return recipient.equals(that.recipient) && mentioned == that.mentioned;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(recipient.hashCode(), mentioned);
  }
}
