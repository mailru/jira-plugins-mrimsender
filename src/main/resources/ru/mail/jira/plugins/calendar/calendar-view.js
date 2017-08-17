define('calendar/calendar-view', [
    'jquery',
    'underscore',
    'backbone',
    'calendar/reminder'
], function($, _, Backbone, Reminder) {
    return Backbone.View.extend({
        el: '#calendar-full-calendar',
        initialize: function(options) {
            this.eventSources = {};
            this.contextPath = options && _.has(options, 'contextPath') ? options.contextPath : AJS.contextPath();
            this.customsButtonOptions = options && _.has(options, 'contextPath') ? options.customsButtonOptions : {};
            this.timeFormat = options && _.has(options, 'timeFormat') ? options.timeFormat : AJS.Meta.get('date-time');
            this.dateTimeFormat = JIRA.translateSimpleDateFormat(options.dateTimeFormat || AJS.Meta.get('date-complete'));
            this.dateFormat = JIRA.translateSimpleDateFormat(options.dateFormat || AJS.Meta.get('date-dmy'));
            this.popupWidth = options && _.has(options, 'popupWidth') ? options.popupWidth : 400;
            this.enableFullscreen = options && _.has(options, 'enableFullscreen') ? options.enableFullscreen : false;
            this.disableCustomEventEditing = options && _.has(options, 'disableCustomEventEditing') ? options.disableCustomEventEditing : false;

            var contextPath = this.contextPath;
            var CustomEvent = Backbone.Model.extend({urlRoot: contextPath + '/rest/mailrucalendar/1.0/customEvent/'});
            var self = this;

            this.eventDialog = AJS.InlineDialog('.calendar-event-object,.vis-item', 'eventDialog', function(content, trigger, showPopup) {
                var event;
                if (self.getViewType() === 'timeline') {
                    var timeline = self.getView().timeline;
                    event = timeline.itemsData.get(timeline.getEventProperties({target: trigger}).item);
                    event = {
                        type: event.eventType,
                        id: event.eventId,
                        eventId: event.eventId,
                        calendarId: event.calendarId
                    }
                } else {
                    event = self.$el.fullCalendar('clientEvents', $(trigger).data('event-id'))[0];
                }

                // Atlassian bug workaround
                content.click(function(e) {
                    e.stopPropagation();
                });

                if (event.type === 'ISSUE') {
                    $.ajax({
                        type: 'GET',
                        url: AJS.format('{0}/rest/mailrucalendar/1.0/calendar/events/{1}/event/{2}/info', contextPath, event.calendarId, event.eventId || event.id),
                        success: function (issue) {
                            content.html(JIRA.Templates.Plugins.MailRuCalendar.issueInfo({
                                issue: issue,
                                contextPath: AJS.contextPath()
                            })).addClass('calendar-event-info-popup');
                            showPopup();
                        },
                        error: function (xhr) {
                            var msg = 'Error while trying to view info about issue => ' + event.eventId;
                            if (xhr.responseText)
                                msg += xhr.responseText;
                            alert(msg);
                        }
                    });
                } else if (event.type === 'CUSTOM') {
                    var customEvent = new CustomEvent({id: -1 * parseInt(event.id)});
                    customEvent.fetch({
                        success: function(model) {
                            content.html(JIRA.Templates.Plugins.MailRuCalendar.customEventInfo({
                                event: model.toJSON(),
                                contextPath: AJS.contextPath(),
                                startDateFormatted: moment(model.get('startDate')).format(event.allDay ? self.dateFormat : self.dateTimeFormat),
                                endDateFormatted: moment(model.get('endDate')).format(event.allDay ? self.dateFormat : self.dateTimeFormat),
                                editDisabled: self.disableCustomEventEditing,
                                reminderName: model.get('reminder') ? Reminder.names[model.get('reminder')] : null
                            })).addClass('calendar-event-info-popup');
                            showPopup();

                            content.find('.edit-button').click(function(e) {
                                e.preventDefault();
                                self.trigger('eventEditTriggered', model);
                                self.eventDialog.hide();
                            });

                            content.find('.delete-button').click(function(e) {
                                e.preventDefault();
                                self.trigger('eventDeleteTriggered', model);
                                self.eventDialog.hide();
                            })
                        },
                        error: function(request) {
                            alert(request.responseText);
                        }
                    });
                }
            }, {
                isRelativeToMouse: true,
                cacheContent: false,
                width: this.popupWidth,
                hideDelay: null,
                onTop: true,
                closeOnTriggerClick: true,
                useLiveEvents: true
            });
        },
        _eventSource: function(id) {
            return this.contextPath + '/rest/mailrucalendar/1.0/calendar/events/' + id;
        },
        _getCalendarHeaderButton: function(buttonName) {
            return this.$el.find('.fc-' + buttonName + '-button');
        },
        _onClickWeekendsVisibility: function(e) {
            e.preventDefault();
            this.trigger('changeWeekendsVisibility');
        },
        _toggleFullscreen: function() {
            this.$('.fc-fullscreen-button span.fc-icon').toggleClass('fc-icon-mailrucalendar-icon-fullscreen fc-icon-mailrucalendar-icon-exit-fullscreen');
            this.fullscreenMode = !this.fullscreenMode;
            if (this.fullscreenMode) {
                $('#header,#timezoneDiffBanner,#announcement-banner,.aui-page-header,#studio-header,#footer').slideUp(400);
                $('.aui-page-panel-nav').animate({width: 'toggle', 'padding': 'toggle'}, 400, $.proxy(function() {
                    if (this.getViewType() == 'timeline')
                        this.$el.fullCalendar('getView').timeline.setOptions({height: $(window).height() - 93 + 'px'});
                }, this));
            } else {
                $('#header,#timezoneDiffBanner,#announcement-banner,.aui-page-header,#studio-header,#footer,.aui-page-panel-nav').fadeIn(400);
                $(window).trigger('resize');
                if (this.getViewType() == 'timeline')
                    this.$el.fullCalendar('getView').timeline.setOptions({height: '450px'});
            }
        },
        _canButtonVisible: function(name) {
            return this.customsButtonOptions[name] == undefined || this.customsButtonOptions[name].visible !== false;
        },
        _eventMove: function(event, duration, revertFunc) {
            var start = event.start.toDate();
            var end = event.end && event.end.toDate();
            if (event.type === 'ISSUE') {
                $.ajax({
                    type: 'PUT',
                    url: this.contextPath + '/rest/mailrucalendar/1.0/calendar/events/' + event.calendarId + '/event/' + event.id + '/move',
                    data: {
                        start: moment(start).format(),
                        end: end ? moment(end).format() : ''
                    },
                    error: function (xhr) {
                        var msg = 'Error while trying to drag event. Issue key => ' + event.id;
                        if (xhr.responseText)
                            msg += xhr.responseText;
                        alert(msg);
                        revertFunc();
                    }
                });
            } else if (event.type === 'CUSTOM') {
                var eventId = -1 * parseInt(event.id);

                var startValue = null;
                var endValue = null;
                var allDay = event.allDay;

                if (start) {
                    var momentStart = event.start.clone();
                    if (momentStart.hasTime()) {
                        startValue = momentStart.format('x');
                        allDay = false;
                    } else {
                        startValue = moment.utc(start).format('x');
                        allDay = true;
                    }
                }

                if (end) {
                    var momentEnd = event.end.clone();
                    if (momentEnd.hasTime()) {
                        endValue = momentEnd.format('x');
                    } else {
                        endValue = moment.utc(end).subtract(1, 'days').format('x')
                    }
                }

                $.ajax({
                    type: 'PUT',
                    url: this.contextPath + '/rest/mailrucalendar/1.0/customEvent/' + eventId + '/move',
                    contentType: 'application/json; charset=utf-8',
                    data: JSON.stringify({
                        allDay: allDay,
                        start: startValue,
                        end: endValue
                    }),
                    error: function (xhr) {
                        var msg = 'Error while trying to drag event. Event id => ' + eventId;
                        if (xhr.responseText)
                            msg += xhr.responseText;
                        alert(msg);
                        revertFunc();
                    }
                });
            }
        },
        updateButtonsVisibility: function(view) {
            if (this._canButtonVisible('zoom-out') && this._canButtonVisible('zoom-in') && view.name === 'timeline')
                this._getCalendarHeaderButton('zoom-out').parent('.fc-button-group').show();
            else
                this._getCalendarHeaderButton('zoom-out').parent('.fc-button-group').hide();
            if (this._canButtonVisible('weekend') && (view.name === 'quarter' || view.name === 'month'))
                this._getCalendarHeaderButton('weekend').show();
            else
                this._getCalendarHeaderButton('weekend').hide();
        },
        zoomOutTimeline: function() {
            var view = this.$el.fullCalendar('getView');
            var canZoomOut = view.zoomOut();
            !canZoomOut && view.calendar.header.disableButton('zoom-out');
            view.calendar.header.enableButton('zoom-in');
        },
        zoomInTimeline: function() {
            var view = this.$el.fullCalendar('getView');
            var canZoomIn = view.zoomIn();
            !canZoomIn && view.calendar.header.disableButton('zoom-in');
            view.calendar.header.enableButton('zoom-out');
        },
        init: function(view, hideWeekends) {
            var viewRenderFirstTime = true;
            var contextPath = this.contextPath;
            var self = this;
            var start = localStorage.getItem('mailrucalendar.start');
            var end = localStorage.getItem('mailrucalendar.end');
            this.$el.fullCalendar({
                contentHeight: 'auto',
                defaultView: view,
                defaultDate: start && end ? new Date((moment(start).toDate().getTime() + moment(end).toDate().getTime()) / 2) : undefined,
                header: {
                    left: 'prev,next today',
                    center: 'title',
                    right: 'weekend zoom-out,zoom-in' + (this.enableFullscreen ? ' fullscreen' : '')
                },
                views: {
                    quarter: {
                        type: 'basic',
                        duration: {months: 3}
                    },
                    timeline: {
                        contextPath: contextPath,
                        calendarView: self
                    }
                },
                customButtons: {
                    weekend: {
                        text: hideWeekends ? AJS.I18n.getText('ru.mail.jira.plugins.calendar.showWeekends') : AJS.I18n.getText('ru.mail.jira.plugins.calendar.hideWeekends'),
                        click: $.proxy(this._onClickWeekendsVisibility, this)
                    },
                    'zoom-out': {
                        icon: 'zoom-out',
                        click: $.proxy(this.zoomOutTimeline, this)
                    },
                    'zoom-in': {
                        icon: 'zoom-in',
                        click: $.proxy(this.zoomInTimeline, this)
                    },
                    fullscreen: {
                        icon: 'mailrucalendar-icon-fullscreen',
                        click: $.proxy(this._toggleFullscreen, this)
                    }
                },
                businessHours: {
                    start: '10:00',
                    end: '19:00'
                },
                weekNumberTitle: '',
                weekNumbers: true,
                weekNumberCalculation: 'ISO',
                timezone: 'local',
                timeFormat: this.timeFormat,
                slotLabelFormat: this.timeFormat,
                lazyFetching: true,
                editable: true,
                draggable: true,
                firstDay: 1,
                allDayText: AJS.I18n.getText('ru.mail.jira.plugins.calendar.allDay'),
                monthNames: [AJS.I18n.getText('ru.mail.jira.plugins.calendar.January'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.February'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.March'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.April'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.May'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.June'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.July'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.August'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.September'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.October'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.November'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.December')],
                monthNamesShort: [AJS.I18n.getText('ru.mail.jira.plugins.calendar.Jan'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Feb'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Mar'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Apr'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.May'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Jun'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Jul'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Aug'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Sep'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Oct'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Nov'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Dec')],
                dayNames: [AJS.I18n.getText('ru.mail.jira.plugins.calendar.Sunday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Monday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Tuesday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Wednesday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Thursday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Friday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Saturday')],
                dayNamesShort: [AJS.I18n.getText('ru.mail.jira.plugins.calendar.Sun'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Mon'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Tue'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Wed'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Thu'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Fri'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Sat')],
                buttonText: {
                    today: AJS.I18n.getText('ru.mail.jira.plugins.calendar.today')
                },
                weekends: !hideWeekends,
                weekMode: 'liquid',
                slotWidth: 100,
                slotDuration: '01:00',
                eventRender: function(event, $element) {
                    $element.data('event-id', event.id);
                    $element.addClass('calendar-event-object');
                    if (event.datesError)
                        $element.addClass('calendar-event-dates-error');

                    if (event.type === 'ISSUE') {
                        $element.find('.fc-title').prepend(event.id + ' ');
                        $element.find('.fc-content')
                            .prepend('<span class="jira-issue-status-lozenge aui-lozenge jira-issue-status-lozenge-' + event.statusColor + '">' + AJS.escapeHtml(event.status) + '</span>')
                            .prepend('<img class="calendar-event-issue-type" alt="" height="16" width="16" src="' + AJS.contextPath() + event.issueTypeImgUrl + '" />');
                    } else if (event.type === 'CUSTOM') {
                        if (event.participants) {
                            var formattedParticipants = null;
                            if (event.participants.length === 1) {
                                formattedParticipants = event.participants[0].displayName
                            } else {
                                formattedParticipants = $.map(event.participants, function(e) {
                                    return e.displayName.split(/\s+/)[0];
                                }).join(', ');
                            }
                            $element.find('.fc-title').prepend(AJS.escapeHTML(formattedParticipants) + ': ');
                        }
                        $element.find('.fc-content')
                            .prepend('<span class="calendar-event-issue-type custom-type-icon custom-type-icon-' + event.issueTypeImgUrl + '-cal" /> ');
                    }
                },
                dayClick: $.proxy(function(date, jsEvent, view) {
                    self.trigger('eventCreateTriggered', {
                        allDay: !date.hasTime(),
                        startDate: date.hasTime() ? date.format() : date.format('YYYY-MM-DD')
                    });
                }, this),
                loading: $.proxy(function(isLoading, view) {
                    viewRenderFirstTime = false;
                    this.trigger(isLoading ? 'startLoading' : 'stopLoading', view.name);
                }, this),
                viewRender: $.proxy(function(view) {
                    if (!viewRenderFirstTime)
                        this.trigger('render', view.name);
                    this.updateButtonsVisibility(view);
                    $('.calendar-visible').find('a.calendar-name').addClass('not-active');
                }, this),
                eventAfterAllRender: $.proxy(function() {
                    this.$el.fullCalendar('unfreezeContentHeight');
                    if (viewRenderFirstTime)
                        viewRenderFirstTime = false;
                    else
                        this.trigger('renderComplete');

                    var view = this.$el.fullCalendar('getView');
                    var start = view.start.clone().startOf('month');
                    var end = view.end.clone();
                    for (; start.isBefore(end); start.add(1, 'M')) {
                        this.$('.fc-day.fc-widget-content[data-date=' + start.format('YYYY-MM-DD') + ']').addClass('fc-first-day-of-month');
                    }
                }, this),
                eventDragStart: function(event) {
                    self.eventDialog && self.eventDialog.hide();
                },
                eventDrop: $.proxy(this._eventMove, this),
                eventResize: $.proxy(this._eventMove, this)
            });
        },
        addEventSource: function(calendarId, silent) {
            !silent && this.trigger('addSource', calendarId);
            this.$el.fullCalendar('addEventSource', {
                url: this._eventSource(calendarId),
                success: $.proxy(function() {
                    !silent && this.trigger('addSourceSuccess', calendarId, true);
                }, this)
            });
            this.eventSources['' + calendarId] = this._eventSource(calendarId);
        },
        isCalendarInSources: function(calendarId) {
            return this.eventSources['' + calendarId];
        },
        removeEventSource: function(calendarId) {
            if (this.eventSources['' + calendarId]) {
                this.$el.fullCalendar('removeEventSource', this._eventSource(calendarId));
                this.eventSources = _.omit(this.eventSources, '' + calendarId);
            }
        },
        removeAllEventSource: function() {
            _.each(this.eventSources, function(sourceUrl) {
                this.$el.fullCalendar('removeEventSource', sourceUrl);
            }, this);
            this.eventSources = {};
        },
        setView: function(viewName) {
            this.$el.fullCalendar('changeView', viewName);
        },
        getView: function() {
            return this.$el.fullCalendar('getView');
        },
        getViewType: function() {
            return this.getView().type;
        },
        getNow: function() {
            return this.$el.fullCalendar('getNow');
        },
        toggleWeekends: function(hideWeekends) {
            var view = this.getViewType();
            if (view === 'quarter' || view === 'month') {
                this._getCalendarHeaderButton('weekend').text(hideWeekends ? AJS.I18n.getText('ru.mail.jira.plugins.calendar.showWeekends') : AJS.I18n.getText('ru.mail.jira.plugins.calendar.hideWeekends'));
                this.$el.fullCalendar('option', 'weekends', !hideWeekends);
            }
        },
        reload: function() {
            this.$el.fullCalendar('refetchEvents');
        }
    });
});