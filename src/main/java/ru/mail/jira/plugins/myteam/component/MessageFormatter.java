/* (C)2020 */
package ru.mail.jira.plugins.myteam.component;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static ru.mail.jira.plugins.myteam.commons.Utils.shieldText;

import com.atlassian.diff.CharacterChunk;
import com.atlassian.diff.DiffChunk;
import com.atlassian.diff.DiffType;
import com.atlassian.diff.DiffViewBean;
import com.atlassian.diff.WordChunk;
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
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.issue.fields.*;
import com.atlassian.jira.issue.fields.screen.FieldScreen;
import com.atlassian.jira.issue.fields.screen.FieldScreenManager;
import com.atlassian.jira.issue.fields.screen.FieldScreenScheme;
import com.atlassian.jira.issue.fields.screen.issuetype.IssueTypeScreenSchemeManager;
import com.atlassian.jira.issue.label.Label;
import com.atlassian.jira.issue.operation.IssueOperations;
import com.atlassian.jira.issue.priority.Priority;
import com.atlassian.jira.issue.resolution.Resolution;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.issue.security.IssueSecurityLevel;
import com.atlassian.jira.issue.security.IssueSecurityLevelManager;
import com.atlassian.jira.project.ProjectConstant;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.message.I18nResolver;
import com.opensymphony.util.TextUtils;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ofbiz.core.entity.GenericValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.commons.SentryClient;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.ButtonRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.CommandRuleType;
import ru.mail.jira.plugins.myteam.component.url.dto.Link;
import ru.mail.jira.plugins.myteam.component.url.dto.LinksInMessage;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;
import ru.mail.jira.plugins.myteam.myteam.dto.User;
import ru.mail.jira.plugins.myteam.service.PluginData;

@Slf4j
@Component
public class MessageFormatter {
  public static final int LIST_PAGE_SIZE = 15;
  public static final int MAX_MESSAGE_COUNT = 50;
  public static final String DELIMITER_STR = "----------";
  private static final int DISPLAY_FIELD_CHARS_LIMIT = 5000;
  private static final int MAX_WHITESPACE_PRESERVATION_LENGTH = 20;

  @SuppressWarnings("InlineFormatString")
  private static final String DESCRIPTION_MARKDOWN_MASKED_LINK_TEMPLATE = "[%s|%s]";

  @SuppressWarnings("InlineFormatString")
  private static final String DESCRIPTION_MARKDOWN_UNMASKED_LINK_TEMPLATE = "%s";

  private static final List<Pattern> patternsToExcludeDescriptionForDiff =
      initPatternsToExcludeDescriptionForDiff();

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
      @Nullable List<List<InlineKeyboardMarkupButton>> buttons, String cancelButtonText) {
    if (buttons == null) {
      List<List<InlineKeyboardMarkupButton>> newButtons = new ArrayList<>();
      newButtons.add(getCancelButtonRow(cancelButtonText));
      return newButtons;
    }
    buttons.add(getCancelButtonRow(cancelButtonText));
    return buttons;
  }

  public static List<List<InlineKeyboardMarkupButton>> buildButtonsWithBack(
      @Nullable List<List<InlineKeyboardMarkupButton>> buttons, String cancelButtonText) {
    if (buttons == null) {
      List<List<InlineKeyboardMarkupButton>> newButtons = new ArrayList<>();
      newButtons.add(getBackButtonRow(cancelButtonText));
      return newButtons;
    }
    buttons.add(getBackButtonRow(cancelButtonText));
    return buttons;
  }

  public static String getUserDisplayName(User user) {
    return format(
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
      if (!isBlank(issueSecurityLevel.getDescription()))
        value += " " + issueSecurityLevel.getDescription();
      appendField(
          sb, i18nResolver.getRawText("issue.field.securitylevel"), shieldText(value), false);
    }
    appendField(sb, i18nResolver.getRawText("issue.field.attachment"), issue.getAttachments());

    String description = issue.getDescription();
    if (StringUtils.isNotBlank(description)) {
      sb.append("\n\n")
          .append(makeMyteamMarkdownFromJira(limitFieldValue(description), useMentionFormat));
    }

    return sb.toString();
  }

  public String createIssueLink(String issueKey) {
    return format("%s/browse/%s", applicationProperties.getString(APKeys.JIRA_BASEURL), issueKey);
  }

  public String createJqlLink(String jql) {
    return format("%s/issues/?jql=%s", applicationProperties.getString(APKeys.JIRA_BASEURL), jql);
  }

  public String createFilterLink(Long id) {
    return format("%s/issues/?filter=%s", applicationProperties.getString(APKeys.JIRA_BASEURL), id);
  }

  public String createMarkdownIssueShortLink(String issueKey) {
    return format(
        "[%s](%s/browse/%s)",
        issueKey, applicationProperties.getString(APKeys.JIRA_BASEURL), issueKey);
  }

  @Nullable
  public String formatEventWithDiff(ApplicationUser recipient, IssueEvent issueEvent) {
    Issue issue = issueEvent.getIssue();
    ApplicationUser user = issueEvent.getUser();
    String issueLink = markdownTextLink(issue.getKey(), createIssueLink(issue.getKey()));
    StringBuilder sb = new StringBuilder();

    boolean useMentionFormat = !recipient.equals(user);

    Long eventTypeId = issueEvent.getEventTypeId();
    Map<Long, String> eventTypeMap = getEventTypeMap();

    String eventTypeKey = eventTypeMap.getOrDefault(eventTypeId, "updated");
    String i18nKey = "ru.mail.jira.plugins.myteam.notification." + eventTypeKey;

    if (EventType.ISSUE_ASSIGNED_ID.equals(eventTypeId)) {
      sb.append(
          i18nResolver.getText(
              i18nKey,
              formatUser(user, "common.words.anonymous", useMentionFormat),
              issueLink,
              formatUser(issue.getAssignee(), "common.concepts.unassigned", useMentionFormat)));
    } else if (EventType.ISSUE_RESOLVED_ID.equals(eventTypeId)
        || EventType.ISSUE_CLOSED_ID.equals(eventTypeId)) {
      Resolution resolution = issue.getResolution();
      sb.append(
          i18nResolver.getText(
              i18nKey,
              formatUser(user, "common.words.anonymous", useMentionFormat),
              issueLink,
              resolution != null
                  ? resolution.getNameTranslation(i18nHelper)
                  : i18nResolver.getText("common.resolution.unresolved")));
    } else if (EventType.ISSUE_COMMENTED_ID.equals(eventTypeId)
        || EventType.ISSUE_COMMENT_EDITED_ID.equals(eventTypeId)) {
      sb.append(
          i18nResolver.getText(
              i18nKey,
              getIssueLink(issue.getKey()),
              issue.getSummary(),
              formatUser(user, "common.words.anonymous", useMentionFormat),
              format(
                  "%s/browse/%s?focusedCommentId=%s&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-%s",
                  jiraBaseUrl,
                  issue.getKey(),
                  issueEvent.getComment().getId(),
                  issueEvent.getComment().getId()),
              makeMyteamMarkdownFromJira(issueEvent.getComment().getBody(), useMentionFormat)));
      return sb.toString();
    } else {
      sb.append(
          i18nResolver.getText(
              i18nKey, formatUser(user, "common.words.anonymous", useMentionFormat), issueLink));
    }
    sb.append("\n").append(shieldText(issue.getSummary()));

    if (issueEvent.getWorklog() != null && !isBlank(issueEvent.getWorklog().getComment()))
      sb.append("\n\n").append(shieldText(issueEvent.getWorklog().getComment()));

    if (EventType.ISSUE_CREATED_ID.equals(eventTypeId))
      sb.append(formatSystemFields(recipient, issue, useMentionFormat));

    sb.append(
        formatChangeLogWithDiff(
            issueEvent.getChangeLog(),
            EventType.ISSUE_ASSIGNED_ID.equals(eventTypeId),
            useMentionFormat));
    if (issueEvent.getComment() != null && !isBlank(issueEvent.getComment().getBody())) {
      if (EventType.ISSUE_COMMENTED_ID.equals(eventTypeId)
          && issueEvent.getComment().getBody().contains("[~" + recipient.getName() + "]")) {
        // do not send message when recipient mentioned in comment
        return null;
      }

      sb.append("\n\n")
          .append(makeMyteamMarkdownFromJira(issueEvent.getComment().getBody(), useMentionFormat));
    }
    return sb.toString();
  }

  @Nullable
  public String formatEvent(ApplicationUser recipient, IssueEvent issueEvent) {
    Issue issue = issueEvent.getIssue();
    ApplicationUser user = issueEvent.getUser();
    String issueLink = markdownTextLink(issue.getKey(), createIssueLink(issue.getKey()));
    StringBuilder sb = new StringBuilder();

    boolean useMentionFormat = !recipient.equals(user);

    Long eventTypeId = issueEvent.getEventTypeId();
    Map<Long, String> eventTypeMap = getEventTypeMap();

    String eventTypeKey = eventTypeMap.getOrDefault(eventTypeId, "updated");
    String i18nKey = "ru.mail.jira.plugins.myteam.notification." + eventTypeKey;

    if (EventType.ISSUE_ASSIGNED_ID.equals(eventTypeId)) {
      sb.append(
          i18nResolver.getText(
              i18nKey,
              formatUser(user, "common.words.anonymous", useMentionFormat),
              issueLink,
              formatUser(issue.getAssignee(), "common.concepts.unassigned", useMentionFormat)));
    } else if (EventType.ISSUE_RESOLVED_ID.equals(eventTypeId)
        || EventType.ISSUE_CLOSED_ID.equals(eventTypeId)) {
      Resolution resolution = issue.getResolution();
      sb.append(
          i18nResolver.getText(
              i18nKey,
              formatUser(user, "common.words.anonymous", useMentionFormat),
              issueLink,
              resolution != null
                  ? resolution.getNameTranslation(i18nHelper)
                  : i18nResolver.getText("common.resolution.unresolved")));
    } else if (EventType.ISSUE_COMMENTED_ID.equals(eventTypeId)
        || EventType.ISSUE_COMMENT_EDITED_ID.equals(eventTypeId)) {
      sb.append(
          i18nResolver.getText(
              i18nKey,
              getIssueLink(issue.getKey()),
              issue.getSummary(),
              formatUser(user, "common.words.anonymous", useMentionFormat),
              format(
                  "%s/browse/%s?focusedCommentId=%s&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-%s",
                  jiraBaseUrl,
                  issue.getKey(),
                  issueEvent.getComment().getId(),
                  issueEvent.getComment().getId()),
              makeMyteamMarkdownFromJira(issueEvent.getComment().getBody(), useMentionFormat)));
      return sb.toString();
    } else {
      sb.append(
          i18nResolver.getText(
              i18nKey, formatUser(user, "common.words.anonymous", useMentionFormat), issueLink));
    }
    sb.append("\n").append(shieldText(issue.getSummary()));

    if (issueEvent.getWorklog() != null && !isBlank(issueEvent.getWorklog().getComment()))
      sb.append("\n\n").append(shieldText(issueEvent.getWorklog().getComment()));

    if (EventType.ISSUE_CREATED_ID.equals(eventTypeId))
      sb.append(formatSystemFields(recipient, issue, useMentionFormat));

    sb.append(
        formatChangeLog(
            issueEvent.getChangeLog(),
            EventType.ISSUE_ASSIGNED_ID.equals(eventTypeId),
            useMentionFormat));
    if (issueEvent.getComment() != null && !isBlank(issueEvent.getComment().getBody())) {
      if (EventType.ISSUE_COMMENTED_ID.equals(eventTypeId)
          && issueEvent.getComment().getBody().contains("[~" + recipient.getName() + "]")) {
        // do not send message when recipient mentioned in comment
        return null;
      }

      sb.append("\n\n")
          .append(makeMyteamMarkdownFromJira(issueEvent.getComment().getBody(), useMentionFormat));
    }
    return sb.toString();
  }

  @Nullable
  public String formatJiraIssueCommentToLink(final Issue issue, final Comment comment) {
    return i18nResolver.getText(
        "ru.mail.jira.plugins.myteam.comment.issue.commentCreated",
        format(
            "%s/browse/%s?focusedCommentId=%s&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-%s",
            jiraBaseUrl, issue.getKey(), comment.getId(), comment.getId()));
  }

  public String formatEvent(MentionIssueEvent mentionIssueEvent) {
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

    if (!isBlank(mentionIssueEvent.getMentionText()))
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
        key, format("%s/browse/%s", applicationProperties.getString(APKeys.JIRA_BASEURL), key));
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

  @Nullable
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
      Collection<?> collection, int pageNumber, int total, int pageSize, String totalLink) {
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
              StringUtils.isNotBlank(totalLink) ? totalLink : Integer.toString(total)));
    }

    return sj.toString();
  }

  public String stringifyIssueList(
      List<Issue> issueList, int pageNumber, int total, String totalLink) {
    return stringifyPagedCollection(
        issueList.stream()
            .map(
                issue ->
                    markdownTextLink(issue.getKey(), createIssueLink(issue.getKey()))
                        + ' '
                        + shieldText(issue.getSummary()))
            .collect(Collectors.toList()),
        pageNumber,
        total,
        LIST_PAGE_SIZE,
        totalLink);
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

  public String formatEmptyFilterSubscription(String filterName, Long filterId) {
    return i18nResolver.getText(
            "ru.mail.jira.plugins.myteam.subscriptions.page.subscription.message.header",
            markdownTextLink(filterName, createFilterLink(filterId)))
        + "\n\n"
        + i18nResolver.getText(
            "ru.mail.jira.plugins.myteam.subscriptions.page.subscription.message.body.empty");
  }

  public String formatIssueFilterSubscription(String filterName, Long filterId, int totalIssues) {
    StringBuilder sb = new StringBuilder();
    sb.append(
        i18nResolver.getText(
            "ru.mail.jira.plugins.myteam.subscriptions.page.subscription.message.header",
            markdownTextLink(filterName, createFilterLink(filterId))));
    sb.append("\n\n");

    String displayIssue =
        totalIssues > MAX_MESSAGE_COUNT
            ? i18nResolver.getText(
                "pager.results.displayissues.short", MAX_MESSAGE_COUNT, totalIssues)
            : String.valueOf(totalIssues);
    sb.append(displayIssue);
    sb.append(" ");
    sb.append(
        i18nResolver.getText(
            "ru.mail.jira.plugins.myteam.subscriptions.page.subscription.message.body"));
    return sb.toString();
  }

  public String formatListFilterSubscription(
      String filterName, Long filterId, String searchJql, SearchResults<Issue> issueSearchResults) {
    StringBuilder sb = new StringBuilder();
    sb.append(
        i18nResolver.getText(
            "ru.mail.jira.plugins.myteam.subscriptions.page.subscription.message.header",
            markdownTextLink(filterName, createFilterLink(filterId))));
    sb.append("\n\n");

    int totalIssues = issueSearchResults.getTotal();
    sb.append(
        stringifyIssueList(
            issueSearchResults.getResults(),
            0,
            totalIssues,
            markdownTextLink(Integer.toString(totalIssues), createJqlLink(searchJql))));
    return sb.toString();
  }

  public String formatAccessRequestMessage(
      @NotNull ApplicationUser user, @NotNull Issue issue, @Nullable String message) {
    StringBuilder sb = new StringBuilder();
    sb.append(
        i18nResolver.getText(
            "ru.mail.jira.plugins.myteam.accessRequest.page.message.title",
            formatUser(user, "common.words.anonymous", true),
            markdownTextLink(issue.getKey(), createIssueLink(issue.getKey())),
            issue.getSummary()));
    if (StringUtils.isNotBlank(message)) {
      sb.append("\n");
      sb.append(
          i18nResolver.getText(
              "ru.mail.jira.plugins.myteam.accessRequest.page.message.text", message));
    }
    return sb.toString();
  }

  @Nullable
  private String formatPriority(@Nullable Priority priority) {
    if (priority != null) return priority.getNameTranslation(i18nHelper);
    else return null;
  }

  private void appendField(
      StringBuilder sb, @Nullable String title, @Nullable String value, boolean appendEmpty) {
    if (appendEmpty || !isBlank(value)) {
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
        if (object instanceof ProjectConstant)
          value.append(shieldText(((ProjectConstant) object).getName()));
        if (object instanceof Attachment) {
          Attachment attachment = (Attachment) object;
          String attachmentUrl = "";
          try {
            attachmentUrl =
                new URI(
                        format(
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

  @Nullable
  private String makeMyteamMarkdownFromJira(@Nullable String inputText, boolean useMentionFormat) {
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
    // Quote
    inputText =
        convertToMarkdown(
            inputText,
            Pattern.compile("\\{[Qq]uote}([^+]*?)\\{[Qq]uote}", Pattern.MULTILINE),
            (input) -> "\n>" + input.group(1).replaceAll("\n", "\n>") + "\n");
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

  private String formatChangeLogWithDiff(
      GenericValue changeLog, boolean ignoreAssigneeField, boolean useMentionFormat) {
    StringBuilder sb = new StringBuilder();
    if (changeLog != null)
      try {
        String changedDescription = null;
        String oldDesc = null;
        boolean oldOrNewDescHasComplexFormatting = false;
        for (GenericValue changeItem : changeLog.getRelated("ChildChangeItem")) {
          String field = StringUtils.defaultString(changeItem.getString("field"));
          String newString = StringUtils.defaultString(changeItem.getString("newstring"));

          if ("description".equals(field)) {
            oldDesc = StringUtils.defaultString(changeItem.getString("oldstring"));
            oldOrNewDescHasComplexFormatting = checkDescOnComplexJiraWikiStyles(oldDesc, newString);
            oldDesc = limitFieldValue(oldDesc);
            changedDescription = limitFieldValue(newString);
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
                                    format(
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

          String oldString = StringUtils.defaultString(changeItem.getString("oldstring"));
          if (("Fix Version".equals(field) || "Component".equals(field) || "Version".equals(field))
              && changeItem.get("oldvalue") != null
              && changeItem.get("newvalue") == null) {
            title = i18nResolver.getText("ru.mail.jira.plugins.myteam.notification.deleted", title);
            appendFieldOldAndNewValue(sb, title, shieldText(oldString), "", true);
            continue;
          }

          if (fieldManager.isNavigableField(field)) {
            final NavigableField navigableField = fieldManager.getNavigableField(field);
            if (navigableField != null) {
              if (navigableField instanceof UserField) {
                appendFieldOldAndNewValue(
                    sb,
                    title,
                    formatUser(
                        userManager.getUserByKey(changeItem.getString("oldvalue")),
                        "common.words.anonymous",
                        true),
                    formatUser(
                        userManager.getUserByKey(changeItem.getString("newvalue")),
                        "common.words.anonymous",
                        true),
                    true);
                continue;
              }
              newString = navigableField.prettyPrintChangeHistory(newString);
              oldString = navigableField.prettyPrintChangeHistory(oldString);
            }
          }

          appendFieldOldAndNewValue(sb, title, shieldText(oldString), shieldText(newString), true);
        }

        if (!isBlank(changedDescription))
          if (oldOrNewDescHasComplexFormatting) {
            sb.append("\n\n")
                .append(makeMyteamMarkdownFromJira(changedDescription, useMentionFormat));
          } else {
            sb.append("\n\n");
            if (isBlank(oldDesc)) {
              sb.append(
                  StringUtils.defaultString(
                      makeMyteamMarkdownFromJira(changedDescription, useMentionFormat), ""));
            } else {
              String diff =
                  buildDiffString(
                      StringUtils.defaultString(
                          makeMyteamMarkdownFromJira(oldDesc, useMentionFormat), ""),
                      StringUtils.defaultString(
                          makeMyteamMarkdownFromJira(changedDescription, useMentionFormat), ""));
              sb.append(diff)
                  .append("\n\n")
                  .append(">___")
                  .append(
                      i18nResolver.getText(
                          "ru.mail.jira.plugins.myteam.notify.diff.description.field"))
                  .append("___");
            }
          }
      } catch (Exception e) {
        SentryClient.capture(e);
      }
    return sb.toString();
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
            changedDescription = limitFieldValue(newString);
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
                                    format(
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
                sb.append("\n")
                    .append(title)
                    .append(": ")
                    .append(
                        formatUser(
                            userManager.getUserByKey(changeItem.getString("newvalue")),
                            "common.words.anonymous",
                            true));
                continue;
              }
              newString = navigableField.prettyPrintChangeHistory(newString);
            }
          }

          appendField(sb, title, shieldText(newString), true);
        }

        if (!isBlank(changedDescription))
          sb.append("\n\n")
              .append(makeMyteamMarkdownFromJira(changedDescription, useMentionFormat));
      } catch (Exception e) {
        SentryClient.capture(e);
      }
    return sb.toString();
  }

  public String limitFieldValue(@NotNull String value) {
    if (value.length() < DISPLAY_FIELD_CHARS_LIMIT) return value;
    return value.substring(0, DISPLAY_FIELD_CHARS_LIMIT) + "...";
  }

  public String formatLinks(final String messageBody, final LinksInMessage linksInMessage) {
    if (linksInMessage.getLinks().isEmpty()) {
      return messageBody;
    }

    final List<Link> links = linksInMessage.getLinks();
    String formattedMesssageBody = messageBody;
    for (final Link link : links) {
      if (link.isMasked()) {
        formattedMesssageBody =
            formattedMesssageBody.replaceFirst(
                Pattern.quote(link.getMask()),
                format(DESCRIPTION_MARKDOWN_MASKED_LINK_TEMPLATE, link.getMask(), link.getLink()));
      } else {
        formattedMesssageBody =
            formattedMesssageBody.replaceFirst(
                link.getLink(),
                format(DESCRIPTION_MARKDOWN_UNMASKED_LINK_TEMPLATE, link.getLink()));
      }
    }

    return formattedMesssageBody;
  }

  private static boolean checkDescOnComplexJiraWikiStyles(String oldDesc, String newDesc) {
    boolean oldOrNewDescHasComplexFormatting = false;
    for (Pattern pattern : patternsToExcludeDescriptionForDiff) {
      if (checkDescOnComplexJiraWikiStyles(oldDesc, pattern)
          || checkDescOnComplexJiraWikiStyles(newDesc, pattern)) {
        oldOrNewDescHasComplexFormatting = true;
        break;
      }
    }

    return oldOrNewDescHasComplexFormatting;
  }

  private static boolean checkDescOnComplexJiraWikiStyles(String inputText, Pattern pattern) {
    return pattern.matcher(inputText).find();
  }

  private static void appendFieldOldAndNewValue(
      StringBuilder sb,
      @Nullable String title,
      @Nullable String oldValue,
      @Nullable String value,
      boolean appendEmpty) {
    if (appendEmpty || !isBlank(value) || !isBlank(oldValue)) {
      if (sb.length() == 0) {
        sb.append("\n");
      }
      if (oldValue == null) {
        oldValue = "";
      }
      if (value == null) {
        value = "";
      }
      sb.append("\n").append(title).append(": ");
      if (oldValue.isEmpty()) {
        sb.append("*" + value + "*");
      } else if (value.isEmpty()) {
        sb.append("~" + oldValue + "~");
      } else {
        sb.append(oldValue.isEmpty() ? "" : "~" + oldValue + "~")
            .append(" ")
            .append(value.isEmpty() ? "" : "*" + value + "*");
      }
    }
  }

  /**
   * @see com.atlassian.jira.web.action.util.DiffViewRenderer#getUnifiedHtml(DiffViewBean, String,
   *     String) reused method for converting JIRA markup to VKTeams markup
   * @param firstString original string
   * @param secondString new string
   * @return diff inline
   */
  @NotNull
  private static String buildDiffString(@NotNull String firstString, @NotNull String secondString) {
    DiffViewBean diffViewBean = DiffViewBean.createWordLevelDiff(firstString, secondString);
    StringBuilder diffBuilder = new StringBuilder();
    String removedStyle = "~";
    String addedStyle = "*";
    for (DiffChunk chunk : diffViewBean.getUnifiedChunks()) {
      if (chunk.getType() == DiffType.CHANGED_WORDS) {
        WordChunk wordChunk = (WordChunk) chunk;
        for (CharacterChunk charChunk : wordChunk.getCharacterChunks()) {

          if (charChunk.getType() == DiffType.DELETED_CHARACTERS) {
            diffBuilder.append(removedStyle);
          } else if (charChunk.getType() == DiffType.ADDED_CHARACTERS) {
            diffBuilder.append(addedStyle);
          }
          diffBuilder.append(print(charChunk.getText()));
          if (charChunk.getType() == DiffType.DELETED_CHARACTERS) {
            diffBuilder.append(removedStyle);
          } else if (charChunk.getType() == DiffType.ADDED_CHARACTERS) {
            diffBuilder.append(addedStyle);
          }
        }
      } else if (chunk
          .getType()
          .getClassName()
          .equals("unchanged")) // probably dead code, but copied from old line-diff.vm
      {
        diffBuilder.append(print(chunk.getText()));
      } else {

        if (chunk.getType() == DiffType.DELETED_WORDS) {
          diffBuilder.append(removedStyle);
        } else if (chunk.getType() == DiffType.ADDED_WORDS) {
          diffBuilder.append(addedStyle);
        }
        diffBuilder.append(print(chunk.getText()));
        if (chunk.getType() == DiffType.DELETED_WORDS) {
          diffBuilder.append(removedStyle);
        } else if (chunk.getType() == DiffType.ADDED_WORDS) {
          diffBuilder.append(addedStyle);
        }
      }
      diffBuilder.append(" "); // ensure visual spacing
    }

    return diffBuilder.toString().replaceAll("<br>", "\n").replaceAll("&nbsp;", "");
  }

  /**
   * @see com.atlassian.jira.web.action.util.DiffViewRenderer#print(String) reused method for
   *     converting JIRA markup to VKTeams markup
   * @param string some string to build general diff string
   * @return input string without newline chars
   */
  private static String print(String string) {
    // 1. Encode html special characters so they look nice
    string = TextUtils.htmlEncode(string, false);

    // 2. replace new line characters with a line break html character
    string = string.replaceAll("(\\r\\n|\\n|\\r)", "<br>");

    // 3. preserve whitespace blocks up greater than 2 up to a threshold
    // (MAX_WHITESPACE_PRESERVATION_LENGTH)
    StringBuffer result = new StringBuffer();
    Matcher matcher = Pattern.compile("(\\s{2,})").matcher(string);

    while (matcher.find()) {
      String match = matcher.group(0);
      int length = match.length();

      // replace up to MAX_WHITESPACE_PRESERVATION_LENGTH with "&nbsp;" and then the rest are normal
      // spaces
      String replacement =
          StringUtils.repeat("&nbsp;", Math.min(length, MAX_WHITESPACE_PRESERVATION_LENGTH))
              + StringUtils.repeat(" ", Math.max(0, length - MAX_WHITESPACE_PRESERVATION_LENGTH));

      // replace the result while maintaining correct index's for the next replacement
      matcher.appendReplacement(result, replacement);
    }

    return matcher.appendTail(result).toString();
  }

  @NotNull
  private static Map<Long, String> getEventTypeMap() {
    Map<Long, String> eventTypeMap = new HashMap<>();
    eventTypeMap.put(EventType.ISSUE_CREATED_ID, "created");
    eventTypeMap.put(EventType.ISSUE_ASSIGNED_ID, "assigned");
    eventTypeMap.put(EventType.ISSUE_RESOLVED_ID, "resolved");
    eventTypeMap.put(EventType.ISSUE_CLOSED_ID, "closed");
    eventTypeMap.put(EventType.ISSUE_COMMENTED_ID, "commented");
    eventTypeMap.put(EventType.ISSUE_COMMENT_EDITED_ID, "commentEdited");
    eventTypeMap.put(EventType.ISSUE_REOPENED_ID, "reopened");
    eventTypeMap.put(EventType.ISSUE_DELETED_ID, "deleted");
    eventTypeMap.put(EventType.ISSUE_MOVED_ID, "moved");
    eventTypeMap.put(EventType.ISSUE_WORKLOGGED_ID, "worklogged");
    eventTypeMap.put(EventType.ISSUE_WORKSTARTED_ID, "workStarted");
    eventTypeMap.put(EventType.ISSUE_WORKSTOPPED_ID, "workStopped");
    eventTypeMap.put(EventType.ISSUE_WORKLOG_UPDATED_ID, "worklogUpdated");
    eventTypeMap.put(EventType.ISSUE_WORKLOG_DELETED_ID, "worklogDeleted");
    return eventTypeMap;
  }

  @NotNull
  private static List<Pattern> initPatternsToExcludeDescriptionForDiff() {
    final List<Pattern> patterns = new ArrayList<>();
    // quotes in desc
    patterns.add(Pattern.compile("\\{[Qq]uote}([^+]*?)\\{[Qq]uote}", Pattern.MULTILINE));
    // code block in desc
    patterns.add(Pattern.compile("\\{[Cc]ode:([a-z]+?)}([^+]*?)\\{[Cc]ode}", Pattern.MULTILINE));
    patterns.add(Pattern.compile("\\{[Cc]ode}([^+]*?)\\{[Cc]ode}", Pattern.MULTILINE));
    // strikethrough text in desc
    patterns.add(Pattern.compile("(^|\\s)-([^- \\n].*?[^- \\n])-($|\\s|\\.)"));
    // bold text in desc
    patterns.add(Pattern.compile("(^|\\s)\\*([^* \\n].*?[^* \\n])\\*($|\\s|\\.)"));
    // ordered/unordered lists
    patterns.add(Pattern.compile("^((?:#|-|\\+|\\*)+) (.*)$", Pattern.MULTILINE));

    return Collections.unmodifiableList(patterns);
  }
}
