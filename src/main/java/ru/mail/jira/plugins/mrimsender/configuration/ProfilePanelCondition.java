package ru.mail.jira.plugins.mrimsender.configuration;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;

import java.util.Map;

public class ProfilePanelCondition implements Condition {
    @Override
    public void init(Map<String, String> paramMap) throws PluginParseException {
    }

    @Override
    public boolean shouldDisplay(Map<String, Object> paramMap) {
        User profileUser = (User) paramMap.get("profileUser");
        User currentUser = (User) paramMap.get("currentUser");
        return profileUser != null && profileUser.equals(currentUser);
    }
}
