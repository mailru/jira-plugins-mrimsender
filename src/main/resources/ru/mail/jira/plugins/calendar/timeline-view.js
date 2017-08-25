(function(factory) {
    factory(moment);
})(function(moment) {
    define('calendar/timeline-view', ['jquery', 'underscore', 'calendar/edit-type-dialog'], function($, _, EditTypeDialog) {
        var FC = $.fullCalendar;
        var View = FC.View;
        var TimelineView;

        // Localize moment.js from JIRA i18n properties
        moment.locale('mailru', {
            months: [AJS.I18n.getText('ru.mail.jira.plugins.calendar.January'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.February'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.March'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.April'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.May'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.June'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.July'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.August'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.September'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.October'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.November'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.December')],
            monthsShort: [AJS.I18n.getText('ru.mail.jira.plugins.calendar.Jan'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Feb'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Mar'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Apr'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.May'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Jun'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Jul'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Aug'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Sep'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Oct'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Nov'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Dec')],
            weekdays: [AJS.I18n.getText('ru.mail.jira.plugins.calendar.Sunday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Monday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Tuesday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Wednesday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Thursday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Friday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Saturday')],
            weekdaysShort: [AJS.I18n.getText('ru.mail.jira.plugins.calendar.Sun'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Mon'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Tue'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Wed'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Thu'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Fri'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Sat')],
            weekdaysMin: [AJS.I18n.getText('ru.mail.jira.plugins.calendar.Sun'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Mon'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Tue'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Wed'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Thu'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Fri'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Sat')],
        });

        // Display today red-line for scale == 'day' and step = 1
        var oldTimeStepGetClassName = vis.TimeStep.prototype.getClassName;
        vis.TimeStep.prototype.getClassName = function() {
            var m = this.moment(this.current);
            var current = m.locale ? m.locale('en') : m.lang('en'); // old versions of moment have .lang() function
            function today(date) {
                return date.isSame(new Date(), 'day') ? ' vis-today' : '';
            }

            var className = oldTimeStepGetClassName.call(this);
            return this.scale == 'day' && this.step == 1 ? className + today(current) + ' vis-' + current.format('dddd').toLowerCase() : className;
        };

        /**
         * Extend Fullcalendar View class to implement Timeline
         */
        TimelineView = View.extend({
            defaultInterval: moment.duration(7, 'd'),
            timelineOptions: {
                //autoResize: false,
                height: 450,
                multiselect: false,
                zoomable: false,
                zoomMin: moment.duration(4, 'h').asMilliseconds(),
                zoomMax: moment.duration(1.5, 'y').asMilliseconds(),
                editable: {
                    add: false,
                    updateTime: true,
                    updateGroup: false,
                    remove: false
                }
            },
            initialize: function() {
            },
            _destroy: function() {
                $('#inline-dialog-eventTimelineDialog').remove();
                this.timeline.off('rangechanged');
                this.timeline.off('rangechange');
                this.timeline = undefined;
            },
            render: function() {
                if (!this.timeline) {
                    this.timeline = new vis.Timeline(this.el[0], null, $.extend(this.timelineOptions, {
                        start: this.start,
                        end: this.end,
                        onMove: $.proxy(this._onMove, this),
                        onMoving: $.proxy(this._onMoving, this)
                    }));
                    $(this.el).on('remove', $.proxy(this._destroy, this));
                    this.timeline.on('rangechanged', $.proxy(this._onRangeChanged, this));
                    this.timeline.on('rangechange', $.proxy(this._onRangeChange, this));
                    this.setRange();
                }
            },
            renderEvents: function(_events) {
                var events = _.map(_events, $.proxy(function(event) {
                    return this._transformEvent(event, false);
                }, this));
                this.timeline.setData({items: events});
            },
            getRangeInterval: function() {
                var diff;
                if (this.timeline) {
                    var visibleRange = this.timeline.getWindow();
                    diff = visibleRange.end.getTime() - visibleRange.start.getTime();
                }
                return diff || this.defaultInterval.asMilliseconds();
            },
            getVisibleRange: function() {
                if (this.timeline)
                    return this.timeline.getWindow();
                return {
                    start: moment().subtract(this.defaultInterval).toDate(),
                    end: moment().add(this.defaultInterval).toDate()
                };
            },
            computeRange: function(date) {
                var intervalStart, intervalEnd, start, end;
                var visibleRange = this.getVisibleRange();
                var diff = this.getRangeInterval();
                if (date) {
                    intervalStart = date.clone().subtract(diff / 2, 'millisecond');
                    intervalEnd = date.clone().add(diff / 2, 'millisecond');
                    start = date.clone().subtract(diff / 2, 'millisecond');
                    end = date.clone().add(diff / 2, 'millisecond');
                } else {
                    intervalStart = $.fullCalendar.moment(visibleRange.start);
                    intervalEnd = $.fullCalendar.moment(visibleRange.end);
                    if (intervalStart.isBefore(this.start) || intervalEnd.isAfter(this.end)) {
                        start = intervalStart.clone().subtract(diff, 'millisecond');
                        end = intervalEnd.clone().add(diff, 'millisecond');
                    } else {
                        start = this.start;
                        end = this.end;
                    }
                }
                return {
                    start: start,
                    intervalStart: intervalStart,
                    end: end,
                    intervalEnd: intervalEnd
                };
            },
            setDate: function(date) {
                if (this.timeline)
                    this.timeline.moveTo(date);
                else
                    View.prototype.setDate.call(this, date);
            },
            setRange: function(range) {
                range = range || this.computeRange();
                this.intervalDuration = moment.duration(range.intervalEnd.toDate().getTime() - range.intervalStart.toDate().getTime());
                View.prototype.setRange.call(this, range);
            },
            zoomOut: function() {
                this.timeline.range.zoom(1.5);
                return this.timelineOptions.zoomMax > this.getRangeInterval();
            },
            zoomIn: function() {
                this.timeline.range.zoom(1 / 1.5);
                return this.timelineOptions.zoomMin < this.getRangeInterval();
            },
            _onMove: function(item, callback) {
                this.options.calendarView.eventDialog && this.options.calendarView.eventDialog.hide();

                var end = item.end;
                var start = item.start;

                if (item.allDay) {
                    start = moment.utc(moment(start).format('YYYY-MM-DD'));
                    if (end) {
                        end = moment.utc(moment(end).format('YYYY-MM-DD'));
                    }
                }

                if (item.eventType === 'ISSUE') {
                    $.ajax({
                        type: 'PUT',
                        url: contextPath + '/rest/mailrucalendar/1.0/calendar/events/' + item.calendarId + '/event/' + item.eventId + '/move',
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
                            callback(this._transformEvent(event, true));
                        }, this)
                    });
                } else if (item.eventType === 'CUSTOM') {
                    var eventId = -1 * parseInt(item.eventId);

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
                                eventId = item.originalId;

                                if (editMode === 'ALL_EVENTS') {
                                    var originalItem = this.timeline.itemsData.get(item.id);

                                    var startDiff = moment(item.start).diff(moment(originalItem.start), 'ms');
                                    var start = moment(item.originalStart).add(startDiff, 'ms');

                                    if (item.allDay) {
                                        data.start = moment.utc(start.format('YYYY-MM-DD')).format('x')
                                    } else {
                                        data.start = moment.format('x');
                                    }

                                    if (data.end) {
                                        var endDiff = moment(item.end).diff(moment(originalItem.end), 'ms');
                                        var end = moment(item.originalEnd).add(endDiff, 'ms');
                                        if (item.allDay) {
                                            data.end = moment.utc(end.format('YYYY-MM-DD')).format('x')
                                        } else {
                                            data.end = moment.format('x');
                                        }
                                    }

                                    console.log(moment(parseInt(data.start)), moment(parseInt(data.end)));

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
                            this.options.calendarView.eventDialog && this.options.calendarView.eventDialog.hide();
                            typeDialog.show();
                        }, this));
                    } else {
                        this._moveCustomEvent(eventId, data, callback);
                    }
                }
            },
            _moveCustomEvent: function(eventId, data, callback) {
                $.ajax({
                    type: 'PUT',
                    url: contextPath + '/rest/mailrucalendar/1.0/customEvent/' + eventId + '/move',
                    contentType: 'application/json; charset=utf-8',
                    data: JSON.stringify(data),
                    error: function (xhr) {
                        var msg = 'Error while trying to drag event. Event id => ' + eventId;
                        if (xhr.responseText)
                            msg += xhr.responseText;
                        alert(msg);
                        callback(null);
                    },
                    success: $.proxy(function (event) {
                        if (data.editMode === 'SINGLE_EVENT') {
                            callback(this._transformEvent(event, true));
                        } else {
                            this.calendar.refetchEvents();
                        }
                    }, this)
                });
            },
            _onMoving: function(item, callback) {
                this.options.calendarView.eventDialog && this.options.calendarView.eventDialog.hide();
                var originalEvent = this.timeline.itemsData.get(item.id);
                if (!originalEvent.startEditable && originalEvent.start.getTime() != item.start.getTime())
                    callback(null);
                else {
                    if (!originalEvent.durationEditable && originalEvent.end)
                        item.end = originalEvent.end.getTime();
                    callback(item);
                }
            },
            _onRangeChange: function() {
                this.options.calendarView.eventDialog && this.options.calendarView.eventDialog.hide();
            },
            _onRangeChanged: function() {
                var _start = this.start;
                var _end = this.end;
                this.setRange();
                this.calendar.date = this.massageCurrentDate(this.calendar.date);
                this.calendar.updateHeaderTitle();
                this.calendar.updateTodayButton();
                if (_start != this.start || _end != this.end) {
                    this.display(this.calendar.date);
                    this.calendar.unfreezeContentHeight();
                    this.calendar.getAndRenderEvents();
                }
            },
            _transformEvent: function(event, raw) {
                var start = event.start;
                var end = event.end;

                if (raw) {
                    start = moment(start);
                    if (end) {
                        end = moment(end);
                    }
                } else {
                    start = start.clone().local();
                    if (end) {
                        end = end.clone().local();
                    }
                }

                return {
                    id: event.calendarId + event.id,
                    eventId: event.id,
                    calendarId: event.calendarId,
                    start: start,
                    end: end,
                    content: this._buildContent(event),
                    className: this._getClassForColor(event.color),
                    style: event.datesError ? 'opacity: 0.4;border-color:#d04437;' : '',
                    startEditable: event.startEditable,
                    durationEditable: event.durationEditable,
                    editable: event.startEditable || event.durationEditable,
                    eventType: event.type,
                    allDay: event.allDay,
                    recurring: event.recurring,
                    recurrenceNumber: event.recurrenceNumber,
                    originalId: event.originalId,
                    originalStart: event.originalStart,
                    originalEnd: event.originalEnd,
                    originalAllDay: event.originalAllDay
                };
            },
            _buildContent: function(event) {
                var content = null;

                if (event.type === 'ISSUE') {
                    content = '<span class="jira-issue-status-lozenge aui-lozenge jira-issue-status-lozenge-' + event.statusColor + '">' + event.status + '</span><span> '+ event.id + ' ' + AJS.escapeHTML(event.title) + '</span>';
                } else if (event.type === 'CUSTOM') {
                    content = '';
                    if (event.participants) {
                        var formattedParticipants = null;
                        if (event.participants.length === 1) {
                            formattedParticipants = event.participants[0].displayName
                        } else {
                            formattedParticipants = $.map(event.participants, function(e) {
                                return e.displayName.split(/\s+/)[0];
                            }).join(', ');
                        }
                        content = AJS.escapeHTML(formattedParticipants) + ': ';
                    }

                    var iconContent = '<span class="calendar-event-issue-type custom-type-icon custom-type-icon-' + event.issueTypeImgUrl + '-cal" /></span>';

                    if (event.recurring) {
                        var editedIcon = '';

                        if (event.parentId) {
                            editedIcon =
                                '<span class="recurring-edited-icon-bg" style="background-color: ' + event.color + '"></span>' +
                                '<span class="aui-icon aui-icon-small aui-iconfont-edit recurring-edited-icon"></span>';
                        }

                        iconContent =
                            '<span>' +
                                iconContent + editedIcon +
                                '<span class="recurring-icon-bg" style="background-color: ' + event.color + '"></span>' +
                                '<span class="aui-icon aui-icon-small aui-iconfont-build recurring-icon"></span>' +
                            '</span>';
                    }

                    content = iconContent + ' ' + content + AJS.escapeHTML(event.title);
                }

                return content
            },
            // todo migrate color from hex to classes
            _getClassForColor: function(color) {
                switch (color) {
                    case '#5dab3e':
                        return 'mailrucalendar-green';
                    case '#d7ad43':
                        return 'mailrucalendar-sand';
                    case '#3e6894':
                        return 'mailrucalendar-blue';
                    case '#c9dad8':
                        return 'mailrucalendar-lightgrey';
                    case '#588e87':
                        return 'mailrucalendar-bluegreen';
                    case '#e18434':
                        return 'mailrucalendar-orange';
                    case '#83382A':
                        return 'mailrucalendar-darkred';
                    case '#D04A32':
                        return 'mailrucalendar-red';
                    case '#3C2B28':
                        return 'mailrucalendar-brown';
                    case '#87A4C0':
                        return 'mailrucalendar-lightblue';
                    case '#A89B95':
                        return 'mailrucalendar-grey';
                    case '#f6c342':
                        return 'mailrucalendar-yellow';
                    case '#205081':
                        return 'mailrucalendar-navy';
                    case '#333333':
                        return 'mailrucalendar-charcoal';
                    case '#707070':
                        return 'mailrucalendar-mediumgray';
                    case '#815b3a':
                        return 'mailrucalendar-mediumbrown';
                    case '#f79232':
                        return 'mailrucalendar-cheetoorange';
                    case '#f1a257':
                        return 'mailrucalendar-tan';
                    case '#d39c3f':
                        return 'mailrucalendar-lightbrown';
                    case '#afe1':
                        return 'mailrucalendar-cyan';
                    case '#4a6785':
                        return 'mailrucalendar-slate';
                    case '#84bbc6':
                        return 'mailrucalendar-coolblue';
                    case '#8eb021':
                        return 'mailrucalendar-limegreen';
                    case '#67ab49':
                        return 'mailrucalendar-midgreen';
                    case '#654982':
                        return 'mailrucalendar-violet';
                    case '#ac707a':
                        return 'mailrucalendar-mauve';
                    case '#f15c75':
                        return 'mailrucalendar-brightpink';
                    case '#f691b2':
                        return 'mailrucalendar-rosie';
                }
            }
        });

        FC.views.timeline = {
            'class': TimelineView
        };
    });
});