/* (C)2021 */
package ru.mail.jira.plugins.myteam.configuration.createissue.customfields;

import com.atlassian.jira.issue.customfields.impl.AbstractCustomFieldType;
import com.atlassian.jira.issue.fields.CustomField;
import java.util.List;
import java.util.Locale;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;
import ru.mail.jira.plugins.myteam.protocol.ChatState;
import ru.mail.jira.plugins.myteam.protocol.IssueCreationDto;

public interface CreateIssueBaseCF {

  Class<? extends AbstractCustomFieldType> getCFTypeClass();

  /**
   * Custom text render for Myteam message
   *
   * @param locale user locale
   * @param field current custom field
   * @param issueCreationDto issue data for creation
   * @param newValue String value from button or text input
   * @return Message to shown in Myteam
   */
  String getInsertFieldMessage(
      Locale locale, CustomField field, IssueCreationDto issueCreationDto, String newValue);

  /**
   * Custom buttons setup attached to issue creation message
   *
   * @param locale user locale
   * @param field current custom field
   * @param issueCreationDto issue data for creation
   * @param newValue String value from button or text input
   * @return Buttons to shown in Myteam
   */
  List<List<InlineKeyboardMarkupButton>> getButtons(
      Locale locale, CustomField field, IssueCreationDto issueCreationDto, String newValue);

  /**
   * Method for get updated chat state
   *
   * @param nextFieldNum current editing field index
   * @param issueCreationDto issue data for creation
   * @return new ChatState
   */
  ChatState getNewChatState(int nextFieldNum, IssueCreationDto issueCreationDto);

  void updateValue(IssueCreationDto issueCreationDto, CustomField field, String newValue);

  /**
   * Map field value from String in IssueCreationDto to valid String array field value
   *
   * @param issueCreationDto issue data for creation
   * @param field current custom field
   * @return valid String array for field in IssueInputParameters
   */
  String[] getValue(IssueCreationDto issueCreationDto, CustomField field);
}
