/* (C)2020 */
package ru.mail.jira.plugins.myteam.protocol.events;

import lombok.Getter;
import ru.mail.jira.plugins.myteam.myteam.dto.ChatType;
import ru.mail.jira.plugins.myteam.myteam.dto.events.CallbackQueryEvent;

@Getter
public class ButtonClickEvent extends MyteamEvent {
  private final String queryId;
  private final long msgId;
  private final String callbackData;

  public ButtonClickEvent(CallbackQueryEvent callbackQueryEvent) {
    super(
        callbackQueryEvent.getMessage().getChat().getChatId(),
        callbackQueryEvent.getFrom().getUserId(),
        ChatType.fromApiValue(callbackQueryEvent.getMessage().getChat().getType()));
    this.queryId = callbackQueryEvent.getQueryId();
    this.msgId = callbackQueryEvent.getMessage().getMsgId();
    this.callbackData = callbackQueryEvent.getCallbackData();
  }
}
