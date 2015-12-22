(function(factory) {
    if (typeof define === 'function' && define.amd) {
        define(['jquery', 'moment'], factory);
    }
    else if (typeof exports === 'object') { // Node/CommonJS
        module.exports = factory(require('jquery'), require('moment'));
    }
    else {
        factory(jQuery, moment);
    }
})(function($, moment) {
    AJS.toInit(function() {
        var FC = $.fullCalendar;
        var View = FC.View;
        var TimelineView;
        var issueInfoTpl = _.template($('#issue-info-template').html());

        moment.locale('mailru', {
            months : [AJS.I18n.getText('ru.mail.jira.plugins.calendar.January'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.February'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.March'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.April'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.May'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.June'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.July'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.August'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.September'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.October'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.November'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.December')],
            monthsShort : [AJS.I18n.getText('ru.mail.jira.plugins.calendar.Jan'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Feb'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Mar'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Apr'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.May'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Jun'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Jul'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Aug'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Sep'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Oct'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Nov'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Dec')],
            weekdays : [AJS.I18n.getText('ru.mail.jira.plugins.calendar.Sunday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Monday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Tuesday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Wednesday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Thursday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Friday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Saturday')],
            weekdaysShort : [AJS.I18n.getText('ru.mail.jira.plugins.calendar.Sun'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Mon'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Tue'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Wed'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Thu'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Fri'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Sat')],
            weekdaysMin : [AJS.I18n.getText('ru.mail.jira.plugins.calendar.Sun'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Mon'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Tue'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Wed'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Thu'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Fri'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Sat')],
        });

        TimelineView = View.extend({
            defaultInterval: moment.duration(7, 'd'),
            timelineOptions: {
                minHeight: 550,
                multiselect: false,
                throttleRedraw: 100,
                zoomable: false,
                zoomMin: moment.duration(4, 'h').asMilliseconds(),
                zoomMax: moment.duration(6, 'M').asMilliseconds()
            },
            initialize: function() {
            },
            _destroy: function() {
                this.eventDialog = undefined;
                this.timeline.off('select');
                this.timeline.off('rangechanged');
                this.timeline.off('rangechange');
                this.timeline = undefined;
            },
            render: function() {
                if (!this.timeline) {
                    this.timeline = new vis.Timeline(this.el[0], null, $.extend(this.timelineOptions, {start: this.start, end: this.end}));
                    $(this.el).on('remove', $.proxy(this._destroy, this));
                    this.timeline.on('select', $.proxy(this._onEventSelect, this));
                    this.timeline.on('rangechanged', $.proxy(this._onRangeChanged, this));
                    this.timeline.on('rangechange', $.proxy(this._onRangeChange, this));
                    this.setRange();
                }
            },
            renderEvents: function(_events) {
                var events = this._transformToTimelineFormat(_events);
                this.timeline.setData({items: events})
            },
            getRangeInterval: function() {
                var diff;
                if(this.timeline) {
                    var visibleRange = this.timeline.getWindow();
                    diff = visibleRange.end.getTime() - visibleRange.start.getTime();
                }
                return diff || this.defaultInterval.asMilliseconds();
            },
            getVisibleRange: function() {
                if(this.timeline)
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
                this.timeline.range.zoom(1.3);
            },
            zoomIn: function() {
                this.timeline.range.zoom(0.7);
            },

            _onRangeChange: function() {
                this.eventDialog && this.eventDialog.hide();
            },
            _onRangeChanged: function() {
                var _start = this.start;
                var _end = this.end;
                this.setRange();
                this.calendar.date = this.massageCurrentDate(this.calendar.date);
                this.calendar.updateHeaderTitle();
                this.calendar.updateTodayButton();
                if(_start != this.start || _end != this.end) {
                    this.display(this.calendar.date);
                    this.calendar.unfreezeContentHeight();
                    this.calendar.getAndRenderEvents();
                }
            },
            _onEventSelect: function(e) {
                var event = this.timeline.itemsData.get(e.items[0]);
                if (!event || !e.items.length || (this.currentSelectedEvent && event.id == this.currentSelectedEvent.id)) {
                    this.eventDialog && this.eventDialog.hide();
                    return;
                }
                var target = e.event.target;
                this.currentSelectedEvent = event;
                this.eventDialog = AJS.InlineDialog(target, "eventDialog", function(content, trigger, showPopup) {
                    $.ajax({
                        type: 'GET',
                        url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/events/' + event.calendarId + '/event/' + event.eventId + '/info',
                        success: function(issue) {
                            content.html(issueInfoTpl({issue: issue})).addClass('calendar-event-info-popup');
                            showPopup();
                        },
                        error: function(xhr) {
                            var msg = "Error while trying to view info about issue => " + event.eventId;
                            if (xhr.responseText)
                                msg += xhr.responseText;
                            alert(msg);
                        }
                    });
                    return false;
                }, {
                    isRelativeToMouse: true,
                    hideDelay: null,
                    onTop: true,
                    closeOnTriggerClick: true,
                    userLiveEvents: true
                });
                this.eventDialog.show();
            },
            _transformToTimelineFormat: function(events) {
                return _.map(events, function(event) {
                    return {
                        id: event.calendarId + event.id,
                        eventId: event.id,
                        calendarId: event.calendarId,
                        start: event.start.toDate(),
                        end: event.end && event.end.toDate(),
                        content: event.title,
                        style: AJS.format('background-color: {0};background: {0};border-color: {0};', event.color)
                    };
                });
            }
        });

        FC.views.timeline = {
            'class': TimelineView
        };

    });
});