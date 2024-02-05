/* (C)2023 */
package ru.mail.jira.plugins.myteam.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.util.I18nHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.mail.jira.plugins.myteam.service.PluginData;

@SuppressWarnings({"MockNotUsedInProduction", "NullAway", "UnusedVariable"})
@ExtendWith(MockitoExtension.class)
class JiraMarkdownToChatMarkdownConverterTest {
  @Mock private UserManager userManager;
  @Mock private I18nHelper i18nHelper;
  @Mock private PluginData pluginData;

  @InjectMocks private JiraMarkdownToChatMarkdownConverter jiraMarkdownToChatMarkdownConverter;

  @Test
  void whenInputStringIsNull() {
    // GIVEN
    String inputString = null;

    // WHEN
    String result =
        jiraMarkdownToChatMarkdownConverter.makeMyteamMarkdownFromJira(inputString, true);

    // THEN
    assertNull(result);
  }

  @Test
  void whenInputStringIsEmpty() {
    // GIVEN
    String inputString = "";

    // WHEN
    String result =
        jiraMarkdownToChatMarkdownConverter.makeMyteamMarkdownFromJira(inputString, true);

    // THEN
    assertEquals("", result);
  }

  @Test
  void whenInputStringIsBlank() {
    // GIVEN
    String inputString = "                     ";

    // WHEN
    String result =
        jiraMarkdownToChatMarkdownConverter.makeMyteamMarkdownFromJira(inputString, true);

    // THEN
    assertEquals(inputString, result);
  }

  @Test
  void whenInputStringNotHasJiraMarkdown() {
    // GIVEN
    String inputString = "SimpleText simpletext\nsimpletext\nmoresimpletext";

    // WHEN
    String result =
        jiraMarkdownToChatMarkdownConverter.makeMyteamMarkdownFromJira(inputString, true);

    // THEN
    assertEquals(inputString, result);
  }

  @Test
  void whenInputStringIsBlankWithNewLines() {
    // GIVEN
    String inputString = "           \n            \n          ";

    // WHEN
    String result =
        jiraMarkdownToChatMarkdownConverter.makeMyteamMarkdownFromJira(inputString, true);

    // THEN
    assertEquals(inputString, result);
  }

  @Test
  void testUnderLine() {
    // GIVEN
    String inputString =
        "* -Lorem- ipsum dolor sit amet, -consectetur adipiscing- elit, sed* do eiusmod tempor incididunt ut *labore* -et dolore magna aliqua-. Ut enim ad minim veniam, *quis* +nostrud exercitation ullamco+ labo-ris *nisi ut aliquip* ex ea commodo * +consequat. Duis+ aute iru-re dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. *Excepteur sint occaecat cupidatat* non proident, -sunt in culpa qui officia- *deserunt mollit* anim id est -laborum-.\n"
            + "\n"
            + "\n"
            + "+asdf asdf asdf\n"
            + "asdf+\n"
            + "\n"
            + "+asdadda+ asd asd. asd\n"
            + "asdasd as. a sd +aasdad+";
    String testedContent =
        "- ~Lorem~ ipsum dolor sit amet, ~consectetur adipiscing~ elit, sed* do eiusmod tempor incididunt ut *labore* ~et dolore magna aliqua~. Ut enim ad minim veniam, *quis* __nostrud exercitation ullamco__ labo\\-ris *nisi ut aliquip* ex ea commodo * __consequat. Duis__ aute iru\\-re dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. *Excepteur sint occaecat cupidatat* non proident, ~sunt in culpa qui officia~ *deserunt mollit* anim id est ~laborum~.\n"
            + "\n"
            + "\n"
            + "\\+asdf asdf asdf\n"
            + "asdf\\+\n"
            + "\n"
            + "__asdadda__ asd asd. asd\n"
            + "asdasd as. a sd __aasdad__";
    // WHEN
    String result =
        jiraMarkdownToChatMarkdownConverter.makeMyteamMarkdownFromJira(inputString, true);

    // THEN
    assertEquals(testedContent, result);
  }

  @Test
  void testLink() {
    // GIVEN
    String inputString =
        "* -Lorem- ipsum dolor sit amet, -consectetur adipiscing- elit, sed* do eiusmod tempor incididunt ut *labore* -et [dolore|http://example.com] magna aliqua-. Ut enim ad minim veniam, *quis* +nostrud exercitation ullamco+ labo-ris *nisi ut aliquip* ex ea commodo * +consequat. Duis+ aute iru-re dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. *Excepteur sint occaecat cupidatat* non proident, -sunt in culpa qui officia- *deserunt mollit* anim id est -laborum-.\n"
            + "\n"
            + "\n"
            + "[asdf asdf asdf\n"
            + "asdf|http://example.com]"
            + "\n"
            + "+asdadda+ asd asd. asd\n"
            + "asdasd as. a sd +aasdad+";

    String testedContent =
        "- ~Lorem~ ipsum dolor sit amet, ~consectetur adipiscing~ elit, sed* do eiusmod tempor incididunt ut *labore* ~et [dolore](http://example.com) magna aliqua~. Ut enim ad minim veniam, *quis* __nostrud exercitation ullamco__ labo\\-ris *nisi ut aliquip* ex ea commodo * __consequat. Duis__ aute iru\\-re dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. *Excepteur sint occaecat cupidatat* non proident, ~sunt in culpa qui officia~ *deserunt mollit* anim id est ~laborum~.\n"
            + "\n"
            + "\n"
            + "\\[asdf asdf asdf\n"
            + "asdf\\|http://example.com\\]"
            + "\n"
            + "__asdadda__ asd asd. asd\n"
            + "asdasd as. a sd __aasdad__";
    // WHEN
    String result =
        jiraMarkdownToChatMarkdownConverter.makeMyteamMarkdownFromJira(inputString, true);

    // THEN
    assertEquals(testedContent, result);
  }

  @Test
  void testMentionWithUsingMentionFormat() {
    // GIVEN
    String inputString =
        "Lorem ipsum dolor sit amet,\n" + "loerm leorm [~i.pupkin@domain] ipsum dolor sit amet\n";

    ApplicationUser mentionUser = mock(ApplicationUser.class);
    when(mentionUser.getEmailAddress()).thenReturn("i.pupkin@domain");
    when(userManager.getUserByName("i.pupkin@domain")).thenReturn(mentionUser);

    String testedContent =
        "Lorem ipsum dolor sit amet,\n"
            + "loerm leorm @\\[i\\.pupkin\\@domain\\] ipsum dolor sit amet\n";

    // WHEN
    String result =
        jiraMarkdownToChatMarkdownConverter.makeMyteamMarkdownFromJira(inputString, true);

    // THEN
    assertEquals(testedContent, result);
  }

  @Test
  void testMentionUserNotFound() {
    // GIVEN
    String inputString =
        "Lorem ipsum dolor sit amet,\n"
            + "loerm leorm [~i.notfounduser@domain] ipsum dolor sit amet\n";

    String testedContent =
        "Lorem ipsum dolor sit amet,\n" + "loerm leorm notfounduser ipsum dolor sit amet\n";

    when(i18nHelper.getText(anyString())).thenReturn("notfounduser");

    // WHEN
    String result =
        jiraMarkdownToChatMarkdownConverter.makeMyteamMarkdownFromJira(inputString, true);

    // THEN
    assertEquals(testedContent, result);
  }

  @Test
  void testMentionWithoutUsingMentionFormat() {
    // GIVEN
    String inputString =
        "Lorem ipsum dolor sit amet,\n" + "loerm leorm [~i.pupkin@domain] ipsum dolor sit amet\n";

    ApplicationUser mentionUser = mock(ApplicationUser.class);
    when(mentionUser.getEmailAddress()).thenReturn("i.pupkin@domain");
    when(mentionUser.getDisplayName()).thenReturn("Pupkin");
    when(userManager.getUserByName("i.pupkin@domain")).thenReturn(mentionUser);
    when(pluginData.getProfileLink()).thenReturn("someLink/");

    String testedContent =
        "Lorem ipsum dolor sit amet,\n"
            + "loerm leorm [Pupkin](someLink/i\\.pupkin\\@domain) ipsum dolor sit amet\n";

    // WHEN
    String result =
        jiraMarkdownToChatMarkdownConverter.makeMyteamMarkdownFromJira(inputString, false);

    // THEN
    assertEquals(testedContent, result);
  }

  @Test
  void testItalic() {
    // GIVEN
    String inputString =
        "_Lorem_ ipsum dolor sit amet, -consectetur adipiscing- elit, sed* do eiusmod _tempor incididunt_ ut *labore* -et [dolore|http://example.com] magna aliqua-. Ut enim ad minim veniam, *quis* +nostrud exercitation ullamco+ labo-ris *nisi ut aliquip* ex ea commodo * +consequat. Duis+ aute iru-re dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. *Excepteur sint occaecat cupidatat* non proident, -sunt in culpa qui officia- *deserunt mollit* anim id est -laborum-.\n"
            + "\n"
            + "\n"
            + "_asdf asdf asdf\n"
            + "asdf asd_"
            + "\n"
            + "+asdadda+ asd asd. asd\n"
            + "asdasd as. a sd +aasdad+";
    String testedContent =
        "_Lorem_ ipsum dolor sit amet, ~consectetur adipiscing~ elit, sed* do eiusmod _tempor incididunt_ ut *labore* ~et [dolore](http://example.com) magna aliqua~. Ut enim ad minim veniam, *quis* __nostrud exercitation ullamco__ labo\\-ris *nisi ut aliquip* ex ea commodo * __consequat. Duis__ aute iru\\-re dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. *Excepteur sint occaecat cupidatat* non proident, ~sunt in culpa qui officia~ *deserunt mollit* anim id est ~laborum~.\n"
            + "\n"
            + "\n"
            + "\\_asdf asdf asdf\n"
            + "asdf asd\\_"
            + "\n"
            + "__asdadda__ asd asd. asd\n"
            + "asdasd as. a sd __aasdad__";
    // WHEN
    String result =
        jiraMarkdownToChatMarkdownConverter.makeMyteamMarkdownFromJira(inputString, true);

    // THEN
    assertEquals(testedContent, result);
  }

  @Test
  void testsingleLineCode() {
    // GIVEN
    String inputString =
        "_Lorem_ ipsum dolor sit amet, -consectetur adipiscing- elit, sed* do eiusmod tempor incididunt ut *labore* -et [dolore|http://example.com] magna aliqua-. Ut enim ad minim veniam, *quis* +nostrud exercitation ullamco+ {{labo-ris}} *nisi ut aliquip* ex ea commodo * +consequat. Duis+ aute iru-re dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. *Excepteur sint occaecat cupidatat* non proident, -sunt in culpa qui officia- *deserunt mollit* anim id est -laborum-.\n"
            + "\n"
            + "\n"
            + "{{asdf asdf asdf\n"
            + "asdf asd}}"
            + "\n"
            + "+asdadda+ asd asd. asd\n"
            + "asdasd as. a sd +aasdad+";
    String testedContent =
        "_Lorem_ ipsum dolor sit amet, ~consectetur adipiscing~ elit, sed* do eiusmod tempor incididunt ut *labore* ~et [dolore](http://example.com) magna aliqua~. Ut enim ad minim veniam, *quis* __nostrud exercitation ullamco__ `labo\\-ris` *nisi ut aliquip* ex ea commodo * __consequat. Duis__ aute iru\\-re dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. *Excepteur sint occaecat cupidatat* non proident, ~sunt in culpa qui officia~ *deserunt mollit* anim id est ~laborum~.\n"
            + "\n"
            + "\n"
            + "\\{\\{asdf asdf asdf\n"
            + "asdf asd\\}\\}"
            + "\n"
            + "__asdadda__ asd asd. asd\n"
            + "asdasd as. a sd __aasdad__";

    // WHEN
    String result =
        jiraMarkdownToChatMarkdownConverter.makeMyteamMarkdownFromJira(inputString, true);

    // THEN
    assertEquals(testedContent, result);
  }

  @Test
  void testQuote() {
    // GIVEN
    String inputString =
        "_Lorem_ {quote}ipsum dolor{quote} sit amet, -consectetur adipiscing- elit, sed* do eiusmod tempor incididunt ut *labore* -et [dolore|http://example.com] magna aliqua-. Ut enim ad minim veniam, *quis* +nostrud exercitation ullamco+ {{labo-ris}} *nisi ut aliquip* ex ea commodo * +consequat. Duis+ aute iru-re dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. *Excepteur sint occaecat cupidatat* non proident, -sunt in culpa qui officia- *deserunt mollit* anim id est -laborum-.\n"
            + "\n"
            + "\n"
            + "{quote}asdf asdf asdf\n"
            + "wad dwadaw\n"
            + "asdf asd{quote}"
            + "\n"
            + "+asdadda+ asd asd. asd\n"
            + "asdasd as. a sd +aasdad+";
    String testedContent =
        "_Lorem_ \n>ipsum dolor\n sit amet, ~consectetur adipiscing~ elit, sed* do eiusmod tempor incididunt ut *labore* ~et [dolore](http://example.com) magna aliqua~. Ut enim ad minim veniam, *quis* __nostrud exercitation ullamco__ `labo\\-ris` *nisi ut aliquip* ex ea commodo * __consequat. Duis__ aute iru\\-re dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. *Excepteur sint occaecat cupidatat* non proident, ~sunt in culpa qui officia~ *deserunt mollit* anim id est ~laborum~.\n"
            + "\n"
            + "\n"
            + "\n>asdf asdf asdf\n>"
            + "wad dwadaw\n>"
            + "asdf asd\n"
            + "\n"
            + "__asdadda__ asd asd. asd\n"
            + "asdasd as. a sd __aasdad__";

    // WHEN
    String result =
        jiraMarkdownToChatMarkdownConverter.makeMyteamMarkdownFromJira(inputString, true);

    // THEN
    assertEquals(testedContent, result);
  }

  @Test
  void testMultiLineCode() {
    // GIVEN
    String inputString =
        "_Lorem_ ipsum {code}dolor{code} sit amet, -consectetur adipiscing- elit, sed* do eiusmod tempor incididunt ut *labore* -et [dolore|http://example.com] magna aliqua-. Ut enim ad minim veniam, *quis* +nostrud exercitation ullamco+ {{labo-ris}} *nisi ut aliquip* ex ea commodo * +consequat. Duis+ aute iru-re dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. *Excepteur sint occaecat cupidatat* non proident, -sunt in culpa qui officia- *deserunt mollit* anim id est -laborum-.\n"
            + "\n"
            + "\n"
            + "{code:java}asdf asdf asdf\n"
            + "asdf asd{code}"
            + "\n"
            + "+asdadda+ asd asd. asd\n"
            + "asdasd as. a sd +aasdad+";
    String testedContent =
        "_Lorem_ ipsum \n```dolor``` sit amet, ~consectetur adipiscing~ elit, sed* do eiusmod tempor incididunt ut *labore* ~et [dolore](http://example.com) magna aliqua~. Ut enim ad minim veniam, *quis* __nostrud exercitation ullamco__ `labo\\-ris` *nisi ut aliquip* ex ea commodo * __consequat. Duis__ aute iru\\-re dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. *Excepteur sint occaecat cupidatat* non proident, ~sunt in culpa qui officia~ *deserunt mollit* anim id est ~laborum~.\n"
            + "\n"
            + "\n"
            + "\n```java asdf asdf asdf\n"
            + "asdf asd```"
            + "\n"
            + "__asdadda__ asd asd. asd\n"
            + "asdasd as. a sd __aasdad__";

    // WHEN
    String result =
        jiraMarkdownToChatMarkdownConverter.makeMyteamMarkdownFromJira(inputString, true);

    // THEN
    assertEquals(testedContent, result);
  }

  @Test
  void italicTestInLink() {
    // GIVEN
    String inputString =
        "Jenkins Job: https://jenkins-cpp.rbdev.mail.ru/job/target-cpp-mr-handler/24183/\n"
            + "    RPM: bannerd-3.0.53-1_ga9a32ff643.x86_64.rpm\n"
            + "    Merge Request URL: https://gitlab.corp.mail.ru/target-cpp/target/merge_requests/5433";
    String testedContent =
        "Jenkins Job: https://jenkins\\-cpp.rbdev.mail.ru/job/target\\-cpp\\-mr\\-handler/24183/\n"
            + "    RPM: bannerd\\-3.0.53\\-1\\_ga9a32ff643.x86\\_64.rpm\n"
            + "    Merge Request URL: https://gitlab.corp.mail.ru/target\\-cpp/target/merge\\_requests/5433";

    // WHEN
    String result =
        jiraMarkdownToChatMarkdownConverter.makeMyteamMarkdownFromJira(inputString, true);

    // THEN
    assertEquals(testedContent, result);
  }

  @Test
  void testWhenLinkJiraMarkdownHasSpecialCharacterInMaskLink() {
    // GIVEN
    String inputString = "[url [NAME]|https://someurl.org]";
    String testedContent = "[url \\[NAME\\]](https://someurl.org)";

    // WHEN
    String result =
        jiraMarkdownToChatMarkdownConverter.makeMyteamMarkdownFromJira(inputString, true);

    // THEN
    assertEquals(testedContent, result);
  }


}
