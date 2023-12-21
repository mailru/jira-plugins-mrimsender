/* (C)2023 */
package ru.mail.jira.plugins.myteam.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.sal.api.message.I18nResolver;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import kong.unirest.HttpResponse;
import kong.unirest.UnirestException;
import kong.unirest.UnirestParsingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.component.MessageFormatter;
import ru.mail.jira.plugins.myteam.component.PermissionHelper;
import ru.mail.jira.plugins.myteam.component.UserData;
import ru.mail.jira.plugins.myteam.db.model.MyteamChatMeta;
import ru.mail.jira.plugins.myteam.db.repository.MyteamChatRepository;
import ru.mail.jira.plugins.myteam.myteam.MyteamApiClient;
import ru.mail.jira.plugins.myteam.myteam.dto.chats.ChatMemberId;
import ru.mail.jira.plugins.myteam.myteam.dto.chats.CreateChatResponse;
import ru.mail.jira.plugins.myteam.service.IssueService;
import ru.mail.jira.plugins.myteam.service.PluginData;
import ru.mail.jira.plugins.myteam.service.StateManager;
import ru.mail.jira.plugins.myteam.service.model.MyteamChatMetaDto;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"MockNotUsedInProduction", "UnusedVariable"})
class UserChatServiceImplTest {

  @Mock
  @SuppressWarnings("NullAway")
  private UserData userData;

  @Mock
  @SuppressWarnings("NullAway")
  private MyteamApiClient myteamClient;

  @Mock
  @SuppressWarnings("NullAway")
  private PermissionHelper permissionHelper;

  @Mock
  @SuppressWarnings("NullAway")
  private I18nResolver i18nResolver;

  @Mock
  @SuppressWarnings("NullAway")
  private StateManager stateManager;

  @Mock
  @SuppressWarnings("NullAway")
  private IssueService issueService;

  @Mock
  @SuppressWarnings("NullAway")
  private MyteamChatRepository myteamChatRepository;

  @Mock
  @SuppressWarnings("NullAway")
  private JiraAuthenticationContext jiraAuthenticationContext;

  @Mock
  @SuppressWarnings("NullAway")
  private MessageFormatter messageFormatter;

  @Mock
  @SuppressWarnings("NullAway")
  private PluginData pluginData;

  @InjectMocks
  @SuppressWarnings("NullAway")
  private UserChatServiceImpl userChatService;

  @Test
  void unsafeLinkChat() {
    // GIVEN
    String issueKey = "someIssueKey";
    String chatId = "someChatId";
    MyteamChatMeta myteamChatMeta = mock(MyteamChatMeta.class);
    when(myteamChatMeta.getChatId()).thenReturn(chatId);
    when(myteamChatMeta.getIssueKey()).thenReturn(issueKey);
    when(myteamChatMeta.getID()).thenReturn(1);
    when(myteamChatRepository.persistChat(eq(chatId), eq(issueKey))).thenReturn(myteamChatMeta);

    // WHEN
    MyteamChatMetaDto myteamChatMetaDto = userChatService.unsafeLinkChat(chatId, issueKey);

    // THEN
    assertNotNull(myteamChatMetaDto);
    assertEquals(1, myteamChatMetaDto.getId());
    assertEquals("someChatId", myteamChatMetaDto.getChatId());
    assertEquals("someIssueKey", myteamChatMetaDto.getIssueKey());
  }

  @Test
  void findChatByIssueKeyWhenChatNotFoundByIssueKey() {
    // GIVEN
    String issueKey = "someIssueKey";
    when(myteamChatRepository.findChatByIssueKey(eq(issueKey))).thenReturn(null);

    // WHEN
    MyteamChatMetaDto chatByIssueKey = userChatService.findChatByIssueKey(issueKey);

    // THEN
    assertNull(chatByIssueKey);
  }

  @Test
  void findChatByIssueKeyWhenChatFoundByIssueKey() {
    // GIVEN
    String issueKey = "someIssueKey";
    MyteamChatMeta myteamChatMeta = mock(MyteamChatMeta.class);
    when(myteamChatMeta.getChatId()).thenReturn("someChatId");
    when(myteamChatMeta.getIssueKey()).thenReturn(issueKey);
    when(myteamChatMeta.getID()).thenReturn(1);
    when(myteamChatRepository.findChatByIssueKey(eq(issueKey))).thenReturn(myteamChatMeta);

    // WHEN
    MyteamChatMetaDto chatByIssueKey = userChatService.findChatByIssueKey(issueKey);

    // THEN
    assertNotNull(chatByIssueKey);
    assertEquals(1, chatByIssueKey.getId());
    assertEquals("someChatId", chatByIssueKey.getChatId());
    assertEquals("someIssueKey", chatByIssueKey.getIssueKey());
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void createChatSuccess(boolean publicChat) throws MyteamServerErrorException, IOException {
    // GIVEN
    String expectedChatId = "someChatId";

    String chatName = "someName";
    String about = "someAbout";
    List<ChatMemberId> members = List.of(new ChatMemberId("someEmail"));
    when(pluginData.getToken()).thenReturn("someToken");

    HttpResponse<CreateChatResponse> httpResponse = mock(HttpResponse.class);
    CreateChatResponse createChatResponse = new CreateChatResponse();
    createChatResponse.setSn(expectedChatId);
    when(httpResponse.getStatus()).thenReturn(200);
    when(httpResponse.getBody()).thenReturn(createChatResponse);
    when(myteamClient.createChat(
            eq("someToken"), eq(chatName), eq(about), eq(members), eq(publicChat)))
        .thenReturn(httpResponse);

    // WHEN
    String chatId = userChatService.createChat(chatName, about, members, publicChat);

    // THEN
    assertEquals(expectedChatId, chatId);
    verify(myteamClient).sendMessageText(eq(expectedChatId), nullable(String.class));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void createChatSuccessButInitialMessageNotSended(boolean publicChat)
      throws MyteamServerErrorException, IOException {
    // GIVEN
    String expectedChatId = "someChatId";

    String chatName = "someName";
    String about = "someAbout";
    List<ChatMemberId> members = List.of(new ChatMemberId("someEmail"));
    when(pluginData.getToken()).thenReturn("someToken");

    HttpResponse<CreateChatResponse> httpResponse = mock(HttpResponse.class);
    CreateChatResponse createChatResponse = new CreateChatResponse();
    createChatResponse.setSn(expectedChatId);
    when(httpResponse.getStatus()).thenReturn(200);
    when(httpResponse.getBody()).thenReturn(createChatResponse);
    when(myteamClient.createChat(
            eq("someToken"), eq(chatName), eq(about), eq(members), eq(publicChat)))
        .thenReturn(httpResponse);
    when(myteamClient.sendMessageText(eq(expectedChatId), nullable(String.class)))
        .thenThrow(new UnirestException("Something happened wrong"));

    // WHEN
    String chatId = userChatService.createChat(chatName, about, members, publicChat);

    // THEN
    assertEquals(expectedChatId, chatId);
    verify(myteamClient).sendMessageText(eq(expectedChatId), nullable(String.class));
  }

  @ParameterizedTest
  @ValueSource(ints = {400, 404})
  void createChatWhenBadResponse(int badResponseStatusCode)
      throws MyteamServerErrorException, IOException {
    // GIVEN
    String expectedChatId = "someChatId";

    String chatName = "someName";
    String about = "someAbout";
    List<ChatMemberId> members = List.of(new ChatMemberId("someEmail"));
    boolean publicChat = true;
    when(pluginData.getToken()).thenReturn("someToken");

    HttpResponse<CreateChatResponse> httpResponse = mock(HttpResponse.class);
    when(httpResponse.getStatus()).thenReturn(badResponseStatusCode);
    when(myteamClient.createChat(
            eq("someToken"), eq(chatName), eq(about), eq(members), eq(publicChat)))
        .thenReturn(httpResponse);

    // WHEN
    RuntimeException runtimeException =
        assertThrows(
            RuntimeException.class,
            () -> userChatService.createChat(chatName, about, members, publicChat));

    // THEN
    assertEquals(
        String.format(
            "Exception during chat creation chat sn not found. Response code: %s",
            badResponseStatusCode),
        runtimeException.getMessage());
    verify(myteamClient, never()).sendMessageText(eq(expectedChatId), nullable(String.class));
  }

  @Test
  void createChatWhenResponseHasNotBody() throws MyteamServerErrorException, IOException {
    // GIVEN
    String expectedChatId = "someChatId";

    String chatName = "someName";
    String about = "someAbout";
    List<ChatMemberId> members = List.of(new ChatMemberId("someEmail"));
    boolean publicChat = true;
    when(pluginData.getToken()).thenReturn("someToken");

    HttpResponse<CreateChatResponse> httpResponse = mock(HttpResponse.class);
    when(httpResponse.getBody()).thenReturn(null);
    when(httpResponse.getStatus()).thenReturn(200);
    when(myteamClient.createChat(
            eq("someToken"), eq(chatName), eq(about), eq(members), eq(publicChat)))
        .thenReturn(httpResponse);

    // WHEN
    RuntimeException runtimeException =
        assertThrows(
            RuntimeException.class,
            () -> userChatService.createChat(chatName, about, members, publicChat));

    // THEN
    assertEquals(
        "Exception during chat creation chat sn not found. Response code: 200",
        runtimeException.getMessage());
    verify(myteamClient, never()).sendMessageText(eq(expectedChatId), nullable(String.class));
  }

  @Test
  void createChatWhenResponseHasParsingError() throws MyteamServerErrorException, IOException {
    // GIVEN
    String expectedChatId = "someChatId";

    String chatName = "someName";
    String about = "someAbout";
    List<ChatMemberId> members = List.of(new ChatMemberId("someEmail"));
    boolean publicChat = true;
    when(pluginData.getToken()).thenReturn("someToken");

    HttpResponse<CreateChatResponse> httpResponse = mock(HttpResponse.class);
    when(httpResponse.getBody()).thenReturn(null);
    when(httpResponse.getStatus()).thenReturn(200);
    when(httpResponse.getParsingError())
        .thenReturn(Optional.of(new UnirestParsingException("someBody", new Exception())));
    when(myteamClient.createChat(
            eq("someToken"), eq(chatName), eq(about), eq(members), eq(publicChat)))
        .thenReturn(httpResponse);

    // WHEN
    RuntimeException runtimeException =
        assertThrows(
            RuntimeException.class,
            () -> userChatService.createChat(chatName, about, members, publicChat));

    // THEN
    assertEquals(
        "Exception during chat creation chat sn not found. Response code: 200",
        runtimeException.getMessage());
    verify(myteamClient, never()).sendMessageText(eq(expectedChatId), nullable(String.class));
  }

  @Test
  void createChatWhenResponseHasNotChatId() throws MyteamServerErrorException, IOException {
    // GIVEN
    String expectedChatId = "someChatId";

    String chatName = "someName";
    String about = "someAbout";
    List<ChatMemberId> members = List.of(new ChatMemberId("someEmail"));
    boolean publicChat = true;
    when(pluginData.getToken()).thenReturn("someToken");

    HttpResponse<CreateChatResponse> httpResponse = mock(HttpResponse.class);
    CreateChatResponse createChatResponse = new CreateChatResponse();
    when(httpResponse.getBody()).thenReturn(createChatResponse);
    when(httpResponse.getStatus()).thenReturn(200);
    when(myteamClient.createChat(
            eq("someToken"), eq(chatName), eq(about), eq(members), eq(publicChat)))
        .thenReturn(httpResponse);

    // WHEN
    RuntimeException runtimeException =
        assertThrows(
            RuntimeException.class,
            () -> userChatService.createChat(chatName, about, members, publicChat));

    // THEN
    assertEquals(
        "Exception during chat creation chat sn not found. Response code: 200",
        runtimeException.getMessage());
    verify(myteamClient, never()).sendMessageText(eq(expectedChatId), nullable(String.class));
  }

  @ParameterizedTest
  @MethodSource(value = "getExceptionsForClient")
  void createChatWhenClientThrowsException(Exception httpClientThrowingException)
      throws MyteamServerErrorException, IOException {
    // GIVEN
    String expectedChatId = "someChatId";
    String chatName = "someName";
    String about = "someAbout";
    List<ChatMemberId> members = List.of(new ChatMemberId("someEmail"));
    boolean publicChat = true;
    when(pluginData.getToken()).thenReturn("someToken");

    when(myteamClient.createChat(
            eq("someToken"), eq(chatName), eq(about), eq(members), eq(publicChat)))
        .thenThrow(httpClientThrowingException);

    // WHEN
    RuntimeException exception =
        assertThrows(
            RuntimeException.class,
            () -> userChatService.createChat(chatName, about, members, publicChat));

    // THEN
    assertEquals(exception.getMessage(), "Exception during chat creation");
    assertEquals(exception.getCause(), httpClientThrowingException);
    verify(myteamClient, never()).sendMessageText(eq(expectedChatId), nullable(String.class));
  }

  public static Stream<Arguments> getExceptionsForClient() {
    return Stream.of(
        Arguments.of(
            new MyteamServerErrorException(500, "some error happened"),
            Arguments.of(new IOException("I/O exception reading/writing bytes")),
            Arguments.arguments(new UnirestException("some parsing exception"))));
  }
}
