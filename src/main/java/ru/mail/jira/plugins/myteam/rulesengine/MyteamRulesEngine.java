/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine;

import org.jeasy.rules.api.Facts;
import org.jeasy.rules.api.Rules;
import org.jeasy.rules.api.RulesEngine;
import org.jeasy.rules.api.RulesEngineParameters;
import org.jeasy.rules.core.DefaultRulesEngine;

public class MyteamRulesEngine {

  private final Rules rules;
  private final RulesEngine rulesEngine;

  public MyteamRulesEngine() {
    rulesEngine =
        new DefaultRulesEngine(
            new RulesEngineParameters(
                true, false, false, RulesEngineParameters.DEFAULT_RULE_PRIORITY_THRESHOLD));
    rules = new Rules();
  }

  public void registerRule(Object rule) {
    rules.register(rule);
  }

  public void fire(Facts facts) {
    rulesEngine.fire(rules, facts);
  }
}
