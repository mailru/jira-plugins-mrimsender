/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.states;

import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import ru.mail.jira.plugins.myteam.rulesengine.models.exceptions.IncorrectIssueTypeException;
import ru.mail.jira.plugins.myteam.rulesengine.models.exceptions.UnsupportedCustomFieldsException;
import ru.mail.jira.plugins.myteam.rulesengine.service.IssueService;
import ru.mail.jira.plugins.myteam.rulesengine.service.UserChatService;

public class CreatingIssueState extends BotState implements CancelableState {
  private static final String DELIMITER_STR = "----------";

  @Getter @Setter private Project project;

  @Getter private IssueType issueType;

  @Getter private Map<Field, String> fieldValues;

  //  @Getter @Setter private boolean isFillingFields = false;

  private final UserChatService userChatService;
  private final IssueService issueService;

  public CreatingIssueState(UserChatService userChatService, IssueService issueService) {
    setWaiting(true);
    this.userChatService = userChatService;
    this.issueService = issueService;
  }

  @Getter private int currentFieldPosition = 0;

  public int nextField() {
    if (currentFieldPosition < fieldValues.size()) currentFieldPosition++;
    return currentFieldPosition;
  }

  public Optional<Field> getCurrentField() {
    List<Field> fields = new ArrayList<>(fieldValues.keySet());
    if (currentFieldPosition >= fields.size()) return Optional.empty();
    return Optional.of(fields.get(currentFieldPosition));
  }

  public void setIssueType(IssueType issueType, ApplicationUser user)
      throws UnsupportedCustomFieldsException, IncorrectIssueTypeException {
    this.issueType = issueType;
    List<Field> fields = issueService.getIssueFields(project, user, issueType.getId());
    fieldValues = new HashMap<>();
    fields.forEach(f -> fieldValues.put(f, ""));
  }

  public Optional<Field> getFirstUnfilledField() {
    List<Field> fields = new ArrayList<>(fieldValues.keySet());
    return fields.stream().filter(f -> fieldValues.get(f).isEmpty()).findFirst();
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

  public String createInsertFieldMessage(Locale locale, String messagePrefix) {
    return String.join("\n", messagePrefix, this.formatIssueCreationFields(locale, fieldValues));
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
  public String getCancelMessage() {
    return "CANCELED";
  }
}
