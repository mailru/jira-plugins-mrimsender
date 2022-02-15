/* (C)2022 */
package ru.mail.jira.plugins.myteam.service.impl;

import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheLoader;
import com.atlassian.cache.CacheManager;
import com.atlassian.cache.CacheSettingsBuilder;
import com.atlassian.jira.exception.NotFoundException;
import com.atlassian.jira.util.lang.Pair;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.google.common.base.Splitter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import ru.mail.jira.plugins.myteam.controller.dto.IssueCreationSettingsDto;
import ru.mail.jira.plugins.myteam.repository.IssueCreationSettingsRepository;
import ru.mail.jira.plugins.myteam.service.IssueCreationSettingsService;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class IssueCreationSettingsServiceImpl implements IssueCreationSettingsService {

    private static final String CACHE_NAME =
            IssueCreationSettingsServiceImpl.class.getName() + ".issueSettingsCache";
    private static final String CACHE_KEY_DELIMITER = "||";
    private static final String CACHE_KEY_DELIMITER_PATTERN = "\\|\\|";

    private final IssueCreationSettingsRepository issueCreationSettingsRepository;

    private final Cache<String, Optional<IssueCreationSettingsDto>>
            issueSettingsCache; // Settings cache. Where key is "{chatId}||{tag}".
    // Example: 74175@chat.agent-#task

    public IssueCreationSettingsServiceImpl(
            IssueCreationSettingsRepository issueCreationSettingsRepository,
            @ComponentImport CacheManager cacheManager) {
        this.issueCreationSettingsRepository = issueCreationSettingsRepository;

        issueSettingsCache =
                cacheManager.getCache(
                        CACHE_NAME,
                        new IssueSettingsCacheLoader(),
                        new CacheSettingsBuilder()
                                .remote()
                                .replicateViaInvalidation()
                                .build());
    }

    @Override
    public List<IssueCreationSettingsDto> getAllSettings() {
        return issueCreationSettingsRepository.findAll().stream()
                .map(IssueCreationSettingsDto::new)
                .collect(Collectors.toList());
    }

    @Override
    public IssueCreationSettingsDto getSettings(int id) throws NotFoundException {
        return new IssueCreationSettingsDto(issueCreationSettingsRepository.get(id));
    }

    @Override
    @Nullable
    public IssueCreationSettingsDto getSettings(String chatId, String tag) {
        if (!hasChatSettings(chatId, tag)) return null;
        return issueSettingsCache.get(getSettingsCacheKey(chatId, tag)).orElse(null); // NotNull
    }

    @Override
    public Optional<IssueCreationSettingsDto> getSettingsByChatId(String chatId) {
        return issueCreationSettingsRepository
                .getSettingsByChatId(chatId)
                .map(IssueCreationSettingsDto::new);
    }

    @Override
    public IssueCreationSettingsDto addDefaultSettings(String chatId) {
        IssueCreationSettingsDto settings =
                IssueCreationSettingsDto.builder().chatId(chatId).enabled(false).tag("task").build();

        issueCreationSettingsRepository.create(settings);
        issueSettingsCache.removeAll();

        return issueSettingsCache.get(getSettingsCacheKey(chatId, "task")).orElse(null); // NotNull
    }

    @Override
    public IssueCreationSettingsDto updateSettings(int id, IssueCreationSettingsDto settings) {
        issueCreationSettingsRepository.update(id, settings);
        issueSettingsCache.removeAll();

        return issueSettingsCache
                .get(getSettingsCacheKey(settings.getChatId(), settings.getTag()))
                .orElse(null); // NotNull
    }

    @Override
    public boolean hasRequiredFields(@Nullable IssueCreationSettingsDto settings) {
        return settings != null
                && settings.getEnabled()
                && settings.getProjectKey() != null
                && settings.getIssueTypeId() != null;
    }

    @Override
    public boolean hasChatSettings(String chatId, String tag) {
        return issueSettingsCache.get(getSettingsCacheKey(chatId, tag)).isPresent(); // NotNull
    }

    private String getSettingsCacheKey(String chatId, String tag) {
        return String.format("%s%s%s", chatId, CACHE_KEY_DELIMITER, tag);
    }

    private Optional<Pair<String, String>> splitSettingsKey(String key) {
        List<String> split = Splitter.onPattern(CACHE_KEY_DELIMITER_PATTERN).splitToList(key);
        if (split.size() != 2) {
            return Optional.empty();
        }
        return Optional.of(Pair.of(split.get(0), split.get(1)));
    }

    private class IssueSettingsCacheLoader
            implements CacheLoader<String, Optional<IssueCreationSettingsDto>> {

        // if null values are possible, e.g. when the key is a database ID that does not actually exist,
        // declare the loader's value type to be a wrapper type such as Guava's Option<Foo> class
        @Override
        public @NotNull Optional<IssueCreationSettingsDto> load(@Nonnull final String fieldConfigId) {
            Optional<Pair<String, String>> keys = splitSettingsKey(fieldConfigId);

            return keys.map(
                            k -> issueCreationSettingsRepository.getSettingsByChatIdAndTag(k.first(), k.second()))
                    .flatMap(s -> s)
                    .map(IssueCreationSettingsDto::new);
        }
    }
}
