/* (C)2021 */
package ru.mail.jira.plugins.myteam.bitbucket.dto.utils;

import lombok.*;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class ChangeDto {
  private RefDto ref;
  private String refId;
  private String fromHash;
  private String toHash;
  private String type;
}