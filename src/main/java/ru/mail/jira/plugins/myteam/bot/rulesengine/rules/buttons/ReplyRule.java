package ru.mail.jira.plugins.myteam.bot.rulesengine.rules.buttons;

import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.accessrequest.service.AccessRequestService;
import ru.mail.jira.plugins.myteam.bot.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.ButtonRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.CommandRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.BaseRule;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.ReplyAccessState;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.base.BotState;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.base.PageableState;
import ru.mail.jira.plugins.myteam.service.RulesEngine;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Rule(name = "reply", description = "reply user access")
public class ReplyRule extends BaseRule {

    static final ButtonRuleType NAME = ButtonRuleType.AccessReply;

    public ReplyRule(UserChatService userChatService, RulesEngine rulesEngine) {
        super(userChatService, rulesEngine);
    }

    @Condition
    public boolean isValid(@Fact("command") String command) {
        return NAME.equalsName(command);
    }

    @Action
    public void execute(@Fact("event") MyteamEvent event, @Fact("state") BotState state) {
        ReplyAccessState replyAccessState = null;
        if (state instanceof ReplyAccessState) {
            replyAccessState = (ReplyAccessState) state;
        }
        if (replyAccessState == null) {
            return;
        }

        AccessRequestService accessRequestService = replyAccessState.getAccessRequestService();

//        rulesEngine.fireCommand(CommandRuleType., event);
    }
}
