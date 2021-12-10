/* (C)2020 */
package ru.mail.jira.plugins.myteam.protocol.listeners;

import com.atlassian.jira.config.LocaleManager;
import com.atlassian.jira.issue.AttachmentManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.attachment.ConvertTemporaryAttachmentParams;
import com.atlassian.jira.issue.attachment.TemporaryAttachmentId;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.thread.JiraThreadLocalUtils;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.message.I18nResolver;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import kong.unirest.HttpResponse;
import kong.unirest.UnirestException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.myteam.commons.Utils;
import ru.mail.jira.plugins.myteam.configuration.UserData;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.myteam.MyteamApiClient;
import ru.mail.jira.plugins.myteam.myteam.dto.ChatType;
import ru.mail.jira.plugins.myteam.myteam.dto.FileResponse;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.CommentaryParts;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.File;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.Mention;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.Part;
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
  private final UserData userData;
  private final I18nResolver i18nResolver;
  private final LocaleManager localeManager;
  private final IssueManager issueManager;
  private final PermissionManager permissionManager;
  private final CommentManager commentManager;
  private final AttachmentManager attachmentManager;
  private final ChatCommandListener chatCommandListener;
  private final RulesEngine rulesEngine;
  private final StateManager stateManager;

  @Autowired
  public MyteamEventsListener(
      ChatStateMapping chatStateMapping,
      MyteamApiClient myteamApiClient,
      UserData userData,
      ChatCommandListener chatCommandListener,
      ButtonClickListener buttonClickListener,
      CreateIssueEventsListener createIssueEventsListener,
      StateManager stateManager,
      RulesEngine rulesEngine,
      @ComponentImport LocaleManager localeManager,
      @ComponentImport I18nResolver i18nResolver,
      @ComponentImport IssueManager issueManager,
      @ComponentImport PermissionManager permissionManager,
      @ComponentImport CommentManager commentManager,
      @ComponentImport AttachmentManager attachmentManager) {
    this.chatsStateMap = chatStateMapping.getChatsStateMap();
    this.attachmentManager = attachmentManager;
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
    this.userData = userData;
    this.i18nResolver = i18nResolver;
    this.localeManager = localeManager;
    this.issueManager = issueManager;
    this.permissionManager = permissionManager;
    this.commentManager = commentManager;
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
      if (chatState.isWaitingForComment()) {
        asyncEventBus.post(new NewCommentMessageEvent(chatMessageEvent, chatState.getIssueKey()));
        return;
      }
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
      case "comment":
        asyncEventBus.post(new CommentIssueClickEvent(buttonClickEvent));
        break;
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

  @Subscribe
  public void handleNewCommentMessageEvent(NewCommentMessageEvent newCommentMessageEvent)
      throws IOException, UnirestException, MyteamServerErrorException {
    JiraThreadLocalUtils.preCall();
    try {
      log.debug("CreateCommentCommand execution started...");
      ApplicationUser commentedUser =
          userData.getUserByMrimLogin(newCommentMessageEvent.getUserId());
      Issue commentedIssue =
          issueManager.getIssueByCurrentKey(newCommentMessageEvent.getCommentingIssueKey());
      if (commentedUser != null && commentedIssue != null) {
        if (permissionManager.hasPermission(
            ProjectPermissions.ADD_COMMENTS, commentedIssue, commentedUser)) {
          commentManager.create(
              commentedIssue,
              commentedUser,
              convertToJiraCommentStyle(newCommentMessageEvent, commentedUser, commentedIssue),
              true);
          log.debug("CreateCommentCommand comment created...");
          myteamApiClient.sendMessageText(
              newCommentMessageEvent.getChatId(),
              i18nResolver.getText(
                  localeManager.getLocaleFor(commentedUser),
                  "ru.mail.jira.plugins.myteam.messageQueueProcessor.commentButton.commentCreated"));
          log.debug("CreateCommentCommand new comment created message sent...");
        } else {
          log.debug("CreateCommentCommand permissions violation occurred...");
          myteamApiClient.sendMessageText(
              newCommentMessageEvent.getChatId(),
              i18nResolver.getText(
                  localeManager.getLocaleFor(commentedUser),
                  "ru.mail.jira.plugins.myteam.messageQueueProcessor.commentButton.noPermissions"));
          log.debug("CreateCommentCommand not enough permissions message sent...");
        }
      }
      log.debug("CreateCommentCommand execution finished...");
    } finally {
      JiraThreadLocalUtils.postCall();
    }
  }

  public String convertToJiraCommentStyle(
      NewCommentMessageEvent event, ApplicationUser commentedUser, Issue commentedIssue) {
    List<Part> parts = event.getMessageParts();
    if (parts == null || parts.size() == 0) return event.getMessage();
    else {
      StringBuilder outPutStrings = new StringBuilder(event.getMessage());
      parts.forEach(
          part -> {
            CommentaryParts currentPartClass =
                CommentaryParts.valueOf(part.getClass().getSimpleName());
            switch (currentPartClass) {
              case File:
                File file = (File) part;
                try {
                  HttpResponse<FileResponse> response = myteamApiClient.getFile(file.getFileId());
                  FileResponse fileInfo = response.getBody();
                  try (InputStream attachment = Utils.loadUrlFile(fileInfo.getUrl())) {
                    TemporaryAttachmentId tmpAttachmentId =
                        attachmentManager.createTemporaryAttachment(attachment, fileInfo.getSize());
                    ConvertTemporaryAttachmentParams params =
                        ConvertTemporaryAttachmentParams.builder()
                            .setTemporaryAttachmentId(tmpAttachmentId)
                            .setAuthor(commentedUser)
                            .setIssue(commentedIssue)
                            .setFilename(fileInfo.getFilename())
                            .setContentType(fileInfo.getType())
                            .setCreatedTime(DateTime.now())
                            .setFileSize(fileInfo.getSize())
                            .build();
                    attachmentManager.convertTemporaryAttachment(params);
                    if (fileInfo.getType().equals("image")) {
                      outPutStrings.append(String.format("!%s!\n", fileInfo.getFilename()));
                    } else {
                      outPutStrings.append(String.format("[%s]\n", fileInfo.getFilename()));
                    }
                    if (file.getCaption() != null) {
                      outPutStrings.append(String.format("%s\n", file.getCaption()));
                    }
                  }
                } catch (UnirestException | IOException | MyteamServerErrorException e) {
                  log.error(
                      "Unable to create attachment for comment on Issue {}",
                      commentedIssue.getKey(),
                      e);
                }
                break;
              case Mention:
                Mention mention = (Mention) part;
                ApplicationUser user = userData.getUserByMrimLogin(mention.getUserId());
                if (user != null) {
                  String temp =
                      Pattern.compile("@\\[" + mention.getUserId() + "]")
                          .matcher(outPutStrings)
                          .replaceAll("[~" + user.getName() + "]");
                  outPutStrings.setLength(0);
                  outPutStrings.append(temp);
                } else {
                  log.error(
                      "Unable change Myteam mention to Jira's mention, because Can't find user with id:{}",
                      mention.getUserId());
                }
                break;
              default:
                break;
            }
          });
      return outPutStrings.toString();
    }
  }
}
