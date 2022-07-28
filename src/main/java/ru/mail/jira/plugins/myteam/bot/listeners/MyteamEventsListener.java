/* (C)2020 */
package ru.mail.jira.plugins.myteam.bot.listeners;

import static ru.mail.jira.plugins.myteam.commons.Const.CHAT_COMMAND_PREFIX;
import static ru.mail.jira.plugins.myteam.commons.Const.ISSUE_CREATION_BY_REPLY_PREFIX;

import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.thread.OffRequestThreadExecutor;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.google.common.base.Splitter;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.Subscribe;
import java.util.Arrays;
import java.util.List;
import kong.unirest.UnirestException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.commons.SentryClient;
import ru.mail.jira.plugins.myteam.bot.events.*;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.CommandRuleType;
import ru.mail.jira.plugins.myteam.component.UserData;
import ru.mail.jira.plugins.myteam.myteam.MyteamApiClient;
import ru.mail.jira.plugins.myteam.myteam.dto.ChatType;
import ru.mail.jira.plugins.myteam.service.RulesEngine;

@Slf4j
@Component
public class MyteamEventsListener {
  private final AsyncEventBus asyncEventBus;
  private final MyteamApiClient myteamApiClient;
  private final RulesEngine rulesEngine;
  private final UserData userData;
  private final OffRequestThreadExecutor offRequestThreadExecutor;
  private final ThreadPoolTaskExecutor jiraBotTaskExecutor;

  @Autowired
  public MyteamEventsListener(
      MyteamApiClient myteamApiClient,
      RulesEngine rulesEngine,
      UserData userData,
      @ComponentImport OffRequestThreadExecutor offRequestThreadExecutor,
      ThreadPoolTaskExecutor jiraBotTaskExecutor) {
    this.rulesEngine = rulesEngine;
    this.userData = userData;
    this.offRequestThreadExecutor = offRequestThreadExecutor;
    this.jiraBotTaskExecutor = jiraBotTaskExecutor;
    this.asyncEventBus =
        new AsyncEventBus(
            this.jiraBotTaskExecutor,
            (exception, context) -> {
              log.error(
                  "Exception occurred in subscriber = {}",
                  context.getSubscriber().toString(),
                  exception);
              SentryClient.capture(exception);
            });
    this.asyncEventBus.register(this);
    this.myteamApiClient = myteamApiClient;
  }

  public void publishEvent(MyteamEvent event) {
    asyncEventBus.post(event);
  }

  @Subscribe
  public void handleNewMessageEvent(ChatMessageEvent event) {

    @Nullable ApplicationUser user = userData.getUserByMrimLogin(event.getUserId());

    offRequestThreadExecutor.execute(
        user,
        () -> {
          String message = event.getMessage();

          if (message != null && myteamApiClient.getBotId() != null) {
            String botMention = String.format("@\\[%s\\]", myteamApiClient.getBotId());
            message = message.replaceAll(botMention, "").trim();
          }

          if (message != null
              && event.getChatType() == ChatType.GROUP
              && message.startsWith(ISSUE_CREATION_BY_REPLY_PREFIX)) {
            handleIssueCreationTag(event);
            return;
          }

          if (message != null && message.startsWith(CHAT_COMMAND_PREFIX)) {
            handleCommand(event);
            return;
          }
          handleStateAction(event);
        });
  }

  @Subscribe
  public void handleButtonClickEvent(ButtonClickEvent event) throws UnirestException {
    @Nullable ApplicationUser user = userData.getUserByMrimLogin(event.getUserId());

    offRequestThreadExecutor.execute(
        user,
        () -> {
          String buttonPrefix = StringUtils.substringBefore(event.getCallbackData(), "-");
          String data = StringUtils.substringAfter(event.getCallbackData(), "-");
          rulesEngine.fireCommand(buttonPrefix, event, data);
        });
  }

  @Subscribe
  public void handleJiraNotifyEvent(JiraNotifyEvent jiraNotifyEvent) throws Exception {
    myteamApiClient.sendMessageText(
        jiraNotifyEvent.getChatId(), jiraNotifyEvent.getMessage(), jiraNotifyEvent.getButtons());
  }

  @Subscribe
  public void handleJiraIssueViewEvent(JiraIssueViewEvent event) {
    @Nullable ApplicationUser user = userData.getUserByMrimLogin(event.getUserId());

    offRequestThreadExecutor.execute(
        user,
        () ->
            rulesEngine.fireCommand(
                CommandRuleType.Issue,
                new SyntheticEvent(event.getChatId(), event.getUserId(), event.getChatType()),
                event.getIssueKey()));
  }

  private void handleCommand(ChatMessageEvent event) {
    String withoutPrefix =
        StringUtils.substringAfter(event.getMessage(), CHAT_COMMAND_PREFIX).toLowerCase();
    String[] split = withoutPrefix.split("\\s+");

    if (split.length == 0) return;

    String command = split[0];
    String args = String.join("", Arrays.asList(split).subList(1, split.length));

    rulesEngine.fireCommand(command == null ? "" : command, event, args);
  }

  private void handleIssueCreationTag(ChatMessageEvent event) {
    String withoutPrefix =
        StringUtils.substringAfter(event.getMessage(), ISSUE_CREATION_BY_REPLY_PREFIX)
            .toLowerCase();
    List<String> split = Splitter.onPattern("\\s+").splitToList(withoutPrefix);

    if (split.size() == 0) return;

    String tag = split.get(0);

    rulesEngine.fireCommand(CommandRuleType.CreateIssueByReply, event, tag);
  }

  private void handleStateAction(ChatMessageEvent event) {
    rulesEngine.fireStateAction(event, event.getMessage());
  }
}
