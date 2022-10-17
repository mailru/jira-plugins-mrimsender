/* (C)2020 */
package ru.mail.jira.plugins.myteam.service.impl;

import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.commons.CommonUtils;
import ru.mail.jira.plugins.myteam.service.PluginData;

@Component
public class PluginDataImpl implements PluginData {
  private static final String PLUGIN_PREFIX = "ru.mail.jira.plugins.myteam:";
  private static final String IS_SET_TOKEN_VIA_FILE = PLUGIN_PREFIX + "setTokenViaFile";
  private static final String TOKEN = PLUGIN_PREFIX + "token";
  private static final String TOKEN_FILE_PATH = PLUGIN_PREFIX + "tokenFilePath";
  private static final String ENABLED_BY_DEFAULT = PLUGIN_PREFIX + "enabledByDefault";
  private static final String NOTIFIED_USER_KEYS = PLUGIN_PREFIX + "notifiedUserKeys";
  private static final String MAIN_NODE_ID = PLUGIN_PREFIX + "mainNodeId";
  private static final String BOT_API_URL = PLUGIN_PREFIX + "botApiUrl";
  private static final String BOT_NAME = PLUGIN_PREFIX + "botName";
  private static final String BOT_LINK = PLUGIN_PREFIX + "botLink";
  private static final String PROFILE_LINK = PLUGIN_PREFIX + "profileLink";
  private static final String EXCLUDING_PROJECT_IDS = PLUGIN_PREFIX + "excludingProjectIds";
  private static final String CHAT_CREATION_BANNED_PROJECT_IDS =
      PLUGIN_PREFIX + "chatCreationBannedProjectIds";

  private final PluginSettingsFactory pluginSettingsFactory;

  public PluginDataImpl(@ComponentImport PluginSettingsFactory pluginSettingsFactory) {
    this.pluginSettingsFactory = pluginSettingsFactory;
  }

  @Override
  public Boolean isSetTokenViaFile() {
    return Boolean.parseBoolean(
        (String) pluginSettingsFactory.createGlobalSettings().get(IS_SET_TOKEN_VIA_FILE));
  }

  @Override
  public void setSetTokenViaFile(Boolean setTokenViaFile) {
    pluginSettingsFactory
        .createGlobalSettings()
        .put(IS_SET_TOKEN_VIA_FILE, String.valueOf(setTokenViaFile));
  }

  @Override
  public String getToken() {
    return (String) pluginSettingsFactory.createGlobalSettings().get(TOKEN);
  }

  @Override
  public void setToken(String token) {
    pluginSettingsFactory.createGlobalSettings().put(TOKEN, token);
  }

  @Override
  public String getTokenFilePath() {
    return (String) pluginSettingsFactory.createGlobalSettings().get(TOKEN_FILE_PATH);
  }

  @Override
  public void setTokenFilePath(String tokenFilePath) {
    pluginSettingsFactory.createGlobalSettings().put(TOKEN_FILE_PATH, tokenFilePath);
  }

  @Override
  public boolean isEnabledByDefault() {
    return Boolean.parseBoolean(
        (String) pluginSettingsFactory.createGlobalSettings().get(ENABLED_BY_DEFAULT));
  }

  @Override
  public void setEnabledByDefault(boolean enabledByDefault) {
    pluginSettingsFactory
        .createGlobalSettings()
        .put(ENABLED_BY_DEFAULT, String.valueOf(enabledByDefault));
  }

  @Override
  public List<String> getNotifiedUserKeys() {
    //noinspection unchecked
    return (List<String>) pluginSettingsFactory.createGlobalSettings().get(NOTIFIED_USER_KEYS);
  }

  @Override
  public void setNotifiedUserKeys(List<String> notifiedUserKeys) {
    pluginSettingsFactory.createGlobalSettings().put(NOTIFIED_USER_KEYS, notifiedUserKeys);
  }

  @Override
  public String getMainNodeId() {
    return (String) pluginSettingsFactory.createGlobalSettings().get(MAIN_NODE_ID);
  }

  @Override
  public void setMainNodeId(@Nullable String mainNodeId) {
    pluginSettingsFactory.createGlobalSettings().put(MAIN_NODE_ID, mainNodeId);
  }

  @Override
  public String getBotApiUrl() {
    return (String) pluginSettingsFactory.createGlobalSettings().get(BOT_API_URL);
  }

  @Override
  public void setBotApiUrl(String botApiUrl) {
    pluginSettingsFactory.createGlobalSettings().put(BOT_API_URL, botApiUrl);
  }

  @Override
  public String getBotName() {
    return (String) pluginSettingsFactory.createGlobalSettings().get(BOT_NAME);
  }

  @Override
  public void setBotName(String botName) {
    pluginSettingsFactory.createGlobalSettings().put(BOT_NAME, botName);
  }

  @Override
  public String getBotLink() {
    return (String) pluginSettingsFactory.createGlobalSettings().get(BOT_LINK);
  }

  @Override
  public void setBotLink(String botLink) {
    pluginSettingsFactory.createGlobalSettings().put(BOT_LINK, botLink);
  }

  @Override
  public String getProfileLink() {
    return (String) pluginSettingsFactory.createGlobalSettings().get(PROFILE_LINK);
  }

  @Override
  public void setProfileLink(String botLink) {
    pluginSettingsFactory.createGlobalSettings().put(PROFILE_LINK, botLink);
  }

  @Override
  public Set<Long> getExcludingProjectIds() {
    String excludingProjectIds =
        (String) pluginSettingsFactory.createGlobalSettings().get(EXCLUDING_PROJECT_IDS);
    if (excludingProjectIds == null) {
      return Collections.emptySet();
    }
    return CommonUtils.split(excludingProjectIds).stream()
        .map(Long::valueOf)
        .collect(Collectors.toSet());
  }

  @Override
  public void setExcludingProjectIds(Set<Long> excludingProjectIds) {
    pluginSettingsFactory
        .createGlobalSettings()
        .put(
            EXCLUDING_PROJECT_IDS,
            CommonUtils.join(
                excludingProjectIds.stream().map(String::valueOf).collect(Collectors.toList())));
  }

  @Override
  public Set<Long> getChatCreationBannedProjectIds() {
    String chatCreationPorjectIds =
        (String) pluginSettingsFactory.createGlobalSettings().get(CHAT_CREATION_BANNED_PROJECT_IDS);
    if (chatCreationPorjectIds == null) {
      return Collections.emptySet();
    }
    return CommonUtils.split(chatCreationPorjectIds).stream()
        .map(Long::valueOf)
        .collect(Collectors.toSet());
  }

  @Override
  public void setChatCreationBannedProjectIds(Set<Long> chatCreationBannedProjectIds) {
    pluginSettingsFactory
        .createGlobalSettings()
        .put(
            CHAT_CREATION_BANNED_PROJECT_IDS,
            CommonUtils.join(
                chatCreationBannedProjectIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.toList())));
  }
}
