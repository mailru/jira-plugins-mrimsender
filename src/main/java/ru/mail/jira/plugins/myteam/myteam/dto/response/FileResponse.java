/* (C)2020 */
package ru.mail.jira.plugins.myteam.myteam.dto.response;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@SuppressWarnings("NullAway")
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileResponse {
  private String type;
  private long size;
  private String filename;
  private String url;
}
