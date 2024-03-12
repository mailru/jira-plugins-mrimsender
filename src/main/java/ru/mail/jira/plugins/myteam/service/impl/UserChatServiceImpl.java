/* (C)2021 */
package ru.mail.jira.plugins.myteam.service.impl;

import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.message.I18nResolver;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.UnirestException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import ru.mail.jira.plugins.commons.SentryClient;
import ru.mail.jira.plugins.myteam.bot.events.ChatMessageEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.base.BotState;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.component.PermissionHelper;
import ru.mail.jira.plugins.myteam.component.UserData;
import ru.mail.jira.plugins.myteam.component.MessageFormatter;
import ru.mail.jira.plugins.myteam.myteam.MyteamApiClient;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;
import ru.mail.jira.plugins.myteam.myteam.dto.chats.ChatInfoResponse;
import ru.mail.jira.plugins.myteam.myteam.dto.chats.ChatMemberId;
import ru.mail.jira.plugins.myteam.myteam.dto.chats.CreateChatResponse;
import ru.mail.jira.plugins.myteam.myteam.dto.chats.GroupChatInfo;
import ru.mail.jira.plugins.myteam.myteam.dto.response.MessageResponse;
import ru.mail.jira.plugins.myteam.service.PluginData;
import ru.mail.jira.plugins.myteam.service.StateManager;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Service
@Slf4j
public class UserChatServiceImpl implements UserChatService {

  private final UserData userData;
  private final MyteamApiClient myteamClient;
  private final PermissionHelper permissionHelper;
  private final I18nResolver i18nResolver;
  private final StateManager stateManager;
  private final JiraAuthenticationContext jiraAuthenticationContext;

  @Getter(onMethod_ = {@Override})
  private final MessageFormatter messageFormatter;

  private final PluginData pluginData;

  public UserChatServiceImpl(
      MyteamApiClient myteamApiClient,
      UserData userData,
      PermissionHelper permissionHelper,
      MessageFormatter messageFormatter,
      StateManager stateManager,
      @ComponentImport I18nResolver i18nResolver,
      @ComponentImport JiraAuthenticationContext jiraAuthenticationContext,
      PluginData pluginData) {
    this.myteamClient = myteamApiClient;
    this.userData = userData;
    this.permissionHelper = permissionHelper;
    this.i18nResolver = i18nResolver;
    this.messageFormatter = messageFormatter;
    this.stateManager = stateManager;
    this.jiraAuthenticationContext = jiraAuthenticationContext;
    this.pluginData = pluginData;
  }

  @Override
  @Nullable
  public ApplicationUser getJiraUserFromUserChatId(@Nullable String id) {
    return userData.getUserByMrimLogin(id);
  }

  @Override
  public @Nullable ApplicationUser getCtxUser() {
    return jiraAuthenticationContext.getLoggedInUser();
  }

  @Override
  public Locale getCtxUserLocale() {
    return jiraAuthenticationContext.getLocale();
  }

  @Override
  public boolean isChatAdmin(String chatId, String userId) {
    return permissionHelper.isChatAdminOrJiraAdmin(chatId, userId);
  }

  @Override
  public String getGroupChatName(String chatId) {
    try {
      ChatInfoResponse chatInfo = myteamClient.getChatInfo(chatId).getBody();
      if (chatInfo instanceof GroupChatInfo) {
        return ((GroupChatInfo) chatInfo).getTitle();
      }
    } catch (MyteamServerErrorException ignored) {
      return "unknown";
    }
    return "unknown";
  }

  @Override
  public String getRawText(Locale locale, String key) {
    return i18nResolver.getRawText(locale, key);
  }

  @Override
  public String getRawText(String key) {
    return i18nResolver.getRawText(key);
  }

  @Override
  public String getText(Locale locale, String key, String param) {
    return i18nResolver.getText(locale, key, param);
  }

  @Override
  public String getText(String key, @Nullable Serializable... params) {
    return i18nResolver.getText(key, params);
  }

  @Override
  @Nullable
  public HttpResponse<MessageResponse> sendMessageText(
      String chatId, String message, @Nullable List<List<InlineKeyboardMarkupButton>> buttons)
      throws MyteamServerErrorException, IOException {
    return myteamClient.sendMessageText(chatId, message, buttons);
  }

  @Override
  @Nullable
  public HttpResponse<MessageResponse> sendMessageText(String chatId, @NotNull String message)
      throws MyteamServerErrorException, IOException {
    return myteamClient.sendMessageText(chatId, message);
  }

  @Override
  public boolean deleteMessages(String chatId, List<Long> messagesId)
      throws MyteamServerErrorException, IOException {
    return myteamClient.deleteMessages(chatId, messagesId).getBody().isOk();
  }

  @Override
  public @Nullable BotState getState(String chatId) {
    return stateManager.getLastState(chatId);
  }

  @Override
  public @Nullable BotState getPrevState(String chatId) {
    return stateManager.getPrevState(chatId);
  }

  @Override
  public void deleteState(String chatId) {
    stateManager.deleteStates(chatId);
  }

  @Override
  public void setState(String chatId, BotState state) {
    stateManager.setState(chatId, state);
  }

  @Override
  public void setState(String chatId, BotState state, boolean deletePrevious) {
    stateManager.setState(chatId, state, deletePrevious);
  }

  @Override
  public void revertState(String chatId) {
    stateManager.revertState(chatId);
  }

  @Override
  public boolean isChatAdmin(ChatMessageEvent event, String userId) {
    return permissionHelper.isChatAdmin(event.getChatId(), userId);
  }

  @Override
  public HttpResponse<MessageResponse> editMessageText(
      String chatId,
      long messageId,
      String text,
      @Nullable List<List<InlineKeyboardMarkupButton>> inlineKeyboardMarkup)
      throws UnirestException, IOException, MyteamServerErrorException {
    return myteamClient.editMessageText(chatId, messageId, text, inlineKeyboardMarkup);
  }

  @Override
  public HttpResponse<JsonNode> answerCallbackQuery(String queryId)
      throws UnirestException, MyteamServerErrorException {
    return myteamClient.answerCallbackQuery(queryId);
  }

  @Override
  public String getBotId() {
    return myteamClient.getBotId();
  }

  public String createChat(
      final String chatName,
      @Nullable final String about,
      final List<ChatMemberId> members,
      final boolean isPublic,
      final String issueKeyToLinkChat) {
    try {
      final HttpResponse<CreateChatResponse> createChatResponse =
          myteamClient.createChat(
              pluginData.getToken(), chatName, StringUtils.defaultString(about), members, isPublic);

      if (createChatResponse.getStatus() == 200
          && createChatResponse.getBody() != null
          && createChatResponse.getBody().getSn() != null) {
        String chatId = createChatResponse.getBody().getSn();
        sendFirstMessageWithCommandsInCreatedChat(chatId);
        return chatId;
      }

      createChatResponse
          .getParsingError()
          .ifPresent(
              e ->
                  SentryClient.capture(
                      e,
                      Map.of(
                          "statusCode",
                          String.valueOf(createChatResponse.getStatus()),
                          "chatName",
                          chatName,
                          "about",
                          StringUtils.defaultString(about),
                          "chatMembers",
                          members.toString(),
                          "publicChat",
                          String.valueOf(isPublic))));
      log.error("Exception during chat creation chat sn not found");

      throw new RuntimeException(
          String.format(
              "Exception during chat creation chat sn not found. Response code: %s",
              createChatResponse.getStatus()));
    } catch (IOException | UnirestException | MyteamServerErrorException e) {
      log.error("Exception during chat creation", e);
      throw new RuntimeException("Exception during chat creation", e);
    }
  }

  private void sendFirstMessageWithCommandsInCreatedChat(String chatId) {
    try {
      myteamClient.sendMessageText(
          chatId,
          i18nResolver.getRawText(
              "ru.mail.jira.plugins.myteam.myteamEventsListener.groupChat.all.commands"));
    } catch (Exception e) {
      log.error("error happened during send message with command in chat", e);
    }
  }
}
