/* (C)2020 */
package ru.mail.jira.plugins.myteam.service;

import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

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

  List<String> getSubscriptionsExcludingGroups();

  void setSubscriptionsExcludingGroups(List<String> subscriptionsExcludingGroups);

  @Nullable
  String getMainNodeId();

  void setMainNodeId(@Nullable String mainNodeId);

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
