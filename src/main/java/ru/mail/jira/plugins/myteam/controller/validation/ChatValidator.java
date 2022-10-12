/* (C)2022 */
package ru.mail.jira.plugins.myteam.controller.validation;

import com.atlassian.sal.api.message.I18nResolver;
import java.util.List;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import kong.unirest.HttpResponse;
import org.jetbrains.annotations.Nullable;
import ru.mail.jira.plugins.myteam.controller.validation.annotation.ChatValidation;
import ru.mail.jira.plugins.myteam.controller.validation.provider.ContextProvider;
import ru.mail.jira.plugins.myteam.myteam.MyteamApiClient;
import ru.mail.jira.plugins.myteam.myteam.dto.chats.ChatInfoResponse;

public class ChatValidator implements ConstraintValidator<ChatValidation, List<String>> {
  @Nullable private I18nResolver i18nResolver;
  @Nullable private MyteamApiClient myteamClient;

  @Override
  public void initialize(ChatValidation constraint) {
    this.i18nResolver = (I18nResolver) ContextProvider.getBean(I18nResolver.class);
    this.myteamClient = (MyteamApiClient) ContextProvider.getBean(MyteamApiClient.class);
  }

  @Override
  public boolean isValid(
      List<String> chatIds, ConstraintValidatorContext constraintValidatorContext) {
    boolean isValid = true;
    if (chatIds != null && myteamClient != null) {
      for (String chatId : chatIds) {
        try {
          HttpResponse<ChatInfoResponse> response = myteamClient.getChatInfo(chatId);
          if (response.getStatus() != 200) {
            isValid = false;
            if (i18nResolver != null) {
              constraintValidatorContext.disableDefaultConstraintViolation();
              constraintValidatorContext.buildConstraintViolationWithTemplate(
                  i18nResolver.getText(
                      "ru.mail.jira.plugins.myteam.subscriptions.page.subscription.field.recipients.chats.error.notExist",
                      chatId));
            }
          }
        } catch (Exception ignored) {
          isValid = false;
          if (i18nResolver != null) {
            constraintValidatorContext.disableDefaultConstraintViolation();
            constraintValidatorContext
                .buildConstraintViolationWithTemplate(
                    i18nResolver.getText(
                        "ru.mail.jira.plugins.myteam.subscriptions.page.subscription.field.recipients.chats.error.notExist",
                        chatId))
                .addConstraintViolation();
          }
        }
      }
    }
    return isValid;
  }
}
