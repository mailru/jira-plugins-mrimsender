/* (C)2021 */
package ru.mail.jira.plugins.myteam.myteam;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.config.LocaleManager;
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
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.GenericValue;
import ru.mail.jira.plugins.myteam.protocol.MessageFormatter;

public class MessageFormatterTest {

  private MessageFormatter messageFormatter;
  private IssueEvent mockedIssueEvent;
  private ApplicationUser recipient;

  @Before
  public void init() {
    ApplicationProperties applicationProperties = Mockito.mock(ApplicationProperties.class);
    ConstantsManager constantsManager = Mockito.mock(ConstantsManager.class);
    DateTimeFormatter dateTimeFormatter = Mockito.mock(DateTimeFormatter.class);
    FieldManager fieldManager = Mockito.mock(FieldManager.class);
    IssueSecurityLevelManager issueSecurityLevelManager =
        Mockito.mock(IssueSecurityLevelManager.class);
    I18nHelper i18nHelper = Mockito.mock(I18nHelper.class);
    IssueTypeScreenSchemeManager issueTypeScreenSchemeManager =
        Mockito.mock(IssueTypeScreenSchemeManager.class);
    FieldScreenManager fieldScreenManager = Mockito.mock(FieldScreenManager.class);
    I18nResolver i18nResolver = Mockito.mock(I18nResolver.class);
    LocaleManager localeManager = Mockito.mock(LocaleManager.class);
    //    ProjectManager projectManager = Mockito.mock(ProjectManager.class);
    //    IssueTypeManager issueTypeManager = Mockito.mock(IssueTypeManager.class);
    //    ProjectComponentManager projectComponentManager =
    // Mockito.mock(ProjectComponentManager.class);
    //    VersionManager versionManager = Mockito.mock(VersionManager.class);
    UserManager userManager = Mockito.mock(UserManager.class);
    AttachmentManager attachmentManager = Mockito.mock(AttachmentManager.class);
    this.messageFormatter =
        new MessageFormatter(
            applicationProperties,
            constantsManager,
            dateTimeFormatter,
            fieldManager,
            issueSecurityLevelManager,
            i18nHelper,
            issueTypeScreenSchemeManager,
            fieldScreenManager,
            i18nResolver,
            localeManager,
            userManager,
            attachmentManager);
    IssueEvent event = Mockito.mock(IssueEvent.class);
    Issue mockedIssue = Mockito.mock(Issue.class);
    ApplicationUser assigneedUser = Mockito.mock(ApplicationUser.class);
    when(assigneedUser.getEmailAddress()).thenReturn("kucher@mail.ru");
    when(assigneedUser.getDisplayName()).thenReturn("Павел Кучер");
    when(mockedIssue.getKey()).thenReturn("TEST-1");
    when(mockedIssue.getAssignee()).thenReturn(assigneedUser);
    when(mockedIssue.getSummary()).thenReturn("Summary");
    when(event.getIssue()).thenReturn(mockedIssue);
    when(event.getEventTypeId()).thenReturn(2L);
    this.mockedIssueEvent = event;
    this.recipient = assigneedUser;
  }

  @Test
  public void testBoldAndNumberedList() throws GenericEntityException {
    GenericValue changeLog = mock(GenericValue.class);
    List<GenericValue> changeLogRelated = new ArrayList<>();
    GenericValue descriptionField = Mockito.mock(GenericValue.class);
    when(descriptionField.getString("field")).thenReturn("description");
    when(descriptionField.getString("newstring"))
        .thenReturn(
            "* *Lorem* ipsum dolor sit amet, consectetur adipiscing elit, sed* do eiusmod tempor incididunt ut *labore* et dolore magna aliqua. Ut enim ad minim veniam, *quis* nostrud exercitation ullamco laboris *nisi ut aliquip* ex ea commodo * consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. *Excepteur sint occaecat cupidatat* non proident, sunt in culpa qui officia *deserunt mollit* anim id est *laborum*.");
    changeLogRelated.add(descriptionField);
    when(changeLog.getRelated("ChildChangeItem")).thenReturn(changeLogRelated);
    when(this.mockedIssueEvent.getChangeLog()).thenReturn(changeLog);
    String testedHeader = "null\nSummary\n\n";
    String testedContent =
        "- *Lorem* ipsum dolor sit amet, consectetur adipiscing elit, sed\\* do eiusmod tempor incididunt ut *labore* et dolore magna aliqua. Ut enim ad minim veniam, *quis* nostrud exercitation ullamco laboris *nisi ut aliquip* ex ea commodo \\* consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. *Excepteur sint occaecat cupidatat* non proident, sunt in culpa qui officia *deserunt mollit* anim id est *laborum*.";
    assertEquals(
        testedHeader + testedContent,
        this.messageFormatter.formatEvent(recipient, this.mockedIssueEvent));
  }

  @Test
  public void testStrikehrought() throws GenericEntityException {
    GenericValue changeLog = mock(GenericValue.class);
    List<GenericValue> changeLogRelated = new ArrayList<>();
    GenericValue descriptionField = Mockito.mock(GenericValue.class);
    when(descriptionField.getString("field")).thenReturn("description");
    when(descriptionField.getString("newstring"))
        .thenReturn(
            "* -Lorem- ipsum dolor sit amet, -consectetur adipiscing- elit, sed* do eiusmod tempor incididunt ut *labore* -et dolore magna aliqua-. Ut enim ad minim veniam, *quis* nostrud exercitation ullamco labo-ris *nisi ut aliquip* ex ea commodo * consequat. Duis aute iru-re dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. *Excepteur sint occaecat cupidatat* non proident, -sunt in culpa qui officia- *deserunt mollit* anim id est -laborum-.");
    changeLogRelated.add(descriptionField);
    when(changeLog.getRelated("ChildChangeItem")).thenReturn(changeLogRelated);
    when(this.mockedIssueEvent.getChangeLog()).thenReturn(changeLog);
    String testedHeader = "null\nSummary\n\n";
    String testedContent =
        "- ~Lorem~ ipsum dolor sit amet, ~consectetur adipiscing~ elit, sed\\* do eiusmod tempor incididunt ut *labore* ~et dolore magna aliqua~. Ut enim ad minim veniam, *quis* nostrud exercitation ullamco labo\\-ris *nisi ut aliquip* ex ea commodo \\* consequat. Duis aute iru\\-re dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. *Excepteur sint occaecat cupidatat* non proident, ~sunt in culpa qui officia~ *deserunt mollit* anim id est ~laborum~.";
    assertEquals(
        testedHeader + testedContent,
        this.messageFormatter.formatEvent(recipient, this.mockedIssueEvent));
  }

  @Test
  public void testUnderLine() throws GenericEntityException {
    GenericValue changeLog = mock(GenericValue.class);
    List<GenericValue> changeLogRelated = new ArrayList<>();
    GenericValue descriptionField = Mockito.mock(GenericValue.class);
    when(descriptionField.getString("field")).thenReturn("description");
    when(descriptionField.getString("newstring"))
        .thenReturn(
            "* -Lorem- ipsum dolor sit amet, -consectetur adipiscing- elit, sed* do eiusmod tempor incididunt ut *labore* -et dolore magna aliqua-. Ut enim ad minim veniam, *quis* +nostrud exercitation ullamco+ labo-ris *nisi ut aliquip* ex ea commodo * +consequat. Duis+ aute iru-re dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. *Excepteur sint occaecat cupidatat* non proident, -sunt in culpa qui officia- *deserunt mollit* anim id est -laborum-.\n"
                + "\n"
                + "\n"
                + "+asdf asdf asdf\n"
                + "asdf+\n"
                + "\n"
                + "+asdadda+ asd asd. asd\n"
                + "asdasd as. a sd +aasdad+");
    changeLogRelated.add(descriptionField);
    when(changeLog.getRelated("ChildChangeItem")).thenReturn(changeLogRelated);
    when(this.mockedIssueEvent.getChangeLog()).thenReturn(changeLog);
    String testedHeader = "null\nSummary\n\n";
    String testedContent =
        "- ~Lorem~ ipsum dolor sit amet, ~consectetur adipiscing~ elit, sed\\* do eiusmod tempor incididunt ut *labore* ~et dolore magna aliqua~. Ut enim ad minim veniam, *quis* __nostrud exercitation ullamco__ labo\\-ris *nisi ut aliquip* ex ea commodo \\* __consequat. Duis__ aute iru\\-re dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. *Excepteur sint occaecat cupidatat* non proident, ~sunt in culpa qui officia~ *deserunt mollit* anim id est ~laborum~.\n"
            + "\n"
            + "\n"
            + "\\+asdf asdf asdf\n"
            + "asdf\\+\n"
            + "\n"
            + "__asdadda__ asd asd. asd\n"
            + "asdasd as. a sd __aasdad__";
    assertEquals(
        testedHeader + testedContent,
        this.messageFormatter.formatEvent(recipient, this.mockedIssueEvent));
  }

  @Test
  public void testLink() throws GenericEntityException {
    GenericValue changeLog = mock(GenericValue.class);
    List<GenericValue> changeLogRelated = new ArrayList<>();
    GenericValue descriptionField = Mockito.mock(GenericValue.class);
    when(descriptionField.getString("field")).thenReturn("description");
    when(descriptionField.getString("newstring"))
        .thenReturn(
            "* -Lorem- ipsum dolor sit amet, -consectetur adipiscing- elit, sed* do eiusmod tempor incididunt ut *labore* -et [dolore|http://example.com] magna aliqua-. Ut enim ad minim veniam, *quis* +nostrud exercitation ullamco+ labo-ris *nisi ut aliquip* ex ea commodo * +consequat. Duis+ aute iru-re dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. *Excepteur sint occaecat cupidatat* non proident, -sunt in culpa qui officia- *deserunt mollit* anim id est -laborum-.\n"
                + "\n"
                + "\n"
                + "[asdf asdf asdf\n"
                + "asdf|http://example.com]"
                + "\n"
                + "+asdadda+ asd asd. asd\n"
                + "asdasd as. a sd +aasdad+");
    changeLogRelated.add(descriptionField);
    when(changeLog.getRelated("ChildChangeItem")).thenReturn(changeLogRelated);
    when(this.mockedIssueEvent.getChangeLog()).thenReturn(changeLog);
    String testedHeader = "null\nSummary\n\n";
    String testedContent =
        "- ~Lorem~ ipsum dolor sit amet, ~consectetur adipiscing~ elit, sed\\* do eiusmod tempor incididunt ut *labore* ~et [dolore](http://example.com) magna aliqua~. Ut enim ad minim veniam, *quis* __nostrud exercitation ullamco__ labo\\-ris *nisi ut aliquip* ex ea commodo \\* __consequat. Duis__ aute iru\\-re dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. *Excepteur sint occaecat cupidatat* non proident, ~sunt in culpa qui officia~ *deserunt mollit* anim id est ~laborum~.\n"
            + "\n"
            + "\n"
            + "\\[asdf asdf asdf\n"
            + "asdf\\|http://example.com\\]"
            + "\n"
            + "__asdadda__ asd asd. asd\n"
            + "asdasd as. a sd __aasdad__";
    assertEquals(
        testedHeader + testedContent,
        this.messageFormatter.formatEvent(recipient, this.mockedIssueEvent));
  }

  @Test
  public void testItalic() throws GenericEntityException {
    GenericValue changeLog = mock(GenericValue.class);
    List<GenericValue> changeLogRelated = new ArrayList<>();
    GenericValue descriptionField = Mockito.mock(GenericValue.class);
    when(descriptionField.getString("field")).thenReturn("description");
    when(descriptionField.getString("newstring"))
        .thenReturn(
            "_Lorem_ ipsum dolor sit amet, -consectetur adipiscing- elit, sed* do eiusmod _tempor incididunt_ ut *labore* -et [dolore|http://example.com] magna aliqua-. Ut enim ad minim veniam, *quis* +nostrud exercitation ullamco+ labo-ris *nisi ut aliquip* ex ea commodo * +consequat. Duis+ aute iru-re dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. *Excepteur sint occaecat cupidatat* non proident, -sunt in culpa qui officia- *deserunt mollit* anim id est -laborum-.\n"
                + "\n"
                + "\n"
                + "_asdf asdf asdf\n"
                + "asdf asd_"
                + "\n"
                + "+asdadda+ asd asd. asd\n"
                + "asdasd as. a sd +aasdad+");
    changeLogRelated.add(descriptionField);
    when(changeLog.getRelated("ChildChangeItem")).thenReturn(changeLogRelated);
    when(this.mockedIssueEvent.getChangeLog()).thenReturn(changeLog);
    String testedHeader = "null\nSummary\n\n";
    String testedContent =
        "_Lorem_ ipsum dolor sit amet, ~consectetur adipiscing~ elit, sed\\* do eiusmod _tempor incididunt_ ut *labore* ~et [dolore](http://example.com) magna aliqua~. Ut enim ad minim veniam, *quis* __nostrud exercitation ullamco__ labo\\-ris *nisi ut aliquip* ex ea commodo \\* __consequat. Duis__ aute iru\\-re dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. *Excepteur sint occaecat cupidatat* non proident, ~sunt in culpa qui officia~ *deserunt mollit* anim id est ~laborum~.\n"
            + "\n"
            + "\n"
            + "\\_asdf asdf asdf\n"
            + "asdf asd\\_"
            + "\n"
            + "__asdadda__ asd asd. asd\n"
            + "asdasd as. a sd __aasdad__";
    assertEquals(
        testedHeader + testedContent,
        this.messageFormatter.formatEvent(recipient, this.mockedIssueEvent));
  }

  @Test
  public void testsingleLineCode() throws GenericEntityException {
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
    assertEquals(
        testedHeader + testedContent,
        this.messageFormatter.formatEvent(recipient, this.mockedIssueEvent));
  }

  @Test
  public void testMultiLineCode() throws GenericEntityException {
    GenericValue changeLog = mock(GenericValue.class);
    List<GenericValue> changeLogRelated = new ArrayList<>();
    GenericValue descriptionField = Mockito.mock(GenericValue.class);
    when(descriptionField.getString("field")).thenReturn("description");
    when(descriptionField.getString("newstring"))
        .thenReturn(
            "_Lorem_ ipsum dolor sit amet, -consectetur adipiscing- elit, sed* do eiusmod tempor incididunt ut *labore* -et [dolore|http://example.com] magna aliqua-. Ut enim ad minim veniam, *quis* +nostrud exercitation ullamco+ {{labo-ris}} *nisi ut aliquip* ex ea commodo * +consequat. Duis+ aute iru-re dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. *Excepteur sint occaecat cupidatat* non proident, -sunt in culpa qui officia- *deserunt mollit* anim id est -laborum-.\n"
                + "\n"
                + "\n"
                + "{code:java}asdf asdf asdf\n"
                + "asdf asd{code}"
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
            + "```java asdf asdf asdf\n"
            + "asdf asd```"
            + "\n"
            + "__asdadda__ asd asd. asd\n"
            + "asdasd as. a sd __aasdad__";
    assertEquals(
        testedHeader + testedContent,
        this.messageFormatter.formatEvent(recipient, this.mockedIssueEvent));
  }

  @Test
  public void italicTestInLink() throws GenericEntityException {
    GenericValue changeLog = mock(GenericValue.class);
    List<GenericValue> changeLogRelated = new ArrayList<>();
    GenericValue descriptionField = Mockito.mock(GenericValue.class);
    when(descriptionField.getString("field")).thenReturn("description");
    when(descriptionField.getString("newstring"))
        .thenReturn(
            "Jenkins Job: https://jenkins-cpp.rbdev.mail.ru/job/target-cpp-mr-handler/24183/\n"
                + "    RPM: bannerd-3.0.53-1_ga9a32ff643.x86_64.rpm\n"
                + "    Merge Request URL: https://gitlab.corp.mail.ru/target-cpp/target/merge_requests/5433");
    changeLogRelated.add(descriptionField);
    when(changeLog.getRelated("ChildChangeItem")).thenReturn(changeLogRelated);
    when(this.mockedIssueEvent.getChangeLog()).thenReturn(changeLog);
    String testedHeader = "null\nSummary\n\n";
    String testedContent =
        "Jenkins Job: https://jenkins\\-cpp.rbdev.mail.ru/job/target\\-cpp\\-mr\\-handler/24183/\n"
            + "    RPM: bannerd\\-3.0.53\\-1\\_ga9a32ff643.x86\\_64.rpm\n"
            + "    Merge Request URL: https://gitlab.corp.mail.ru/target\\-cpp/target/merge\\_requests/5433";
    assertEquals(
        testedHeader + testedContent,
        this.messageFormatter.formatEvent(recipient, this.mockedIssueEvent));
  }

  @Test
  public void JD1784TaskTestTrue() throws GenericEntityException {
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
    assertEquals(
        testedHeader + testedContent,
        this.messageFormatter.formatEvent(recipient, this.mockedIssueEvent));
  }

  @Test
  public void JD1784TaskTestFalse() throws GenericEntityException {
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
    assertEquals(
        testedHeader + testedContent,
        this.messageFormatter.formatEvent(recipient, this.mockedIssueEvent));
  }
}
