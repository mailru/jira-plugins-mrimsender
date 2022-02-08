/* (C)2020 */
package ru.mail.jira.plugins.myteam.model;

import java.util.List;
import java.util.Set;

public interface PluginData {
  Boolean isSetTokenViaFile();

  void setSetTokenViaFile(Boolean setTokenViaFile);

  String getToken();

  void setToken(String token);

  String getTokenFilePath();

  void setTokenFilePath(String tokenFilePath);

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

  String getProfileLink();

  void setProfileLink(String profileLink);

  Set<Long> getExcludingProjectIds();

  void setExcludingProjectIds(Set<Long> excludingProjectIds);

  Set<Long> getChatCreationBannedProjectIds();

  void setChatCreationBannedProjectIds(Set<Long> chatCreationBannedProjectIds);
}
