package ru.mail.jira.plugins.mrimsender.configuration;

import java.util.List;

public interface PluginData {
    @Deprecated
    String getHost();
    @Deprecated
    void setHost(String host);

    @Deprecated
    Integer getPort();
    @Deprecated
    void setPort(Integer port);

    @Deprecated
    String getLogin();
    @Deprecated
    void setLogin(String login);

    @Deprecated
    String getPassword();
    @Deprecated
    void setPassword(String password);

    String getToken();
    void setToken(String token);

    boolean isEnabledByDefault();
    void setEnabledByDefault(boolean enabledByDefault);

    List<String> getNotifiedUserKeys();
    void setNotifiedUserKeys(List<String> notifiedUserKeys);
}
