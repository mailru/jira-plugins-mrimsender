/* (C)2022 */
package ru.mail.jira.plugins.myteam.db.model;

import java.util.Date;
import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;
import org.jetbrains.annotations.Nullable;

@Table("MYTEAM_SUBSCRIPTION")
public interface FilterSubscription extends Entity {
  @Indexed
  Long getFilterId();

  void setFilterId(Long filterId);

  @Indexed
  String getUserKey();

  void setUserKey(String userKey);

  @Indexed
  RecipientsType getRecipientsType();

  void setRecipientsType(RecipientsType recipientsType);

  @StringLength(StringLength.UNLIMITED)
  String getRecipients();

  void setRecipients(String recipients);

  String getScheduleMode();

  void setScheduleMode(String scheduleMode);

  String getCronExpression();

  void setCronExpression(String cronExpression);

  @Nullable
  Date getLastRun();

  void setLastRun(@Nullable Date lastRun);

  FilterSubscriptionType getType();

  void setType(FilterSubscriptionType type);

  boolean isEmailOnEmpty();

  void setEmailOnEmpty(boolean emailOnEmpty);
}
