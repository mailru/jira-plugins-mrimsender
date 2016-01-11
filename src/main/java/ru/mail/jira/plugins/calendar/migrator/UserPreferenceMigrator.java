package ru.mail.jira.plugins.calendar.migrator;

import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.security.roles.ProjectRole;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import ru.mail.jira.plugins.calendar.model.Calendar;
import ru.mail.jira.plugins.calendar.model.Share;
import ru.mail.jira.plugins.calendar.service.CalendarService;
import ru.mail.jira.plugins.calendar.service.UserDataService;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UserPreferenceMigrator {
    private final static Logger log = LoggerFactory.getLogger(UserPreferenceMigrator.class);

    private final String PLUGIN_KEY = "SimpleCalendar";

    private final CalendarService calendarService;
    private final UserDataService updateUserData;

    private final GlobalPermissionManager globalPermissionManager;
    private final GroupManager groupManager;
    private final PermissionManager permissionManager;
    private final PluginSettingsFactory pluginSettingsFactory;
    private final ProjectManager projectManager;
    private final ProjectRoleManager projectRoleManager;
    private final UserManager userManager;

    public UserPreferenceMigrator(CalendarService calendarService,
                                  UserDataService updateUserData,
                                  GlobalPermissionManager globalPermissionManager,
                                  GroupManager groupManager,
                                  PermissionManager permissionManager,
                                  PluginSettingsFactory pluginSettingsFactory,
                                  ProjectManager projectManager,
                                  ProjectRoleManager projectRoleManager,
                                  UserManager userManager) {
        this.calendarService = calendarService;
        this.updateUserData = updateUserData;
        this.globalPermissionManager = globalPermissionManager;
        this.groupManager = groupManager;
        this.permissionManager = permissionManager;
        this.pluginSettingsFactory = pluginSettingsFactory;
        this.projectManager = projectManager;
        this.projectRoleManager = projectRoleManager;
        this.userManager = userManager;
    }

    public synchronized void migrate(Map<Long, Integer> oldToNewCalendar) {
        if (oldToNewCalendar == null || oldToNewCalendar.isEmpty())
            return;

        PluginSettings pluginSettings = pluginSettingsFactory.createSettingsForKey(PLUGIN_KEY);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        log.info("Migrate user preferense");
        for (ApplicationUser user : userManager.getAllApplicationUsers()) {
            if (!user.isActive())
                continue;

            try {
                String xml = (String) pluginSettings.get(prefKey(user.getName()));
                log.info("user pref config => " + xml);

                if (StringUtils.isEmpty(xml)) {
                    pluginSettings.remove(prefKey(user.getName()));
                    continue;
                }

                MigratedUserPreferences oldUserPreferences = readUserPreferences(xml, dbf);
                final String userKey = user.getKey();
                final boolean isUserAdmin = globalPermissionManager.hasPermission(GlobalPermissionKey.ADMINISTER, user);

                List<Integer> showedCalendars = new ArrayList<Integer>();
                Set<Long> shadowCalendars = oldUserPreferences.shadowCalendars;
                for (long oldCalendarId : oldToNewCalendar.keySet()) {
                    if (shadowCalendars != null && shadowCalendars.contains(oldCalendarId))
                        continue;

                    Calendar calendar = calendarService.getCalendar(oldToNewCalendar.get(oldCalendarId));
                    if (calendar.getAuthorKey().equals(userKey)) {
                        showedCalendars.add(calendar.getID());
                    } else if (calendar.getShares() != null && calendar.getShares().length > 0) {
                        // because we don't want that admin will see all events from all shared calendar after updating.
                        if (isUserAdmin)
                            continue;

                        Share[] shares = calendar.getShares();
                        for (Share share : shares) {
                            if (share.getGroup() != null) {
                                Group group = groupManager.getGroup(share.getGroup());
                                if (group != null && groupManager.isUserInGroup(user, group)) {
                                    showedCalendars.add(calendar.getID());
                                    break;
                                }
                            } else {
                                Project project = projectManager.getProjectObj(share.getProject());
                                if (project != null) {
                                    if (share.getRole() != 0) {
                                        ProjectRole projectRole = projectRoleManager.getProjectRole(share.getRole());
                                        if (projectRole != null && projectRoleManager.isUserInProjectRole(user, projectRole, project)) {
                                            showedCalendars.add(calendar.getID());
                                            break;
                                        }
                                    } else {
                                        if (permissionManager.hasPermission(Permissions.BROWSE, project, user, false)) {
                                            showedCalendars.add(calendar.getID());
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                log.info("showedCalendars => " + showedCalendars);
                updateUserData.updateUserData(userKey, oldUserPreferences.defaultView, oldUserPreferences.hideWeekend, showedCalendars);
                pluginSettings.remove(prefKey(user.getName()));
            } catch (Exception e) {
                log.error("Error while trying to migrate user preferences for user => " + user.getName(), e);
            }
        }
    }

    private MigratedUserPreferences readUserPreferences(String xml, DocumentBuilderFactory dbf) throws Exception {
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document dom = db.parse(new ByteArrayInputStream(xml.getBytes("utf-8")));
        NodeList userPreferencesNodeList = dom.getDocumentElement().getChildNodes();

        MigratedUserPreferences result = new MigratedUserPreferences();
        for (int i = 0, n = userPreferencesNodeList.getLength(); i < n; i++) {
            Node item = userPreferencesNodeList.item(i);
            if ("defaultView".equals(item.getNodeName())) {
                if (item.getTextContent().equals("basicWeek") || item.getTextContent().equals("basicDay"))
                    result.defaultView = item.getTextContent();
                else
                    result.defaultView = "month";
            } else if ("hideWeekend".equals(item.getNodeName())) {
                result.hideWeekend = "true".equalsIgnoreCase(item.getTextContent());
            } else if ("shadowCalendars".equals(item.getNodeName())) {
                NodeList shadowCalendarsNodeList = item.getChildNodes();
                Set<Long> shadowCalendars = new HashSet<Long>();
                for (int j = 0, k = shadowCalendarsNodeList.getLength(); j < k; j++) {
                    Node shadowCalendarNode = shadowCalendarsNodeList.item(j);
                    if ("long".equals(shadowCalendarNode.getNodeName())) {
                        try {
                            shadowCalendars.add(Long.parseLong(shadowCalendarNode.getTextContent()));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
                result.shadowCalendars = shadowCalendars;
            }
        }
        return result;
    }

    private String prefKey(String user) {
        return (user + ".userpref");
    }

    class MigratedUserPreferences {
        String defaultView;
        boolean hideWeekend;
        Set<Long> shadowCalendars;

        @Override
        public String toString() {
            return "MigratedUserPreferences{" +
                    "defaultView='" + defaultView + '\'' +
                    ", hideWeekend=" + hideWeekend +
                    ", shadowCalendars=" + shadowCalendars +
                    '}';
        }
    }
}
