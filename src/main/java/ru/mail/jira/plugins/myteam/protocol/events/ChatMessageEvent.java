/* (C)2020 */
package ru.mail.jira.plugins.myteam.protocol.events;

import java.util.List;
import lombok.Getter;
import ru.mail.jira.plugins.myteam.myteam.dto.ChatType;
import ru.mail.jira.plugins.myteam.myteam.dto.events.NewMessageEvent;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.Forward;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.Part;

@Getter
public class ChatMessageEvent implements Event {
  private final String chatId;
  private final String uerId;
  private final String message;
  private final ChatType chatType;
  private final List<Part> messageParts;
  private final boolean hasForwards;

  public ChatMessageEvent(NewMessageEvent newMessageEvent) {
    chatId = newMessageEvent.getChat().getChatId();
    uerId = newMessageEvent.getFrom().getUserId();
    message = newMessageEvent.getText();
    chatType = ChatType.fromApiValue(newMessageEvent.getChat().getType());
    messageParts = newMessageEvent.getParts();
    if (messageParts != null)
      hasForwards = messageParts.stream().anyMatch(part -> part instanceof Forward);
    else hasForwards = false;
  }
}
