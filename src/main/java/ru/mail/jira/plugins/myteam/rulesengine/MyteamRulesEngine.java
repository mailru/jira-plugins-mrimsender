/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine;

import java.util.Collections;
import java.util.List;
import org.jeasy.rules.api.Fact;
import org.jeasy.rules.api.Facts;
import org.jeasy.rules.api.Rules;
import org.jeasy.rules.api.RulesEngine;
import org.jeasy.rules.core.DefaultRulesEngine;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.myteam.myteam.dto.ChatType;
import ru.mail.jira.plugins.myteam.protocol.events.ChatMessageEvent;
import ru.mail.jira.plugins.myteam.protocol.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.ButtonClickEvent;

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

  public static Facts formCommandFacts(String command, ButtonClickEvent event) {
    return formCommandFacts(command, event, Collections.emptyList());
  }

  public static Facts formCommandFacts(String command, ChatMessageEvent event) {
    return formCommandFacts(command, event, Collections.emptyList());
  }

  //  public static Facts formCommandFacts(String command, MyteamEvent event) {
  //    return formCommandFacts(command, event, Collections.emptyList());
  //  }

  public static Facts formCommandFacts(String command, MyteamEvent event, List<String> args) {
    Facts facts = new Facts();
    facts.put("command", command);
    facts.add(new Fact<>("event", event));
    facts.add(new Fact<>("args", args));
    facts.add(new Fact<>("isGroup", event.getChatType().equals(ChatType.GROUP)));
    return facts;
  }
}
