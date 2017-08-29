define('calendar/recurrence', ['underscore'], function(_) {
    var
        names = {
            'DAILY': AJS.I18n.getText('ru.mail.jira.plugins.calendar.customEvents.recurring.DAILY'),
            'WEEKDAYS': AJS.I18n.getText('ru.mail.jira.plugins.calendar.customEvents.recurring.WEEKDAYS'),
            'MON_WED_FRI': AJS.I18n.getText('ru.mail.jira.plugins.calendar.customEvents.recurring.MON_WED_FRI'),
            'TUE_THU': AJS.I18n.getText('ru.mail.jira.plugins.calendar.customEvents.recurring.TUE_THU'),
            'DAYS_OF_WEEK': AJS.I18n.getText('ru.mail.jira.plugins.calendar.customEvents.recurring.DAYS_OF_WEEK'),
            'MONTHLY': AJS.I18n.getText('ru.mail.jira.plugins.calendar.customEvents.recurring.MONTHLY'),
            'YEARLY': AJS.I18n.getText('ru.mail.jira.plugins.calendar.customEvents.recurring.YEARLY'),
            'CRON': AJS.I18n.getText('ru.mail.jira.plugins.calendar.customEvents.recurring.CRON')
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
            days: AJS.I18n.getText('ru.mail.jira.plugins.calendar.customEvents.recurring.period.days'),
            weeks: AJS.I18n.getText('ru.mail.jira.plugins.calendar.customEvents.recurring.period.weeks'),
            months: AJS.I18n.getText('ru.mail.jira.plugins.calendar.customEvents.recurring.period.months'),
            years: AJS.I18n.getText('ru.mail.jira.plugins.calendar.customEvents.recurring.period.years')
        },
        periods: periods,
        shortDaysOfWeek: {
            MONDAY: AJS.I18n.getText('ru.mail.jira.plugins.calendar.customEvents.recurring.daysOfWeek.short.MONDAY'),
            TUESDAY: AJS.I18n.getText('ru.mail.jira.plugins.calendar.customEvents.recurring.daysOfWeek.short.TUESDAY'),
            WEDNESDAY: AJS.I18n.getText('ru.mail.jira.plugins.calendar.customEvents.recurring.daysOfWeek.short.WEDNESDAY'),
            THURSDAY: AJS.I18n.getText('ru.mail.jira.plugins.calendar.customEvents.recurring.daysOfWeek.short.THURSDAY'),
            FRIDAY: AJS.I18n.getText('ru.mail.jira.plugins.calendar.customEvents.recurring.daysOfWeek.short.FRIDAY'),
            SATURDAY: AJS.I18n.getText('ru.mail.jira.plugins.calendar.customEvents.recurring.daysOfWeek.short.SATURDAY'),
            SUNDAY: AJS.I18n.getText('ru.mail.jira.plugins.calendar.customEvents.recurring.daysOfWeek.short.SUNDAY')
        },
        daysOfWeek: {
            MONDAY: AJS.I18n.getText('ru.mail.jira.plugins.calendar.Monday'),
            TUESDAY: AJS.I18n.getText('ru.mail.jira.plugins.calendar.Tuesday'),
            WEDNESDAY: AJS.I18n.getText('ru.mail.jira.plugins.calendar.Wednesday'),
            THURSDAY: AJS.I18n.getText('ru.mail.jira.plugins.calendar.Thursday'),
            FRIDAY: AJS.I18n.getText('ru.mail.jira.plugins.calendar.Friday'),
            SATURDAY: AJS.I18n.getText('ru.mail.jira.plugins.calendar.Saturday'),
            SUNDAY: AJS.I18n.getText('ru.mail.jira.plugins.calendar.Sunday')
        }
    }
});