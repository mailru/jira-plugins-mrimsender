/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.rules.state.issuecreation;

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
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;
import ru.mail.jira.plugins.myteam.protocol.MessageFormatter;
import ru.mail.jira.plugins.myteam.protocol.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.rulesengine.models.BaseRule;
import ru.mail.jira.plugins.myteam.rulesengine.models.exceptions.ProjectBannedException;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.StateActionRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.service.IssueService;
import ru.mail.jira.plugins.myteam.rulesengine.service.RulesEngine;
import ru.mail.jira.plugins.myteam.rulesengine.service.UserChatService;
import ru.mail.jira.plugins.myteam.rulesengine.states.BotState;
import ru.mail.jira.plugins.myteam.rulesengine.states.CreatingIssueState;
import ru.mail.jira.plugins.myteam.rulesengine.states.SelectingProjectState;

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
      throws MyteamServerErrorException, IOException {
    String chatId = event.getChatId();

    ApplicationUser user = userChatService.getJiraUserFromUserChatId(chatId);
    if (user != null) {
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
          //          userChatService.deleteState(chatId);
          //          userChatService.setState(chatId, prevState);

          Collection<IssueType> projectIssueTypes =
              issueService.getProjectIssueTypes(project, user);

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
        log.error(e.getLocalizedMessage());
        userChatService.sendMessageText(
            chatId,
            userChatService.getRawText(
                locale,
                "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.notEnoughPermissions"));
      } catch (ProjectBannedException e) {
        log.error(e.getLocalizedMessage());
        userChatService.sendMessageText(
            chatId,
            userChatService.getRawText(
                locale,
                "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.selectedProjectIsBanned"));
      }
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
