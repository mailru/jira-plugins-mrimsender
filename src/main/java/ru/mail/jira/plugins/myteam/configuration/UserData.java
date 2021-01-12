/* (C)2020 */
package ru.mail.jira.plugins.myteam.configuration;

import com.atlassian.jira.bc.user.search.UserSearchService;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.UserPropertyManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.myteam.model.PluginData;

@Component
public class UserData {
  private static final String MRIM_LOGIN_USER_PROPERTY = "USER_MYTEAM_LOGIN";
  private static final String IS_ENABLED_USER_PROPERTY = "USER_MYTEAM_STATUS";

  private final PluginData pluginData;
  private final UserPropertyManager userPropertyManager;
  private final UserSearchService userSearchService;
  private final Cache<String, ApplicationUser> userByMrimLoginCache =
      Caffeine.newBuilder().expireAfterWrite(8, TimeUnit.HOURS).build();

  @Autowired
  public UserData(
      @ComponentImport UserPropertyManager userPropertyManager,
      @ComponentImport UserSearchService userSearchService,
      PluginData pluginData) {
    this.userPropertyManager = userPropertyManager;
    this.userSearchService = userSearchService;
    this.pluginData = pluginData;
  }

  public String getMrimLogin(ApplicationUser user) {
    String mrimLogin = userPropertyManager.getPropertySet(user).getString(MRIM_LOGIN_USER_PROPERTY);
    if (mrimLogin == null) mrimLogin = user.getEmailAddress();
    return mrimLogin;
  }

  public void setMrimLogin(ApplicationUser user, String mrimLogin) {
    userPropertyManager.getPropertySet(user).setString(MRIM_LOGIN_USER_PROPERTY, mrimLogin);
  }

  public boolean isEnabled(ApplicationUser user) {
    String enabled = userPropertyManager.getPropertySet(user).getString(IS_ENABLED_USER_PROPERTY);
    if (enabled == null) return pluginData.isEnabledByDefault();
    return Boolean.parseBoolean(enabled);
  }

  public void setEnabled(ApplicationUser user, boolean enabled) {
    userPropertyManager
        .getPropertySet(user)
        .setString(IS_ENABLED_USER_PROPERTY, Boolean.toString(enabled));
  }

  @Nullable
  /** mrimLogin in most cases equals to user email */
  public ApplicationUser getUserByMrimLogin(String mrimLogin) {
    return userByMrimLoginCache.get(
        mrimLogin,
        (login) ->
            StreamSupport.stream(userSearchService.findUsersByEmail(login).spliterator(), false)
                .filter(ApplicationUser::isActive)
                .findFirst()
                .orElse(null));
  }
}
