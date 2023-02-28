/* (C)2020 */
package ru.mail.jira.plugins.myteam.component;

import com.atlassian.jira.bc.user.search.UserSearchService;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.UserPropertyManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.myteam.service.PluginData;

@Component
public class UserData {
  private static final String MRIM_LOGIN_USER_PROPERTY = "USER_MYTEAM_LOGIN";
  private static final String IS_ENABLED_USER_PROPERTY = "USER_MYTEAM_STATUS";
  private static final String IS_CREATE_CHATS_WITH_USER_ALLOWED = "USER_MYTEAM_CHATSCREATION";

  private final PluginData pluginData;
  private final UserPropertyManager userPropertyManager;
  private final UserSearchService userSearchService;
  private final Cache<String, ApplicationUser> userByMrimLoginCache =
      Caffeine.newBuilder().expireAfterAccess(8, TimeUnit.HOURS).build();

  @Autowired
  public UserData(
      @ComponentImport UserPropertyManager userPropertyManager,
      @ComponentImport UserSearchService userSearchService,
      PluginData pluginData) {
    this.userPropertyManager = userPropertyManager;
    this.userSearchService = userSearchService;
    this.pluginData = pluginData;
  }

  public void setMrimLogin(ApplicationUser user, String mrimLogin) {
    userPropertyManager.getPropertySet(user).setString(MRIM_LOGIN_USER_PROPERTY, mrimLogin);
  }

  public boolean isEnabled(ApplicationUser user) {
    try {
      String enabled = userPropertyManager.getPropertySet(user).getString(IS_ENABLED_USER_PROPERTY);
      if (enabled == null) return pluginData.isEnabledByDefault();
      return Boolean.parseBoolean(enabled);
    } catch (Exception e) {
      return pluginData.isEnabledByDefault();
    }
  }

  public void setEnabled(ApplicationUser user, boolean enabled) {
    userPropertyManager
        .getPropertySet(user)
        .setString(IS_ENABLED_USER_PROPERTY, Boolean.toString(enabled));
  }

  public boolean isCreateChatsWithUserAllowed(ApplicationUser user) {
    try {
      String isAllowed =
          userPropertyManager.getPropertySet(user).getString(IS_CREATE_CHATS_WITH_USER_ALLOWED);
      // ALLOWED BY DEFAULT
      if (isAllowed == null) return true;
      return Boolean.parseBoolean(isAllowed);
    } catch (Exception e) {
      return true;
    }
  }

  public void setCreateChatsWithUserAllowed(ApplicationUser user, boolean isAllowed) {
    userPropertyManager
        .getPropertySet(user)
        .setString(IS_CREATE_CHATS_WITH_USER_ALLOWED, Boolean.toString(isAllowed));
  }

  @Nullable
  /** mrimLogin in most cases equals to user email */
  public ApplicationUser getUserByMrimLogin(@Nullable String mrimLogin) {
    if (mrimLogin == null) {
      return null;
    }
    return userByMrimLoginCache.get(
        mrimLogin,
        (login) ->
            StreamSupport.stream(userSearchService.findUsersByEmail(login).spliterator(), false)
                .findFirst()
                .orElse(null));
  }
}
