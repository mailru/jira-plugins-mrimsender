package ru.mail.jira.plugins.calendar.common;

import com.atlassian.jira.avatar.Avatar;
import com.atlassian.jira.avatar.AvatarService;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.calendar.rest.dto.UserDto;

@Component
public class UserUtils {
    private final AvatarService avatarService;

    @Autowired
    public UserUtils(
            @ComponentImport AvatarService avatarService
    ) {
        this.avatarService = avatarService;
    }

    public UserDto buildUserDto(ApplicationUser user, Avatar.Size avatarSize) {
        UserDto result = new UserDto();
        result.setKey(user.getKey());
        result.setName(user.getName());
        result.setDisplayName(user.getDisplayName());
        result.setAvatarUrl(avatarService.getAvatarURL(user, user, avatarSize).toString());
        return result;
    }

    public UserDto buildNonExistingUserDto(String key) {
        UserDto result = new UserDto();
        result.setKey(key);
        result.setName(key);
        result.setDisplayName(key);
        result.setAvatarUrl(null);
        return result;
    }
}
