/* (C)2020 */
package ru.mail.jira.plugins.myteam.model;

import java.util.List;
import java.util.Set;

public interface PluginData {
  String getToken();

  void setToken(String token);

  boolean isEnabledByDefault();

  void setEnabledByDefault(boolean enabledByDefault);

  List<String> getNotifiedUserKeys();

  void setNotifiedUserKeys(List<String> notifiedUserKeys);

  String getMainNodeId();

  void setMainNodeId(String mainNodeId);

  String getBotApiUrl();

  void setBotApiUrl(String botApiUrl);

  String getBotName();

  void setBotName(String botName);

  String getBotLink();

  void setBotLink(String botLink);

  Set<Long> getExcludingProjectIds();

  void setExcludingProjectIds(Set<Long> excludingProjectIds);
}
