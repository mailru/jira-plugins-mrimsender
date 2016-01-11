package ru.mail.jira.plugins.calendar.migrator;

import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import ru.mail.jira.plugins.calendar.model.Calendar;
import ru.mail.jira.plugins.calendar.service.CalendarEventService;
import ru.mail.jira.plugins.calendar.service.CalendarService;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class CalendarMigrator {
    private final static Logger log = LoggerFactory.getLogger(CalendarMigrator.class);

    private final String PLUGIN_KEY = "SimpleCalendar";
    private final String CALENDARS = "calendars";

    private final CalendarService calendarService;
    private final PluginSettingsFactory pluginSettingsFactory;
    private final UserManager userManager;

    public CalendarMigrator(CalendarService calendarService,
                            PluginSettingsFactory pluginSettingsFactory,
                            UserManager userManager) {
        this.calendarService = calendarService;
        this.pluginSettingsFactory = pluginSettingsFactory;
        this.userManager = userManager;
    }

    public synchronized Map<Long, Integer> migrateCalendars() throws Exception {
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

            MigratedCalendar oldCalendar = readCalendar(xml, dbf);

            try {
                ApplicationUser user = userManager.getUserByName(oldCalendar.author);
                if (user == null || !user.isActive()) {
                    pluginSettings.remove(calendarPluginSettgingKey);
                    iterator.remove();
                    continue;
                }

                String source = oldCalendar.isProject ? "project_" + oldCalendar.targetId : "filter_" + oldCalendar.targetId;
                String eventStart, eventEnd = null;

                if (oldCalendar.startField == null && oldCalendar.endField == null) {
                    eventStart = CalendarEventService.DUE_DATE_KEY;
                } else {
                    eventStart = oldCalendar.startField;
                    eventEnd = oldCalendar.endField;
                }

                StringBuilder shares = new StringBuilder();
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

                String color = getColors().get(random.nextInt(6));
                Calendar createdCalendar = calendarService.createCalendar(user, oldCalendar.name, source, color, eventStart, eventEnd, StringUtils.join(oldCalendar.extraFields, ","), shares.toString(), false, true);
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

        log.error("===== All errors during migrating calendars ======");
        log.error("Errors count => " + errors.size());
        log.error(errors.toString());
        log.error("===== End of errors ======");

        if (calendarIdList.size() == 0)
            pluginSettings.remove(CALENDARS);
        else
            pluginSettings.put(CALENDARS, StringUtils.join(calendarIdList, '&'));

        return result;
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
                            extraFields.add(CalendarService.STATUS);
                        } else if ("labels".equals(extraFieldName)) {
                            extraFields.add(CalendarService.LABELS);
                        } else if ("components".equals(extraFieldName)) {
                            extraFields.add(CalendarService.COMPONENTS);
                        } else if ("duedate".equals(extraFieldName)) {
                            extraFields.add(CalendarService.DUEDATE);
                        } else if ("environment".equals(extraFieldName)) {
                            extraFields.add(CalendarService.ENVIRONMENT);
                        } else if ("priority".equals(extraFieldName)) {
                            extraFields.add(CalendarService.PRIORITY);
                        } else if ("resolution".equals(extraFieldName)) {
                            extraFields.add(CalendarService.RESOLUTION);
                        } else if ("affect".equals(extraFieldName)) {
                            extraFields.add(CalendarService.AFFECT);
                        } else if ("created".equals(extraFieldName)) {
                            extraFields.add(CalendarService.CREATED);
                        } else if ("updated".equals(extraFieldName)) {
                            extraFields.add(CalendarService.UPDATED);
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
}
