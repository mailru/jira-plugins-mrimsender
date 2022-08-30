/* (C)2020 */
package ru.mail.jira.plugins.myteam.component;

import static ru.mail.jira.plugins.myteam.commons.Utils.shieldText;

import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.datetime.DateTimeStyle;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.issue.MentionIssueEvent;
import com.atlassian.jira.event.type.EventType;
import com.atlassian.jira.issue.AttachmentManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.attachment.Attachment;
import com.atlassian.jira.issue.fields.*;
import com.atlassian.jira.issue.fields.screen.FieldScreen;
import com.atlassian.jira.issue.fields.screen.FieldScreenManager;
import com.atlassian.jira.issue.fields.screen.FieldScreenScheme;
import com.atlassian.jira.issue.fields.screen.issuetype.IssueTypeScreenSchemeManager;
import com.atlassian.jira.issue.label.Label;
import com.atlassian.jira.issue.operation.IssueOperations;
import com.atlassian.jira.issue.priority.Priority;
import com.atlassian.jira.issue.resolution.Resolution;
import com.atlassian.jira.issue.security.IssueSecurityLevel;
import com.atlassian.jira.issue.security.IssueSecurityLevelManager;
import com.atlassian.jira.project.ProjectConstant;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.message.I18nResolver;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.ofbiz.core.entity.GenericValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.commons.SentryClient;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.ButtonRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.CommandRuleType;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;
import ru.mail.jira.plugins.myteam.myteam.dto.User;
import ru.mail.jira.plugins.myteam.service.PluginData;

@Slf4j
@Component
public class MessageFormatter {
  public static final int LIST_PAGE_SIZE = 15;
  public static final String DELIMITER_STR = "----------";

  private final ApplicationProperties applicationProperties;
  private final DateTimeFormatter dateTimeFormatter;
  private final FieldManager fieldManager;
  private final IssueSecurityLevelManager issueSecurityLevelManager;
  private final I18nHelper i18nHelper;
  private final IssueTypeScreenSchemeManager issueTypeScreenSchemeManager;
  private final FieldScreenManager fieldScreenManager;
  private final I18nResolver i18nResolver;
  private final String jiraBaseUrl;
  private final UserManager userManager;
  private final AttachmentManager attachmentManager;
  private final PluginData pluginData;

  @Autowired
  public MessageFormatter(
      @ComponentImport ApplicationProperties applicationProperties,
      @ComponentImport DateTimeFormatter dateTimeFormatter,
      @ComponentImport FieldManager fieldManager,
      @ComponentImport IssueSecurityLevelManager issueSecurityLevelManager,
      @ComponentImport I18nHelper i18nHelper,
      @ComponentImport IssueTypeScreenSchemeManager issueTypeScreenSchemeManager,
      @ComponentImport FieldScreenManager fieldScreenManager,
      @ComponentImport I18nResolver i18nResolver,
      @ComponentImport UserManager userManager,
      @ComponentImport AttachmentManager attachmentManager,
      PluginData pluginData) {
    this.applicationProperties = applicationProperties;
    this.dateTimeFormatter = dateTimeFormatter;
    this.fieldManager = fieldManager;
    this.issueSecurityLevelManager = issueSecurityLevelManager;
    this.i18nHelper = i18nHelper;
    this.issueTypeScreenSchemeManager = issueTypeScreenSchemeManager;
    this.fieldScreenManager = fieldScreenManager;
    this.i18nResolver = i18nResolver;
    this.jiraBaseUrl = applicationProperties.getString(APKeys.JIRA_BASEURL);
    this.userManager = userManager;
    this.attachmentManager = attachmentManager;
    this.pluginData = pluginData;
  }

  public static void addRowWithButton(
      List<List<InlineKeyboardMarkupButton>> buttons, InlineKeyboardMarkupButton button) {
    List<InlineKeyboardMarkupButton> newButtonsRow = new ArrayList<>(1);
    newButtonsRow.add(button);
    buttons.add(newButtonsRow);
  }

  public static List<InlineKeyboardMarkupButton> getCancelButtonRow(String title) {
    List<InlineKeyboardMarkupButton> buttonsRow = new ArrayList<>();
    buttonsRow.add(
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(title, ButtonRuleType.Cancel.getName()));
    return buttonsRow;
  }

  public static List<InlineKeyboardMarkupButton> getBackButtonRow(String title) {
    List<InlineKeyboardMarkupButton> buttonsRow = new ArrayList<>();
    buttonsRow.add(
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(title, ButtonRuleType.Revert.getName()));
    return buttonsRow;
  }

  public static List<List<InlineKeyboardMarkupButton>> buildButtonsWithCancel(
      List<List<InlineKeyboardMarkupButton>> buttons, String cancelButtonText) {
    if (buttons == null) {
      List<List<InlineKeyboardMarkupButton>> newButtons = new ArrayList<>();
      newButtons.add(getCancelButtonRow(cancelButtonText));
      return newButtons;
    }
    buttons.add(getCancelButtonRow(cancelButtonText));
    return buttons;
  }

  public static List<List<InlineKeyboardMarkupButton>> buildButtonsWithBack(
      List<List<InlineKeyboardMarkupButton>> buttons, String cancelButtonText) {
    if (buttons == null) {
      List<List<InlineKeyboardMarkupButton>> newButtons = new ArrayList<>();
      newButtons.add(getBackButtonRow(cancelButtonText));
      return newButtons;
    }
    buttons.add(getBackButtonRow(cancelButtonText));
    return buttons;
  }

  public static String getUserDisplayName(User user) {
    return String.format(
            "%s %s", user.getFirstName(), user.getLastName() != null ? user.getLastName() : "")
        .trim();
  }

  public String getMyteamLink(String email) {
    return pluginData.getProfileLink() + email;
  }

  public String formatSystemFields(
      ApplicationUser recipient, Issue issue, boolean useMentionFormat) {
    StringBuilder sb = new StringBuilder();
    if (issue.getIssueType() != null)
      appendField(
          sb,
          i18nResolver.getRawText("issue.field.issuetype"),
          shieldText(issue.getIssueType().getNameTranslation(i18nHelper)),
          false);

    appendField(
        sb, i18nResolver.getRawText("issue.field.affectsversions"), issue.getAffectedVersions());
    appendField(
        sb,
        i18nResolver.getRawText("issue.field.assignee"),
        formatUser(issue.getAssignee(), "common.concepts.unassigned", useMentionFormat),
        false);

    appendField(sb, i18nResolver.getRawText("issue.field.components"), issue.getComponents());

    if (issue.getCreated() != null)
      appendField(
          sb,
          i18nResolver.getRawText("issue.field.created"),
          dateTimeFormatter
              .forUser(recipient)
              .withStyle(DateTimeStyle.COMPLETE)
              .format(issue.getCreated()),
          false);

    if (issue.getDueDate() != null)
      appendField(
          sb,
          i18nResolver.getRawText("issue.field.duedate"),
          dateTimeFormatter
              .forUser(recipient)
              .withSystemZone()
              .withStyle(DateTimeStyle.DATE)
              .format(issue.getDueDate()),
          false);

    appendField(
        sb,
        i18nResolver.getRawText("issue.field.environment"),
        shieldText(issue.getEnvironment()),
        false);
    appendField(sb, i18nResolver.getRawText("issue.field.fixversions"), issue.getFixVersions());
    appendField(sb, i18nResolver.getRawText("issue.field.labels"), issue.getLabels());
    appendField(
        sb,
        i18nResolver.getRawText("issue.field.priority"),
        shieldText(formatPriority(issue.getPriority())),
        false);
    appendField(
        sb,
        i18nResolver.getRawText("issue.field.reporter"),
        formatUser(issue.getReporter(), "common.concepts.no.reporter", useMentionFormat),
        false);

    if (issue.getSecurityLevelId() != null) {
      IssueSecurityLevel issueSecurityLevel =
          issueSecurityLevelManager.getSecurityLevel(issue.getSecurityLevelId());
      String value = issueSecurityLevel.getName();
      if (!StringUtils.isBlank(issueSecurityLevel.getDescription()))
        value += " " + issueSecurityLevel.getDescription();
      appendField(
          sb, i18nResolver.getRawText("issue.field.securitylevel"), shieldText(value), false);
    }
    appendField(sb, i18nResolver.getRawText("issue.field.attachment"), issue.getAttachments());

    if (!StringUtils.isBlank(issue.getDescription())) {
      sb.append("\n\n")
          .append(makeMyteamMarkdownFromJira(issue.getDescription(), useMentionFormat));
    }

    return sb.toString();
  }

  public String createIssueLink(String issueKey) {
    return String.format(
        "%s/browse/%s", applicationProperties.getString(APKeys.JIRA_BASEURL), issueKey);
  }

  public String createMarkdownIssueShortLink(String issueKey) {
    return String.format(
        "[%s](%s/browse/%s)",
        issueKey, applicationProperties.getString(APKeys.JIRA_BASEURL), issueKey);
  }

  public String formatEvent(ApplicationUser recipient, IssueEvent issueEvent) {
    Issue issue = issueEvent.getIssue();
    ApplicationUser user = issueEvent.getUser();
    String issueLink = markdownTextLink(issue.getKey(), createIssueLink(issue.getKey()));
    StringBuilder sb = new StringBuilder();

    boolean useMentionFormat = !recipient.equals(user);
    Long eventTypeId = issueEvent.getEventTypeId();
    if (EventType.ISSUE_CREATED_ID.equals(eventTypeId)) {
      sb.append(
          i18nResolver.getText(
              "ru.mail.jira.plugins.myteam.notification.created",
              formatUser(user, "common.words.anonymous", useMentionFormat),
              issueLink));
    } else if (EventType.ISSUE_UPDATED_ID.equals(eventTypeId)
        || EventType.ISSUE_COMMENT_DELETED_ID.equals(eventTypeId)
        || EventType.ISSUE_GENERICEVENT_ID.equals(eventTypeId)) {
      // {0} обновил запрос [ {1} ]
      sb.append(
          i18nResolver.getText(
              "ru.mail.jira.plugins.myteam.notification.updated",
              formatUser(user, "common.words.anonymous", useMentionFormat),
              issueLink));
    } else if (EventType.ISSUE_ASSIGNED_ID.equals(eventTypeId)) {
      sb.append(
          i18nResolver.getText(
              "ru.mail.jira.plugins.myteam.notification.assigned",
              formatUser(user, "common.words.anonymous", useMentionFormat),
              issueLink,
              formatUser(issue.getAssignee(), "common.concepts.unassigned", useMentionFormat)));
    } else if (EventType.ISSUE_RESOLVED_ID.equals(eventTypeId)) {
      Resolution resolution = issue.getResolution();
      sb.append(
          i18nResolver.getText(
              "ru.mail.jira.plugins.myteam.notification.resolved",
              formatUser(user, "common.words.anonymous", useMentionFormat),
              issueLink,
              resolution != null
                  ? resolution.getNameTranslation(i18nHelper)
                  : i18nResolver.getText("common.resolution.unresolved")));
    } else if (EventType.ISSUE_CLOSED_ID.equals(eventTypeId)) {
      Resolution resolution = issue.getResolution();
      sb.append(
          i18nResolver.getText(
              "ru.mail.jira.plugins.myteam.notification.closed",
              formatUser(user, "common.words.anonymous", useMentionFormat),
              issueLink,
              resolution != null
                  ? resolution.getNameTranslation(i18nHelper)
                  : i18nResolver.getText("common.resolution.unresolved")));
    } else if (EventType.ISSUE_COMMENTED_ID.equals(eventTypeId)) {
      sb.append(
          i18nResolver.getText(
              "ru.mail.jira.plugins.myteam.notification.commented",
              formatUser(user, "common.words.anonymous", useMentionFormat),
              issueLink));
    } else if (EventType.ISSUE_COMMENT_EDITED_ID.equals(eventTypeId)) {
      sb.append(
          i18nResolver.getText(
              "ru.mail.jira.plugins.myteam.notification.commentEdited",
              formatUser(user, "common.words.anonymous", useMentionFormat),
              issueLink));
    } else if (EventType.ISSUE_REOPENED_ID.equals(eventTypeId)) {
      sb.append(
          i18nResolver.getText(
              "ru.mail.jira.plugins.myteam.notification.reopened",
              formatUser(user, "common.words.anonymous", useMentionFormat),
              issueLink));
    } else if (EventType.ISSUE_DELETED_ID.equals(eventTypeId)) {
      sb.append(
          i18nResolver.getText(
              "ru.mail.jira.plugins.myteam.notification.deleted",
              formatUser(user, "common.words.anonymous", useMentionFormat),
              issueLink));
    } else if (EventType.ISSUE_MOVED_ID.equals(eventTypeId)) {
      sb.append(
          i18nResolver.getText(
              "ru.mail.jira.plugins.myteam.notification.moved",
              formatUser(user, "common.words.anonymous", useMentionFormat),
              issueLink));
    } else if (EventType.ISSUE_WORKLOGGED_ID.equals(eventTypeId)) {
      sb.append(
          i18nResolver.getText(
              "ru.mail.jira.plugins.myteam.notification.worklogged",
              formatUser(user, "common.words.anonymous", useMentionFormat),
              issueLink));
    } else if (EventType.ISSUE_WORKSTARTED_ID.equals(eventTypeId)) {
      sb.append(
          i18nResolver.getText(
              "ru.mail.jira.plugins.myteam.notification.workStarted",
              formatUser(user, "common.words.anonymous", useMentionFormat),
              issueLink));
    } else if (EventType.ISSUE_WORKSTOPPED_ID.equals(eventTypeId)) {
      sb.append(
          i18nResolver.getText(
              "ru.mail.jira.plugins.myteam.notification.workStopped",
              formatUser(user, "common.words.anonymous", useMentionFormat),
              issueLink));
    } else if (EventType.ISSUE_WORKLOG_UPDATED_ID.equals(eventTypeId)) {
      sb.append(
          i18nResolver.getText(
              "ru.mail.jira.plugins.myteam.notification.worklogUpdated",
              formatUser(user, "common.words.anonymous", useMentionFormat),
              issueLink));
    } else if (EventType.ISSUE_WORKLOG_DELETED_ID.equals(eventTypeId)) {
      sb.append(
          i18nResolver.getText(
              "ru.mail.jira.plugins.myteam.notification.worklogDeleted",
              formatUser(user, "common.words.anonymous", useMentionFormat),
              issueLink));
    } else {
      sb.append(
          i18nResolver.getText(
              "ru.mail.jira.plugins.myteam.notification.updated",
              formatUser(user, "common.words.anonymous", useMentionFormat),
              issueLink));
    }
    sb.append("\n").append(shieldText(issue.getSummary()));

    if (issueEvent.getWorklog() != null
        && !StringUtils.isBlank(issueEvent.getWorklog().getComment()))
      sb.append("\n\n").append(shieldText(issueEvent.getWorklog().getComment()));

    if (EventType.ISSUE_CREATED_ID.equals(eventTypeId))
      sb.append(formatSystemFields(recipient, issue, useMentionFormat));

    sb.append(
        formatChangeLog(
            issueEvent.getChangeLog(),
            EventType.ISSUE_ASSIGNED_ID.equals(eventTypeId),
            useMentionFormat));
    if (issueEvent.getComment() != null && !StringUtils.isBlank(issueEvent.getComment().getBody()))
      sb.append("\n\n")
          .append(makeMyteamMarkdownFromJira(issueEvent.getComment().getBody(), useMentionFormat));
    return sb.toString();
  }

  public String formatEvent(ApplicationUser recipient, MentionIssueEvent mentionIssueEvent) {
    Issue issue = mentionIssueEvent.getIssue();
    ApplicationUser user = mentionIssueEvent.getFromUser();
    String issueLink = getIssueLink(issue.getKey());

    StringBuilder sb = new StringBuilder();
    sb.append(
        i18nResolver.getText(
            "ru.mail.jira.plugins.myteam.notification.mentioned",
            formatUser(user, "common.words.anonymous", true),
            issueLink));
    sb.append("\n").append(shieldText(issue.getSummary()));

    if (!StringUtils.isBlank(mentionIssueEvent.getMentionText()))
      sb.append("\n\n")
          .append(makeMyteamMarkdownFromJira(mentionIssueEvent.getMentionText(), true));

    return sb.toString();
  }

  public String createIssueSummary(Issue issue, ApplicationUser user) {
    StringBuilder sb = new StringBuilder();
    sb.append(getIssueLink(issue.getKey()))
        .append("   ")
        .append(shieldText(issue.getSummary()))
        .append("\n");

    // append status field because it doesn't exist in formatSystemFields string
    appendField(
        sb,
        i18nHelper.getText(fieldManager.getField(IssueFieldConstants.STATUS).getNameKey()),
        shieldText(issue.getStatus().getNameTranslation(i18nHelper)),
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
                                  shieldText(customField.getFieldName()),
                                  shieldText(customField.getValueFromIssue(issue)),
                                  false);
                          }
                        }));

    return sb.toString();
  }

  private String getIssueLink(String key) {
    return markdownTextLink(
        key,
        String.format("%s/browse/%s", applicationProperties.getString(APKeys.JIRA_BASEURL), key));
  }

  public List<List<InlineKeyboardMarkupButton>> getCancelButton() {
    List<List<InlineKeyboardMarkupButton>> buttons = new ArrayList<>();

    buttons.add(getCancelButtonRow());

    return buttons;
  }

  public List<InlineKeyboardMarkupButton> getCancelButtonRow() {
    return getCancelButtonRow(
        i18nResolver.getRawText(
            "ru.mail.jira.plugins.myteam.mrimsenderEventListener.cancelButton.text"));
  }

  public List<List<InlineKeyboardMarkupButton>> getMenuButtons() {
    List<List<InlineKeyboardMarkupButton>> buttons = new ArrayList<>();

    // create 'search issue' button
    InlineKeyboardMarkupButton showIssueButton =
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            i18nResolver.getRawText(
                "ru.mail.jira.plugins.myteam.messageFormatter.mainMenu.showIssueButton.text"),
            ButtonRuleType.SearchIssueByKeyInput.getName());
    addRowWithButton(buttons, showIssueButton);

    // create 'Active issues assigned to me' button
    InlineKeyboardMarkupButton activeAssignedIssuesButton =
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            i18nResolver.getRawText(
                "ru.mail.jira.plugins.myteam.messageFormatter.mainMenu.activeIssuesAssignedToMeButton.text"),
            CommandRuleType.AssignedIssues.getName());
    addRowWithButton(buttons, activeAssignedIssuesButton);

    // create 'Active issues i watching' button
    InlineKeyboardMarkupButton activeWatchingIssuesButton =
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            i18nResolver.getRawText(
                "ru.mail.jira.plugins.myteam.messageFormatter.mainMenu.activeIssuesWatchingByMeButton.text"),
            CommandRuleType.WatchingIssues.getName());
    addRowWithButton(buttons, activeWatchingIssuesButton);

    // create 'Active issues crated by me' button
    InlineKeyboardMarkupButton activeCreatedIssuesButton =
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            i18nResolver.getRawText(
                "ru.mail.jira.plugins.myteam.messageFormatter.mainMenu.activeIssuesCreatedByMeButton.text"),
            CommandRuleType.CreatedIssues.getName());
    addRowWithButton(buttons, activeCreatedIssuesButton);

    // create 'Search issue by JQL' button
    InlineKeyboardMarkupButton searchIssueByJqlButton =
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            i18nResolver.getRawText(
                "ru.mail.jira.plugins.myteam.messageFormatter.mainMenu.searchIssueByJqlButton.text"),
            ButtonRuleType.SearchIssueByJqlInput.getName());
    addRowWithButton(buttons, searchIssueByJqlButton);

    // create 'create issue' button
    InlineKeyboardMarkupButton createIssueButton =
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            i18nResolver.getRawText(
                "ru.mail.jira.plugins.myteam.messageFormatter.mainMenu.createIssueButton.text"),
            ButtonRuleType.CreateIssue.getName());
    addRowWithButton(buttons, createIssueButton);
    return buttons;
  }

  public List<List<InlineKeyboardMarkupButton>> getListButtons(boolean withPrev, boolean withNext) {
    if (!withPrev && !withNext) return null;
    List<List<InlineKeyboardMarkupButton>> buttons = new ArrayList<>(1);

    List<InlineKeyboardMarkupButton> pagerButtonsRow = getPagerButtonsRow(withPrev, withNext);
    if (pagerButtonsRow.size() > 0) {
      buttons.add(pagerButtonsRow);
    }
    return buttons;
  }

  public List<InlineKeyboardMarkupButton> getPagerButtonsRow(boolean withPrev, boolean withNext) {
    List<InlineKeyboardMarkupButton> newButtonsRow = new ArrayList<>();
    if (withPrev) {
      newButtonsRow.add(
          InlineKeyboardMarkupButton.buildButtonWithoutUrl(
              i18nResolver.getRawText(
                  "ru.mail.jira.plugins.myteam.messageFormatter.listButtons.prevPageButton.text"),
              ButtonRuleType.PrevPage.getName()));
    }
    if (withNext) {
      newButtonsRow.add(
          InlineKeyboardMarkupButton.buildButtonWithoutUrl(
              i18nResolver.getRawText(
                  "ru.mail.jira.plugins.myteam.messageFormatter.listButtons.nextPageButton.text"),
              ButtonRuleType.NextPage.getName()));
    }
    return newButtonsRow;
  }

  public String stringifyPagedCollection(
      Collection<?> collection, int pageNumber, int total, int pageSize) {
    if (collection.size() == 0) return "";

    StringJoiner sj = new StringJoiner("\n\n");

    // stringify collection
    collection.forEach(obj -> sj.add(obj.toString()));

    // append string with current (and total) page number info
    if ((pageNumber + 1) * pageSize < total) {
      int firstResultPageIndex = pageNumber * pageSize + 1;
      int lastResultPageIndex = firstResultPageIndex + collection.size() - 1;
      sj.add(DELIMITER_STR);
      sj.add(
          i18nResolver.getText(
              "pager.results.displayissues.short",
              String.join(
                  " - ",
                  Integer.toString(firstResultPageIndex),
                  Integer.toString(lastResultPageIndex)),
              Integer.toString(total)));
    }

    return sj.toString();
  }

  public String stringifyIssueList(List<Issue> issueList, int pageNumber, int total) {
    return stringifyPagedCollection(
        issueList.stream()
            .map(
                issue ->
                    markdownTextLink(issue.getKey(), createIssueLink(issue.getKey()))
                        + ' '
                        + issue.getSummary())
            .collect(Collectors.toList()),
        pageNumber,
        total,
        LIST_PAGE_SIZE);
  }

  public String stringifyFieldsCollection(Collection<? extends Field> fields) {
    return fields.stream()
        .map(field -> i18nResolver.getRawText(field.getNameKey()))
        .collect(Collectors.joining("\n"));
  }

  public String formatMyteamUserLink(User user) {
    StringBuilder str = new StringBuilder();

    str.append("[").append(user.getFirstName()).append(" ");

    if (user.getLastName() != null) {
      str.append(" ").append(user.getLastName());
    }
    str.append("|").append(getMyteamLink(user.getUserId())).append("]");
    return str.toString();
  }

  public String formatUser(ApplicationUser user, String messageKey, boolean mention) {
    if (user != null) {
      if (mention) {
        return "@\\[" + shieldText(user.getEmailAddress()) + "\\]";
      }

      return "["
          + shieldText(user.getDisplayName())
          + "]("
          + shieldText(pluginData.getProfileLink() + user.getEmailAddress())
          + ")";
    } else return i18nHelper.getText(messageKey);
  }

  private String formatPriority(@Nullable Priority priority) {
    if (priority != null) return priority.getNameTranslation(i18nHelper);
    else return null;
  }

  private void appendField(StringBuilder sb, String title, String value, boolean appendEmpty) {
    if (appendEmpty || !StringUtils.isBlank(value)) {
      if (sb.length() == 0) sb.append("\n");
      sb.append("\n").append(title).append(": ").append(StringUtils.defaultString(value));
    }
  }

  private void appendUserField(
      StringBuilder sb, String title, ApplicationUser user, boolean mentionFormat) {
    sb.append("\n")
        .append(title)
        .append(": ")
        .append(formatUser(user, "common.words.anonymous", mentionFormat));
  }

  private void appendField(StringBuilder sb, String title, Collection<?> collection) {
    if (collection != null) {
      StringBuilder value = new StringBuilder();
      Iterator<?> iterator = collection.iterator();
      while (iterator.hasNext()) {
        Object object = iterator.next();
        if (object instanceof ProjectConstant)
          value.append(shieldText(((ProjectConstant) object).getName()));
        if (object instanceof Attachment) {
          Attachment attachment = (Attachment) object;
          String attachmentUrl = "";
          try {
            attachmentUrl =
                new URI(
                        String.format(
                            "%s/secure/attachment/%d/%s",
                            jiraBaseUrl, attachment.getId(), attachment.getFilename()),
                        false,
                        StandardCharsets.UTF_8.toString())
                    .getEscapedURI();
          } catch (URIException e) {
            SentryClient.capture(e);
            log.error("Unable to create attachment link for file: {}", attachment.getFilename());
          }
          value.append(markdownTextLink(attachment.getFilename(), attachmentUrl));
        }
        if (object instanceof Label) value.append(shieldText(((Label) object).getLabel()));
        if (iterator.hasNext()) value.append(", ");
      }
      appendField(sb, title, value.toString(), false);
    }
  }

  private String markdownTextLink(String text, String url) {
    if (text != null) {
      try {
        return "["
            + shieldText(text)
            + "]("
            + shieldText(new URI(url, false, StandardCharsets.UTF_8.toString()).getEscapedURI())
            + ")";
      } catch (URIException e) {
        SentryClient.capture(e);
        log.error("Unable to create text link for issueKey: {}, url:{}", text, url);
      }
    }
    return url;
  }

  private String convertToMarkdown(
      String inputText, Pattern pattern, Function<Matcher, String> converter) {
    int lastIndex = 0;
    StringBuilder output = new StringBuilder();
    Matcher matcher = pattern.matcher(inputText);
    while (matcher.find()) {
      output.append(inputText, lastIndex, matcher.start()).append(converter.apply(matcher));
      lastIndex = matcher.end();
    }
    if (lastIndex < inputText.length()) {
      output.append(inputText, lastIndex, inputText.length());
    }
    return output.toString();
  }

  private String makeMyteamMarkdownFromJira(String inputText, boolean useMentionFormat) {
    if (inputText == null) {
      return null;
    }
    // codeBlockPattern
    inputText =
        convertToMarkdown(
            inputText,
            Pattern.compile("\\{[Cc]ode:([a-z]+?)}([^+]*?)\\{[Cc]ode}", Pattern.MULTILINE),
            (input) -> "\n±`±`±`" + input.group(1) + " " + input.group(2) + "±`±`±`");
    inputText =
        convertToMarkdown(
            inputText,
            Pattern.compile("\\{[Cc]ode}([^+]*?)\\{[Cc]ode}", Pattern.MULTILINE),
            (input) -> "\n±`±`±`" + input.group(1) + "±`±`±`");
    // inlineCodePattern
    inputText =
        convertToMarkdown(
            inputText,
            Pattern.compile("\\{\\{([^}?\\n]+)}}"),
            (input) -> "±`" + input.group(1) + "±`");
    // mentionPattern
    inputText =
        convertToMarkdown(
            inputText,
            Pattern.compile("\\[~(.*?)]"),
            (input) -> {
              ApplicationUser mentionUser = userManager.getUserByName(input.group(1));
              if (mentionUser != null) {
                if (useMentionFormat) {
                  return "±@\\±[" + shieldText(mentionUser.getEmailAddress()) + "\\±]";
                }
                return "±["
                    + shieldText(mentionUser.getDisplayName())
                    + "±]±("
                    + shieldText(pluginData.getProfileLink() + mentionUser.getEmailAddress())
                    + "±)";
              } else return i18nHelper.getText("common.words.anonymous");
            });
    // strikethroughtPattern
    inputText =
        convertToMarkdown(
            inputText,
            Pattern.compile("(^|\\s)-([^- \\n].*?[^- \\n])-($|\\s|\\.)"),
            (input) -> input.group(1) + "±~" + input.group(2) + "±~" + input.group(3));
    // multi level numbered list
    inputText =
        convertToMarkdown(
            inputText,
            Pattern.compile("^((?:#|-|\\+|\\*)+) (.*)$", Pattern.MULTILINE),
            (input) -> "±- " + input.group(2));
    // bold Pattern
    inputText =
        convertToMarkdown(
            inputText,
            Pattern.compile("(^|\\s)\\*([^* \\n].*?[^* \\n])\\*($|\\s|\\.)"),
            (input) -> input.group(1) + "±*" + input.group(2) + "±*" + input.group(3));
    // underLinePattern
    inputText =
        convertToMarkdown(
            inputText,
            Pattern.compile("(^|\\s)\\+([^+ \\n].*?[^+ \\n])\\+($|\\s|\\.)"),
            (input) -> input.group(1) + "±_±_" + input.group(2) + "±_±_" + input.group(3));
    // linkPattern
    inputText =
        convertToMarkdown(
            inputText,
            Pattern.compile("\\[([^~|?\n]+)\\|(.+?)]"),
            (input) -> "±[" + input.group(1) + "±]±(" + input.group(2) + "±)");
    // Italic
    inputText =
        convertToMarkdown(
            inputText,
            Pattern.compile("(^|\\s)_([^_ \\n].*?[^_ \\n])_($|\\s|\\.)"),
            (input) -> input.group(1) + "±_" + input.group(2) + "±_" + input.group(3));
    // Single characters
    inputText =
        convertToMarkdown(
            inputText,
            Pattern.compile("(?<!±)([`{}+|@\\[\\]()~\\-*_])"),
            input -> "\\" + input.group(1));
    // Marked characters
    inputText =
        convertToMarkdown(
            inputText, Pattern.compile("±([`{}+|@\\[\\]()~\\-*_])"), input -> input.group(1));

    return inputText;
  }

  private String formatChangeLog(
      GenericValue changeLog, boolean ignoreAssigneeField, boolean useMentionFormat) {
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
              || ("assignee".equals(field) && ignoreAssigneeField)) continue;

          String title = field;
          if ("Attachment".equals(field)) {
            String attachmentId = changeItem.getString("newvalue");
            if (StringUtils.isNotEmpty(attachmentId)) {
              Attachment attachment = attachmentManager.getAttachment(Long.valueOf(attachmentId));
              try {
                sb.append("\n\n")
                    .append(
                        markdownTextLink(
                            attachment.getFilename(),
                            new URI(
                                    String.format(
                                        "%s/secure/attachment/%d/%s",
                                        jiraBaseUrl, attachment.getId(), attachment.getFilename()),
                                    false,
                                    StandardCharsets.UTF_8.toString())
                                .getEscapedURI()));
              } catch (URIException e) {
                SentryClient.capture(e);
                log.error(
                    "Can't find attachment id:{} name:{}",
                    changeItem.getString("newvalue"),
                    changeItem.getString("newstring"),
                    e);
              }
            } else {
              sb.append("\n\n")
                  .append(
                      i18nResolver.getText(
                          "ru.mail.jira.plugins.myteam.notification.attachmentDeleted",
                          changeItem.getString("oldstring")));
            }
            continue;
          }
          if (!"custom".equalsIgnoreCase(changeItem.getString("fieldtype")))
            title = i18nResolver.getText("issue.field." + field.replaceAll(" ", "").toLowerCase());

          if (("Fix Version".equals(field) || "Component".equals(field) || "Version".equals(field))
              && changeItem.get("oldvalue") != null
              && changeItem.get("newvalue") == null) {
            newString = changeItem.getString("oldstring");
            title = i18nResolver.getText("ru.mail.jira.plugins.myteam.notification.deleted", title);
          }

          if (fieldManager.isNavigableField(field)) {
            final NavigableField navigableField = fieldManager.getNavigableField(field);
            if (navigableField != null) {
              if (navigableField instanceof UserField) {
                appendUserField(
                    sb,
                    title,
                    userManager.getUserByKey(changeItem.getString("newvalue")),
                    useMentionFormat);
                continue;
              }
              newString = navigableField.prettyPrintChangeHistory(newString);
            }
          }

          appendField(sb, title, shieldText(newString), true);
        }

        if (!StringUtils.isBlank(changedDescription))
          sb.append("\n\n")
              .append(makeMyteamMarkdownFromJira(changedDescription, useMentionFormat));
      } catch (Exception e) {
        SentryClient.capture(e);
      }
    return sb.toString();
  }
}
