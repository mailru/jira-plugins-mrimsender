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
import com.atlassian.jira.web.component.cron.generator.CronExpressionGenerator;
import com.atlassian.jira.web.component.cron.parser.CronExpressionParser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.scheduler.SchedulerService;
import com.atlassian.scheduler.status.JobDetails;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import net.java.ao.Query;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.commons.CommonUtils;
import ru.mail.jira.plugins.commons.dao.PagingAndSortingRepository;
import ru.mail.jira.plugins.commons.dto.jira.UserDto;
import ru.mail.jira.plugins.myteam.commons.CalendarUtils;
import ru.mail.jira.plugins.myteam.commons.Const;
import ru.mail.jira.plugins.myteam.controller.dto.FilterSubscriptionDto;
import ru.mail.jira.plugins.myteam.controller.dto.JqlFilterDto;
import ru.mail.jira.plugins.myteam.db.model.FilterSubscription;
import ru.mail.jira.plugins.myteam.db.model.RecipientsType;
import ru.mail.jira.plugins.myteam.service.FilterSubscriptionService;

@Component
@SuppressWarnings("NullAway")
public class FilterSubscriptionRepository
    extends PagingAndSortingRepository<FilterSubscription, FilterSubscriptionDto> {
  private final JiraAuthenticationContext jiraAuthenticationContext;
  private final SearchRequestService searchRequestService;
  private final SchedulerService schedulerService;
  private final UserManager userManager;

  public FilterSubscriptionRepository(
      ActiveObjects ao,
      @ComponentImport JiraAuthenticationContext jiraAuthenticationContext,
      @ComponentImport SearchRequestService searchRequestService,
      @ComponentImport SchedulerService schedulerService,
      @ComponentImport UserManager userManager) {
    super(ao);
    this.jiraAuthenticationContext = jiraAuthenticationContext;
    this.searchRequestService = searchRequestService;
    this.schedulerService = schedulerService;
    this.userManager = userManager;
  }

  @Override
  @Nullable
  public FilterSubscriptionDto entityToDto(@NotNull FilterSubscription entity) {
    ApplicationUser creator = userManager.getUserByKey(entity.getUserKey());
    SearchRequest searchRequest =
        searchRequestService.getFilter(new JiraServiceContextImpl(creator), entity.getFilterId());
    RecipientsType recipientsType = entity.getRecipientsType();
    String recipients = entity.getRecipients();
    Map<String, String[]> scheduleParams = getScheduleParams(entity);

    FilterSubscriptionDto dto = new FilterSubscriptionDto();
    dto.setId(entity.getID());
    if (searchRequest != null) {
      dto.setFilter(new JqlFilterDto(searchRequest, creator));
    }
    dto.setCreator(buildUserDto(creator));
    dto.setRecipientsType(recipientsType);
    if (recipientsType.equals(RecipientsType.USER)) {
      dto.setUsers(
          CommonUtils.split(recipients).stream()
              .map(key -> buildUserDto(userManager.getUserByKey(key)))
              .collect(Collectors.toList()));
    } else if (recipientsType.equals(RecipientsType.GROUP)) {
      dto.setGroups(CommonUtils.split(recipients));
    } else {
      dto.setChats(CommonUtils.split(recipients));
    }
    dto.setScheduleDescription(getCronExpressionDescription(entity.getCronExpression()));
    if (scheduleParams.containsKey("scheduleMode"))
      dto.setScheduleMode(scheduleParams.get("scheduleMode")[0]);
    if (scheduleParams.containsKey("hours"))
      dto.setHours(Integer.parseInt(scheduleParams.get("hours")[0]));
    if (scheduleParams.containsKey("minutes"))
      dto.setMinutes(Integer.parseInt(scheduleParams.get("minutes")[0]));
    if (scheduleParams.containsKey("weekdays"))
      dto.setWeekDays(Arrays.asList(scheduleParams.get("weekdays")));
    if (scheduleParams.containsKey("monthDay"))
      dto.setMonthDay(Integer.parseInt(scheduleParams.get("monthDay")[0]));
    if (scheduleParams.containsKey("advanced")) dto.setAdvanced(scheduleParams.get("advanced")[0]);
    dto.setLastRun(CalendarUtils.formatDate(entity.getLastRun()));
    dto.setNextRun(getNextRun(entity));
    dto.setType(entity.getType());
    dto.setEmailOnEmpty(entity.isEmailOnEmpty());
    dto.setSeparateIssues(entity.isSeparateIssues());
    return dto;
  }

  @Override
  public void updateEntityFromDto(
      @NotNull FilterSubscriptionDto dto, @NotNull FilterSubscription entity) {
    ApplicationUser loggedInUser = jiraAuthenticationContext.getLoggedInUser();
    RecipientsType recipientsType = dto.getRecipientsType();
    List<UserDto> users = dto.getUsers();
    List<String> groups = dto.getGroups();
    List<String> chats = dto.getChats();

    entity.setFilterId(dto.getFilter().getId());
    if (dto.getCreator() == null) {
      entity.setUserKey(Objects.requireNonNull(Objects.requireNonNull(loggedInUser).getKey()));
    } else {
      entity.setUserKey(
          Objects.requireNonNull(Objects.requireNonNull(dto.getCreator()).getUserKey()));
    }
    entity.setRecipientsType(recipientsType);
    if (recipientsType.equals(RecipientsType.USER)) {
      entity.setRecipients(
          Objects.requireNonNull(users).stream()
              .map(UserDto::getUserKey)
              .collect(Collectors.joining(",")));
    } else if (recipientsType.equals(RecipientsType.GROUP)) {
      entity.setRecipients(String.join(",", Objects.requireNonNull(groups)));
    } else {
      entity.setRecipients(String.join(",", Objects.requireNonNull(chats)));
    }
    entity.setScheduleMode(dto.getScheduleMode());
    entity.setCronExpression(Objects.requireNonNull(getCronExpressionString(dto)));
    entity.setLastRun(CalendarUtils.parseDate(dto.getLastRun()));
    entity.setType(dto.getType());
    entity.setEmailOnEmpty(dto.isEmailOnEmpty());
    entity.setSeparateIssues(dto.isSeparateIssues());
  }

  @Override
  public @Nullable String mapDbField(@Nullable String s) {
    return null;
  }

  public FilterSubscription[] getSubscriptions() {
    return ao.find(FilterSubscription.class);
  }

  public FilterSubscription[] getSubscriptions(
      @Nullable List<String> userKeys,
      @Nullable Long filterId,
      @Nullable RecipientsType recipientsType,
      @Nullable List<String> recipients) {
    StringBuilder queryBuilder = new StringBuilder();
    if (userKeys != null && !userKeys.isEmpty()) {
      queryBuilder.append("(");
      for (String userKey : userKeys) {
        queryBuilder.append(String.format("USER_KEY = '%s'", userKey));
        if (userKeys.indexOf(userKey) != userKeys.size() - 1) queryBuilder.append(" OR ");
      }
      queryBuilder.append(")");
    }
    if (filterId != null) {
      if (queryBuilder.length() > 0) queryBuilder.append(" AND ");
      queryBuilder.append(String.format("FILTER_ID = %s", filterId));
    }
    if (recipientsType != null) {
      if (queryBuilder.length() > 0) queryBuilder.append(" AND ");
      queryBuilder.append(String.format("RECIPIENTS_TYPE = '%s'", recipientsType));
    }
    if (recipients != null && !recipients.isEmpty()) {
      if (queryBuilder.length() > 0) queryBuilder.append(" AND ");
      queryBuilder.append("(");
      for (String recipient : recipients) {
        queryBuilder.append(String.format("LOWER(RECIPIENTS) LIKE LOWER('%%%s%%')", recipient));
        if (recipients.indexOf(recipient) != recipients.size() - 1) queryBuilder.append(" OR ");
      }
      queryBuilder.append(")");
    }

    if (queryBuilder.length() == 0) return getSubscriptions();
    Query query = Query.select();
    query.setWhereClause(queryBuilder.toString());
    return ao.find(FilterSubscription.class, query);
  }

  public FilterSubscription updateLastRun(int id, LocalDateTime dateTime) {
    FilterSubscription entity = get(id);
    entity.setLastRun(CalendarUtils.convertToDate(dateTime));
    entity.save();
    return entity;
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

  @Nullable
  private UserDto buildUserDto(ApplicationUser user) {
    if (user == null) return null;
    UserDto userDto = new UserDto();
    userDto.setUserKey(user.getKey());
    userDto.setDisplayName(user.getDisplayName());
    return userDto;
  }

  public Map<String, String[]> getScheduleParams(FilterSubscription filterSubscription) {
    String scheduleModeMode = filterSubscription.getScheduleMode();
    Map<String, String[]> scheduleModeParams = new HashMap<>();
    scheduleModeParams.put("scheduleMode", new String[] {scheduleModeMode});
    if (CronEditorBean.ADVANCED_MODE.equals(scheduleModeMode)) {
      scheduleModeParams.put("advanced", new String[] {filterSubscription.getCronExpression()});
      return scheduleModeParams;
    }

    CronExpressionParser cronExpressionParser =
        new CronExpressionParser(filterSubscription.getCronExpression());
    CronEditorBean cronEditorBean = cronExpressionParser.getCronEditorBean();
    if (cronEditorBean.getMinutes() != null && cronEditorBean.getHoursRunOnce() != null) {
      scheduleModeParams.put(
          "hours",
          new String[] {
            String.valueOf(
                CalendarUtils.get24HourTime(
                    Integer.parseInt(cronEditorBean.getHoursRunOnce()),
                    cronEditorBean.getHoursRunOnceMeridian()))
          });
      scheduleModeParams.put("minutes", new String[] {cronEditorBean.getMinutes()});
    }
    if (CronEditorBean.DAYS_OF_WEEK_SPEC_MODE.equals(scheduleModeMode)
        && cronEditorBean.getSpecifiedDaysPerWeek() != null)
      scheduleModeParams.put(
          "weekdays", StringUtils.split(cronEditorBean.getSpecifiedDaysPerWeek(), ","));
    if (CronEditorBean.DAYS_OF_MONTH_SPEC_MODE.equals(scheduleModeMode)
        && cronEditorBean.getDayOfMonth() != null)
      scheduleModeParams.put("monthDay", new String[] {cronEditorBean.getDayOfMonth()});
    return scheduleModeParams;
  }

  private Map<String, String[]> buildCronEditorBeanParams(
      String scheduleMode,
      Integer hours,
      Integer minutes,
      List<String> weekdays,
      Integer monthDay,
      String advanced) {
    Map<String, String[]> result = new HashMap<>();
    if (StringUtils.isNotBlank(scheduleMode))
      result.put(
          String.format("%s.dailyWeeklyMonthly", Const.SCHEDULE_PREFIX),
          new String[] {scheduleMode});
    if (!CronEditorBean.ADVANCED_MODE.equals(scheduleMode) && hours != null && minutes != null) {
      result.put(
          String.format("%s.runOnceHours", Const.SCHEDULE_PREFIX),
          new String[] {String.valueOf(CalendarUtils.get12HourTime(hours))});
      result.put(
          String.format("%s.runOnceMins", Const.SCHEDULE_PREFIX),
          new String[] {String.valueOf(minutes)});
      result.put(
          String.format("%s.runOnceMeridian", Const.SCHEDULE_PREFIX),
          new String[] {CalendarUtils.getMeridianIndicator(hours)});
    }
    if (CronEditorBean.ADVANCED_MODE.equals(scheduleMode))
      result.put(String.format("%s.cronString", Const.SCHEDULE_PREFIX), new String[] {advanced});
    if (CronEditorBean.DAYS_OF_WEEK_SPEC_MODE.equals(scheduleMode))
      result.put(
          String.format("%s.weekday", Const.SCHEDULE_PREFIX),
          weekdays != null ? weekdays.toArray(String[]::new) : new String[0]);
    if (CronEditorBean.DAYS_OF_MONTH_SPEC_MODE.equals(scheduleMode) && monthDay != null) {
      result.put(
          String.format("%s.monthDay", Const.SCHEDULE_PREFIX), new String[] {monthDay.toString()});
      result.put(
          String.format("%s.daysOfMonthOpt", Const.SCHEDULE_PREFIX), new String[] {"dayOfMonth"});
      result.put(String.format("%s.day", Const.SCHEDULE_PREFIX), new String[] {"1"});
      result.put(String.format("%s.week", Const.SCHEDULE_PREFIX), new String[] {"1"});
    }
    result.put(String.format("%s.interval", Const.SCHEDULE_PREFIX), new String[] {"0"});
    return result;
  }

  private String getCronExpressionString(FilterSubscriptionDto filterSubscriptionDto) {
    Map<String, String[]> scheduleParams =
        buildCronEditorBeanParams(
            filterSubscriptionDto.getScheduleMode(),
            filterSubscriptionDto.getHours(),
            filterSubscriptionDto.getMinutes(),
            filterSubscriptionDto.getWeekDays(),
            filterSubscriptionDto.getMonthDay(),
            filterSubscriptionDto.getAdvanced());
    CronEditorBean cronEditorBean = new CronEditorBean(Const.SCHEDULE_PREFIX, scheduleParams);
    CronExpressionGenerator cronExpressionGenerator = new CronExpressionGenerator();
    return cronExpressionGenerator.getCronExpressionFromInput(cronEditorBean);
  }

  @Nullable
  private String getNextRun(@NotNull FilterSubscription subscription) {
    JobDetails jobDetails =
        this.schedulerService.getJobDetails(
            FilterSubscriptionService.getJobId(subscription.getID()));
    return jobDetails == null ? null : CalendarUtils.formatDate(jobDetails.getNextRunTime());
  }
}
