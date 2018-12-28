package ru.mail.jira.plugins.mrimsender.configuration;

import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import org.apache.commons.lang3.StringUtils;
import ru.mail.jira.plugins.commons.CommonUtils;
import ru.mail.jira.plugins.mrimsender.protocol.IcqBot;

import java.util.List;

public class MrimsenderConfigurationAction extends JiraWebActionSupport {
    private final PluginData pluginData;
    private final IcqBot icqBot;

    private boolean saved;
    private String token;
    private boolean enabledByDefault;
    private String notifiedUsers;

    private List<String> notifiedUserKeys;

    public MrimsenderConfigurationAction(PluginData pluginData, IcqBot icqBot) {
        this.pluginData = pluginData;
        this.icqBot = icqBot;
    }

    @Override
    public String doDefault() {
        token = pluginData.getToken();
        enabledByDefault = pluginData.isEnabledByDefault();
        notifiedUsers = CommonUtils.convertUserKeysToJoinedString(pluginData.getNotifiedUserKeys());
        return INPUT;
    }

    @RequiresXsrfCheck
    @Override
    protected String doExecute() {
        pluginData.setToken(token);
        pluginData.setEnabledByDefault(enabledByDefault);
        pluginData.setNotifiedUserKeys(notifiedUserKeys);

        saved = true;
        notifiedUsers = CommonUtils.convertUserKeysToJoinedString(notifiedUserKeys);

        icqBot.initToken();
        return INPUT;
    }

    @Override
    protected void doValidation() {
        if (StringUtils.isEmpty(token))
            addError("token", getText("ru.mail.jira.plugins.mrimsender.configuration.specifyToken"));

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
    public String getToken() {
        return token;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setToken(String token) {
        this.token = token;
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