package ru.mail.jira.plugins.mrimsender.configuration;

import com.atlassian.jira.bc.user.search.UserSearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.UserPropertyManager;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class UserData {
    private static final String MRIM_LOGIN_USER_PROPERTY = "USER_MRIM_LOGIN";
    private static final String IS_ENABLED_USER_PROPERTY = "USER_MRIM_STATUS";

    private final PluginData pluginData = ComponentAccessor.getOSGiComponentInstanceOfType(PluginData.class);
    private final UserPropertyManager userPropertyManager;
    private final UserSearchService userSearchService;

    public UserData(UserPropertyManager userPropertyManager, UserSearchService userSearchService) {
        this.userPropertyManager = userPropertyManager;
        this.userSearchService = userSearchService;
    }

    public String getMrimLogin(ApplicationUser user) {
        String mrimLogin = userPropertyManager.getPropertySet(user).getString(MRIM_LOGIN_USER_PROPERTY);
        if (mrimLogin == null)
            mrimLogin = user.getEmailAddress();
        return mrimLogin;
    }

    public void setMrimLogin(ApplicationUser user, String mrimLogin) {
        userPropertyManager.getPropertySet(user).setString(MRIM_LOGIN_USER_PROPERTY, mrimLogin);
    }

    public boolean isEnabled(ApplicationUser user) {
        String enabled = userPropertyManager.getPropertySet(user).getString(IS_ENABLED_USER_PROPERTY);
        if (enabled == null)
            return pluginData.isEnabledByDefault();
        return Boolean.parseBoolean(enabled);
    }

    public void setEnabled(ApplicationUser user, boolean enabled) {
        userPropertyManager.getPropertySet(user).setString(IS_ENABLED_USER_PROPERTY, Boolean.toString(enabled));
    }

    @Nullable
    public ApplicationUser getUserByMrimLogin(String mrimLogin) {
        // mrimLogin in most cases equals to user email
        return StreamSupport.stream(userSearchService.findUsersByEmail(mrimLogin).spliterator(), false)
                            .filter(ApplicationUser::isActive)
                            .findFirst()
                            .orElse(null);
    }
}
