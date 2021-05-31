/* (C)2021 */
package ru.mail.jira.plugins.myteam.bitbucket.dto.utils;

import java.util.List;
import lombok.*;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class LinksDto {
  private List<LinkDto> self;
  private List<LinkDto> clone;
}
