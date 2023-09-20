/* (C)2023 */
package ru.mail.jira.plugins.myteam.myteam.dto;

import java.util.List;
import lombok.Data;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.jetbrains.annotations.Nullable;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class TextFormatMetadata {
  @Nullable private List<Link> link;

  @JsonIgnoreProperties(ignoreUnknown = true)
  @Data
  public static final class Link {
    private int length;
    private int offset;
    @Nullable private String url;
  }
}
