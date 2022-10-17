/* (C)2020 */
package ru.mail.jira.plugins.myteam.myteam.dto.parts;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@SuppressWarnings("NullAway")
@ToString
public class File extends Part<File.Data> {
  public String getFileId() {
    return this.getPayload().fileId;
  }

  public String getCaption() {
    return this.getPayload().caption;
  }

  public String getType() {
    return this.getPayload().type;
  }

  @Getter
  @Setter
  @ToString
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Data {
    private String fileId;
    private String caption;
    private String type;
  }
}
