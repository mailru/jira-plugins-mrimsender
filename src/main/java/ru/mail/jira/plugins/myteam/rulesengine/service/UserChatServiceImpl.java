/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.service;

import com.atlassian.jira.config.LocaleManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.message.I18nResolver;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.UnirestException;
import lombok.Getter;
import org.jeasy.rules.api.Facts;
import org.springframework.stereotype.Service;
import ru.mail.jira.plugins.myteam.configuration.UserData;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.myteam.MyteamApiClient;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;
import ru.mail.jira.plugins.myteam.myteam.dto.MessageResponse;
import ru.mail.jira.plugins.myteam.protocol.MessageFormatter;
import ru.mail.jira.plugins.myteam.rulesengine.MyteamRulesEngine;
import ru.mail.jira.plugins.myteam.rulesengine.states.BotState;

@Service
public class UserChatServiceImpl implements UserChatService {

  private final UserData userData;
  private final LocaleManager localeManager;
  private final MyteamRulesEngine myTeamRulesEngine;
  private final MyteamApiClient myteamClient;
  private final I18nResolver i18nResolver;
  private final StateManager stateManager;

  @Getter(onMethod_ = {@Override})
  private final MessageFormatter messageFormatter;

  public UserChatServiceImpl(
      MyteamApiClient myteamApiClient,
      UserData userData,
      MyteamRulesEngine myTeamRulesEngine,
      MessageFormatter messageFormatter,
      StateManager stateManager,
      @ComponentImport LocaleManager localeManager,
      @ComponentImport I18nResolver i18nResolver) {
    this.myteamClient = myteamApiClient;
    this.userData = userData;
    this.myTeamRulesEngine = myTeamRulesEngine;
    this.localeManager = localeManager;
    this.i18nResolver = i18nResolver;
    this.messageFormatter = messageFormatter;
    this.stateManager = stateManager;
  }

  @Override
  public ApplicationUser getJiraUserFromUserChatId(String id) {
    return userData.getUserByMrimLogin(id);
  }

  @Override
  public Locale getUserLocale(ApplicationUser user) {
    return localeManager.getLocaleFor(user);
  }

  @Override
  public String getRawText(Locale locale, String key) {
    return i18nResolver.getRawText(locale, key);
  }

  @Override
  public HttpResponse<MessageResponse> sendMessageText(
      String chatId, String message, List<List<InlineKeyboardMarkupButton>> buttons)
      throws MyteamServerErrorException, IOException {
    myteamClient.sendMessageText(chatId, message, buttons);
    return null;
  }

  @Override
  public void sendMessageText(String chatId, String message)
      throws MyteamServerErrorException, IOException {
    myteamClient.sendMessageText(chatId, message);
  }

  @Override
  public BotState getState(String chatId) {
    return stateManager.getState(chatId);
  }

  @Override
  public void setState(String chatId, BotState state) {
    stateManager.setState(chatId, state);
  }

  @Override
  public HttpResponse<MessageResponse> editMessageText(
      String chatId,
      long messageId,
      String text,
      List<List<InlineKeyboardMarkupButton>> inlineKeyboardMarkup)
      throws UnirestException, IOException, MyteamServerErrorException {
    return myteamClient.editMessageText(chatId, messageId, text, inlineKeyboardMarkup);
  }

  @Override
  public HttpResponse<JsonNode> answerCallbackQuery(String queryId)
      throws UnirestException, MyteamServerErrorException {
    return myteamClient.answerCallbackQuery(queryId);
  }

  @Override
  public void fireRule(Facts facts) {
    myTeamRulesEngine.fire(facts);
  }
}
