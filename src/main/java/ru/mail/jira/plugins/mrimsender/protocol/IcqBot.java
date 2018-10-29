package ru.mail.jira.plugins.mrimsender.protocol;

import org.apache.log4j.Logger;
import ru.mail.jira.plugins.commons.HttpSender;
import ru.mail.jira.plugins.mrimsender.configuration.PluginData;

import java.io.IOException;
import java.net.URLEncoder;

public class IcqBot {
    private static final Logger log = Logger.getLogger(MrimsenderEventListener.class);

    private final PluginData pluginData;
    private String botToken;

    public IcqBot(PluginData pluginData) {
        this.pluginData = pluginData;
        initToken();
    }

    public void sendMessage(String mrimLogin, String message) {
        if(botToken == null)
            initToken();
        try {
            HttpSender httpSender = new HttpSender("https://botapi.icq.net/im/sendIM");
            httpSender.setHeader("Content-Type", "application/x-www-form-urlencoded");
            String result = URLEncoder.encode("aimsid", "UTF-8") +
                    "=" + URLEncoder.encode(botToken, "UTF-8") +
                    "&" +
                    URLEncoder.encode("t", "UTF-8") +
                    "=" + URLEncoder.encode(mrimLogin, "UTF-8") +
                    "&" +
                    URLEncoder.encode("message", "UTF-8") +
                    "=" + URLEncoder.encode(message, "UTF-8");
            httpSender.sendPost(result);
        } catch (IOException e) {
            log.error("sending to " + mrimLogin, e);
        }
    }

    public void initToken() {
        this.botToken = pluginData.getToken();
    }
}
