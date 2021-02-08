/* (C)2020 */
package ru.mail.jira.plugins.myteam.protocol.events;

import java.util.List;
import lombok.Getter;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.Part;

@Getter
public class NewCommentMessageEvent {
  private final String userId;
  private final String chatId;
  private final String message;
  private final String commentingIssueKey;
  private final List<Part> messageParts;

  public NewCommentMessageEvent(ChatMessageEvent chatMessageEvent, String commentingIssueKey) {
    this.userId = chatMessageEvent.getUserId();
    this.chatId = chatMessageEvent.getChatId();
    this.messageParts = chatMessageEvent.getMessageParts();
    this.message = chatMessageEvent.getMessage();
    this.commentingIssueKey = commentingIssueKey;
  }
}
