/* (C)2022 */
package ru.mail.jira.plugins.myteam.accessrequest.model;

import java.util.Date;
import net.java.ao.Entity;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

@Table("ACCESS_REQUEST_HIST")
public interface AccessRequestHistory extends Entity {
  String getRequesterKey();

  void setRequesterKey(String requesterKey);

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
}
