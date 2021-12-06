/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine;

import org.jeasy.rules.api.Fact;
import org.jeasy.rules.api.Facts;
import org.jeasy.rules.api.Rules;
import org.jeasy.rules.api.RulesEngine;
import org.jeasy.rules.core.DefaultRulesEngine;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.myteam.myteam.dto.ChatType;
import ru.mail.jira.plugins.myteam.protocol.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.rulesengine.models.RuleEventType;

@Component
public class MyteamRulesEngine {

  private final Rules rules;
  private final RulesEngine rulesEngine;

  public MyteamRulesEngine() {
    rulesEngine = new DefaultRulesEngine();
    rules = new Rules();
  }

  public void registerRule(Object rule) {
    rules.register(rule);
  }

  public void fire(Facts facts) {
    rulesEngine.fire(rules, facts);
  }

  public static Facts formCommandFacts(RuleEventType command, MyteamEvent event) {
    return formCommandFacts(command.getName(), event, "");
  }

  public static Facts formCommandFacts(RuleEventType command, MyteamEvent event, String args) {
    return formCommandFacts(command.getName(), event, args);
  }

  public static Facts formCommandFacts(String command, MyteamEvent event, String args) {
    Facts facts = formBasicsFacts(command, event);
    facts.add(new Fact<>("args", args));
    return facts;
  }

  public static Facts formBasicsFacts(String command, MyteamEvent event) {
    Facts facts = new Facts();
    facts.put("command", command);
    facts.add(new Fact<>("event", event));
    facts.add(new Fact<>("isGroup", event.getChatType().equals(ChatType.GROUP)));
    return facts;
  }

  public static Facts formBasicsFacts(RuleEventType command, MyteamEvent event) {
    return formBasicsFacts(command.getName(), event);
  }
}
