/* (C)2024 */
package ru.mail.jira.plugins.myteam.service.subscription;

import ru.mail.jira.plugins.myteam.db.model.FilterSubscription;
import ru.mail.jira.plugins.myteam.db.model.RecipientsType;

public interface FilterSubscriptionSender {

  void sendMyteamNotifications(final FilterSubscription filterSubscription);

  RecipientsType getRecipientType();
}
