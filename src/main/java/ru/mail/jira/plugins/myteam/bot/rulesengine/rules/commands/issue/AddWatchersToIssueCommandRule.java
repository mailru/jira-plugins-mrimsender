/* (C)2024 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.rules.commands.issue;

import com.atlassian.jira.bc.user.search.UserSearchService;
import com.atlassian.jira.exception.IssueNotFoundException;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.JiraKeyUtils;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.mail.jira.plugins.commons.SentryClient;
import ru.mail.jira.plugins.myteam.bot.events.ChatMessageEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.CommandRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.ErrorRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.BaseRule;
import ru.mail.jira.plugins.myteam.commons.Utils;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.myteam.MyteamApiClient;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.Mention;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.Part;
import ru.mail.jira.plugins.myteam.service.IssueService;
import ru.mail.jira.plugins.myteam.service.RulesEngine;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Rule(name = "/addWatchers", description = "add to watchers issues passed users")
@Slf4j
public class AddWatchersToIssueCommandRule extends BaseRule {
  private static final RuleType NAME = CommandRuleType.AddWatchers;
  private static final String NAME_LOWER_CASE = CommandRuleType.AddWatchers.getName().toLowerCase();

  private final IssueService issueService;
  private final MyteamApiClient myteamApiClient;
  private final UserSearchService userSearchService;

  public AddWatchersToIssueCommandRule(
      final UserChatService userChatService,
      final RulesEngine rulesEngine,
      final IssueService issueService,
      final MyteamApiClient myteamApiClient,
      final UserSearchService userSearchService) {
    super(userChatService, rulesEngine);
    this.issueService = issueService;
    this.myteamApiClient = myteamApiClient;
    this.userSearchService = userSearchService;
  }

  @Condition
  public boolean isValid(@Fact("command") final String command) {
    return NAME_LOWER_CASE.equals(command);
  }

  @Action
  public void execute(@Fact("event") final ChatMessageEvent event)
      throws MyteamServerErrorException, IOException {
    try {
      final List<String> issueKeysFromString =
          JiraKeyUtils.getIssueKeysFromString(event.getMessage());

      if (issueKeysFromString.size() == 0) {
        throw new AddWatcherCommandIllegalArgumentException("issueKeys");
      }

      final List<Issue> issuesToWatch =
          issueKeysFromString.stream()
              .map(issueService::getIssue)
              .collect(Collectors.toUnmodifiableList());

      final Set<ApplicationUser> usersToWatchIssues =
          resolveUserToWatchFromEvent(event, issueKeysFromString);
      for (final Issue issueToWatch : issuesToWatch) {
        for (final ApplicationUser userToWatchIssue : usersToWatchIssues) {
          issueService.watchIssue(issueToWatch, userToWatchIssue);
        }
      }
      userChatService.sendMessageText(
          event.getChatId(),
          userChatService.getText(
              "ru.mail.jira.plugins.myteam.messageQueueProcessor.addWatcher.success",
              usersToWatchIssues.stream()
                  .map(user -> messageFormatter.formatUser(user, "common.words.anonymous", false))
                  .collect(Collectors.joining(",")),
              Utils.shieldText(issueKeysFromString.toString())));
    } catch (AddWatcherCommandIllegalArgumentException e) {
      userChatService.sendMessageText(
          event.getChatId(),
          userChatService.getRawText(
              "ru.mail.jira.plugins.myteam.messageQueueProcessor.addWatcher.message.notContains."
                  + e.getMessage()));
    } catch (IssueNotFoundException e) {
      rulesEngine.fireError(ErrorRuleType.IssueNotFound, event, e);
    } catch (Exception e) {
      log.error(e.getLocalizedMessage(), e);
      SentryClient.capture(e);
      userChatService.sendMessageText(
          event.getUserId(),
          Utils.shieldText(
              String.format(
                  "Возникла ошибка при добавлении пользователей в наблюдатели.%n%n%s",
                  e.getLocalizedMessage())));
    }
  }

  @NotNull
  private Set<ApplicationUser> resolveUserToWatchFromEvent(
      final ChatMessageEvent event, List<String> issueKeysFromString) {
    final String botId = myteamApiClient.getBotId();

    final List<String> usersIds = resolveUsersIdsFromMentions(event, botId);
    final List<String> usersEmails =
        resolveUsersEmailsFromEventMessage(
            event.getMessage(), botId, usersIds, issueKeysFromString);

    if (usersIds == null && usersEmails == null) {
      throw new AddWatcherCommandIllegalArgumentException("users");
    }

    final Set<ApplicationUser> usersToWatch = new HashSet<>();

    if (usersIds != null) {
      usersIds.stream().map(userChatService::getJiraUserFromUserChatId).forEach(usersToWatch::add);
    }

    if (usersEmails != null) {
      usersEmails.stream()
          .map(userSearchService::findUsersByEmail)
          .forEach(it -> it.forEach(usersToWatch::add));
    }

    if (usersToWatch.size() == 0) {
      throw new AddWatcherCommandIllegalArgumentException("users");
    }

    return usersToWatch;
  }

  @Nullable
  private List<String> resolveUsersEmailsFromEventMessage(
      String messageBody,
      final String botId,
      @Nullable List<String> usersIds,
      @NotNull List<String> issueKeys) {
    messageBody =
        messageBody
            .replaceAll("/" + NAME.getName(), "")
            .replaceAll(String.format("@\\[%s\\]", botId), "");
    if (usersIds != null) {
      for (String userId : usersIds) {
        messageBody = messageBody.replaceAll(String.format("@\\[%s\\]", userId), "");
      }
    }

    for (String issueKey : issueKeys) {
      messageBody = messageBody.replaceAll(issueKey, "");
    }

    messageBody = messageBody.trim();

    if (StringUtils.isBlank(messageBody)) {
      return null;
    }

    return Arrays.stream(messageBody.split(","))
        .map(String::trim)
        .filter(StringUtils::isNotBlank)
        .collect(Collectors.toUnmodifiableList());
  }

  @Nullable
  private static List<String> resolveUsersIdsFromMentions(
      final ChatMessageEvent event, final String botId) {
    if (event.isHasMentions()) {
      final List<Part> messageParts = event.getMessageParts();
      if (messageParts != null) {
        return messageParts.stream()
            .filter(Mention.class::isInstance)
            .map(Mention.class::cast)
            .map(Mention::getUserId)
            .filter(userId -> !userId.equals(botId))
            .collect(Collectors.toUnmodifiableList());
      }
    }

    return null;
  }

  private static final class AddWatcherCommandIllegalArgumentException extends RuntimeException {

    public AddWatcherCommandIllegalArgumentException(final String messagePrefix) {
      super(messagePrefix);
    }
  }
}
