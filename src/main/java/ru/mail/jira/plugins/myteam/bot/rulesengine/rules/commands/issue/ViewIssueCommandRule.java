/* (C)2021 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.rules.commands.issue;

import com.atlassian.jira.exception.IssueNotFoundException;
import com.atlassian.jira.exception.IssuePermissionException;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.user.ApplicationUser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.bot.events.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.bot.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.ButtonRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.CommandRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.ErrorRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.BaseRule;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.ViewingIssueState;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.base.BotState;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;
import ru.mail.jira.plugins.myteam.service.IssueService;
import ru.mail.jira.plugins.myteam.service.RulesEngine;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Rule(name = "/issue", description = "View issue by key")
public class ViewIssueCommandRule extends BaseRule {

  static final RuleType NAME = CommandRuleType.Issue;
  private final IssueService issueService;

  public ViewIssueCommandRule(
      UserChatService userChatService, RulesEngine rulesEngine, IssueService issueService) {
    super(userChatService, rulesEngine);
    this.issueService = issueService;
  }

  @Condition
  public boolean isValid(@Fact("command") String command) {
    return NAME.equalsName(command);
  }

  @Action
  public void execute(
      @Fact("event") MyteamEvent event,
      @Fact("args") String issueKey,
      @Fact("isGroup") boolean isGroup)
      throws MyteamServerErrorException, IOException {
    ApplicationUser user = userChatService.getJiraUserFromUserChatId(event.getUserId());
    if (issueKey.length() > 0 && user != null) {
      String chatId = event.getChatId();
      try {
        Issue issue = issueService.getIssueByUser(issueKey, user);

        userChatService.sendMessageText(
            chatId,
            messageFormatter.createIssueSummary(issue, user),
            isGroup
                ? null
                : getIssueButtons(issue.getKey(), user, issueService.isUserWatching(issue, user)));
        updateState(chatId, issueKey);
      } catch (IssuePermissionException e) {
        rulesEngine.fireError(ErrorRuleType.IssueNoPermission, event, e);
        userChatService.deleteState(chatId);

      } catch (IssueNotFoundException e) {
        rulesEngine.fireError(ErrorRuleType.IssueNotFound, event, e);
        userChatService.deleteState(chatId);
      }
    }

    if (event instanceof ButtonClickEvent) {
      userChatService.answerCallbackQuery(((ButtonClickEvent) event).getQueryId());
    }
  }

  private void updateState(String chatId, String issueKey) {
    BotState state = userChatService.getState(chatId);

    if (state instanceof ViewingIssueState) {
      ((ViewingIssueState) state).setIssueKey(issueKey);
      state.setWaiting(false);
    } else {
      userChatService.setState(chatId, new ViewingIssueState(userChatService, issueKey));
    }
  }

  private List<List<InlineKeyboardMarkupButton>> getIssueButtons(
      String issueKey, ApplicationUser recipient, boolean isWatching) {
    List<List<InlineKeyboardMarkupButton>> buttons = new ArrayList<>();
    List<InlineKeyboardMarkupButton> buttonsRow = new ArrayList<>();
    buttons.add(buttonsRow);

    buttonsRow.add(
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            userChatService.getRawText(
                userChatService.getUserLocale(recipient),
                "ru.mail.jira.plugins.myteam.mrimsenderEventListener.commentButton.text"),
            String.join("-", ButtonRuleType.CommentIssue.getName(), issueKey)));

    buttonsRow.add(
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            userChatService.getRawText(
                userChatService.getUserLocale(recipient),
                "ru.mail.jira.plugins.myteam.mrimsenderEventListener.showCommentsButton.text"),
            String.join("-", ButtonRuleType.ViewComments.getName(), issueKey)));

    ArrayList<InlineKeyboardMarkupButton> watchButtonRow = new ArrayList<>();

    watchButtonRow.add(
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            userChatService.getRawText(
                userChatService.getUserLocale(recipient),
                isWatching
                    ? "ru.mail.jira.plugins.myteam.mrimsenderEventListener.unwatchButton.text"
                    : "ru.mail.jira.plugins.myteam.mrimsenderEventListener.watchButton.text"),
            String.join(
                "-",
                isWatching
                    ? CommandRuleType.UnwatchIssue.getName()
                    : CommandRuleType.WatchIssue.getName(),
                issueKey)));

    watchButtonRow.add(
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            userChatService.getRawText(
                userChatService.getUserLocale(recipient),
                "ru.mail.jira.plugins.myteam.mrimsenderEventListener.assign.text"),
            String.join("-", CommandRuleType.AssignIssue.getName(), issueKey)));

    buttons.add(watchButtonRow);

    ArrayList<InlineKeyboardMarkupButton> transitionButtonRow = new ArrayList<>();

    transitionButtonRow.add(
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            userChatService.getRawText(
                userChatService.getUserLocale(recipient),
                "ru.mail.jira.plugins.myteam.messageFormatter.editIssue.transitionChange.title"),
            String.join("-", CommandRuleType.IssueTransition.getName(), issueKey)));

    buttons.add(transitionButtonRow);

    return buttons;
  }
}
