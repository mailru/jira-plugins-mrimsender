/* (C)2020 */
package ru.mail.jira.plugins.myteam.service.impl;

import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.mail.jira.plugins.myteam.bot.events.JiraNotifyEvent;
import ru.mail.jira.plugins.myteam.bot.listeners.MyteamEventsListener;
import ru.mail.jira.plugins.myteam.commons.Utils;
import ru.mail.jira.plugins.myteam.component.UserData;
import ru.mail.jira.plugins.myteam.service.MyteamService;

@Service
public class MyteamServiceImpl implements MyteamService {
  private final GroupManager groupManager;
  private final UserData userData;
  private final MyteamEventsListener myteamEventsListener;

  @Autowired
  public MyteamServiceImpl(
      @ComponentImport GroupManager groupManager,
      MyteamEventsListener myteamEventsListener,
      UserData userData) {
    this.groupManager = groupManager;
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
  public void sendMessageToUserGroup(String groupName, String message) {
    if (groupName == null || StringUtils.isEmpty(message))
      throw new IllegalArgumentException("Group name and message must be specified");

    Group group = groupManager.getGroup(groupName);
    if (group == null)
      throw new IllegalArgumentException(
          String.format("Group with name %s does not exist", groupName));

    for (ApplicationUser user : groupManager.getUsersInGroup(group)) {
      sendMessage(user, message);
    }
  }

  @Override
  public void sendMessage(String chatId, String message) {
    myteamEventsListener.publishEvent(new JiraNotifyEvent(chatId, Utils.shieldText(message), null));
  }
}
