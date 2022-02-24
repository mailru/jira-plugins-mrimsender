/* (C)2020 */
package ru.mail.jira.plugins.myteam.service;

import com.atlassian.jira.issue.Issue;
import java.util.ArrayList;
import ru.mail.jira.plugins.myteam.service.dto.FieldDto;

public interface HtmlFieldFactory {

  ArrayList<FieldDto> getFields(Issue issue, boolean requiredOnlye);
}
