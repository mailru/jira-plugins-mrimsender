/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.states;

import static ru.mail.jira.plugins.myteam.protocol.MessageFormatter.LIST_PAGE_SIZE;

import com.atlassian.jira.exception.ParseException;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.user.ApplicationUser;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;
import ru.mail.jira.plugins.myteam.protocol.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.rulesengine.service.IssueService;
import ru.mail.jira.plugins.myteam.rulesengine.service.UserChatService;

@Slf4j
public class JqlSearchState implements PageableState {

  public static final int JQL_SEARCH_PAGE_SIZE = 15;

  private final IssueService issueService;
  private final UserChatService userChatService;
  private final String jql;
  private int page = 0;

  public JqlSearchState(
      UserChatService userChatService, IssueService issueService, MyteamEvent event, String jql) {
    this.issueService = issueService;
    this.userChatService = userChatService;
    this.jql = jql;
  }

  @Override
  public void nextPage(MyteamEvent event) {
    page++;
    try {
      updateMessage(event, true);
    } catch (MyteamServerErrorException | IOException e) {
      log.error(e.getLocalizedMessage());
    }
  }

  @Override
  public void prevPage(MyteamEvent event) {
    page--;
    try {
      updateMessage(event, true);
    } catch (MyteamServerErrorException | IOException e) {
      log.error(e.getLocalizedMessage());
    }
  }

  public void updateMessage(MyteamEvent event, boolean editMessage)
      throws MyteamServerErrorException, IOException {
    ApplicationUser user = userChatService.getJiraUserFromUserChatId(event.getUserId());
    Locale locale = userChatService.getUserLocale(user);
    try {
      SearchResults<Issue> parseResult =
          issueService.SearchByJql(jql, user, page, JQL_SEARCH_PAGE_SIZE);
      if (parseResult.getTotal() == 0) {
        userChatService.sendMessageText(
            event.getChatId(),
            userChatService.getRawText(
                locale,
                "ru.mail.jira.plugins.myteam.myteamEventsListener.searchIssues.emptyResult"));
      } else {
        String msg =
            userChatService
                .getMessageFormatter()
                .stringifyIssueList(locale, parseResult.getResults(), page, parseResult.getTotal());
        List<List<InlineKeyboardMarkupButton>> buttons =
            userChatService
                .getMessageFormatter()
                .getPagerButtons(
                    locale, page != 0, parseResult.getTotal() > (page + 1) * LIST_PAGE_SIZE);
        if (event instanceof ButtonClickEvent && editMessage)
          userChatService.editMessageText(
              event.getChatId(), ((ButtonClickEvent) event).getMsgId(), msg, buttons);
        else userChatService.sendMessageText(event.getChatId(), msg, buttons);
      }
      if (event instanceof ButtonClickEvent)
        userChatService.answerCallbackQuery(((ButtonClickEvent) event).getQueryId());

    } catch (SearchException | ParseException e) {
      if (event instanceof ButtonClickEvent)
        userChatService.answerCallbackQuery(((ButtonClickEvent) event).getQueryId());

      userChatService.sendMessageText(
          event.getChatId(),
          userChatService.getRawText(
              locale,
              "ru.mail.jira.plugins.myteam.myteamEventsListener.searchIssues.jqlParseError.text"));
    }
  }
}
