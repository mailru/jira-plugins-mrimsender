/* (C)2022 */
package ru.mail.jira.plugins.myteam.myteam.dto.response;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import ru.mail.jira.plugins.myteam.myteam.dto.ChatAdmin;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdminsResponse {
  private List<ChatAdmin> admins;
}
