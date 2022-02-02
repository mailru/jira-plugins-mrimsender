package ru.mail.jira.plugins.myteam.model;

import net.java.ao.Entity;
import net.java.ao.schema.Table;

@Table("MYTEAM_ISSUE_CREATION_SETTINGS")
public interface IssueCreationSettingsEntity extends Entity {

  String getChatId();

  void setChatId(String chatId);

  boolean isEnabled();

  void setEnabled(boolean enabled);

  void setTag(String tag);

  String getTag();

  String getProjectKey();

  void setProjectKey(String key);

  String getIssueTypeId();

  void setIssueTypeId(String id);

  void setLabels(String labels);

  String getLabels();

}
