/* (C)2022 */
package ru.mail.jira.plugins.myteam.configuration.createissue.customfields;

import com.atlassian.jira.issue.fields.AssigneeSystemField;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.message.I18nResolver;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.mail.jira.plugins.myteam.configuration.UserData;
import ru.mail.jira.plugins.myteam.configuration.createissue.FieldInputMessageInfo;
import ru.mail.jira.plugins.myteam.exceptions.ValidationException;
import ru.mail.jira.plugins.myteam.rulesengine.states.issuecreation.FillingIssueFieldState;

public class AssigneeValueHandler implements CreateIssueFieldValueHandler {
  private final UserData userData;
  private final I18nResolver i18nResolver;

  private static final Pattern pattern = Pattern.compile("^@\\[(.+)]$");

  public AssigneeValueHandler(UserData userData, I18nResolver i18nResolver) {
    this.userData = userData;
    this.i18nResolver = i18nResolver;
  }

  @Override
  public String getClassName() {
    return AssigneeSystemField.class.getName();
  }

  @Override
  public @NotNull FieldInputMessageInfo getMessageInfo(
      @NotNull Project project,
      @NotNull IssueType issueType,
      @NotNull ApplicationUser user,
      @NotNull Locale locale,
      @NotNull FillingIssueFieldState state) {
    return FieldInputMessageInfo.builder()
        .message(
            i18nResolver.getRawText(
                locale,
                "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.assigneeField.message"))
        .build();
  }

  @Override
  public String updateValue(String value, String newValue) throws ValidationException {
    Matcher match = pattern.matcher(newValue);
    if (match.find()) {
      @Nullable ApplicationUser user = userData.getUserByMrimLogin(match.group(1));
      if (user == null) {
        throw new ValidationException("User not found");
      }
      return user.getUsername();
    } else {
      throw new ValidationException("Send mention via @, not common message");
    }
  }
}
