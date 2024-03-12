package ru.mail.jira.plugins.myteam.controller.dto;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

@Getter
public final class IssueCreationSettingsProjectKeyToIdMigrationResultDto {
    private final List<IssueCreationSettingsMigratedInfoDto> notMigratedSettings;
    private final List<IssueCreationSettingsMigratedInfoDto> migratedSettings;

    private IssueCreationSettingsProjectKeyToIdMigrationResultDto(final List<IssueCreationSettingsMigratedInfoDto> notMigratedSettings, final List<IssueCreationSettingsMigratedInfoDto> migratedSettings) {
        this.notMigratedSettings = Collections.unmodifiableList(notMigratedSettings);
        this.migratedSettings = Collections.unmodifiableList(migratedSettings);
    }

    @Getter
    public static final class IssueCreationSettingsMigratedInfoDto {
        private final int id;
        private final String projectKey;
        @Nullable
        private final String error;

        private IssueCreationSettingsMigratedInfoDto(final int id, final String projectKey, @Nullable final String error) {
            this.id = id;
            this.projectKey = projectKey;
            this.error = error;
        }
    }

    public static IssueCreationSettingsProjectKeyToIdMigrationResultDto of(final List<IssueCreationSettingsMigratedInfoDto> notMigratedSettings, final List<IssueCreationSettingsMigratedInfoDto> migratedSettings) {
        return new IssueCreationSettingsProjectKeyToIdMigrationResultDto(notMigratedSettings, migratedSettings);
    }

    public static IssueCreationSettingsMigratedInfoDto of(final int id, final String projectKey, @Nullable final String error) {
        return new IssueCreationSettingsMigratedInfoDto(id, projectKey, error);
    }

    public static IssueCreationSettingsMigratedInfoDto of(final int id, final String projectKey) {
        return IssueCreationSettingsProjectKeyToIdMigrationResultDto.of(id, projectKey, null);
    }

}
