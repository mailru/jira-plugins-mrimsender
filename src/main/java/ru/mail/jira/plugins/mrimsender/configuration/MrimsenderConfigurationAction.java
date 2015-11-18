package ru.mail.jira.plugins.mrimsender.configuration;

import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import org.apache.commons.lang3.StringUtils;
import ru.mail.jira.plugins.commons.CommonUtils;
import ru.mail.jira.plugins.mrimsender.protocol.MrimsenderThread;
import ru.mail.jira.plugins.mrimsender.protocol.packages.Worker;

import java.util.List;

public class MrimsenderConfigurationAction extends JiraWebActionSupport {
    private final PluginData pluginData;

    private boolean saved;
    private String host;
    private String port;
    private String login;
    private String password;
    private boolean enabledByDefault;
    private String notifiedUsers;

    private Integer portParsed;
    private List<String> notifiedUserKeys;

    public MrimsenderConfigurationAction(PluginData pluginData) {
        this.pluginData = pluginData;
    }

    @Override
    public String doDefault() throws Exception {
        host = pluginData.getHost();
        port = pluginData.getPort() == null ? null : pluginData.getPort().toString();
        login = pluginData.getLogin();
        password = pluginData.getPassword();
        enabledByDefault = pluginData.isEnabledByDefault();
        notifiedUsers = CommonUtils.convertUserKeysToJoinedString(pluginData.getNotifiedUserKeys());
        return INPUT;
    }

    @RequiresXsrfCheck
    @Override
    protected String doExecute() throws Exception {
        pluginData.setHost(host);
        pluginData.setPort(portParsed);
        pluginData.setLogin(login);
        pluginData.setPassword(password);
        pluginData.setEnabledByDefault(enabledByDefault);
        pluginData.setNotifiedUserKeys(notifiedUserKeys);
        MrimsenderThread.relogin();

        saved = true;
        port = portParsed == null ? null : portParsed.toString();
        notifiedUsers = CommonUtils.convertUserKeysToJoinedString(notifiedUserKeys);
        return INPUT;
    }

    @Override
    protected void doValidation() {
        if (!StringUtils.isBlank(port))
            try {
                portParsed = Integer.valueOf(port.trim());
            } catch (NumberFormatException e) {
                addError("port", getText("ru.mail.jira.plugins.mrimsender.configuration.specifyPort"));
            }

        if (!getErrors().containsKey("port"))
            if (StringUtils.isEmpty(host) ^ portParsed == null)
                addError("port", getText("ru.mail.jira.plugins.mrimsender.configuration.specifyHostAndPort"));

        if (StringUtils.isEmpty(login))
            addError("login", getText("ru.mail.jira.plugins.mrimsender.configuration.specifyLogin"));

        if (StringUtils.isEmpty(password))
            addError("password", getText("ru.mail.jira.plugins.mrimsender.configuration.specifyPassword"));

        if (!invalidInput())
            try {
                Worker worker = new Worker(host.trim(), portParsed);
                try {
                    if (!worker.login(login, password))
                        addError("password", getText("ru.mail.jira.plugins.mrimsender.configuration.invalidLoginAndPassword"));
                } finally {
                    worker.close();
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                if (StringUtils.isEmpty(host) && portParsed == null)
                    addErrorMessage(getText("ru.mail.jira.plugins.mrimsender.configuration.invalidHostAndPort"));
                else
                    addError("port", getText("ru.mail.jira.plugins.mrimsender.configuration.invalidHostAndPort"));
            }

        try {
            notifiedUserKeys = CommonUtils.convertJoinedStringToUserKeys(notifiedUsers);
        } catch (IllegalArgumentException e) {
            addError("notifiedUsers", e.getMessage());
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public boolean isSaved() {
        return saved;
    }

    @SuppressWarnings("UnusedDeclaration")
    public String getHost() {
        return host;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setHost(String host) {
        this.host = host;
    }

    @SuppressWarnings("UnusedDeclaration")
    public String getPort() {
        return port;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setPort(String port) {
        this.port = port;
    }

    @SuppressWarnings("UnusedDeclaration")
    public String getLogin() {
        return login;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setLogin(String login) {
        this.login = login;
    }

    @SuppressWarnings("UnusedDeclaration")
    public String getPassword() {
        return password;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setPassword(String password) {
        this.password = password;
    }

    @SuppressWarnings("UnusedDeclaration")
    public boolean isEnabledByDefault() {
        return enabledByDefault;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setEnabledByDefault(boolean enabledByDefault) {
        this.enabledByDefault = enabledByDefault;
    }

    @SuppressWarnings("UnusedDeclaration")
    public String getNotifiedUsers() {
        return notifiedUsers;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setNotifiedUsers(String notifiedUsers) {
        this.notifiedUsers = notifiedUsers;
    }
}
