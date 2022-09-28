/* (C)2022 */
package ru.mail.jira.plugins.myteam.db.repository;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.bc.filter.SearchRequestService;
import com.atlassian.jira.issue.search.SearchRequest;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.web.component.cron.CronEditorBean;
import com.atlassian.jira.web.component.cron.generator.CronExpressionDescriptor;
import com.atlassian.jira.web.component.cron.parser.CronExpressionParser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Objects;
import net.java.ao.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.commons.dao.PagingAndSortingRepository;
import ru.mail.jira.plugins.commons.dto.jira.UserDto;
import ru.mail.jira.plugins.myteam.controller.dto.FilterSubscriptionDto;
import ru.mail.jira.plugins.myteam.controller.dto.JqlFilterDto;
import ru.mail.jira.plugins.myteam.db.model.FilterSubscription;

@Component
public class FilterSubscriptionRepository
    extends PagingAndSortingRepository<FilterSubscription, FilterSubscriptionDto> {
  public static final DateTimeFormatter DATE_TIME_FORMATTER =
      java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

  private final JiraAuthenticationContext jiraAuthenticationContext;
  private final SearchRequestService searchRequestService;
  private final UserManager userManager;

  public FilterSubscriptionRepository(
      ActiveObjects ao,
      @ComponentImport JiraAuthenticationContext jiraAuthenticationContext,
      @ComponentImport SearchRequestService searchRequestService,
      @ComponentImport UserManager userManager) {
    super(ao);
    this.jiraAuthenticationContext = jiraAuthenticationContext;
    this.searchRequestService = searchRequestService;
    this.userManager = userManager;
  }

  @Override
  public FilterSubscriptionDto entityToDto(@NotNull FilterSubscription entity) {
    ApplicationUser user = userManager.getUserByKey(entity.getUserKey());
    UserDto userDto = new UserDto();
    if (user != null) {
      userDto.setUserKey(user.getKey());
      userDto.setDisplayName(user.getDisplayName());
    }
    SearchRequest filter =
        searchRequestService.getFilter(new JiraServiceContextImpl(user), entity.getFilterId());
    FilterSubscriptionDto dto = new FilterSubscriptionDto(entity);
    dto.setFilter(
        new JqlFilterDto(filter.getId(), filter.getName(), filter.getQuery().getQueryString()));
    dto.setCronExpressionDescription(getCronExpressionDescription(entity.getCronExpression()));
    dto.setUser(userDto);
    return dto;
  }

  @Override
  public void updateEntityFromDto(
      @NotNull FilterSubscriptionDto dto, @NotNull FilterSubscription entity) {
    if (dto.getFilter() != null)
      entity.setFilterId(Objects.requireNonNull(dto.getFilter().getId()));
    if (dto.getUser() != null)
      entity.setUserKey(Objects.requireNonNull(dto.getUser().getUserKey()));
    entity.setGroupName(dto.getGroupName());
    entity.setCronExpression(Objects.requireNonNull(dto.getCronExpression()));
    if (dto.getLastRun() != null)
      entity.setLastRun(
          Date.from(
              LocalDateTime.parse(dto.getLastRun(), DATE_TIME_FORMATTER)
                  .atZone(ZoneId.systemDefault())
                  .toInstant()));
    entity.setEmailOnEmpty(Objects.requireNonNull(dto.getEmailOnEmpty()));
  }

  @Override
  public @Nullable String mapDbField(@Nullable String s) {
    return null;
  }

  public FilterSubscription[] getSubscription(@NotNull String userKey) {
    return ao.find(FilterSubscription.class, Query.select().where("USER_KEY = ?", userKey));
  }

  private String getCronExpressionDescription(String cronExpression) {
    CronExpressionParser cronExpresionParser = new CronExpressionParser(cronExpression);
    if (cronExpresionParser.isValidForEditor()) {
      CronEditorBean cronEditorBean = cronExpresionParser.getCronEditorBean();
      return new CronExpressionDescriptor(jiraAuthenticationContext.getI18nHelper())
          .getPrettySchedule(cronEditorBean);
    } else {
      return cronExpression;
    }
  }
}
