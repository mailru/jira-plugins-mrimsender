/* (C)2023 */
package ru.mail.jira.plugins.myteam.i18n;

import lombok.AllArgsConstructor;

// todo: добавил перечисление для i18n так как код слишком громоздким становится из-за ключей:
// убрать?
@AllArgsConstructor
public enum JiraBotI18nProperties implements I18nProperty {
  COMMENT_CREATED_SUCCESSFUL_MESSAGE_KEY(
      "ru.mail.jira.plugins.myteam.messageQueueProcessor.commentButton.commentCreated"),
  CREATE_COMMENT_NOT_HAVE_PERMISSION_MESSAGE_KEY(
      "ru.mail.jira.plugins.myteam.messageQueueProcessor.commentButton.noPermissions"),
  COMMENT_VALIDATION_ERROR_MESSAGE_KEY(
      "ru.mail.jira.plugins.myteam.messageQueueProcessor.commentButton.commentCreated"),
  COMMENT_NO_HAS_ISSUE_KEY_IN_EVENT_MESSAGE_KEY(
      "ru.mail.jira.plugins.myteam.messageQueueProcessor.commentButton.commentCreated"),
  ;

  private final String messageKey;

  @Override
  public String getMessageKey() {
    return messageKey;
  }
}
