define('calendar/recurrence', ['underscore'], function(_) {
    var
        names = {
            'DAILY': AJS.I18n.getText('ru.mail.jira.plugins.calendar.customEvents.recurrence.DAILY'),
            'WEEKDAYS': AJS.I18n.getText('ru.mail.jira.plugins.calendar.customEvents.recurrence.WEEKDAYS'),
            'MON_WED_FRI': AJS.I18n.getText('ru.mail.jira.plugins.calendar.customEvents.recurrence.MON_WED_FRI'),
            'TUE_THU': AJS.I18n.getText('ru.mail.jira.plugins.calendar.customEvents.recurrence.TUE_THU'),
            'DAYS_OF_WEEK': AJS.I18n.getText('ru.mail.jira.plugins.calendar.customEvents.recurrence.DAYS_OF_WEEK'),
            'MONTHLY': AJS.I18n.getText('ru.mail.jira.plugins.calendar.customEvents.recurrence.MONTHLY'),
            'YEARLY': AJS.I18n.getText('ru.mail.jira.plugins.calendar.customEvents.recurrence.YEARLY'),
            'CRON': AJS.I18n.getText('ru.mail.jira.plugins.calendar.customEvents.recurrence.CRON')
        },
        periods = {
            'DAILY': 'days',
            'WEEKDAYS': 'weeks',
            'MON_WED_FRI': 'weeks',
            'TUE_THU': 'weeks',
            'DAYS_OF_WEEK': 'weeks',
            'MONTHLY': 'months',
            'YEARLY': 'years'
        },
        options = [
            {
                value: 'DAILY'
            },
            {
                value: 'WEEKDAYS'
            },
            {
                value: 'MON_WED_FRI'
            },
            {
                value: 'TUE_THU'
            },
            {
                value: 'DAYS_OF_WEEK'
            },
            {
                value: 'MONTHLY'
            },
            {
                value: 'YEARLY'
            },
            {
                value: 'CRON'
            }
        ];

    _.each(options, function(e) {
        e.name = names[e.value];
    });

    return {
        names: names,
        options: options,
        none: AJS.I18n.getText('ru.mail.jira.plugins.calendar.customEvents.reminder.none'),
        periodNames: {
            days: AJS.I18n.getText('ru.mail.jira.plugins.calendar.customEvents.recurrence.period.days'),
            weeks: AJS.I18n.getText('ru.mail.jira.plugins.calendar.customEvents.recurrence.period.weeks'),
            months: AJS.I18n.getText('ru.mail.jira.plugins.calendar.customEvents.recurrence.period.months'),
            years: AJS.I18n.getText('ru.mail.jira.plugins.calendar.customEvents.recurrence.period.years')
        },
        periods: periods
    }
});