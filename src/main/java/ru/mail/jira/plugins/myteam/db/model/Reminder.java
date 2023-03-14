/* (C)2023 */
package ru.mail.jira.plugins.myteam.db.model;

import java.util.Date;
import net.java.ao.Entity;
import net.java.ao.schema.StringLength;
import org.jetbrains.annotations.Nullable;

public interface Reminder extends Entity {
  String getIssueKey();

  void setIssueKey(@Nullable String issueKey);

  String getUserEmail();

  void setUserEmail(@Nullable String userEmail);

  @Nullable
  @StringLength(StringLength.UNLIMITED)
  String getDescription();

  void setDescription(@Nullable String description);

  Date getDate();

  void setDate(@Nullable Date date);
}
