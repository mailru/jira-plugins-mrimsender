/* (C)2022 */
package ru.mail.jira.plugins.myteam.accessrequest.controller.dto;

import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.mail.jira.plugins.commons.dto.jira.UserDto;

@SuppressWarnings("NullAway")
@Getter
@Setter
@NoArgsConstructor
public class AccessRequestDto {
  @NotNull @XmlElement private List<UserDto> users;

  @Nullable @XmlElement private String message;

  @XmlElement private boolean sent;

  @Nullable @XmlElement private String requesterKey;

  @Nullable @XmlElement private Long issueId;
}
