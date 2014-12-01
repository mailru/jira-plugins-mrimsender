package ru.mail.jira.plugins.mrimsender.configuration;

import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

import java.util.List;

public class PluginDataImpl implements PluginData {
    private static final String PLUGIN_PREFIX = "ru.mail.jira.plugins.mrimsender:";
    private static final String HOST = PLUGIN_PREFIX + "host";
    private static final String PORT = PLUGIN_PREFIX + "port";
    private static final String LOGIN = PLUGIN_PREFIX + "login";
    private static final String PASSWORD = PLUGIN_PREFIX + "password";
    private static final String NOTIFIED_USER_KEYS = PLUGIN_PREFIX + "notifiedUserKeys";

    private final PluginSettingsFactory pluginSettingsFactory;

    public PluginDataImpl(PluginSettingsFactory pluginSettingsFactory) {
        this.pluginSettingsFactory = pluginSettingsFactory;
    }

    @Override
    public String getHost() {
        return (String) pluginSettingsFactory.createGlobalSettings().get(HOST);
    }

    @Override
    public void setHost(String host) {
        pluginSettingsFactory.createGlobalSettings().put(HOST, host);
    }

    @Override
    public Integer getPort() {
        try {
            return Integer.parseInt((String) pluginSettingsFactory.createGlobalSettings().get(PORT));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public void setPort(Integer port) {
        pluginSettingsFactory.createGlobalSettings().put(PORT, port == null ? null : port.toString());
    }

    @Override
    public String getLogin() {
        return (String) pluginSettingsFactory.createGlobalSettings().get(LOGIN);
    }

    @Override
    public void setLogin(String login) {
        pluginSettingsFactory.createGlobalSettings().put(LOGIN, login);
    }

    @Override
    public String getPassword() {
        return (String) pluginSettingsFactory.createGlobalSettings().get(PASSWORD);
    }

    @Override
    public void setPassword(String password) {
        pluginSettingsFactory.createGlobalSettings().put(PASSWORD, password);
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
}
