/* (C)2023 */
package ru.mail.jira.plugins.myteam.myteam.dto;

import java.util.List;
import lombok.Data;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@SuppressWarnings("NullAway")
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TextFormatMetadata {
  private List<Link> link;

  @SuppressWarnings("NullAway")
  @JsonIgnoreProperties(ignoreUnknown = true)
  @Data
  public static final class Link {
    private int length;
    private int offset;
    private String url;
  }
}
