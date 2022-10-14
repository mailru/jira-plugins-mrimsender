/* (C)2021 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.states.issue.creation;

import com.atlassian.jira.project.Project;
import java.io.IOException;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import ru.mail.jira.plugins.myteam.bot.events.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.bot.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.core.Pager;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.base.BotState;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.base.CancelableState;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.base.PageableState;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.component.MessageFormatter;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;
import ru.mail.jira.plugins.myteam.service.IssueService;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@SuppressWarnings({"NullAway"})
@Slf4j
public class SelectingProjectState extends BotState implements PageableState, CancelableState {
  public static final String DELIMITER_STR = "----------";

  private final IssueService issueService;
  private final UserChatService userChatService;
  private final MessageFormatter messageFormatter;
  private final Pager pager;

  public SelectingProjectState(IssueService issueService, UserChatService userChatService) {
    this.issueService = issueService;
    this.userChatService = userChatService;
    this.messageFormatter = userChatService.getMessageFormatter();
    int PROJECT_LIST_PAGE_SIZE = 10;
    pager = new Pager(0, PROJECT_LIST_PAGE_SIZE);
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
    List<Project> allowedProjectList = issueService.getAllowedProjects();

    pager.setTotal(allowedProjectList.size());

    List<Project> nextProjectsInterval =
        allowedProjectList.stream()
            .skip((long) pager.getPage() * pager.getPageSize())
            .limit(pager.getPageSize())
            .collect(Collectors.toList());
    try {
      if (event instanceof ButtonClickEvent) {
        userChatService.answerCallbackQuery(((ButtonClickEvent) event).getQueryId());
      }

      String msg =
          createSelectProjectMessage(
              nextProjectsInterval,
              pager.getPage(),
              allowedProjectList.size(),
              pager.getPageSize());

      List<List<InlineKeyboardMarkupButton>> buttons =
          MessageFormatter.buildButtonsWithCancel(
              messageFormatter.getListButtons(pager.hasPrev(), pager.hasNext()),
              userChatService.getRawText(
                  "ru.mail.jira.plugins.myteam.myteamEventsListener.cancelIssueCreationButton.text"));

      if (event instanceof ButtonClickEvent && editMessage) {
        userChatService.editMessageText(
            event.getChatId(), ((ButtonClickEvent) event).getMsgId(), msg, buttons);
      } else {
        userChatService.sendMessageText(event.getChatId(), msg, buttons);
      }
    } catch (IOException | MyteamServerErrorException e) {
      log.error(e.getLocalizedMessage(), e);
    }
  }

  @Override
  public void cancel(MyteamEvent event) {
    try {
      String msg =
          userChatService.getRawText(
              "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.canceled");
      if (event instanceof ButtonClickEvent) {
        userChatService.answerCallbackQuery(((ButtonClickEvent) event).getQueryId());
        userChatService.editMessageText(
            event.getChatId(), ((ButtonClickEvent) event).getMsgId(), msg, null);
      } else {
        userChatService.sendMessageText(event.getChatId(), msg);
      }
      userChatService.deleteState(event.getChatId());
    } catch (MyteamServerErrorException | IOException e) {
      log.error(e.getLocalizedMessage(), e);
    }
  }

  private String createSelectProjectMessage(
      List<Project> visibleProjects, int pageNumber, int totalProjectsNum, int pageSize) {
    StringJoiner sj = new StringJoiner("\n");
    sj.add(
        userChatService.getRawText(
            "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.selectProject.message"));
    sj.add(DELIMITER_STR);
    List<String> formattedProjectList =
        visibleProjects.stream()
            .map(proj -> String.join("", "[", proj.getKey(), "] ", proj.getName()))
            .collect(Collectors.toList());
    sj.add(
        messageFormatter.stringifyPagedCollection(
            formattedProjectList, pageNumber, totalProjectsNum, pageSize, null));
    return sj.toString();
  }
}
