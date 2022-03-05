/* (C)2021 */
package ru.mail.jira.plugins.myteam.service.impl;

import com.atlassian.crowd.exception.UserNotFoundException;
import com.atlassian.jira.config.LocaleManager;
import com.atlassian.jira.exception.IssueNotFoundException;
import com.atlassian.jira.issue.Issue;
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
import org.springframework.stereotype.Service;
import ru.mail.jira.plugins.myteam.commons.PermissionHelper;
import ru.mail.jira.plugins.myteam.configuration.UserData;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.myteam.MyteamApiClient;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;
import ru.mail.jira.plugins.myteam.myteam.dto.response.MessageResponse;
import ru.mail.jira.plugins.myteam.protocol.MessageFormatter;
import ru.mail.jira.plugins.myteam.repository.MyteamChatRepository;
import ru.mail.jira.plugins.myteam.rulesengine.models.exceptions.LinkIssueWithChatException;
import ru.mail.jira.plugins.myteam.rulesengine.states.base.BotState;
import ru.mail.jira.plugins.myteam.service.IssueService;
import ru.mail.jira.plugins.myteam.service.StateManager;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Service
public class UserChatServiceImpl implements UserChatService {

  private final UserData userData;
  private final LocaleManager localeManager;
  private final MyteamApiClient myteamClient;
  private final PermissionHelper permissionHelper;
  private final I18nResolver i18nResolver;
  private final StateManager stateManager;
  private final IssueService issueService;
  private final MyteamChatRepository myteamChatRepository;

  @Getter(onMethod_ = {@Override})
  private final MessageFormatter messageFormatter;

  public UserChatServiceImpl(
      MyteamApiClient myteamApiClient,
      UserData userData,
      PermissionHelper permissionHelper,
      MessageFormatter messageFormatter,
      StateManager stateManager,
      IssueService issueService,
      MyteamChatRepository myteamChatRepository,
      @ComponentImport LocaleManager localeManager,
      @ComponentImport I18nResolver i18nResolver) {
    this.myteamClient = myteamApiClient;
    this.userData = userData;
    this.permissionHelper = permissionHelper;
    this.localeManager = localeManager;
    this.i18nResolver = i18nResolver;
    this.messageFormatter = messageFormatter;
    this.stateManager = stateManager;
    this.issueService = issueService;
    this.myteamChatRepository = myteamChatRepository;
  }

  @Override
  public ApplicationUser getJiraUserFromUserChatId(String id) throws UserNotFoundException {
    ApplicationUser user = userData.getUserByMrimLogin(id);
    if (user == null) throw new UserNotFoundException(id);
    return user;
  }

  @Override
  public boolean isChatAdmin(String chatId, String userId) {
    return permissionHelper.isChatAdminOrJiraAdmin(chatId, userId);
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
  public String getText(Locale locale, String key, String param) {
    return i18nResolver.getText(locale, key, param);
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
  public boolean deleteMessages(String chatId, List<Long> messagesId)
      throws MyteamServerErrorException, IOException {
    return myteamClient.deleteMessages(chatId, messagesId).getBody().isOk();
  }

  @Override
  public BotState getState(String chatId) {
    return stateManager.getLastState(chatId);
  }

  @Override
  public BotState getPrevState(String chatId) {
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
  public void linkChat(String chatId, String issueKey)
      throws IssueNotFoundException, LinkIssueWithChatException {
    Issue issue = issueService.getIssue(issueKey);
    if (issue != null) {
      if (myteamChatRepository.findChatByIssueKey(issueKey) == null) {
        myteamChatRepository.persistChat(chatId, issueKey);
      } else {
        throw new LinkIssueWithChatException("Issue already linked to the chat");
      }
    }
  }

  @Override
  public String getBotId() {
    return myteamClient.getBotId();
  }
}
