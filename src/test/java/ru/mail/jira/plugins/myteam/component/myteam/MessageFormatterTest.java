/* (C)2021 */
package ru.mail.jira.plugins.myteam.component.myteam;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import com.atlassian.jira.config.LocaleManager;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.event.issue.IssueEvent;
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
import ru.mail.jira.plugins.myteam.component.JiraMarkdownToChatMarkdownConverter;
import ru.mail.jira.plugins.myteam.component.MessageFormatter;
import ru.mail.jira.plugins.myteam.component.url.dto.Link;
import ru.mail.jira.plugins.myteam.component.url.dto.LinksInMessage;
import ru.mail.jira.plugins.myteam.service.PluginData;

@SuppressWarnings({"MockNotUsedInProduction", "UnusedVariable"})
public class MessageFormatterTest {

  private MessageFormatter messageFormatter;
  private IssueEvent mockedIssueEvent;
  private ApplicationUser recipient;

  @Before
  public void init() {
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
    LocaleManager localeManager = Mockito.mock(LocaleManager.class);
    //    ProjectManager projectManager = Mockito.mock(ProjectManager.class);
    //    IssueTypeManager issueTypeManager = Mockito.mock(IssueTypeManager.class);
    //    ProjectComponentManager projectComponentManager =
    // Mockito.mock(ProjectComponentManager.class);
    //    VersionManager versionManager = Mockito.mock(VersionManager.class);
    UserManager userManager = Mockito.mock(UserManager.class);
    ApplicationUser mentionUser = Mockito.mock(ApplicationUser.class);
    when(mentionUser.getEmailAddress()).thenReturn("i.pupkin@domain");
    when(mentionUser.getDisplayName()).thenReturn("Pupkin");
    when(userManager.getUserByName("i.pupkin@domain")).thenReturn(mentionUser);
    PluginData pluginData = Mockito.mock(PluginData.class);
    this.messageFormatter =
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
            new JiraMarkdownToChatMarkdownConverter(userManager, i18nHelper, pluginData));
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
  public void formatLinksSuccessWithUrls() {
    // GIVEN
    String message = "mask(url) http://localost:2990";
    List<Link> links = new ArrayList<>();
    links.add(Link.of("https://some.domen", "mask(url)", true));
    links.add(Link.of("http://localost:2990", "http://localost:2990", false));
    // WHEN
    String result = messageFormatter.formatLinks(message, LinksInMessage.of(links));
    // THEN
    assertEquals("[mask(url)|https://some.domen] http://localost:2990", result);
  }

  @Test
  public void formatLinksSuccessWithUrlsWhenMaskHaveSpecialSymbolsForRegex() {
    // GIVEN
    String message = "mask(!&&!url!&!).-[] http://localost:2990";
    List<Link> links = new ArrayList<>();
    links.add(Link.of("https://some.domen", "mask(!&&!url!&!).-[]", true));
    links.add(Link.of("http://localost:2990", "http://localost:2990", false));
    // WHEN
    String result = messageFormatter.formatLinks(message, LinksInMessage.of(links));
    // THEN
    assertEquals("[mask(!&&!url!&!).-[]|https://some.domen] http://localost:2990", result);
  }

  @Test
  public void formatLinksSuccessWithoutUrls() {
    // GIVEN
    String message = "sometext";
    // WHEN
    String result = messageFormatter.formatLinks(message, LinksInMessage.of(emptyList()));
    // THEN
    assertEquals(message, result);
  }
}
