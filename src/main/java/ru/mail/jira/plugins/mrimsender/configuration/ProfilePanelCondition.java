package ru.mail.jira.plugins.mrimsender.configuration;

import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;

import java.util.Map;

public class ProfilePanelCondition implements Condition {
    @Override
    public void init(Map<String, String> paramMap) throws PluginParseException {
    }

    @Override
    public boolean shouldDisplay(Map<String, Object> paramMap) {
        ApplicationUser profileUser = (ApplicationUser) paramMap.get("profileUser");
        ApplicationUser currentUser = (ApplicationUser) paramMap.get("currentUser");
        return profileUser != null && profileUser.equals(currentUser);
    }
}
