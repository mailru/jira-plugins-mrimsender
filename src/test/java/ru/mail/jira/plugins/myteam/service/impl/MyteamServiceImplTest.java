package ru.mail.jira.plugins.myteam.service.impl;

import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.jira.avatar.AvatarService;
import com.atlassian.jira.bc.user.search.UserSearchService;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.exception.IssueNotFoundException;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.sal.api.message.I18nResolver;
import kong.unirest.HttpResponse;
import kong.unirest.UnirestParsingException;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.matchers.InstanceOf;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.mail.jira.plugins.myteam.bot.events.JiraIssueViewEvent;
import ru.mail.jira.plugins.myteam.bot.events.JiraNotifyEvent;
import ru.mail.jira.plugins.myteam.bot.listeners.MyteamEventsListener;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.exceptions.LinkIssueWithChatException;
import ru.mail.jira.plugins.myteam.commons.Utils;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.component.UserData;
import ru.mail.jira.plugins.myteam.db.model.MyteamChatMeta;
import ru.mail.jira.plugins.myteam.db.repository.MyteamChatRepository;
import ru.mail.jira.plugins.myteam.myteam.MyteamApiClient;
import ru.mail.jira.plugins.myteam.myteam.dto.chats.ChatInfoResponse;
import ru.mail.jira.plugins.myteam.myteam.dto.chats.CreateChatResponse;
import ru.mail.jira.plugins.myteam.myteam.dto.chats.GroupChatInfo;
import ru.mail.jira.plugins.myteam.service.PluginData;
import ru.mail.jira.plugins.myteam.service.model.MyteamChatMetaDto;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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
    private MyteamApiClient myteamApiClient;
    @Mock
    @SuppressWarnings("NullAway")
    private PluginData pluginData;
    @Mock
    @SuppressWarnings("NullAway")
    private I18nResolver i18nResolver;
    @Mock
    @SuppressWarnings("NullAway")
    private MyteamChatRepository myteamChatRepository;
    @Mock
    @SuppressWarnings("NullAway")
    private ApplicationProperties applicationProperties;
    @Mock
    @SuppressWarnings("NullAway")
    private UserManager userManager;
    @Mock
    @SuppressWarnings("NullAway")
    private UserSearchService userSearchService;
    @Mock
    @SuppressWarnings("NullAway")
    private AvatarService avatarService;
    @Mock
    @SuppressWarnings("NullAway")
    private IssueManager issueManager;

    @InjectMocks
    @SuppressWarnings("NullAway")
    private MyteamServiceImpl myteamService;


    @ParameterizedTest
    @EmptySource
    void sendMessageWhenMessageIsEmptyString(String message) {
        // GIVEN
        ApplicationUser applicationUser = mock(ApplicationUser.class);

        // WHEN // THEN
        assertThrows(IllegalArgumentException.class, () -> myteamService.sendMessage(applicationUser, message));
    }

    @Test
    void sendMessageWhenApplicationUserNull() {
        // GIVEN
        ApplicationUser applicationUser = null;
        String message = "some message";

        // WHEN // THEN
        assertThrows(IllegalArgumentException.class, () -> myteamService.sendMessage(applicationUser, message));
    }

    @Test
    void sendMessageWhenApplicationUserIsNotActive() {
        // GIVEN
        ApplicationUser applicationUser = mock(ApplicationUser.class);
        when(applicationUser.isActive()).thenReturn(false);
        String message = "some message";

        // WHEN
        boolean sent = myteamService.sendMessage(applicationUser, message);

        // THEN
        assertFalse(sent);
    }

    @ParameterizedTest
    @EmptySource
    void sendMessageWhenApplicationUserEmailEmptyString(String mrimLogin) {
        // GIVEN
        ApplicationUser applicationUser = mock(ApplicationUser.class);
        when(applicationUser.isActive()).thenReturn(true);
        when(applicationUser.getEmailAddress()).thenReturn(mrimLogin);
        String message = "some message";

        // WHEN
        boolean sent = myteamService.sendMessage(applicationUser, message);

        // THEN
        assertFalse(sent);
    }

    @Test
    void sendMessageWhenUserCannotReceiveMessage() {
        // GIVEN
        ApplicationUser applicationUser = mock(ApplicationUser.class);
        when(applicationUser.isActive()).thenReturn(true);
        when(applicationUser.getEmailAddress()).thenReturn("login");
        String message = "some message";

        when(userData.isEnabled(eq(applicationUser))).thenReturn(false);

        // WHEN
        boolean sent = myteamService.sendMessage(applicationUser, message);

        // THEN
        assertFalse(sent);
    }

    @Test
    void sendMessageWhenUserCanReceiveMessage() {
        // GIVEN
        ApplicationUser applicationUser = mock(ApplicationUser.class);
        when(applicationUser.isActive()).thenReturn(true);
        when(applicationUser.getEmailAddress()).thenReturn("login");
        String message = "some message";

        when(userData.isEnabled(eq(applicationUser))).thenReturn(true);

        // WHEN
        boolean sent = myteamService.sendMessage(applicationUser, message);

        // THEN
        assertTrue(sent);
        verify(myteamEventsListener).publishEvent(any(JiraNotifyEvent.class));
    }

    @Test
    void sendMessageToUserGroupWhenGroupNameIsNull() {
        // GIVEN
        String groupName = null;
        String message = "some message";
        // WHEN // THEN
        assertThrows(IllegalArgumentException.class, () -> myteamService.sendMessageToUserGroup(groupName, message));
    }

    @ParameterizedTest
    @EmptySource
    void sendMessageToUserGroupWhenMessageEmptyString(String message) {
        // GIVEN
        String groupName = "someGroupName";

        // WHEN // THEN
        assertThrows(IllegalArgumentException.class, () -> myteamService.sendMessageToUserGroup(groupName, message));
    }

    @Test
    void sendMessageToUserGroupWhenGroupNotFound() {
        // GIVEN
        String groupName = "someGroupName";
        String message = "someMessage";
        when(groupManager.getGroup(eq(groupName))).thenReturn(null);

        // WHEN // THEN
        assertThrows(IllegalArgumentException.class, () -> myteamService.sendMessageToUserGroup(groupName, message));
    }

    @Test
    void sendMessageToUserGroupWhenGroupIsEmpty() {
        // GIVEN
        String groupName = "someGroupName";
        String message = "someMessage";

        Group group = mock(Group.class);
        when(groupManager.getGroup(eq(groupName))).thenReturn(group);
        when(groupManager.getUsersInGroup(eq(group))).thenReturn(Collections.emptyList());

        // WHEN
        myteamService.sendMessageToUserGroup(groupName, message);

        // THEN
        verify(groupManager).getGroup(eq(groupName));
        verify(groupManager).getUsersInGroup(eq(group));
    }

    @Test
    void sendMessageToUserGroupWhenGroupNotEmpty() {
        // GIVEN
        String groupName = "someGroupName";
        String message = "someMessage";

        Group group = mock(Group.class);
        when(groupManager.getGroup(eq(groupName))).thenReturn(group);
        ApplicationUser user = mock(ApplicationUser.class);
        when(user.isActive()).thenReturn(true);
        when(user.getEmailAddress()).thenReturn("login");
        when(userData.isEnabled(eq(user))).thenReturn(true);
        when(groupManager.getUsersInGroup(eq(group))).thenReturn(Collections.singletonList(user));

        // WHEN
        myteamService.sendMessageToUserGroup(groupName, message);

        // THEN
        verify(groupManager).getGroup(eq(groupName));
        verify(groupManager).getUsersInGroup(eq(group));
        verify(user).isActive();
        verify(user).getEmailAddress();
        verify(userData).isEnabled(eq(user));
        verify(myteamEventsListener).publishEvent(any(JiraNotifyEvent.class));
    }

    @Test
    void testSendMessage() {
        // GIVEN
        String chatId = "someChatId";
        String message = "someMessage";

        // WHEN
        myteamService.sendMessage(chatId, message);

        // THEN
        verify(myteamEventsListener).publishEvent(any(JiraNotifyEvent.class));
    }

    @ParameterizedTest
    @EmptySource
    void findChatByIssueKeyWhenIssueKeyEmptyString(String issueKey) {
        // WHEN // THEN
        assertThrows(IllegalArgumentException.class, () -> myteamService.findChatByIssueKey(issueKey));
    }

    @Test
    void findChatByIssueKeyChatNotFoundByIssueKey() {
        // GIVEN
        String issueKey = "ABC-123";
        when(myteamChatRepository.findChatByIssueKey(eq(issueKey))).thenReturn(null);

        // WHEN
        MyteamChatMetaDto chatByIssueKey = myteamService.findChatByIssueKey(issueKey);

        // THEN
        assertNull(chatByIssueKey);
    }

    @Test
    void findChatByIssueKeyChatFoundByIssueKey() {
        // GIVEN
        String issueKey = "ABC-123";
        MyteamChatMeta myteamChatMeta = mock(MyteamChatMeta.class);
        when(myteamChatMeta.getID()).thenReturn(1);
        when(myteamChatMeta.getChatId()).thenReturn("someChatId");
        when(myteamChatMeta.getIssueKey()).thenReturn(issueKey);
        when(myteamChatRepository.findChatByIssueKey(eq(issueKey))).thenReturn(myteamChatMeta);

        // WHEN
        MyteamChatMetaDto chatByIssueKey = myteamService.findChatByIssueKey(issueKey);

        // THEN
        assertNotNull(chatByIssueKey);
        assertEquals(1, chatByIssueKey.getId());
        assertEquals("someChatId", chatByIssueKey.getChatId());
        assertEquals(issueKey, chatByIssueKey.getIssueKey());
    }

    @ParameterizedTest
    @EmptySource
    void linkChatWhenIssueKeyIsNull(String issueKey) {
        // GIVEN
        String chatId = "someChatId";

        // WHEN // THEN
        assertThrows(IllegalArgumentException.class, () -> myteamService.linkChat(chatId, issueKey));
    }

    @Test
    void linkChatWhenIssueNotFoundByIssueKey() {
        // GIVEN
        String chatId = "someChatId";
        String issueKey = "ABC-123";

        when(issueManager.getIssueByKeyIgnoreCase(eq(issueKey))).thenReturn(null);

        // WHEN // THEN
        assertThrows(IssueNotFoundException.class, () -> myteamService.linkChat(chatId, issueKey));
    }

    @Test
    void linkChatWhenProjectBannedForLinkingChat() {
        // GIVEN
        String chatId = "someChatId";
        String issueKey = "ABC-123";

        MutableIssue issueToLink = mock(MutableIssue.class);
        when(issueToLink.getProjectId()).thenReturn(10000L);
        when(pluginData.getChatCreationBannedProjectIds()).thenReturn(Collections.singleton(10000L));

        when(issueManager.getIssueByKeyIgnoreCase(eq(issueKey))).thenReturn(issueToLink);

        // WHEN // THEN
        assertThrows(IllegalArgumentException.class, () -> myteamService.linkChat(chatId, issueKey));
    }

    @Test
    void linkChatWhenChatAlreadyLinkedToIssue() {
        // GIVEN
        String chatId = "someChatId";
        String issueKey = "ABC-123";

        MutableIssue issueToLink = mock(MutableIssue.class);
        when(issueToLink.getProjectId()).thenReturn(10000L);
        when(pluginData.getChatCreationBannedProjectIds()).thenReturn(Collections.singleton(10001L));

        when(issueManager.getIssueByKeyIgnoreCase(eq(issueKey))).thenReturn(issueToLink);

        MyteamChatMeta myteamChatMeta = mock(MyteamChatMeta.class);
        when(myteamChatMeta.getID()).thenReturn(1);
        when(myteamChatMeta.getChatId()).thenReturn(chatId);
        when(myteamChatMeta.getIssueKey()).thenReturn(issueKey);
        when(myteamChatRepository.findChatByIssueKey(eq(issueKey))).thenReturn(myteamChatMeta);

        // WHEN // THEN
        assertThrows(LinkIssueWithChatException.class, () -> myteamService.linkChat(chatId, issueKey));
    }

    @Test
    void linkChatWhenChatNotLinkedToIssue() throws LinkIssueWithChatException {
        // GIVEN
        String chatId = "someChatId";
        String issueKey = "ABC-123";

        MutableIssue issueToLink = mock(MutableIssue.class);
        when(issueToLink.getProjectId()).thenReturn(10000L);
        when(pluginData.getChatCreationBannedProjectIds()).thenReturn(Collections.singleton(10001L));

        when(issueManager.getIssueByKeyIgnoreCase(eq(issueKey))).thenReturn(issueToLink);
        when(myteamChatRepository.findChatByIssueKey(eq(issueKey))).thenReturn(null);

        // WHEN
        myteamService.linkChat(chatId, issueKey);

        // THEN
        verify(myteamChatRepository).persistChat(eq(chatId), eq(issueKey));
    }

    @ParameterizedTest
    @EmptySource
    void createChatByJiraApplicationUsersWhenChatNameEmptyString(String chatName) {
        // GIVEN
        String issueKeyLinkToChat = null;
        List<ApplicationUser> jiraUsers = null;
        ApplicationUser loggedInUser = null;
        boolean isPublic = true;

        // WHEN // THEN
        assertThrows(IllegalArgumentException.class, () -> myteamService.createChatByJiraApplicationUsers(issueKeyLinkToChat, chatName, jiraUsers, loggedInUser, isPublic));
    }

    @Test
    void createChatByJiraApplicationUsersWhenLoggedInUserIsNull() {
        // GIVEN
        String issueKeyLinkToChat = null;
        String chatName = "someChatName";
        List<ApplicationUser> jiraUsers = null;
        ApplicationUser loggedInUser = null;
        boolean isPublic = true;

        // WHEN // THEN
        assertThrows(IllegalArgumentException.class, () -> myteamService.createChatByJiraApplicationUsers(issueKeyLinkToChat, chatName, jiraUsers, loggedInUser, isPublic));
    }

    @Test
    void createChatByJiraApplicationUsersMoreThanMaxUserCountForCreatingChat() {
        // GIVEN
        String issueKeyLinkToChat = "ABC-123";
        String chatName = "someChatName";
        List<ApplicationUser> jiraUsers = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            ApplicationUser applicationUser = mock(ApplicationUser.class);
            when(applicationUser.getEmailAddress()).thenReturn("someEmail" + i);
            when(userData.isCreateChatsWithUserAllowed(eq(applicationUser))).thenReturn(true);
            jiraUsers.add(applicationUser);
        }
        ApplicationUser loggedInUser = mock(ApplicationUser.class);
        boolean isPublic = true;


        MutableIssue issueToLink = mock(MutableIssue.class);
        when(issueToLink.getProjectId()).thenReturn(10000L);
        when(pluginData.getChatCreationBannedProjectIds()).thenReturn(Collections.singleton(10001L));

        when(issueManager.getIssueByKeyIgnoreCase(eq(issueKeyLinkToChat))).thenReturn(issueToLink);
        when(myteamChatRepository.findChatByIssueKey(eq(issueKeyLinkToChat))).thenReturn(null);

        // WHEN // THEN
        assertThrows(IllegalArgumentException.class, () -> myteamService.createChatByJiraApplicationUsers(issueKeyLinkToChat, chatName, jiraUsers, loggedInUser, isPublic));
    }

    @Test
    void createChatByJiraApplicationUsersWhenListUserNotEmptyAndChatWasCreatedWithoutSettingUpdatedAboutChat() throws LinkIssueWithChatException, MyteamServerErrorException, IOException {
        // GIVEN
        String issueKeyLinkToChat = "ABC-123";
        String chatName = "someChatName";
        ApplicationUser user = mock(ApplicationUser.class);
        when(user.getEmailAddress()).thenReturn("someEmail");
        when(userData.isCreateChatsWithUserAllowed(eq(user))).thenReturn(true);
        List<ApplicationUser> jiraUsers = Collections.singletonList(user);
        ApplicationUser loggedInUser = mock(ApplicationUser.class);
        boolean isPublic = true;

        MutableIssue issueToLink = mock(MutableIssue.class);
        when(issueToLink.getProjectId()).thenReturn(10000L);
        when(pluginData.getChatCreationBannedProjectIds()).thenReturn(Collections.singleton(10001L));

        when(issueManager.getIssueByKeyIgnoreCase(eq(issueKeyLinkToChat))).thenReturn(issueToLink);
        when(myteamChatRepository.findChatByIssueKey(eq(issueKeyLinkToChat))).thenReturn(null);
        when(applicationProperties.getString(eq(APKeys.JIRA_BASEURL))).thenReturn("baseurl");

        when(i18nResolver.getText(eq("ru.mail.jira.plugins.myteam.createChat.about.text"), eq(""), eq("baseurl/browse/ABC-123"))).thenReturn("someAbout with link issue baseurl/browse/ABC-123");
        when(pluginData.getToken()).thenReturn("someToken");

        HttpResponse<CreateChatResponse> httpResponse = mock(HttpResponse.class);
        when(httpResponse.getStatus()).thenReturn(200);
        CreateChatResponse createChatResponse = new CreateChatResponse();
        createChatResponse.setSn("someChatId");
        when(httpResponse.getBody()).thenReturn(createChatResponse);
        when(httpResponse.getStatus()).thenReturn(200);
        when(myteamApiClient.createChat(eq("someToken"), eq(chatName), eq("someAbout with link issue baseurl/browse/ABC-123"), anyList(), eq(isPublic))).thenReturn(httpResponse);
        String someFirstMessageInChatAfterCreating = "some first message";
        when(i18nResolver.getRawText(eq("ru.mail.jira.plugins.myteam.myteamEventsListener.groupChat.all.commands"))).thenReturn(someFirstMessageInChatAfterCreating);
        when(myteamApiClient.getChatInfo(eq("someChatId"))).thenThrow(new MyteamServerErrorException(500, "error!"));

        // WHEN
        RuntimeException exception = assertThrows(RuntimeException.class, () -> myteamService.createChatByJiraApplicationUsers(issueKeyLinkToChat, chatName, jiraUsers, loggedInUser, isPublic));

        // THEN
        assertEquals(MyteamServerErrorException.class, exception.getCause().getClass());
        assertEquals(500, ((MyteamServerErrorException) exception.getCause()).getStatus());
        assertEquals("Exception during chat creation", exception.getMessage());

        verify(userData).isCreateChatsWithUserAllowed(any(ApplicationUser.class));
        verify(user).getEmailAddress();
        verify(applicationProperties).getString(eq(APKeys.JIRA_BASEURL));
        verify(i18nResolver).getText(eq("ru.mail.jira.plugins.myteam.createChat.about.text"), eq(""), eq("baseurl/browse/ABC-123"));
        verify(pluginData).getToken();
        verify(myteamApiClient).createChat(eq("someToken"), eq(chatName), eq("someAbout with link issue baseurl/browse/ABC-123"), anyList(), eq(isPublic));
        verify(myteamChatRepository).persistChat(eq("someChatId"), eq(issueKeyLinkToChat));
        verify(myteamApiClient).sendMessageText(eq("someChatId"), eq(someFirstMessageInChatAfterCreating));
        verify(myteamEventsListener).publishEvent(any(JiraIssueViewEvent.class));
    }

    @ParameterizedTest
    @EmptySource
    void createChatByJiraApplicationUsersWhenListUserEmptyAndChatWasCreatedWithoutSettingUpdatedAboutChat(List<ApplicationUser> jiraUsers) throws LinkIssueWithChatException, MyteamServerErrorException, IOException {
        // GIVEN
        String issueKeyLinkToChat = "ABC-123";
        String chatName = "someChatName";
        ApplicationUser loggedInUser = mock(ApplicationUser.class);
        boolean isPublic = true;

        MutableIssue issueToLink = mock(MutableIssue.class);
        when(issueToLink.getProjectId()).thenReturn(10000L);
        when(pluginData.getChatCreationBannedProjectIds()).thenReturn(Collections.singleton(10001L));

        when(issueManager.getIssueByKeyIgnoreCase(eq(issueKeyLinkToChat))).thenReturn(issueToLink);
        when(myteamChatRepository.findChatByIssueKey(eq(issueKeyLinkToChat))).thenReturn(null);
        when(applicationProperties.getString(eq(APKeys.JIRA_BASEURL))).thenReturn("baseurl");

        when(i18nResolver.getText(eq("ru.mail.jira.plugins.myteam.createChat.about.text"), eq(""), eq("baseurl/browse/ABC-123"))).thenReturn("someAbout with link issue baseurl/browse/ABC-123");
        when(pluginData.getToken()).thenReturn("someToken");

        HttpResponse<CreateChatResponse> httpResponse = mock(HttpResponse.class);
        when(httpResponse.getStatus()).thenReturn(200);
        CreateChatResponse createChatResponse = new CreateChatResponse();
        createChatResponse.setSn("someChatId");
        when(httpResponse.getBody()).thenReturn(createChatResponse);
        when(httpResponse.getStatus()).thenReturn(200);
        when(myteamApiClient.createChat(eq("someToken"), eq(chatName), eq("someAbout with link issue baseurl/browse/ABC-123"), anyList(), eq(isPublic))).thenReturn(httpResponse);
        String someFirstMessageInChatAfterCreating = "some first message";
        when(i18nResolver.getRawText(eq("ru.mail.jira.plugins.myteam.myteamEventsListener.groupChat.all.commands"))).thenReturn(someFirstMessageInChatAfterCreating);
        when(myteamApiClient.getChatInfo(eq("someChatId"))).thenThrow(new MyteamServerErrorException(500, "error!"));

        // WHEN
        RuntimeException exception = assertThrows(RuntimeException.class, () -> myteamService.createChatByJiraApplicationUsers(issueKeyLinkToChat, chatName, jiraUsers, loggedInUser, isPublic));

        // THEN
        assertEquals(MyteamServerErrorException.class, exception.getCause().getClass());
        assertEquals(500, ((MyteamServerErrorException) exception.getCause()).getStatus());
        assertEquals("Exception during chat creation", exception.getMessage());

        verify(applicationProperties).getString(eq(APKeys.JIRA_BASEURL));
        verify(i18nResolver).getText(eq("ru.mail.jira.plugins.myteam.createChat.about.text"), eq(""), eq("baseurl/browse/ABC-123"));
        verify(pluginData).getToken();
        verify(myteamApiClient).createChat(eq("someToken"), eq(chatName), eq("someAbout with link issue baseurl/browse/ABC-123"), anyList(), eq(isPublic));
        verify(myteamChatRepository).persistChat(eq("someChatId"), eq(issueKeyLinkToChat));
        verify(myteamApiClient).sendMessageText(eq("someChatId"), eq(someFirstMessageInChatAfterCreating));
        verify(myteamEventsListener).publishEvent(any(JiraIssueViewEvent.class));
    }



    @Test
    void createChatByJiraUserIdsWhenFirstMessageWillNotSent() throws MyteamServerErrorException, IOException, LinkIssueWithChatException {
        // GIVEN
        String issueKeyLinkToChat = "ABC-123";
        String chatName = "someChatName";
        List<Long> jiraUsers = Collections.singletonList(10000L);
        ApplicationUser user = mock(ApplicationUser.class);
        when(user.isActive()).thenReturn(true);
        when(user.getEmailAddress()).thenReturn("someEmail");
        when(userManager.getUserById(eq(10000L))).thenReturn(Optional.of(user));
        when(userData.isCreateChatsWithUserAllowed(eq(user))).thenReturn(true);
        ApplicationUser loggedInUser = mock(ApplicationUser.class);
        boolean isPublic = true;

        MutableIssue issueToLink = mock(MutableIssue.class);
        when(issueToLink.getProjectId()).thenReturn(10000L);
        when(pluginData.getChatCreationBannedProjectIds()).thenReturn(Collections.singleton(10001L));

        when(issueManager.getIssueByKeyIgnoreCase(eq(issueKeyLinkToChat))).thenReturn(issueToLink);
        when(myteamChatRepository.findChatByIssueKey(eq(issueKeyLinkToChat))).thenReturn(null);
        when(applicationProperties.getString(eq(APKeys.JIRA_BASEURL))).thenReturn("baseurl");

        when(i18nResolver.getText(eq("ru.mail.jira.plugins.myteam.createChat.about.text"), eq(""), eq("baseurl/browse/ABC-123"))).thenReturn("someAbout with link issue baseurl/browse/ABC-123");
        String someBotToken = "someToken";
        when(pluginData.getToken()).thenReturn(someBotToken);

        HttpResponse<CreateChatResponse> httpResponse = mock(HttpResponse.class);
        when(httpResponse.getStatus()).thenReturn(200);
        CreateChatResponse createChatResponse = new CreateChatResponse();
        String createdChatId = "someChatId";
        createChatResponse.setSn(createdChatId);
        when(httpResponse.getBody()).thenReturn(createChatResponse);
        when(httpResponse.getStatus()).thenReturn(200);
        when(myteamApiClient.createChat(eq(someBotToken), eq(chatName), eq("someAbout with link issue baseurl/browse/ABC-123"), anyList(), eq(isPublic))).thenReturn(httpResponse);
        String someFirstMessageInChatAfterCreating = "some first message";
        when(i18nResolver.getRawText(eq("ru.mail.jira.plugins.myteam.myteamEventsListener.groupChat.all.commands"))).thenReturn(someFirstMessageInChatAfterCreating);

        when(myteamApiClient.sendMessageText(eq(createdChatId), eq(someFirstMessageInChatAfterCreating))).thenThrow(new MyteamServerErrorException(500, "error!"));

        HttpResponse<ChatInfoResponse> chatInfoHttpResponse = mock(HttpResponse.class);
        GroupChatInfo chatInfoResponse = new GroupChatInfo();
        chatInfoResponse.setInviteLink("inviteLink");
        chatInfoResponse.setTitle(chatName);
        when(chatInfoHttpResponse.getStatus()).thenReturn(200);
        when(chatInfoHttpResponse.getBody()).thenReturn(chatInfoResponse);
        when(myteamApiClient.getChatInfo(eq(createdChatId))).thenReturn(chatInfoHttpResponse);

        when(userSearchService.findUsersByEmail(eq("someEmail"))).thenReturn(Collections.singletonList(user));
        when(avatarService.getAvatarURL(eq(loggedInUser), eq(user))).thenReturn(URI.create("http://localhost:8080/some_avatar_url"));
        when(i18nResolver.getText(eq("ru.mail.jira.plugins.myteam.createChat.about.text"), eq(chatInfoResponse.getInviteLink()), eq("baseurl/browse/ABC-123"))).thenReturn("updated someAbout with link issue baseurl/browse/ABC-123");


        // WHEN
        myteamService.createChatByJiraUserIds(issueKeyLinkToChat, chatName, jiraUsers, loggedInUser, isPublic);

        // THEN
        verify(userData).isCreateChatsWithUserAllowed(any(ApplicationUser.class));
        verify(user).getEmailAddress();
        verify(applicationProperties, times(2)).getString(eq(APKeys.JIRA_BASEURL));
        verify(i18nResolver).getText(eq("ru.mail.jira.plugins.myteam.createChat.about.text"), eq(""), eq("baseurl/browse/ABC-123"));
        verify(pluginData, times(2)).getToken();
        verify(myteamApiClient).createChat(eq(someBotToken), eq(chatName), eq("someAbout with link issue baseurl/browse/ABC-123"), anyList(), eq(isPublic));
        verify(myteamChatRepository).persistChat(eq(createdChatId), eq(issueKeyLinkToChat));
        verify(myteamApiClient).getChatInfo(eq(createdChatId));
        verify(myteamApiClient).sendMessageText(eq("someChatId"), eq(someFirstMessageInChatAfterCreating));
        verify(myteamEventsListener).publishEvent(any(JiraIssueViewEvent.class));
        verify(userSearchService).findUsersByEmail(eq("someEmail"));
        verify(avatarService).getAvatarURL(eq(loggedInUser), eq(user));
        verify(user).isActive();
        verify(i18nResolver).getText(eq("ru.mail.jira.plugins.myteam.createChat.about.text"), eq(chatInfoResponse.getInviteLink()), eq("baseurl/browse/ABC-123"));
        verify(myteamApiClient).setAboutChat(eq(someBotToken), eq(createdChatId), eq("updated someAbout with link issue baseurl/browse/ABC-123"));
    }

    @ParameterizedTest
    @MethodSource(value = "getBadResponseForCreatingChat")
    void createChatByJiraUserIdsWhenChatNotCreated(HttpResponse<CreateChatResponse> createChatResponseHttpResponse) throws MyteamServerErrorException, IOException, LinkIssueWithChatException {
        // GIVEN
        String issueKeyLinkToChat = "ABC-123";
        String chatName = "someChatName";
        List<Long> jiraUsers = Collections.singletonList(10000L);
        ApplicationUser user = mock(ApplicationUser.class);
        when(user.getEmailAddress()).thenReturn("someEmail");
        when(userManager.getUserById(eq(10000L))).thenReturn(Optional.of(user));
        when(userData.isCreateChatsWithUserAllowed(eq(user))).thenReturn(true);
        ApplicationUser loggedInUser = mock(ApplicationUser.class);
        boolean isPublic = true;

        MutableIssue issueToLink = mock(MutableIssue.class);
        when(issueToLink.getProjectId()).thenReturn(10000L);
        when(pluginData.getChatCreationBannedProjectIds()).thenReturn(Collections.singleton(10001L));

        when(issueManager.getIssueByKeyIgnoreCase(eq(issueKeyLinkToChat))).thenReturn(issueToLink);
        when(myteamChatRepository.findChatByIssueKey(eq(issueKeyLinkToChat))).thenReturn(null);
        when(applicationProperties.getString(eq(APKeys.JIRA_BASEURL))).thenReturn("baseurl");

        when(i18nResolver.getText(eq("ru.mail.jira.plugins.myteam.createChat.about.text"), eq(""), eq("baseurl/browse/ABC-123"))).thenReturn("someAbout with link issue baseurl/browse/ABC-123");
        String someBotToken = "someToken";
        when(pluginData.getToken()).thenReturn(someBotToken);

        when(myteamApiClient.createChat(eq(someBotToken), eq(chatName), eq("someAbout with link issue baseurl/browse/ABC-123"), anyList(), eq(isPublic))).thenReturn(createChatResponseHttpResponse);


        // WHEN // THEN
        assertThrows(RuntimeException.class, () -> myteamService.createChatByJiraUserIds(issueKeyLinkToChat, chatName, jiraUsers, loggedInUser, isPublic));
    }

    @ParameterizedTest
    @MethodSource(value = "getBadResponseForGettingAboutChatInfo")
    void createChatByJiraUserIdsWhenChatWasCreatedAndGettingAboutChatReturnBadHttpResponse(HttpResponse<ChatInfoResponse> chatInfoResponseHttpResponse) throws MyteamServerErrorException, IOException, LinkIssueWithChatException {
        // GIVEN
        String issueKeyLinkToChat = "ABC-123";
        String chatName = "someChatName";
        List<Long> jiraUsers = Collections.singletonList(10000L);
        ApplicationUser user = mock(ApplicationUser.class);
        when(user.getEmailAddress()).thenReturn("someEmail");
        when(userManager.getUserById(eq(10000L))).thenReturn(Optional.of(user));
        when(userData.isCreateChatsWithUserAllowed(eq(user))).thenReturn(true);
        ApplicationUser loggedInUser = mock(ApplicationUser.class);
        boolean isPublic = true;

        MutableIssue issueToLink = mock(MutableIssue.class);
        when(issueToLink.getProjectId()).thenReturn(10000L);
        when(pluginData.getChatCreationBannedProjectIds()).thenReturn(Collections.singleton(10001L));

        when(issueManager.getIssueByKeyIgnoreCase(eq(issueKeyLinkToChat))).thenReturn(issueToLink);
        when(myteamChatRepository.findChatByIssueKey(eq(issueKeyLinkToChat))).thenReturn(null);
        when(applicationProperties.getString(eq(APKeys.JIRA_BASEURL))).thenReturn("baseurl");

        when(i18nResolver.getText(eq("ru.mail.jira.plugins.myteam.createChat.about.text"), eq(""), eq("baseurl/browse/ABC-123"))).thenReturn("someAbout with link issue baseurl/browse/ABC-123");
        String someBotToken = "someToken";
        when(pluginData.getToken()).thenReturn(someBotToken);

        HttpResponse<CreateChatResponse> httpResponse = mock(HttpResponse.class);
        when(httpResponse.getStatus()).thenReturn(200);
        CreateChatResponse createChatResponse = new CreateChatResponse();
        String createdChatId = "someChatId";
        createChatResponse.setSn(createdChatId);
        when(httpResponse.getBody()).thenReturn(createChatResponse);
        when(httpResponse.getStatus()).thenReturn(200);
        when(myteamApiClient.createChat(eq(someBotToken), eq(chatName), eq("someAbout with link issue baseurl/browse/ABC-123"), anyList(), eq(isPublic))).thenReturn(httpResponse);
        String someFirstMessageInChatAfterCreating = "some first message";
        when(i18nResolver.getRawText(eq("ru.mail.jira.plugins.myteam.myteamEventsListener.groupChat.all.commands"))).thenReturn(someFirstMessageInChatAfterCreating);
        when(myteamApiClient.getChatInfo(eq(createdChatId))).thenReturn(chatInfoResponseHttpResponse);


        // WHEN // THEN
        assertThrows(RuntimeException.class, () -> myteamService.createChatByJiraUserIds(issueKeyLinkToChat, chatName, jiraUsers, loggedInUser, isPublic));
    }

    @Test
    void createChatByJiraUserIdsWhenWholeFlowSuccess() throws MyteamServerErrorException, IOException, LinkIssueWithChatException {
        // GIVEN
        String issueKeyLinkToChat = "ABC-123";
        String chatName = "someChatName";
        List<Long> jiraUsers = Collections.singletonList(10000L);
        ApplicationUser user = mock(ApplicationUser.class);
        when(user.isActive()).thenReturn(true);
        when(user.getEmailAddress()).thenReturn("someEmail");
        when(userManager.getUserById(eq(10000L))).thenReturn(Optional.of(user));
        when(userData.isCreateChatsWithUserAllowed(eq(user))).thenReturn(true);
        ApplicationUser loggedInUser = mock(ApplicationUser.class);
        boolean isPublic = true;

        MutableIssue issueToLink = mock(MutableIssue.class);
        when(issueToLink.getProjectId()).thenReturn(10000L);
        when(pluginData.getChatCreationBannedProjectIds()).thenReturn(Collections.singleton(10001L));

        when(issueManager.getIssueByKeyIgnoreCase(eq(issueKeyLinkToChat))).thenReturn(issueToLink);
        when(myteamChatRepository.findChatByIssueKey(eq(issueKeyLinkToChat))).thenReturn(null);
        when(applicationProperties.getString(eq(APKeys.JIRA_BASEURL))).thenReturn("baseurl");

        when(i18nResolver.getText(eq("ru.mail.jira.plugins.myteam.createChat.about.text"), eq(""), eq("baseurl/browse/ABC-123"))).thenReturn("someAbout with link issue baseurl/browse/ABC-123");
        String someBotToken = "someToken";
        when(pluginData.getToken()).thenReturn(someBotToken);

        HttpResponse<CreateChatResponse> httpResponse = mock(HttpResponse.class);
        when(httpResponse.getStatus()).thenReturn(200);
        CreateChatResponse createChatResponse = new CreateChatResponse();
        String createdChatId = "someChatId";
        createChatResponse.setSn(createdChatId);
        when(httpResponse.getBody()).thenReturn(createChatResponse);
        when(httpResponse.getStatus()).thenReturn(200);
        when(myteamApiClient.createChat(eq(someBotToken), eq(chatName), eq("someAbout with link issue baseurl/browse/ABC-123"), anyList(), eq(isPublic))).thenReturn(httpResponse);
        String someFirstMessageInChatAfterCreating = "some first message";
        when(i18nResolver.getRawText(eq("ru.mail.jira.plugins.myteam.myteamEventsListener.groupChat.all.commands"))).thenReturn(someFirstMessageInChatAfterCreating);
        HttpResponse<ChatInfoResponse> chatInfoHttpResponse = mock(HttpResponse.class);
        GroupChatInfo chatInfoResponse = new GroupChatInfo();
        chatInfoResponse.setInviteLink("inviteLink");
        chatInfoResponse.setTitle(chatName);
        when(chatInfoHttpResponse.getStatus()).thenReturn(200);
        when(chatInfoHttpResponse.getBody()).thenReturn(chatInfoResponse);
        when(myteamApiClient.getChatInfo(eq(createdChatId))).thenReturn(chatInfoHttpResponse);

        when(userSearchService.findUsersByEmail(eq("someEmail"))).thenReturn(Collections.singletonList(user));
        when(avatarService.getAvatarURL(eq(loggedInUser), eq(user))).thenReturn(URI.create("http://localhost:8080/some_avatar_url"));
        when(i18nResolver.getText(eq("ru.mail.jira.plugins.myteam.createChat.about.text"), eq(chatInfoResponse.getInviteLink()), eq("baseurl/browse/ABC-123"))).thenReturn("updated someAbout with link issue baseurl/browse/ABC-123");


        // WHEN
        myteamService.createChatByJiraUserIds(issueKeyLinkToChat, chatName, jiraUsers, loggedInUser, isPublic);

        // THEN
        verify(userData).isCreateChatsWithUserAllowed(any(ApplicationUser.class));
        verify(user).getEmailAddress();
        verify(applicationProperties, times(2)).getString(eq(APKeys.JIRA_BASEURL));
        verify(i18nResolver).getText(eq("ru.mail.jira.plugins.myteam.createChat.about.text"), eq(""), eq("baseurl/browse/ABC-123"));
        verify(pluginData, times(2)).getToken();
        verify(myteamApiClient).createChat(eq(someBotToken), eq(chatName), eq("someAbout with link issue baseurl/browse/ABC-123"), anyList(), eq(isPublic));
        verify(myteamChatRepository).persistChat(eq(createdChatId), eq(issueKeyLinkToChat));
        verify(myteamApiClient).getChatInfo(eq(createdChatId));
        verify(myteamApiClient).sendMessageText(eq("someChatId"), eq(someFirstMessageInChatAfterCreating));
        verify(myteamEventsListener).publishEvent(any(JiraIssueViewEvent.class));
        verify(userSearchService).findUsersByEmail(eq("someEmail"));
        verify(avatarService).getAvatarURL(eq(loggedInUser), eq(user));
        verify(user).isActive();
        verify(i18nResolver).getText(eq("ru.mail.jira.plugins.myteam.createChat.about.text"), eq(chatInfoResponse.getInviteLink()), eq("baseurl/browse/ABC-123"));
        verify(myteamApiClient).setAboutChat(eq(someBotToken), eq(createdChatId), eq("updated someAbout with link issue baseurl/browse/ABC-123"));
    }

    private static Stream<Arguments> getBadResponseForCreatingChat() {
        HttpResponse<CreateChatResponse> badResponse1 = mock(HttpResponse.class);
        when(badResponse1.getStatus()).thenReturn(500);
        when(badResponse1.getBody()).thenReturn(null);
        HttpResponse<CreateChatResponse> badResponse2 = mock(HttpResponse.class);
        when(badResponse2.getStatus()).thenReturn(200);
        when(badResponse2.getBody()).thenReturn(null);

        HttpResponse<CreateChatResponse> badResponse3 = mock(HttpResponse.class);
        when(badResponse3.getStatus()).thenReturn(200);
        String badOriginalBody = "{\"sn\": \"123132\"}";
        RuntimeException someInternalParsingException = new RuntimeException();
        when(badResponse3.getBody()).thenReturn(null);
        when(badResponse3.getParsingError()).thenReturn(Optional.of(new UnirestParsingException(badOriginalBody, someInternalParsingException)));
        return Stream.of(Arguments.of(badResponse1), Arguments.of(badResponse2), Arguments.of(badResponse3));
    }

    public static Stream<Arguments> getBadResponseForGettingAboutChatInfo() {
        HttpResponse<ChatInfoResponse> badResponse1 = mock(HttpResponse.class);
        when(badResponse1.getStatus()).thenReturn(500);
        when(badResponse1.getBody()).thenReturn(null);

        HttpResponse<CreateChatResponse> badResponse2 = mock(HttpResponse.class);
        when(badResponse2.getStatus()).thenReturn(200);
        String badOriginalBody = "someOriginalBody";
        RuntimeException someInternalParsingException = new RuntimeException();
        when(badResponse2.getBody()).thenReturn(null);
        when(badResponse2.getParsingError()).thenReturn(Optional.of(new UnirestParsingException(badOriginalBody, someInternalParsingException)));
        return Stream.of(Arguments.of(badResponse1), Arguments.of(badResponse2));
    }
}