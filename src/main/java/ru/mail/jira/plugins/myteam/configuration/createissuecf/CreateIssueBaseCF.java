/* (C)2021 */
package ru.mail.jira.plugins.myteam.configuration.createissuecf;

import com.atlassian.jira.issue.customfields.impl.AbstractCustomFieldType;
import com.atlassian.jira.issue.fields.CustomField;
import java.util.List;
import java.util.Locale;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;
import ru.mail.jira.plugins.myteam.protocol.ChatState;
import ru.mail.jira.plugins.myteam.protocol.IssueCreationDto;

public interface CreateIssueBaseCF {

  Class<? extends AbstractCustomFieldType> getCFTypeClass();

  String getInsertFieldMessage(
      Locale locale, CustomField field, IssueCreationDto issueCreationDto, String newValue);

  List<List<InlineKeyboardMarkupButton>> getButtons(
      Locale locale, CustomField field, IssueCreationDto issueCreationDto, String newValue);

  ChatState getNewChatState(int nextFieldNum, IssueCreationDto issueCreationDto);

  void updateValue(IssueCreationDto issueCreationDto, CustomField field, String newValue);

  String[] getValue(IssueCreationDto issueCreationDto, CustomField field);
}
