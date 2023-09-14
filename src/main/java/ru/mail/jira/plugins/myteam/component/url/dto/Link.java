/* (C)2023 */
package ru.mail.jira.plugins.myteam.component.url.dto;

import javax.validation.constraints.NotNull;
import lombok.*;

@Value(staticConstructor = "of")
public class Link {
  @NotNull String link;
  @NotNull String mask;

  @EqualsAndHashCode.Exclude boolean masked;
}
