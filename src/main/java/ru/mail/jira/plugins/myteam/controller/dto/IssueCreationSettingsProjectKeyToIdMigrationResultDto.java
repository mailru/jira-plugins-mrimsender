/* (C)2024 */
package ru.mail.jira.plugins.myteam.controller.dto;

import java.util.Collections;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

@Getter
@XmlRootElement
public final class IssueCreationSettingsProjectKeyToIdMigrationResultDto {
  @XmlElement private final List<IssueCreationSettingsMigratedInfoDto> notMigratedSettings;
  @XmlElement private final List<IssueCreationSettingsMigratedInfoDto> migratedSettings;

  private IssueCreationSettingsProjectKeyToIdMigrationResultDto(
      final List<IssueCreationSettingsMigratedInfoDto> notMigratedSettings,
      final List<IssueCreationSettingsMigratedInfoDto> migratedSettings) {
    this.notMigratedSettings = Collections.unmodifiableList(notMigratedSettings);
    this.migratedSettings = Collections.unmodifiableList(migratedSettings);
  }

  @Getter
  public static final class IssueCreationSettingsMigratedInfoDto {
    @XmlElement private final int id;
    @XmlElement @Nullable private final String projectKey;
    @XmlElement @Nullable private final String error;

    private IssueCreationSettingsMigratedInfoDto(
        final int id, @Nullable final String projectKey, @Nullable final String error) {
      this.id = id;
      this.projectKey = projectKey;
      this.error = error;
    }
  }

  public static IssueCreationSettingsProjectKeyToIdMigrationResultDto of(
      final List<IssueCreationSettingsMigratedInfoDto> notMigratedSettings,
      final List<IssueCreationSettingsMigratedInfoDto> migratedSettings) {
    return new IssueCreationSettingsProjectKeyToIdMigrationResultDto(
        notMigratedSettings, migratedSettings);
  }

  public static IssueCreationSettingsMigratedInfoDto of(
      final int id, @Nullable final String projectKey, @Nullable final String error) {
    return new IssueCreationSettingsMigratedInfoDto(id, projectKey, error);
  }

  public static IssueCreationSettingsMigratedInfoDto of(final int id, final String projectKey) {
    return IssueCreationSettingsProjectKeyToIdMigrationResultDto.of(id, projectKey, null);
  }
}
