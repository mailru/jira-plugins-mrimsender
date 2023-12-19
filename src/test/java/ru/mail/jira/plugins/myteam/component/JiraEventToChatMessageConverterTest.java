/* (C)2021 */
package ru.mail.jira.plugins.myteam.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.issue.AttachmentManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.screen.FieldScreenManager;
import com.atlassian.jira.issue.fields.screen.issuetype.IssueTypeScreenSchemeManager;
import com.atlassian.jira.issue.security.IssueSecurityLevelManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.sal.api.message.I18nResolver;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.GenericValue;
import ru.mail.jira.plugins.myteam.service.PluginData;

@SuppressWarnings({"MockNotUsedInProduction", "UnusedVariable"})
class JiraEventToChatMessageConverterTest {

  private JiraEventToChatMessageConverter jiraEventToChatMessageConverter;
  private IssueEvent mockedIssueEvent;
  private ApplicationUser recipient;

  private I18nResolver i18nResolver;

  private PluginMentionService pluginMentionService;

  @BeforeEach
  void init() {
    ApplicationProperties applicationProperties = Mockito.mock(ApplicationProperties.class);
    DateTimeFormatter dateTimeFormatter = Mockito.mock(DateTimeFormatter.class);
    FieldManager fieldManager = Mockito.mock(FieldManager.class);
    IssueSecurityLevelManager issueSecurityLevelManager =
        Mockito.mock(IssueSecurityLevelManager.class);
    I18nHelper i18nHelper = Mockito.mock(I18nHelper.class);
    IssueTypeScreenSchemeManager issueTypeScreenSchemeManager =
        Mockito.mock(IssueTypeScreenSchemeManager.class);
    FieldScreenManager fieldScreenManager = Mockito.mock(FieldScreenManager.class);
    I18nResolver i18nResolver = Mockito.mock(I18nResolver.class);
    this.i18nResolver = i18nResolver;
    UserManager userManager = Mockito.mock(UserManager.class);
    ApplicationUser mentionUser = Mockito.mock(ApplicationUser.class);
    when(mentionUser.getEmailAddress()).thenReturn("i.pupkin@domain");
    when(mentionUser.getDisplayName()).thenReturn("Pupkin");
    when(userManager.getUserByName("i.pupkin@domain")).thenReturn(mentionUser);
    AttachmentManager attachmentManager = Mockito.mock(AttachmentManager.class);
    PluginData pluginData = Mockito.mock(PluginData.class);
    PluginMentionService pluginMentionService = mock(PluginMentionService.class);
    this.pluginMentionService = pluginMentionService;
    this.jiraEventToChatMessageConverter =
        new JiraEventToChatMessageConverter(
            new MessageFormatter(
                applicationProperties,
                dateTimeFormatter,
                fieldManager,
                issueSecurityLevelManager,
                i18nHelper,
                issueTypeScreenSchemeManager,
                fieldScreenManager,
                i18nResolver,
                pluginData,
                mock(JiraMarkdownToChatMarkdownConverter.class)),
            new JiraMarkdownToChatMarkdownConverter(userManager, i18nHelper, pluginData),
            new DiffFieldChatMessageGenerator(),
            attachmentManager,
            applicationProperties,
            i18nResolver,
            i18nHelper,
            fieldManager,
            userManager,
            pluginMentionService);
    IssueEvent event = Mockito.mock(IssueEvent.class);
    Issue mockedIssue = Mockito.mock(Issue.class);
    ApplicationUser assigneedUser = Mockito.mock(ApplicationUser.class);
    when(assigneedUser.getEmailAddress()).thenReturn("kucher@mail.ru");
    when(assigneedUser.getUsername()).thenReturn("kucher");
    when(assigneedUser.getDisplayName()).thenReturn("Павел Кучер");
    when(mockedIssue.getKey()).thenReturn("TEST-1");
    when(mockedIssue.getAssignee()).thenReturn(assigneedUser);
    when(mockedIssue.getSummary()).thenReturn("Summary");
    when(userManager.getUserByName("kucher")).thenReturn(assigneedUser);
    when(event.getIssue()).thenReturn(mockedIssue);
    when(event.getEventTypeId()).thenReturn(2L);
    when(i18nResolver.getText(eq("ru.mail.jira.plugins.myteam.notify.diff.description.field")))
        .thenReturn("help description message");
    this.mockedIssueEvent = event;
    this.recipient = assigneedUser;
  }

  @Test
  void testsingleLineCodeWithoutDiff() throws GenericEntityException {
    // GIVEN
    GenericValue changeLog = mock(GenericValue.class);
    List<GenericValue> changeLogRelated = new ArrayList<>();
    GenericValue descriptionField = Mockito.mock(GenericValue.class);
    when(descriptionField.getString("field")).thenReturn("description");
    when(descriptionField.getString("newstring"))
        .thenReturn(
            "_Lorem_ ipsum dolor sit amet, -consectetur adipiscing- elit, sed* do eiusmod tempor incididunt ut *labore* -et [dolore|http://example.com] magna aliqua-. Ut enim ad minim veniam, *quis* +nostrud exercitation ullamco+ {{labo-ris}} *nisi ut aliquip* ex ea commodo * +consequat. Duis+ aute iru-re dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. *Excepteur sint occaecat cupidatat* non proident, -sunt in culpa qui officia- *deserunt mollit* anim id est -laborum-.\n"
                + "\n"
                + "\n"
                + "{{asdf asdf asdf\n"
                + "asdf asd}}"
                + "\n"
                + "+asdadda+ asd asd. asd\n"
                + "asdasd as. a sd +aasdad+");
    changeLogRelated.add(descriptionField);
    when(changeLog.getRelated("ChildChangeItem")).thenReturn(changeLogRelated);
    when(this.mockedIssueEvent.getChangeLog()).thenReturn(changeLog);
    String testedHeader = "null\nSummary\n\n";
    String testedContent =
        "_Lorem_ ipsum dolor sit amet, ~consectetur adipiscing~ elit, sed\\* do eiusmod tempor incididunt ut *labore* ~et [dolore](http://example.com) magna aliqua~. Ut enim ad minim veniam, *quis* __nostrud exercitation ullamco__ `labo\\-ris` *nisi ut aliquip* ex ea commodo \\* __consequat. Duis__ aute iru\\-re dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. *Excepteur sint occaecat cupidatat* non proident, ~sunt in culpa qui officia~ *deserunt mollit* anim id est ~laborum~.\n"
            + "\n"
            + "\n"
            + "\\{\\{asdf asdf asdf\n"
            + "asdf asd\\}\\}"
            + "\n"
            + "__asdadda__ asd asd. asd\n"
            + "asdasd as. a sd __aasdad__";

    // WHEN
    String result =
        this.jiraEventToChatMessageConverter.formatEventWithDiff(recipient, this.mockedIssueEvent);

    // THEN
    assertEquals(testedHeader + testedContent, result);
  }

  @Test
  void JD1784TaskTestTrue() throws GenericEntityException {
    // GIVEN
    GenericValue changeLog = mock(GenericValue.class);
    List<GenericValue> changeLogRelated = new ArrayList<>();
    GenericValue descriptionField = Mockito.mock(GenericValue.class);
    when(descriptionField.getString("field")).thenReturn("description");
    when(descriptionField.getString("newstring"))
        .thenReturn("Начало предложения -какой либо текст для проверки- ДОЛЖНО");
    changeLogRelated.add(descriptionField);
    when(changeLog.getRelated("ChildChangeItem")).thenReturn(changeLogRelated);
    when(this.mockedIssueEvent.getChangeLog()).thenReturn(changeLog);
    String testedHeader = "null\nSummary\n\n";
    String testedContent = "Начало предложения ~какой либо текст для проверки~ ДОЛЖНО";

    // WHEN
    String result =
        this.jiraEventToChatMessageConverter.formatEventWithDiff(recipient, this.mockedIssueEvent);

    // THEN
    assertEquals(testedHeader + testedContent, result);
  }

  @Test
  void JD1784TaskTestFalse() throws GenericEntityException {
    // GIVEN
    GenericValue changeLog = mock(GenericValue.class);
    List<GenericValue> changeLogRelated = new ArrayList<>();
    GenericValue descriptionField = Mockito.mock(GenericValue.class);
    when(descriptionField.getString("field")).thenReturn("description");
    when(descriptionField.getString("newstring"))
        .thenReturn("Начало предложения - какой либо текст для проверки - НЕДОЛЖНО");
    changeLogRelated.add(descriptionField);
    when(changeLog.getRelated("ChildChangeItem")).thenReturn(changeLogRelated);
    when(this.mockedIssueEvent.getChangeLog()).thenReturn(changeLog);
    String testedHeader = "null\nSummary\n\n";
    String testedContent = "Начало предложения \\- какой либо текст для проверки \\- НЕДОЛЖНО";

    // WHEN
    String result =
        this.jiraEventToChatMessageConverter.formatEventWithDiff(recipient, this.mockedIssueEvent);

    // THEN
    assertEquals(testedHeader + testedContent, result);
  }

  @Test
  void formatEventWithDiffTest() throws GenericEntityException {
    // GIVEN
    String prevDescriptionFieldMarkup =
        "+Lorem ipsum dolor sit amet+, consectetur adipiscing elit, "
            + "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, "
            + "quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo  consequat. "
            + "Duis aute irure dolor in _reprehenderit_ in voluptate velit esse cillum dolore eu fugiat nulla pariatur."
            + " Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";
    String newDescriptionFieldMarkup =
        "+Lorem ipsum dolor sit amet+, consectetur adipiscing elit, "
            + "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, "
            + "quis nostrud exercitation ullamco ex ea commodo  consequat. "
            + "Duis aute irure dolor in _reprehe_ in voluptate velit esse cillum dolore eu fugiat nulla pariatur."
            + " Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";

    GenericValue changeLog = mock(GenericValue.class);
    List<GenericValue> changeLogRelated = new ArrayList<>();
    GenericValue descriptionField = Mockito.mock(GenericValue.class);
    when(descriptionField.getString("field")).thenReturn("description");
    when(descriptionField.getString("newstring")).thenReturn(newDescriptionFieldMarkup);
    when(descriptionField.getString("oldstring")).thenReturn(prevDescriptionFieldMarkup);
    changeLogRelated.add(descriptionField);
    when(changeLog.getRelated("ChildChangeItem")).thenReturn(changeLogRelated);
    when(this.mockedIssueEvent.getChangeLog()).thenReturn(changeLog);
    String testedHeader = "null\nSummary\n\n";
    String expectedResult =
        testedHeader
            + "\\+Lorem ipsum dolor sit amet\\+, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco ~ laboris nisi ut aliquip~  ex ea commodoconsequat. Duis aute irure dolor in ~ _reprehenderit_~ * _reprehe_*  in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum. \n"
            + "\n"
            + ">___help description message___";

    // WHEN
    String actualResult =
        this.jiraEventToChatMessageConverter.formatEventWithDiff(recipient, this.mockedIssueEvent);

    // THEN
    assertEquals(expectedResult, actualResult);
  }

  @Test
  void formatEventWithDiffTestWhenOldOrNewDescHasQuotes() throws GenericEntityException {
    // GIVEN
    String prevDescriptionFieldMarkup =
        "+Lorem ipsum dolor sit amet+, {quote}\nlorem\nlorem\n{quote} consectetur adipiscing elit, "
            + "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, "
            + "quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo  consequat. "
            + "Duis aute irure dolor in _reprehenderit_ in voluptate velit esse cillum dolore eu fugiat nulla pariatur."
            + " Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";
    String newDescriptionFieldMarkup =
        "+Lorem ipsum dolor sit amet+, consectetur adipiscing elit, "
            + "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, "
            + "quis nostrud exercitation ullamco ex ea commodo  consequat. "
            + "Duis aute irure dolor in _reprehe_ in voluptate velit esse cillum dolore eu fugiat nulla pariatur."
            + " Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";

    GenericValue changeLog = mock(GenericValue.class);
    List<GenericValue> changeLogRelated = new ArrayList<>();
    GenericValue descriptionField = Mockito.mock(GenericValue.class);
    when(descriptionField.getString("field")).thenReturn("description");
    when(descriptionField.getString("newstring")).thenReturn(newDescriptionFieldMarkup);
    when(descriptionField.getString("oldstring")).thenReturn(prevDescriptionFieldMarkup);
    changeLogRelated.add(descriptionField);
    when(changeLog.getRelated("ChildChangeItem")).thenReturn(changeLogRelated);
    when(this.mockedIssueEvent.getChangeLog()).thenReturn(changeLog);
    String testedHeader = "null\nSummary\n\n";
    String expectedResult =
        testedHeader
            + "\\+Lorem ipsum dolor sit amet\\+, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco ex ea commodo  consequat. Duis aute irure dolor in _reprehe_ in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";

    // WHEN
    String actualResult =
        this.jiraEventToChatMessageConverter.formatEventWithDiff(recipient, this.mockedIssueEvent);

    // THEN
    assertEquals(expectedResult, actualResult);
  }

  @Test
  void formatEventWithDiffTestWhenOldOrNewDescHasCodeBlock() throws GenericEntityException {
    // GIVEN
    String prevDescriptionFieldMarkup =
        "+Lorem ipsum dolor sit amet+, {code:xml}\n"
            + "    <test>\n"
            + "        <another tag=\"attribute\"/>\n"
            + "    </test>\n"
            + "{code} consectetur adipiscing elit, "
            + "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, "
            + "quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo  consequat. "
            + "Duis aute irure dolor in _reprehenderit_ in voluptate velit esse cillum dolore eu fugiat nulla pariatur."
            + " Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";
    String newDescriptionFieldMarkup =
        "+Lorem ipsum dolor sit amet+, consectetur adipiscing elit, "
            + "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, "
            + "quis nostrud exercitation ullamco ex ea commodo  consequat. "
            + "Duis aute irure dolor in _reprehe_ in voluptate velit esse cillum dolore eu fugiat nulla pariatur."
            + " Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";

    GenericValue changeLog = mock(GenericValue.class);
    List<GenericValue> changeLogRelated = new ArrayList<>();
    GenericValue descriptionField = Mockito.mock(GenericValue.class);
    when(descriptionField.getString("field")).thenReturn("description");
    when(descriptionField.getString("newstring")).thenReturn(newDescriptionFieldMarkup);
    when(descriptionField.getString("oldstring")).thenReturn(prevDescriptionFieldMarkup);
    changeLogRelated.add(descriptionField);
    when(changeLog.getRelated("ChildChangeItem")).thenReturn(changeLogRelated);
    when(this.mockedIssueEvent.getChangeLog()).thenReturn(changeLog);
    String testedHeader = "null\nSummary\n\n";
    String expectedResult =
        testedHeader
            + "\\+Lorem ipsum dolor sit amet\\+, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco ex ea commodo  consequat. Duis aute irure dolor in _reprehe_ in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";

    // WHEN
    String actualResult =
        this.jiraEventToChatMessageConverter.formatEventWithDiff(recipient, this.mockedIssueEvent);

    // THEN
    assertEquals(expectedResult, actualResult);
  }

  @Test
  void formatEventWithDiffTestWhenOldOrNewDescHasUnorderedList() throws GenericEntityException {
    // GIVEN
    String prevDescriptionFieldMarkup =
        "+Lorem ipsum dolor sit amet+,  consectetur adipiscing elit, "
            + "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, "
            + "quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo  consequat. "
            + "Duis aute irure dolor in _reprehenderit_ in voluptate velit esse cillum dolore eu fugiat nulla pariatur."
            + " Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";
    String newDescriptionFieldMarkup =
        "+Lorem ipsum dolor sit amet+,\n"
            + "* bullet\n"
            + "* indented\n"
            + "consectetur adipiscing elit, "
            + "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, "
            + "quis nostrud exercitation ullamco ex ea commodo  consequat. "
            + "Duis aute irure dolor in _reprehe_ in voluptate velit esse cillum dolore eu fugiat nulla pariatur."
            + " Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";

    GenericValue changeLog = mock(GenericValue.class);
    List<GenericValue> changeLogRelated = new ArrayList<>();
    GenericValue descriptionField = Mockito.mock(GenericValue.class);
    when(descriptionField.getString("field")).thenReturn("description");
    when(descriptionField.getString("newstring")).thenReturn(newDescriptionFieldMarkup);
    when(descriptionField.getString("oldstring")).thenReturn(prevDescriptionFieldMarkup);
    changeLogRelated.add(descriptionField);
    when(changeLog.getRelated("ChildChangeItem")).thenReturn(changeLogRelated);
    when(this.mockedIssueEvent.getChangeLog()).thenReturn(changeLog);
    String testedHeader = "null\nSummary\n\n";
    String expectedResult =
        testedHeader
            + "\\+Lorem ipsum dolor sit amet\\+,\n"
            + "- bullet\n"
            + "- indented\n"
            + "consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco ex ea commodo  consequat. Duis aute irure dolor in _reprehe_ in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";

    // WHEN
    String actualResult =
        this.jiraEventToChatMessageConverter.formatEventWithDiff(recipient, this.mockedIssueEvent);

    // THEN
    assertEquals(expectedResult, actualResult);
  }

  @Test
  void formatEventWithDiffTestWhenOldOrNewDescHasOrderedList() throws GenericEntityException {
    // GIVEN
    String prevDescriptionFieldMarkup =
        "+Lorem ipsum dolor sit amet+,  consectetur adipiscing elit, "
            + "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, "
            + "quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo  consequat. "
            + "Duis aute irure dolor in _reprehenderit_ in voluptate velit esse cillum dolore eu fugiat nulla pariatur."
            + " Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";
    String newDescriptionFieldMarkup =
        "+Lorem ipsum dolor sit amet+,\n"
            + "# bullet\n"
            + "# indented\n"
            + "consectetur adipiscing elit, "
            + "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, "
            + "quis nostrud exercitation ullamco ex ea commodo  consequat. "
            + "Duis aute irure dolor in _reprehe_ in voluptate velit esse cillum dolore eu fugiat nulla pariatur."
            + " Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";

    GenericValue changeLog = mock(GenericValue.class);
    List<GenericValue> changeLogRelated = new ArrayList<>();
    GenericValue descriptionField = Mockito.mock(GenericValue.class);
    when(descriptionField.getString("field")).thenReturn("description");
    when(descriptionField.getString("newstring")).thenReturn(newDescriptionFieldMarkup);
    when(descriptionField.getString("oldstring")).thenReturn(prevDescriptionFieldMarkup);
    changeLogRelated.add(descriptionField);
    when(changeLog.getRelated("ChildChangeItem")).thenReturn(changeLogRelated);
    when(this.mockedIssueEvent.getChangeLog()).thenReturn(changeLog);
    String testedHeader = "null\nSummary\n\n";
    String expectedResult =
        testedHeader
            + "\\+Lorem ipsum dolor sit amet\\+,\n"
            + "- bullet\n"
            + "- indented\n"
            + "consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco ex ea commodo  consequat. Duis aute irure dolor in _reprehe_ in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";

    // WHEN
    String actualResult =
        this.jiraEventToChatMessageConverter.formatEventWithDiff(recipient, this.mockedIssueEvent);

    // THEN
    assertEquals(expectedResult, actualResult);
  }

  @Test
  void formatEventWithDiffTestWhenOldOrNewDescHasBoldText() throws GenericEntityException {
    // GIVEN
    String prevDescriptionFieldMarkup =
        "+Lorem ipsum dolor sit amet+, consectetur adipiscing elit, "
            + "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, "
            + "quis nostrud exercitation *ullamco* laboris nisi ut aliquip ex ea commodo  consequat. "
            + "Duis aute irure dolor in _reprehenderit_ in voluptate velit esse cillum dolore eu fugiat nulla pariatur."
            + " Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";
    String newDescriptionFieldMarkup =
        "+Lorem ipsum dolor sit amet+, "
            + "consectetur adipiscing elit, "
            + "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, "
            + "quis nostrud exercitation ullamco ex ea commodo  consequat. "
            + "Duis aute irure dolor in _reprehe_ in voluptate velit esse cillum dolore eu fugiat nulla pariatur."
            + " Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";

    GenericValue changeLog = mock(GenericValue.class);
    List<GenericValue> changeLogRelated = new ArrayList<>();
    GenericValue descriptionField = Mockito.mock(GenericValue.class);
    when(descriptionField.getString("field")).thenReturn("description");
    when(descriptionField.getString("newstring")).thenReturn(newDescriptionFieldMarkup);
    when(descriptionField.getString("oldstring")).thenReturn(prevDescriptionFieldMarkup);
    changeLogRelated.add(descriptionField);
    when(changeLog.getRelated("ChildChangeItem")).thenReturn(changeLogRelated);
    when(this.mockedIssueEvent.getChangeLog()).thenReturn(changeLog);
    String testedHeader = "null\nSummary\n\n";
    String expectedResult =
        testedHeader
            + "\\+Lorem ipsum dolor sit amet\\+, "
            + "consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco ex ea commodo  consequat. Duis aute irure dolor in _reprehe_ in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";

    // WHEN
    String actualResult =
        this.jiraEventToChatMessageConverter.formatEventWithDiff(recipient, this.mockedIssueEvent);

    // THEN
    assertEquals(expectedResult, actualResult);
  }

  @Test
  void formatEventWithDiffTestWhenOldOrNewDescHasStrikeThrough() throws GenericEntityException {
    // GIVEN
    String prevDescriptionFieldMarkup =
        "+Lorem ipsum dolor sit amet+, consectetur adipiscing elit, "
            + "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, "
            + "quis nostrud exercitation -ullamc- laboris nisi ut aliquip ex ea commodo  consequat. "
            + "Duis aute irure dolor in _reprehenderit_ in voluptate velit esse cillum dolore eu fugiat nulla pariatur."
            + " Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";
    String newDescriptionFieldMarkup =
        "+Lorem ipsum dolor sit amet+, "
            + "consectetur adipiscing elit, "
            + "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, "
            + "quis nostrud exercitation ullamco ex ea commodo  consequat. "
            + "Duis aute irure dolor in _reprehe_ in voluptate velit esse cillum dolore eu fugiat nulla pariatur."
            + " Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";

    GenericValue changeLog = mock(GenericValue.class);
    List<GenericValue> changeLogRelated = new ArrayList<>();
    GenericValue descriptionField = Mockito.mock(GenericValue.class);
    when(descriptionField.getString("field")).thenReturn("description");
    when(descriptionField.getString("newstring")).thenReturn(newDescriptionFieldMarkup);
    when(descriptionField.getString("oldstring")).thenReturn(prevDescriptionFieldMarkup);
    changeLogRelated.add(descriptionField);
    when(changeLog.getRelated("ChildChangeItem")).thenReturn(changeLogRelated);
    when(this.mockedIssueEvent.getChangeLog()).thenReturn(changeLog);
    String testedHeader = "null\nSummary\n\n";
    String expectedResult =
        testedHeader
            + "\\+Lorem ipsum dolor sit amet\\+, "
            + "consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco ex ea commodo  consequat. Duis aute irure dolor in _reprehe_ in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";

    // WHEN
    String actualResult =
        this.jiraEventToChatMessageConverter.formatEventWithDiff(recipient, this.mockedIssueEvent);

    // THEN
    assertEquals(expectedResult, actualResult);
  }

  @Test
  void formatEventWithDiffTestWhenOldOrNewDescHasStrikeThroughAndSomeFieldHasNewAndOldValue()
      throws GenericEntityException {
    // GIVEN
    String prevDescriptionFieldMarkup =
        "+Lorem ipsum dolor sit amet+, consectetur adipiscing elit, "
            + "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, "
            + "quis nostrud exercitation -ullamc- laboris nisi ut aliquip ex ea commodo  consequat. "
            + "Duis aute irure dolor in _reprehenderit_ in voluptate velit esse cillum dolore eu fugiat nulla pariatur."
            + " Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";
    String newDescriptionFieldMarkup =
        "+Lorem ipsum dolor sit amet+, "
            + "consectetur adipiscing elit, "
            + "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, "
            + "quis nostrud exercitation ullamco ex ea commodo  consequat. "
            + "Duis aute irure dolor in _reprehe_ in voluptate velit esse cillum dolore eu fugiat nulla pariatur."
            + " Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";

    GenericValue changeLog = mock(GenericValue.class);
    List<GenericValue> changeLogRelated = new ArrayList<>();
    GenericValue descriptionField = Mockito.mock(GenericValue.class);
    GenericValue fieldNumber = Mockito.mock(GenericValue.class);
    GenericValue fieldText = Mockito.mock(GenericValue.class);
    when(descriptionField.getString("field")).thenReturn("description");
    when(descriptionField.getString("newstring")).thenReturn(newDescriptionFieldMarkup);
    when(descriptionField.getString("oldstring")).thenReturn(prevDescriptionFieldMarkup);
    when(fieldNumber.getString("field")).thenReturn("NumberField");
    when(fieldNumber.getString("fieldtype")).thenReturn("custom");
    when(fieldNumber.getString("newstring")).thenReturn("8.21");
    when(fieldNumber.getString("oldstring")).thenReturn("8.20");
    when(fieldText.getString("field")).thenReturn("TextField");
    when(fieldText.getString("fieldtype")).thenReturn("custom");
    when(fieldText.getString("newstring")).thenReturn("NewTextInField");
    when(fieldText.getString("oldstring")).thenReturn("OldTextInField");
    changeLogRelated.add(fieldNumber);
    changeLogRelated.add(fieldText);
    changeLogRelated.add(descriptionField);
    when(changeLog.getRelated("ChildChangeItem")).thenReturn(changeLogRelated);
    when(this.mockedIssueEvent.getChangeLog()).thenReturn(changeLog);
    String testedHeader = "null\nSummary\n\n";
    String expectedResult =
        testedHeader
            + "NumberField: ~8\\.20~ *8\\.21*"
            + "\n"
            + "TextField: ~OldTextInField~ *NewTextInField*"
            + "\n\n\\+Lorem ipsum dolor sit amet\\+, "
            + "consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco ex ea commodo  consequat. Duis aute irure dolor in _reprehe_ in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";

    // WHEN
    String actualResult =
        this.jiraEventToChatMessageConverter.formatEventWithDiff(recipient, this.mockedIssueEvent);

    // THEN
    assertEquals(expectedResult, actualResult);
  }

  @Test
  void checkThatNewDescriptionHasUserMentionInText() throws GenericEntityException {
    // GIVEN
    String prevDescriptionFieldMarkup = null;
    String newDescriptionFieldMarkup = "text\n\n[~kucher]";
    GenericValue changeLog = mock(GenericValue.class);
    List<GenericValue> changeLogRelated = new ArrayList<>();
    GenericValue descriptionField = Mockito.mock(GenericValue.class);
    when(descriptionField.getString("field")).thenReturn("description");
    when(descriptionField.getString("newstring")).thenReturn(newDescriptionFieldMarkup);
    when(descriptionField.getString("oldstring")).thenReturn(prevDescriptionFieldMarkup);
    changeLogRelated.add(descriptionField);
    when(changeLog.getRelated("ChildChangeItem")).thenReturn(changeLogRelated);
    when(this.mockedIssueEvent.getChangeLog()).thenReturn(changeLog);

    when(pluginMentionService.checkMentionUserInDescription(
            eq(mockedIssueEvent.getIssue()), eq(recipient), eq(true)))
        .thenReturn(true);

    when(i18nResolver.getText(
            eq("ru.mail.jira.plugins.myteam.notification.updated.and.mentioned"),
            nullable(String.class),
            eq("[TEST\\-1](null/browse/TEST\\-1)")))
        .thenReturn("user обновил(а) и упомянул(а) вас в запросе");
    String expected =
        "user обновил(а) и упомянул(а) вас в запросе\n"
            + "Summary\n"
            + "\n"
            + "text\n"
            + "\n"
            + "@\\[kucher\\@mail\\.ru\\]";
    // WHEN
    String result =
        jiraEventToChatMessageConverter.formatEventWithDiff(recipient, mockedIssueEvent);

    // THEN
    assertEquals(expected, result);
  }

  @Test
  void checkThatDescriptionHasNotMention() throws GenericEntityException {
    // GIVEN
    String prevDescriptionFieldMarkup = "text\n\ntext";
    String newDescriptionFieldMarkup = "text\n\n";
    GenericValue changeLog = mock(GenericValue.class);
    List<GenericValue> changeLogRelated = new ArrayList<>();
    GenericValue descriptionField = Mockito.mock(GenericValue.class);
    when(descriptionField.getString("field")).thenReturn("description");
    when(descriptionField.getString("newstring")).thenReturn(newDescriptionFieldMarkup);
    when(descriptionField.getString("oldstring")).thenReturn(prevDescriptionFieldMarkup);
    changeLogRelated.add(descriptionField);
    when(changeLog.getRelated("ChildChangeItem")).thenReturn(changeLogRelated);
    when(this.mockedIssueEvent.getChangeLog()).thenReturn(changeLog);
    when(pluginMentionService.checkMentionUserInDescription(
            eq(mockedIssueEvent.getIssue()), eq(recipient), eq(true)))
        .thenReturn(false);

    String expected =
        "null\n"
            + "Summary\n"
            + "\n"
            + "text\n"
            + "\n"
            + " ~text~ \n"
            + "\n"
            + ">___help description message___";

    // WHEN
    String result =
        jiraEventToChatMessageConverter.formatEventWithDiff(recipient, mockedIssueEvent);

    // THEN
    assertEquals(expected, result);
  }
}
