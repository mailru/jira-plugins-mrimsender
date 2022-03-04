/* (C)2022 */
package ru.mail.jira.plugins.myteam.model;

import net.java.ao.Entity;
import net.java.ao.OneToMany;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;
import org.jetbrains.annotations.Nullable;
import ru.mail.jira.plugins.myteam.commons.IssueReporter;

@Table("MYTEAM_ISSUE_CFG")
public interface IssueCreationSettings extends Entity {

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

  @Nullable
  IssueReporter getReporter();

  void setReporter(IssueReporter issueReporter);

  @Nullable
  @StringLength(value = StringLength.UNLIMITED)
  String getCreationSuccessTemplate();

  @StringLength(value = StringLength.UNLIMITED)
  void setCreationSuccessTemplate(String template);

  @Nullable
  String getIssueSummaryTemplate();

  void setIssueSummaryTemplate(String template);

  @OneToMany
  AdditionalIssueField[] getAdditionalFields();
}
