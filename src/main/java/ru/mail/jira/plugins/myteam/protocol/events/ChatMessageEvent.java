/* (C)2020 */
package ru.mail.jira.plugins.myteam.protocol.events;

import java.util.List;
import lombok.Getter;
import ru.mail.jira.plugins.myteam.myteam.dto.ChatType;
import ru.mail.jira.plugins.myteam.myteam.dto.User;
import ru.mail.jira.plugins.myteam.myteam.dto.events.NewMessageEvent;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.Forward;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.Part;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.Reply;

@Getter
public class ChatMessageEvent extends MyteamEvent {
  private final String message;
  private final Long messageId;
  private final User from;
  private final List<Part> messageParts;
  private final boolean hasForwards;
  private final boolean hasReply;

  public ChatMessageEvent(NewMessageEvent newMessageEvent) {
    super(
        newMessageEvent.getChat().getChatId(),
        newMessageEvent.getFrom().getUserId(),
        ChatType.fromApiValue(newMessageEvent.getChat().getType()));
    from = newMessageEvent.getFrom();
    message = newMessageEvent.getText();
    messageId = newMessageEvent.getMsgId();
    messageParts = newMessageEvent.getParts();
    if (messageParts != null) {
      hasForwards = messageParts.stream().anyMatch(part -> part instanceof Forward);
      hasReply = messageParts.stream().anyMatch(part -> part instanceof Reply);
    } else {
      hasForwards = false;
      hasReply = false;
    }
  }
}
