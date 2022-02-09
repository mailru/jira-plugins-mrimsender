/* (C)2022 */
package ru.mail.jira.plugins.myteam.repository;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import java.util.Optional;
import net.java.ao.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Repository;
import ru.mail.jira.plugins.commons.dao.PagingAndSortingRepository;
import ru.mail.jira.plugins.myteam.dto.IssueCreationSettingsDto;
import ru.mail.jira.plugins.myteam.model.IssueCreationSettingsEntity;

@Repository
public class IssueCreationSettingsRepository
    extends PagingAndSortingRepository<IssueCreationSettingsEntity, IssueCreationSettingsDto> {

  public IssueCreationSettingsRepository(@ComponentImport ActiveObjects ao) {
    super(ao);
  }

  @Override
  public IssueCreationSettingsDto entityToDto(@NotNull IssueCreationSettingsEntity entity) {
    return new IssueCreationSettingsDto(entity);
  }

  @Override
  public void updateEntityFromDto(
      @NotNull IssueCreationSettingsDto dto, @NotNull IssueCreationSettingsEntity entity) {
    entity.setChatId(dto.getChatId());
    entity.setTag(dto.getTag());
    entity.setIssueTypeId(dto.getIssueTypeId());
    entity.setProjectKey(dto.getProjectKey());
    entity.setEnabled(dto.getEnabled());
    //    entity.setLabels(
    //        (dto.getLabels() == null || dto.getLabels().size() == 0)
    //            ? null
    //            : String.join(IssueCreationSettingsDto.LABELS_DELIMITER, dto.getLabels()));
  }

  @Override
  public @Nullable String mapDbField(@Nullable String s) {
    return null;
  }

  public Optional<IssueCreationSettingsEntity> getSettingsByChatId(String chatId) {

    IssueCreationSettingsEntity[] settings =
        ao.find(IssueCreationSettingsEntity.class, Query.select().where("CHAT_ID = ?", chatId));

    if (settings.length == 0) {
      return Optional.empty();
    }

    //    Arrays.stream(ao.find(IssueCreationSettingsEntity.class)).forEach(ao::delete);

    return Optional.of(settings[0]);
  }
}
