/* (C)2021 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.states;

import com.atlassian.jira.exception.IssueNotFoundException;
import com.atlassian.jira.exception.IssuePermissionException;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.user.ApplicationUser;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import ru.mail.jira.plugins.myteam.bot.events.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.bot.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.core.Pager;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.ErrorRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.base.BotState;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.base.PageableState;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.component.JiraMarkdownToChatMarkdownConverter;
import ru.mail.jira.plugins.myteam.component.MessageFormatter;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;
import ru.mail.jira.plugins.myteam.service.IssueService;
import ru.mail.jira.plugins.myteam.service.RulesEngine;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@SuppressWarnings({"NullAway"})
@Slf4j
public class ViewingIssueCommentsState extends BotState implements PageableState {

  private static final int COMMENT_LIST_PAGE_SIZE = 5;

  @Getter @Setter private String issueKey;
  private final IssueService issueService;
  private final UserChatService userChatService;
  private final RulesEngine rulesEngine;
  private final MessageFormatter messageFormatter;
  private final Pager pager;
  private final JiraMarkdownToChatMarkdownConverter jiraMarkdownToChatMarkdownConverter;

  public ViewingIssueCommentsState(
      String issueKey,
      IssueService issueService,
      UserChatService userChatService,
      RulesEngine rulesEngine,
      JiraMarkdownToChatMarkdownConverter jiraMarkdownToChatMarkdownConverter) {
    super();
    this.issueKey = issueKey;
    this.issueService = issueService;
    this.userChatService = userChatService;
    this.rulesEngine = rulesEngine;
    this.messageFormatter = userChatService.getMessageFormatter();
    this.jiraMarkdownToChatMarkdownConverter = jiraMarkdownToChatMarkdownConverter;
    pager = new Pager(0, COMMENT_LIST_PAGE_SIZE);
  }

  @Override
  public void nextPage(MyteamEvent event) {
    pager.nextPage();
    updatePage(event, true);
  }

  @Override
  public void prevPage(MyteamEvent event) {
    pager.prevPage();
    updatePage(event, true);
  }

  @Override
  public void updatePage(MyteamEvent event, boolean editMessage) {
    try {
      ApplicationUser user = userChatService.getJiraUserFromUserChatId(event.getUserId());
      List<Comment> totalComments = issueService.getIssueComments(issueKey, user);

      if (event instanceof ButtonClickEvent)
        userChatService.answerCallbackQuery(((ButtonClickEvent) event).getQueryId());

      if (totalComments == null || totalComments.size() == 0) {

        userChatService.sendMessageText(
            event.getChatId(),
            userChatService.getRawText(
                "ru.mail.jira.plugins.myteam.myteamEventsListener.showComments.empty"));

      } else {
        pager.setTotal(totalComments.size());

        List<Comment> comments =
            totalComments.stream()
                .skip((long) pager.getPage() * pager.getPageSize())
                .limit(pager.getPageSize())
                .collect(Collectors.toList());

        List<List<InlineKeyboardMarkupButton>> buttons =
            messageFormatter.getListButtons(pager.hasPrev(), pager.hasNext());

        String msg = stringifyIssueCommentsList(comments, pager.getPage(), pager.getTotal());

        if (event instanceof ButtonClickEvent && editMessage)
          userChatService.editMessageText(
              event.getChatId(), ((ButtonClickEvent) event).getMsgId(), msg, buttons);
        else userChatService.sendMessageText(event.getChatId(), msg, buttons);
      }
    } catch (IssuePermissionException e) {
      rulesEngine.fireError(ErrorRuleType.IssueNoPermission, event, e);
    } catch (IssueNotFoundException e) {
      rulesEngine.fireError(ErrorRuleType.IssueNotFound, event, e);
    } catch (MyteamServerErrorException | IOException e) {
      log.error(e.getLocalizedMessage(), e);
    }
  }

  private String stringifyIssueCommentsList(List<Comment> commentList, int pageNumber, int total) {
    SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    return messageFormatter.stringifyPagedCollection(
        commentList.stream()
            .map(
                comment ->
                    String.join(
                        "",
                        messageFormatter.markdownTextLink(
                            dateFormatter.format(comment.getCreated()),
                            messageFormatter.getCommentLink(comment.getIssue(), comment)),
                        "\n",
                        messageFormatter.formatUser(
                            comment.getAuthorApplicationUser(), "common.words.anonymous", false),
                        ":",
                        " ",
                        StringUtils.defaultString(
                            jiraMarkdownToChatMarkdownConverter.makeMyteamMarkdownFromJira(
                                comment.getBody(), false)),
                        ""))
            .collect(Collectors.toList()),
        pageNumber,
        total,
        COMMENT_LIST_PAGE_SIZE,
        null);
  }
}
