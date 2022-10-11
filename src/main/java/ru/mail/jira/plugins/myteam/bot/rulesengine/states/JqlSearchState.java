/* (C)2021 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.states;

import static ru.mail.jira.plugins.myteam.component.MessageFormatter.LIST_PAGE_SIZE;

import com.atlassian.jira.exception.ParseException;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.user.ApplicationUser;
import java.io.IOException;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import ru.mail.jira.plugins.commons.SentryClient;
import ru.mail.jira.plugins.myteam.bot.events.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.bot.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.base.BotState;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.base.CancelableState;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.base.PageableState;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;
import ru.mail.jira.plugins.myteam.service.IssueService;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@SuppressWarnings({"NullAway"})
@Slf4j
public class JqlSearchState extends BotState implements PageableState, CancelableState {

  public static final int JQL_SEARCH_PAGE_SIZE = 15;

  private final IssueService issueService;
  private final UserChatService userChatService;
  @Getter private final String jql;
  private int page = 0;

  public JqlSearchState(UserChatService userChatService, IssueService issueService, String jql) {
    this.issueService = issueService;
    this.userChatService = userChatService;
    this.jql = jql;
  }

  @Override
  public void nextPage(MyteamEvent event) {
    page++;
    updatePage(event, true);
  }

  @Override
  public void prevPage(MyteamEvent event) {
    page--;
    updatePage(event, true);
  }

  @Override
  public void updatePage(MyteamEvent event, boolean editMessage) {
    ApplicationUser user = userChatService.getJiraUserFromUserChatId(event.getUserId());

    try {
      if (event instanceof ButtonClickEvent) {
        userChatService.answerCallbackQuery(((ButtonClickEvent) event).getQueryId());
      }

      SearchResults<Issue> parseResult =
          issueService.searchByJql(jql, user, page, JQL_SEARCH_PAGE_SIZE);
      if (parseResult.getTotal() == 0) {
        userChatService.sendMessageText(
            event.getChatId(),
            userChatService.getRawText(
                "ru.mail.jira.plugins.myteam.myteamEventsListener.searchIssues.emptyResult"));
      } else {
        String msg =
            userChatService
                .getMessageFormatter()
                .stringifyIssueList(parseResult.getResults(), page, parseResult.getTotal());
        List<List<InlineKeyboardMarkupButton>> buttons =
            userChatService
                .getMessageFormatter()
                .getListButtons(page != 0, parseResult.getTotal() > (page + 1) * LIST_PAGE_SIZE);
        if (event instanceof ButtonClickEvent && editMessage)
          userChatService.editMessageText(
              event.getChatId(), ((ButtonClickEvent) event).getMsgId(), msg, buttons);
        else userChatService.sendMessageText(event.getChatId(), msg, buttons);
      }

    } catch (SearchException | ParseException e) {

      try {
        userChatService.sendMessageText(
            event.getChatId(),
            userChatService.getRawText(
                "ru.mail.jira.plugins.myteam.myteamEventsListener.searchIssues.jqlParseError.text"));
      } catch (MyteamServerErrorException | IOException ex) {
        log.error(e.getLocalizedMessage(), e);
      }
    } catch (MyteamServerErrorException | IOException e) {
      log.error(e.getLocalizedMessage(), e);
    }
  }

  @Override
  public UserChatService getUserChatService() {
    return userChatService;
  }

  @Override
  public void onError(Exception e) {
    SentryClient.capture(e);
    log.error(e.getLocalizedMessage(), e);
  }
}
