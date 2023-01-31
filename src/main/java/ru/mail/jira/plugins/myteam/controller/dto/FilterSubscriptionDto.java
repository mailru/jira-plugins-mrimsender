/* (C)2022 */
package ru.mail.jira.plugins.myteam.controller.dto;

import java.util.List;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;
import ru.mail.jira.plugins.commons.dto.jira.UserDto;
import ru.mail.jira.plugins.myteam.controller.validation.annotation.ChatValidation;
import ru.mail.jira.plugins.myteam.controller.validation.annotation.ConditionalValidation;
import ru.mail.jira.plugins.myteam.controller.validation.annotation.CronValidation;
import ru.mail.jira.plugins.myteam.db.model.FilterSubscriptionType;
import ru.mail.jira.plugins.myteam.db.model.RecipientsType;

@SuppressWarnings("NullAway")
@Getter
@Setter
@NoArgsConstructor
@ConditionalValidation(
    conditionalProperty = "recipientsType",
    values = {"USER"},
    requiredProperties = {"users"})
@ConditionalValidation(
    conditionalProperty = "recipientsType",
    values = {"GROUP"},
    requiredProperties = {"groups"})
@ConditionalValidation(
    conditionalProperty = "recipientsType",
    values = {"CHAT"},
    requiredProperties = {"chats"})
@ConditionalValidation(
    conditionalProperty = "scheduleMode",
    values = {"daily"},
    requiredProperties = {"hours", "minutes"})
@ConditionalValidation(
    conditionalProperty = "scheduleMode",
    values = {"daysOfWeek"},
    requiredProperties = {"hours", "minutes", "weekDays"})
@ConditionalValidation(
    conditionalProperty = "scheduleMode",
    values = {"daysOfMonth"},
    requiredProperties = {"hours", "minutes", "monthDay"})
@ConditionalValidation(
    conditionalProperty = "scheduleMode",
    values = {"advanced"},
    requiredProperties = {"advanced"})
public class FilterSubscriptionDto {
  @XmlElement private Integer id;

  @NotNull @XmlElement private JqlFilterDto filter;

  @Nullable @XmlElement private UserDto creator;

  @NotNull @XmlElement private RecipientsType recipientsType;

  @Nullable @XmlElement private List<UserDto> users;

  @Nullable @XmlElement private List<String> groups;

  @ChatValidation @Nullable @XmlElement private List<String> chats;

  @NotNull @XmlElement private String scheduleMode;

  @Nullable @XmlElement private String scheduleDescription;

  @Nullable @XmlElement private Integer hours;

  @Nullable @XmlElement private Integer minutes;

  @Nullable @XmlElement private List<String> weekDays;

  @Nullable @XmlElement private Integer monthDay;

  @CronValidation @Nullable @XmlElement private String advanced;

  @Nullable @XmlElement private String lastRun;

  @Nullable @XmlElement private String nextRun;

  @NotNull @XmlElement private FilterSubscriptionType type;

  @XmlElement private boolean emailOnEmpty;

  @XmlElement private boolean separateIssues;
}
