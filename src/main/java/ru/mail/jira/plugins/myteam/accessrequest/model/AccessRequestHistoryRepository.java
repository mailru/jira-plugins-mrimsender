/* (C)2022 */
package ru.mail.jira.plugins.myteam.accessrequest.model;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.stream.Collectors;
import net.java.ao.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.commons.CommonUtils;
import ru.mail.jira.plugins.commons.dao.PagingAndSortingRepository;
import ru.mail.jira.plugins.commons.dto.jira.UserDto;
import ru.mail.jira.plugins.myteam.accessrequest.controller.dto.AccessRequestDto;
import ru.mail.jira.plugins.myteam.accessrequest.controller.dto.DtoUtils;
import ru.mail.jira.plugins.myteam.commons.Utils;

@Component
@SuppressWarnings("NullAway")
public class AccessRequestHistoryRepository
    extends PagingAndSortingRepository<AccessRequestHistory, AccessRequestDto> {
  private final DtoUtils dtoUtils;
  private final UserManager userManager;

  public AccessRequestHistoryRepository(
      ActiveObjects ao, DtoUtils dtoUtils, @ComponentImport UserManager userManager) {
    super(ao);
    this.userManager = userManager;
    this.dtoUtils = dtoUtils;
  }

  @Override
  public AccessRequestDto entityToDto(@NotNull AccessRequestHistory entity) {
    AccessRequestDto dto = new AccessRequestDto();
    dto.setUsers(
        CommonUtils.split(entity.getUserKeys()).stream()
            .map(key -> dtoUtils.buildUserDto(userManager.getUserByKey(key)))
            .collect(Collectors.toList()));
    dto.setMessage(entity.getMessage());
    dto.setSend(Boolean.TRUE);
    dto.setRequesterKey(entity.getRequesterKey());
    dto.setIssueId(entity.getIssueId());
    return null;
  }

  @Override
  public void updateEntityFromDto(
      @NotNull AccessRequestDto dto, @NotNull AccessRequestHistory entity) {
    entity.setUserKeys(
        dto.getUsers().stream().map(UserDto::getUserKey).collect(Collectors.joining(",")));
    entity.setMessage(dto.getMessage());
    entity.setRequesterKey(Objects.requireNonNull(dto.getRequesterKey()));
    entity.setIssueId(Objects.requireNonNull(dto.getIssueId()));
    entity.setDate(Utils.convertToDate(LocalDateTime.now(ZoneId.systemDefault())));
  }

  @Override
  public @Nullable String mapDbField(@Nullable String s) {
    return null;
  }

  @Nullable
  public AccessRequestHistory getAccessRequestHistory(String requesterKey, long issueId) {
    AccessRequestHistory[] histories =
        ao.find(
            AccessRequestHistory.class,
            Query.select().where("REQUESTER_KEY = ? AND ISSUE_ID = ?", requesterKey, issueId));
    return histories.length > 0 ? histories[0] : null;
  }
}
