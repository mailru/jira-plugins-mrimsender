/* (C)2020 */
package ru.mail.jira.plugins.myteam.protocol.listeners;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import kong.unirest.UnirestException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.myteam.MyteamApiClient;
import ru.mail.jira.plugins.myteam.myteam.dto.ChatType;
import ru.mail.jira.plugins.myteam.protocol.ChatState;
import ru.mail.jira.plugins.myteam.protocol.ChatStateMapping;
import ru.mail.jira.plugins.myteam.protocol.events.*;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.*;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.additionalfields.*;
import ru.mail.jira.plugins.myteam.rulesengine.service.RulesEngine;
import ru.mail.jira.plugins.myteam.rulesengine.service.StateManager;

@Slf4j
@Component
public class MyteamEventsListener {
  private static final String THREAD_NAME_PREFIX = "icq-events-listener-thread-pool";
  private static final String CHAT_COMMAND_PREFIX = "/";

  private final ConcurrentHashMap<String, ChatState> chatsStateMap;
  private final ExecutorService executorService =
      Executors.newFixedThreadPool(
          2, new ThreadFactoryBuilder().setNameFormat(THREAD_NAME_PREFIX).build());
  private final AsyncEventBus asyncEventBus;
  private final MyteamApiClient myteamApiClient;
  private final ChatCommandListener chatCommandListener;
  private final RulesEngine rulesEngine;
  private final StateManager stateManager;

  @Autowired
  public MyteamEventsListener(
      ChatStateMapping chatStateMapping,
      MyteamApiClient myteamApiClient,
      ChatCommandListener chatCommandListener,
      ButtonClickListener buttonClickListener,
      CreateIssueEventsListener createIssueEventsListener,
      StateManager stateManager,
      RulesEngine rulesEngine) {
    this.chatsStateMap = chatStateMapping.getChatsStateMap();
    this.rulesEngine = rulesEngine;
    this.stateManager = stateManager;
    this.asyncEventBus =
        new AsyncEventBus(
            executorService,
            (exception, context) ->
                log.error(
                    "Exception occurred in subscriber = {}",
                    context.getSubscriber().toString(),
                    exception));
    this.asyncEventBus.register(this);
    this.asyncEventBus.register(chatCommandListener);
    this.asyncEventBus.register(buttonClickListener);
    this.asyncEventBus.register(createIssueEventsListener);
    this.myteamApiClient = myteamApiClient;
    this.chatCommandListener = chatCommandListener;
  }

  public void publishEvent(MyteamEvent event) {
    asyncEventBus.post(event);
  }

  @Subscribe
  public void handleNewMessageEvent(ChatMessageEvent chatMessageEvent) {
    String chatId = chatMessageEvent.getChatId();
    boolean isGroupChatEvent = chatMessageEvent.getChatType() == ChatType.GROUP;

    // if chat is in some state then use our state processing logic
    if (!isGroupChatEvent && chatsStateMap.containsKey(chatId)) {
      ChatState chatState = chatsStateMap.remove(chatId);
      if (chatState.isWaitingForProjectSelect()) {
        asyncEventBus.post(
            new SelectedProjectMessageEvent(chatMessageEvent, chatState.getIssueCreationDto()));
        return;
      }
      if (chatState.isWaitingForIssueTypeSelect()) {
        asyncEventBus.post(
            new SelectedIssueTypeMessageEvent(chatMessageEvent, chatState.getIssueCreationDto()));
        return;
      }
      if (chatState.isNewIssueRequiredFieldsFillingState()) {
        asyncEventBus.post(
            new NewIssueFieldValueMessageEvent(
                chatMessageEvent,
                chatState.getIssueCreationDto(),
                chatState.getCurrentFillingFieldNum()));
        return;
      }
    }

    // if chat isn't in some state then just process new message
    String message = chatMessageEvent.getMessage();

    if (message != null && message.startsWith(CHAT_COMMAND_PREFIX)) {
      handleCommand(chatMessageEvent);
      return;
    }
    handleStateAction(chatMessageEvent);
  }

  private void handleCommand(ChatMessageEvent event) {
    String withoutPrefix =
        StringUtils.substringAfter(event.getMessage(), CHAT_COMMAND_PREFIX).toLowerCase();
    String[] split = withoutPrefix.split("\\s+");

    if (split.length == 0) return;

    String command = split[0];
    String args = String.join("", Arrays.asList(split).subList(1, split.length));

    rulesEngine.fireCommand(command, event, args);
  }

  private void handleButtonClick(ButtonClickEvent event) {
    String buttonPrefix = StringUtils.substringBefore(event.getCallbackData(), "-");
    String data = StringUtils.substringAfter(event.getCallbackData(), "-");
    rulesEngine.fireCommand(buttonPrefix, event, data);
  }

  private void handleStateAction(ChatMessageEvent event) {
    rulesEngine.fireStateAction(
        stateManager.getState(event.getChatId()), event, event.getMessage());
  }

  @Subscribe
  public void handleButtonClickEvent(ButtonClickEvent buttonClickEvent)
      throws UnirestException, MyteamServerErrorException {
    String buttonPrefix = StringUtils.substringBefore(buttonClickEvent.getCallbackData(), "-");
    String chatId = buttonClickEvent.getChatId();
    boolean isGroupChatEvent = buttonClickEvent.getChatType() == ChatType.GROUP;

    handleButtonClick(buttonClickEvent);

    // if chat is in some state then use our state processing logic
    if (!isGroupChatEvent && chatsStateMap.containsKey(chatId)) {
      ChatState chatState = chatsStateMap.remove(chatId);
      if (chatState.isWaitingForProjectSelect()) {
        if (buttonPrefix.equals("nextProjectListPage")) {
          asyncEventBus.post(
              new NextProjectsPageClickEvent(
                  buttonClickEvent,
                  chatState.getCurrentSelectListPage(),
                  chatState.getIssueCreationDto()));
          return;
        }
        if (buttonPrefix.equals("prevProjectListPage")) {
          asyncEventBus.post(
              new PrevProjectsPageClickEvent(
                  buttonClickEvent,
                  chatState.getCurrentSelectListPage(),
                  chatState.getIssueCreationDto()));
          return;
        }
      }
      if (chatState.isWaitingForAdditionalFieldSelect()) {
        if (buttonPrefix.equals("nextAdditionalFieldListPage")) {
          asyncEventBus.post(
              new NextAdditionalFieldPageClickEvent(
                  buttonClickEvent,
                  chatState.getCurrentSelectListPage(),
                  chatState.getIssueCreationDto()));
          return;
        }
        if (buttonPrefix.equals("prevAdditionalFieldListPage")) {
          asyncEventBus.post(
              new PrevAdditionalFieldPageClickEvent(
                  buttonClickEvent,
                  chatState.getCurrentSelectListPage(),
                  chatState.getIssueCreationDto()));
          return;
        }
      }
      if (chatState.isWaitingForIssueTypeSelect()) {
        if (buttonPrefix.equals("selectIssueType")) {
          asyncEventBus.post(
              new IssueTypeButtonClickEvent(buttonClickEvent, chatState.getIssueCreationDto()));
          return;
        }
      }
      if (chatState.isWaitingForNewIssueButtonFillingState()) {
        if (buttonPrefix.equals("selectIssueButtonValue")) {
          asyncEventBus.post(
              new NewIssueFieldValueButtonClickEvent(
                  buttonClickEvent,
                  chatState.getIssueCreationDto(),
                  chatState.getCurrentFillingFieldNum()));
          return;
        }
      }
      if (chatState.isWaitingForNewIssueButtonFillingState()) {
        if (buttonPrefix.equals("updateIssueButtonValue")) {
          asyncEventBus.post(
              new NewIssueFieldValueUpdateButtonClickEvent(
                  buttonClickEvent,
                  chatState.getIssueCreationDto(),
                  chatState.getCurrentFillingFieldNum()));
          return;
        }
      }
      if (chatState.isWaitingForIssueCreationConfirm()) {
        if (buttonPrefix.equals("addExtraIssueFields")) {
          asyncEventBus.post(
              new AddAdditionalIssueFieldClickEvent(
                  buttonClickEvent, chatState.getIssueCreationDto()));
          return;
        }
        if (buttonPrefix.equals("confirmIssueCreation")) {
          asyncEventBus.post(
              new CreateIssueConfirmClickEvent(buttonClickEvent, chatState.getIssueCreationDto()));
          return;
        }
      }

      if (chatState.isWaitingForAdditionalFieldSelect()) {
        if (buttonPrefix.equals("selectAdditionalField")) {
          asyncEventBus.post(
              new SelectAdditionalIssueFieldClickEvent(
                  buttonClickEvent,
                  chatState.getIssueCreationDto(),
                  StringUtils.substringAfter(buttonClickEvent.getCallbackData(), "-")));
        }
        if (buttonPrefix.equals("cancel")) {
          asyncEventBus.post(
              new CancelAdditionalFieldClickEvent(
                  buttonClickEvent, chatState.getIssueCreationDto()));
          return;
        }
      }
      if (chatState.isWaiting() && buttonPrefix.equals("cancel")) {
        asyncEventBus.post(new CancelClickEvent(buttonClickEvent));
        return;
      }
    }

    // if chat isn't in some state then just process new command
    switch (buttonPrefix) {
      case "createIssue":
        asyncEventBus.post(new CreateIssueClickEvent(buttonClickEvent));
        break;
      default:
        // fix infinite spinners situations for not recognized button clicks
        // for example next or prev button click when chat state was cleared
        myteamApiClient.answerCallbackQuery(buttonClickEvent.getQueryId());
        break;
    }
  }

  @Subscribe
  public void handleJiraNotifyEvent(JiraNotifyEvent jiraNotifyEvent) throws Exception {
    myteamApiClient.sendMessageText(
        jiraNotifyEvent.getChatId(), jiraNotifyEvent.getMessage(), jiraNotifyEvent.getButtons());
  }

  @Subscribe
  public void handleBitbucketNotifyEvent(BitbucketNotifyEvent bitbucketNotifyEvent)
      throws Exception {
    myteamApiClient.sendMessageText(
        bitbucketNotifyEvent.getChatId(),
        bitbucketNotifyEvent.getMessage(),
        bitbucketNotifyEvent.getButtons());
  }

  @Subscribe
  public void handleJiraIssueViewEvent(JiraIssueViewEvent jiraIssueViewEvent)
      throws IOException, UnirestException, MyteamServerErrorException {
    if (jiraIssueViewEvent.isGroupChat())
      chatCommandListener.sendIssueViewToGroup(
          jiraIssueViewEvent.getIssueKey(),
          jiraIssueViewEvent.getInitiator(),
          jiraIssueViewEvent.getChatId());
    else
      chatCommandListener.sendIssueViewToUser(
          jiraIssueViewEvent.getIssueKey(),
          jiraIssueViewEvent.getInitiator(),
          jiraIssueViewEvent.getChatId());
  }
}
