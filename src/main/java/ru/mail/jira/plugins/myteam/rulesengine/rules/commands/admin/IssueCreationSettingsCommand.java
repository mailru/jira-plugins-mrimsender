package ru.mail.jira.plugins.myteam.rulesengine.rules.commands.admin;

import com.atlassian.crowd.exception.UserNotFoundException;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.protocol.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.rulesengine.models.exceptions.AdminRulesRequiredException;
import ru.mail.jira.plugins.myteam.rulesengine.rules.GroupAdminRule;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.CommandRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.rulesengine.service.RulesEngine;
import ru.mail.jira.plugins.myteam.rulesengine.service.UserChatService;


@Rule(name = "/configure_task", description = "Configures issues creation in chat")
public class IssueCreationSettingsCommand extends GroupAdminRule {
  static final RuleType NAME = CommandRuleType.IssueCreationSettings;

  public IssueCreationSettingsCommand(UserChatService userChatService, RulesEngine rulesEngine) {
    super(userChatService, rulesEngine);
  }

  @Condition
  public boolean isValid(@Fact("command") String command) throws AdminRulesRequiredException {
    checkAdminRules();
    return NAME.equalsName(command);
  }

  @Action
  public void execute(@Fact("event") MyteamEvent event) throws UserNotFoundException {
//    rulesEngine.fireCommand(
//        CommandRuleType.SearchByJql,
//        event,
//        "watcher = currentUser() AND resolution = Unresolved ORDER BY updated");
  }
}

