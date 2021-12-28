/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.core;

import org.jeasy.rules.api.Facts;
import org.jeasy.rules.api.Rules;
import org.jeasy.rules.api.RulesEngineParameters;
import org.jeasy.rules.core.DefaultRulesEngine;

public final class MyRulesEngine {

  private final DefaultRulesEngine rulesEngine;
  private final Rules rules;

  public MyRulesEngine(RulesEngineParameters parameters) {
    rulesEngine = new DefaultRulesEngine(parameters);
    rulesEngine.registerRuleListener(new MyRuleListener());
    rules = new Rules();
  }

  public void registerRule(Object rule) {
    rules.register(rule);
  }

  public void fire(Facts facts) {
    rulesEngine.fire(rules, facts);
  }
}
