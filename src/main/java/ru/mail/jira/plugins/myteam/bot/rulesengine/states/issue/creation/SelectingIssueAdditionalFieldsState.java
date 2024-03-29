/* (C)2021 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.states.issue.creation;

import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import ru.mail.jira.plugins.myteam.bot.events.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.bot.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.core.Pager;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.StateActionRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.base.BotState;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.base.CancelableState;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.base.PageableState;
import ru.mail.jira.plugins.myteam.commons.IssueFieldsFilter;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.component.MessageFormatter;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;
import ru.mail.jira.plugins.myteam.service.IssueCreationService;
import ru.mail.jira.plugins.myteam.service.RulesEngine;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@SuppressWarnings({"NullAway"})
@Slf4j
public class SelectingIssueAdditionalFieldsState extends BotState
    implements CancelableState, PageableState {

  public static final int ADDITIONAL_FIELD_ONE_COLUMN_MAX_COUNT = 5;
  public static final int ADDITIONAL_FIELD_TWO_COLUMN_MAX_COUNT = 15;

  private final MessageFormatter messageFormatter;
  @Getter private final Project project;

  @Getter private final IssueType issueType;

  private final Pager pager;
  private final UserChatService userChatService;
  private final IssueCreationService issueCreationService;
  private final RulesEngine rulesEngine;

  public SelectingIssueAdditionalFieldsState(
      Project project,
      IssueType issueType,
      UserChatService userChatService,
      IssueCreationService issueCreationService,
      RulesEngine rulesEngine) {
    this.project = project;
    this.issueType = issueType;
    this.userChatService = userChatService;
    this.issueCreationService = issueCreationService;
    this.messageFormatter = userChatService.getMessageFormatter();
    this.rulesEngine = rulesEngine;

    int ADDITIONAL_FIELDS_LIST_PAGE_SIZE = 5;
    pager = new Pager(0, ADDITIONAL_FIELDS_LIST_PAGE_SIZE);
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
    LinkedHashMap<Field, String> nonRequiredFields =
        issueCreationService.getIssueCreationFieldsValues(
            project, issueType, null, null, IssueFieldsFilter.NON_REQUIRED);

    pager.setTotal(nonRequiredFields.size());

    List<Field> pageFieldsInterval =
        nonRequiredFields.keySet().stream()
            .skip((long) pager.getPage() * pager.getPageSize())
            .limit(pager.getPageSize())
            .collect(Collectors.toList());

    String msg =
        userChatService.getRawText(
            "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.selectProject.message");
    List<List<InlineKeyboardMarkupButton>> buttons =
        getSelectAdditionalFieldMessageButtons(
            pager.hasPrev(), pager.hasNext(), pageFieldsInterval);

    try {
      if (event instanceof ButtonClickEvent) {
        userChatService.answerCallbackQuery(((ButtonClickEvent) event).getQueryId());
        userChatService.editMessageText(
            event.getChatId(), ((ButtonClickEvent) event).getMsgId(), msg, buttons);
      } else {
        userChatService.sendMessageText(event.getChatId(), msg, buttons);
      }

    } catch (MyteamServerErrorException | IOException e) {
      log.error(e.getLocalizedMessage(), e);
    }
  }

  @Override
  public void cancel(MyteamEvent event) {
    userChatService.revertState(event.getChatId());
    rulesEngine.fireCommand(StateActionRuleType.ShowCreatingIssueProgressMessage, event);
    if (event instanceof ButtonClickEvent) {
      try {
        userChatService.answerCallbackQuery(((ButtonClickEvent) event).getQueryId());
      } catch (MyteamServerErrorException e) {
        log.error(e.getLocalizedMessage(), e);
      }
    }
  }

  private List<List<InlineKeyboardMarkupButton>> getSelectAdditionalFieldMessageButtons(
      boolean withPrev, boolean withNext, List<Field> fields) {
    List<List<InlineKeyboardMarkupButton>> buttons =
        messageFormatter.getListButtons(withPrev, withNext);

    int colCount =
        fields.size() <= ADDITIONAL_FIELD_ONE_COLUMN_MAX_COUNT
            ? 1
            : (fields.size() <= ADDITIONAL_FIELD_TWO_COLUMN_MAX_COUNT ? 2 : 3);

    List<List<InlineKeyboardMarkupButton>> fieldButtons = new ArrayList<>();
    List<InlineKeyboardMarkupButton> fieldButtonRow = new ArrayList<>();

    int i = 0;
    while (i < fields.size()) {
      Field field = fields.get(i);
      fieldButtonRow.add(
          InlineKeyboardMarkupButton.buildButtonWithoutUrl(
              field.getName(),
              String.join(
                  "-", StateActionRuleType.SelectAdditionalField.getName(), field.getId())));

      if (i % colCount == 0) {
        fieldButtons.add(fieldButtonRow);
        fieldButtonRow = new ArrayList<>();
      }
      i++;
    }

    String cancelTitle =
        userChatService.getRawText(
            "ru.mail.jira.plugins.myteam.mrimsenderEventListener.cancelButton.text");

    if (buttons == null || buttons.size() == 0) { // if no pager buttons
      fieldButtons.add(MessageFormatter.getCancelButtonRow(cancelTitle));
      return fieldButtons;
    } else {
      buttons.get(0).add(InlineKeyboardMarkupButton.buildButtonWithoutUrl(cancelTitle, "cancel"));
      buttons.addAll(0, fieldButtons);
    }

    return buttons;
  }
}
