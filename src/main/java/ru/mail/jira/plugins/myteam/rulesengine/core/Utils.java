/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.core;

import com.atlassian.jira.issue.AttachmentManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.attachment.ConvertTemporaryAttachmentParams;
import com.atlassian.jira.issue.attachment.TemporaryAttachmentId;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.user.ApplicationUser;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.regex.Pattern;
import kong.unirest.HttpResponse;
import kong.unirest.UnirestException;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.myteam.configuration.UserData;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.myteam.MyteamApiClient;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.CommentaryParts;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.File;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.Mention;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.Part;
import ru.mail.jira.plugins.myteam.myteam.dto.response.FileResponse;
import ru.mail.jira.plugins.myteam.protocol.events.ChatMessageEvent;

@Slf4j
@Component
public class Utils {

  private final UserData userData;
  private final MyteamApiClient myteamApiClient;
  private final AttachmentManager attachmentManager;

  public Utils(
      UserData userData, MyteamApiClient myteamApiClient, AttachmentManager attachmentManager) {
    this.userData = userData;
    this.myteamApiClient = myteamApiClient;
    this.attachmentManager = attachmentManager;
  }

  public String convertToJiraCommentStyle(
      ChatMessageEvent event, ApplicationUser commentedUser, Issue commentedIssue) {
    List<Part> parts = event.getMessageParts();
    if (parts == null || parts.size() == 0) return event.getMessage();
    else {
      StringBuilder outPutStrings = new StringBuilder(event.getMessage());
      parts.forEach(
          part -> {
            CommentaryParts currentPartClass =
                CommentaryParts.valueOf(part.getClass().getSimpleName());
            switch (currentPartClass) {
              case File:
                File file = (File) part;
                try {
                  HttpResponse<FileResponse> response = myteamApiClient.getFile(file.getFileId());
                  FileResponse fileInfo = response.getBody();
                  try (InputStream attachment =
                      ru.mail.jira.plugins.myteam.commons.Utils.loadUrlFile(fileInfo.getUrl())) {
                    TemporaryAttachmentId tmpAttachmentId =
                        attachmentManager.createTemporaryAttachment(attachment, fileInfo.getSize());
                    ConvertTemporaryAttachmentParams params =
                        ConvertTemporaryAttachmentParams.builder()
                            .setTemporaryAttachmentId(tmpAttachmentId)
                            .setAuthor(commentedUser)
                            .setIssue(commentedIssue)
                            .setFilename(fileInfo.getFilename())
                            .setContentType(fileInfo.getType())
                            .setCreatedTime(DateTime.now())
                            .setFileSize(fileInfo.getSize())
                            .build();
                    attachmentManager.convertTemporaryAttachment(params);
                    if (fileInfo.getType().equals("image")) {
                      outPutStrings.append(String.format("!%s!\n", fileInfo.getFilename()));
                    } else {
                      outPutStrings.append(String.format("[%s]\n", fileInfo.getFilename()));
                    }
                    if (file.getCaption() != null) {
                      outPutStrings.append(String.format("%s\n", file.getCaption()));
                    }
                  }
                } catch (UnirestException | IOException | MyteamServerErrorException e) {
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
                  String temp =
                      Pattern.compile("@\\[" + mention.getUserId() + "]")
                          .matcher(outPutStrings)
                          .replaceAll("[~" + user.getName() + "]");
                  outPutStrings.setLength(0);
                  outPutStrings.append(temp);
                } else {
                  log.error(
                      "Unable change Myteam mention to Jira's mention, because Can't find user with id:{}",
                      mention.getUserId());
                }
                break;
              default:
                break;
            }
          });
      return outPutStrings.toString();
    }
  }

  public static boolean isArrayLikeField(Field field) {
    switch (field.getId()) {
      case IssueFieldConstants.FIX_FOR_VERSIONS:
      case IssueFieldConstants.COMPONENTS:
      case IssueFieldConstants.AFFECTED_VERSIONS:
      case IssueFieldConstants.ISSUE_LINKS:
      case IssueFieldConstants.LABELS:
      case IssueFieldConstants.VOTES:
        // never shown on issue creation screen
      case IssueFieldConstants.WATCHES:
        return true;
    }
    return false;
  }
}
