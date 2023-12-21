/* (C)2023 */
package ru.mail.jira.plugins.myteam.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.atlassian.jira.exception.IssueNotFoundException;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.ApplicationUser;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.mail.jira.plugins.myteam.bot.listeners.MyteamEventsListener;
import ru.mail.jira.plugins.myteam.component.UserData;
import ru.mail.jira.plugins.myteam.service.IssueService;
import ru.mail.jira.plugins.myteam.service.UserChatService;
import ru.mail.jira.plugins.myteam.service.model.MyteamChatMetaDto;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"MockNotUsedInProduction", "UnusedVariable"})
class MyteamServiceImplTest {

  @Mock
  @SuppressWarnings("NullAway")
  private GroupManager groupManager;

  @Mock
  @SuppressWarnings("NullAway")
  private UserData userData;

  @Mock
  @SuppressWarnings("NullAway")
  private MyteamEventsListener myteamEventsListener;

  @Mock
  @SuppressWarnings("NullAway")
  private UserChatService userChatService;

  @Mock
  @SuppressWarnings("NullAway")
  private IssueService issueService;

  @InjectMocks
  @SuppressWarnings("NullAway")
  private MyteamServiceImpl myteamService;

  @ParameterizedTest
  @EmptySource
  void findChatByIssueKeyWhenIssueKeyNull(String issueKey) {
    // WHEN // THEN
    assertThrows(IllegalArgumentException.class, () -> myteamService.findChatByIssueKey(issueKey));
  }

  @Test
  void findChatByIssueKeyWhenChatNotFound() {
    // GIVEN
    String issueKey = "someIssueKey";

    when(userChatService.findChatByIssueKey(eq(issueKey))).thenReturn(null);

    // WHEN
    MyteamChatMetaDto chatByIssueKey = myteamService.findChatByIssueKey(issueKey);

    // THEN
    assertNull(chatByIssueKey);
    verify(userChatService).findChatByIssueKey(eq(issueKey));
  }

  @Test
  void findChatByIssueKeyWhenChatFound() {
    // GIVEN
    String issueKey = "someIssueKey";

    MyteamChatMetaDto myteamChatMetaDto = MyteamChatMetaDto.of(1, "someChatId", "someIssueKey");
    when(userChatService.findChatByIssueKey(eq(issueKey))).thenReturn(myteamChatMetaDto);

    // WHEN
    MyteamChatMetaDto chatByIssueKey = myteamService.findChatByIssueKey(issueKey);

    // THEN
    assertNotNull(chatByIssueKey);
    verify(userChatService).findChatByIssueKey(eq(issueKey));
  }

  @ParameterizedTest
  @EmptySource
  void createChatWhenIssueKeyIsNull(String issuekey) {
    // WHEN // THEN
    assertThrows(
        IllegalArgumentException.class,
        () ->
            myteamService.createChat(
                List.of(mock(ApplicationUser.class)), "chatName", null, true, issuekey));
  }

  @Test
  void createChatWhenIssueNotFoundByKey() {
    // GIVEN
    String issueKey = "someIssueKey";
    when(issueService.getIssue(eq(issueKey))).thenReturn(null);

    // WHEN // THEN
    assertThrows(
        IssueNotFoundException.class,
        () ->
            myteamService.createChat(
                List.of(mock(ApplicationUser.class)), "chatName", null, true, issueKey));
  }

  @Test
  void createChatWhenIssueAlreadyLinkedToChat() {
    // GIVEN
    String issueKey = "someIssueKey";
    when(issueService.getIssue(eq(issueKey))).thenReturn(mock(Issue.class));

    when(userChatService.findChatByIssueKey(eq(issueKey)))
        .thenReturn(MyteamChatMetaDto.of(1, "someChatId", issueKey));

    // WHEN // THEN
    assertThrows(
        IllegalArgumentException.class,
        () ->
            myteamService.createChat(
                List.of(mock(ApplicationUser.class)), "chatName", null, true, issueKey));
  }

  @ParameterizedTest
  @EmptySource
  void createChatWhenListOfMemberEmpty(List<ApplicationUser> members) {
    // GIVEN
    String issueKey = "someIssueKey";
    when(issueService.getIssue(eq(issueKey))).thenReturn(mock(Issue.class));
    when(userChatService.findChatByIssueKey(eq(issueKey))).thenReturn(null);

    // WHEN // THEN
    assertThrows(
        IllegalArgumentException.class,
        () -> myteamService.createChat(members, "chatName", null, true, issueKey));
  }

  @ParameterizedTest
  @EmptySource
  void createChatWhenChatNameEmpty(String chatName) {
    // GIVEN
    String issueKey = "someIssueKey";
    when(issueService.getIssue(eq(issueKey))).thenReturn(mock(Issue.class));
    when(userChatService.findChatByIssueKey(eq(issueKey))).thenReturn(null);

    // WHEN // THEN
    assertThrows(
        IllegalArgumentException.class,
        () ->
            myteamService.createChat(
                List.of(mock(ApplicationUser.class)), chatName, null, true, issueKey));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void createChatSuccess(boolean publicChat) {
    // GIVEN
    String issueKey = "someIssueKey";
    String chatName = "someChatName";

    ApplicationUser chatMember = mock(ApplicationUser.class);
    when(chatMember.isActive()).thenReturn(true);
    when(chatMember.getEmailAddress()).thenReturn("some");

    when(issueService.getIssue(eq(issueKey))).thenReturn(mock(Issue.class));
    when(userChatService.findChatByIssueKey(eq(issueKey))).thenReturn(null);
    String createdChatId = "someChatId";
    when(userChatService.createChat(
            eq(chatName), nullable(String.class), anyList(), eq(publicChat)))
        .thenReturn(createdChatId);
    MyteamChatMetaDto expected = MyteamChatMetaDto.of(1, createdChatId, issueKey);
    when(userChatService.unsafeLinkChat(eq(createdChatId), eq(issueKey))).thenReturn(expected);
    when(userData.isCreateChatsWithUserAllowed(eq(chatMember))).thenReturn(true);

    // WHEN
    MyteamChatMetaDto chatDto =
        myteamService.createChat(List.of(chatMember), chatName, null, publicChat, issueKey);

    // THEN
    assertNotNull(chatDto);
    assertEquals(expected, chatDto);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void createChatWhenErrorHappenedDuringCreatingChatInVkTeams(boolean publicChat) {
    // GIVEN
    String issueKey = "someIssueKey";
    String chatName = "someChatName";
    when(issueService.getIssue(eq(issueKey))).thenReturn(mock(Issue.class));
    when(userChatService.findChatByIssueKey(eq(issueKey))).thenReturn(null);
    when(userChatService.createChat(
            eq(chatName), nullable(String.class), anyList(), eq(publicChat)))
        .thenThrow(RuntimeException.class);
    ApplicationUser chatMember = mock(ApplicationUser.class);
    when(chatMember.isActive()).thenReturn(true);
    when(chatMember.getEmailAddress()).thenReturn("some");
    when(userData.isCreateChatsWithUserAllowed(eq(chatMember))).thenReturn(true);

    // WHEN
    MyteamChatMetaDto chatDto =
        myteamService.createChat(List.of(chatMember), chatName, null, publicChat, issueKey);

    // THEN
    assertNull(chatDto);
    verify(userChatService, never()).unsafeLinkChat(anyString(), anyString());
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void createChatWhenErrorHappenedDuringPersistChatIdInDatabase(boolean publicChat) {
    // GIVEN
    String issueKey = "someIssueKey";
    String chatName = "someChatName";

    ApplicationUser chatMember = mock(ApplicationUser.class);
    when(chatMember.isActive()).thenReturn(true);
    when(chatMember.getEmailAddress()).thenReturn("some");

    when(issueService.getIssue(eq(issueKey))).thenReturn(mock(Issue.class));
    when(userChatService.findChatByIssueKey(eq(issueKey))).thenReturn(null);
    String createdChatId = "someChatId";
    when(userChatService.createChat(
            eq(chatName), nullable(String.class), anyList(), eq(publicChat)))
        .thenReturn(createdChatId);
    when(userChatService.unsafeLinkChat(eq(createdChatId), eq(issueKey)))
        .thenThrow(RuntimeException.class);
    when(userData.isCreateChatsWithUserAllowed(eq(chatMember))).thenReturn(true);

    // WHEN
    MyteamChatMetaDto chatDto =
        myteamService.createChat(List.of(chatMember), chatName, null, publicChat, issueKey);

    // THEN
    assertNull(chatDto);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void createChatSuccessWhenListHasOnlyOneActiveUserAndAllowedCreateChatWithThisUser(
      boolean publicChat) {
    // GIVEN
    String issueKey = "someIssueKey";
    String chatName = "someChatName";

    ApplicationUser nullableApplicationUser = null;

    ApplicationUser notActiveUser = mock(ApplicationUser.class);
    when(notActiveUser.isActive()).thenReturn(false);

    ApplicationUser allowedCreateChatWithActiveUser = mock(ApplicationUser.class);
    when(allowedCreateChatWithActiveUser.isActive()).thenReturn(true);
    when(allowedCreateChatWithActiveUser.getEmailAddress()).thenReturn("activeUserEmail");
    when(userData.isCreateChatsWithUserAllowed(eq(allowedCreateChatWithActiveUser)))
        .thenReturn(true);

    ApplicationUser notAllowedCreateChatWithActiveUser = mock(ApplicationUser.class);
    when(notAllowedCreateChatWithActiveUser.isActive()).thenReturn(true);
    when(userData.isCreateChatsWithUserAllowed(notAllowedCreateChatWithActiveUser))
        .thenReturn(false);

    List<@Nullable ApplicationUser> members = new ArrayList<>();
    members.add(nullableApplicationUser);
    members.add(notActiveUser);
    members.add(allowedCreateChatWithActiveUser);
    members.add(notAllowedCreateChatWithActiveUser);

    when(issueService.getIssue(eq(issueKey))).thenReturn(mock(Issue.class));
    when(userChatService.findChatByIssueKey(eq(issueKey))).thenReturn(null);
    String createdChatId = "someChatId";
    when(userChatService.createChat(
            eq(chatName), nullable(String.class), anyList(), eq(publicChat)))
        .thenReturn(createdChatId);
    MyteamChatMetaDto expected = MyteamChatMetaDto.of(1, createdChatId, issueKey);
    when(userChatService.unsafeLinkChat(eq(createdChatId), eq(issueKey))).thenReturn(expected);

    // WHEN
    MyteamChatMetaDto chatDto =
        myteamService.createChat(members, chatName, null, publicChat, issueKey);

    // THEN
    assertNotNull(chatDto);
    assertEquals(expected, chatDto);
    verify(userChatService)
        .createChat(
            eq(chatName),
            nullable(String.class),
            argThat(
                argument ->
                    argument != null
                        && argument.size() == 1
                        && argument.get(0) != null
                        && "activeUserEmail".equals(argument.get(0).getSn())),
            eq(publicChat));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void createChatWhenListChatMembersHasOnlyNullableAndNotActiveAndNotAllowedCreateChatWithUsers(
      boolean publicChat) {
    // GIVEN
    String issueKey = "someIssueKey";
    String chatName = "someChatName";

    ApplicationUser nullableApplicationUser = null;

    ApplicationUser notActiveUser = mock(ApplicationUser.class);
    when(notActiveUser.isActive()).thenReturn(false);

    ApplicationUser notAllowedCreateChatWithActiveUser = mock(ApplicationUser.class);
    when(notAllowedCreateChatWithActiveUser.isActive()).thenReturn(true);
    when(userData.isCreateChatsWithUserAllowed(eq(notAllowedCreateChatWithActiveUser)))
        .thenReturn(false);

    List<@Nullable ApplicationUser> members = new ArrayList<>();
    members.add(nullableApplicationUser);
    members.add(notActiveUser);
    members.add(notAllowedCreateChatWithActiveUser);

    when(issueService.getIssue(eq(issueKey))).thenReturn(mock(Issue.class));
    when(userChatService.findChatByIssueKey(eq(issueKey))).thenReturn(null);

    // WHEN // THEN
    assertThrows(
        IllegalArgumentException.class,
        () -> myteamService.createChat(members, chatName, null, publicChat, issueKey));
  }
}
