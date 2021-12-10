/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.states;

import com.atlassian.jira.exception.IssueNotFoundException;
import com.atlassian.jira.exception.IssuePermissionException;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.user.ApplicationUser;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;
import ru.mail.jira.plugins.myteam.protocol.MessageFormatter;
import ru.mail.jira.plugins.myteam.protocol.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.rulesengine.core.Pager;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.ErrorRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.service.IssueService;
import ru.mail.jira.plugins.myteam.rulesengine.service.RulesEngine;
import ru.mail.jira.plugins.myteam.rulesengine.service.UserChatService;

@Slf4j
public class ViewingIssueCommentsState extends BotState implements PageableState {

  private static final int COMMENT_LIST_PAGE_SIZE = 2;

  @Getter @Setter private String issueKey;
  private final IssueService issueService;
  private final UserChatService userChatService;
  private final RulesEngine rulesEngine;
  private final MessageFormatter messageFormatter;
  private Pager pager;

  public ViewingIssueCommentsState(
      String issueKey,
      IssueService issueService,
      UserChatService userChatService,
      RulesEngine rulesEngine) {
    super();
    this.issueKey = issueKey;
    this.issueService = issueService;
    this.userChatService = userChatService;
    this.rulesEngine = rulesEngine;
    this.messageFormatter = userChatService.getMessageFormatter();
  }

  @Override
  public void nextPage(MyteamEvent event) {
    pager.nextPage();
    try {
      updateMessage(event, true);
    } catch (MyteamServerErrorException | IOException e) {
      log.error(e.getLocalizedMessage());
    }
  }

  @Override
  public void prevPage(MyteamEvent event) {
    pager.prevPage();
    try {
      updateMessage(event, true);
    } catch (MyteamServerErrorException | IOException e) {
      log.error(e.getLocalizedMessage());
    }
  }

  public void updateMessage(MyteamEvent event, boolean editMessage)
      throws MyteamServerErrorException, IOException {

    ApplicationUser user = userChatService.getJiraUserFromUserChatId(event.getUserId());
    if (user != null) {
      try {
        Locale locale = userChatService.getUserLocale(user);
        List<Comment> totalComments = issueService.getIssueComments(issueKey, user);
        if (totalComments == null || totalComments.size() == 0) {

          userChatService.sendMessageText(
              event.getChatId(),
              userChatService.getRawText(
                  locale, "ru.mail.jira.plugins.myteam.myteamEventsListener.showComments.empty"));

        } else {
          if (pager == null) {
            pager = new Pager(totalComments.size(), COMMENT_LIST_PAGE_SIZE);
          }

          List<Comment> comments =
              totalComments.stream()
                  .skip((long) pager.getPage() * pager.getPerPage())
                  .limit(pager.getPerPage())
                  .collect(Collectors.toList());

          List<List<InlineKeyboardMarkupButton>> buttons =
              messageFormatter.getViewCommentsButtons(locale, pager.hasPrev(), pager.hasNext());
          String msg =
              messageFormatter.stringifyIssueCommentsList(
                  locale, comments, pager.getPage(), pager.getTotal());

          if (event instanceof ButtonClickEvent && editMessage)
            userChatService.editMessageText(
                event.getChatId(), ((ButtonClickEvent) event).getMsgId(), msg, buttons);
          else userChatService.sendMessageText(event.getChatId(), msg, buttons);
        }
      } catch (IssuePermissionException e) {
        rulesEngine.fireError(ErrorRuleType.IssueNoPermission, event, e.getLocalizedMessage());
      } catch (IssueNotFoundException e) {
        rulesEngine.fireError(ErrorRuleType.IssueNotFound, event, e.getLocalizedMessage());
      }
    }

    if (event instanceof ButtonClickEvent)
      userChatService.answerCallbackQuery(((ButtonClickEvent) event).getQueryId());
  }
}
