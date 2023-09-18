/* (C)2023 */
package ru.mail.jira.plugins.myteam.myteam.dto;

import java.util.List;
import lombok.Data;

@SuppressWarnings("NullAway")
@Data
public class TextFormatMetadata {
  private List<Link> link;

  @SuppressWarnings("NullAway")
  @Data
  public static final class Link {
    private int length;
    private int offset;
    private String url;
  }
}
