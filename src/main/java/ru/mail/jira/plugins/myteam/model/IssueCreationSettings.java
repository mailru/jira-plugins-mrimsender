/* (C)2022 */
package ru.mail.jira.plugins.myteam.model;

import net.java.ao.Entity;
import net.java.ao.OneToMany;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;
import org.jetbrains.annotations.Nullable;
import ru.mail.jira.plugins.myteam.commons.IssueReporter;

@Table("MYTEAM_ISSUE_CFG")
public interface IssueCreationSettings extends Entity {
  @Indexed
  String getChatId();

  void setChatId(String chatId);

  boolean isEnabled();

  void setEnabled(boolean enabled);

  @Indexed
  String getTag();

  void setTag(String tag);

  @Nullable
  String getProjectKey();

  void setProjectKey(String key);

  @Nullable
  String getIssueTypeId();

  void setIssueTypeId(String id);

  @Nullable
  @StringLength(StringLength.UNLIMITED)
  String getLabels();

  void setLabels(String labels);

  @Nullable
  IssueReporter getReporter();

  void setReporter(IssueReporter issueReporter);

  @Nullable
  @StringLength(StringLength.UNLIMITED)
  String getCreationSuccessTemplate();

  void setCreationSuccessTemplate(String template);

  @Nullable
  @StringLength(StringLength.UNLIMITED)
  String getIssueSummaryTemplate();

  void setIssueSummaryTemplate(String template);

  @OneToMany
  AdditionalIssueField[] getAdditionalFields();
}
