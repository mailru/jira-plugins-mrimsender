/* (C)2020 */
package ru.mail.jira.plugins.myteam.myteam;

import java.io.IOException;
import java.util.List;
import javax.annotation.Nonnull;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.UnirestException;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.myteam.dto.FetchResponseDto;
import ru.mail.jira.plugins.myteam.myteam.dto.FileResponse;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;
import ru.mail.jira.plugins.myteam.myteam.dto.MessageResponse;
import ru.mail.jira.plugins.myteam.myteam.dto.chats.ChatInfoResponse;
import ru.mail.jira.plugins.myteam.myteam.dto.chats.ChatMember;
import ru.mail.jira.plugins.myteam.myteam.dto.chats.ChatMemberId;
import ru.mail.jira.plugins.myteam.myteam.dto.chats.CreateChatResponse;
import ru.mail.jira.plugins.myteam.myteam.dto.chats.SuccessResponse;

public interface MyteamApiClient {
  HttpResponse<MessageResponse> sendMessageText(
      String chatId, String text, List<List<InlineKeyboardMarkupButton>> inlineKeyboardMarkup)
      throws UnirestException, IOException, MyteamServerErrorException;

  HttpResponse<MessageResponse> sendMessageText(String chatId, String text)
      throws UnirestException, IOException, MyteamServerErrorException;

  HttpResponse<FetchResponseDto> getEvents(long lastEventId, long pollTime)
      throws UnirestException, MyteamServerErrorException;

  HttpResponse<JsonNode> answerCallbackQuery(
      String queryId, String text, boolean showAlert, String url)
      throws UnirestException, MyteamServerErrorException;

  HttpResponse<JsonNode> answerCallbackQuery(String queryId)
      throws UnirestException, MyteamServerErrorException;

  void updateSettings();

  HttpResponse<FileResponse> getFile(String fileId)
      throws UnirestException, MyteamServerErrorException;

  HttpResponse<MessageResponse> editMessageText(
      String chatId,
      long messageId,
      String text,
      List<List<InlineKeyboardMarkupButton>> inlineKeyboardMarkup)
      throws UnirestException, IOException, MyteamServerErrorException;

  HttpResponse<CreateChatResponse> createChat(
      @Nonnull String creatorBotToken,
      @Nonnull String name,
      String about,
      @Nonnull List<ChatMemberId> members,
      boolean isPublic)
      throws IOException, UnirestException, MyteamServerErrorException;

  HttpResponse<SuccessResponse> setAboutChat(
      @Nonnull String botToken, @Nonnull String chatId, String about)
      throws IOException, UnirestException, MyteamServerErrorException;

  HttpResponse<ChatInfoResponse> getChatInfo(@Nonnull String botToken, @Nonnull String chatId)
      throws UnirestException, MyteamServerErrorException;

  HttpResponse<ChatMember> getMembers(@Nonnull String chatId) throws UnirestException;
}
