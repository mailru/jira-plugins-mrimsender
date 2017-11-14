package ru.mail.jira.plugins.mrimsender.protocol;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.I18nHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import ru.mail.jira.plugins.commons.CommonUtils;
import ru.mail.jira.plugins.mrimsender.configuration.PluginData;
import ru.mail.jira.plugins.mrimsender.protocol.packages.Worker;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class MrimsenderThread extends Thread {
    private static final int MAX_QUEUE_SIZE = 10000;
    private static final long MIN_SLEEP_INTERVAL = 30 * 1000;
    private static final long MAX_SLEEP_INTERVAL = 32 * 60 * 1000;

    private static final Logger log = Logger.getLogger(MrimsenderThread.class);
    private static MrimsenderThread instance;
    private volatile boolean doClose;
    private volatile boolean doLogin = true;
    private final BlockingDeque<Pair<String, String>> messageQueue = new LinkedBlockingDeque<Pair<String, String>>();
    private Worker worker;

    public static synchronized void startInstance() {
        if (instance == null) {
            instance = new MrimsenderThread();
            instance.start();
        }
    }

    public static synchronized void stopInstance() {
        if (instance != null) {
            instance.doClose = true;
            instance.interrupt();
            instance = null;
        }
    }

    public static synchronized void relogin() {
        log.info("MrimsenderThread is " + instance);
        if (instance != null) {
            instance.doLogin = true;
            instance.interrupt();
        } else
            startInstance();
    }

    public static synchronized void sendMessage(String email, String message) {
        if (instance != null) {
            if (instance.messageQueue.size() >= MAX_QUEUE_SIZE)
                instance.messageQueue.pollFirst();
            instance.messageQueue.addLast(new ImmutablePair<String, String>(email, message));
        }
    }

    private MrimsenderThread() {}

    @Override
    public void run() {
        while (!doClose)
            try {
                if (Thread.interrupted())
                    throw new InterruptedException();

                if (doLogin) {
                    doLogin = false;
                    resetWorker();
                }
                if (worker == null) {
                    sleep(60 * 1000);
                    continue;
                }

                worker.processAvailablePackages();

                Pair<String, String> message = messageQueue.pollFirst(1, TimeUnit.SECONDS);
                if (message != null) {
                    messageQueue.addFirst(message);
                    worker.sendMessage(message.getLeft(), message.getRight());
                    messageQueue.pollFirst();
                } else
                    worker.ping();
            } catch (InterruptedException e) {
                if (doClose)
                    break;
            } catch (Exception e) {
                log.error(e.getMessage(), e);

                // Try to recover the state
                sendEmail(false, 0);
                log.info("Trying to reconnect...");
                resetWorker();
                if (worker == null) {
                    try {
                        boolean reconnect = false;
                        long sleepInterval = MIN_SLEEP_INTERVAL;
                        while (!reconnect && sleepInterval < MAX_SLEEP_INTERVAL) {
                            sendEmail(false, (int) (sleepInterval / (60 * 1000)));
                            sleep(sleepInterval);
                            log.info("Trying to reconnect...");
                            resetWorker();
                            if (worker != null) {
                                reconnect = true;
                                log.info("Successfully connected!");
                            } else
                                sleepInterval = sleepInterval * 2;
                        }

                        if (sleepInterval > MAX_SLEEP_INTERVAL)
                            sendEmail(true, 0);
                    } catch (InterruptedException ignore) {
                    }
                } else
                    log.info("Successfully connected!");
            }
        closeWorker();
    }

    private void sendEmail(boolean unableToReconnectMessage, int reconnectInterval) {
        try {
            PluginData pluginData = ComponentAccessor.getOSGiComponentInstanceOfType(PluginData.class);
            for (String recipientKey : pluginData.getNotifiedUserKeys()) {
                ApplicationUser recipient = ComponentAccessor.getUserManager().getUserByKey(recipientKey);
                if (recipient == null)
                    throw new IllegalStateException(String.format("Recipient user is not found by key (%s)", recipientKey));

                I18nHelper i18nHelper = ComponentAccessor.getI18nHelperFactory().getInstance(recipient);
                String message = unableToReconnectMessage ? i18nHelper.getText("ru.mail.jira.plugins.mrimsender.email.unableToReconnect") : i18nHelper.getText("ru.mail.jira.plugins.mrimsender.email.unableToConnect", reconnectInterval);
                CommonUtils.sendEmail(recipient, i18nHelper.getText("ru.mail.jira.plugins.mrimsender.email.subject"), message);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void resetWorker() {
        try {
            closeWorker();

            PluginData pluginData = ComponentAccessor.getOSGiComponentInstanceOfType(PluginData.class);
            String host = pluginData.getHost();
            Integer port = pluginData.getPort();
            String login = pluginData.getLogin();
            String password = pluginData.getPassword();
            if (StringUtils.isEmpty(login) || StringUtils.isEmpty(password))
                return;

            worker = new Worker(host, port);
            if (!worker.login(login, password))
                throw new Exception(String.format("Unable to authenticate using specified login (%s) and password", login));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            closeWorker();
        }
    }

    private void closeWorker() {
        if (worker != null)
            try {
                worker.close();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            } finally {
                worker = null;
            }
    }
}
