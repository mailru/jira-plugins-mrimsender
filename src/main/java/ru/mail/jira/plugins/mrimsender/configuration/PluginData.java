package ru.mail.jira.plugins.mrimsender.configuration;

import java.util.List;

public interface PluginData {
    String getHost();
    void setHost(String host);

    Integer getPort();
    void setPort(Integer port);

    String getLogin();
    void setLogin(String login);

    String getPassword();
    void setPassword(String password);

    boolean isEnabledByDefault();
    void setEnabledByDefault(boolean enabledByDefault);

    List<String> getNotifiedUserKeys();
    void setNotifiedUserKeys(List<String> notifiedUserKeys);
}
