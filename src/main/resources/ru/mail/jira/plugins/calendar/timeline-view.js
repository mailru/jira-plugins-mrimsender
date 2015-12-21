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

        TimelineView = View.extend({
            defaultInterval: moment.duration(7, 'd'),
            timelineOptions: {
                animateZoom: false,
                minHeight: 400,
                zoomable: false,
                zoomMin: moment.duration(4, 'h').asMilliseconds(),
                zoomMax: moment.duration(6, 'M').asMilliseconds(),
                'MONTHS': [AJS.I18n.getText('ru.mail.jira.plugins.calendar.January'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.February'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.March'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.April'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.May'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.June'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.July'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.August'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.September'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.October'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.November'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.December')],
                'MONTHS_SHORT': [AJS.I18n.getText('ru.mail.jira.plugins.calendar.Jan'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Feb'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Mar'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Apr'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.May'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Jun'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Jul'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Aug'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Sep'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Oct'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Nov'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Dec')],
                'DAYS': [AJS.I18n.getText('ru.mail.jira.plugins.calendar.Sunday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Monday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Tuesday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Wednesday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Thursday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Friday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Saturday')],
                'DAYS_SHORT': [AJS.I18n.getText('ru.mail.jira.plugins.calendar.Sun'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Mon'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Tue'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Wed'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Thu'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Fri'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Sat')],
            },
            initialize: function() {
            },
            _destroy: function() {
                this.eventDialog = undefined;
                this.timeline.deleteAllItems()
                this.timeline = undefined;
            },
            getSelectedEvent: function() {
                var sel = this.timeline.getSelection();
                var row = sel.length ? sel[0].row : undefined;
                return this.events[row];
            },
            render: function() {
                if (!this.timeline) {
                    this.calendar.option('contentHeight', 600);

                    this.timeline = new links.Timeline(this.el[0], this.timelineOptions);
                    $(this.el).on('remove', $.proxy(this._destroy, this));
                    links.events.addListener(this.timeline, 'select', $.proxy(this._onEventSelect, this));
                    links.events.addListener(this.timeline, 'rangechanged', $.proxy(this._onRangeChanged, this));
                    links.events.addListener(this.timeline, 'rangechange', $.proxy(this._onRangeChange, this));
                    this.setRange();
                }
            },
            renderEvents: function(_events) {
                this.events = this._transformToTimelineFormat(_events);
                this.timeline.draw(this.events, null);
            },
            getRangeInterval: function() {
                var diff = 0;
                if(this.timeline) {
                    var visibleRange = this.timeline.getVisibleChartRange();
                    diff = visibleRange.end.getTime() - visibleRange.start.getTime();
                }
                return diff < this.defaultInterval.asMilliseconds() ? this.defaultInterval.asMilliseconds() : diff;
            },
            getVisibleRange: function() {
                if(this.timeline)
                    return this.timeline.getVisibleChartRange();
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
            setRange: function(range) {
                if (!range) {
                    range = this.computeRange();
                } else if (this.timeline)
                    this.timeline.setVisibleChartRange(range.start.toDate(), range.end.toDate());

                this.intervalDuration = moment.duration(range.end.toDate().getTime() - range.start.toDate().getTime());
                View.prototype.setRange.call(this, range);
            },
            zoomOut: function() {
                this.timeline.zoom(-0.3);
                this._onRangeChanged();
            },
            zoomIn: function() {
                this.timeline.zoom(0.3);
                this._onRangeChanged();
            },

            _onRangeChange: function() {
                this.timeline.setSelection([]);
                this.eventDialog && this.eventDialog.hide();
            },
            _onRangeChanged: function() {
                this.setRange();
                this.calendar.date = this.massageCurrentDate(this.calendar.date);
                this.display(this.calendar.date);
                this.calendar.unfreezeContentHeight();
                this.calendar.updateHeaderTitle();
                this.calendar.updateTodayButton();
                this.calendar.getAndRenderEvents();
            },
            _onEventSelect: function() {
                var event = this.getSelectedEvent();
                var target = $(".timeline-event-selected")
                if (!event || event === this.currentSelectedEvent) {
                    this.eventDialog && this.eventDialog.hide();
                    return;
                }
                this.currentSelectedEvent = event;
                this.eventDialog = AJS.InlineDialog(target, "eventDialog", function(content, trigger, showPopup) {
                    $.ajax({
                        type: 'GET',
                        url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/events/' + event.calendarId + '/event/' + event.id + '/info',
                        success: function(issue) {
                            content.html(issueInfoTpl({issue: issue})).addClass('calendar-event-info-popup');
                            showPopup();
                        },
                        error: function(xhr) {
                            var msg = "Error while trying to view info about issue => " + event.id;
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
                        id: event.id,
                        calendarId: event.calendarId,
                        start: event.start.toDate(),
                        end: event.end && event.end.toDate(),
                        content: event.title,
                        className: event.color
                    };
                });
            }
        });

        FC.views.timeline = {
            'class': TimelineView
        };

    });
});