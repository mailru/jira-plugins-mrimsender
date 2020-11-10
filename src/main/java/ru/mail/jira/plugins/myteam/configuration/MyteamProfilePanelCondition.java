/* (C)2020 */
package ru.mail.jira.plugins.myteam.configuration;

import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;
import java.util.Map;

public class MyteamProfilePanelCondition implements Condition {
  @Override
  public void init(Map<String, String> paramMap) throws PluginParseException {}

  @Override
  public boolean shouldDisplay(Map<String, Object> paramMap) {
    ApplicationUser profileUser = (ApplicationUser) paramMap.get("profileUser");
    ApplicationUser currentUser = (ApplicationUser) paramMap.get("currentUser");
    return profileUser != null && profileUser.equals(currentUser);
  }
}
