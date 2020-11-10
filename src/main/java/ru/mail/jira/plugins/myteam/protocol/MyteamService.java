/* (C)2020 */
package ru.mail.jira.plugins.myteam.protocol;

import com.atlassian.jira.user.ApplicationUser;

public interface MyteamService {
  /**
   * Send message to user in Mail.Ru Agent.
   *
   * @param user recipient of the message
   * @param message sent message
   * @return message sent or not
   */
  boolean sendMessage(ApplicationUser user, String message);
  /**
   * Send message to recipient with id in Mail.Ru Agent.
   *
   * @param chatId recipient of the message
   * @param message sent message
   * @return message sent or not
   */
  void sendMessage(String chatId, String message);
}
