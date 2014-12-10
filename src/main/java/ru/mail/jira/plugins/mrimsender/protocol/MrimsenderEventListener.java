package ru.mail.jira.plugins.mrimsender.protocol;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.issue.MentionIssueEvent;
import com.atlassian.jira.notification.NotificationRecipient;
import com.atlassian.jira.notification.NotificationSchemeManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.ApplicationUsers;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import ru.mail.jira.plugins.mrimsender.configuration.PluginData;
import ru.mail.jira.plugins.mrimsender.configuration.UserData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MrimsenderEventListener implements InitializingBean, DisposableBean {
    private static final Logger log = Logger.getLogger(MrimsenderEventListener.class);

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

    private void sendMessage(Collection<ApplicationUser> recipients, Object event) {
        if (!StringUtils.isEmpty(pluginData.getLogin()) && !StringUtils.isEmpty(pluginData.getPassword()))
            for (ApplicationUser recipient : recipients) {
                String mrimLogin = userData.getMrimLogin(recipient);
                if (recipient.isActive() && !StringUtils.isBlank(mrimLogin) && userData.isEnabled(recipient)) {
                    String message = null;
                    if (event instanceof IssueEvent)
                        message = new MessageFormatter(recipient).formatEvent((IssueEvent) event);
                    if (event instanceof MentionIssueEvent)
                        message = new MessageFormatter(recipient).formatEvent((MentionIssueEvent) event);
                    if (message != null)
                        MrimsenderThread.sendMessage(mrimLogin, message);
                }
            }
    }

    @SuppressWarnings("unused")
    @EventListener
    public void onIssueEvent(IssueEvent issueEvent) {
        try {
            if (issueEvent.isSendMail()) {
                List<ApplicationUser> recipients = new ArrayList<ApplicationUser>();
                for (NotificationRecipient notificationRecipient : notificationSchemeManager.getRecipients(issueEvent))
                    recipients.add(notificationRecipient.getUser());
                sendMessage(recipients, issueEvent);
            }
        } catch (Exception e) {
            log.error(e);
        }
    }

    @SuppressWarnings("unused")
    @EventListener
    public void onMentionIssueEvent(MentionIssueEvent mentionIssueEvent) {
        try {
            List<ApplicationUser> recipients = new ArrayList<ApplicationUser>();
            for (User directoryUser : mentionIssueEvent.getToUsers()) {
                ApplicationUser user = ApplicationUsers.from(directoryUser);
                if (!mentionIssueEvent.getCurrentRecipients().contains(new NotificationRecipient(user)))
                    recipients.add(user);
            }
            sendMessage(recipients, mentionIssueEvent);
        } catch (Exception e) {
            log.error(e);
        }
    }
}
