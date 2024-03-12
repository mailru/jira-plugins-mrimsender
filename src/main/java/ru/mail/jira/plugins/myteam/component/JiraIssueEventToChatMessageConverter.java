/* (C)2023 */
package ru.mail.jira.plugins.myteam.component;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static ru.mail.jira.plugins.myteam.commons.Utils.shieldText;

import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.type.EventType;
import com.atlassian.jira.issue.AttachmentManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.attachment.Attachment;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.NavigableField;
import com.atlassian.jira.issue.fields.UserField;
import com.atlassian.jira.issue.resolution.Resolution;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.message.I18nResolver;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
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
import ru.mail.jira.plugins.myteam.component.event.JiraEventToChatMessageConverter;
import ru.mail.jira.plugins.myteam.bot.listeners.IssueEventRecipient;
import ru.mail.jira.plugins.myteam.component.event.issue.IssueEventToChatMessageData;

@Component
@Slf4j
public class JiraIssueEventToChatMessageConverter
    implements JiraEventToChatMessageConverter<IssueEventToChatMessageData> {
  private static final List<Pattern> PATTERNS_TO_EXCLUDE_DESCRIPTION_FOR_DIFF =
      initPatternsToExcludeDescriptionForDiff();
  private static final Map<Long, String> EVENT_TYPE_MAP = getEventTypeMap();

  private static final String I18N_PART_KEY = "ru.mail.jira.plugins.myteam.notification.";
  private static final String MENTION_UPDATED_ISSUE_I18N_KEY =
      "ru.mail.jira.plugins.myteam.notification.updated.and.mentioned";

  private static final String DEFAULT_EVENT_I18N_KEY =
      "ru.mail.jira.plugins.myteam.notification.updated";

  private final MessageFormatter messageFormatter;
  private final JiraMarkdownToChatMarkdownConverter jiraMarkdownToChatMarkdownConverter;
  private final DiffFieldChatMessageGenerator diffFieldChatMessageGenerator;
  private final AttachmentManager attachmentManager;
  private final ApplicationProperties applicationProperties;
  private final I18nResolver i18nResolver;
  private final I18nHelper i18nHelper;
  private final FieldManager fieldManager;
  private final UserManager userManager;

  @Autowired
  public JiraIssueEventToChatMessageConverter(
      final MessageFormatter messageFormatter,
      final JiraMarkdownToChatMarkdownConverter jiraMarkdownToChatMarkdownConverter,
      final DiffFieldChatMessageGenerator diffFieldChatMessageGenerator,
      @ComponentImport final AttachmentManager attachmentManager,
      @ComponentImport final ApplicationProperties applicationProperties,
      @ComponentImport final I18nResolver i18nResolver,
      @ComponentImport final I18nHelper i18nHelper,
      @ComponentImport final FieldManager fieldManager,
      @ComponentImport final UserManager userManager) {
    this.messageFormatter = messageFormatter;
    this.jiraMarkdownToChatMarkdownConverter = jiraMarkdownToChatMarkdownConverter;
    this.diffFieldChatMessageGenerator = diffFieldChatMessageGenerator;
    this.attachmentManager = attachmentManager;
    this.applicationProperties = applicationProperties;
    this.i18nResolver = i18nResolver;
    this.i18nHelper = i18nHelper;
    this.fieldManager = fieldManager;
    this.userManager = userManager;
  }

  @Override
  public String convert(final IssueEventToChatMessageData issueEventToChatMessageData) {
    final IssueEvent issueEvent = issueEventToChatMessageData.getIssueEvent();
    final Issue issue = issueEvent.getIssue();
    final ApplicationUser user = issueEvent.getUser();
    final StringBuilder sb = new StringBuilder();

    IssueEventRecipient issueEventRecipient = issueEventToChatMessageData.getIssueEventRecipient();
    ApplicationUser recipient = issueEventRecipient.getRecipient();
    final boolean useMentionFormat = !recipient.equals(user);

    final Long eventTypeId = issueEvent.getEventTypeId();

    final String i18nKey = I18N_PART_KEY + EVENT_TYPE_MAP.getOrDefault(eventTypeId, "updated");

    if (EventType.ISSUE_ASSIGNED_ID.equals(eventTypeId)) {
      sb.append(appendAssigneeOnIssueAssignedEvent(issue, i18nKey, user, useMentionFormat));
    } else if (EventType.ISSUE_RESOLVED_ID.equals(eventTypeId)
        || EventType.ISSUE_CLOSED_ID.equals(eventTypeId)) {
      sb.append(appendStringOnIssueResoledOrCloseEvent(issue, i18nKey, user, useMentionFormat));
    } else if (EventType.ISSUE_COMMENTED_ID.equals(eventTypeId)
        || EventType.ISSUE_COMMENT_EDITED_ID.equals(eventTypeId)) {
      sb.append(
          appendStringOnIssueCommentedOrCommentEditedEvent(
              issueEvent, i18nKey, user, useMentionFormat, issueEventRecipient));
      return sb.toString();
    } else {
      String issueLink =
          messageFormatter.markdownTextLink(
              issue.getKey(), messageFormatter.createIssueLink(issue.getKey()));
      if (DEFAULT_EVENT_I18N_KEY.equals(i18nKey) && issueEventRecipient.isMentioned()) {
        sb.append(
            i18nResolver.getText(
                MENTION_UPDATED_ISSUE_I18N_KEY,
                messageFormatter.formatUser(user, "common.words.anonymous", useMentionFormat),
                issueLink));
      } else {
        sb.append(
            i18nResolver.getText(
                i18nKey,
                messageFormatter.formatUser(user, "common.words.anonymous", useMentionFormat),
                issueLink));
      }
    }
    sb.append("\n").append(shieldText(issue.getSummary()));

    if (issueEvent.getWorklog() != null && !isBlank(issueEvent.getWorklog().getComment())) {
      sb.append("\n\n").append(shieldText(issueEvent.getWorklog().getComment()));
    }

    if (EventType.ISSUE_CREATED_ID.equals(eventTypeId)) {
      sb.append(
          messageFormatter.formatSystemFields(
              issueEventRecipient.getRecipient(), issue, useMentionFormat));
    }

    sb.append(
        formatChangeLogWithDiff(
            issueEvent.getChangeLog(),
            EventType.ISSUE_ASSIGNED_ID.equals(eventTypeId),
            useMentionFormat));
    if (issueEvent.getComment() != null && !isBlank(issueEvent.getComment().getBody())) {
      if (EventType.ISSUE_COMMENTED_ID.equals(eventTypeId)
          && issueEvent
              .getComment()
              .getBody()
              .contains("[~" + issueEventRecipient.getRecipient().getName() + "]")) {
        // do not send message when applicationUserWrapper mentioned in comment
        return "";
      }

      sb.append("\n\n")
          .append(
              jiraMarkdownToChatMarkdownConverter.makeMyteamMarkdownFromJira(
                  issueEvent.getComment().getBody(), useMentionFormat));
    }
    return sb.toString();
  }


  @Nullable
  public String formatEventWithDiff(final IssueEventRecipient issueEventRecipient, final IssueEvent issueEvent) {
    return convert(IssueEventToChatMessageData.of(issueEventRecipient, issueEvent));
  }

  @NotNull
  private String appendAssigneeOnIssueAssignedEvent(
      final Issue issue,
      final String i18nKey,
      final ApplicationUser user,
      final boolean useMentionFormat) {
    String issueLink =
        messageFormatter.markdownTextLink(
            issue.getKey(), messageFormatter.createIssueLink(issue.getKey()));
    return i18nResolver.getText(
        i18nKey,
        messageFormatter.formatUser(user, "common.words.anonymous", useMentionFormat),
        issueLink,
        messageFormatter.formatUser(
            issue.getAssignee(), "common.concepts.unassigned", useMentionFormat));
  }

  @NotNull
  private String appendStringOnIssueResoledOrCloseEvent(
      final Issue issue,
      final String i18nKey,
      final ApplicationUser user,
      final boolean useMentionFormat) {
    final Resolution resolution = issue.getResolution();
    final String issueLink =
        messageFormatter.markdownTextLink(
            issue.getKey(), messageFormatter.createIssueLink(issue.getKey()));
    return i18nResolver.getText(
        i18nKey,
        messageFormatter.formatUser(user, "common.words.anonymous", useMentionFormat),
        issueLink,
        resolution != null
            ? resolution.getNameTranslation(i18nHelper)
            : i18nResolver.getText("common.resolution.unresolved"));
  }

  @Nullable
  private String appendStringOnIssueCommentedOrCommentEditedEvent(
      final IssueEvent issueEvent,
      final String i18nKey,
      final ApplicationUser user,
      final boolean useMentionFormat,
      final IssueEventRecipient issueEventRecipient) {
    String messageText;
    if (EventType.ISSUE_COMMENT_EDITED_ID.equals(issueEvent.getEventTypeId())) {
      messageText = appendEditedCommentWithDiff(issueEvent, useMentionFormat);
    } else {
      messageText =
          jiraMarkdownToChatMarkdownConverter.makeMyteamMarkdownFromJira(
              issueEvent.getComment().getBody(), useMentionFormat);
    }
    String definedI18nKeyOnMentionedCommented = i18nKey;

    if (useMentionFormat && issueEventRecipient.isMentioned()) {
      definedI18nKeyOnMentionedCommented =
          Objects.equals(issueEvent.getEventTypeId(), EventType.ISSUE_COMMENTED_ID)
              ? "ru.mail.jira.plugins.myteam.notification.commented.and.mentioned"
              : "ru.mail.jira.plugins.myteam.notification.commented.edited.and.mentioned";
    }
    final Issue issue = issueEvent.getIssue();

    return i18nResolver.getText(
        definedI18nKeyOnMentionedCommented,
        messageFormatter.getIssueLink(issue.getKey()),
        shieldText(issue.getSummary()),
        messageFormatter.formatUser(user, "common.words.anonymous", useMentionFormat),
        messageFormatter.getShieldCommentLink(issue, issueEvent.getComment()),
        messageText);
  }

  @Nullable
  private String appendEditedCommentWithDiff(
      @NotNull final IssueEvent issueEvent, final boolean useMentionFormat) {
    final String messageText;
    final Object origCommentObj =
        issueEvent.getParams().get(CommentManager.EVENT_ORIGINAL_COMMENT_PARAMETER);
    if (origCommentObj instanceof Comment) {
      messageText =
          appendEditedCommentWithDiff(issueEvent, useMentionFormat, (Comment) origCommentObj);
    } else {
      messageText =
          jiraMarkdownToChatMarkdownConverter.makeMyteamMarkdownFromJira(
              issueEvent.getComment().getBody(), useMentionFormat);
    }
    return messageText;
  }

  @Nullable
  private String appendEditedCommentWithDiff(
      @NotNull final IssueEvent issueEvent,
      final boolean useMentionFormat,
      final Comment origCommentObj) {
    final String messageText;
    final String originalCommentBody = origCommentObj.getBody();
    final String newCommentBody = issueEvent.getComment().getBody();

    if (checkTextOnComplexJiraWikiStyles(originalCommentBody, newCommentBody)) {
      messageText =
          jiraMarkdownToChatMarkdownConverter.makeMyteamMarkdownFromJira(
              newCommentBody, useMentionFormat);
    } else {
      messageText =
          diffFieldChatMessageGenerator.buildDiffString(
              StringUtils.defaultString(
                  jiraMarkdownToChatMarkdownConverter.makeMyteamMarkdownFromJira(
                      originalCommentBody, useMentionFormat)),
              StringUtils.defaultString(
                  jiraMarkdownToChatMarkdownConverter.makeMyteamMarkdownFromJira(
                      newCommentBody, useMentionFormat)));
    }
    return messageText;
  }

  private String formatChangeLogWithDiff(
      final GenericValue changeLog,
      final boolean ignoreAssigneeField,
      final boolean useMentionFormat) {
    if (changeLog == null) {
      return "";
    }
    final StringBuilder sb = new StringBuilder();
    try {
      String jiraBaseUrl = applicationProperties.getString(APKeys.JIRA_BASEURL);
      String changedDescription = null;
      String oldDesc = null;
      boolean oldOrNewDescHasComplexFormatting = false;
      for (GenericValue changeItem : changeLog.getRelated("ChildChangeItem")) {
        String field = StringUtils.defaultString(changeItem.getString("field"));
        String newString = StringUtils.defaultString(changeItem.getString("newstring"));

        if ("description".equals(field)) {
          oldDesc = StringUtils.defaultString(changeItem.getString("oldstring"));
          oldOrNewDescHasComplexFormatting = checkTextOnComplexJiraWikiStyles(oldDesc, newString);
          oldDesc = messageFormatter.limitFieldValue(oldDesc);
          changedDescription = messageFormatter.limitFieldValue(newString);
          continue;
        }

        if ("WorklogTimeSpent".equals(field)
            || "WorklogId".equals(field)
            || ("assignee".equals(field) && ignoreAssigneeField)) {
          continue;
        }

        if ("Attachment".equals(field)) {
          sb.append("\n\n").append(appendAttachmentFromChangedItem(changeItem, jiraBaseUrl));
          continue;
        }

        String title = field;
        if (!"custom".equalsIgnoreCase(changeItem.getString("fieldtype"))) {
          title = i18nResolver.getText("issue.field." + field.replaceAll(" ", "").toLowerCase());
        }

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
              appendNavigableUserField(sb, title, changeItem);
              continue;
            }
            newString = navigableField.prettyPrintChangeHistory(newString);
            oldString = navigableField.prettyPrintChangeHistory(oldString);
          }
        }

        appendFieldOldAndNewValue(sb, title, shieldText(oldString), shieldText(newString), true);
      }

      if (!isBlank(changedDescription)) {
        sb.append("\n\n")
            .append(
                appendDescription(
                    oldOrNewDescHasComplexFormatting,
                    oldDesc,
                    changedDescription,
                    useMentionFormat));
      }
    } catch (Exception e) {
      SentryClient.capture(e);
    }
    return sb.toString();
  }

  @NotNull
  private String appendAttachmentFromChangedItem(
      final GenericValue changeItem, String jiraBaseUrl) {
    String attachmentId = changeItem.getString("newvalue");
    if (StringUtils.isNotEmpty(attachmentId)) {
      Attachment attachment = attachmentManager.getAttachment(Long.valueOf(attachmentId));
      return tryAppendNotNullAttachment(jiraBaseUrl, attachment, changeItem);
    } else {
      return i18nResolver.getText(
          "ru.mail.jira.plugins.myteam.notification.attachmentDeleted",
          shieldText(changeItem.getString("oldstring")));
    }
  }

  @NotNull
  private String tryAppendNotNullAttachment(
      final String jiraBaseUrl, final Attachment attachment, final GenericValue changeItem) {
    try {
      return messageFormatter.markdownTextLink(
          attachment.getFilename(),
          new URI(
                  format(
                      "%s/secure/attachment/%d/%s",
                      jiraBaseUrl, attachment.getId(), attachment.getFilename()),
                  false,
                  StandardCharsets.UTF_8.toString())
              .getEscapedURI());
    } catch (URIException e) {
      SentryClient.capture(e);
      log.error(
          "Can't find attachment id:{} name:{}",
          changeItem.getString("newvalue"),
          changeItem.getString("newstring"),
          e);
      return "";
    }
  }

  private String appendDescription(
      final boolean oldOrNewDescHasComplexFormatting,
      @Nullable final String oldDesc,
      final String changedDescription,
      final boolean useMentionFormat) {
    if (oldOrNewDescHasComplexFormatting) {
      return StringUtils.defaultString(
          jiraMarkdownToChatMarkdownConverter.makeMyteamMarkdownFromJira(
              changedDescription, useMentionFormat));
    } else {
      if (isBlank(oldDesc)) {
        return StringUtils.defaultString(
            jiraMarkdownToChatMarkdownConverter.makeMyteamMarkdownFromJira(
                changedDescription, useMentionFormat),
            "");
      } else {
        return appendDescriptionWithDiff(oldDesc, changedDescription, useMentionFormat);
      }
    }
  }

  private void appendNavigableUserField(
      final StringBuilder sb, final String title, final GenericValue changeItem) {
    appendFieldOldAndNewValue(
        sb,
        title,
        messageFormatter.formatUser(
            userManager.getUserByKey(changeItem.getString("oldvalue")),
            "common.words.anonymous",
            false),
        messageFormatter.formatUser(
            userManager.getUserByKey(changeItem.getString("newvalue")),
            "common.words.anonymous",
            true),
        true);
  }

  @NotNull
  private String appendDescriptionWithDiff(
      final String oldDesc, final String changedDescription, final boolean useMentionFormat) {
    String diff =
        diffFieldChatMessageGenerator.buildDiffString(
            StringUtils.defaultString(
                jiraMarkdownToChatMarkdownConverter.makeMyteamMarkdownFromJira(
                    oldDesc, useMentionFormat),
                ""),
            StringUtils.defaultString(
                jiraMarkdownToChatMarkdownConverter.makeMyteamMarkdownFromJira(
                    changedDescription, useMentionFormat),
                ""));
    return diff
        + "\n\n"
        + ">___"
        + i18nResolver.getText("ru.mail.jira.plugins.myteam.notify.diff.description.field")
        + "___";
  }

  private static boolean checkTextOnComplexJiraWikiStyles(
      final String oldDesc, final String newDesc) {
    boolean oldOrNewDescHasComplexFormatting = false;
    for (final Pattern pattern : PATTERNS_TO_EXCLUDE_DESCRIPTION_FOR_DIFF) {
      if (checkTextOnComplexJiraWikiStyles(oldDesc, pattern)
          || checkTextOnComplexJiraWikiStyles(newDesc, pattern)) {
        oldOrNewDescHasComplexFormatting = true;
        break;
      }
    }

    return oldOrNewDescHasComplexFormatting;
  }

  private static boolean checkTextOnComplexJiraWikiStyles(
      final String inputText, final Pattern pattern) {
    return pattern.matcher(inputText).find();
  }

  private void appendFieldOldAndNewValue(
      final StringBuilder sb,
      @NotNull final String title,
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
      sb.append("\n").append(shieldText(title)).append(": ");
      if (oldValue.isEmpty()) {
        sb.append(diffFieldChatMessageGenerator.markNewValue(value));
      } else if (value.isEmpty()) {
        sb.append(diffFieldChatMessageGenerator.markOldValue(oldValue));
      } else {
        sb.append(oldValue.isEmpty() ? "" : diffFieldChatMessageGenerator.markOldValue(oldValue))
            .append(" ")
            .append(value.isEmpty() ? "" : diffFieldChatMessageGenerator.markNewValue(value));
      }
    }
  }

  @NotNull
  private static Map<Long, String> getEventTypeMap() {
    final Map<Long, String> eventTypeMap = new HashMap<>();
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
    return Collections.unmodifiableMap(eventTypeMap);
  }

  private static List<Pattern> initPatternsToExcludeDescriptionForDiff() {
    final List<Pattern> patterns = new ArrayList<>();
    // quotes in desc
    patterns.add(JiraMarkdownTextPattern.QUOTES_PATTERN);
    // code block in desc
    patterns.add(JiraMarkdownTextPattern.CODE_BLOCK_PATTERN_1);
    patterns.add(JiraMarkdownTextPattern.CODE_BLOCK_PATTERN_2);
    // strikethrough text in desc
    patterns.add(JiraMarkdownTextPattern.STRIKETHROUGH_PATTERN);
    // bold text in desc
    patterns.add(JiraMarkdownTextPattern.BOLD_PATTERN);
    // ordered/unordered lists
    patterns.add(JiraMarkdownTextPattern.MULTILEVEL_NUMBERED_LIST_PATTERN);
    // macro panel
    patterns.add(JiraMarkdownTextPattern.PANEL_PATTERN);

    return Collections.unmodifiableList(patterns);
  }
}
