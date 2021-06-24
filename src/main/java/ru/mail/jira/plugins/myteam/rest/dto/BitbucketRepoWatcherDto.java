/* (C)2021 */
package ru.mail.jira.plugins.myteam.rest.dto;

import lombok.*;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BitbucketRepoWatcherDto {
  private String email;
}
