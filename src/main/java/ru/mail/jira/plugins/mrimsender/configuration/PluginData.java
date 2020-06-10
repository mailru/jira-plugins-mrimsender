package ru.mail.jira.plugins.mrimsender.configuration;

import java.util.List;

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
}
