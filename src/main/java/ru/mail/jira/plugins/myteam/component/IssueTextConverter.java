/* (C)2021 */
package ru.mail.jira.plugins.myteam.component;

import com.atlassian.jira.issue.AttachmentManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.attachment.ConvertTemporaryAttachmentParams;
import com.atlassian.jira.issue.attachment.TemporaryAttachmentId;
import com.atlassian.jira.user.ApplicationUser;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import kong.unirest.HttpResponse;
import kong.unirest.UnirestException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.commons.SentryClient;
import ru.mail.jira.plugins.myteam.bot.events.ChatMessageEvent;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.myteam.MyteamApiClient;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.*;
import ru.mail.jira.plugins.myteam.myteam.dto.response.FileResponse;

@Slf4j
@Component
public class IssueTextConverter {

  private final UserData userData;
  private final MyteamApiClient myteamApiClient;
  private final AttachmentManager attachmentManager;

  public IssueTextConverter(
      UserData userData, MyteamApiClient myteamApiClient, AttachmentManager attachmentManager) {
    this.userData = userData;
    this.myteamApiClient = myteamApiClient;
    this.attachmentManager = attachmentManager;
  }

  public String convertToJiraCommentStyle(
      ChatMessageEvent event, ApplicationUser commentedUser, Issue commentedIssue) {
    List<Part> parts = event.getMessageParts();
    String message = Objects.requireNonNullElse(event.getMessage(), "");
    StringBuilder outPutStrings = new StringBuilder(message);
    if (parts != null) {
      parts.forEach(
          part -> {
            CommentaryParts currentPartClass = CommentaryParts.fromPartClass(part.getClass());
            if (currentPartClass == null) {
              return;
            }

            switch (currentPartClass) {
              case File:
                File file = (File) part;
                try {
                  HttpResponse<FileResponse> response = myteamApiClient.getFile(file.getFileId());
                  FileResponse fileInfo = response.getBody();
                  try (InputStream attachment = myteamApiClient.loadUrlFile(fileInfo.getUrl())) {
                    boolean isUploaded =
                        uploadAttachment(attachment, fileInfo, commentedUser, commentedIssue);
                    if (isUploaded) {
                      outPutStrings.setLength(0);
                      outPutStrings.append(
                          buildAttachmentLink(
                              file.getFileId(), fileInfo.getType(), fileInfo.getFilename(), null));
                      outPutStrings.append(message);
                    }
                    if (fileInfo.getType().equals("image")) {
                      outPutStrings.append(
                          String.format(
                              "https://files-n.internal.myteam.mail.ru/get/%s\n",
                              file.getFileId()));
                    }
                    if (file.getCaption() != null) {
                      outPutStrings.append(String.format("%s\n", file.getCaption()));
                    }
                  }
                } catch (UnirestException | IOException | MyteamServerErrorException e) {
                  SentryClient.capture(e);
                  log.error(
                      "Unable to create attachment for comment on Issue {}",
                      commentedIssue.getKey(),
                      e);
                }
                break;
              case Mention:
                Mention mention = (Mention) part;
                ApplicationUser user = userData.getUserByMrimLogin(mention.getUserId());
                if (user != null) {
                  String replacedMention =
                      replaceMention(outPutStrings, mention.getUserId(), user.getName());
                  outPutStrings.setLength(0);
                  outPutStrings.append(replacedMention);
                } else {
                  String errorMessage =
                      String.format(
                          "Unable change Myteam mention to Jira's mention, because can't find user with id: %s",
                          mention.getUserId());
                  SentryClient.capture(errorMessage);
                  log.error(errorMessage);
                }
                break;
              default:
                break;
            }
          });
    }

    return ru.mail.jira.plugins.myteam.commons.Utils.removeAllEmojis(outPutStrings.toString());
  }

  public String convertToJiraDescriptionStyle(Part part, Issue issue) {
    List<Part> messageParts =
        part instanceof Reply
            ? ((Reply) part).getMessage().getParts()
            : ((Forward) part).getMessage().getParts();
    String text =
        part instanceof Reply
            ? ((Reply) part).getMessage().getText()
            : ((Forward) part).getMessage().getText();
    StringBuilder outPutStrings = new StringBuilder();
    if (messageParts != null) {
      messageParts.forEach(
          messagePart -> {
            CommentaryParts currentPartClass =
                CommentaryParts.fromPartClass(messagePart.getClass());
            if (currentPartClass == null) {
              return;
            }

            switch (currentPartClass) {
              case File:
                File file = (File) messagePart;
                try {
                  HttpResponse<FileResponse> response = myteamApiClient.getFile(file.getFileId());
                  FileResponse fileInfo = response.getBody();
                  try (InputStream attachment = myteamApiClient.loadUrlFile(fileInfo.getUrl())) {
                    boolean isUploaded =
                        uploadAttachment(attachment, fileInfo, issue.getReporterUser(), issue);
                    if (isUploaded) {
                      outPutStrings.append(
                          buildAttachmentLink(
                              file.getFileId(), fileInfo.getType(), fileInfo.getFilename(), text));
                    } else {
                      outPutStrings.append(text);
                    }
                  }
                } catch (UnirestException | IOException | MyteamServerErrorException e) {
                  SentryClient.capture(e);
                  log.error("Unable to add attachment to Issue {}", issue.getKey(), e);
                }
                break;
              case Mention:
                Mention mention = (Mention) messagePart;
                ApplicationUser user = userData.getUserByMrimLogin(mention.getUserId());
                if (user != null) {
                  outPutStrings.append(replaceMention(text, mention.getUserId(), user.getName()));
                } else {
                  outPutStrings.append(
                      replaceMention(text, mention.getUserId(), mention.getFirstName()));
                  String errorMessage =
                      String.format(
                          "Unable change Myteam mention to Jira's mention, because can't find user with id: %s",
                          mention.getUserId());
                  SentryClient.capture(errorMessage);
                  log.error(errorMessage);
                }
                break;
              default:
                break;
            }
          });
    } else {
      outPutStrings.append(text);
    }
    return outPutStrings.toString();
  }

  private boolean uploadAttachment(
      InputStream attachment, FileResponse fileInfo, ApplicationUser user, Issue issue) {
    try {
      TemporaryAttachmentId tmpAttachmentId =
          attachmentManager.createTemporaryAttachment(attachment, fileInfo.getSize());
      ConvertTemporaryAttachmentParams params =
          ConvertTemporaryAttachmentParams.builder()
              .setTemporaryAttachmentId(tmpAttachmentId)
              .setAuthor(user)
              .setIssue(issue)
              .setFilename(fileInfo.getFilename())
              .setContentType(fileInfo.getType())
              .setCreatedTime(DateTime.now())
              .setFileSize(fileInfo.getSize())
              .build();
      attachmentManager.convertTemporaryAttachment(params);
      return true;
    } catch (Exception e) {
      SentryClient.capture(e);
      log.error(e.getLocalizedMessage(), e);
      return false;
    }
  }

  private String replaceMention(CharSequence text, String userId, String userName) {
    return Pattern.compile("@\\[" + userId + "]").matcher(text).replaceAll("[~" + userName + "]");
  }

  private String buildAttachmentLink(String fileId, String fileType, String fileName, String text) {
    String linkFormat = fileType.equals("image") ? "!%s|thumbnail!\n" : "[^%s]\n";
    if (text == null) {
      return String.format(linkFormat, fileName);
    } else {
      Matcher matcher = Pattern.compile("(https?://.*/get/" + fileId + ").*").matcher(text);
      String myteamFileUrl = StringUtils.EMPTY;
      while (matcher.find()) {
        myteamFileUrl = matcher.group(1);
      }
      return String.format(
          "%s\n%s", matcher.replaceAll(String.format(linkFormat, fileName)), myteamFileUrl);
    }
  }
}