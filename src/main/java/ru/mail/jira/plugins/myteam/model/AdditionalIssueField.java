/* (C)2022 */
package ru.mail.jira.plugins.myteam.model;

import net.java.ao.Entity;
import net.java.ao.schema.Table;

@Table("MYTEAM_ISSUE_CFG_CF")
public interface AdditionalIssueField extends Entity {
  String getFieldId();

  void setFieldId(String fieldId);

  String getValue();

  void setValue(String value);

  IssueCreationSettings getIssueCreationSettings();

  void setIssueCreationSettings(IssueCreationSettings issueCreationSettings);
}
