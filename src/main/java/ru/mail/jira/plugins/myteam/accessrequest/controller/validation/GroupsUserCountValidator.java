/* (C)2023 */
package ru.mail.jira.plugins.myteam.accessrequest.controller.validation;

import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.I18nHelper;
import java.util.Collection;
import java.util.List;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.jetbrains.annotations.Nullable;
import ru.mail.jira.plugins.myteam.accessrequest.controller.validation.annotation.GroupsUserCountValidation;
import ru.mail.jira.plugins.myteam.controller.validation.provider.ContextProvider;

public class GroupsUserCountValidator
    implements ConstraintValidator<GroupsUserCountValidation, List<String>> {
  public static final int MAX_USER_COUNT = 50;

  @Nullable private I18nHelper i18nHelper;
  @Nullable private GroupManager groupManager;

  @Override
  public void initialize(final GroupsUserCountValidation constraint) {
    this.i18nHelper = (I18nHelper) ContextProvider.getBean(I18nHelper.class);
    this.groupManager = (GroupManager) ContextProvider.getBean(GroupManager.class);
  }

  @Override
  public boolean isValid(
      List<String> groups, ConstraintValidatorContext constraintValidatorContext) {
    boolean isValid = true;
    if (i18nHelper != null && groupManager != null && groups != null && !groups.isEmpty()) {
      for (String group : groups) {
        Collection<ApplicationUser> groupUsers = groupManager.getUsersInGroup(group);
        if (groupUsers != null && groupUsers.size() > MAX_USER_COUNT) {
          isValid = false;
          constraintValidatorContext.disableDefaultConstraintViolation();
          constraintValidatorContext
              .buildConstraintViolationWithTemplate(
                  i18nHelper.getText(
                      "ru.mail.jira.plugins.myteam.accessRequest.configuration.page.error.validation.groups.users",
                      String.valueOf(MAX_USER_COUNT),
                      group))
              .addConstraintViolation();
          break;
        }
      }
    }
    return isValid;
  }
}
