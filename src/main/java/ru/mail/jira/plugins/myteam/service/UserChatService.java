/* (C)2021 */
package ru.mail.jira.plugins.myteam.service;

import com.atlassian.jira.user.ApplicationUser;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.UnirestException;
import org.jetbrains.annotations.Nullable;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.exceptions.LinkIssueWithChatException;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.base.BotState;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.component.MessageFormatter;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;
import ru.mail.jira.plugins.myteam.myteam.dto.response.MessageResponse;

public interface UserChatService {

  // Utils
  @Nullable
  ApplicationUser getJiraUserFromUserChatId(@Nullable String id);

  @Nullable
  ApplicationUser getCtxUser();

  Locale getCtxUserLocale();

  String getRawText(Locale locale, String key);

  String getRawText(String key);

  String getText(Locale locale, String s, String data);

  String getText(String s, @Nullable Serializable... data);

  MessageFormatter getMessageFormatter();

  // Myteam Client

  @Nullable
  HttpResponse<MessageResponse> sendMessageText(
      String chatId, String message, @Nullable List<List<InlineKeyboardMarkupButton>> buttons)
      throws MyteamServerErrorException, IOException;

  @Nullable
  HttpResponse<MessageResponse> sendMessageText(String chatId, @Nullable String message)
      throws MyteamServerErrorException, IOException;

  boolean deleteMessages(String chatId, List<Long> messagesId)
      throws MyteamServerErrorException, IOException;

  HttpResponse<MessageResponse> editMessageText(
      String chatId,
      long messageId,
      String text,
      @Nullable List<List<InlineKeyboardMarkupButton>> inlineKeyboardMarkup)
      throws UnirestException, IOException, MyteamServerErrorException;

  HttpResponse<JsonNode> answerCallbackQuery(String queryId)
      throws UnirestException, MyteamServerErrorException;

  void linkChat(String chatId, String issueKey) throws LinkIssueWithChatException;

  String getBotId();

  boolean isChatAdmin(String chatId, String userId);

  String getGroupChatName(String chatId);

  // State Manager

  @Nullable
  BotState getState(String chatId);

  @Nullable
  BotState getPrevState(String chatId);

  void deleteState(String chatId);

  void setState(String chatId, BotState state);

  void setState(String chatId, BotState state, boolean deletePrevious);

  void revertState(String chatId);
}
