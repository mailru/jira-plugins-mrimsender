package ru.mail.jira.plugins.calendar.service.gantt;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.mail.jira.plugins.calendar.service.Order;

import java.util.List;

@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
public class GanttParams {
    private Order order;
    private String groupBy;
    private Long sprintId;
    private List<String> fields;
}
