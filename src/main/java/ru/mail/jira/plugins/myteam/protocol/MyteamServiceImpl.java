/* (C)2020 */
package ru.mail.jira.plugins.myteam.protocol;

import com.atlassian.jira.user.ApplicationUser;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.mail.jira.plugins.myteam.configuration.UserData;
import ru.mail.jira.plugins.myteam.protocol.events.JiraNotifyEvent;
import ru.mail.jira.plugins.myteam.protocol.listeners.MyteamEventsListener;

@Service
public class MyteamServiceImpl implements MyteamService {
  private final UserData userData;
  private final MyteamEventsListener myteamEventsListener;

  @Autowired
  public MyteamServiceImpl(MyteamEventsListener myteamEventsListener, UserData userData) {
    this.userData = userData;
    this.myteamEventsListener = myteamEventsListener;
  }

  @Override
  public boolean sendMessage(ApplicationUser user, String message) {
    if (user == null || StringUtils.isEmpty(message))
      throw new IllegalArgumentException("User and message must be specified");

    String mrimLogin = userData.getMrimLogin(user);
    if (user.isActive() && !StringUtils.isBlank(mrimLogin) && userData.isEnabled(user)) {
      sendMessage(mrimLogin, message);
      return true;
    }
    return false;
  }

  @Override
  public void sendMessage(String chatId, String message) {
    myteamEventsListener.publishEvent(new JiraNotifyEvent(chatId, message, null));
  }
}
