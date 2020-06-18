package ru.mail.jira.plugins.myteam.configuration;

import com.atlassian.jira.bc.user.search.UserSearchService;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.UserPropertyManager;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;

public class UserData {
    private final static String MRIM_LOGIN_USER_PROPERTY = "USER_MYTEAM_LOGIN";
    private final static String IS_ENABLED_USER_PROPERTY = "USER_MYTEAM_STATUS";

    //private final PluginData pluginData = ComponentAccessor.getOSGiComponentInstanceOfType(PluginData.class);
    private final PluginData pluginData;
    private final UserPropertyManager userPropertyManager;
    private final UserSearchService userSearchService;
    private final Cache<String, ApplicationUser> userByMrimLoginCache = Caffeine.newBuilder()
                                                                                .expireAfterWrite(8, TimeUnit.HOURS)
                                                                                .build();

    public UserData(UserPropertyManager userPropertyManager, UserSearchService userSearchService, PluginData pluginData) {
        this.userPropertyManager = userPropertyManager;
        this.userSearchService = userSearchService;
        this.pluginData = pluginData;
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
    /**
     * mrimLogin in most cases equals to user email
     */
    public ApplicationUser getUserByMrimLogin(String mrimLogin) {
        return userByMrimLoginCache.get(mrimLogin, (login) -> StreamSupport.stream(userSearchService.findUsersByEmail(login).spliterator(), false)
                                                                           .filter(ApplicationUser::isActive)
                                                                           .findFirst()
                                                                           .orElse(null));
    }
}
