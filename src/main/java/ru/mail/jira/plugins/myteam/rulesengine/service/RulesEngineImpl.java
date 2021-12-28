/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.service;

import org.jeasy.rules.api.Fact;
import org.jeasy.rules.api.Facts;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.myteam.myteam.dto.ChatType;
import ru.mail.jira.plugins.myteam.protocol.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.rulesengine.core.MyteamRulesEngine;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.ErrorRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.rulesengine.rules.buttons.*;
import ru.mail.jira.plugins.myteam.rulesengine.rules.commands.*;
import ru.mail.jira.plugins.myteam.rulesengine.rules.errors.IssueNoPermissionErrorRule;
import ru.mail.jira.plugins.myteam.rulesengine.rules.errors.IssueNotFoundErrorRule;
import ru.mail.jira.plugins.myteam.rulesengine.rules.service.DefaultMessageRule;
import ru.mail.jira.plugins.myteam.rulesengine.rules.service.SearchByJqlIssuesRule;
import ru.mail.jira.plugins.myteam.rulesengine.rules.state.issuecomment.IssueCommentInputRule;
import ru.mail.jira.plugins.myteam.rulesengine.rules.state.issuecreation.*;
import ru.mail.jira.plugins.myteam.rulesengine.rules.state.issuesearch.IssueKeyInputRule;
import ru.mail.jira.plugins.myteam.rulesengine.rules.state.jqlsearch.JqlInputRule;
import ru.mail.jira.plugins.myteam.rulesengine.states.base.BotState;
import ru.mail.jira.plugins.myteam.rulesengine.states.base.EmptyState;

@Component
public class RulesEngineImpl implements RulesEngine, InitializingBean {

  private final MyteamRulesEngine commandsRuleEngine;
  private final MyteamRulesEngine errorsRuleEngine;
  private final MyteamRulesEngine stateActionsRuleEngine;

  private final IssueCreationService issueCreationService;
  private final UserChatService userChatService;
  private final IssueService issueService;

  public RulesEngineImpl(
      IssueCreationService issueCreationService,
      UserChatService userChatService,
      IssueService issueService) {
    this.issueCreationService = issueCreationService;
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
    commandsRuleEngine.registerRule(new CancelRule(userChatService, this));
    commandsRuleEngine.registerRule(new CommentIssueRule(userChatService, this));
    commandsRuleEngine.registerRule(
        new CreateIssueRule(userChatService, this, issueService, issueCreationService));
    commandsRuleEngine.registerRule(new ViewCommentsRule(userChatService, this, issueService));

    // Commands
    commandsRuleEngine.registerRule(new HelpCommandRule(userChatService, this));
    commandsRuleEngine.registerRule(new MenuCommandRule(userChatService, this));
    commandsRuleEngine.registerRule(new WatchingIssuesCommandRule(userChatService, this));
    commandsRuleEngine.registerRule(new AssignedIssuesCommandRule(userChatService, this));
    commandsRuleEngine.registerRule(new CreatedIssuesCommandRule(userChatService, this));
    commandsRuleEngine.registerRule(new LinkIssueWithChatCommandRule(userChatService, this));
    commandsRuleEngine.registerRule(new ViewIssueCommandRule(userChatService, this, issueService));
    commandsRuleEngine.registerRule(new WatchIssueCommandRule(userChatService, this, issueService));
    commandsRuleEngine.registerRule(
        new UnwatchIssueCommandRule(userChatService, this, issueService));
    commandsRuleEngine.registerRule(
        new FieldValueEditRule(userChatService, this, issueCreationService));
    commandsRuleEngine.registerRule(
        new FieldValueSelectRule(userChatService, this, issueCreationService));

    // Service
    commandsRuleEngine.registerRule(new SearchByJqlIssuesRule(userChatService, this, issueService));

    // States
    stateActionsRuleEngine.registerRule(new JqlInputRule(userChatService, this));
    stateActionsRuleEngine.registerRule(
        new ProjectKeyInputRule(userChatService, this, issueService));
    stateActionsRuleEngine.registerRule(
        new IssueCommentInputRule(userChatService, this, issueService));
    stateActionsRuleEngine.registerRule(new FieldInputRule(userChatService, this));
    stateActionsRuleEngine.registerRule(new IssueKeyInputRule(userChatService, this));

    commandsRuleEngine.registerRule(
        new IssueTypeSelectButtonRule(userChatService, this, issueService));
    commandsRuleEngine.registerRule(
        new ShowIssueCreationProgressRule(userChatService, this, issueCreationService));
    commandsRuleEngine.registerRule(
        new ConfirmIssueCreationRule(userChatService, this, issueCreationService));
    commandsRuleEngine.registerRule(
        new AddAdditionalFieldsRule(userChatService, this, issueCreationService));
    commandsRuleEngine.registerRule(
        new SelectAdditionalFieldRule(userChatService, this, issueCreationService));

    // Errors
    errorsRuleEngine.registerRule(new IssueNotFoundErrorRule(userChatService, this));
    errorsRuleEngine.registerRule(new IssueNoPermissionErrorRule(userChatService, this));
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
  public void fireStateAction(MyteamEvent event, String args) {
    Facts facts = formBasicFacts(event, args);
    stateActionsRuleEngine.fire(facts);
  }

  @Override
  public void fireError(ErrorRuleType errorType, MyteamEvent event, Exception e) {
    Facts facts = new Facts();
    facts.add(new Fact<>("error", errorType));
    facts.add(new Fact<>("event", event));
    facts.add(new Fact<>("exception", e));
    errorsRuleEngine.fire(facts);
  }

  private Facts formBasicFacts(MyteamEvent event, String args) {
    Facts facts = new Facts();

    BotState state = userChatService.getState(event.getChatId());
    BotState prevState = userChatService.getPrevState(event.getChatId());

    facts.put("args", args);
    facts.add(new Fact<>("event", event));
    facts.add(new Fact<>("state", state == null ? new EmptyState() : state));
    facts.add(new Fact<>("prevState", prevState == null ? new EmptyState() : prevState));
    facts.add(new Fact<>("isGroup", event.getChatType().equals(ChatType.GROUP)));
    return facts;
  }
}
