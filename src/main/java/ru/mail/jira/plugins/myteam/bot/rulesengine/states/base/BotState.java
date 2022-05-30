/* (C)2021 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.states.base;

public abstract class BotState {

  protected boolean isWaiting = false;

  public void setWaiting(boolean isWaiting) {
    this.isWaiting = isWaiting;
  }

  public boolean isWaiting() {
    return isWaiting;
  }
}
