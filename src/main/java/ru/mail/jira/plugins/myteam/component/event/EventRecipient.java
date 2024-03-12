/* (C)2024 */
package ru.mail.jira.plugins.myteam.component.event;

import com.atlassian.jira.user.ApplicationUser;
import java.util.Objects;
import lombok.Getter;

@Getter
public class EventRecipient {
  protected final ApplicationUser recipient;

  protected EventRecipient(final ApplicationUser recipient) {
    this.recipient = recipient;
  }

  public static EventRecipient of(final ApplicationUser recipient) {
    return new EventRecipient(recipient);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof EventRecipient)) {
      return false;
    }

    EventRecipient that = (EventRecipient) o;
    return Objects.equals(recipient, that.recipient);
  }

  @Override
  public int hashCode() {
    return Objects.hash(recipient);
  }
}
