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
import ru.mail.jira.plugins.myteam.controller.dto.IssueCreationSettingsDto;
import ru.mail.jira.plugins.myteam.model.IssueCreationSettings;

@Repository
public class IssueCreationSettingsRepository
    extends PagingAndSortingRepository<IssueCreationSettings, IssueCreationSettingsDto> {

  public IssueCreationSettingsRepository(@ComponentImport ActiveObjects ao) {
    super(ao);
  }

  @Override
  public IssueCreationSettingsDto entityToDto(@NotNull IssueCreationSettings entity) {
    return new IssueCreationSettingsDto(entity);
  }

  @Override
  public void updateEntityFromDto(
      @NotNull IssueCreationSettingsDto dto, @NotNull IssueCreationSettings entity) {
    entity.setChatId(dto.getChatId());
    entity.setTag(dto.getTag());
    entity.setIssueTypeId(dto.getIssueTypeId());
    entity.setProjectKey(dto.getProjectKey());
    entity.setEnabled(dto.getEnabled());
    entity.setLabels(
        (dto.getLabels() == null || dto.getLabels().size() == 0)
            ? null
            : String.join(IssueCreationSettingsDto.LABELS_DELIMITER, dto.getLabels()));
  }

  @Override
  public @Nullable String mapDbField(@Nullable String s) {
    return null;
  }

  public Optional<IssueCreationSettings> getSettingsByChatId(String chatId) {

    IssueCreationSettings[] settings =
        ao.find(IssueCreationSettings.class, Query.select().where("CHAT_ID = ?", chatId));

    if (settings.length == 0) {
      return Optional.empty();
    }

    //    Arrays.stream(ao.find(IssueCreationSettingsEntity.class)).forEach(ao::delete);

    return Optional.of(settings[0]);
  }
}
