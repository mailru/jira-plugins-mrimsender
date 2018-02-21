require(['jquery', 'underscore', 'backbone', 'calendar/non-working-day-configuration-dialog'], function($, _, Backbone, NonWorkingDayConfigurationDialog) { {
    AJS.toInit(function () {
        /* Models and Collections*/
        var NonWorkingDay = Backbone.Model.extend({
            urlRoot: AJS.contextPath() + '/rest/mailrucalendar/1.0/configuration/workingDays/nonWorkingDay',
            idAttribute: 'id'
        });
        var NonWorkingDayCollection = Backbone.Collection.extend({
            model: NonWorkingDay,
            url: AJS.contextPath() + '/rest/mailrucalendar/1.0/configuration/workingDays/nonWorkingDays'
        });

        /* Instances */
        var nonWorkingDayCollection = new NonWorkingDayCollection();

        var MainView = Backbone.View.extend({
            el: 'body',
            events: {
                'change .calendar-working-day' : 'changeWorkingDays',
                'click #calendar-add-date': 'showNonWorkingDayConfigurationDialog',
                'click .non-working-day-delete': 'deleteNonWorkingDay',
                'submit #worktime-form': 'updateWorkTime'
            },
            initialize: function() {
                this.collection.on('request', this.startLoadingScriptsCallback);
                this.collection.on('sync', this.finishLoadingScriptsCallback);
                this.collection.on('add', this._addNonWorkingDay, this);
                this.collection.on('remove', this._removeNonWorkingDay, this);
            },
            startLoadingScriptsCallback: function() {
                AJS.dim();
                JIRA.Loading.showLoadingIndicator();
            },
            finishLoadingScriptsCallback: function() {
                JIRA.Loading.hideLoadingIndicator();
                AJS.undim();
            },
            changeWorkingDays: function(e) {
                var selectedDays = [];
                this.$('input.calendar-working-day:checked').map(function(index, item) { selectedDays.push($(item).attr('id')); });
                $.ajax({
                    type: 'POST',
                    url: AJS.contextPath() + '/rest/mailrucalendar/1.0/configuration/workingDays',
                    data: {
                        days: selectedDays.join(', ')
                    }
                });
            },
            showNonWorkingDayConfigurationDialog: function(e) {
                e.preventDefault();
                e.stopPropagation();

                var nonWorkingDayConfigurationDialog = new NonWorkingDayConfigurationDialog ({
                    model: new NonWorkingDay(),
                    collection: this.collection
                });
                nonWorkingDayConfigurationDialog.show();
            },
            deleteNonWorkingDay: function(e) {
                e.preventDefault();
                e.stopPropagation();
                this.collection.get($(e.currentTarget).parents('.non-working-day').attr('id')).destroy();
            },
            _addNonWorkingDay: function(day) {
                $('#calendar-non-working-days').append(JIRA.Templates.Plugins.MailRuCalendar.Configuration.WorkingDays.nonWorkingDay({day: day.toJSON()}));
            },
            _removeNonWorkingDay: function(day) {
                $('#calendar-non-working-days tr[id="' + day.id + '"]').remove();
                this.finishLoadingScriptsCallback();
            },
            updateWorkTime: function() {
                $.ajax({
                    type: 'POST',
                    url: AJS.contextPath() + '/rest/mailrucalendar/1.0/configuration/workingDays/time',
                    data: {
                        startTime: $("#worktime-start-time").val(),
                        endTime: $("#worktime-end-time").val()
                    },
                    success: function() {
                        AJS.flag({
                            type: 'success',
                            body: 'Updated',
                            close: 'auto'
                        });
                    },
                    error: function(xhr) {
                        AJS.flag({
                            type: 'error',
                            body: xhr.responseText,
                            close: 'auto'
                        });
                    }
                });

                return false; //prevent default submit
            }
        });

        var mainView = new MainView({collection: nonWorkingDayCollection});

        /* Fetch data */
        nonWorkingDayCollection.fetch();
    });
}});