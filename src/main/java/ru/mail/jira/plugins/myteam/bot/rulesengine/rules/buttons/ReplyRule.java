/* (C)2023 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.rules.buttons;

import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import java.io.IOException;
import java.util.List;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.accessrequest.controller.dto.AccessRequestConfigurationDto;
import ru.mail.jira.plugins.myteam.accessrequest.service.AccessRequestService;
import ru.mail.jira.plugins.myteam.bot.events.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.ButtonRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.BaseRule;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.service.RulesEngine;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Rule(name = "reply", description = "reply user access")
public class ReplyRule extends BaseRule {

  public static final String COMMAND_ALLOW = "allow";
  public static final String COMMAND_FORBID = "forbid";
  public static final String COMMAND_DEV = "dev";
  static final ButtonRuleType NAME = ButtonRuleType.AccessReply;
  private final AccessRequestService accessRequestService;
  private final ProjectManager projectManager;

  public ReplyRule(
      UserChatService userChatService,
      RulesEngine rulesEngine,
      AccessRequestService accessRequestService,
      ProjectManager projectManager) {
    super(userChatService, rulesEngine);
    this.accessRequestService = accessRequestService;
    this.projectManager = projectManager;
  }

  @Condition
  public boolean isValid(@Fact("command") String command) {
    return NAME.equalsName(command);
  }

  @Action
  public void execute(@Fact("event") ButtonClickEvent event, @Fact("args") String args)
      throws MyteamServerErrorException, IOException {
    List<String> parsedArgs = RuleType.parseArgs(args);
    String command = parsedArgs.get(0);
    String projectKey = parsedArgs.get(1);

    Project project = projectManager.getProjectByCurrentKey(projectKey);
    AccessRequestConfigurationDto accessRequestConfigurationDto =
        accessRequestService.getAccessRequestConfiguration(project);

    if (accessRequestConfigurationDto != null) {
      //      accessRequestConfigurationDto.getUsers().stream()
      //              .map(dto -> user)
    }

    //    accessRequestService.getAccessRequestConfiguration(event.)
    //    projectManager.getProjectByCurrentKey()
    //    accessRequestService.getAccessRequestConfiguration()
    if (command.equals(COMMAND_ALLOW)) {
      System.out.println(projectKey);
      System.out.println(accessRequestService);
      System.out.println(projectManager);

    } else if (command.equals(COMMAND_FORBID)) {
      System.out.println(projectKey);
    } else if (command.equals(COMMAND_DEV)) {
      userChatService.sendMessageText(event.getChatId(), Long.toString(event.getMsgId()));
      userChatService.answerCallbackQuery(event.getQueryId());
    }
  }
}
