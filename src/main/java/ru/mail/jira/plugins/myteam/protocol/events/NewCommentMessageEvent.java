/* (C)2020 */
package ru.mail.jira.plugins.myteam.protocol.events;

import lombok.Getter;

@Getter
public class NewCommentMessageEvent {
  private final String userId;
  private final String chatId;
  private final String message;
  private final String commentingIssueKey;

  public NewCommentMessageEvent(ChatMessageEvent chatMessageEvent, String commentingIssueKey) {
    this.userId = chatMessageEvent.getUerId();
    this.chatId = chatMessageEvent.getChatId();
    this.message = chatMessageEvent.getMessage();
    this.commentingIssueKey = commentingIssueKey;
  }
}
