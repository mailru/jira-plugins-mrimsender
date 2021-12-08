/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.service;

import com.atlassian.jira.user.ApplicationUser;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.UnirestException;
import org.jeasy.rules.api.Facts;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;
import ru.mail.jira.plugins.myteam.myteam.dto.MessageResponse;
import ru.mail.jira.plugins.myteam.protocol.MessageFormatter;
import ru.mail.jira.plugins.myteam.rulesengine.states.BotState;

public interface UserChatService {

  // Utils

  ApplicationUser getJiraUserFromUserChatId(String id);

  Locale getUserLocale(ApplicationUser user);

  String getRawText(Locale locale, String key);

  MessageFormatter getMessageFormatter();

  // Myteam Client

  HttpResponse<MessageResponse> sendMessageText(
      String chatId, String message, List<List<InlineKeyboardMarkupButton>> buttons)
      throws MyteamServerErrorException, IOException;

  void sendMessageText(String chatId, String message)
      throws MyteamServerErrorException, IOException;

  HttpResponse<MessageResponse> editMessageText(
      String chatId,
      long messageId,
      String text,
      List<List<InlineKeyboardMarkupButton>> inlineKeyboardMarkup)
      throws UnirestException, IOException, MyteamServerErrorException;

  HttpResponse<JsonNode> answerCallbackQuery(String queryId)
      throws UnirestException, MyteamServerErrorException;

  // State Manager

  BotState getState(String chatId);

  void deleteState(String chatId);

  void setState(String chatId, BotState state);

  // Rules Engine

  void fireRule(Facts facts);
}
