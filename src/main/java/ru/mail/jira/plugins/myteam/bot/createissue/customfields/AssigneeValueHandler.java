/* (C)2022 */
package ru.mail.jira.plugins.myteam.bot.createissue.customfields;

import com.atlassian.jira.issue.fields.AssigneeSystemField;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.message.I18nResolver;
import java.util.Locale;
import org.jetbrains.annotations.Nullable;
import ru.mail.jira.plugins.myteam.bot.createissue.FieldInputMessageInfo;
import ru.mail.jira.plugins.myteam.bot.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.commons.Utils;
import ru.mail.jira.plugins.myteam.commons.exceptions.ValidationException;
import ru.mail.jira.plugins.myteam.component.UserData;
import ru.mail.jira.plugins.myteam.rulesengine.states.issuecreation.FillingIssueFieldState;

public class AssigneeValueHandler implements CreateIssueFieldValueHandler {
  private final UserData userData;
  private final I18nResolver i18nResolver;

  public AssigneeValueHandler(UserData userData, I18nResolver i18nResolver) {
    this.userData = userData;
    this.i18nResolver = i18nResolver;
  }

  @Override
  public String getClassName() {
    return AssigneeSystemField.class.getName();
  }

  @Override
  public FieldInputMessageInfo getMessageInfo(
      Project project,
      IssueType issueType,
      ApplicationUser user,
      Locale locale,
      FillingIssueFieldState state) {
    return FieldInputMessageInfo.builder()
        .message(
            i18nResolver.getRawText(
                locale,
                "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.assigneeField.message"))
        .build();
  }

  @Override
  public String updateValue(String value, String newValue, MyteamEvent event)
      throws ValidationException {

    @Nullable String userEmail = Utils.getEmailFromMention(event);

    if (userEmail != null) {
      @Nullable ApplicationUser user = userData.getUserByMrimLogin(userEmail);
      if (user == null) {
        throw new ValidationException("User not found");
      }
      return user.getUsername();
    } else {
      throw new ValidationException("Send mention via @, not common message");
    }
  }
}
