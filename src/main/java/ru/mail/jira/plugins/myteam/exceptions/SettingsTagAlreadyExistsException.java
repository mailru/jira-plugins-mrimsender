/* (C)2022 */
package ru.mail.jira.plugins.myteam.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class SettingsTagAlreadyExistsException extends Exception {
  public SettingsTagAlreadyExistsException(String message) {
    super(message);
  }
}
