define('calendar/reminder', ['underscore'], function(_) {
    var
        names = {
            'MINUTES_5': AJS.I18n.getText('ru.mail.jira.plugins.calendar.customEvents.reminder.MINUTES_5'),
            'MINUTES_10': AJS.I18n.getText('ru.mail.jira.plugins.calendar.customEvents.reminder.MINUTES_10'),
            'MINUTES_15': AJS.I18n.getText('ru.mail.jira.plugins.calendar.customEvents.reminder.MINUTES_15'),
            'MINUTES_30': AJS.I18n.getText('ru.mail.jira.plugins.calendar.customEvents.reminder.MINUTES_30'),
            'HOURS_1': AJS.I18n.getText('ru.mail.jira.plugins.calendar.customEvents.reminder.HOURS_1'),
            'HOURS_8': AJS.I18n.getText('ru.mail.jira.plugins.calendar.customEvents.reminder.HOURS_8'),
            'DAYS_1': AJS.I18n.getText('ru.mail.jira.plugins.calendar.customEvents.reminder.DAYS_1'),
            'WEEKS_1': AJS.I18n.getText('ru.mail.jira.plugins.calendar.customEvents.reminder.WEEKS_1')
        },
        options = [
            {
                value: 'MINUTES_5'
            },
            {
                value: 'MINUTES_10'
            },
            {
                value: 'MINUTES_15'
            },
            {
                value: 'MINUTES_30'
            },
            {
                value: 'HOURS_1'
            },
            {
                value: 'HOURS_8'
            },
            {
                value: 'DAYS_1'
            },
            {
                value: 'WEEKS_1'
            }
        ];

    _.each(options, function(e) {
        e.name = names[e.value];
    });

    return {
        names: names,
        options: options,
        none: AJS.I18n.getText('ru.mail.jira.plugins.calendar.customEvents.reminder.none')
    }
});