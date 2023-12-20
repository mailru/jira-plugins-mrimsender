/* (C)2023 */
package ru.mail.jira.plugins.myteam.bot.listeners;

import com.atlassian.jira.user.ApplicationUser;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Getter
@RequiredArgsConstructor
public class MentionedApplicationUser {

  private final ApplicationUser applicationUser;
  private final boolean mentioned;


  public static MentionedApplicationUser mentionedUser(@NotNull final ApplicationUser applicationUser) {
    return new MentionedApplicationUser(applicationUser, true);
  }

  public static MentionedApplicationUser notMentionedUser(@NotNull final ApplicationUser applicationUser) {
    return new MentionedApplicationUser(applicationUser, false);
  }
}
