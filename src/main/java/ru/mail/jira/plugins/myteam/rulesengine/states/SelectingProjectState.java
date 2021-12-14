/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.states;

import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;
import ru.mail.jira.plugins.myteam.protocol.MessageFormatter;
import ru.mail.jira.plugins.myteam.protocol.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.rulesengine.core.Pager;
import ru.mail.jira.plugins.myteam.rulesengine.service.IssueService;
import ru.mail.jira.plugins.myteam.rulesengine.service.UserChatService;

@Slf4j
public class SelectingProjectState extends BotState implements PageableState {

  private final IssueService issueService;
  private final UserChatService userChatService;
  private final MessageFormatter messageFormatter;
  private final Pager pager;
  private final String messagePrefix;

  public SelectingProjectState(
      IssueService issueService, UserChatService userChatService, String messagePrefix) {
    this.issueService = issueService;
    this.userChatService = userChatService;
    this.messageFormatter = userChatService.getMessageFormatter();
    this.messagePrefix = messagePrefix;
    int PROJECT_LIST_PAGE_SIZE = 1;
    pager = new Pager(0, PROJECT_LIST_PAGE_SIZE);
  }

  @Override
  public void nextPage(MyteamEvent event) {
    pager.nextPage();
    updateMessage(event, true);
  }

  @Override
  public void prevPage(MyteamEvent event) {
    pager.prevPage();
    updateMessage(event, true);
  }

  public void updateMessage(MyteamEvent event, boolean editMessage) {

    ApplicationUser user = userChatService.getJiraUserFromUserChatId(event.getChatId());
    Locale locale = userChatService.getUserLocale(user);

    List<Project> allowedProjectList = issueService.getAllowedProjects();

    pager.setTotal(allowedProjectList.size());

    List<Project> nextProjectsInterval =
        allowedProjectList.stream()
            .skip((long) pager.getPage() * pager.getPerPage())
            .limit(pager.getPerPage())
            .collect(Collectors.toList());
    try {
      if (event instanceof ButtonClickEvent) {
        userChatService.answerCallbackQuery(((ButtonClickEvent) event).getQueryId());
      }

      String msg =
          messagePrefix
              + messageFormatter.createSelectProjectMessage(
                  locale, nextProjectsInterval, pager.getPage(), allowedProjectList.size());

      List<List<InlineKeyboardMarkupButton>> buttons =
          messageFormatter.buildButtonsWithCancel(
              messageFormatter.getListButtons(locale, pager.hasPrev(), pager.hasNext()),
              userChatService.getRawText(
                  locale,
                  "ru.mail.jira.plugins.myteam.myteamEventsListener.cancelIssueCreationButton.text"));

      if (event instanceof ButtonClickEvent && editMessage) {
        userChatService.editMessageText(
            event.getChatId(), ((ButtonClickEvent) event).getMsgId(), msg, buttons);
      } else {
        userChatService.sendMessageText(event.getChatId(), msg, buttons);
      }
    } catch (IOException | MyteamServerErrorException e) {
      log.error(e.getLocalizedMessage());
    }
  }
}
