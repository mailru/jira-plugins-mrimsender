require(['jquery', 'backbone'], function($, Backbone) {
    AJS.toInit(function() {
        /* Models */
        var Schedule = Backbone.Model.extend();

        /* Collections */
        var ScheduleCollection = Backbone.Collection.extend({
            model: Schedule,
            url: AJS.contextPath() + '/rest/mailrucalendar/1.0/schedule'
        });

        /* Instances */
        var scheduleCollection = new ScheduleCollection();

        /* View */
        var MainView = Backbone.View.extend({
            el: 'body#jira',
            events: {
                'click .calendar-schedule-run': 'runSchedule'
            },
            initialize: function() {
                this.collection.on('add', this._addSchedule, this);
                this.collection.on('request', this.startLoadingCallback);
                this.collection.on('sync', this.finishLoadingCallback);
            },
            startLoadingCallback: function() {
                AJS.dim();
                JIRA.Loading.showLoadingIndicator();
            },
            finishLoadingCallback: function() {
                JIRA.Loading.hideLoadingIndicator();
                AJS.undim();
            },
            runSchedule: function(e){
                e.preventDefault();
                var scheduleId = this.$(e.currentTarget).parents('.calendar-schedule-buttons-dropdown').data('id');
                this.startLoadingCallback();
                $.ajax({
                    type: 'GET',
                    url: AJS.contextPath() + '/rest/mailrucalendar/1.0/schedule/clone/' + scheduleId,
                    success: function() {
                        location.reload();
                    },
                    error: function(xhr) {
                        mainView.finishLoadingCallback();
                        alert(xhr.responseText);
                    }
                });
            },
            _addSchedule: function(schedule) {
                $('#calendar-schedules-list').append(JIRA.Templates.Plugins.MailRuCalendar.Schedule.scheduleEntry({schedule: schedule.toJSON(),contextPath: AJS.contextPath()}));
            }
        });

        var mainView = new MainView({collection: scheduleCollection});

        /* Fetch data */
        scheduleCollection.fetch();
    });
});