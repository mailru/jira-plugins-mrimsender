/* (C)2022 */
package ru.mail.jira.plugins.myteam.accessrequest.service;

import com.atlassian.jira.config.LocaleManager;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.customfields.CustomFieldType;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.watchers.WatcherManager;
import com.atlassian.jira.mail.Email;
import com.atlassian.jira.mail.builder.EmailBuilder;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.security.roles.ProjectRole;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.mail.queue.MailQueue;
import com.atlassian.mail.queue.MailQueueItem;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.message.I18nResolver;
import com.atlassian.velocity.VelocityManager;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javax.validation.Valid;
import kong.unirest.HttpResponse;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import ru.mail.jira.plugins.commons.CommonUtils;
import ru.mail.jira.plugins.commons.SentryClient;
import ru.mail.jira.plugins.myteam.accessrequest.controller.dto.AccessRequestConfigurationDto;
import ru.mail.jira.plugins.myteam.accessrequest.controller.dto.AccessRequestDto;
import ru.mail.jira.plugins.myteam.accessrequest.controller.dto.DtoUtils;
import ru.mail.jira.plugins.myteam.accessrequest.model.AccessRequestConfiguration;
import ru.mail.jira.plugins.myteam.accessrequest.model.AccessRequestConfigurationRepository;
import ru.mail.jira.plugins.myteam.accessrequest.model.AccessRequestHistory;
import ru.mail.jira.plugins.myteam.accessrequest.model.AccessRequestHistoryRepository;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.ButtonRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.buttons.ReplyRule;
import ru.mail.jira.plugins.myteam.commons.Utils;
import ru.mail.jira.plugins.myteam.component.MessageFormatter;
import ru.mail.jira.plugins.myteam.component.MyteamAuditService;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;
import ru.mail.jira.plugins.myteam.myteam.dto.response.MessageResponse;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Component
@Validated
@SuppressWarnings("NullAway")
public class AccessRequestService {
  private static final int SEND_ACCESS_REQUEST_NAX_USER_COUNT = 50;

  private final AccessRequestConfigurationRepository accessRequestConfigurationRepository;
  private final AccessRequestHistoryRepository accessRequestHistoryRepository;
  private final DtoUtils dtoUtils;
  private final MessageFormatter messageFormatter;
  private final MyteamAuditService myteamAuditService;
  private final UserChatService userChatService;
  private final ApplicationProperties applicationProperties;
  private final CustomFieldManager customFieldManager;
  private final GroupManager groupManager;
  private final I18nResolver i18nResolver;
  private final LocaleManager localeManager;
  private final MailQueue mailQueue;
  private final ProjectRoleManager projectRoleManager;
  private final UserManager userManager;
  private final VelocityManager velocityManager;
  private final WatcherManager watcherManager;

  public AccessRequestService(
      AccessRequestConfigurationRepository accessRequestConfigurationRepository,
      AccessRequestHistoryRepository accessRequestHistoryRepository,
      DtoUtils dtoUtils,
      MessageFormatter messageFormatter,
      MyteamAuditService myteamAuditService,
      UserChatService userChatService,
      @ComponentImport ApplicationProperties applicationProperties,
      @ComponentImport CustomFieldManager customFieldManager,
      @ComponentImport GroupManager groupManager,
      @ComponentImport I18nResolver i18nResolver,
      @ComponentImport LocaleManager localeManager,
      @ComponentImport MailQueue mailQueue,
      @ComponentImport ProjectRoleManager projectRoleManager,
      @ComponentImport UserManager userManager,
      @ComponentImport VelocityManager velocityManager,
      @ComponentImport WatcherManager watcherManager) {
    this.accessRequestConfigurationRepository = accessRequestConfigurationRepository;
    this.accessRequestHistoryRepository = accessRequestHistoryRepository;
    this.dtoUtils = dtoUtils;
    this.messageFormatter = messageFormatter;
    this.myteamAuditService = myteamAuditService;
    this.applicationProperties = applicationProperties;
    this.customFieldManager = customFieldManager;
    this.groupManager = groupManager;
    this.i18nResolver = i18nResolver;
    this.localeManager = localeManager;
    this.mailQueue = mailQueue;
    this.projectRoleManager = projectRoleManager;
    this.userManager = userManager;
    this.velocityManager = velocityManager;
    this.watcherManager = watcherManager;
    this.userChatService = userChatService;
  }

  @Nullable
  public AccessRequestDto getAccessRequest(
      @NotNull ApplicationUser loggedInUser, @NotNull Issue issue) {
    Project project = issue.getProjectObject();
    if (project == null) throw new IllegalStateException();

    AccessRequestDto accessRequestDto = new AccessRequestDto();

    AccessRequestHistory accessRequestHistory =
        accessRequestHistoryRepository.getAccessRequestHistory(
            loggedInUser.getKey(), issue.getId());
    if (!isAccessRequestExpired(accessRequestHistory)) {
      accessRequestDto.setUsers(
          CommonUtils.split(accessRequestHistory.getUserKeys()).stream()
              .map(userKey -> dtoUtils.buildUserDto(userManager.getUserByKey(userKey)))
              .collect(Collectors.toList()));
      accessRequestDto.setMessage(accessRequestHistory.getMessage());
      accessRequestDto.setSent(Boolean.TRUE);

    } else {
      AccessRequestConfiguration configuration =
          accessRequestConfigurationRepository.getAccessRequestConfiguration(project.getId());
      if (configuration == null) return null;

      Set<ApplicationUser> participants = new HashSet<>();
      if (configuration.getUserKeys() != null) {
        CommonUtils.split(configuration.getUserKeys())
            .forEach(userKey -> participants.add(userManager.getUserByKey(userKey)));
      }
      if (configuration.getGroups() != null) {
        CommonUtils.split(configuration.getGroups())
            .forEach(groupName -> participants.addAll(groupManager.getUsersInGroup(groupName)));
      }
      if (configuration.getProjectRoleIds() != null) {
        CommonUtils.split(configuration.getProjectRoleIds())
            .forEach(
                projectRoleId ->
                    participants.addAll(
                        getUsersFromProjectRole(project, Long.parseLong(projectRoleId))));
      }
      if (configuration.getUserFieldIds() != null) {
        CommonUtils.split(configuration.getUserFieldIds())
            .forEach(fieldId -> participants.addAll(getUsersFromCustomField(issue, fieldId)));
      }

      accessRequestDto.setUsers(
          participants.stream()
              .filter(Objects::nonNull)
              .map(dtoUtils::buildUserDto)
              .collect(Collectors.toList()));
      accessRequestDto.setSent(Boolean.FALSE);
    }

    return accessRequestDto;
  }

  public AccessRequestHistory sendAccessRequest(
      @NotNull ApplicationUser loggedInUser,
      @NotNull Issue issue,
      @Valid AccessRequestDto accessRequestDto) {
    accessRequestDto.setRequesterKey(loggedInUser.getKey());
    accessRequestDto.setIssueId(issue.getId());

    AccessRequestHistory history =
        accessRequestHistoryRepository.getAccessRequestHistory(
            loggedInUser.getKey(), issue.getId());
    if (history != null) {
      history = accessRequestHistoryRepository.update(history.getID(), accessRequestDto);
    } else {
      history = accessRequestHistoryRepository.create(accessRequestDto);
    }
    if (issue.getProjectId() != null) {
      AccessRequestConfiguration configuration =
          accessRequestConfigurationRepository.getAccessRequestConfiguration(issue.getProjectId());
      if (configuration != null) {
        AtomicReference<Long> msgIdTemporary = new AtomicReference<>();
        List<Pair<ApplicationUser, Long>> userIdsInfo = new ArrayList<>();
        accessRequestDto.getUsers().stream()
            .map(dto -> userManager.getUserByKey(dto.getUserKey()))
            .filter(user -> user != null && user.isActive())
            .limit(SEND_ACCESS_REQUEST_NAX_USER_COUNT)
            .forEach(
                user -> {
                  try {
                    if (configuration.isSendMessage()) {
                      msgIdTemporary.set(sendMessageTemporary(user, loggedInUser, issue));
                      userIdsInfo.add(Pair.of(user, msgIdTemporary.get()));
                    }
                    if (configuration.isSendEmail())
                      sendEmail(user, loggedInUser, issue, accessRequestDto.getMessage());
                  } catch (Exception e) {
                    SentryClient.capture(e, Map.of("user", user.getKey()));
                  }
                });

        if (configuration.isSendMessage()) {
          List<String> replyIds = userIdsInfo.stream().map(pair -> pair.getRight().toString()).collect(Collectors.toList());
          for (Pair<ApplicationUser, Long> useridInfo : userIdsInfo) {
            sendMessageWithAnswer(
                useridInfo.getKey(),
                loggedInUser,
                useridInfo.getValue(),
                issue,
                accessRequestDto.getMessage(),
                RuleType.joinArgs(replyIds));
          }
        }
      }
    }

    myteamAuditService.userSendAccessRequestContragent(loggedInUser, issue, history);
    return history;
  }

  @Nullable
  public AccessRequestConfigurationDto getAccessRequestConfiguration(@NotNull Project project) {
    AccessRequestConfiguration configuration =
        accessRequestConfigurationRepository.getAccessRequestConfiguration(project.getId());
    if (configuration == null) return null;

    return accessRequestConfigurationRepository.entityToDto(configuration);
  }

  public AccessRequestConfiguration createAccessRequestConfiguration(
      @Valid AccessRequestConfigurationDto configurationDto) {
    try {
      return accessRequestConfigurationRepository.create(configurationDto);
    } catch (Exception err) {
      System.out.println(err.getMessage());
      return null;
    }
  }

  public AccessRequestConfiguration updateAccessRequestConfiguration(
      int configurationId, @Valid AccessRequestConfigurationDto configurationDto) {
    return accessRequestConfigurationRepository.update(configurationId, configurationDto);
  }

  public void deleteAccessRequestConfiguration(int configurationId) {
    accessRequestConfigurationRepository.deleteById(configurationId);
  }

  private Set<ApplicationUser> getUsersFromProjectRole(@NotNull Project project, Long roleId) {
    ProjectRole projectRole = projectRoleManager.getProjectRole(roleId);
    return projectRoleManager.getProjectRoleActors(projectRole, project).getApplicationUsers();
  }

  private List<ApplicationUser> getUsersFromCustomField(
      @NotNull Issue issue, @NotNull String customFieldId) {
    if (customFieldId.equals(AccessRequestConfigurationRepository.REPORTER)) {
      return Collections.singletonList(issue.getReporter());
    }
    if (customFieldId.equals(AccessRequestConfigurationRepository.ASSIGNEE)) {
      return Collections.singletonList(issue.getAssignee());
    }
    if (customFieldId.equals(AccessRequestConfigurationRepository.WATCHERS)) {
      return (List<ApplicationUser>) watcherManager.getWatchersUnsorted(issue);
    }
    CustomField customField = customFieldManager.getCustomFieldObject(customFieldId);
    if (customField != null) {
      CustomFieldType customFieldType = customField.getCustomFieldType();
      if (customFieldType instanceof com.atlassian.jira.issue.customfields.impl.UserCFType) {
        return Collections.singletonList((ApplicationUser) issue.getCustomFieldValue(customField));
      }
      if (customFieldType instanceof com.atlassian.jira.issue.customfields.impl.MultiUserCFType) {
        return (List<ApplicationUser>) issue.getCustomFieldValue(customField);
      }
    }
    return Collections.emptyList();
  }

  private boolean isAccessRequestExpired(@Nullable AccessRequestHistory history) {
    if (history == null) return true;
    return Utils.convertToLocalDateTime(history.getDate())
        .plusDays(1)
        .isBefore(LocalDateTime.now(ZoneId.systemDefault()));
  }

  //  private void sendMessage(ApplicationUser to, ApplicationUser from, Issue issue, String
  // message) {
  //    try {
  //      userChatService.sendMessageText(
  //          to.getEmailAddress(), messageFormatter.formatAccessRequestMessage(from, issue,
  // message));
  //    } catch (Exception e) {
  //      SentryClient.capture(
  //          e,
  //          Map.of(
  //              "to", to.getEmailAddress(), "from", from.getEmailAddress(), "issue",
  // issue.getKey()));
  //    }
  //  }

  private Long sendMessageTemporary(ApplicationUser to, ApplicationUser from, Issue issue) {
    try {
      HttpResponse<MessageResponse> response =
          userChatService.sendMessageText(to.getEmailAddress(), "Access Request Formatting...");
      if (response.isSuccess()) {
        return response.getBody().getMsgId();
      }
    } catch (Exception e) {
      SentryClient.capture(
          e,
          Map.of(
              "to", to.getEmailAddress(), "from", from.getEmailAddress(), "issue", issue.getKey()));
    }
    return null;
  }

  private void sendMessageWithAnswer(
      ApplicationUser to,
      ApplicationUser from,
      Long msgId,
      Issue issue,
      String message,
      String replyIdsArgs) {
    try {
      userChatService.editMessageText(
          to.getEmailAddress(),
          msgId,
          messageFormatter.formatAccessRequestMessage(from, issue, message),
          getReplyButtons(Objects.requireNonNull(issue.getProjectObject()).getKey(), replyIdsArgs));
    } catch (Exception e) {
      SentryClient.capture(
          e,
          Map.of(
              "to", to.getEmailAddress(), "from", from.getEmailAddress(), "issue", issue.getKey()));
    }
  }

  private List<List<InlineKeyboardMarkupButton>> getReplyButtons(
      String projectKey, String replyIdsArgs) {
    List<List<InlineKeyboardMarkupButton>> buttons = new ArrayList<>();
    List<InlineKeyboardMarkupButton> buttonsRow = new ArrayList<>();

    buttonsRow.add(
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            i18nResolver.getRawText(
                "ru.mail.jira.plugins.myteam.accessRequest.configuration.rule.type.allow"),
            String.join(
                "-",
                ButtonRuleType.AccessReply.getName(),
                RuleType.joinArgs(List.of(ReplyRule.COMMAND_ALLOW, projectKey, replyIdsArgs)))));
    buttonsRow.add(
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            i18nResolver.getRawText(
                "ru.mail.jira.plugins.myteam.accessRequest.configuration.rule.type.forbid"),
            String.join(
                "-",
                ButtonRuleType.AccessReply.getName(),
                RuleType.joinArgs(List.of(ReplyRule.COMMAND_FORBID, projectKey, replyIdsArgs)))));
    buttonsRow.add(
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            "Вывод id",
            String.join(
                "-",
                ButtonRuleType.AccessReply.getName(),
                RuleType.joinArgs(List.of(ReplyRule.COMMAND_DEV, projectKey)))));
    buttons.add(buttonsRow);

    return buttons;
  }

  private void sendEmail(
      @NotNull ApplicationUser to,
      @NotNull ApplicationUser from,
      @NotNull Issue issue,
      @Nullable String message) {
    Locale locale = localeManager.getLocaleFor(to);
    Map<String, Object> params = new HashMap<>();
    params.put("locale", locale);
    params.put("user", dtoUtils.buildUserDto(from, applicationProperties));
    params.put("message", message);
    params.put("issue", issue);
    params.put(
        "issueUrl",
        String.format(
            "%s/browse/%s", applicationProperties.getString(APKeys.JIRA_BASEURL), issue.getKey()));
    params.put("i18nResolver", i18nResolver);

    MailQueueItem item =
        new EmailBuilder(new Email(to.getEmailAddress()), "text/html", locale)
            .withSubject(
                i18nResolver.getText(
                    locale,
                    "ru.mail.jira.plugins.myteam.accessRequest.page.email.title",
                    from.getDisplayName(),
                    issue.getKey()))
            .withBody(
                velocityManager.getEncodedBody(
                    "ru/mail/jira/plugins/myteam/accessrequest/",
                    "access-request-mail-template.vm",
                    "UTF-8",
                    params))
            .renderLater();
    mailQueue.addItem(item);
  }
}
