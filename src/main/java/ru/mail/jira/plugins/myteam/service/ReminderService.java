/* (C)2023 */
package ru.mail.jira.plugins.myteam.service;

import com.atlassian.jira.user.ApplicationUser;
import java.util.List;
import javax.naming.NoPermissionException;
import org.jetbrains.annotations.Nullable;
import ru.mail.jira.plugins.myteam.controller.dto.ReminderDto;

public interface ReminderService {
  int addReminder(ReminderDto reminder, @Nullable ApplicationUser user);

  List<ReminderDto> getIssueReminders(String issueKey, ApplicationUser user);

  void deleteReminder(Integer id, ApplicationUser user) throws NoPermissionException;

  ReminderDto getReminder(Integer id, ApplicationUser user) throws NoPermissionException;
}
