/* (C)2022 */
package ru.mail.jira.plugins.myteam.accessrequest.model;

import java.util.Date;
import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;
import org.jetbrains.annotations.Nullable;

@Table("ACCESS_REQUEST_HIST")
public interface AccessRequestHistory extends Entity {
  String getRequesterKey();

  void setRequesterKey(String requesterKey);

  @Indexed
  long getIssueId();

  void setIssueId(long issueId);

  @StringLength(StringLength.UNLIMITED)
  String getUserKeys();

  void setUserKeys(String userKeys);

  @StringLength(StringLength.UNLIMITED)
  String getMessage();

  void setMessage(String message);

  Date getDate();

  void setDate(Date date);

  @Nullable
  Boolean getReplyStatus();

  void setReplyStatus(Boolean status);

  @Nullable
  String getReplyAdmin();

  void setReplyAdmin(String userKey);

  @Nullable
  Date getReplyDate();

  void setReplyDate(Date date);
}
