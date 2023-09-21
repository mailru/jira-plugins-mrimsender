/* (C)2020 */
package ru.mail.jira.plugins.myteam.bot.events;

import java.util.List;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;
import ru.mail.jira.plugins.myteam.myteam.dto.ChatType;
import ru.mail.jira.plugins.myteam.myteam.dto.EmptyFormatMetadata;
import ru.mail.jira.plugins.myteam.myteam.dto.TextFormatMetadata;
import ru.mail.jira.plugins.myteam.myteam.dto.User;
import ru.mail.jira.plugins.myteam.myteam.dto.events.NewMessageEvent;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.Forward;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.Mention;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.Part;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.Reply;

@Getter
@ToString
public class ChatMessageEvent extends MyteamEvent {
  private final String message;
  private final Long messageId;
  private final User from;
  @Nullable private final List<Part> messageParts;
  private final boolean hasForwards;
  private final boolean hasReply;
  private final boolean hasMentions;
  private final TextFormatMetadata format;

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
      hasMentions = messageParts.stream().anyMatch(part -> part instanceof Mention);
    } else {
      hasForwards = false;
      hasReply = false;
      hasMentions = false;
    }
    format =
        newMessageEvent.getFormat() != null
            ? newMessageEvent.getFormat()
            : new EmptyFormatMetadata();
  }

  public ChatMessageEvent(ChatMessageEvent chatMessageEvent) {
    from = chatMessageEvent.getFrom();
    message = chatMessageEvent.getMessage();
    messageId = chatMessageEvent.getMessageId();
    messageParts = chatMessageEvent.getMessageParts();
    if (messageParts != null) {
      hasForwards = messageParts.stream().anyMatch(part -> part instanceof Forward);
      hasReply = messageParts.stream().anyMatch(part -> part instanceof Reply);
      hasMentions = messageParts.stream().anyMatch(part -> part instanceof Mention);
    } else {
      hasForwards = false;
      hasReply = false;
      hasMentions = false;
    }
    format =
        chatMessageEvent.getFormat() != null
            ? chatMessageEvent.getFormat()
            : new EmptyFormatMetadata();
  }
}
