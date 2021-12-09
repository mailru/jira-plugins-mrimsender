/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.service;

import org.jeasy.rules.api.Fact;
import org.jeasy.rules.api.Facts;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.myteam.myteam.dto.ChatType;
import ru.mail.jira.plugins.myteam.protocol.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.rulesengine.core.MyteamRulesEngine;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.rulesengine.rules.buttons.NextPageRule;
import ru.mail.jira.plugins.myteam.rulesengine.rules.buttons.PrevPageRule;
import ru.mail.jira.plugins.myteam.rulesengine.rules.buttons.SearchIssueByJqlInputRule;
import ru.mail.jira.plugins.myteam.rulesengine.rules.buttons.SearchIssueByKeyInputRule;
import ru.mail.jira.plugins.myteam.rulesengine.rules.commands.*;
import ru.mail.jira.plugins.myteam.rulesengine.rules.service.DefaultMessageRule;
import ru.mail.jira.plugins.myteam.rulesengine.rules.service.SearchByJqlIssuesRule;
import ru.mail.jira.plugins.myteam.rulesengine.rules.state.issuesearch.IssueKeyInputRule;
import ru.mail.jira.plugins.myteam.rulesengine.rules.state.jqlsearch.JqlInputRule;
import ru.mail.jira.plugins.myteam.rulesengine.states.BotState;

@Component
@Scope("chatService")
public class RulesEngineImpl implements RulesEngine, InitializingBean {

  private final MyteamRulesEngine commandsRuleEngine;
  private final MyteamRulesEngine errorsRuleEngine;
  private final MyteamRulesEngine stateActionsRuleEngine;

  private final UserChatService userChatService;
  private final IssueService issueService;

  public RulesEngineImpl(UserChatService userChatService, IssueService issueService) {
    this.userChatService = userChatService;
    this.issueService = issueService;
    this.commandsRuleEngine = new MyteamRulesEngine();
    this.errorsRuleEngine = new MyteamRulesEngine();
    this.stateActionsRuleEngine = new MyteamRulesEngine();
  }

  @Override
  public void afterPropertiesSet() {
    // Defaults
    stateActionsRuleEngine.registerRule(new DefaultMessageRule(userChatService, this));

    // Buttons
    commandsRuleEngine.registerRule(new SearchIssueByJqlInputRule(userChatService, this));
    commandsRuleEngine.registerRule(new SearchIssueByKeyInputRule(userChatService, this));
    commandsRuleEngine.registerRule(new NextPageRule(userChatService, this));
    commandsRuleEngine.registerRule(new PrevPageRule(userChatService, this));

    // Commands
    commandsRuleEngine.registerRule(new HelpCommandRule(userChatService, this));
    commandsRuleEngine.registerRule(new MenuCommandRule(userChatService, this));
    commandsRuleEngine.registerRule(new WatchingIssuesCommandRule(userChatService, this));
    commandsRuleEngine.registerRule(new AssignedIssuesCommandRule(userChatService, this));
    commandsRuleEngine.registerRule(new CreatedIssuesCommandRule(userChatService, this));
    commandsRuleEngine.registerRule(new ViewIssueCommandRule(userChatService, this, issueService));
    commandsRuleEngine.registerRule(new WatchIssueCommandRule(userChatService, this, issueService));
    commandsRuleEngine.registerRule(
        new UnwatchIssueCommandRule(userChatService, this, issueService));

    // Services
    commandsRuleEngine.registerRule(new SearchByJqlIssuesRule(userChatService, this, issueService));

    // States

    stateActionsRuleEngine.registerRule(new JqlInputRule(userChatService, this));

    stateActionsRuleEngine.registerRule(new IssueKeyInputRule(userChatService, this));
  }

  @Override
  public void fireCommand(RuleType command, MyteamEvent event) {
    fireCommand(command.getName(), event, "");
  }

  @Override
  public void fireCommand(RuleType command, MyteamEvent event, String args) {
    fireCommand(command.getName(), event, args);
  }

  @Override
  public void fireCommand(String command, MyteamEvent event, String args) {
    Facts facts = formBasicFacts(event, args);
    facts.put("command", command);
    commandsRuleEngine.fire(facts);
  }

  @Override
  public void fireStateAction(BotState state, MyteamEvent event, String args) {
    Facts facts = formBasicFacts(event, args);
    facts.add(new Fact<>("state", state));
    stateActionsRuleEngine.fire(facts);
  }

  @Override
  public void fireError(Facts facts) {
    errorsRuleEngine.fire(facts);
  }

  private Facts formBasicFacts(MyteamEvent event, String args) {
    Facts facts = new Facts();
    facts.put("args", args);
    facts.add(new Fact<>("event", event));
    facts.add(new Fact<>("isGroup", event.getChatType().equals(ChatType.GROUP)));
    return facts;
  }
}
