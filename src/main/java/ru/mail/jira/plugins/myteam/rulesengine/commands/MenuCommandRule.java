/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.commands;

import com.atlassian.jira.user.ApplicationUser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;
import ru.mail.jira.plugins.myteam.protocol.MessageFormatter;
import ru.mail.jira.plugins.myteam.protocol.events.ChatMessageEvent;
import ru.mail.jira.plugins.myteam.rulesengine.UserChatService;

@Rule(name = "/menu command rule", description = "shows menu")
public class MenuCommandRule extends BaseCommandRule {
  static final String NAME = "menu";

  public MenuCommandRule(UserChatService userChatService) {
    super(userChatService);
  }

  @Condition
  public boolean isHelpCommand(@Fact("command") String command) {
    return command.equals(NAME);
  }

  @Action
  public void showMenu(@Fact("event") ChatMessageEvent event)
      throws MyteamServerErrorException, IOException {
    ApplicationUser user = userChatService.getJiraUserFromUserChatId(event.getUserId());
    if (user != null) {
      Locale locale = userChatService.getUserLocale(user);
      myteamClient.sendMessageText(
          event.getChatId(),
          i18nResolver.getRawText(
              locale, "ru.mail.jira.plugins.myteam.messageQueueProcessor.mainMenu.text"),
          getMenuButtons(user));
    }
  }

  public List<List<InlineKeyboardMarkupButton>> getMenuButtons(ApplicationUser user) {
    Locale locale = userChatService.getUserLocale(user);
    List<List<InlineKeyboardMarkupButton>> buttons = new ArrayList<>();

    // create 'search issue' button
    InlineKeyboardMarkupButton showIssueButton =
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            i18nResolver.getRawText(
                locale,
                "ru.mail.jira.plugins.myteam.messageFormatter.mainMenu.showIssueButton.text"),
            "showIssue");
    MessageFormatter.addRowWithButton(buttons, showIssueButton);

    // create 'Active issues assigned to me' button
    InlineKeyboardMarkupButton activeAssignedIssuesButton =
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            i18nResolver.getRawText(
                locale,
                "ru.mail.jira.plugins.myteam.messageFormatter.mainMenu.activeIssuesAssignedToMeButton.text"),
            "activeIssuesAssigned");
    MessageFormatter.addRowWithButton(buttons, activeAssignedIssuesButton);

    // create 'Active issues i watching' button
    InlineKeyboardMarkupButton activeWatchingIssuesButton =
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            i18nResolver.getRawText(
                locale,
                "ru.mail.jira.plugins.myteam.messageFormatter.mainMenu.activeIssuesWatchingByMeButton.text"),
            "activeIssuesWatching");
    MessageFormatter.addRowWithButton(buttons, activeWatchingIssuesButton);

    // create 'Active issues crated by me' button
    InlineKeyboardMarkupButton activeCreatedIssuesButton =
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            i18nResolver.getRawText(
                locale,
                "ru.mail.jira.plugins.myteam.messageFormatter.mainMenu.activeIssuesCreatedByMeButton.text"),
            "activeIssuesCreated");
    MessageFormatter.addRowWithButton(buttons, activeCreatedIssuesButton);

    // create 'Search issue by JQL' button
    InlineKeyboardMarkupButton searchIssueByJqlButton =
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            i18nResolver.getRawText(
                locale,
                "ru.mail.jira.plugins.myteam.messageFormatter.mainMenu.searchIssueByJqlButton.text"),
            "searchByJql");
    MessageFormatter.addRowWithButton(buttons, searchIssueByJqlButton);

    // create 'create issue' button
    InlineKeyboardMarkupButton createIssueButton =
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            i18nResolver.getRawText(
                locale,
                "ru.mail.jira.plugins.myteam.messageFormatter.mainMenu.createIssueButton.text"),
            "createIssue");
    MessageFormatter.addRowWithButton(buttons, createIssueButton);
    return buttons;
  }
}
