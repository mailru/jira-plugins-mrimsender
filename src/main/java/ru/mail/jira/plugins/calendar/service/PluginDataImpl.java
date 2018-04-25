package ru.mail.jira.plugins.calendar.service;

import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.commons.CommonUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    @Override
    public synchronized List<Integer> getWorkingDays() {
        String workingDays = (String) this.pluginSettings.get("workingDays");
        if (workingDays == null)
            return new ArrayList<Integer>(Arrays.asList(1, 2, 3, 4, 5));
        if (StringUtils.isBlank(workingDays))
            return new ArrayList<>();
        List<Integer> result = new ArrayList<>();
        for (String day : CommonUtils.split(workingDays))
            result.add(Integer.parseInt(day));
        return result;
    }

    @Override
    public synchronized void setWorkingDays(String workingDays) {
        this.pluginSettings.put("workingDays", workingDays);
    }
}
