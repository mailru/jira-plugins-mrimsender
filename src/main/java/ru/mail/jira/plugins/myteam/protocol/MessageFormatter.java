/* (C)2020 */
package ru.mail.jira.plugins.myteam.protocol;

import com.atlassian.jira.bc.project.component.ProjectComponentManager;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.config.IssueTypeManager;
import com.atlassian.jira.config.LocaleManager;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.datetime.DateTimeStyle;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.issue.MentionIssueEvent;
import com.atlassian.jira.event.type.EventType;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueConstant;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.attachment.Attachment;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.issue.fields.*;
import com.atlassian.jira.issue.fields.screen.FieldScreen;
import com.atlassian.jira.issue.fields.screen.FieldScreenManager;
import com.atlassian.jira.issue.fields.screen.FieldScreenScheme;
import com.atlassian.jira.issue.fields.screen.issuetype.IssueTypeScreenSchemeManager;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.label.Label;
import com.atlassian.jira.issue.operation.IssueOperations;
import com.atlassian.jira.issue.priority.Priority;
import com.atlassian.jira.issue.resolution.Resolution;
import com.atlassian.jira.issue.security.IssueSecurityLevel;
import com.atlassian.jira.issue.security.IssueSecurityLevelManager;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectConstant;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.MessageSet;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.message.I18nResolver;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.GenericValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;

@Component
public class MessageFormatter {
  public static final int LIST_PAGE_SIZE = 15;
  public static final int COMMENT_LIST_PAGE_SIZE = 10;
  public static final int ADDITIONAL_FIELD_LIST_PAGE_SIZE = 30;
  public static final int ADDITIONAL_FIELD_ONE_COLUMN_MAX_COUNT = 5;
  public static final int ADDITIONAL_FIELD_TWO_COLUMN_MAX_COUNT = 15;
  public static final String DELIMITER_STR = "----------";

  private final ApplicationProperties applicationProperties;
  private final ConstantsManager constantsManager;
  private final DateTimeFormatter dateTimeFormatter;
  private final FieldManager fieldManager;
  private final IssueSecurityLevelManager issueSecurityLevelManager;
  private final I18nHelper i18nHelper;
  private final IssueTypeScreenSchemeManager issueTypeScreenSchemeManager;
  private final FieldScreenManager fieldScreenManager;
  private final I18nResolver i18nResolver;
  private final LocaleManager localeManager;
  private final String jiraBaseUrl;
  private final ProjectManager projectManager;
  private final IssueTypeManager issueTypeManager;
  private final GroupManager groupManager;
  private final ProjectComponentManager projectComponentManager;
  private final VersionManager versionManager;

  @Autowired
  public MessageFormatter(
      @ComponentImport ApplicationProperties applicationProperties,
      @ComponentImport ConstantsManager constantsManager,
      @ComponentImport DateTimeFormatter dateTimeFormatter,
      @ComponentImport FieldManager fieldManager,
      @ComponentImport IssueSecurityLevelManager issueSecurityLevelManager,
      @ComponentImport I18nHelper i18nHelper,
      @ComponentImport IssueTypeScreenSchemeManager issueTypeScreenSchemeManager,
      @ComponentImport FieldScreenManager fieldScreenManager,
      @ComponentImport I18nResolver i18nResolver,
      @ComponentImport LocaleManager localeManager,
      @ComponentImport ProjectManager projectManager,
      @ComponentImport IssueTypeManager issueTypeManager,
      @ComponentImport GroupManager groupManager,
      @ComponentImport ProjectComponentManager projectComponentManager,
      @ComponentImport VersionManager versionManager) {
    this.applicationProperties = applicationProperties;
    this.constantsManager = constantsManager;
    this.dateTimeFormatter = dateTimeFormatter;
    this.fieldManager = fieldManager;
    this.issueSecurityLevelManager = issueSecurityLevelManager;
    this.i18nHelper = i18nHelper;
    this.issueTypeScreenSchemeManager = issueTypeScreenSchemeManager;
    this.fieldScreenManager = fieldScreenManager;
    this.i18nResolver = i18nResolver;
    this.localeManager = localeManager;
    this.jiraBaseUrl = applicationProperties.getString(APKeys.JIRA_BASEURL);
    this.projectManager = projectManager;
    this.issueTypeManager = issueTypeManager;
    this.groupManager = groupManager;
    this.projectComponentManager = projectComponentManager;
    this.versionManager = versionManager;
  }

  private String formatUser(ApplicationUser user, String messageKey, boolean mention) {
    if (user != null) {
      if (mention) {
        return "@\\[" + user.getEmailAddress() + "\\]";
      }
      return user.getDisplayName() + " (" + user.getEmailAddress() + ")";
    } else return i18nHelper.getText(messageKey);
  }

  private String formatPriority(Priority priority) {
    if (priority != null
        && !priority.getId().equals(constantsManager.getDefaultPriorityObject().getId()))
      return priority.getNameTranslation(i18nHelper);
    else return null;
  }

  private void appendField(StringBuilder sb, String title, String value, boolean appendEmpty) {
    if (appendEmpty || !StringUtils.isBlank(value)) {
      if (sb.length() == 0) sb.append("\n");
      sb.append("\n").append(title).append(": ").append(StringUtils.defaultString(value));
    }
  }

  private void appendField(StringBuilder sb, String title, Collection<?> collection) {
    if (collection != null) {
      StringBuilder value = new StringBuilder();
      Iterator<?> iterator = collection.iterator();
      while (iterator.hasNext()) {
        Object object = iterator.next();
        if (object instanceof ProjectConstant) value.append(((ProjectConstant) object).getName());
        if (object instanceof Attachment) {
          Attachment attachment = (Attachment) object;
          String attachmentUrl =
              String.format(
                  "%s/secure/attachment/%d/%s",
                  jiraBaseUrl, attachment.getId(), attachment.getFilename());
          value.append(attachment.getFilename());
          value.append(" - ");
          value.append(attachmentUrl);
        }
        if (object instanceof Label) value.append(((Label) object).getLabel());
        if (iterator.hasNext()) value.append(", ");
      }
      appendField(sb, title, value.toString(), false);
    }
  }

  public String formatSystemFields(
      ApplicationUser recipient, Issue issue, boolean useMentionFormat) {
    StringBuilder sb = new StringBuilder();

    if (issue.getIssueType() != null)
      appendField(
          sb,
          i18nHelper.getText("issue.field.issuetype"),
          issue.getIssueType().getNameTranslation(i18nHelper),
          false);

    appendField(sb, i18nHelper.getText("issue.field.affectsversions"), issue.getAffectedVersions());
    appendField(
        sb,
        i18nHelper.getText("issue.field.assignee"),
        formatUser(issue.getAssignee(), "common.concepts.unassigned", useMentionFormat),
        false);

    appendField(sb, i18nHelper.getText("issue.field.components"), issue.getComponents());

    if (issue.getCreated() != null)
      appendField(
          sb,
          i18nHelper.getText("issue.field.created"),
          dateTimeFormatter
              .forUser(recipient)
              .withStyle(DateTimeStyle.COMPLETE)
              .format(issue.getCreated()),
          false);

    if (issue.getDueDate() != null)
      appendField(
          sb,
          i18nHelper.getText("issue.field.duedate"),
          dateTimeFormatter
              .forUser(recipient)
              .withSystemZone()
              .withStyle(DateTimeStyle.DATE)
              .format(issue.getDueDate()),
          false);

    appendField(sb, i18nHelper.getText("issue.field.environment"), issue.getEnvironment(), false);
    appendField(sb, i18nHelper.getText("issue.field.fixversions"), issue.getFixVersions());
    appendField(sb, i18nHelper.getText("issue.field.labels"), issue.getLabels());
    appendField(
        sb, i18nHelper.getText("issue.field.priority"), formatPriority(issue.getPriority()), false);
    appendField(
        sb,
        i18nHelper.getText("issue.field.reporter"),
        formatUser(issue.getReporter(), "common.concepts.no.reporter", useMentionFormat),
        false);

    if (issue.getSecurityLevelId() != null) {
      IssueSecurityLevel issueSecurityLevel =
          issueSecurityLevelManager.getSecurityLevel(issue.getSecurityLevelId());
      String value = issueSecurityLevel.getName();
      if (!StringUtils.isBlank(issueSecurityLevel.getDescription()))
        value += " " + issueSecurityLevel.getDescription();
      appendField(sb, i18nHelper.getText("issue.field.securitylevel"), value, false);
    }
    appendField(sb, i18nHelper.getText("issue.field.attachment"), issue.getAttachments());

    if (!StringUtils.isBlank(issue.getDescription()))
      sb.append("\n\n").append(issue.getDescription());

    return sb.toString();
  }

  private String formatChangeLog(GenericValue changeLog, boolean ignoreAssigneeField) {
    StringBuilder sb = new StringBuilder();
    if (changeLog != null)
      try {
        String changedDescription = null;

        for (GenericValue changeItem : changeLog.getRelated("ChildChangeItem")) {
          String field = StringUtils.defaultString(changeItem.getString("field"));
          String newString = StringUtils.defaultString(changeItem.getString("newstring"));

          if ("description".equals(field)) {
            changedDescription = newString;
            continue;
          }
          if ("WorklogTimeSpent".equals(field)
              || "WorklogId".equals(field)
              || "assignee".equals(field) && ignoreAssigneeField) continue;

          String title = field;
          if (!"custom".equalsIgnoreCase(changeItem.getString("fieldtype")))
            title = i18nHelper.getText("issue.field." + field.replaceAll(" ", "").toLowerCase());

          if (("Fix Version".equals(field) || "Component".equals(field) || "Version".equals(field))
              && changeItem.get("oldvalue") != null
              && changeItem.get("newvalue") == null) {
            newString = changeItem.getString("oldstring");
            title = i18nHelper.getText("ru.mail.jira.plugins.myteam.notification.deleted", title);
          }

          if (fieldManager.isNavigableField(field)) {
            final NavigableField navigableField = fieldManager.getNavigableField(field);
            if (navigableField != null)
              newString = navigableField.prettyPrintChangeHistory(newString, i18nHelper);
          }

          appendField(sb, title, newString, true);
        }

        if (!StringUtils.isBlank(changedDescription)) sb.append("\n\n").append(changedDescription);
      } catch (GenericEntityException ignored) {
      }
    return sb.toString();
  }

  public String createIssueLink(Issue issue) {
    return String.format(
        "%s/browse/%s", applicationProperties.getString(APKeys.JIRA_BASEURL), issue.getKey());
  }

  private String markdownTextLink(String issueKey, String issueLink) {
    if (issueKey != null) {
      return "[" + issueKey + "](" + issueLink + ")";
    } else return issueLink;
  }

  public String formatEvent(ApplicationUser recipient, IssueEvent issueEvent) {
    Issue issue = issueEvent.getIssue();
    ApplicationUser user = issueEvent.getUser();
    String issueLink = markdownTextLink(issue.getKey(), createIssueLink(issue));

    StringBuilder sb = new StringBuilder();

    boolean useMentionFormat = !recipient.equals(user);
    Long eventTypeId = issueEvent.getEventTypeId();
    if (EventType.ISSUE_CREATED_ID.equals(eventTypeId)) {
      sb.append(
          i18nHelper.getText(
              "ru.mail.jira.plugins.myteam.notification.created",
              formatUser(user, "common.words.anonymous", useMentionFormat),
              issueLink));
    } else if (EventType.ISSUE_UPDATED_ID.equals(eventTypeId)
        || EventType.ISSUE_COMMENT_DELETED_ID.equals(eventTypeId)
        || EventType.ISSUE_GENERICEVENT_ID.equals(eventTypeId)) {
      sb.append(
          i18nHelper.getText(
              "ru.mail.jira.plugins.myteam.notification.updated",
              formatUser(user, "common.words.anonymous", useMentionFormat),
              issueLink));
    } else if (EventType.ISSUE_ASSIGNED_ID.equals(eventTypeId)) {
      sb.append(
          i18nHelper.getText(
              "ru.mail.jira.plugins.myteam.notification.assigned",
              formatUser(user, "common.words.anonymous", useMentionFormat),
              issueLink,
              formatUser(issue.getAssignee(), "common.concepts.unassigned", useMentionFormat)));
    } else if (EventType.ISSUE_RESOLVED_ID.equals(eventTypeId)) {
      Resolution resolution = issue.getResolution();
      sb.append(
          i18nHelper.getText(
              "ru.mail.jira.plugins.myteam.notification.resolved",
              formatUser(user, "common.words.anonymous", useMentionFormat),
              issueLink,
              resolution != null
                  ? resolution.getNameTranslation(i18nHelper)
                  : i18nHelper.getText("common.resolution.unresolved")));
    } else if (EventType.ISSUE_CLOSED_ID.equals(eventTypeId)) {
      Resolution resolution = issue.getResolution();
      sb.append(
          i18nHelper.getText(
              "ru.mail.jira.plugins.myteam.notification.closed",
              formatUser(user, "common.words.anonymous", useMentionFormat),
              issueLink,
              resolution != null
                  ? resolution.getNameTranslation(i18nHelper)
                  : i18nHelper.getText("common.resolution.unresolved")));
    } else if (EventType.ISSUE_COMMENTED_ID.equals(eventTypeId)) {
      sb.append(
          i18nHelper.getText(
              "ru.mail.jira.plugins.myteam.notification.commented",
              formatUser(user, "common.words.anonymous", useMentionFormat),
              issueLink));
    } else if (EventType.ISSUE_COMMENT_EDITED_ID.equals(eventTypeId)) {
      sb.append(
          i18nHelper.getText(
              "ru.mail.jira.plugins.myteam.notification.commentEdited",
              formatUser(user, "common.words.anonymous", useMentionFormat),
              issueLink));
    } else if (EventType.ISSUE_REOPENED_ID.equals(eventTypeId)) {
      sb.append(
          i18nHelper.getText(
              "ru.mail.jira.plugins.myteam.notification.reopened",
              formatUser(user, "common.words.anonymous", useMentionFormat),
              issueLink));
    } else if (EventType.ISSUE_DELETED_ID.equals(eventTypeId)) {
      sb.append(
          i18nHelper.getText(
              "ru.mail.jira.plugins.myteam.notification.deleted",
              formatUser(user, "common.words.anonymous", useMentionFormat),
              issueLink));
    } else if (EventType.ISSUE_MOVED_ID.equals(eventTypeId)) {
      sb.append(
          i18nHelper.getText(
              "ru.mail.jira.plugins.myteam.notification.moved",
              formatUser(user, "common.words.anonymous", useMentionFormat),
              issueLink));
    } else if (EventType.ISSUE_WORKLOGGED_ID.equals(eventTypeId)) {
      sb.append(
          i18nHelper.getText(
              "ru.mail.jira.plugins.myteam.notification.worklogged",
              formatUser(user, "common.words.anonymous", useMentionFormat),
              issueLink));
    } else if (EventType.ISSUE_WORKSTARTED_ID.equals(eventTypeId)) {
      sb.append(
          i18nHelper.getText(
              "ru.mail.jira.plugins.myteam.notification.workStarted",
              formatUser(user, "common.words.anonymous", useMentionFormat),
              issueLink));
    } else if (EventType.ISSUE_WORKSTOPPED_ID.equals(eventTypeId)) {
      sb.append(
          i18nHelper.getText(
              "ru.mail.jira.plugins.myteam.notification.workStopped",
              formatUser(user, "common.words.anonymous", useMentionFormat),
              issueLink));
    } else if (EventType.ISSUE_WORKLOG_UPDATED_ID.equals(eventTypeId)) {
      sb.append(
          i18nHelper.getText(
              "ru.mail.jira.plugins.myteam.notification.worklogUpdated",
              formatUser(user, "common.words.anonymous", useMentionFormat),
              issueLink));
    } else if (EventType.ISSUE_WORKLOG_DELETED_ID.equals(eventTypeId)) {
      sb.append(
          i18nHelper.getText(
              "ru.mail.jira.plugins.myteam.notification.worklogDeleted",
              formatUser(user, "common.words.anonymous", useMentionFormat),
              issueLink));
    } else {
      sb.append(
          i18nHelper.getText(
              "ru.mail.jira.plugins.myteam.notification.updated",
              formatUser(user, "common.words.anonymous", useMentionFormat),
              issueLink));
    }

    sb.append("\n").append(issue.getSummary());

    if (issueEvent.getWorklog() != null
        && !StringUtils.isBlank(issueEvent.getWorklog().getComment()))
      sb.append("\n\n").append(issueEvent.getWorklog().getComment());

    if (EventType.ISSUE_CREATED_ID.equals(eventTypeId))
      sb.append(formatSystemFields(recipient, issue, useMentionFormat));

    sb.append(
        formatChangeLog(
            issueEvent.getChangeLog(), EventType.ISSUE_ASSIGNED_ID.equals(eventTypeId)));

    if (issueEvent.getComment() != null && !StringUtils.isBlank(issueEvent.getComment().getBody()))
      sb.append("\n\n").append(issueEvent.getComment().getBody());

    return sb.toString();
  }

  public String formatEvent(MentionIssueEvent mentionIssueEvent) {
    Issue issue = mentionIssueEvent.getIssue();
    ApplicationUser user = mentionIssueEvent.getFromUser();
    String issueLink = markdownTextLink(issue.getKey(), createIssueLink(issue));

    StringBuilder sb = new StringBuilder();
    sb.append(
        i18nHelper.getText(
            "ru.mail.jira.plugins.myteam.notification.mentioned",
            formatUser(user, "common.words.anonymous", true),
            issueLink));
    sb.append("\n").append(issue.getSummary());

    if (!StringUtils.isBlank(mentionIssueEvent.getMentionText()))
      sb.append("\n\n").append(mentionIssueEvent.getMentionText());

    return sb.toString();
  }

  public String createIssueSummary(Issue issue, ApplicationUser user) {
    StringBuilder sb = new StringBuilder();
    sb.append(markdownTextLink(issue.getKey(), createIssueLink(issue))).append('\n');
    // append status field because it doesn't exist in formatSystemFields string
    appendField(
        sb,
        i18nHelper.getText(fieldManager.getField(IssueFieldConstants.STATUS).getNameKey()),
        issue.getStatus().getNameTranslation(i18nHelper),
        false);

    sb.append(formatSystemFields(user, issue, true));
    FieldScreenScheme fieldScreenScheme = issueTypeScreenSchemeManager.getFieldScreenScheme(issue);
    FieldScreen fieldScreen =
        fieldScreenScheme.getFieldScreen(IssueOperations.VIEW_ISSUE_OPERATION);

    fieldScreenManager
        .getFieldScreenTabs(fieldScreen)
        .forEach(
            tab ->
                fieldScreenManager
                    .getFieldScreenLayoutItems(tab)
                    .forEach(
                        fieldScreenLayoutItem -> {
                          Field field = fieldManager.getField(fieldScreenLayoutItem.getFieldId());
                          if (fieldManager.isCustomField(field)
                              && !fieldManager.isFieldHidden(user, field)) {
                            CustomField customField = (CustomField) field;
                            if (customField.isShown(issue))
                              appendField(
                                  sb,
                                  customField.getFieldName(),
                                  customField.getValueFromIssue(issue),
                                  false);
                          }
                        }));

    return sb.toString();
  }

  public List<List<InlineKeyboardMarkupButton>> getAllIssueButtons(
      String issueKey, ApplicationUser recipient) {
    Locale recipientLocale = localeManager.getLocaleFor(recipient);

    List<List<InlineKeyboardMarkupButton>> buttons = new ArrayList<>();
    List<InlineKeyboardMarkupButton> buttonsRow = new ArrayList<>();
    buttons.add(buttonsRow);

    InlineKeyboardMarkupButton issueInfo = new InlineKeyboardMarkupButton();
    issueInfo.setText(
        i18nResolver.getRawText(
            recipientLocale,
            "ru.mail.jira.plugins.myteam.mrimsenderEventListener.quickViewButton.text"));
    issueInfo.setCallbackData(String.join("-", "view", issueKey));
    buttonsRow.add(issueInfo);

    InlineKeyboardMarkupButton comment = new InlineKeyboardMarkupButton();
    comment.setText(
        i18nResolver.getRawText(
            recipientLocale,
            "ru.mail.jira.plugins.myteam.mrimsenderEventListener.commentButton.text"));
    comment.setCallbackData(String.join("-", "comment", issueKey));
    buttonsRow.add(comment);

    InlineKeyboardMarkupButton showMenuButton =
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            i18nResolver.getText(
                recipientLocale, "ru.mail.jira.plugins.myteam.messageQueueProcessor.mainMenu.text"),
            "showMenu");
    addRowWithButton(buttons, showMenuButton);

    return buttons;
  }

  public List<List<InlineKeyboardMarkupButton>> getIssueButtons(
      String issueKey, ApplicationUser recipient) {
    List<List<InlineKeyboardMarkupButton>> buttons = new ArrayList<>();
    List<InlineKeyboardMarkupButton> buttonsRow = new ArrayList<>();
    buttons.add(buttonsRow);

    buttonsRow.add(
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            i18nResolver.getText(
                localeManager.getLocaleFor(recipient),
                "ru.mail.jira.plugins.myteam.mrimsenderEventListener.commentButton.text"),
            String.join("-", "comment", issueKey)));

    buttonsRow.add(
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            i18nResolver.getText(
                localeManager.getLocaleFor(recipient),
                "ru.mail.jira.plugins.myteam.mrimsenderEventListener.showCommentsButton.text"),
            String.join("-", "showComments", issueKey)));

    return buttons;
  }

  public List<List<InlineKeyboardMarkupButton>> getIssueCreationConfirmButtons(
      ApplicationUser recipient) {
    List<List<InlineKeyboardMarkupButton>> buttons = new ArrayList<>();
    List<InlineKeyboardMarkupButton> buttonsRow = new ArrayList<>();
    buttons.add(buttonsRow);

    buttonsRow.add(
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            i18nResolver.getText(
                localeManager.getLocaleFor(recipient),
                "ru.mail.jira.plugins.myteam.mrimsenderEventListener.issueCreationConfirmButton.text"),
            "confirmIssueCreation"));

    buttonsRow.add(
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            i18nResolver.getText(
                localeManager.getLocaleFor(recipient),
                "ru.mail.jira.plugins.myteam.mrimsenderEventListener.issueAddExtraFieldsButton.text"),
            "addExtraIssueFields"));
    return buildButtonsWithCancel(
        buttons,
        i18nResolver.getText(
            localeManager.getLocaleFor(recipient),
            "ru.mail.jira.plugins.myteam.myteamEventsListener.cancelIssueCreationButton.text"));
  }

  public List<List<InlineKeyboardMarkupButton>> getCancelButton(Locale locale) {
    List<List<InlineKeyboardMarkupButton>> buttons = new ArrayList<>();

    buttons.add(
        getCancelButtonRow(
            i18nResolver.getRawText(
                locale, "ru.mail.jira.plugins.myteam.mrimsenderEventListener.cancelButton.text")));

    return buttons;
  }

  private void addRowWithButton(
      List<List<InlineKeyboardMarkupButton>> buttons, InlineKeyboardMarkupButton button) {
    List<InlineKeyboardMarkupButton> newButtonsRow = new ArrayList<>(1);
    newButtonsRow.add(button);
    buttons.add(newButtonsRow);
  }

  public List<List<InlineKeyboardMarkupButton>> getMenuButtons(ApplicationUser currentUser) {
    Locale locale = localeManager.getLocaleFor(currentUser);
    List<List<InlineKeyboardMarkupButton>> buttons = new ArrayList<>();

    // create 'search issue' button
    InlineKeyboardMarkupButton showIssueButton =
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            i18nResolver.getRawText(
                locale,
                "ru.mail.jira.plugins.myteam.messageFormatter.mainMenu.showIssueButton.text"),
            "showIssue");
    addRowWithButton(buttons, showIssueButton);

    // create 'Active issues assigned to me' button
    InlineKeyboardMarkupButton activeAssignedIssuesButton =
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            i18nResolver.getRawText(
                locale,
                "ru.mail.jira.plugins.myteam.messageFormatter.mainMenu.activeIssuesAssignedToMeButton.text"),
            "activeIssuesAssigned");
    addRowWithButton(buttons, activeAssignedIssuesButton);

    // create 'Active issues i watching' button
    InlineKeyboardMarkupButton activeWatchingIssuesButton =
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            i18nResolver.getRawText(
                locale,
                "ru.mail.jira.plugins.myteam.messageFormatter.mainMenu.activeIssuesWatchingByMeButton.text"),
            "activeIssuesWatching");
    addRowWithButton(buttons, activeWatchingIssuesButton);

    // create 'Active issues crated by me' button
    InlineKeyboardMarkupButton activeCreatedIssuesButton =
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            i18nResolver.getRawText(
                locale,
                "ru.mail.jira.plugins.myteam.messageFormatter.mainMenu.activeIssuesCreatedByMeButton.text"),
            "activeIssuesCreated");
    addRowWithButton(buttons, activeCreatedIssuesButton);

    // create 'Search issue by JQL' button
    InlineKeyboardMarkupButton searchIssueByJqlButton =
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            i18nResolver.getRawText(
                locale,
                "ru.mail.jira.plugins.myteam.messageFormatter.mainMenu.searchIssueByJqlButton.text"),
            "searchByJql");
    addRowWithButton(buttons, searchIssueByJqlButton);

    // create 'create issue' button
    InlineKeyboardMarkupButton createIssueButton =
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            i18nResolver.getRawText(
                locale,
                "ru.mail.jira.plugins.myteam.messageFormatter.mainMenu.createIssueButton.text"),
            "createIssue");
    addRowWithButton(buttons, createIssueButton);
    return buttons;
  }

  private List<List<InlineKeyboardMarkupButton>> getListButtons(
      Locale locale,
      boolean withPrev,
      boolean withNext,
      String prevButtonData,
      String nextButtonData) {
    if (!withPrev && !withNext) return null;
    List<List<InlineKeyboardMarkupButton>> buttons = new ArrayList<>(1);
    List<InlineKeyboardMarkupButton> newButtonsRow = new ArrayList<>();
    if (withPrev) {
      newButtonsRow.add(
          InlineKeyboardMarkupButton.buildButtonWithoutUrl(
              i18nResolver.getRawText(
                  locale,
                  "ru.mail.jira.plugins.myteam.messageFormatter.listButtons.prevPageButton.text"),
              prevButtonData));
    }
    if (withNext) {
      newButtonsRow.add(
          InlineKeyboardMarkupButton.buildButtonWithoutUrl(
              i18nResolver.getRawText(
                  locale,
                  "ru.mail.jira.plugins.myteam.messageFormatter.listButtons.nextPageButton.text"),
              nextButtonData));
    }
    buttons.add(newButtonsRow);
    return buttons;
  }

  public List<List<InlineKeyboardMarkupButton>> getIssueListButtons(
      Locale locale, boolean withPrev, boolean withNext) {
    return getListButtons(locale, withPrev, withNext, "prevIssueListPage", "nextIssueListPage");
  }

  public String stringifyMap(Map<?, ?> map) {
    if (map == null) return "";
    return map.entrySet().stream()
        .map(
            ((entry) -> String.join(" : ", entry.getKey().toString(), entry.getValue().toString())))
        .collect(Collectors.joining("\n"));
  }

  public String stringifyCollection(Locale locale, Collection<?> collection) {
    StringJoiner sj = new StringJoiner("\n");

    // stringify collection
    collection.forEach(obj -> sj.add(obj.toString()));
    return sj.toString();
  }

  public String stringifyPagedCollection(
      Locale locale, Collection<?> collection, int pageNumber, int total, int pageSize) {
    if (collection.size() == 0) return "";

    StringJoiner sj = new StringJoiner("\n");

    // stringify collection
    collection.forEach(obj -> sj.add(obj.toString()));

    // append string with current (and total) page number info
    if ((pageNumber + 1) * pageSize < total) {
      int firstResultPageIndex = pageNumber * pageSize + 1;
      int lastResultPageIndex = firstResultPageIndex + collection.size() - 1;
      sj.add(DELIMITER_STR);
      sj.add(
          i18nResolver.getText(
              locale,
              "pager.results.displayissues.short",
              String.join(
                  " - ",
                  Integer.toString(firstResultPageIndex),
                  Integer.toString(lastResultPageIndex)),
              Integer.toString(total)));
    }

    return sj.toString();
  }

  public String stringifyIssueList(
      Locale locale, List<Issue> issueList, int pageNumber, int total) {
    return stringifyPagedCollection(
        locale,
        issueList.stream()
            .map(
                issue ->
                    String.join(
                        "",
                        "["
                            + issue.getKey()
                            + "]("
                            + String.format("%s/browse/%s", jiraBaseUrl, issue.getKey())
                            + ") ",
                        issue.getSummary()))
            .collect(Collectors.toList()),
        pageNumber,
        total,
        LIST_PAGE_SIZE);
  }

  public String stringifyIssueCommentsList(
      Locale locale, List<Comment> commentList, int pageNumber, int total) {
    SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    return stringifyPagedCollection(
        locale,
        commentList.stream()
            .map(
                comment ->
                    String.join(
                        "",
                        "[",
                        dateFormatter.format(comment.getCreated()),
                        "] ",
                        "[",
                        comment.getAuthorFullName(),
                        "] ",
                        comment.getBody()))
            .collect(Collectors.toList()),
        pageNumber,
        total,
        COMMENT_LIST_PAGE_SIZE);
  }

  public String stringifyJqlClauseErrorsMap(MessageSet messageSet, Locale locale) {
    StringJoiner joiner = new StringJoiner("\n");
    String errorsTitle = i18nResolver.getRawText(locale, "common.words.errors") + ":";
    joiner.add(errorsTitle);
    messageSet.getErrorMessages().forEach(joiner::add);
    return joiner.toString();
  }

  public String createSelectProjectMessage(
      Locale locale, List<Project> visibleProjects, int pageNumber, int totalProjectsNum) {
    StringJoiner sj = new StringJoiner("\n");
    sj.add(
        i18nResolver.getRawText(
            locale,
            "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.selectProject.message"));
    sj.add(DELIMITER_STR);
    List<String> formattedProjectList =
        visibleProjects.stream()
            .map(proj -> String.join("", "[", proj.getKey(), "] ", proj.getName()))
            .collect(Collectors.toList());
    sj.add(
        stringifyPagedCollection(
            locale, formattedProjectList, pageNumber, totalProjectsNum, LIST_PAGE_SIZE));
    return sj.toString();
  }

  public List<List<InlineKeyboardMarkupButton>> getSelectProjectMessageButtons(
      Locale locale, boolean withPrev, boolean withNext) {
    return getListButtons(locale, withPrev, withNext, "prevProjectListPage", "nextProjectListPage");
  }

  public List<List<InlineKeyboardMarkupButton>> getSelectAdditionalFieldMessageButtons(
      Locale locale, boolean withPrev, boolean withNext, List<Field> fields) {
    List<List<InlineKeyboardMarkupButton>> buttons =
        getListButtons(
            locale,
            withPrev,
            withNext,
            "prevAdditionalFieldListPage",
            "nextAdditionalFieldListPage");

    int colCount =
        fields.size() <= ADDITIONAL_FIELD_ONE_COLUMN_MAX_COUNT
            ? 1
            : (fields.size() <= ADDITIONAL_FIELD_TWO_COLUMN_MAX_COUNT ? 2 : 3);

    List<List<InlineKeyboardMarkupButton>> fieldButtons = new ArrayList<>();
    List<InlineKeyboardMarkupButton> fieldButtonRow = new ArrayList<>();

    int i = 0;
    while (i < fields.size()) {
      Field field = fields.get(i);
      fieldButtonRow.add(
          InlineKeyboardMarkupButton.buildButtonWithoutUrl(
              field.getName(), String.join("-", "selectAdditionalField", field.getId())));

      if (i % colCount == 0) {
        fieldButtons.add(fieldButtonRow);
        fieldButtonRow = new ArrayList<>();
      }
      i++;
    }

    String cancelTitle =
        i18nResolver.getRawText(
            locale, "ru.mail.jira.plugins.myteam.mrimsenderEventListener.cancelButton.text");

    if (buttons == null || buttons.size() == 0) { // if no pager buttons
      fieldButtons.add(getCancelButtonRow(cancelTitle));
      return fieldButtons;
    } else {
      buttons.get(0).add(InlineKeyboardMarkupButton.buildButtonWithoutUrl(cancelTitle, "cancel"));
      buttons.addAll(0, fieldButtons);
    }

    return buttons;
  }

  public List<List<InlineKeyboardMarkupButton>> getViewCommentsButtons(
      Locale locale, boolean withPrev, boolean withNext) {
    return getListButtons(
        locale, withPrev, withNext, "prevIssueCommentsListPage", "nextIssueCommentsListPage");
  }

  public String getSelectIssueTypeMessage(Locale locale) {
    return i18nResolver.getRawText(
        locale, "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.selectIssueType.message");
  }

  public List<List<InlineKeyboardMarkupButton>> buildIssueTypesButtons(
      Collection<IssueType> issueTypes) {
    List<List<InlineKeyboardMarkupButton>> buttons = new ArrayList<>();
    issueTypes.forEach(
        issueType -> {
          InlineKeyboardMarkupButton issueTypeButton =
              InlineKeyboardMarkupButton.buildButtonWithoutUrl(
                  issueType.getNameTranslation(i18nHelper),
                  String.join("-", "selectIssueType", issueType.getId()));
          addRowWithButton(buttons, issueTypeButton);
        });
    return buttons;
  }

  public List<List<InlineKeyboardMarkupButton>> buildPrioritiesButtons(
      Collection<Priority> priorities) {
    List<List<InlineKeyboardMarkupButton>> buttons = new ArrayList<>();
    priorities.forEach(
        priority -> {
          InlineKeyboardMarkupButton issueTypeButton =
              InlineKeyboardMarkupButton.buildButtonWithoutUrl(
                  priority.getNameTranslation(i18nHelper),
                  String.join("-", "selectIssueButtonValue", priority.getName()));
          addRowWithButton(buttons, issueTypeButton);
        });
    return buttons;
  }

  public List<List<InlineKeyboardMarkupButton>> buildButtonsWithCancel(
      List<List<InlineKeyboardMarkupButton>> buttons, String cancelButtonText) {
    if (buttons == null) {
      List<List<InlineKeyboardMarkupButton>> newButtons = new ArrayList<>();
      newButtons.add(getCancelButtonRow(cancelButtonText));
      return newButtons;
    }
    buttons.add(getCancelButtonRow(cancelButtonText));
    return buttons;
  }

  public String formatIssueCreationDto(Locale locale, IssueCreationDto issueCreationDto) {
    StringJoiner sj = new StringJoiner("\n");

    sj.add(DELIMITER_STR);
    sj.add(
        i18nResolver.getRawText(
            locale,
            "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.currentIssueCreationDtoState"));
    sj.add(
        String.join(
            " ",
            i18nResolver.getRawText(locale, "Project:"),
            projectManager.getProjectObj(issueCreationDto.getProjectId()).getName()));
    sj.add(
        String.join(
            " ",
            i18nResolver.getRawText(locale, "IssueType:"),
            issueTypeManager
                .getIssueType(issueCreationDto.getIssueTypeId())
                .getNameTranslation(locale.toString())));
    issueCreationDto
        .getRequiredIssueCreationFieldValues()
        .forEach(
            (field, value) ->
                sj.add(
                    String.join(
                        " : ",
                        i18nResolver.getRawText(locale, field.getNameKey()),
                        value.isEmpty() ? "-" : value)));
    return sj.toString();
  }

  public String stringifyFieldsCollection(Locale locale, Collection<? extends Field> fields) {
    return String.join(
        "\n",
        fields.stream()
            .map(field -> i18nResolver.getRawText(locale, field.getNameKey()))
            .collect(Collectors.toList()));
  }

  public String createInsertFieldMessage(
      Locale locale, Field field, IssueCreationDto issueCreationDto) {
    if (isArrayLikeField(field)) {
      return String.join(
          "\n",
          i18nResolver.getText(
              locale,
              "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.insertIssueField.arrayMessage",
              i18nResolver.getRawText(locale, field.getNameKey()).toLowerCase(locale)),
          this.formatIssueCreationDto(locale, issueCreationDto));
    }
    return String.join(
        "\n",
        i18nResolver.getText(
            locale,
            "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.insertIssueField.message",
            i18nResolver.getRawText(locale, field.getNameKey()).toLowerCase(locale)),
        this.formatIssueCreationDto(locale, issueCreationDto));
  }

  private List<InlineKeyboardMarkupButton> getCancelButtonRow(String title) {
    List<InlineKeyboardMarkupButton> buttonsRow = new ArrayList<>();
    buttonsRow.add(InlineKeyboardMarkupButton.buildButtonWithoutUrl(title, "cancel"));
    return buttonsRow;
  }

  private boolean isArrayLikeField(Field field) {
    switch (field.getId()) {
      case IssueFieldConstants.FIX_FOR_VERSIONS:
      case IssueFieldConstants.COMPONENTS:
      case IssueFieldConstants.AFFECTED_VERSIONS:
      case IssueFieldConstants.ISSUE_LINKS:
      case IssueFieldConstants.LABELS:
      case IssueFieldConstants.VOTES:
        // never shown on issue creation screen
      case IssueFieldConstants.WATCHES:
        return true;
    }
    return false;
  }

  private String[] mapStringToArrayFieldValue(Long projectId, Field field, String fieldValue) {
    List<String> fieldValues =
        Arrays.stream(fieldValue.split(",")).map(String::trim).collect(Collectors.toList());

    switch (field.getId()) {
      case IssueFieldConstants.FIX_FOR_VERSIONS:
      case IssueFieldConstants.AFFECTED_VERSIONS:
        return fieldValues.stream()
            .map(strValue -> versionManager.getVersion(projectId, strValue))
            .filter(Objects::nonNull)
            .map(version -> version.getId().toString())
            .toArray(String[]::new);
      case IssueFieldConstants.COMPONENTS:
        return fieldValues.stream()
            .map(
                strValue ->
                    Optional.ofNullable(
                            projectComponentManager.findByComponentName(projectId, strValue))
                        .map(projectComponent -> projectComponent.getId().toString())
                        .orElse(null))
            .toArray(String[]::new);

      case IssueFieldConstants.ISSUE_LINKS:
        IssueLinksSystemField issueLinksSystemField = (IssueLinksSystemField) field;
        // hmmm....  well to parse input strings to IssueLinksSystemField.IssueFieldValue we should
        // strict user input format
        break;
      case IssueFieldConstants.LABELS:
        // TODO find existing labels via some labelManager or labelSearchers,
        //  right now label search methods, without issue parameter, don't exist
        /*return fieldValues.stream()
        .map(strValue -> labelManager.getSuggestedLabels())
        .filter(Objects::nonNull)
        .map(label -> label.getId().toString())
        .toArray(String[]::new);*/
        return fieldValues.toArray(new String[0]);
    }
    return fieldValues.toArray(new String[0]);
  }

  private String[] mapStringToSingleFieldValue(Long projectId, Field field, String fieldValue) {
    // no preprocessing for description field needed
    if (field.getId().equals(IssueFieldConstants.DESCRIPTION)) return new String[] {fieldValue};

    List<String> fieldValues =
        Arrays.stream(fieldValue.split(",")).map(String::trim).collect(Collectors.toList());

    // this field list was made based on information of which fields implements
    // AbstractOrderableField.getRelevantParams method
    switch (field.getId()) {
      case IssueFieldConstants.ASSIGNEE:
        // no additional mapping needed
        break;
      case IssueFieldConstants.ATTACHMENT:
        // not supported right now
        return new String[0];
      case IssueFieldConstants.COMMENT:
        // TODO internally uses some additional map keys for mapping comment level
        //  and comment editing/creating/removing
        break;
      case IssueFieldConstants.DUE_DATE:
        // no additional mapping needed ???
        // TODO maybe inserted user input should be mapped additionally to jira internal date format
        break;
      case IssueFieldConstants.PRIORITY:
        if (!fieldValues.isEmpty()) {
          String priorityStrValue = fieldValues.get(0);
          String selectedPriorityId =
              constantsManager.getPriorities().stream()
                  .filter(
                      priority ->
                          priority.getName().equals(priorityStrValue)
                              || priority.getNameTranslation(i18nHelper).equals(priorityStrValue))
                  .findFirst()
                  .map(IssueConstant::getId)
                  .orElse("");
          return new String[] {selectedPriorityId};
        }
        break;
      case IssueFieldConstants.REPORTER:
        // no additional mapping needed
        break;
      case IssueFieldConstants.RESOLUTION:
        if (!fieldValues.isEmpty()) {
          String resolutionStrValue = fieldValues.get(0);
          String selectedResolutionId =
              constantsManager.getResolutions().stream()
                  .filter(
                      resolution ->
                          resolution.getName().equals(resolutionStrValue)
                              || resolution
                                  .getNameTranslation(i18nHelper)
                                  .equals(resolutionStrValue))
                  .findFirst()
                  .map(IssueConstant::getId)
                  .orElse("");
          return new String[] {selectedResolutionId};
        }
        break;
      case IssueFieldConstants.SECURITY:
        if (!fieldValues.isEmpty()) {
          String issueSecurityLevelName = fieldValues.get(0);
          String selectedResolutionId =
              issueSecurityLevelManager.getIssueSecurityLevelsByName(issueSecurityLevelName)
                  .stream()
                  .findFirst()
                  .map(securityLevel -> Long.toString(securityLevel.getId()))
                  .orElse("");
          return new String[] {selectedResolutionId};
        }
        break;
      case IssueFieldConstants.TIMETRACKING:
        // TODO internally uses some additional map keys for mapping timetracking
        break;
      case IssueFieldConstants.WORKLOG:
        // TODO should we map this ???
        break;
    }
    return fieldValues.toArray(new String[0]);
  }

  public String[] mapUserInputStringToFieldValue(Long projectId, Field field, String fieldValue) {
    if (isArrayLikeField(field)) {
      return mapStringToArrayFieldValue(projectId, field, fieldValue);
    }
    return mapStringToSingleFieldValue(projectId, field, fieldValue);
  }
}
