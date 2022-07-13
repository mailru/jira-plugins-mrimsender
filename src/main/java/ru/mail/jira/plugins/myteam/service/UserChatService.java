/* (C)2021 */
package ru.mail.jira.plugins.myteam.service;

import com.atlassian.jira.user.ApplicationUser;
import java.io.IOException;
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

  Locale getUserLocale(@Nullable String userId);

  Locale getUserLocale(@Nullable ApplicationUser user);

  String getRawText(Locale locale, String key);

  String getText(Locale locale, String s, String data);

  MessageFormatter getMessageFormatter();

  // Myteam Client

  HttpResponse<MessageResponse> sendMessageText(
      String chatId, String message, List<List<InlineKeyboardMarkupButton>> buttons)
      throws MyteamServerErrorException, IOException;

  void sendMessageText(String chatId, String message)
      throws MyteamServerErrorException, IOException;

  boolean deleteMessages(String chatId, List<Long> messagesId)
      throws MyteamServerErrorException, IOException;

  HttpResponse<MessageResponse> editMessageText(
      String chatId,
      long messageId,
      String text,
      List<List<InlineKeyboardMarkupButton>> inlineKeyboardMarkup)
      throws UnirestException, IOException, MyteamServerErrorException;

  HttpResponse<JsonNode> answerCallbackQuery(String queryId)
      throws UnirestException, MyteamServerErrorException;

  void linkChat(String chatId, String issueKey) throws LinkIssueWithChatException;

  String getBotId();

  boolean isChatAdmin(String chatId, String userId);

  String getGroupChatName(String chatId);

  // State Manager

  BotState getState(String chatId);

  BotState getPrevState(String chatId);

  void deleteState(String chatId);

  void setState(String chatId, BotState state);

  void setState(String chatId, BotState state, boolean deletePrevious);

  void revertState(String chatId);
}
