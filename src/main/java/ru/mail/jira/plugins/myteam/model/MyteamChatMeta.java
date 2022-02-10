/* (C)2020 */
package ru.mail.jira.plugins.myteam.model;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.Table;

@Table("MYTEAM_CHAT_META")
public interface MyteamChatMeta extends Entity {
  // Chat id of myteam chat
  String getChatId();

  void setChatId(String chatId);

  // Issue key (upper-case) of an issue where myteam chat was created
  @Indexed
  String getIssueKey();

  void setIssueKey(String issueKey);
}
