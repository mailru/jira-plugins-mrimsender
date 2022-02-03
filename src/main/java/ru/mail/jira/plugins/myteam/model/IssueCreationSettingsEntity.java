/* (C)2022 */
package ru.mail.jira.plugins.myteam.model;

import net.java.ao.Entity;
import net.java.ao.schema.Table;
import org.jetbrains.annotations.Nullable;

@Table("MYTEAM_ISSUE_CFG")
public interface IssueCreationSettingsEntity extends Entity {

  String getChatId();

  void setChatId(String chatId);

  boolean isEnabled();

  void setEnabled(boolean enabled);

  void setTag(String tag);

  String getTag();

  @Nullable
  String getProjectKey();

  void setProjectKey(String key);

  @Nullable
  String getIssueTypeId();

  void setIssueTypeId(String id);

  @Nullable
  String getLabels();

  void setLabels(String labels);
}
