/* (C)2022 */
package ru.mail.jira.plugins.myteam.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ErrorResponse {
  private String errorCode;
  private String error;
}
