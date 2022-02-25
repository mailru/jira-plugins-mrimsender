/* (C)2022 */
package ru.mail.jira.plugins.myteam.repository;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.java.ao.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Repository;
import ru.mail.jira.plugins.commons.dao.PagingAndSortingRepository;
import ru.mail.jira.plugins.myteam.controller.dto.IssueCreationSettingsDto;
import ru.mail.jira.plugins.myteam.model.AdditionalIssueField;
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
    ao.executeInTransaction(
        () -> {
          entity.setChatId(dto.getChatId());
          entity.setTag(dto.getTag());
          entity.setIssueTypeId(dto.getIssueTypeId());
          entity.setProjectKey(dto.getProjectKey());
          entity.setEnabled(dto.getEnabled());
          entity.setLabels(
              (dto.getLabels() == null || dto.getLabels().size() == 0)
                  ? null
                  : String.join(IssueCreationSettingsDto.LABELS_DELIMITER, dto.getLabels()));

          Map<String, AdditionalIssueField> entityAdditionalFields = new HashMap<>();

          Arrays.stream(entity.getAdditionalFields())
              .forEach(
                  f -> {
                    if (entityAdditionalFields.containsKey(
                        f.getFieldId())) { // delete duplicates such as checkbox group
                      ao.delete(f);
                    } else {
                      entityAdditionalFields.put(f.getFieldId(), f);
                    }
                  });

          dto.getAdditionalFields()
              .forEach(
                  f -> {
                    if (entityAdditionalFields.containsKey(f.getField())) {
                      AdditionalIssueField field = entityAdditionalFields.get(f.getField());
                      field.setValue(f.getValue());
                      field.save();
                      entityAdditionalFields.remove(f.getField());
                    } else {
                      AdditionalIssueField field = ao.create(AdditionalIssueField.class);
                      field.setIssueCreationSettings(entity);
                      field.setFieldId(f.getField());
                      field.setValue(f.getValue());
                      field.save();
                    }
                  });

          entityAdditionalFields.values().forEach(ao::delete);
          return entity;
        });
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

    return Optional.of(settings[0]);
  }

  public Optional<IssueCreationSettings> getSettingsByChatIdAndTag(String chatId, String tag) {

    IssueCreationSettings[] settings =
        ao.find(
            IssueCreationSettings.class,
            Query.select().where("CHAT_ID = ? AND TAG = ?", chatId, tag));

    if (settings.length == 0) {
      return Optional.empty();
    }

    return Optional.of(settings[0]);
  }
}
