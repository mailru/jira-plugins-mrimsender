/* (C)2021 */
package ru.mail.jira.plugins.myteam.service.impl;

import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.sal.api.lifecycle.LifecycleAware;
import org.jeasy.rules.api.Fact;
import org.jeasy.rules.api.Facts;
import org.jeasy.rules.api.RulesEngineParameters;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.myteam.bot.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.core.RulesEngine;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.ErrorRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.buttons.*;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.commands.HelpCommandRule;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.commands.MenuCommandRule;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.commands.admin.IssueCreationSettingsCommand;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.commands.issue.*;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.commands.issue.editing.IssueTransitionRule;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.commands.issue.editing.IssueTransitionSelectRule;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.errors.IssueNoPermissionErrorRule;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.errors.IssueNotFoundErrorRule;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.service.CreateIssueByReplyRule;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.service.DefaultMessageRule;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.service.SearchByJqlIssuesRule;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.state.assignissue.AssignIssueInputRule;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.state.issuecomment.IssueCommentInputRule;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.state.issuecreation.*;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.state.issuesearch.IssueKeyInputRule;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.state.jqlsearch.JqlInputRule;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.base.BotState;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.base.EmptyState;
import ru.mail.jira.plugins.myteam.component.IssueTextConverter;
import ru.mail.jira.plugins.myteam.myteam.dto.ChatType;
import ru.mail.jira.plugins.myteam.service.IssueCreationService;
import ru.mail.jira.plugins.myteam.service.IssueCreationSettingsService;
import ru.mail.jira.plugins.myteam.service.IssueService;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Component
@ExportAsService(LifecycleAware.class)
public class RulesEngineImpl
    implements ru.mail.jira.plugins.myteam.service.RulesEngine, LifecycleAware {

  private final RulesEngine commandsRuleEngine;
  private final RulesEngine errorsRuleEngine;
  private final RulesEngine stateActionsRuleEngine;

  private final IssueCreationService issueCreationService;
  private final UserChatService userChatService;
  private final IssueService issueService;
  private final IssueCreationSettingsService issueCreationSettingsService;
  private final IssueTextConverter issueTextConverter;

  public RulesEngineImpl(
      IssueCreationService issueCreationService,
      UserChatService userChatService,
      IssueService issueService,
      IssueCreationSettingsService issueCreationSettingsService,
      IssueTextConverter issueTextConverter) {
    this.issueCreationService = issueCreationService;
    this.userChatService = userChatService;
    this.issueService = issueService;
    this.issueCreationSettingsService = issueCreationSettingsService;
    this.issueTextConverter = issueTextConverter;

    RulesEngineParameters engineParams =
        new RulesEngineParameters(
            true, false, false, RulesEngineParameters.DEFAULT_RULE_PRIORITY_THRESHOLD);
    this.commandsRuleEngine = new RulesEngine(engineParams, userChatService);
    this.errorsRuleEngine = new RulesEngine(engineParams, userChatService);
    this.stateActionsRuleEngine = new RulesEngine(engineParams, userChatService);
  }

  @Override
  public void onStart() {
    // Defaults
    stateActionsRuleEngine.registerRule(new DefaultMessageRule(userChatService, this));

    // Buttons
    commandsRuleEngine.registerRule(new SearchIssueByJqlInputRule(userChatService, this));
    commandsRuleEngine.registerRule(new SearchIssueByKeyInputRule(userChatService, this));
    commandsRuleEngine.registerRule(new NextPageRule(userChatService, this));
    commandsRuleEngine.registerRule(new PrevPageRule(userChatService, this));
    commandsRuleEngine.registerRule(new RevertRule(userChatService, this));
    commandsRuleEngine.registerRule(new CancelRule(userChatService, this));
    commandsRuleEngine.registerRule(new CommentIssueRule(userChatService, this));
    commandsRuleEngine.registerRule(
        new CreateIssueRule(userChatService, this, issueService, issueCreationService));
    commandsRuleEngine.registerRule(new ViewCommentsRule(userChatService, this, issueService));

    // Admin Group Commands

    commandsRuleEngine.registerRule(
        new IssueCreationSettingsCommand(userChatService, this, issueService));

    // Commands
    commandsRuleEngine.registerRule(new HelpCommandRule(userChatService, this));
    commandsRuleEngine.registerRule(new MenuCommandRule(userChatService, this));
    commandsRuleEngine.registerRule(new WatchingIssuesCommandRule(userChatService, this));
    commandsRuleEngine.registerRule(new AssignedIssuesCommandRule(userChatService, this));
    commandsRuleEngine.registerRule(new CreatedIssuesCommandRule(userChatService, this));
    commandsRuleEngine.registerRule(new LinkIssueWithChatCommandRule(userChatService, this));
    commandsRuleEngine.registerRule(new AssignIssueCommandRule(userChatService, this));
    commandsRuleEngine.registerRule(new ViewIssueCommandRule(userChatService, this, issueService));
    commandsRuleEngine.registerRule(new WatchIssueCommandRule(userChatService, this, issueService));
    commandsRuleEngine.registerRule(new IssueTransitionRule(userChatService, this, issueService));
    commandsRuleEngine.registerRule(
        new IssueTransitionSelectRule(userChatService, this, issueService));
    commandsRuleEngine.registerRule(
        new UnwatchIssueCommandRule(userChatService, this, issueService));
    commandsRuleEngine.registerRule(
        new FieldValueEditRule(userChatService, this, issueCreationService));
    commandsRuleEngine.registerRule(
        new FieldValueSelectRule(userChatService, this, issueCreationService));

    // Service
    commandsRuleEngine.registerRule(new SearchByJqlIssuesRule(userChatService, this, issueService));
    commandsRuleEngine.registerRule(
        new CreateIssueByReplyRule(
            userChatService,
            this,
            issueCreationSettingsService,
            issueCreationService,
            issueService,
            issueTextConverter));

    // States
    stateActionsRuleEngine.registerRule(new JqlInputRule(userChatService, this));
    stateActionsRuleEngine.registerRule(
        new ProjectKeyInputRule(userChatService, this, issueService));
    stateActionsRuleEngine.registerRule(
        new IssueCommentInputRule(userChatService, this, issueService));
    stateActionsRuleEngine.registerRule(new FieldInputRule(userChatService, this));
    stateActionsRuleEngine.registerRule(new IssueKeyInputRule(userChatService, this));
    stateActionsRuleEngine.registerRule(
        new AssignIssueInputRule(userChatService, this, issueService));

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
  public void onStop() {}

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

    facts.put("args", args == null ? "" : args);
    facts.add(new Fact<>("event", event));
    facts.add(new Fact<>("state", state == null ? new EmptyState() : state));
    facts.add(new Fact<>("prevState", prevState == null ? new EmptyState() : prevState));
    facts.add(new Fact<>("isGroup", event.getChatType() == ChatType.GROUP));
    return facts;
  }
}
