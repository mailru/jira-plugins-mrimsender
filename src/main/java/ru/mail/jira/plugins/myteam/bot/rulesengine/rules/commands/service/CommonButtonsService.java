/* (C)2023 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.rules.commands.service;

import com.atlassian.jira.user.ApplicationUser;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.ButtonRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.CommandRuleType;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Component
public class CommonButtonsService {
  private final UserChatService userChatService;

  public CommonButtonsService(UserChatService userChatService) {
    this.userChatService = userChatService;
  }

  public List<List<InlineKeyboardMarkupButton>> getIssueButtons(
      String issueKey, ApplicationUser recipient, boolean isWatching) {
    List<List<InlineKeyboardMarkupButton>> buttons = new ArrayList<>();
    List<InlineKeyboardMarkupButton> buttonsRow = new ArrayList<>();
    buttons.add(buttonsRow);

    buttonsRow.add(
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            userChatService.getRawText(
                "ru.mail.jira.plugins.myteam.mrimsenderEventListener.commentButton.text"),
            String.join("-", ButtonRuleType.CommentIssue.getName(), issueKey)));

    buttonsRow.add(
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            userChatService.getRawText(
                "ru.mail.jira.plugins.myteam.mrimsenderEventListener.showCommentsButton.text"),
            String.join("-", ButtonRuleType.ViewComments.getName(), issueKey)));

    ArrayList<InlineKeyboardMarkupButton> watchButtonRow = new ArrayList<>();

    watchButtonRow.add(
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            userChatService.getRawText(
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
                "ru.mail.jira.plugins.myteam.mrimsenderEventListener.assign.text"),
            String.join("-", CommandRuleType.AssignIssue.getName(), issueKey)));

    buttons.add(watchButtonRow);

    ArrayList<InlineKeyboardMarkupButton> transitionButtonRow = new ArrayList<>();

    transitionButtonRow.add(
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            userChatService.getRawText(
                "ru.mail.jira.plugins.myteam.messageFormatter.editIssue.transitionChange.title"),
            String.join("-", CommandRuleType.IssueTransition.getName(), issueKey)));

    buttons.add(transitionButtonRow);

    return buttons;
  }
}
