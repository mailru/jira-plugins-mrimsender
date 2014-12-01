package ru.mail.jira.plugins.mrimsender.protocol;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.ApplicationUsers;
import com.atlassian.jira.user.util.UserManager;
import org.apache.commons.lang3.StringUtils;
import ru.mail.jira.plugins.mrimsender.configuration.UserData;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class UserSearcher {
    public static final UserSearcher INSTANCE = new UserSearcher();

    private final ConcurrentMap<String, String> mrimLoginCache = new ConcurrentHashMap<String, String>();
    private final ConcurrentMap<String, String> emailCache = new ConcurrentHashMap<String, String>();
    private final UserData userData = new UserData();
    private final UserManager userManager = ComponentAccessor.getUserManager();

    private UserSearcher() {
        for (User directoryUser : userManager.getAllUsers())
            try {
                updateUser(ApplicationUsers.from(directoryUser));
            } catch (IllegalStateException ignore) {
                // Workaround for users that have no unique key mapping
            }
    }

    public void updateUser(ApplicationUser user) {
        String mrimLogin = userData.getMrimLogin(user);
        if (user.isActive() && StringUtils.isNotBlank(mrimLogin))
            mrimLoginCache.put(mrimLogin.toLowerCase(), user.getKey());

        String email = user.getEmailAddress();
        if (StringUtils.isNotBlank(email))
            emailCache.put(email.toLowerCase(), user.getKey());
    }

    public ApplicationUser getUserByMrimLogin(String mrimLogin) {
        String userKey = mrimLoginCache.get(mrimLogin.toLowerCase());
        if (StringUtils.isNotEmpty(userKey)) {
            ApplicationUser user = userManager.getUserByKey(userKey);
            if (user != null && user.isActive() && mrimLogin.equalsIgnoreCase(userData.getMrimLogin(user)))
                return user;
            else
                mrimLoginCache.remove(mrimLogin.toLowerCase());
        }

        for (User directoryUser : userManager.getAllUsers())
            try {
                ApplicationUser user = ApplicationUsers.from(directoryUser);
                if (user.isActive() && mrimLogin.equalsIgnoreCase(userData.getMrimLogin(user))) {
                    mrimLoginCache.put(mrimLogin.toLowerCase(), user.getKey());
                    return user;
                }
            } catch (IllegalStateException ignore) {
                // Workaround for users that have no unique key mapping
            }

        return null;
    }

    public ApplicationUser getUserByEmail(String email) {
        String userKey = emailCache.get(email.toLowerCase());
        if (StringUtils.isNotEmpty(userKey)) {
            ApplicationUser user = userManager.getUserByKey(userKey);
            if (user != null && email.equalsIgnoreCase(user.getEmailAddress()))
                return user;
            else
                emailCache.remove(email.toLowerCase());
        }

        for (User directoryUser : userManager.getAllUsers())
            try {
                ApplicationUser user = ApplicationUsers.from(directoryUser);
                if (email.equalsIgnoreCase(user.getEmailAddress())) {
                    emailCache.put(email.toLowerCase(), user.getKey());
                    return user;
                }
            } catch (IllegalStateException ignore) {
                // Workaround for users that have no unique key mapping
            }

        return null;
    }
}
