package ru.mail.jira.plugins.mrimsender.protocol;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.notification.NotificationRecipient;
import com.atlassian.jira.notification.NotificationSchemeManager;
import com.atlassian.jira.user.ApplicationUser;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import ru.mail.jira.plugins.mrimsender.configuration.PluginData;
import ru.mail.jira.plugins.mrimsender.configuration.UserData;

public class MrimsenderEventListener implements InitializingBean, DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(MrimsenderEventListener.class);

    private final EventPublisher eventPublisher;
    private final NotificationSchemeManager notificationSchemeManager;
    private final PluginData pluginData;
    private final UserData userData = new UserData();

    public MrimsenderEventListener(EventPublisher eventPublisher, NotificationSchemeManager notificationSchemeManager, PluginData pluginData) {
        this.eventPublisher = eventPublisher;
        this.notificationSchemeManager = notificationSchemeManager;
        this.pluginData = pluginData;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        MrimsenderThread.startInstance();
        eventPublisher.register(this);
    }
 
    @Override
    public void destroy() throws Exception {
        MrimsenderThread.stopInstance();
        eventPublisher.unregister(this);
    }

    @SuppressWarnings("unused")
    @EventListener
    public void onIssueEvent(IssueEvent issueEvent) {
        try {
            if (StringUtils.isEmpty(pluginData.getLogin()) || StringUtils.isEmpty(pluginData.getPassword()))
                return;

            for (NotificationRecipient notificationRecipient : notificationSchemeManager.getRecipients(issueEvent)) {
                ApplicationUser user = notificationRecipient.getUser();
                String mrimLogin = userData.getMrimLogin(user);
                if (user.isActive() && userData.isEnabled(user) && !StringUtils.isBlank(mrimLogin)) {
                    String message = new MessageFormatter(user).formatIssueEvent(issueEvent);
                    MrimsenderThread.sendMessage(mrimLogin, message);
                }
            }
        } catch (Exception e) {
            log.error("Unable to send Mail.Ru Agent notifications", e);
        }
    }
}
