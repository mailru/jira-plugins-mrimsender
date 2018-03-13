package ru.mail.jira.plugins.calendar.service;

import com.atlassian.query.order.SortOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor @Getter
public class Order {
    private final String field;
    private final SortOrder order;
}
