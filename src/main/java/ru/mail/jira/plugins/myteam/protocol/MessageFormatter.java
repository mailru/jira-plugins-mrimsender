/* (C)2020 */
package ru.mail.jira.plugins.myteam.protocol;

import static java.util.stream.Collectors.joining;

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
import com.atlassian.jira.issue.AttachmentManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueConstant;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.attachment.Attachment;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.NavigableField;
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
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.MessageSet;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.message.I18nResolver;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.lang3.StringUtils;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.GenericValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.myteam.bitbucket.dto.*;
import ru.mail.jira.plugins.myteam.bitbucket.dto.utils.*;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;
import ru.mail.jira.plugins.myteam.rulesengine.models.ButtonRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.models.CommandRuleType;

@Slf4j
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
  private final ProjectComponentManager projectComponentManager;
  private final VersionManager versionManager;
  private final UserManager userManager;
  private final AttachmentManager attachmentManager;

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
      @ComponentImport ProjectComponentManager projectComponentManager,
      @ComponentImport VersionManager versionManager,
      @ComponentImport UserManager userManager,
      @ComponentImport AttachmentManager attachmentManager) {
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
    this.projectComponentManager = projectComponentManager;
    this.versionManager = versionManager;
    this.userManager = userManager;
    this.attachmentManager = attachmentManager;
  }

  private String formatUser(ApplicationUser user, String messageKey, boolean mention) {
    if (user != null) {
      if (mention) {
        return "@\\[" + shieldText(user.getEmailAddress()) + "\\]";
      }

      return "["
          + shieldText(user.getDisplayName())
          + "]("
          + shieldText("https://u.internal.myteam.mail.ru/profile/" + user.getEmailAddress())
          + ")";
    } else return i18nHelper.getText(messageKey);
  }

  private String formatBitbucketUser(UserDto user, String messageKey, boolean mention) {
    if (user != null) {
      if (mention) {
        return "@\\[" + shieldText(user.getEmailAddress()) + "\\]";
      }

      return markdownTextLink(
          user.getDisplayName(),
          "https://u.internal.myteam.mail.ru/profile/" + user.getEmailAddress());
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

  public String formatSystemFields(
      ApplicationUser recipient, Issue issue, boolean useMentionFormat) {
    StringBuilder sb = new StringBuilder();
    Locale recipientLocale = localeManager.getLocaleFor(recipient);
    if (issue.getIssueType() != null)
      appendField(
          sb,
          i18nResolver.getRawText(recipientLocale, "issue.field.issuetype"),
          shieldText(issue.getIssueType().getNameTranslation(i18nHelper)),
          false);

    appendField(
        sb,
        i18nResolver.getRawText(recipientLocale, "issue.field.affectsversions"),
        issue.getAffectedVersions());
    appendField(
        sb,
        i18nResolver.getRawText(recipientLocale, "issue.field.assignee"),
        formatUser(issue.getAssignee(), "common.concepts.unassigned", useMentionFormat),
        false);

    appendField(
        sb,
        i18nResolver.getRawText(recipientLocale, "issue.field.components"),
        issue.getComponents());

    if (issue.getCreated() != null)
      appendField(
          sb,
          i18nResolver.getRawText(recipientLocale, "issue.field.created"),
          dateTimeFormatter
              .forUser(recipient)
              .withStyle(DateTimeStyle.COMPLETE)
              .format(issue.getCreated()),
          false);

    if (issue.getDueDate() != null)
      appendField(
          sb,
          i18nResolver.getRawText(recipientLocale, "issue.field.duedate"),
          dateTimeFormatter
              .forUser(recipient)
              .withSystemZone()
              .withStyle(DateTimeStyle.DATE)
              .format(issue.getDueDate()),
          false);

    appendField(
        sb,
        i18nResolver.getRawText(recipientLocale, "issue.field.environment"),
        shieldText(issue.getEnvironment()),
        false);
    appendField(
        sb,
        i18nResolver.getRawText(recipientLocale, "issue.field.fixversions"),
        issue.getFixVersions());
    appendField(
        sb, i18nResolver.getRawText(recipientLocale, "issue.field.labels"), issue.getLabels());
    appendField(
        sb,
        i18nResolver.getRawText(recipientLocale, "issue.field.priority"),
        shieldText(formatPriority(issue.getPriority())),
        false);
    appendField(
        sb,
        i18nResolver.getRawText(recipientLocale, "issue.field.reporter"),
        formatUser(issue.getReporter(), "common.concepts.no.reporter", useMentionFormat),
        false);

    if (issue.getSecurityLevelId() != null) {
      IssueSecurityLevel issueSecurityLevel =
          issueSecurityLevelManager.getSecurityLevel(issue.getSecurityLevelId());
      String value = issueSecurityLevel.getName();
      if (!StringUtils.isBlank(issueSecurityLevel.getDescription()))
        value += " " + issueSecurityLevel.getDescription();
      appendField(
          sb,
          i18nResolver.getRawText(recipientLocale, "issue.field.securitylevel"),
          shieldText(value),
          false);
    }
    appendField(
        sb,
        i18nResolver.getRawText(recipientLocale, "issue.field.attachment"),
        issue.getAttachments());

    if (!StringUtils.isBlank(issue.getDescription())) {
      sb.append("\n\n")
          .append(makeMyteamMarkdownFromJira(issue.getDescription(), useMentionFormat));
    }

    return sb.toString();
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
        log.error("Unable to create text link for issueKey: {}, url:{}", text, url);
      }
    }
    return url;
  }

  private String getHostLink(LinksDto linksDto) {
    List<LinkDto> selfLinkDto = linksDto.getSelf();
    if (selfLinkDto != null && selfLinkDto.size() > 0 && selfLinkDto.get(0).getHref() != null) {
      String[] selfLinkDtoParts = StringUtils.split(selfLinkDto.get(0).getHref(), "/");
      if (selfLinkDtoParts.length >= 3)
        return String.join("/", selfLinkDtoParts[0], selfLinkDtoParts[1], selfLinkDtoParts[2]);
    }
    return null;
  }

  private String makePullRequestLink(
      String hostLink, String projectKey, String repoName, long pullRequestId) {
    return hostLink
        + "/projects/"
        + projectKey
        + "/repos/"
        + repoName
        + "/pull-requests/"
        + pullRequestId
        + "/overview";
  }

  private String makeCommitLink(
      String hostLink, String projectKey, String repoName, String CommitHash) {
    return hostLink + "/projects/" + projectKey + "/repos/" + repoName + "/commits/" + CommitHash;
  }

  private String makeRepoLink(String hostLink, String projectKey, String projectName) {
    return hostLink + "/projects/" + projectKey + "/repos/" + projectName + "/browse";
  }

  private String makeBranchLink(
      String hostLink, String projectKey, String projectName, String branchId) {
    return hostLink
        + "/projects/"
        + projectKey
        + "/repos/"
        + projectName
        + "/browse?at="
        + branchId;
  }

  private String makeReviewersText(
      List<PullRequestParticipantDto> reviewers,
      String recipientEmailAddress,
      Locale recipientLocale) {
    return reviewers.stream()
        .map(
            reviewer -> {
              if (reviewer.getUser().getEmailAddress().equals(recipientEmailAddress)) {
                return i18nResolver.getText(
                    recipientLocale,
                    "ru.mail.jira.plugins.myteam.bitbucket.notification.pr.reviewers.you");
              } else {
                return formatBitbucketUser(reviewer.getUser(), "common.words.anonymous", true);
              }
            })
        .collect(joining(","));
  }

  private String makeChangedReviewersText(
      List<PullRequestParticipantDto> reviewers,
      String recipientEmailAddress,
      Locale recipientLocale) {

    return reviewers.stream()
        .map(
            reviewer -> {
              if (reviewer.getEmailAddress().equals(recipientEmailAddress)) {
                return i18nResolver.getText(
                    recipientLocale,
                    "ru.mail.jira.plugins.myteam.bitbucket.notification.pr.reviewers.you");
              } else {
                return markdownTextLink(
                    reviewer.getName(),
                    "https://u.internal.myteam.mail.ru/profile/" + reviewer.getEmailAddress());
              }
            })
        .collect(joining(", "));
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
            Pattern.compile("\\{[Cc]ode}([^+]*?)\\{[Cc]ode}", Pattern.MULTILINE),
            (input) -> "±`±`±`" + input.group(1) + "±`±`±`");
    inputText =
        convertToMarkdown(
            inputText,
            Pattern.compile("\\{[Cc]ode:([a-z]+?)}([^+]*?)\\{[Cc]ode}", Pattern.MULTILINE),
            (input) -> "±`±`±`" + input.group(1) + " " + input.group(2) + "±`±`±`");
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
                  return "±@±[" + shieldText(mentionUser.getEmailAddress()) + "±]";
                }
                return "±["
                    + shieldText(mentionUser.getDisplayName())
                    + "±]±("
                    + shieldText(
                        "https://u.internal.myteam.mail.ru/profile/"
                            + mentionUser.getEmailAddress())
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
            Pattern.compile("\\[([^|?\n]+)\\|(.+?)]"),
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

  public String shieldText(String inputDescription) {
    if (inputDescription == null) {
      return null;
    }
    StringBuilder result = new StringBuilder();
    char[] arrayFromInput = inputDescription.toCharArray();
    for (char c : arrayFromInput) {
      switch (c) {
        case '*':
          result.append("\\*");
          break;
        case '_':
          result.append("\\_");
          break;
        case '~':
          result.append("\\~");
          break;
        case '`':
          result.append("\\`");
          break;
        case '-':
          result.append("\\-");
          break;
        case '>':
          result.append("\\>");
          break;
        case '\\':
          result.append("\\\\");
          break;
        case '{':
          result.append("\\{");
          break;
        case '}':
          result.append("\\}");
          break;
        case '[':
          result.append("\\[");
          break;
        case ']':
          result.append("\\]");
          break;
        case '(':
          result.append("\\(");
          break;
        case ')':
          result.append("\\)");
          break;
        case '#':
          result.append("\\#");
          break;
        case '+':
          result.append("\\+");
          break;
        case '.':
          result.append("\\.");
          break;
        case '!':
          result.append("\\!");
          break;
        default:
          result.append(c);
      }
    }
    return result.toString();
  }

  private String formatChangeLog(
      GenericValue changeLog,
      boolean ignoreAssigneeField,
      Locale recipientLocale,
      boolean useMentionFormat) {
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
            Attachment attachment =
                attachmentManager.getAttachment(Long.valueOf(changeItem.getString("newvalue")));
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
              log.error(
                  "Can't find attachment id:{} name:{}",
                  changeItem.getString("newvalue"),
                  changeItem.getString("newstring"),
                  e);
            }
            continue;
          }
          if (!"custom".equalsIgnoreCase(changeItem.getString("fieldtype")))
            title =
                i18nResolver.getText(
                    recipientLocale, "issue.field." + field.replaceAll(" ", "").toLowerCase());

          if (("Fix Version".equals(field) || "Component".equals(field) || "Version".equals(field))
              && changeItem.get("oldvalue") != null
              && changeItem.get("newvalue") == null) {
            newString = changeItem.getString("oldstring");
            title =
                i18nResolver.getText(
                    recipientLocale, "ru.mail.jira.plugins.myteam.notification.deleted", title);
          }

          if (fieldManager.isNavigableField(field)) {
            final NavigableField navigableField = fieldManager.getNavigableField(field);
            if (navigableField != null)
              newString = navigableField.prettyPrintChangeHistory(newString, i18nHelper);
          }

          appendField(sb, title, shieldText(newString), true);
        }

        if (!StringUtils.isBlank(changedDescription))
          sb.append("\n\n")
              .append(makeMyteamMarkdownFromJira(changedDescription, useMentionFormat));
      } catch (GenericEntityException ignored) {
        // ignore
      }
    return sb.toString();
  }

  public String createIssueLink(Issue issue) {
    return String.format(
        "%s/browse/%s", applicationProperties.getString(APKeys.JIRA_BASEURL), issue.getKey());
  }

  public String formatBitbucketEvent(ApplicationUser recipient, BitbucketEventDto bitbucketEvent)
      throws UnsupportedEncodingException {
    StringBuilder sb = new StringBuilder();
    boolean useMentionFormat = true;
    // TODO Вернуть когда починят меншены
    // = !recipient.getEmailAddress().toLowerCase().equals(actor.getEmailAddress().toLowerCase())
    Locale recipientLocale = localeManager.getLocaleFor(recipient);
    // Repository events тут
    if (bitbucketEvent instanceof RepositoryPush) {
      RepositoryPush repositoryPush = (RepositoryPush) bitbucketEvent;
      UserDto actor = repositoryPush.getActor();

      RepositoryDto repositoryDto = repositoryPush.getRepository();
      String hostLink = getHostLink(repositoryDto.getLinks());
      List<ChangeDto> changesDTOs = repositoryPush.getChanges();
      try {
        sb.append(
            i18nResolver.getText(
                recipientLocale,
                "ru.mail.jira.plugins.myteam.bitbucket.notification.pushed",
                formatBitbucketUser(actor, "common.words.anonymous", useMentionFormat),
                markdownTextLink(
                    repositoryDto.getName(),
                    makeRepoLink(
                        hostLink, repositoryDto.getProject().getKey(), repositoryDto.getName())),
                markdownTextLink(
                    i18nResolver.getText(
                        recipientLocale, "ru.mail.jira.plugins.myteam.bitbucket.commit"),
                    makeCommitLink(
                        hostLink,
                        repositoryDto.getProject().getKey(),
                        repositoryDto.getName(),
                        changesDTOs.get(0).getToHash()))));
      } catch (NullPointerException exception) {
        log.error(
            "Error: Can't notify user {} about RepositoryPush.",
            recipient.getEmailAddress(),
            exception);
      }
    }
    if (bitbucketEvent instanceof RepositoryModified) {
      RepositoryModified repositoryModified = (RepositoryModified) bitbucketEvent;
      UserDto actor = repositoryModified.getActor();
      RepositoryDto newRepo = repositoryModified.getNewRepo();
      String hostLink = getHostLink(newRepo.getLinks());
      try {
        sb.append(
            i18nResolver.getText(
                recipientLocale,
                "ru.mail.jira.plugins.myteam.bitbucket.notification.modified",
                formatBitbucketUser(actor, "common.words.anonymous", useMentionFormat),
                repositoryModified.getOldRepo().getName(),
                markdownTextLink(
                    repositoryModified.getNewRepo().getName(),
                    makeRepoLink(
                        hostLink,
                        repositoryModified.getNewRepo().getProject().getKey(),
                        repositoryModified.getNewRepo().getName()))));
      } catch (NullPointerException exception) {
        log.error(
            "Error: Can't notify user {} about RepositoryModified.",
            recipient.getEmailAddress(),
            exception);
      }
    }
    if (bitbucketEvent instanceof RepositoryForked) {
      RepositoryForked repositoryForked = (RepositoryForked) bitbucketEvent;
      UserDto actor = repositoryForked.getActor();
      String hostLink = getHostLink(repositoryForked.getRepository().getLinks());

      try {

        sb.append(
            i18nResolver.getText(
                recipientLocale,
                "ru.mail.jira.plugins.myteam.bitbucket.notification.forked",
                formatBitbucketUser(actor, "common.words.anonymous", useMentionFormat),
                markdownTextLink(
                    repositoryForked.getRepository().getOrigin().getName(),
                    makeRepoLink(
                        hostLink,
                        repositoryForked.getRepository().getOrigin().getProject().getKey(),
                        repositoryForked.getRepository().getOrigin().getName())),
                markdownTextLink(
                    repositoryForked.getRepository().getName(),
                    makeRepoLink(
                        hostLink,
                        repositoryForked.getRepository().getProject().getKey(),
                        repositoryForked.getRepository().getName()))));
      } catch (NullPointerException exception) {
        log.error(
            "Error: Can't notify user {} about RepositoryForked.",
            recipient.getEmailAddress(),
            exception);
      }
    }
    if (bitbucketEvent instanceof RepositoryMirrorSynchronized) {
      RepositoryMirrorSynchronized repositoryMirrorSynchronized =
          (RepositoryMirrorSynchronized) bitbucketEvent;
      List<LinkDto> linkDtos = repositoryMirrorSynchronized.getRepository().getLinks().getSelf();

      try {
        sb.append(
            i18nResolver.getText(
                recipientLocale,
                "ru.mail.jira.plugins.myteam.bitbucket.notification.mirror",
                repositoryMirrorSynchronized.getMirrorServer().getName(),
                repositoryMirrorSynchronized.getMirrorServer().getId(),
                markdownTextLink(
                    repositoryMirrorSynchronized.getRepository().getName(),
                    makeRepoLink(
                        linkDtos.get(0).getHref(),
                        repositoryMirrorSynchronized.getProjectKey(),
                        repositoryMirrorSynchronized.getRepository().getName())),
                repositoryMirrorSynchronized.getSyncType()));
      } catch (NullPointerException exception) {
        log.error(
            "Error: Can't notify user {} about RepositoryMirrorSynchronized.",
            recipient.getEmailAddress(),
            exception);
      }
    }
    if (bitbucketEvent instanceof RepositoryCommitCommentCreated) {
      RepositoryCommitCommentCreated repositoryCommitCommentCreated =
          (RepositoryCommitCommentCreated) bitbucketEvent;
      UserDto actor = repositoryCommitCommentCreated.getActor();
      String hostLink = getHostLink(repositoryCommitCommentCreated.getRepository().getLinks());

      try {
        sb.append(
            i18nResolver.getText(
                recipientLocale,
                "ru.mail.jira.plugins.myteam.bitbucket.notification.comment.create",
                formatBitbucketUser(actor, "common.words.anonymous", useMentionFormat),
                markdownTextLink(
                    i18nResolver.getText(
                        recipientLocale, "ru.mail.jira.plugins.myteam.bitbucket.commit"),
                    makeCommitLink(
                        hostLink,
                        repositoryCommitCommentCreated.getProjectKey(),
                        repositoryCommitCommentCreated.getRepository().getName(),
                        repositoryCommitCommentCreated.getCommitHash())),
                shieldText(repositoryCommitCommentCreated.getComment().getText())));
      } catch (NullPointerException exception) {
        log.error(
            "Error: Can't notify user {} about RepositoryCommitCommentCreated.",
            recipient.getEmailAddress(),
            exception);
      }
    }
    if (bitbucketEvent instanceof RepositoryCommitCommentEdited) {
      RepositoryCommitCommentEdited repositoryCommitCommentEdited =
          (RepositoryCommitCommentEdited) bitbucketEvent;
      UserDto actor = repositoryCommitCommentEdited.getActor();
      String hostLink = getHostLink(repositoryCommitCommentEdited.getRepository().getLinks());
      try {
        sb.append(
            i18nResolver.getText(
                recipientLocale,
                "ru.mail.jira.plugins.myteam.bitbucket.notification.comment.edit",
                formatBitbucketUser(actor, "common.words.anonymous", useMentionFormat),
                markdownTextLink(
                    i18nResolver.getText(
                        recipientLocale, "ru.mail.jira.plugins.myteam.bitbucket.commite"),
                    makeCommitLink(
                        hostLink,
                        repositoryCommitCommentEdited.getProjectKey(),
                        repositoryCommitCommentEdited.getRepository().getName(),
                        repositoryCommitCommentEdited.getCommitHash())),
                shieldText(repositoryCommitCommentEdited.getPreviousComment()),
                shieldText(repositoryCommitCommentEdited.getComment().getText())));
      } catch (NullPointerException exception) {
        log.error(
            "Error: Can't notify user {} about RepositoryCommitCommentEdited.",
            recipient.getEmailAddress(),
            exception);
      }
    }
    if (bitbucketEvent instanceof RepositoryCommitCommentDeleted) {
      RepositoryCommitCommentDeleted repositoryCommitCommentDeleted =
          (RepositoryCommitCommentDeleted) bitbucketEvent;
      UserDto actor = repositoryCommitCommentDeleted.getActor();
      String hostLink = getHostLink(repositoryCommitCommentDeleted.getRepository().getLinks());
      try {
        sb.append(
            i18nResolver.getText(
                recipientLocale,
                "ru.mail.jira.plugins.myteam.bitbucket.notification.comment.delete",
                formatBitbucketUser(actor, "common.words.anonymous", useMentionFormat),
                markdownTextLink(
                    i18nResolver.getText(
                        recipientLocale, "ru.mail.jira.plugins.myteam.bitbucket.commite"),
                    makeCommitLink(
                        hostLink,
                        repositoryCommitCommentDeleted.getProjectKey(),
                        repositoryCommitCommentDeleted.getRepository().getName(),
                        repositoryCommitCommentDeleted.getCommitHash())),
                shieldText(repositoryCommitCommentDeleted.getComment().getText())));
      } catch (NullPointerException exception) {
        log.error(
            "Error: Can't notify user {} about RepositoryCommitCommentDeleted.",
            recipient.getEmailAddress(),
            exception);
      }
    }

    // PR events
    if (bitbucketEvent instanceof PullRequestOpened) {
      PullRequestOpened pullRequestOpened = (PullRequestOpened) bitbucketEvent;
      UserDto actor = pullRequestOpened.getActor();
      String hostLink = getHostLink(pullRequestOpened.getPullRequest().getLinks());
      try {
        sb.append(
            i18nResolver.getText(
                recipientLocale,
                "ru.mail.jira.plugins.myteam.bitbucket.notification.pr.opened",
                formatBitbucketUser(actor, "common.words.anonymous", useMentionFormat),
                markdownTextLink(
                    i18nResolver.getText(
                        recipientLocale, "ru.mail.jira.plugins.myteam.bitbucket.pr"),
                    makePullRequestLink(
                        hostLink,
                        pullRequestOpened.getProjectKey(),
                        pullRequestOpened.getPullRequest().getToRef().getRepository().getName(),
                        pullRequestOpened.getPullRequest().getId())),
                markdownTextLink(
                    pullRequestOpened.getPullRequest().getToRef().getRepository().getName(),
                    makeRepoLink(
                        hostLink,
                        pullRequestOpened
                            .getPullRequest()
                            .getToRef()
                            .getRepository()
                            .getProject()
                            .getKey(),
                        pullRequestOpened.getPullRequest().getToRef().getRepository().getName())),
                markdownTextLink(
                    pullRequestOpened.getPullRequest().getFromRef().getId(),
                    makeBranchLink(
                        hostLink,
                        pullRequestOpened
                            .getPullRequest()
                            .getFromRef()
                            .getRepository()
                            .getProject()
                            .getKey(),
                        pullRequestOpened.getPullRequest().getFromRef().getRepository().getName(),
                        pullRequestOpened.getPullRequest().getFromRef().getId())),
                markdownTextLink(
                    pullRequestOpened.getPullRequest().getToRef().getId(),
                    makeBranchLink(
                        hostLink,
                        pullRequestOpened
                            .getPullRequest()
                            .getToRef()
                            .getRepository()
                            .getProject()
                            .getKey(),
                        pullRequestOpened.getPullRequest().getToRef().getRepository().getName(),
                        pullRequestOpened.getPullRequest().getToRef().getId())),
                pullRequestOpened.getPullRequest().getTitle(),
                shieldText(pullRequestOpened.getPullRequest().getDescription()),
                makeReviewersText(
                    pullRequestOpened.getPullRequest().getReviewers(),
                    recipient.getEmailAddress(),
                    recipientLocale)));
      } catch (NullPointerException exception) {
        log.error(
            "Error: Can't notify user {} about opened pull request.",
            recipient.getEmailAddress(),
            exception);
      }
    }
    if (bitbucketEvent instanceof PullRequestModified) {
      PullRequestModified pullRequestModified = (PullRequestModified) bitbucketEvent;
      UserDto actor = pullRequestModified.getActor();
      String hostLink = getHostLink(pullRequestModified.getPullRequest().getLinks());

      byte counter = 1;
      StringBuilder changes = new StringBuilder();

      if (pullRequestModified.getPreviousTitle() != null
          && pullRequestModified.getPullRequest() != null
          && pullRequestModified.getPullRequest().getTitle() != null
          && !pullRequestModified
              .getPullRequest()
              .getTitle()
              .equals(pullRequestModified.getPreviousTitle())) {
        changes.append(
            i18nResolver.getText(
                recipientLocale,
                "ru.mail.jira.plugins.myteam.bitbucket.notification.pr.modified.title",
                String.valueOf(counter) + '.',
                pullRequestModified.getPreviousTitle(),
                pullRequestModified.getPullRequest().getTitle()));
        counter++;
      }
      if (pullRequestModified.getPreviousDescription() != null
          && pullRequestModified.getPullRequest() != null
          && pullRequestModified.getPullRequest().getDescription() != null
          && !pullRequestModified
              .getPullRequest()
              .getDescription()
              .equals(pullRequestModified.getPreviousDescription())) {
        changes.append(
            i18nResolver.getText(
                recipientLocale,
                "ru.mail.jira.plugins.myteam.bitbucket.notification.pr.modified.description",
                String.valueOf(counter) + '.',
                shieldText(pullRequestModified.getPreviousDescription()),
                shieldText(pullRequestModified.getPullRequest().getDescription())));
        counter++;
      }
      if (pullRequestModified.getPreviousTarget() != null
          && pullRequestModified.getPreviousTarget().getId() != null
          && pullRequestModified.getPullRequest() != null
          && pullRequestModified.getPullRequest().getToRef() != null
          && pullRequestModified.getPullRequest().getToRef().getId() != null
          && !pullRequestModified
              .getPreviousTarget()
              .getId()
              .equals(pullRequestModified.getPullRequest().getToRef().getId())) {
        changes.append(
            i18nResolver.getText(
                recipientLocale,
                "ru.mail.jira.plugins.myteam.bitbucket.notification.pr.modified.target",
                String.valueOf(counter) + '.',
                markdownTextLink(
                    pullRequestModified.getPreviousTarget().getId(),
                    makeBranchLink(
                        hostLink,
                        pullRequestModified.getProjectKey(),
                        pullRequestModified.getPullRequest().getToRef().getRepository().getName(),
                        pullRequestModified.getPreviousTarget().getId())),
                markdownTextLink(
                    pullRequestModified.getPullRequest().getToRef().getId(),
                    makeBranchLink(
                        hostLink,
                        pullRequestModified.getProjectKey(),
                        pullRequestModified.getPullRequest().getToRef().getRepository().getName(),
                        pullRequestModified.getPullRequest().getToRef().getId()))));
        counter++;
      }
      if (counter == 1) return "Empty PullRequestModified event";
      try {
        sb.append(
            i18nResolver.getText(
                recipientLocale,
                "ru.mail.jira.plugins.myteam.bitbucket.notification.pr.modified",
                formatBitbucketUser(actor, "common.words.anonymous", useMentionFormat),
                markdownTextLink(
                    i18nResolver.getText(
                        recipientLocale, "ru.mail.jira.plugins.myteam.bitbucket.pr"),
                    makePullRequestLink(
                        hostLink,
                        pullRequestModified.getProjectKey(),
                        pullRequestModified.getPullRequest().getToRef().getRepository().getName(),
                        pullRequestModified.getPullRequest().getId())),
                markdownTextLink(
                    pullRequestModified.getPullRequest().getToRef().getRepository().getName(),
                    makeRepoLink(
                        hostLink,
                        pullRequestModified
                            .getPullRequest()
                            .getToRef()
                            .getRepository()
                            .getProject()
                            .getKey(),
                        pullRequestModified.getPullRequest().getToRef().getRepository().getName())),
                pullRequestModified.getPullRequest().getTitle(),
                changes.toString()));
      } catch (NullPointerException exception) {
        log.error(
            "Error: Can't notify user {} about modified pull request.",
            recipient.getEmailAddress(),
            exception);
      }
    }
    if (bitbucketEvent instanceof PullRequestReviewersUpdated) {
      PullRequestReviewersUpdated pullRequestReviewersUpdated =
          (PullRequestReviewersUpdated) bitbucketEvent;
      UserDto actor = pullRequestReviewersUpdated.getActor();
      String hostLink = getHostLink(pullRequestReviewersUpdated.getPullRequest().getLinks());
      try {
        sb.append(
            i18nResolver.getText(
                recipientLocale,
                "ru.mail.jira.plugins.myteam.bitbucket.notification.pr.reviewers.updated",
                formatBitbucketUser(actor, "common.words.anonymous", useMentionFormat),
                markdownTextLink(
                    i18nResolver.getText(
                        recipientLocale, "ru.mail.jira.plugins.myteam.bitbucket.pre"),
                    makePullRequestLink(
                        hostLink,
                        pullRequestReviewersUpdated.getProjectKey(),
                        pullRequestReviewersUpdated
                            .getPullRequest()
                            .getToRef()
                            .getRepository()
                            .getName(),
                        pullRequestReviewersUpdated.getPullRequest().getId())),
                markdownTextLink(
                    pullRequestReviewersUpdated
                        .getPullRequest()
                        .getToRef()
                        .getRepository()
                        .getName(),
                    makeRepoLink(
                        hostLink,
                        pullRequestReviewersUpdated
                            .getPullRequest()
                            .getToRef()
                            .getRepository()
                            .getProject()
                            .getKey(),
                        pullRequestReviewersUpdated
                            .getPullRequest()
                            .getToRef()
                            .getRepository()
                            .getName())),
                pullRequestReviewersUpdated.getPullRequest().getTitle()));

        if (pullRequestReviewersUpdated.getAddedReviewers() != null
            && pullRequestReviewersUpdated.getAddedReviewers().size() > 0) {
          sb.append(
              i18nResolver.getText(
                  recipientLocale,
                  "ru.mail.jira.plugins.myteam.bitbucket.notification.pr.reviewersupdated.added",
                  makeChangedReviewersText(
                      pullRequestReviewersUpdated.getAddedReviewers(),
                      recipient.getEmailAddress(),
                      recipientLocale)));
        }
        if (pullRequestReviewersUpdated.getRemovedReviewers() != null
            && pullRequestReviewersUpdated.getRemovedReviewers().size() > 0) {
          sb.append(
              i18nResolver.getText(
                  recipientLocale,
                  "ru.mail.jira.plugins.myteam.bitbucket.notification.pr.reviewersupdated.removed",
                  makeChangedReviewersText(
                      pullRequestReviewersUpdated.getRemovedReviewers(),
                      recipient.getEmailAddress(),
                      recipientLocale)));
        }
      } catch (NullPointerException exception) {
        log.error(
            "Error: Can't notify user {} about PullRequestReviewersUpdated.",
            recipient.getEmailAddress(),
            exception);
      }
    }
    if (bitbucketEvent instanceof PullRequestApprovedByReviewer) {
      PullRequestApprovedByReviewer pullRequestApprovedByReviewer =
          (PullRequestApprovedByReviewer) bitbucketEvent;
      UserDto actor = pullRequestApprovedByReviewer.getActor();
      String hostLink = getHostLink(pullRequestApprovedByReviewer.getPullRequest().getLinks());
      try {
        sb.append(
            i18nResolver.getText(
                recipientLocale,
                "ru.mail.jira.plugins.myteam.bitbucket.notification.pr.approved",
                formatBitbucketUser(actor, "common.words.anonymous", useMentionFormat),
                markdownTextLink(
                    i18nResolver.getText(
                        recipientLocale, "ru.mail.jira.plugins.myteam.bitbucket.pr"),
                    makePullRequestLink(
                        hostLink,
                        pullRequestApprovedByReviewer.getProjectKey(),
                        pullRequestApprovedByReviewer
                            .getPullRequest()
                            .getToRef()
                            .getRepository()
                            .getName(),
                        pullRequestApprovedByReviewer.getPullRequest().getId())),
                pullRequestApprovedByReviewer.getPullRequest().getTitle(),
                markdownTextLink(
                    pullRequestApprovedByReviewer
                        .getPullRequest()
                        .getToRef()
                        .getRepository()
                        .getName(),
                    makeRepoLink(
                        hostLink,
                        pullRequestApprovedByReviewer
                            .getPullRequest()
                            .getToRef()
                            .getRepository()
                            .getProject()
                            .getKey(),
                        pullRequestApprovedByReviewer
                            .getPullRequest()
                            .getToRef()
                            .getRepository()
                            .getName())),
                shieldText(pullRequestApprovedByReviewer.getPullRequest().getDescription())));
      } catch (NullPointerException exception) {
        log.error(
            "Error: Can't notify user {} about PullRequestApprovedByReviewer.",
            recipient.getEmailAddress(),
            exception);
      }
    }
    if (bitbucketEvent instanceof PullRequestUnapprovedByReviewer) {
      PullRequestUnapprovedByReviewer pullRequestUnapprovedByReviewer =
          (PullRequestUnapprovedByReviewer) bitbucketEvent;
      UserDto actor = pullRequestUnapprovedByReviewer.getActor();
      String hostLink = getHostLink(pullRequestUnapprovedByReviewer.getPullRequest().getLinks());
      try {
        sb.append(
            i18nResolver.getText(
                recipientLocale,
                "ru.mail.jira.plugins.myteam.bitbucket.notification.pr.unapproved",
                formatBitbucketUser(actor, "common.words.anonymous", useMentionFormat),
                markdownTextLink(
                    i18nResolver.getText(
                        recipientLocale, "ru.mail.jira.plugins.myteam.bitbucket.pra"),
                    makePullRequestLink(
                        hostLink,
                        pullRequestUnapprovedByReviewer.getProjectKey(),
                        pullRequestUnapprovedByReviewer
                            .getPullRequest()
                            .getToRef()
                            .getRepository()
                            .getName(),
                        pullRequestUnapprovedByReviewer.getPullRequest().getId())),
                pullRequestUnapprovedByReviewer.getPullRequest().getTitle(),
                markdownTextLink(
                    pullRequestUnapprovedByReviewer
                        .getPullRequest()
                        .getToRef()
                        .getRepository()
                        .getName(),
                    makeRepoLink(
                        hostLink,
                        pullRequestUnapprovedByReviewer
                            .getPullRequest()
                            .getToRef()
                            .getRepository()
                            .getProject()
                            .getKey(),
                        pullRequestUnapprovedByReviewer
                            .getPullRequest()
                            .getToRef()
                            .getRepository()
                            .getName())),
                shieldText(pullRequestUnapprovedByReviewer.getPullRequest().getDescription())));
      } catch (NullPointerException exception) {
        log.error(
            "Error: Can't notify user {} about PullRequestUnapprovedByReviewer.",
            recipient.getEmailAddress(),
            exception);
      }
    }
    if (bitbucketEvent instanceof PullRequestNeedsWorkByReviewer) {
      PullRequestNeedsWorkByReviewer pullRequestNeedsWorkByReviewer =
          (PullRequestNeedsWorkByReviewer) bitbucketEvent;
      UserDto actor = pullRequestNeedsWorkByReviewer.getActor();
      String hostLink = getHostLink(pullRequestNeedsWorkByReviewer.getPullRequest().getLinks());
      try {
        sb.append(
            i18nResolver.getText(
                recipientLocale,
                "ru.mail.jira.plugins.myteam.bitbucket.notification.pr.needswork",
                formatBitbucketUser(actor, "common.words.anonymous", useMentionFormat),
                markdownTextLink(
                    i18nResolver.getText(
                        recipientLocale, "ru.mail.jira.plugins.myteam.bitbucket.pr"),
                    makePullRequestLink(
                        hostLink,
                        pullRequestNeedsWorkByReviewer.getProjectKey(),
                        pullRequestNeedsWorkByReviewer
                            .getPullRequest()
                            .getToRef()
                            .getRepository()
                            .getName(),
                        pullRequestNeedsWorkByReviewer.getPullRequest().getId())),
                pullRequestNeedsWorkByReviewer.getPullRequest().getTitle(),
                markdownTextLink(
                    pullRequestNeedsWorkByReviewer
                        .getPullRequest()
                        .getToRef()
                        .getRepository()
                        .getName(),
                    makeRepoLink(
                        hostLink,
                        pullRequestNeedsWorkByReviewer
                            .getPullRequest()
                            .getToRef()
                            .getRepository()
                            .getProject()
                            .getKey(),
                        pullRequestNeedsWorkByReviewer
                            .getPullRequest()
                            .getToRef()
                            .getRepository()
                            .getName()))));
      } catch (NullPointerException exception) {
        log.error(
            "Error: Can't notify user {} about PullRequestNeedsWorkByReviewer.",
            recipient.getEmailAddress(),
            exception);
      }
    }
    if (bitbucketEvent instanceof PullRequestMerged) {
      PullRequestMerged pullRequestMerged = (PullRequestMerged) bitbucketEvent;
      UserDto actor = pullRequestMerged.getActor();
      String hostLink = getHostLink(pullRequestMerged.getPullRequest().getLinks());
      try {
        sb.append(
            i18nResolver.getText(
                recipientLocale,
                "ru.mail.jira.plugins.myteam.bitbucket.notification.pr.merged",
                formatBitbucketUser(actor, "common.words.anonymous", useMentionFormat),
                markdownTextLink(
                    i18nResolver.getText(
                        recipientLocale, "ru.mail.jira.plugins.myteam.bitbucket.pr"),
                    makePullRequestLink(
                        hostLink,
                        pullRequestMerged.getProjectKey(),
                        pullRequestMerged.getPullRequest().getToRef().getRepository().getName(),
                        pullRequestMerged.getPullRequest().getId())),
                pullRequestMerged.getPullRequest().getTitle(),
                markdownTextLink(
                    pullRequestMerged.getPullRequest().getToRef().getRepository().getName(),
                    makeRepoLink(
                        hostLink,
                        pullRequestMerged
                            .getPullRequest()
                            .getToRef()
                            .getRepository()
                            .getProject()
                            .getKey(),
                        pullRequestMerged.getPullRequest().getToRef().getRepository().getName()))));
      } catch (NullPointerException exception) {
        log.error(
            "Error: Can't notify user {} about PullRequestMerged.",
            recipient.getEmailAddress(),
            exception);
      }
    }
    if (bitbucketEvent instanceof PullRequestDeclined) {
      PullRequestDeclined pullRequestDeclined = (PullRequestDeclined) bitbucketEvent;
      UserDto actor = pullRequestDeclined.getActor();
      String hostLink = getHostLink(pullRequestDeclined.getPullRequest().getLinks());
      try {
        sb.append(
            i18nResolver.getText(
                recipientLocale,
                "ru.mail.jira.plugins.myteam.bitbucket.notification.pr.declined",
                formatBitbucketUser(actor, "common.words.anonymous", useMentionFormat),
                markdownTextLink(
                    i18nResolver.getText(
                        recipientLocale, "ru.mail.jira.plugins.myteam.bitbucket.pr"),
                    makePullRequestLink(
                        hostLink,
                        pullRequestDeclined.getProjectKey(),
                        pullRequestDeclined.getPullRequest().getToRef().getRepository().getName(),
                        pullRequestDeclined.getPullRequest().getId())),
                pullRequestDeclined.getPullRequest().getTitle(),
                markdownTextLink(
                    pullRequestDeclined.getPullRequest().getToRef().getRepository().getName(),
                    makeRepoLink(
                        hostLink,
                        pullRequestDeclined
                            .getPullRequest()
                            .getToRef()
                            .getRepository()
                            .getProject()
                            .getKey(),
                        pullRequestDeclined
                            .getPullRequest()
                            .getToRef()
                            .getRepository()
                            .getName()))));
      } catch (NullPointerException exception) {
        log.error(
            "Error: Can't notify user {} about PullRequestDeclined.",
            recipient.getEmailAddress(),
            exception);
      }
    }
    if (bitbucketEvent instanceof PullRequestDeleted) {
      PullRequestDeleted pullRequestDeleted = (PullRequestDeleted) bitbucketEvent;
      UserDto actor = pullRequestDeleted.getActor();
      String hostLink = getHostLink(pullRequestDeleted.getPullRequest().getLinks());
      try {
        sb.append(
            i18nResolver.getText(
                recipientLocale,
                "ru.mail.jira.plugins.myteam.bitbucket.notification.pr.deleted",
                formatBitbucketUser(actor, "common.words.anonymous", useMentionFormat),
                markdownTextLink(
                    i18nResolver.getText(
                        recipientLocale, "ru.mail.jira.plugins.myteam.bitbucket.pr"),
                    makePullRequestLink(
                        hostLink,
                        pullRequestDeleted.getProjectKey(),
                        pullRequestDeleted.getPullRequest().getToRef().getRepository().getName(),
                        pullRequestDeleted.getPullRequest().getId())),
                pullRequestDeleted.getPullRequest().getTitle(),
                markdownTextLink(
                    pullRequestDeleted.getPullRequest().getToRef().getRepository().getName(),
                    makeRepoLink(
                        hostLink,
                        pullRequestDeleted
                            .getPullRequest()
                            .getToRef()
                            .getRepository()
                            .getProject()
                            .getKey(),
                        pullRequestDeleted
                            .getPullRequest()
                            .getToRef()
                            .getRepository()
                            .getName()))));
      } catch (NullPointerException exception) {
        log.error(
            "Error: Can't notify user {} about PullRequestDeleted.",
            recipient.getEmailAddress(),
            exception);
      }
    }
    if (bitbucketEvent instanceof PullRequestCommentAdded) {
      PullRequestCommentAdded pullRequestCommentAdded = (PullRequestCommentAdded) bitbucketEvent;
      UserDto actor = pullRequestCommentAdded.getActor();
      String hostLink =
          getHostLink(
              pullRequestCommentAdded.getPullRequest().getToRef().getRepository().getLinks());
      try {
        sb.append(
            i18nResolver.getText(
                recipientLocale,
                "ru.mail.jira.plugins.myteam.bitbucket.notification.pr.comment.added",
                formatBitbucketUser(actor, "common.words.anonymous", useMentionFormat),
                markdownTextLink(
                    i18nResolver.getText(
                        recipientLocale, "ru.mail.jira.plugins.myteam.bitbucket.pr"),
                    makePullRequestLink(
                        hostLink,
                        pullRequestCommentAdded.getProjectKey(),
                        pullRequestCommentAdded
                            .getPullRequest()
                            .getToRef()
                            .getRepository()
                            .getName(),
                        pullRequestCommentAdded.getPullRequest().getId())),
                pullRequestCommentAdded.getPullRequest().getTitle(),
                markdownTextLink(
                    pullRequestCommentAdded.getPullRequest().getToRef().getRepository().getName(),
                    makeRepoLink(
                        hostLink,
                        pullRequestCommentAdded
                            .getPullRequest()
                            .getToRef()
                            .getRepository()
                            .getProject()
                            .getKey(),
                        pullRequestCommentAdded
                            .getPullRequest()
                            .getToRef()
                            .getRepository()
                            .getName())),
                pullRequestCommentAdded.getComment().getText()));
      } catch (NullPointerException exception) {
        log.error(
            "Error: Can't notify user {} about PullRequestCommentAdded.",
            recipient.getEmailAddress(),
            exception);
      }
    }
    if (bitbucketEvent instanceof PullRequestCommentEdited) {
      PullRequestCommentEdited pullRequestCommentEdited = (PullRequestCommentEdited) bitbucketEvent;
      UserDto actor = pullRequestCommentEdited.getActor();
      String hostLink =
          getHostLink(
              pullRequestCommentEdited.getPullRequest().getToRef().getRepository().getLinks());

      try {
        sb.append(
            i18nResolver.getText(
                recipientLocale,
                "ru.mail.jira.plugins.myteam.bitbucket.notification.pr.comment.edited",
                formatBitbucketUser(actor, "common.words.anonymous", useMentionFormat),
                markdownTextLink(
                    i18nResolver.getText(
                        recipientLocale, "ru.mail.jira.plugins.myteam.bitbucket.pry"),
                    makePullRequestLink(
                        hostLink,
                        pullRequestCommentEdited.getProjectKey(),
                        pullRequestCommentEdited
                            .getPullRequest()
                            .getToRef()
                            .getRepository()
                            .getName(),
                        pullRequestCommentEdited.getPullRequest().getId())),
                pullRequestCommentEdited.getPullRequest().getTitle(),
                markdownTextLink(
                    pullRequestCommentEdited.getPullRequest().getToRef().getRepository().getName(),
                    makeRepoLink(
                        hostLink,
                        pullRequestCommentEdited
                            .getPullRequest()
                            .getToRef()
                            .getRepository()
                            .getProject()
                            .getKey(),
                        pullRequestCommentEdited
                            .getPullRequest()
                            .getToRef()
                            .getRepository()
                            .getName())),
                pullRequestCommentEdited.getPreviousCommentText(),
                pullRequestCommentEdited.getComment().getText()));
      } catch (NullPointerException exception) {
        log.error(
            "Error: Can't notify user {} about PullRequestCommentEdited.",
            recipient.getEmailAddress(),
            exception);
      }
    }
    if (bitbucketEvent instanceof PullRequestCommentDeleted) {
      PullRequestCommentDeleted pullRequestCommentDeleted =
          (PullRequestCommentDeleted) bitbucketEvent;
      UserDto actor = pullRequestCommentDeleted.getActor();
      String hostLink =
          getHostLink(
              pullRequestCommentDeleted.getPullRequest().getToRef().getRepository().getLinks());
      try {
        sb.append(
            i18nResolver.getText(
                recipientLocale,
                "ru.mail.jira.plugins.myteam.bitbucket.notification.pr.comment.deleted",
                formatBitbucketUser(actor, "common.words.anonymous", useMentionFormat),
                markdownTextLink(
                    i18nResolver.getText(
                        recipientLocale, "ru.mail.jira.plugins.myteam.bitbucket.pry"),
                    makePullRequestLink(
                        hostLink,
                        pullRequestCommentDeleted.getProjectKey(),
                        pullRequestCommentDeleted
                            .getPullRequest()
                            .getToRef()
                            .getRepository()
                            .getName(),
                        pullRequestCommentDeleted.getPullRequest().getId())),
                pullRequestCommentDeleted.getPullRequest().getTitle(),
                markdownTextLink(
                    pullRequestCommentDeleted.getPullRequest().getToRef().getRepository().getName(),
                    makeRepoLink(
                        hostLink,
                        pullRequestCommentDeleted
                            .getPullRequest()
                            .getToRef()
                            .getRepository()
                            .getProject()
                            .getKey(),
                        pullRequestCommentDeleted
                            .getPullRequest()
                            .getToRef()
                            .getRepository()
                            .getName())),
                pullRequestCommentDeleted.getComment().getText()));
      } catch (NullPointerException exception) {
        log.error(
            "Error: Can't notify user {} about PullRequestCommentEdited.",
            recipient.getEmailAddress(),
            exception);
      }
    }
    sb.append("\n");
    return sb.toString();
  }

  public String formatEvent(ApplicationUser recipient, IssueEvent issueEvent) {
    Issue issue = issueEvent.getIssue();
    ApplicationUser user = issueEvent.getUser();
    String issueLink = markdownTextLink(issue.getKey(), createIssueLink(issue));
    Locale recipientLocale = localeManager.getLocaleFor(recipient);
    StringBuilder sb = new StringBuilder();

    boolean useMentionFormat = !recipient.equals(user);
    Long eventTypeId = issueEvent.getEventTypeId();
    if (EventType.ISSUE_CREATED_ID.equals(eventTypeId)) {
      sb.append(
          i18nResolver.getText(
              recipientLocale,
              "ru.mail.jira.plugins.myteam.notification.created",
              formatUser(user, "common.words.anonymous", useMentionFormat),
              issueLink));
    } else if (EventType.ISSUE_UPDATED_ID.equals(eventTypeId)
        || EventType.ISSUE_COMMENT_DELETED_ID.equals(eventTypeId)
        || EventType.ISSUE_GENERICEVENT_ID.equals(eventTypeId)) {
      // {0} обновил запрос [ {1} ]
      sb.append(
          i18nResolver.getText(
              recipientLocale,
              "ru.mail.jira.plugins.myteam.notification.updated",
              formatUser(user, "common.words.anonymous", useMentionFormat),
              issueLink));
    } else if (EventType.ISSUE_ASSIGNED_ID.equals(eventTypeId)) {
      sb.append(
          i18nResolver.getText(
              recipientLocale,
              "ru.mail.jira.plugins.myteam.notification.assigned",
              formatUser(user, "common.words.anonymous", useMentionFormat),
              issueLink,
              formatUser(issue.getAssignee(), "common.concepts.unassigned", useMentionFormat)));
    } else if (EventType.ISSUE_RESOLVED_ID.equals(eventTypeId)) {
      Resolution resolution = issue.getResolution();
      sb.append(
          i18nResolver.getText(
              recipientLocale,
              "ru.mail.jira.plugins.myteam.notification.resolved",
              formatUser(user, "common.words.anonymous", useMentionFormat),
              issueLink,
              resolution != null
                  ? resolution.getNameTranslation(i18nHelper)
                  : i18nResolver.getText(recipientLocale, "common.resolution.unresolved")));
    } else if (EventType.ISSUE_CLOSED_ID.equals(eventTypeId)) {
      Resolution resolution = issue.getResolution();
      sb.append(
          i18nResolver.getText(
              recipientLocale,
              "ru.mail.jira.plugins.myteam.notification.closed",
              formatUser(user, "common.words.anonymous", useMentionFormat),
              issueLink,
              resolution != null
                  ? resolution.getNameTranslation(i18nHelper)
                  : i18nResolver.getText(recipientLocale, "common.resolution.unresolved")));
    } else if (EventType.ISSUE_COMMENTED_ID.equals(eventTypeId)) {
      sb.append(
          i18nResolver.getText(
              recipientLocale,
              "ru.mail.jira.plugins.myteam.notification.commented",
              formatUser(user, "common.words.anonymous", useMentionFormat),
              issueLink));
    } else if (EventType.ISSUE_COMMENT_EDITED_ID.equals(eventTypeId)) {
      sb.append(
          i18nResolver.getText(
              recipientLocale,
              "ru.mail.jira.plugins.myteam.notification.commentEdited",
              formatUser(user, "common.words.anonymous", useMentionFormat),
              issueLink));
    } else if (EventType.ISSUE_REOPENED_ID.equals(eventTypeId)) {
      sb.append(
          i18nResolver.getText(
              recipientLocale,
              "ru.mail.jira.plugins.myteam.notification.reopened",
              formatUser(user, "common.words.anonymous", useMentionFormat),
              issueLink));
    } else if (EventType.ISSUE_DELETED_ID.equals(eventTypeId)) {
      sb.append(
          i18nResolver.getText(
              recipientLocale,
              "ru.mail.jira.plugins.myteam.notification.deleted",
              formatUser(user, "common.words.anonymous", useMentionFormat),
              issueLink));
    } else if (EventType.ISSUE_MOVED_ID.equals(eventTypeId)) {
      sb.append(
          i18nResolver.getText(
              recipientLocale,
              "ru.mail.jira.plugins.myteam.notification.moved",
              formatUser(user, "common.words.anonymous", useMentionFormat),
              issueLink));
    } else if (EventType.ISSUE_WORKLOGGED_ID.equals(eventTypeId)) {
      sb.append(
          i18nResolver.getText(
              recipientLocale,
              "ru.mail.jira.plugins.myteam.notification.worklogged",
              formatUser(user, "common.words.anonymous", useMentionFormat),
              issueLink));
    } else if (EventType.ISSUE_WORKSTARTED_ID.equals(eventTypeId)) {
      sb.append(
          i18nResolver.getText(
              recipientLocale,
              "ru.mail.jira.plugins.myteam.notification.workStarted",
              formatUser(user, "common.words.anonymous", useMentionFormat),
              issueLink));
    } else if (EventType.ISSUE_WORKSTOPPED_ID.equals(eventTypeId)) {
      sb.append(
          i18nResolver.getText(
              recipientLocale,
              "ru.mail.jira.plugins.myteam.notification.workStopped",
              formatUser(user, "common.words.anonymous", useMentionFormat),
              issueLink));
    } else if (EventType.ISSUE_WORKLOG_UPDATED_ID.equals(eventTypeId)) {
      sb.append(
          i18nResolver.getText(
              recipientLocale,
              "ru.mail.jira.plugins.myteam.notification.worklogUpdated",
              formatUser(user, "common.words.anonymous", useMentionFormat),
              issueLink));
    } else if (EventType.ISSUE_WORKLOG_DELETED_ID.equals(eventTypeId)) {
      sb.append(
          i18nResolver.getText(
              recipientLocale,
              "ru.mail.jira.plugins.myteam.notification.worklogDeleted",
              formatUser(user, "common.words.anonymous", useMentionFormat),
              issueLink));
    } else {
      sb.append(
          i18nResolver.getText(
              recipientLocale,
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
            recipientLocale,
            useMentionFormat));
    if (issueEvent.getComment() != null && !StringUtils.isBlank(issueEvent.getComment().getBody()))
      sb.append("\n\n")
          .append(makeMyteamMarkdownFromJira(issueEvent.getComment().getBody(), useMentionFormat));
    return sb.toString();
  }

  public String formatEvent(MentionIssueEvent mentionIssueEvent) {
    Issue issue = mentionIssueEvent.getIssue();
    ApplicationUser user = mentionIssueEvent.getFromUser();
    String issueLink =
        markdownTextLink(
            issue.getKey(),
            String.format(
                "%s/browse/%s",
                applicationProperties.getString(APKeys.JIRA_BASEURL), issue.getKey()));

    StringBuilder sb = new StringBuilder();
    sb.append(
        i18nHelper.getText(
            "ru.mail.jira.plugins.myteam.notification.mentioned",
            formatUser(user, "common.words.anonymous", true),
            issueLink));
    sb.append("\n").append(shieldText(issue.getSummary()));

    if (!StringUtils.isBlank(mentionIssueEvent.getMentionText()))
      sb.append("\n\n").append(shieldText(mentionIssueEvent.getMentionText()));

    return sb.toString();
  }

  public String createIssueSummary(Issue issue, ApplicationUser user) {
    StringBuilder sb = new StringBuilder();
    sb.append(
            markdownTextLink(
                issue.getKey(),
                String.format(
                    "%s/browse/%s",
                    applicationProperties.getString(APKeys.JIRA_BASEURL), issue.getKey())))
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
            CommandRuleType.Menu.getName());
    addRowWithButton(buttons, showMenuButton);

    return buttons;
  }

  public List<List<InlineKeyboardMarkupButton>> getIssueButtons(
      String issueKey, ApplicationUser recipient, boolean isWatching) {
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

    ArrayList<InlineKeyboardMarkupButton> watchButtonRow = new ArrayList<>();

    watchButtonRow.add(
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            i18nResolver.getText(
                localeManager.getLocaleFor(recipient),
                isWatching
                    ? "ru.mail.jira.plugins.myteam.mrimsenderEventListener.unwatchButton.text"
                    : "ru.mail.jira.plugins.myteam.mrimsenderEventListener.watchButton.text"),
            String.join("-", isWatching ? "unwatch" : "watch", issueKey)));

    buttons.add(watchButtonRow);

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

  public static void addRowWithButton(
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
            ButtonRuleType.SearchIssueByKeyInput.getName());
    addRowWithButton(buttons, showIssueButton);

    // create 'Active issues assigned to me' button
    InlineKeyboardMarkupButton activeAssignedIssuesButton =
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            i18nResolver.getRawText(
                locale,
                "ru.mail.jira.plugins.myteam.messageFormatter.mainMenu.activeIssuesAssignedToMeButton.text"),
            CommandRuleType.AssignedIssues.getName());
    addRowWithButton(buttons, activeAssignedIssuesButton);

    // create 'Active issues i watching' button
    InlineKeyboardMarkupButton activeWatchingIssuesButton =
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            i18nResolver.getRawText(
                locale,
                "ru.mail.jira.plugins.myteam.messageFormatter.mainMenu.activeIssuesWatchingByMeButton.text"),
            CommandRuleType.WatchingIssues.getName());
    addRowWithButton(buttons, activeWatchingIssuesButton);

    // create 'Active issues crated by me' button
    InlineKeyboardMarkupButton activeCreatedIssuesButton =
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            i18nResolver.getRawText(
                locale,
                "ru.mail.jira.plugins.myteam.messageFormatter.mainMenu.activeIssuesCreatedByMeButton.text"),
            CommandRuleType.CreatedIssues.getName());
    addRowWithButton(buttons, activeCreatedIssuesButton);

    // create 'Search issue by JQL' button
    InlineKeyboardMarkupButton searchIssueByJqlButton =
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            i18nResolver.getRawText(
                locale,
                "ru.mail.jira.plugins.myteam.messageFormatter.mainMenu.searchIssueByJqlButton.text"),
            ButtonRuleType.SearchIssueByJqlInput.getName());
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

  public List<List<InlineKeyboardMarkupButton>> getPagerButtons(
      Locale locale, boolean withPrev, boolean withNext) {
    if (!withPrev && !withNext) return null;
    List<List<InlineKeyboardMarkupButton>> buttons = new ArrayList<>(1);
    List<InlineKeyboardMarkupButton> newButtonsRow = new ArrayList<>();
    if (withPrev) {
      newButtonsRow.add(
          InlineKeyboardMarkupButton.buildButtonWithoutUrl(
              i18nResolver.getRawText(
                  locale,
                  "ru.mail.jira.plugins.myteam.messageFormatter.listButtons.prevPageButton.text"),
              ButtonRuleType.PrevPage.getName()));
    }
    if (withNext) {
      newButtonsRow.add(
          InlineKeyboardMarkupButton.buildButtonWithoutUrl(
              i18nResolver.getRawText(
                  locale,
                  "ru.mail.jira.plugins.myteam.messageFormatter.listButtons.nextPageButton.text"),
              ButtonRuleType.NextPage.getName()));
    }
    buttons.add(newButtonsRow);
    return buttons;
  }

  public List<List<InlineKeyboardMarkupButton>> getIssueListButtons(
      Locale locale, boolean withPrev, boolean withNext) {
    return getListButtons(
        locale,
        withPrev,
        withNext,
        ButtonRuleType.PrevPage.getName(),
        ButtonRuleType.NextPage.getName());
  }

  public String stringifyMap(Map<?, ?> map) {
    if (map == null) return "";
    return map.entrySet().stream()
        .map((entry) -> String.join(" : ", entry.getKey().toString(), entry.getValue().toString()))
        .collect(joining("\n"));
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
                    markdownTextLink(issue.getKey(), createIssueLink(issue))
                        + ' '
                        + issue.getSummary())
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
                        "\\[",
                        dateFormatter.format(comment.getCreated()),
                        "\\] ",
                        "\\[",
                        comment.getAuthorFullName(),
                        "\\] ",
                        shieldText(comment.getBody())))
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
        //                IssueLinksSystemField issueLinksSystemField = (IssueLinksSystemField)
        // field;
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

  private String[] mapStringToSingleFieldValue(Field field, String fieldValue) {
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
    return mapStringToSingleFieldValue(field, fieldValue);
  }
}
