/* (C)2022 */
package ru.mail.jira.plugins.myteam.accessrequest.controller.dto;

import com.atlassian.jira.avatar.Avatar;
import com.atlassian.jira.avatar.AvatarService;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.commons.dto.jira.UserDto;

@Component
public class DtoUtils {
  private final AvatarService avatarService;
  private final JiraAuthenticationContext jiraAuthenticationContext;

  public DtoUtils(
      @ComponentImport AvatarService avatarService,
      @ComponentImport JiraAuthenticationContext jiraAuthenticationContext) {
    this.avatarService = avatarService;
    this.jiraAuthenticationContext = jiraAuthenticationContext;
  }

  @Nullable
  public UserDto buildUserDto(ApplicationUser user) {
    ApplicationUser loggedInUser = jiraAuthenticationContext.getLoggedInUser();
    if (user == null) return null;
    UserDto userDto = new UserDto();
    userDto.setUserKey(user.getKey());
    userDto.setDisplayName(user.getDisplayName());
    userDto.setEmail(user.getEmailAddress());
    if (loggedInUser != null) {
      userDto.setAvatarUrl(
          avatarService.getAvatarURL(loggedInUser, user, Avatar.Size.LARGE).toString());
    }
    return userDto;
  }

  @Nullable
  public UserDto buildUserDto(ApplicationUser user, ApplicationProperties applicationProperties) {
    UserDto userDto = buildUserDto(user);
    if (userDto == null) return null;
    userDto.setProfileUrl(
        String.format(
            "%s/secure/ViewProfile.jspa?name=%s",
            applicationProperties.getString(APKeys.JIRA_BASEURL), user.getName()));
    return userDto;
  }
}
