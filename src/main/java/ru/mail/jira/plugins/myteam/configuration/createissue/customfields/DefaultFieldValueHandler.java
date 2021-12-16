/* (C)2021 */
package ru.mail.jira.plugins.myteam.configuration.createissue.customfields;

import com.atlassian.jira.issue.customfields.impl.AbstractCustomFieldType;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.sal.api.message.I18nResolver;
import java.util.List;
import java.util.Locale;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;
import ru.mail.jira.plugins.myteam.rulesengine.core.Utils;

public class DefaultFieldValueHandler implements CreateIssueFieldValueHandler {

  private final I18nResolver i18nResolver;

  public DefaultFieldValueHandler(I18nResolver i18nResolver) {
    this.i18nResolver = i18nResolver;
  }

  @Override
  public Class<? extends AbstractCustomFieldType> getCFTypeClass() {
    return null;
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
    return null;
  }

  @Override
  public String updateValue(String value, String newValue) {
    return newValue;
  }

  @Override
  public String[] getValueAsArray(String value, CustomField field) {
    return new String[0];
  }
}
