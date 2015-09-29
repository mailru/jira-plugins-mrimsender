package ru.mail.jira.plugins.mrimsender.protocol;

import com.atlassian.jira.user.ApplicationUser;

public interface MrimService {
    /**
     * Send message to user in Mail.Ru Agent.
     *
     * @param user    recipient of the message
     * @param message sent message
     * @return        message sent or not
     */
    boolean sendMessage(ApplicationUser user, String message);
}
