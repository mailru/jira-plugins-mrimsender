require(['jquery', 'backbone'], function($, Backbone) {
    AJS.toInit(function() {
        /* View */
        var MainView = Backbone.View.extend({
            el: '#calendar-schedule',
            events: {
                'change #calendar-schedule-daily': 'checkedDailySchedule',
                'change #calendar-schedule-weekly': 'checkedWeeklySchedule',
                'change #calendar-schedule-monthly': 'checkedMonthlySchedule',
                'change #calendar-schedule-advanced': 'checkedAdvancedSchedule'
            },
            initialize: function() {
                this.initIntervalField();
            },
            initIntervalField: function() {
                $('#calendar-schedule-interval-month-days').auiSelect2();
                $('#calendar-schedule-interval-hours').auiSelect2();
                $('#calendar-schedule-interval-minutes').auiSelect2();

                if ($('.calendar-schedule-type input.radio:checked').length == 0)
                    this.$("#calendar-schedule-daily").prop('checked',true);

                if (this.$('#calendar-schedule-weekly:checked').length == 0)
                    this.$('.calendar-schedule-interval-weekdays').hide();
                if (this.$('#calendar-schedule-monthly:checked').length == 0)
                    this.$('#calendar-schedule-interval-month-days').auiSelect2('data', '').parent('.field-group').hide();
                if (this.$('#calendar-schedule-advanced:checked').length != 0)
                    this.$('.calendar-schedule-interval-time').hide();
                if (this.$('#calendar-schedule-advanced:checked').length == 0)
                    this.$('.calendar-schedule-interval-advanced').hide();
            },
            hideAllIntervals: function() {
                this.$('.calendar-schedule-interval .field-group').hide();
            },
            clearAllIntervals: function() {
                this.$('#calendar-schedule-interval-hours').auiSelect2('data', '');
                this.$('#calendar-schedule-interval-minutes').auiSelect2('data', '');
                this.$('.calendar-schedule-interval-weekdays input:checked').prop("checked", false);
                this.$('#calendar-schedule-interval-month-days').auiSelect2('data', '');
                this.$('#calendar-schedule-interval-advanced').val('');
                this.$('div.error').text('');
            },
            checkedDailySchedule: function() {
                this.hideAllIntervals();
                this.clearAllIntervals();
                this.$('.calendar-schedule-interval-time').show();
            },
            checkedWeeklySchedule: function() {
                this.hideAllIntervals();
                this.clearAllIntervals();
                this.$('.calendar-schedule-interval-time').show();
                this.$('.calendar-schedule-interval-weekdays').show();
            },
            checkedMonthlySchedule: function() {
                this.hideAllIntervals();
                this.clearAllIntervals();
                this.$('.calendar-schedule-interval-time').show();
                this.$('.calendar-schedule-interval-month-days').show();
            },
            checkedAdvancedSchedule: function() {
                this.hideAllIntervals();
                this.clearAllIntervals();
                this.$('.calendar-schedule-interval-advanced').show();
            }
        });

        var mainView = new MainView();
        JIRA.bind(JIRA.Events.NEW_CONTENT_ADDED, function (e, context, reason) {
            if (reason == JIRA.CONTENT_ADDED_REASON.dialogReady && $(context).has('form#calendar-schedule').length)
                new MainView();
        });
    });
});