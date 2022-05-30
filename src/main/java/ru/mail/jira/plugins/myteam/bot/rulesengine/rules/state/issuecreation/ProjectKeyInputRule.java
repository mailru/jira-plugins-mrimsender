/* (C)2021 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.rules.state.issuecreation;

import com.atlassian.crowd.exception.UserNotFoundException;
import com.atlassian.jira.exception.PermissionException;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.bot.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.exceptions.ProjectBannedException;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.StateActionRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.BaseRule;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.base.BotState;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.issuecreation.CreatingIssueState;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.issuecreation.SelectingProjectState;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.component.MessageFormatter;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;
import ru.mail.jira.plugins.myteam.service.IssueService;
import ru.mail.jira.plugins.myteam.service.RulesEngine;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Slf4j
@Rule(
    name = "project key input result",
    description = "Fired when waiting for project key on input while creating new issue")
public class ProjectKeyInputRule extends BaseRule {

  private final IssueService issueService;

  public ProjectKeyInputRule(
      UserChatService userChatService, RulesEngine rulesEngine, IssueService issueService) {
    super(userChatService, rulesEngine);
    this.issueService = issueService;
  }

  @Condition
  public boolean isValid(
      @Fact("state") BotState state,
      @Fact("prevState") BotState prevState,
      @Fact("args") String projectKey) {
    return state instanceof SelectingProjectState
        && prevState instanceof CreatingIssueState
        && projectKey != null
        && projectKey.length() > 0;
  }

  @Action
  public void execute(
      @Fact("event") MyteamEvent event,
      @Fact("prevState") CreatingIssueState prevState,
      @Fact("args") String projectKey)
      throws MyteamServerErrorException, IOException, UserNotFoundException {
    String chatId = event.getChatId();
    ApplicationUser user = userChatService.getJiraUserFromUserChatId(chatId);
    Locale locale = userChatService.getUserLocale(user);

    try {

      Project project = issueService.getProject(projectKey, user);
      if (project == null) {
        userChatService.sendMessageText(
            chatId,
            userChatService.getRawText(
                locale,
                "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.selectedProjectNotValid"));
      } else {
        prevState.setProject(project);
        userChatService.revertState(chatId);

        Collection<IssueType> projectIssueTypes = issueService.getProjectIssueTypes(project, user);

        userChatService.sendMessageText(
            chatId,
            getSelectIssueTypeMessage(locale),
            MessageFormatter.buildButtonsWithCancel(
                buildIssueTypesButtons(projectIssueTypes, locale),
                userChatService.getRawText(
                    locale,
                    "ru.mail.jira.plugins.myteam.myteamEventsListener.cancelIssueCreationButton.text")));
      }
    } catch (PermissionException e) {
      log.error(e.getLocalizedMessage(), e);
      userChatService.sendMessageText(
          chatId,
          userChatService.getRawText(
              locale,
              "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.notEnoughPermissions"));
    } catch (ProjectBannedException e) {
      log.error(e.getLocalizedMessage(), e);
      userChatService.sendMessageText(
          chatId,
          userChatService.getRawText(
              locale,
              "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.selectedProjectIsBanned"));
    }
  }

  private String getSelectIssueTypeMessage(Locale locale) {
    return userChatService.getRawText(
        locale, "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.selectIssueType.message");
  }

  private List<List<InlineKeyboardMarkupButton>> buildIssueTypesButtons(
      Collection<IssueType> issueTypes, Locale locale) {
    List<List<InlineKeyboardMarkupButton>> buttons = new ArrayList<>();
    issueTypes.forEach(
        issueType -> {
          InlineKeyboardMarkupButton issueTypeButton =
              InlineKeyboardMarkupButton.buildButtonWithoutUrl(
                  issueType.getNameTranslation(locale.getLanguage()),
                  String.join(
                      "-", StateActionRuleType.SelectIssueType.getName(), issueType.getId()));
          MessageFormatter.addRowWithButton(buttons, issueTypeButton);
        });
    return buttons;
  }
}
