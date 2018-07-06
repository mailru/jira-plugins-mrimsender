package ru.mail.jira.plugins.calendar.service;

import lombok.*;

import java.util.List;

@Getter @Builder @AllArgsConstructor(access = AccessLevel.PRIVATE) @NoArgsConstructor
public class EventBuilderOptions {
    private List<String> fields = null;
    private boolean includeIssueInfo = false;
    private boolean ignoreMissingStart = false;
    private boolean forGantt = false;
}
