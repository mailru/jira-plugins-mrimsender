/* (C)2021 */
package ru.mail.jira.plugins.myteam.configuration.createissue.customfields;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.issue.fields.PrioritySystemField;
import com.atlassian.jira.issue.priority.Priority;
import com.atlassian.sal.api.message.I18nResolver;
import java.util.*;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;
import ru.mail.jira.plugins.myteam.protocol.MessageFormatter;
import ru.mail.jira.plugins.myteam.rulesengine.core.Utils;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.ButtonRuleType;

public class PriorityValueHandler implements CreateIssueFieldValueHandler {
  private final I18nResolver i18nResolver;
  private final ConstantsManager constantsManager;

  public PriorityValueHandler(I18nResolver i18nResolver) {
    this.i18nResolver = i18nResolver;
    constantsManager = ComponentAccessor.getConstantsManager();
  }

  @Override
  public String getClassName() {
    return PrioritySystemField.class.getName();
  }

  @Override
  public String getInsertFieldMessage(Field field, Locale locale) {
    if (Utils.isArrayLikeField(field)) {
      return i18nResolver.getText(
          locale,
          "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.insertIssueField.arrayMessage",
          i18nResolver.getRawText(locale, field.getNameKey()).toLowerCase(locale));
    }
    return i18nResolver.getText(
        locale,
        "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.insertIssueField.message",
        i18nResolver.getRawText(locale, field.getNameKey()).toLowerCase(locale));
  }

  @Override
  public List<List<InlineKeyboardMarkupButton>> getButtons(
      Field field, String value, Locale locale) {

    List<List<InlineKeyboardMarkupButton>> buttons = new ArrayList<>();
    Collection<Priority> priorities = constantsManager.getPriorities();
    priorities.forEach(
        priority -> {
          InlineKeyboardMarkupButton issueTypeButton =
              InlineKeyboardMarkupButton.buildButtonWithoutUrl(
                  priority.getNameTranslation(locale.getLanguage()),
                  String.join(
                      "-", ButtonRuleType.SelectIssueCreationValue.getName(), priority.getName()));
          MessageFormatter.addRowWithButton(buttons, issueTypeButton);
        });
    return buttons;
  }

  @Override
  public String updateValue(String value, String newValue) {
    return newValue;
  }

  @Override
  public String[] getValueAsArray(String value, Field field) {
    return Collections.singletonList(value).toArray(new String[0]);
  }
}
