/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine;

import org.jeasy.rules.api.Facts;
import org.jeasy.rules.api.Rules;
import org.jeasy.rules.api.RulesEngine;
import org.jeasy.rules.core.DefaultRulesEngine;
import org.springframework.stereotype.Component;

@Component
public class MyTeamRulesEngine {

  private final Rules rules;
  private final RulesEngine rulesEngine;

  public MyTeamRulesEngine() {
    rulesEngine = new DefaultRulesEngine();
    rules = new Rules();
  }

  public void registerRule(Object rule) {
    rules.register(rule);
  }

  public void fire(Facts facts) {
    rulesEngine.fire(rules, facts);
  }
}
