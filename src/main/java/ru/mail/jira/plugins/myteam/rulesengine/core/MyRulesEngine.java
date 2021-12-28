/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.core;

import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.jeasy.rules.api.*;

@Slf4j
public final class MyRulesEngine implements RulesEngine {

  private final Rules rules;

  private final RulesEngineParameters parameters;
  private final List<RuleListener> ruleListeners;
  private final List<RulesEngineListener> rulesEngineListeners;

  public MyRulesEngine(RulesEngineParameters parameters) {
    this.parameters = parameters;
    this.ruleListeners = new ArrayList<>();
    this.rulesEngineListeners = new ArrayList<>();
    registerRuleListener(new MyRuleListener());
    rules = new Rules();
  }

  @Override
  public RulesEngineParameters getParameters() {
    return new RulesEngineParameters(
        this.parameters.isSkipOnFirstAppliedRule(),
        this.parameters.isSkipOnFirstFailedRule(),
        this.parameters.isSkipOnFirstNonTriggeredRule(),
        this.parameters.getPriorityThreshold());
  }

  @Override
  public List<RuleListener> getRuleListeners() {
    return Collections.unmodifiableList(this.ruleListeners);
  }

  @Override
  public List<RulesEngineListener> getRulesEngineListeners() {
    return Collections.unmodifiableList(this.rulesEngineListeners);
  }

  public void registerRuleListener(RuleListener ruleListener) {
    this.ruleListeners.add(ruleListener);
  }

  public void registerRuleListeners(List<RuleListener> ruleListeners) {
    this.ruleListeners.addAll(ruleListeners);
  }

  public void registerRulesEngineListener(RulesEngineListener rulesEngineListener) {
    this.rulesEngineListeners.add(rulesEngineListener);
  }

  public void registerRulesEngineListeners(List<RulesEngineListener> rulesEngineListeners) {
    this.rulesEngineListeners.addAll(rulesEngineListeners);
  }

  public void registerRule(Object rule) {
    rules.register(rule);
  }

  public void fire(Facts facts) {
    this.fire(rules, facts);
  }

  @Override
  public void fire(Rules rules, Facts facts) {
    this.triggerListenersBeforeRules(rules, facts);
    this.doFire(rules, facts);
    this.triggerListenersAfterRules(rules, facts);
  }

  void doFire(Rules rules, Facts facts) {
    if (rules.isEmpty()) {
      log.warn("No rules registered! Nothing to apply");
    } else {
      this.logEngineParameters();
      this.log(rules);
      this.log(facts);
      log.debug("Rules evaluation started");

      for (Rule rule : rules) {
        String name = rule.getName();
        int priority = rule.getPriority();
        if (priority > this.parameters.getPriorityThreshold()) {
          log.debug(
              "Rule priority threshold ({}) exceeded at rule '{}' with priority={}, next rules will be skipped",
              this.parameters.getPriorityThreshold(),
              name,
              priority);
          break;
        }

        if (!this.shouldBeEvaluated(rule, facts)) {
          log.debug("Rule '{}' has been skipped before being evaluated", name);
        } else {
          boolean evaluationResult = false;

          try {
            evaluationResult = rule.evaluate(facts);
          } catch (RuntimeException e) {
            this.triggerListenersOnEvaluationError(rule, facts, e);
            if (this.parameters.isSkipOnFirstNonTriggeredRule()) {
              log.debug(
                  "Next rules will be skipped since parameter skipOnFirstNonTriggeredRule is set");
              break;
            }
          }

          if (evaluationResult) {
            log.debug("Rule '{}' triggered", name);
            this.triggerListenersAfterEvaluate(rule, facts, true);

            try {
              this.triggerListenersBeforeExecute(rule, facts);
              rule.execute(facts);
              log.debug("Rule '{}' performed successfully", name);
              this.triggerListenersOnSuccess(rule, facts);
              if (this.parameters.isSkipOnFirstAppliedRule()) {
                log.debug(
                    "Next rules will be skipped since parameter skipOnFirstAppliedRule is set");
                break;
              }
            } catch (Exception e) {
              this.triggerListenersOnFailure(rule, e, facts);
              if (this.parameters.isSkipOnFirstFailedRule()) {
                log.debug(
                    "Next rules will be skipped since parameter skipOnFirstFailedRule is set");
                break;
              }
            }
          } else {
            log.debug("Rule '{}' has been evaluated to false, it has not been executed", name);
            this.triggerListenersAfterEvaluate(rule, facts, false);
            if (this.parameters.isSkipOnFirstNonTriggeredRule()) {
              log.debug(
                  "Next rules will be skipped since parameter skipOnFirstNonTriggeredRule is set");
              break;
            }
          }
        }
      }
    }
  }

  private void logEngineParameters() {
    log.debug("{}", this.parameters);
  }

  private void log(Rules rules) {
    log.debug("Registered rules:");

    for (Rule rule : rules) {
      log.debug(
          "Rule { name = '{}', description = '{}', priority = '{}'}",
          rule.getName(),
          rule.getDescription(),
          rule.getPriority());
    }
  }

  private void log(Facts facts) {
    log.debug("Known facts:");

    for (Fact<?> value : facts) {
      log.debug("{}", value);
    }
  }

  @Override
  public Map<Rule, Boolean> check(Rules rules, Facts facts) {
    this.triggerListenersBeforeRules(rules, facts);
    Map<Rule, Boolean> result = this.doCheck(rules, facts);
    this.triggerListenersAfterRules(rules, facts);
    return result;
  }

  private Map<Rule, Boolean> doCheck(Rules rules, Facts facts) {
    log.debug("Checking rules");
    Map<Rule, Boolean> result = new HashMap<>();

    for (Rule rule : rules) {
      if (this.shouldBeEvaluated(rule, facts)) {
        result.put(rule, rule.evaluate(facts));
      }
    }

    return result;
  }

  private void triggerListenersOnFailure(Rule rule, Exception exception, Facts facts) {
    this.ruleListeners.forEach((ruleListener) -> ruleListener.onFailure(rule, facts, exception));
  }

  private void triggerListenersOnSuccess(Rule rule, Facts facts) {
    this.ruleListeners.forEach((ruleListener) -> ruleListener.onSuccess(rule, facts));
  }

  private void triggerListenersBeforeExecute(Rule rule, Facts facts) {
    this.ruleListeners.forEach((ruleListener) -> ruleListener.beforeExecute(rule, facts));
  }

  private boolean triggerListenersBeforeEvaluate(Rule rule, Facts facts) {
    return this.ruleListeners.stream()
        .allMatch((ruleListener) -> ruleListener.beforeEvaluate(rule, facts));
  }

  private void triggerListenersAfterEvaluate(Rule rule, Facts facts, boolean evaluationResult) {
    this.ruleListeners.forEach(
        (ruleListener) -> ruleListener.afterEvaluate(rule, facts, evaluationResult));
  }

  private void triggerListenersOnEvaluationError(Rule rule, Facts facts, Exception exception) {
    this.ruleListeners.forEach(
        (ruleListener) -> ruleListener.onEvaluationError(rule, facts, exception));
  }

  private void triggerListenersBeforeRules(Rules rule, Facts facts) {
    this.rulesEngineListeners.forEach(
        (rulesEngineListener) -> rulesEngineListener.beforeEvaluate(rule, facts));
  }

  private void triggerListenersAfterRules(Rules rule, Facts facts) {
    this.rulesEngineListeners.forEach(
        (rulesEngineListener) -> rulesEngineListener.afterExecute(rule, facts));
  }

  private boolean shouldBeEvaluated(Rule rule, Facts facts) {
    return this.triggerListenersBeforeEvaluate(rule, facts);
  }
}
