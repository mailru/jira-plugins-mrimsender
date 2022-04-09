/* (C)2022 */
package ru.mail.jira.plugins.myteam.model;

import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;
import org.jetbrains.annotations.Nullable;

@Table("MYTEAM_ISSUE_DEFAULT_CFG")
public interface IssueCreationSettingsDefault {
  @Nullable
  @StringLength(StringLength.UNLIMITED)
  String getCreationSuccessTemplate();

  void setCreationSuccessTemplate(String template);

  @Nullable
  @StringLength(StringLength.UNLIMITED)
  String getIssueSummaryTemplate();

  void setIssueSummaryTemplate(String template);
}
