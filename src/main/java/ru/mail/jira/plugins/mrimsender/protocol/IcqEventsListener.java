package ru.mail.jira.plugins.mrimsender.protocol;

import com.google.common.eventbus.Subscribe;
import org.apache.commons.lang3.StringUtils;
import ru.mail.jira.plugins.mrimsender.icq.IcqApiClient;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.CallbackQueryEvent;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.NewMessageEvent;
import ru.mail.jira.plugins.mrimsender.protocol.commands.CancelCommand;
import ru.mail.jira.plugins.mrimsender.protocol.commands.Command;
import ru.mail.jira.plugins.mrimsender.protocol.commands.CommentCreatedCommand;
import ru.mail.jira.plugins.mrimsender.protocol.commands.CreateCommentCommand;
import ru.mail.jira.plugins.mrimsender.protocol.commands.ViewIssueCommand;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class IcqEventsListener {
    private final ConcurrentHashMap<String, String> chatsStateMap = new ConcurrentHashMap<>();
    private Map<String, Command> buttonCommandsMapping = new HashMap<>();

    private final CommentCreatedCommand commentCreatedCommand;
    private final IcqApiClient icqApiClient;
    public IcqEventsListener(CreateCommentCommand createCommentCommand, ViewIssueCommand viewIssueCommand, CancelCommand cancelCommand, CommentCreatedCommand commentCreatedCommand, IcqApiClient icqApiClient) {
        buttonCommandsMapping.put("view", viewIssueCommand);
        buttonCommandsMapping.put("comment", createCommentCommand);
        buttonCommandsMapping.put("cancel", cancelCommand);
        this.commentCreatedCommand = commentCreatedCommand;
        this.icqApiClient = icqApiClient;
    }

    @Subscribe
    public void handlerNewMessageEvent(NewMessageEvent newMessageEvent) throws Exception {
        String chatId = newMessageEvent.getChat().getChatId();
        if (chatsStateMap.containsKey(chatId)) {
            commentCreatedCommand.execute(newMessageEvent);
        } else {
            //todo показать кнопки с выбором таски
            //showDefaultMenu(newMessageEvent.getChat(), newMessageEvent.getFrom());
        }
    }

    @Subscribe
    public void handleCallbackQueryEvent(CallbackQueryEvent callbackQueryEvent) throws Exception {
        String callbackData = callbackQueryEvent.getCallbackData();
        String buttonPrefix = StringUtils.substringBefore(callbackData, "-");
        buttonCommandsMapping.get(buttonPrefix).execute(callbackQueryEvent);
    }

    @Subscribe
    public void handleJiraNotifyEvent(IcqEventsPublisher.JiraNotifyEvent jiraNotifyEvent) throws Exception{
        icqApiClient.sendMessageText(jiraNotifyEvent.getChatId(), jiraNotifyEvent.getMessage(), jiraNotifyEvent.getButtons());
    }
}
