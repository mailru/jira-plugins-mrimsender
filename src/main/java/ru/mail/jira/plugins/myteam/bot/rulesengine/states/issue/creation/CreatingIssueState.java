/* (C)2021 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.states.issue.creation;

import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import java.io.IOException;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import ru.mail.jira.plugins.myteam.bot.events.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.bot.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.exceptions.IncorrectIssueTypeException;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.exceptions.UnsupportedCustomFieldsException;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.base.BotState;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.base.CancelableState;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.service.IssueCreationService;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Slf4j
public class CreatingIssueState extends BotState implements CancelableState {
  private static final String DELIMITER_STR = "----------";

  @Getter @Setter private Project project;

  @Getter private IssueType issueType;

  @Getter private Map<Field, String> fieldValues;

  private final UserChatService userChatService;
  private final IssueCreationService issueCreationService;

  public CreatingIssueState(
      UserChatService userChatService, IssueCreationService issueCreationService) {
    setWaiting(true);
    this.userChatService = userChatService;
    this.issueCreationService = issueCreationService;
  }

  @Getter private int currentFieldPosition = 0;

  public void nextField(boolean skipFilled) {
    if (currentFieldPosition < fieldValues.size()) currentFieldPosition++;

    if (skipFilled) {
      getCurrentField()
          .ifPresent(
              field -> {
                String value = fieldValues.get(field);
                if (!value.isEmpty()) nextField(true);
              });
    }
  }

  public Optional<Field> getCurrentField() {
    List<Field> fields = new ArrayList<>(fieldValues.keySet());
    if (currentFieldPosition >= fields.size()) return Optional.empty();
    return Optional.of(fields.get(currentFieldPosition));
  }

  public void setIssueType(IssueType issueType, ApplicationUser user)
      throws UnsupportedCustomFieldsException, IncorrectIssueTypeException {
    this.issueType = issueType;
    List<Field> fields = issueCreationService.getIssueFields(project, user, issueType.getId());
    fieldValues = new HashMap<>();
    fields.forEach(f -> fieldValues.put(f, ""));
  }

  public boolean hasUnfilledFields() {
    return getFirstUnfilledField().isPresent();
  }

  public void setFieldValue(Field field, String value) {
    fieldValues.put(field, value);
  }

  public String getFieldValue(Field field) {
    return fieldValues.get(field);
  }

  public void setCurrentFieldValue(String value) {
    Optional<Field> field = getCurrentField();
    field.ifPresent(f -> fieldValues.put(f, value));
  }

  public void addField(Field field) {
    if (fieldValues.containsKey(field)) {
      fieldValues.remove(field);
    }
    fieldValues.put(field, "");
    currentFieldPosition = getFirstUnfilledFieldPosition();
  }

  public void removeField(Field field) {
    fieldValues.remove(field);
  }

  public String createInsertFieldMessage(Locale locale, String messageHeader) {
    return String.join("\n", messageHeader, this.formatIssueCreationFields(locale, fieldValues));
  }

  private Optional<Field> getFirstUnfilledField() {
    List<Field> fields = new ArrayList<>(fieldValues.keySet());
    return fields.stream().filter(f -> fieldValues.get(f).isEmpty()).findFirst();
  }

  private int getFirstUnfilledFieldPosition() {
    Optional<Field> field = getFirstUnfilledField();
    return field
        .map(value -> new ArrayList<>(fieldValues.keySet()).indexOf(value))
        .orElseGet(() -> fieldValues.size() - 1);
  }

  private String formatIssueCreationFields(Locale locale, Map<Field, String> fieldValuesMap) {
    StringJoiner sj = new StringJoiner("\n");

    sj.add(DELIMITER_STR);
    sj.add(
        userChatService.getRawText(
            locale,
            "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.currentIssueCreationDtoState"));
    sj.add(String.join(" ", userChatService.getRawText(locale, "Project:"), project.getName()));
    sj.add(
        String.join(
            " ",
            userChatService.getRawText(locale, "IssueType:"),
            issueType.getNameTranslation(locale.toString())));
    fieldValuesMap.forEach(
        (field, value) ->
            sj.add(
                String.join(
                    " : ",
                    userChatService.getRawText(locale, field.getNameKey()),
                    value.isEmpty() ? "-" : value)));
    return sj.toString();
  }

  @Override
  public void cancel(MyteamEvent event) {
    userChatService.deleteState(event.getChatId());
    Locale locale = userChatService.getUserLocale(event.getChatId());

    String msg =
        userChatService.getRawText(
            locale, "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.canceled");
    try {
      if (event instanceof ButtonClickEvent) {
        userChatService.answerCallbackQuery(((ButtonClickEvent) event).getQueryId());
        userChatService.editMessageText(
            event.getChatId(), ((ButtonClickEvent) event).getMsgId(), msg, null);
      } else {
        userChatService.sendMessageText(event.getChatId(), msg);
      }
    } catch (MyteamServerErrorException | IOException e) {
      log.error(e.getLocalizedMessage(), e);
    }
  }
}
