/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.service;

import com.atlassian.jira.user.ApplicationUser;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import kong.unirest.HttpResponse;
import org.jeasy.rules.api.Facts;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;
import ru.mail.jira.plugins.myteam.myteam.dto.MessageResponse;
import ru.mail.jira.plugins.myteam.protocol.MessageFormatter;

public interface UserChatService {

  ApplicationUser getJiraUserFromUserChatId(String id);

  Locale getUserLocale(ApplicationUser user);

  String getRawText(Locale locale, String key);

  MessageFormatter getMessageFormatter();

  HttpResponse<MessageResponse> sendMessageText(
      String chatId, String message, List<List<InlineKeyboardMarkupButton>> buttons)
      throws MyteamServerErrorException, IOException;

  void sendMessageText(String chatId, String message)
      throws MyteamServerErrorException, IOException;

  void fireRule(Facts facts);
}
