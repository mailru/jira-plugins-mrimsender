package ru.mail.jira.plugins.mrimsender.protocol;

import com.atlassian.jira.user.ApplicationUser;
import org.apache.commons.lang3.StringUtils;
import ru.mail.jira.plugins.mrimsender.configuration.UserData;
import ru.mail.jira.plugins.mrimsender.protocol.events.JiraNotifyEvent;
import ru.mail.jira.plugins.mrimsender.protocol.listeners.IcqEventsListener;

public class MrimServiceImpl implements MrimService {
    private final UserData userData;
    private final IcqEventsListener icqEventsListener;

    public MrimServiceImpl(IcqEventsListener icqEventsListener, UserData userData) {
        this.userData = userData;
        this.icqEventsListener = icqEventsListener;
    }

    @Override
    public boolean sendMessage(ApplicationUser user, String message) {
        if (user == null || StringUtils.isEmpty(message))
            throw new IllegalArgumentException("User and message must be specified");

        String mrimLogin = userData.getMrimLogin(user);
        if (user.isActive() && !StringUtils.isBlank(mrimLogin) && userData.isEnabled(user)) {
            sendMessage(mrimLogin, message);
            return true;
        }
        return false;
    }

    @Override
    public void sendMessage(String chatId, String message) {
        icqEventsListener.publishEvent(new JiraNotifyEvent(chatId, message, null));
    }
}
