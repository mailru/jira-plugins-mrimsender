package ru.mail.jira.plugins.calendar.upgrades.v2;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.external.ModelVersion;
import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.security.roles.ProjectRole;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.ApplicationUsers;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.transaction.TransactionCallback;
import net.java.ao.Query;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import ru.mail.jira.plugins.calendar.model.Calendar;
import ru.mail.jira.plugins.calendar.model.Permission;
import ru.mail.jira.plugins.calendar.model.UserCalendar;
import ru.mail.jira.plugins.calendar.model.UserData;
import ru.mail.jira.plugins.calendar.model.archive.Share;
import ru.mail.jira.plugins.calendar.service.CalendarEventService;
import ru.mail.jira.plugins.calendar.service.CalendarServiceImpl;

import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings({"deprecation"})
public class Version2UpgradeTask implements ActiveObjectsUpgradeTask {
    private final static Logger log = LoggerFactory.getLogger(Version2UpgradeTask.class);

    private final static String PLUGIN_KEY_VERSION_1 = "SimpleCalendar";
    private final static String CALENDARS = "calendars";
    private static final String CALENDARS_HAVE_BEEN_MIGRATED_KEY = "chbm";
    private static final String PLUGIN_KEY = "ru.mail.jira.plugins.mailrucal";

    private final static Pattern GROUP_PATTERN = Pattern.compile("group=(.*)");
    private final static Pattern PROJECT_ROLE_PATTERN = Pattern.compile("project=(\\d*)( role=(\\d*))?");

    private final GlobalPermissionManager globalPermissionManager;
    private final GroupManager groupManager;
    private final PermissionManager permissionManager;
    private final PluginSettingsFactory pluginSettingsFactory;
    private final ProjectManager projectManager;
    private final ProjectRoleManager projectRoleManager;
    private final UserManager userManager;

    public Version2UpgradeTask(GlobalPermissionManager globalPermissionManager, GroupManager groupManager, PermissionManager permissionManager, PluginSettingsFactory pluginSettingsFactory, ProjectManager projectManager, ProjectRoleManager projectRoleManager, UserManager userManager) {
        this.globalPermissionManager = globalPermissionManager;
        this.groupManager = groupManager;
        this.permissionManager = permissionManager;
        this.pluginSettingsFactory = pluginSettingsFactory;
        this.projectManager = projectManager;
        this.projectRoleManager = projectRoleManager;
        this.userManager = userManager;
    }

    @Override
    public ModelVersion getModelVersion() {
        return ModelVersion.valueOf("2");
    }

    @Override
    public void upgrade(ModelVersion currentVersion, final ActiveObjects ao) {
        log.info("Current version " + currentVersion.toString());
        if (currentVersion.isOlderThan(getModelVersion())) {
            ao.migrate(Share.class, Calendar.class, Permission.class, UserCalendar.class, UserData.class);
            log.info("Run upgrade task to version 2");
            try {
                PluginSettings pluginSettings = pluginSettingsFactory.createSettingsForKey(PLUGIN_KEY_VERSION_1);
                if (pluginSettings.get(CALENDARS_HAVE_BEEN_MIGRATED_KEY) == null) {
                    log.info("Calendar have not been migrated to version 2");

                    Map<Long, Integer> oldToNewCalendarIds = migrateCalendars(ao);
                    migrateUserData(ao, oldToNewCalendarIds);

                    pluginSettings.put(CALENDARS_HAVE_BEEN_MIGRATED_KEY, "migrated");
                } else
                    log.info("Calendar have been migrated to version 2 earlier");
            } catch (Exception e) {
                log.error("Error while trying to check old calendars", e);
            }
        }
    }

    private Map<Long, Integer> migrateCalendars(final ActiveObjects ao) throws Exception {
        log.info("migrate calendars");
        Random random = new Random();
        Map<Long, Integer> result = new HashMap<Long, Integer>();
        PluginSettings pluginSettings = pluginSettingsFactory.createSettingsForKey(PLUGIN_KEY);
        List<Long> calendarIdList = getCalendarIdList();

        List<String> errors = new ArrayList<String>();

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Iterator<Long> iterator = calendarIdList.iterator();
        while (iterator.hasNext()) {
            Long oldCalendarId = iterator.next();
            String calendarPluginSettgingKey = calKey(oldCalendarId);
            String xml = (String) pluginSettings.get(calendarPluginSettgingKey);

            if (StringUtils.isEmpty(xml)) {
                pluginSettings.remove(calendarPluginSettgingKey);
                iterator.remove();
                continue;
            }

            final MigratedCalendar oldCalendar = readCalendar(xml, dbf);

            try {
                final ApplicationUser user = userManager.getUserByName(oldCalendar.author);
                if (user == null || !user.isActive()) {
                    pluginSettings.remove(calendarPluginSettgingKey);
                    iterator.remove();
                    continue;
                }

                final String source = oldCalendar.isProject ? "project_" + oldCalendar.targetId : "filter_" + oldCalendar.targetId;
                final String eventStart, eventEnd;

                if (oldCalendar.startField == null && oldCalendar.endField == null) {
                    eventStart = CalendarEventService.DUE_DATE_KEY;
                    eventEnd = null;
                } else {
                    eventStart = oldCalendar.startField;
                    eventEnd = oldCalendar.endField;
                }

                final StringBuilder shares = new StringBuilder();
                for (String sharedGroup : oldCalendar.sharedGroups)
                    shares.append("group=").append(sharedGroup).append(';');

                for (Pair<Long, Long> projectRole : oldCalendar.projectRoles) {
                    shares.append("project=").append(projectRole.getLeft());
                    if (projectRole.getRight() != null && projectRole.getRight() > 0)
                        shares.append(" role=").append(projectRole.getRight());
                    shares.append(';');
                }

                if (shares.length() > 0)
                    shares.deleteCharAt(shares.length() - 1);

                final String color = getColors().get(random.nextInt(6));
                Calendar createdCalendar = ao.executeInTransaction(new TransactionCallback<Calendar>() {
                    @Override
                    public Calendar doInTransaction() {
                        //                        validateCalendar(user, name, source, color, eventStart, displayedFields, shares, true, fromMigration);
                        final List<LocalShare> calendarShares = getLocalShares(user, shares.toString());
                        Calendar calendar = ao.create(Calendar.class);
                        calendar.setAuthorKey(user.getKey());
                        calendar.setName(oldCalendar.name);
                        calendar.setSource(source);
                        calendar.setColor(color);
                        calendar.setEventStart(eventStart);
                        calendar.setEventEnd(eventEnd);
                        calendar.setDisplayedFields(StringUtils.join(oldCalendar.extraFields, ","));
                        calendar.save();

                        for (LocalShare localShare : calendarShares) {
                            Share share = ao.create(Share.class);
                            if (localShare.groupName != null)
                                share.setGroup(localShare.groupName);
                            else {
                                share.setProject(localShare.projectId);
                                if (localShare.roleId != null)
                                    share.setRole(localShare.roleId);
                            }
                            share.setCalendar(calendar);
                            share.save();
                        }
                        UserData[] userDatas = ao.find(UserData.class, Query.select().where("USER_KEY = ?", user.getKey()));
                        UserData userData = userDatas.length > 0 ? userDatas[0] : null;
                        if (userData != null) {
                            if (StringUtils.isNotEmpty(userData.getShowedCalendars())) {
                                List<String> calendarIds = new ArrayList<String>(Arrays.asList(userData.getShowedCalendars().split(";")));
                                String calendarIdStr = String.valueOf(calendar.getID());
                                if (!calendarIds.contains(calendarIdStr))
                                    calendarIds.add(calendarIdStr);

                                List<String> showedCalendars = new ArrayList<String>(calendarIds);
                                Iterator<String> iterator = showedCalendars.iterator();
                                while (iterator.hasNext())
                                    if (ao.get(Calendar.class, Integer.parseInt(iterator.next())) == null)
                                        iterator.remove();

                                userData.setShowedCalendars(StringUtils.join(showedCalendars, ";"));
                            } else
                                userData.setShowedCalendars(String.valueOf(calendar.getID()));
                        } else {
                            userData = ao.create(UserData.class);
                            userData.setHideWeekends(false);
                            userData.setShowTime(false);
                            userData.setShowedCalendars(String.valueOf(calendar.getID()));
                            userData.setUserKey(user.getKey());
                            userData.setICalUid(UUID.randomUUID().toString().substring(0, 8));
                        }
                        userData.save();
                        return calendar;
                    }
                });
                //calendarService.createCalendar(user, oldCalendar.name, source, color, eventStart, eventEnd, StringUtils.join(oldCalendar.extraFields, ","), shares.toString(), false, true);
                log.info("Create calenar => " + oldCalendar.name);
                result.put(oldCalendarId, createdCalendar.getID());
                pluginSettings.remove(calendarPluginSettgingKey);
                iterator.remove();
            } catch (Exception e) {
                log.error("Error while trying to migrate calendar with id => " + xml, e);
                errors.add(String.format("Error => %s, oldCalendarId => %s", e.getMessage(), oldCalendarId) + e.getMessage());
            }
        }

        log.info("Calendars count with errors => " + calendarIdList.size());
        if (!errors.isEmpty()) {
            log.error("===== All errors during migrating calendars ======");
            log.error("Errors count => " + errors.size());
            log.error(errors.toString());
            log.error("===== End of errors ======");
        }
        if (calendarIdList.size() == 0)
            pluginSettings.remove(CALENDARS);
        else
            pluginSettings.put(CALENDARS, StringUtils.join(calendarIdList, '&'));

        return result;
    }

    private List<LocalShare> getLocalShares(ApplicationUser user, String shares) {
        final List<LocalShare> calendarShares = new ArrayList<LocalShare>();
        if (StringUtils.isNotBlank(shares)) {
            Set<String> uniqueShares = new HashSet<String>();
            for (String shareExpr : shares.split(";")) {
                if (!uniqueShares.add(shareExpr))
                    continue;

                LocalShare localShare;
                if ((localShare = getGroupFromExpr(shareExpr)) != null)
                    calendarShares.add(localShare);
                else if ((localShare = getProjectRoleFromExpr(user, shareExpr)) != null)
                    calendarShares.add(localShare);
            }
        }
        return calendarShares;
    }

    @Nullable
    private LocalShare getGroupFromExpr(String expr) {
        Matcher matcher = GROUP_PATTERN.matcher(expr);
        if (matcher.matches()) {
            String groupName = matcher.group(1);
            if (!groupManager.groupExists(groupName))
                throw new IllegalArgumentException(String.format("Group with name => %s doesn't exist", groupName));
            return new LocalShare(groupName);
        }
        return null;
    }

    @Nullable
    private LocalShare getProjectRoleFromExpr(ApplicationUser user, String expr) {
        Matcher matcher = PROJECT_ROLE_PATTERN.matcher(expr);
        if (matcher.matches()) {
            Long projectId = Long.parseLong(matcher.group(1));

            Project project = projectManager.getProjectObj(projectId);
            if (project == null)
                throw new IllegalArgumentException("Bad project id in shares => " + projectId);
            if (!permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, project, user, false))
                throw new IllegalArgumentException("No permission to view project in shares => " + project.getName());

            String roleIdStr = matcher.group(3);
            if (StringUtils.isNotEmpty(roleIdStr)) {
                long roleId = Long.parseLong(roleIdStr);
                ProjectRole projectRole = projectRoleManager.getProjectRole(roleId);
                if (projectRole == null)
                    throw new IllegalArgumentException("Can not found project role with id => " + roleIdStr);
                return new LocalShare(projectId, roleId);
            }
            return new LocalShare(projectId, null);
        }
        return null;
    }

    private List<String> getColors() {
        return Arrays.asList("#5dab3e", "#d7ad43", "#3e6894", "#c9dad8", "#588e87", "#e18434",
                             "#83382A", "#D04A32", "#3C2B28", "#87A4C0", "#A89B95");
    }

    private List<Long> getCalendarIdList() {
        PluginSettings pluginSettings = pluginSettingsFactory.createSettingsForKey(PLUGIN_KEY);
        Object calendarsSetting = pluginSettings.get(CALENDARS);
        return calendarsSetting == null ? new ArrayList<Long>(0) : strToListLongs((String) calendarsSetting);
    }

    private MigratedCalendar readCalendar(String xml, DocumentBuilderFactory dbf) throws Exception {
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document dom = db.parse(new ByteArrayInputStream(xml.getBytes("utf-8")));
        NodeList calendarNodeList = dom.getDocumentElement().getChildNodes();

        MigratedCalendar migratedCalendar = new MigratedCalendar();
        for (int i = 0, n = calendarNodeList.getLength(); i < n; i++) {
            Node item = calendarNodeList.item(i);
            if ("creator".equals(item.getNodeName())) {
                migratedCalendar.author = item.getTextContent().trim();
            } else if ("name".equals(item.getNodeName())) {
                migratedCalendar.name = item.getTextContent().trim();
            } else if ("type".equals(item.getNodeName())) {
                migratedCalendar.isProject = item.getTextContent().trim().equals("0");
            } else if ("target".equals(item.getNodeName())) {
                migratedCalendar.targetId = Long.parseLong(item.getTextContent().trim());
            } else if ("color".equals(item.getNodeName())) {
                migratedCalendar.color = item.getTextContent().trim();
            } else if ("groups".equals(item.getNodeName())) {
                NodeList childNodes = item.getChildNodes();
                List<String> sharedGroups = new ArrayList<String>();
                for (int j = 0, k = childNodes.getLength(); j < k; j++) {
                    Node sharedGroupItem = childNodes.item(j);
                    if ("string".equals(sharedGroupItem.getNodeName()))
                        sharedGroups.add(sharedGroupItem.getTextContent());
                }
                migratedCalendar.sharedGroups = sharedGroups;
            } else if ("projRoles".equals(item.getNodeName())) {
                List<Pair<Long, Long>> projectRoles = new ArrayList<Pair<Long, Long>>();
                NodeList projectRoleNodes = item.getChildNodes();
                for (int j = 0, k = projectRoleNodes.getLength(); j < k; j++) {
                    Node projectRoleNode = projectRoleNodes.item(j);
                    if (projectRoleNode.getNodeName().endsWith("ProjRole")) {
                        NodeList projectRoleFields = projectRoleNode.getChildNodes();
                        long projectId = 0;
                        long roleId = 0;
                        for (int l = 0, m = projectRoleFields.getLength(); l < m; l++) {
                            Node projectRoleField = projectRoleFields.item(l);
                            if ("project".equals(projectRoleField.getNodeName())) {
                                projectId = Long.parseLong(projectRoleField.getTextContent().trim());
                            } else if ("role".equals(projectRoleField.getNodeName()) &&
                                    StringUtils.isNotBlank(projectRoleField.getTextContent())) {
                                roleId = Long.parseLong(projectRoleField.getTextContent().trim());
                            }
                        }
                        if (projectId > 0)
                            projectRoles.add(Pair.of(projectId, roleId));
                    }
                }
                migratedCalendar.projectRoles = projectRoles;
            } else if ("startPoint".equals(item.getNodeName()) && StringUtils.isNotBlank(item.getTextContent())) {
                migratedCalendar.startField = item.getTextContent();
            } else if ("endPoint".equals(item.getNodeName()) && StringUtils.isNotBlank(item.getTextContent())) {
                migratedCalendar.endField = item.getTextContent();
            } else if ("fields".equals(item.getNodeName())) {
                List<String> extraFields = new ArrayList<String>();
                NodeList extraFieldsNodeList = item.getChildNodes();
                for (int j = 0, k = extraFieldsNodeList.getLength(); j < k; j++) {
                    Node extraField = extraFieldsNodeList.item(j);
                    if ("string".equals(extraField.getNodeName()) && StringUtils.isNotBlank(extraField.getTextContent())) {
                        String extraFieldName = extraField.getTextContent();
                        if ("issuestatus".equals(extraFieldName)) {
                            extraFields.add(CalendarServiceImpl.STATUS);
                        } else if ("labels".equals(extraFieldName)) {
                            extraFields.add(CalendarServiceImpl.LABELS);
                        } else if ("components".equals(extraFieldName)) {
                            extraFields.add(CalendarServiceImpl.COMPONENTS);
                        } else if ("duedate".equals(extraFieldName)) {
                            extraFields.add(CalendarServiceImpl.DUEDATE);
                        } else if ("environment".equals(extraFieldName)) {
                            extraFields.add(CalendarServiceImpl.ENVIRONMENT);
                        } else if ("priority".equals(extraFieldName)) {
                            extraFields.add(CalendarServiceImpl.PRIORITY);
                        } else if ("resolution".equals(extraFieldName)) {
                            extraFields.add(CalendarServiceImpl.RESOLUTION);
                        } else if ("affect".equals(extraFieldName)) {
                            extraFields.add(CalendarServiceImpl.AFFECT);
                        } else if ("created".equals(extraFieldName)) {
                            extraFields.add(CalendarServiceImpl.CREATED);
                        } else if ("updated".equals(extraFieldName)) {
                            extraFields.add(CalendarServiceImpl.UPDATED);
                        }
                    }
                }
                migratedCalendar.extraFields = extraFields;
            }
        }
        return migratedCalendar;
    }

    private String calKey(Long calId) {
        return (calId + ".data");
    }

    public static List<Long> strToListLongs(String source) {
        List<Long> list = new ArrayList<Long>();
        if (StringUtils.isEmpty(source))
            return list;

        for (String str : source.split("&")) {
            try {
                list.add(Long.valueOf(str));
            } catch (NumberFormatException ignored) {
                //ignore
            }
        }
        return list;
    }

    static class LocalShare {
        String groupName;
        Long projectId;
        Long roleId;

        public LocalShare(String groupName) {
            this.groupName = groupName;
        }

        public LocalShare(Long projectId, Long roleId) {
            this.projectId = projectId;
            this.roleId = roleId;
        }
    }

    class MigratedCalendar {
        String author;
        String name;
        List<String> sharedGroups;
        List<Pair<Long, Long>> projectRoles;
        boolean isProject;
        long targetId;
        String color;
        String startField;
        String endField;
        List<String> extraFields;

        @Override
        public String toString() {
            return "MigratedCalendar{" +
                    "author='" + author + '\'' +
                    ", name='" + name + '\'' +
                    ", sharedGroups=" + sharedGroups +
                    ", projectRoles=" + projectRoles +
                    ", isProject=" + isProject +
                    ", targetId=" + targetId +
                    ", color='" + color + '\'' +
                    ", startField='" + startField + '\'' +
                    ", endField='" + endField + '\'' +
                    ", extraFields=" + extraFields +
                    '}';
        }
    }

    private void migrateUserData(final ActiveObjects ao, final Map<Long, Integer> oldToNewCalendar) {
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

                final MigratedUserPreferences oldUserPreferences = readUserPreferences(xml, dbf);
                final String userKey = user.getKey();
                final boolean isUserAdmin = globalPermissionManager.hasPermission(GlobalPermissionKey.ADMINISTER, user);

                final List<Integer> showedCalendars = new ArrayList<Integer>();
                Set<Long> shadowCalendars = oldUserPreferences.shadowCalendars;
                for (final long oldCalendarId : oldToNewCalendar.keySet()) {
                    if (shadowCalendars != null && shadowCalendars.contains(oldCalendarId))
                        continue;

                    Calendar calendar = ao.executeInTransaction(new TransactionCallback<Calendar>() {
                        @Override
                        public Calendar doInTransaction() {
                            return ao.get(Calendar.class, oldToNewCalendar.get(oldCalendarId));
                        }
                    });
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
                                if (group != null && groupManager.isUserInGroup(ApplicationUsers.toDirectoryUser(user), group)) {
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
                                        if (permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, project, user, false)) {
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
                ao.executeInTransaction(new TransactionCallback<Void>() {
                    @Override
                    public Void doInTransaction() {
                        UserData[] userDatas = ao.find(UserData.class, Query.select().where("USER_KEY = ?", userKey));
                        UserData userData;
                        if (userDatas.length == 0) {
                            userData = ao.create(UserData.class);
                            userData.setUserKey(userKey);
                            userData.setHideWeekends(false);
                            userData.save();
                        } else
                            userData = userDatas[0];

                        if (userData.getIcalUid() == null)
                            userData.setICalUid(UUID.randomUUID().toString().substring(0, 8));
                        if (oldUserPreferences.defaultView != null)
                            userData.setDefaultView(oldUserPreferences.defaultView);
                        userData.setHideWeekends(oldUserPreferences.hideWeekend);
                        if (showedCalendars.size() > 0) {
                            if (userData.getShowedCalendars() == null)
                                userData.setShowedCalendars(StringUtils.join(showedCalendars, ";"));
                            else {
                                String[] split = StringUtils.split(userData.getShowedCalendars(), ';');
                                List<Integer> showedCalendarIds = new ArrayList<Integer>();

                                for (String showedCalendarIdStr : Arrays.asList(split))
                                    showedCalendarIds.add(Integer.parseInt(showedCalendarIdStr));

                                for (Integer extraCalendarId : showedCalendars) {
                                    if (!showedCalendarIds.contains(extraCalendarId))
                                        showedCalendarIds.add(extraCalendarId);
                                }
                                userData.setShowedCalendars(StringUtils.join(showedCalendarIds, ";"));
                            }
                        }
                        userData.save();
                        return null;
                    }
                });
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
