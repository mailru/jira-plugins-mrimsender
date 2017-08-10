package ru.mail.jira.plugins.calendar.service;

import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PluginDataImpl implements PluginData {
    private static final String PLUGIN_KEY = "ru.mail.jira.plugins.calendar";
    private final PluginSettings pluginSettings;

    @Autowired
    public PluginDataImpl(@ComponentImport PluginSettingsFactory pluginSettingsFactory) {
        this.pluginSettings = pluginSettingsFactory.createSettingsForKey(PLUGIN_KEY);
    }

    @Override
    public synchronized Long getReminderLastRun() {
        String stringVal = (String) this.pluginSettings.get("reminderLastRun");
        if (stringVal != null) {
            return Long.parseLong(stringVal);
        }
        return null;
    }

    @Override
    public synchronized void setReminderLastRun(long timestamp) {
        this.pluginSettings.put("reminderLastRun", String.valueOf(timestamp));
    }
}
