package ru.mail.jira.plugins.calendar.configuration;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.calendar.service.PluginData;

import java.util.Date;
import java.util.List;

@Component
public class WorkingDaysService {
    private final ActiveObjects ao;
    private final PluginData pluginData;

    @Autowired
    public WorkingDaysService(@ComponentImport ActiveObjects ao, PluginData pluginData) {
        this.ao = ao;
        this.pluginData = pluginData;
    }

    public List<Integer> getWorkingDays() {
        return pluginData.getWorkingDays();
    }

    public void setWorkingDays(String workingDays) {
        pluginData.setWorkingDays(workingDays);
    }

    public NonWorkingDay[] getNonWorkingDays() {
        return ao.find(NonWorkingDay.class);
    }

    public NonWorkingDay addNonWorkingDay(Date date, String description) {
        NonWorkingDay nonWorkingDay = ao.create(NonWorkingDay.class);
        nonWorkingDay.setDate(date);
        nonWorkingDay.setDescription(description);
        nonWorkingDay.save();
        return nonWorkingDay;
    }

    public void deleteNonWorkingDay(int id) {
        ao.delete(ao.get(NonWorkingDay.class, id));
    }
}
