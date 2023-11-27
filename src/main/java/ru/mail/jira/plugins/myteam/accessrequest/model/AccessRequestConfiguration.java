/* (C)2022 */
package ru.mail.jira.plugins.myteam.accessrequest.model;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

@Table("ACCESS_REQUEST_CONF")
public interface AccessRequestConfiguration extends Entity {
  @Indexed
  long getProjectId();

  void setProjectId(long projectId);

  @StringLength(StringLength.UNLIMITED)
  String getUserKeys();

  void setUserKeys(String userKeys);

  @StringLength(StringLength.UNLIMITED)
  String getGroups();

  void setGroups(String groups);

  @StringLength(StringLength.UNLIMITED)
  String getProjectRoleIds();

  void setProjectRoleIds(String projectRoleIds);

  @StringLength(StringLength.UNLIMITED)
  String getUserFieldIds();

  void setUserFieldIds(String userFieldIds);

  boolean isSendEmail();

  void setSendEmail(boolean sendEmail);

  boolean isSendMessage();

  void setSendMessage(boolean sendMessage);

  @StringLength(StringLength.UNLIMITED)
  String getVotersKeys();

  void setVotersKeys(String voters);
}
