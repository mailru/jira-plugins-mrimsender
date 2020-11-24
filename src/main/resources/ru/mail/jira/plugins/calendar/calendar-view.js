var mailrucalMoment = window.moment;

define('mailrucal/moment', [], function() {
    return mailrucalMoment;
});

define('calendar/calendar-view', [
    'jquery',
    'underscore',
    'backbone',
    'calendar/reminder',
    'calendar/edit-type-dialog',
    'calendar/recurrence',
    'calendar/preferences',
    'mailrucal/moment',
    'calendar/timeline-view-plugin'

], function($, _, Backbone, Reminder, EditTypeDialog, Recurring, Preferences, moment, TimelineViewPlugin) {
    function getContextPath() {
        if (AJS.gadget) {
            return AJS.gadget.getBaseUrl();
        } else {
            return AJS.contextPath();
        }
    }

    return Backbone.View.extend({
        el: '#calendar-full-calendar',
        initialize: function(options) {
            this.eventSources = {};
            this.contextPath = options && _.has(options, 'contextPath') ? options.contextPath : AJS.contextPath();
            this.customsButtonOptions = options && _.has(options, 'customsButtonOptions') ? options.customsButtonOptions : {};
            this.timeFormat = options && _.has(options, 'timeFormat') ? options.timeFormat : AJS.Meta.get('date-time');
            this.dateTimeFormat = JIRA.translateSimpleDateFormat(options.dateTimeFormat || AJS.Meta.get('date-complete'));
            this.dateFormat = JIRA.translateSimpleDateFormat(options.dateFormat || AJS.Meta.get('date-dmy'));
            this.timezone = 'local';
            this.popupWidth = options && _.has(options, 'popupWidth') ? options.popupWidth : 400;
            this.enableFullscreen = options && _.has(options, 'enableFullscreen') ? options.enableFullscreen : false;
            this.disableCustomEventEditing = options && _.has(options, 'disableCustomEventEditing') ? options.disableCustomEventEditing : false;
        },
        _eventSource: function(id) {
            return this.contextPath + '/rest/mailrucalendar/1.0/calendar/events/' + id;
        },
        _getCalendarHeaderButton: function(buttonName) {
            return this.$el.find('.fc-' + buttonName + '-button');
        },
        _getCustomButtons: function(hideWeekends) {
            return {
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
            }
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
                    this.calendar.updateSize();
                    if (this.getViewType() === 'timeline') {
                        var timeline = this.getTimelineHelper().timeline;
                        timeline.setOptions({height: $(window).height() - 134 + 'px'});
                        timeline.redraw();
                    }
                }, this));
            } else {
                $('#header,#timezoneDiffBanner,#announcement-banner,.aui-page-header,#studio-header,#footer,.aui-page-panel-nav').fadeIn(400);
                this.calendar.updateSize();
                if (this.getViewType() === 'timeline') {
                    var timeline = this.getTimelineHelper().timeline;
                    timeline.setOptions({height: '450px'});
                    timeline.redraw();
                }
            }

        },
        _canButtonVisible: function(name) {
            return this.customsButtonOptions[name] == undefined || this.customsButtonOptions[name].visible !== false;
        },
        _eventDrop: function(params) {
            this._eventMove(params.event, params.delta, params.revert, false);
        },
        _eventResize: function(params) {
            var delta = FullCalendarMoment.toMomentDuration(params.endDelta) - FullCalendarMoment.toMomentDuration(params.startDelta);
            this._eventMove(params.event, delta, params.revert, true);
        },
        _eventMove: function(event, delta, revertFunc, isResize) {
            var start = event.start;
            var end = event.end && event.end;
            if (event.extendedProps.type === 'ISSUE') {
                $.ajax({
                    type: 'PUT',
                    url: this.contextPath + '/rest/mailrucalendar/1.0/calendar/events/' + event.extendedProps.calendarId + '/event/' + event.id + '/move',
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
            } else if (event.extendedProps.type === 'CUSTOM') {
                var eventId = event.extendedProps.originalId;

                var startValue = null;
                var endValue = null;
                var allDay = event.allDay;

                if (start) {
                    var momentStart = moment(event.start).clone();
                    startValue = momentStart.format('x');
                }

                if (end) {
                    var momentEnd = moment(event.end).clone();
                    if (!allDay) {
                        endValue = momentEnd.format('x');
                    } else {
                        endValue = momentEnd.subtract(1, 'days').format('x')
                    }
                }

                var data = {
                    allDay: allDay,
                    start: startValue,
                    end: endValue,
                    editMode: 'SINGLE_EVENT',
                    parentId: null,
                    recurrenceNumber: null
                };

                if (event.extendedProps.recurring) {
                    var typeDialog = new EditTypeDialog({
                        header: "Edit type",
                        okHandler: $.proxy(function(editMode) {
                            data.editMode = editMode;

                            if (editMode === 'SINGLE_EVENT' && !event.extendedProps.parentId) {
                                data.parentId = event.extendedProps.originalId;
                                data.recurrenceNumber = event.extendedProps.recurrenceNumber;
                            }
                            eventId = event.extendedProps.originalId;

                            if (editMode === 'ALL_EVENTS') {
                                var startMoment = moment(event.extendedProps.originalStart);
                                if (isResize) {
                                    data.start = startMoment.format('x');
                                    if (endValue) {
                                        data.end = moment(event.extendedProps.originalEnd || event.extendedProps.originalStart).add(FullCalendarMoment.toMomentDuration(delta)).format('x');
                                    } else {
                                        data.end = null;
                                    }
                                } else {
                                    if (data.allDay && !event.extendedProps.originalAllDay) {
                                        startMoment = moment.utc(startMoment.format('YYYY-MM-DD')).startOf('day');
                                    } else if (!data.allDay && event.extendedProps.originalAllDay) {
                                        startMoment = moment(startMoment.format('YYYY-MM-DD')).local().startOf('day');
                                    }
                                    data.start = startMoment.add(FullCalendarMoment.toMomentDuration(delta)).format('x');
                                    if (endValue) {
                                        var diff = moment.utc(parseInt(endValue)).diff(moment.utc(parseInt(startValue)), 'ms');
                                        data.end = startMoment.clone().add(diff, 'ms').format('x');
                                    } else {
                                        data.end = null;
                                    }
                                }

                                if (event.extendedProps.parentId) {
                                    eventId = event.extendedProps.parentId;
                                }
                            }
                            this._moveCustomEvent(eventId, data, revertFunc);
                        }, this),
                        cancelHandler: function() {
                            revertFunc();
                        }
                    });

                    typeDialog.show();
                } else {
                    this._moveCustomEvent(eventId, data, revertFunc);
                }
            }
        },
        _moveCustomEvent: function(eventId, data, revertFunc) {
            $.ajax({
                type: 'PUT',
                url: this.contextPath + '/rest/mailrucalendar/1.0/customEvent/' + eventId + '/move',
                contentType: 'application/json; charset=utf-8',
                data: JSON.stringify(data),
                success: $.proxy(function(updatedEvent) {
                    if (data.editMode === 'SINGLE_EVENT') {
                        var event = this.calendar.getEventById(updatedEvent.id);
                        event.setStart(updatedEvent.start);
                        event.setEnd(updatedEvent.end);
                        if (this.getViewType() === 'timeline') {
                            revertFunc(this.getTimelineHelper()._transformEvent(event, true));
                        }
                    } else {
                        this.reload();
                    }
                }, this),
                error: function (xhr) {
                    var msg = 'Error while trying to drag event. Event id => ' + eventId;
                    if (xhr.responseText)
                        msg += xhr.responseText;
                    alert(msg);
                    revertFunc(null);
                }
            });
        },
        _formatLocale: function(localeCode) {
            var locale = moment.langData(localeCode);
            return locale === null ? 'en' : locale._abbr;
        },
        updateButtonsVisibility: function(view) {
            if (this._canButtonVisible('zoom-out') && this._canButtonVisible('zoom-in') && view.type === 'timeline')
                this._getCalendarHeaderButton('zoom-out').parent('.fc-button-group').show();
            else
                this._getCalendarHeaderButton('zoom-out').parent('.fc-button-group').hide();
            if (this._canButtonVisible('weekend') && (view.type === 'quarter' || view.type === 'dayGridMonth'))
                this._getCalendarHeaderButton('weekend').show();
            else
                this._getCalendarHeaderButton('weekend').hide();
        },
        zoomOutTimeline: function() {
            var canZoomOut = this.getTimelineHelper().zoomOut();
            !canZoomOut && $('.fc-zoom-out-button').prop('disabled', true);
            $('.fc-zoom-in-button').prop('disabled', false);
        },
        zoomInTimeline: function() {
            var canZoomIn = this.getTimelineHelper().zoomIn();
            !canZoomIn && $('.fc-zoom-in-button').prop('disabled', true);
            $('.fc-zoom-out-button').prop('disabled', false);
        },
        _initEventDialog: function() {
            var contextPath = this.contextPath;
            var CustomEvent = Backbone.Model.extend({urlRoot: contextPath + '/rest/mailrucalendar/1.0/customEvent/'});
            var self = this;
            this.eventDialog = AJS.InlineDialog('.calendar-event-object:not(.holiday-item-content),.vis-item', 'eventDialog', function(content, trigger, showPopup) {
                var event;
                if (self.getViewType() === 'timeline') {
                    var timeline = self.getTimelineHelper().timeline;
                    var timelineEvent = timeline.itemsData.get(timeline.getEventProperties({target: trigger}).item);
                    if (timelineEvent.id) {
                        event = {
                            extendedProps: {
                                type: timelineEvent.eventType,
                                calendarId: timelineEvent.calendarId,
                                recurring: timelineEvent.recurring,
                                recurrenceNumber: timelineEvent.recurrenceNumber,
                                originalId: timelineEvent.originalId,
                            },
                            id: timelineEvent.eventId,
                            eventId: timelineEvent.eventId,
                            allDay: timelineEvent.allDay,
                            start: timelineEvent.start,
                            end: timelineEvent.end
                        }
                    }
                } else {
                    event = self.calendar.getEventById($(trigger).data('event-id'));
                }

                console.log(event, trigger);

                // Atlassian bug workaround
                content.click(function(e) {
                    e.stopPropagation();
                });

                if (!event) {
                    content.html('');
                    showPopup();
                    self._hideEventDialog();
                    return;
                }

                content.html('<span class="aui-icon aui-icon-wait">Loading...</span>');
                showPopup();

                if (event.extendedProps.type === 'ISSUE') {
                    $.ajax({
                        type: 'GET',
                        url: AJS.format('{0}/rest/mailrucalendar/1.0/calendar/events/{1}/event/{2}/info', contextPath, event.extendedProps.calendarId, event.eventId || event.id),
                        success: function (issue) {
                            content.html(JIRA.Templates.Plugins.MailRuCalendar.issueInfo({
                                issue: issue,
                                contextPath: getContextPath()
                            })).addClass('calendar-event-info-popup');
                            self.eventDialog.refresh();
                        },
                        error: function (xhr) {
                            var msg = 'Error while trying to view info about issue => ' + event.id;
                            if (xhr.responseText)
                                msg += xhr.responseText;
                            alert(msg);
                        }
                    });
                } else if (event.extendedProps.type === 'CUSTOM') {
                    var id = event.extendedProps.originalId;
                    var customEvent = new CustomEvent({id: id});
                    customEvent.fetch({
                        success: function(model) {
                            var jsonEvent = model.toJSON();
                            if (event.extendedProps.recurring) {
                                jsonEvent.recurrenceNumber = event.extendedProps.recurrenceNumber;

                                if (!jsonEvent.parentId) {
                                    if (event.allDay) {
                                        jsonEvent.startDate = moment(event.start).format('YYYY-MM-DD');
                                        if (event.end) {
                                            jsonEvent.endDate = moment(event.end).subtract(1, 'days').format('YYYY-MM-DD');
                                        } else {
                                            jsonEvent.endDate = null;
                                        }
                                    } else {
                                        jsonEvent.startDate = parseInt(moment(event.start).format('x'));
                                        if (event.end) {
                                            jsonEvent.endDate = parseInt(moment(event.end).format('x'));
                                        } else {
                                            jsonEvent.endDate = null;
                                        }
                                    }
                                }
                            }

                            content.html(JIRA.Templates.Plugins.MailRuCalendar.customEventInfo({
                                event: jsonEvent,
                                contextPath: getContextPath(),
                                startDateFormatted: event.allDay ? moment.utc(jsonEvent.startDate).format( self.dateFormat) : moment(jsonEvent.startDate).format(self.dateTimeFormat),
                                endDateFormatted: event.allDay ? moment.utc(jsonEvent.endDate).format(self.dateFormat) : moment(jsonEvent.endDate).format(self.dateTimeFormat),
                                editDisabled: self.disableCustomEventEditing,
                                reminderName: jsonEvent.reminder ? Reminder.names[jsonEvent.reminder] : null,
                                recurrenceTypeName: Recurring.names[jsonEvent.recurrenceType],
                                periodName: Recurring.periodNames[Recurring.periods[jsonEvent.recurrenceType]],
                                parentStartDateFormatted: jsonEvent.originalAllDay ? moment.utc(jsonEvent.originalStartDate).format(self.dateFormat) : moment(jsonEvent.originalStartDate).format(self.dateTimeFormat),
                                recurrenceEndDateFormatted: jsonEvent.originalAllDay ? moment.utc(jsonEvent.recurrenceEndDate).format(self.dateFormat) : moment(jsonEvent.recurrenceEndDate).format(self.dateTimeFormat),
                                daysOfWeek: jsonEvent.recurrenceType === 'DAYS_OF_WEEK' ?
                                    jsonEvent.recurrenceExpression.split(',').map(function(dayOfWeek) {
                                        return Recurring.daysOfWeek[dayOfWeek];
                                    }).join(', ') : ''
                            })).addClass('calendar-event-info-popup');
                            self.eventDialog.refresh();

                            content.find('.edit-button').click(function(e) {
                                e.preventDefault();
                                self.trigger('eventEditTriggered', model, jsonEvent);
                                self._hideEventDialog();
                            });

                            content.find('.delete-button').click(function(e) {
                                e.preventDefault();
                                self.trigger('eventDeleteTriggered', model);
                                self._hideEventDialog();
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
        _hideEventDialog: function() {
            this.eventDialog && this.eventDialog.hide();
        },
        _onMoving: function(item, callback) {
            this._hideEventDialog();
            var originalEvent = this.getTimelineHelper().timeline.itemsData.get(item.id);
            if (!originalEvent.startEditable && originalEvent.start.getTime() !== item.start.getTime())
                callback(null);
            else {
                if (!originalEvent.durationEditable && originalEvent.end)
                    item.end = originalEvent.end.getTime();
                callback(item);
            }
        },
        _onMove: function(item, callback) {
            this._hideEventDialog();

            var end = item.end;
            var start = item.start;

            var timelineHelper = this.getTimelineHelper();

            if (item.allDay) {
                start = moment.utc(moment(start).format('YYYY-MM-DD'));
                if (end) {
                    end = moment.utc(moment(end).format('YYYY-MM-DD'));
                }
            }

            if (item.eventType === 'ISSUE') {
                $.ajax({
                    type: 'PUT',
                    url: this.options.calendarView.contextPath + '/rest/mailrucalendar/1.0/calendar/events/' + item.calendarId + '/event/' + item.eventId + '/move',
                    data: {
                        start: moment(start).format(),
                        end: end ? moment(end).format() : ''
                    },
                    error: function (xhr) {
                        var msg = 'Error while trying to drag event. Issue key => ' + item.eventId;
                        if (xhr.responseText)
                            msg += xhr.responseText;
                        alert(msg);
                        callback(null);
                    },
                    success: $.proxy(function (event) {
                        if (event.groups && event.groups.length) {
                            this.reload();
                        } else {
                            callback(timelineHelper._transformEvent(event, true));
                        }
                    }, this)
                });
            } else if (item.eventType === 'CUSTOM') {
                var eventId = item.originalId;

                var data = {
                    allDay: item.allDay,
                    start: !item.allDay ? moment(start).format('x') : moment.utc(start).format('x'),
                    end: end ? !item.allDay ? moment(end).format('x') : moment.utc(end).subtract(1, 'days').format('x') : null,
                    editMode: 'SINGLE_EVENT',
                    parentId: null,
                    recurrenceNumber: null
                };

                if (item.recurring) {
                    var typeDialog = new EditTypeDialog({
                        header: "Edit type",
                        okHandler: $.proxy(function(editMode) {
                            data.editMode = editMode;

                            if (editMode === 'SINGLE_EVENT' && !item.parentId) {
                                data.parentId = item.originalId;
                                data.recurrenceNumber = item.recurrenceNumber;
                            }

                            if (editMode === 'ALL_EVENTS') {
                                var originalItem = timelineHelper.timeline.itemsData.get(item.id);

                                var startDiff = moment(item.start).diff(moment(originalItem.start), 'ms');
                                var start = moment(item.originalStart).add(startDiff, 'ms');

                                if (item.allDay) {
                                    data.start = moment.utc(start.format('YYYY-MM-DD')).format('x')
                                } else {
                                    data.start = start.format('x');
                                }

                                if (data.end) {
                                    var endDiff = moment(item.end).diff(moment(originalItem.end), 'ms');
                                    var end = moment(item.originalEnd).add(endDiff, 'ms');
                                    if (item.allDay) {
                                        data.end = moment.utc(end.format('YYYY-MM-DD')).format('x')
                                    } else {
                                        data.end = end.format('x');
                                    }
                                }

                                if (item.parentId) {
                                    eventId = item.parentId;
                                }
                            }

                            this._moveCustomEvent(eventId, data, callback);
                        }, this),
                        cancelHandler: function() {
                            callback(null);
                        }
                    });

                    _.defer($.proxy(function() {
                        this._hideEventDialog();
                        typeDialog.show();
                    }, this));
                } else {
                    this._moveCustomEvent(eventId, data, callback);
                }
            }
        },
        _onRangeChanged: function() {
            this.calendar.setRange(this.getTimelineHelper().getVisibleRange());
            this.reload();
        },
        _initTimelineGroupPicker: function(){
            var contextPath = this.contextPath;
            $.getJSON(contextPath + '/rest/mailrucalendar/1.0/calendar/config/applicationStatus', $.proxy(function(data) {
                this.getTimelineHelper()._doInitGroupPicker(data.SOFTWARE);
                var $groupByField = $('#calendar-group-by-field');
                $groupByField.change($.proxy(function() {
                    var value = $groupByField.val();
                    if (value === 'none') {
                        $groupByField.auiSelect2('val', null);
                        value = null;
                    }
                    Preferences.setItem('groupBy', $groupByField.val());
                    this.reload();
                }, this));
            }, this));
        },
        init: function(view, hideWeekends, workingDays, start, end) {
            var viewRenderFirstTime = true;
            var self = this;
            this.calendar = new FullCalendar.Calendar(this.$el[0], {
                contentHeight: 'auto',
                initialView : view,
                initialDate: start && end ? new Date((moment(start).toDate().getTime() + moment(end).toDate().getTime()) / 2) : undefined,
                headerToolbar: {
                    left: 'prev,next today',
                    center: 'title',
                    right: 'weekend zoom-out,zoom-in' + (this.enableFullscreen ? ' fullscreen' : '')
                },
                plugins: [ TimelineViewPlugin],
                views: {
                    quarter: {
                        type: 'dayGrid',
                        duration: {months: 3}
                    },
                    timeline: {
                        type: 'timeline',
                        duration: { days: 14 },
                    }
                },
                customButtons: this._getCustomButtons(hideWeekends),
                businessHours: {
                    daysOfWeek: workingDays,
                    start: '10:00',
                    end: '19:00'
                },
                weekText: '',
                weekNumbers: true,
                weekNumberCalculation: 'local',
                locale: this._formatLocale(AJS.Meta.get('user-locale')),
                timezone: self.timezone,
                eventTimeFormat: this.timeFormat,
                lazyFetching: true,
                editable: true,
                draggable: true,
                firstDay: AJS.Meta.get('mailrucal-use-iso8601') ? 1 : 0,
                allDayContent: AJS.I18n.getText('ru.mail.jira.plugins.calendar.allDay'),
                buttonText: {
                    today: AJS.I18n.getText('ru.mail.jira.plugins.calendar.today')
                },
                weekends: !hideWeekends,
                fixedWeekCount: false,
                slotMinWidth: 100,
                slotDuration: '01:00',
                viewDidMount: $.proxy(function(options) {
                    var view = options.view;
                    if (view.type === 'timeline') {
                        var timeline = this.getTimelineHelper().timeline;
                        timeline.setOptions({
                            onMove: $.proxy(this._onMove, this),
                            onMoving: $.proxy(this._onMoving, this),
                            orientation: 'top'
                        });
                        timeline.on('select', $.proxy(function(properties) {
                            var $calendar = $("#calendar-full-calendar, #mailru-calendar-gadget-full-calendar");
                            if (properties.items && properties.items.length) {
                                $calendar.addClass('no-tooltips');
                            } else {
                                $calendar.removeClass('no-tooltips');
                            }
                        }, this));
                        timeline.on('rangechanged', $.proxy(this._onRangeChanged, this));
                        timeline.on('rangechange', $.proxy(this._hideEventDialog, this));

                        this._initTimelineGroupPicker();
                    }
                }, this),
                eventSources: [{url: this.contextPath + '/rest/mailrucalendar/1.0/calendar/events/holidays'}],
                eventDidMount: function(options) {
                    var $element = $(options.el);
                    var event = options.event;

                    $element.data('event-id', event.id);
                    $element.addClass('calendar-event-object');
                    if (event.extendedProps.datesError)
                        $element.addClass('calendar-event-dates-error');

                    if (event.extendedProps.type === 'ISSUE') {
                        $element.find('.fc-event-title').prepend(event.id + ' ');
                        var $eventContent = $element.find('.fc-event-title');
                        if (event.extendedProps.status)
                            $eventContent.prepend('<span class="jira-issue-status-lozenge aui-lozenge jira-issue-status-lozenge-' + event.extendedProps.statusColor + '">' + AJS.escapeHtml(event.extendedProps.status) + '</span>');
                        if (event.extendedProps.priorityImgUrl) {
                            $eventContent.prepend('<img class="calendar-event-issue-type" alt="" height="16" width="16" src="' + getContextPath() + event.extendedProps.priorityImgUrl + '" />');
                        }
                        $eventContent.prepend('<img class="calendar-event-issue-type" alt="" height="16" width="16" src="' + getContextPath() + event.extendedProps.issueTypeImgUrl + '" />');
                    } else if (event.extendedProps.type === 'CUSTOM') {
                        if (event.extendedProps.participants) {
                            var formattedParticipants = null;
                            if (event.extendedProps.participants.length === 1) {
                                formattedParticipants = event.extendedProps.participants[0].displayName
                            } else {
                                formattedParticipants = $.map(event.extendedProps.participants, function(e) {
                                    return e.displayName.split(/\s+/)[0];
                                }).join(', ');
                            }
                            $element.find('.fc-event-title-container').prepend(AJS.escapeHTML(formattedParticipants) + ': ');
                        }

                        var iconContent = '<span class="calendar-event-issue-type custom-type-icon custom-type-icon-' + event.extendedProps.issueTypeImgUrl + '-cal" />';

                        if (event.extendedProps.recurring) {
                            var editedIcon = '';

                            if (event.extendedProps.parentId) {
                                editedIcon =
                                    '<span class="recurring-edited-icon-bg" style="background-color: ' + event.backgroundColor + '"></span>' +
                                    '<span class="aui-icon aui-icon-small aui-iconfont-edit recurring-edited-icon"></span>';
                            }

                            iconContent =
                                '<span>' +
                                    iconContent + editedIcon +
                                    '<span class="recurring-icon-bg" style="background-color: ' + event.backgroundColor + '"></span>' +
                                    '<span class="aui-icon aui-icon-small aui-iconfont-build recurring-icon"></span>' +
                                '</span>';
                        }

                        $element.find('.fc-event-title-container').prepend(iconContent + ' ');
                    } else if (event.extendedProps.type === 'HOLIDAY') {
                        $element.append(event.title).addClass('holiday-item-content');
                    }
                },
                dateClick: $.proxy(function(params) {
                    var date = moment(params.date);
                    self.trigger('eventCreateTriggered', {
                        allDay: !params.allDay,
                        startDate: params.allDay ? date.format() : date.format('YYYY-MM-DD')
                    });
                }, this),
                loading: $.proxy(function(isLoading) {
                    viewRenderFirstTime = false;
                    if (!isLoading) {
                        this.trigger('renderComplete');
                        var view = this.getView();
                        var start = moment(view.activeStart).clone().startOf('month');
                        var end = moment(view.activeEnd).clone();
                        for (; start.isBefore(end); start.add(1, 'M')) {
                            this.$('.fc-day.fc-daygrid-day[data-date=' + start.format('YYYY-MM-DD') + ']').addClass('fc-first-day-of-month');
                        }
                    }
                }, this),
                datesSet: $.proxy(function(params) {
                    var view = params.view;
                    if (!viewRenderFirstTime)
                        this.trigger('render', view.type);
                    this.updateButtonsVisibility(view);
                    $('.calendar-visible').find('a.calendar-name').addClass('not-active');
                }, this),
                eventDragStart: function(params) {
                    self._hideEventDialog();
                },
                eventDrop: $.proxy(this._eventDrop, this),
                eventResize: $.proxy(this._eventResize, this)
            });
            this.calendar.render();

            this._initEventDialog();
        },
        addEventSource: function(calendarId, silent) {
            !silent && this.trigger('addSource', calendarId);
            this.calendar.addEventSource({
                id: calendarId,
                url: this._eventSource(calendarId),
                success: $.proxy(function() {
                    !silent && this.trigger('addSourceSuccess', calendarId, true);
                }, this),
                extraParams: $.proxy(function() {
                    return {
                        groupBy: Preferences.getItem('groupBy')
                    };
                }, this)
            });
            this.eventSources['' + calendarId] = this._eventSource(calendarId);
        },
        isCalendarInSources: function(calendarId) {
            return this.eventSources['' + calendarId];
        },
        removeEventSource: function(calendarId) {
            if (this.eventSources['' + calendarId]) {
                this.calendar.getEventSourceById(calendarId).remove();
                this.eventSources = _.omit(this.eventSources, '' + calendarId);
                this.trigger('renderComplete');
            }
        },
        removeAllEventSource: function() {
            _.each(this.eventSources, function(sourceUrl, calendarId) {
                this.calendar.getEventSourceById(calendarId).remove();
            }, this);
            this.eventSources = {};
        },
        setView: function(viewName) {
            this.calendar.changeView(viewName);
            this.trigger('renderComplete');
        },
        getView: function() {
            return this.calendar.view;
        },
        getViewType: function() {
            return this.getView().type;
        },
        getTimelineHelper: function() {
            return this.getView().getCurrentData().viewSpec.optionDefaults.getTimelineHelper();
        },
        getNow: function() {
            return this.calendar.currentData.dateProfileGenerator.nowDate;
        },
        computeRange: function(date) {
            return this.calendar.currentData.dateProfileGenerator.buildCurrentRangeInfo(date).range;
        },
        toggleWeekends: function(hideWeekends) {
            var view = this.getViewType();
            if (view === 'quarter' || view === 'dayGridMonth') {
                this.calendar.setOption('weekends', !hideWeekends);
                this.calendar.setOption('customButtons', this._getCustomButtons(!hideWeekends));
                this.trigger('renderComplete');
            }
        },
        reload: function() {
            this.calendar.refetchEvents();
        },
        setTimezone: function(timezone) {
            this.timezone = timezone;
        }
    });
});
