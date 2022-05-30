/* (C)2022 */
package ru.mail.jira.plugins.myteam.service.model;

import lombok.Getter;

@Getter
public class FieldInfo {
  private final String name;
  private final String value;
  private final String html;

  public FieldInfo(String name, String value, String html) {
    this.name = name;
    this.value = value;
    this.html = html;
  }
}
