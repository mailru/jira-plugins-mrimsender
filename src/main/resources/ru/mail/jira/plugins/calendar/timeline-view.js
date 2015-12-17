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
            timelineOptions: {
                animateZoom: false,
                minHeight: 400,
                zoomable: false,
                zoomMin: moment.duration(4, 'h').asMilliseconds(),
                zoomMax: moment.duration(1, 'y').asMilliseconds(),
                'MONTHS': [AJS.I18n.getText('ru.mail.jira.plugins.calendar.January'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.February'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.March'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.April'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.May'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.June'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.July'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.August'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.September'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.October'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.November'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.December')],
                'MONTHS_SHORT': [AJS.I18n.getText('ru.mail.jira.plugins.calendar.Jan'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Feb'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Mar'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Apr'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.May'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Jun'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Jul'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Aug'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Sep'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Oct'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Nov'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Dec')],
                'DAYS': [AJS.I18n.getText('ru.mail.jira.plugins.calendar.Sunday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Monday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Tuesday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Wednesday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Thursday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Friday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Saturday')],
                'DAYS_SHORT': [AJS.I18n.getText('ru.mail.jira.plugins.calendar.Sun'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Mon'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Tue'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Wed'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Thu'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Fri'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Sat')],
            },
            initialize: function() {
            },
            _destroy: function() {
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
                    this.timeline = new links.Timeline(this.el[0], this.timelineOptions);
                    $(this.el).on('remove', $.proxy(this._destroy, this));
                    links.events.addListener(this.timeline, 'select', $.proxy(this._onEventSelect, this));
                    links.events.addListener(this.timeline, 'rangechanged', $.proxy(this._onRangeChanged, this));
                    this.setRange();
                }
            },
            renderEvents: function(_events) {
                this.events = this._transformToTimelineFormat(_events);
                this.timeline.draw(this.events, null);
            },
            computeRange: function(date) {
                if (this.timeline) {
                    var visibleRange = this.timeline.getVisibleChartRange();
                    var start = $.fullCalendar.moment(visibleRange.start);
                    var end = $.fullCalendar.moment(visibleRange.end);
                    if (date) {
                        var diff = visibleRange.end.getTime() - visibleRange.start.getTime();
                        start = date.clone().subtract(diff / 2, 'millisecond');
                        end = date.clone().add(diff / 2, 'millisecond');
                    }
                    return {
                        start: start,
                        intervalStart: start.clone(),
                        end: end,
                        intervalEnd: end.clone()
                    };
                } else
                    return View.prototype.computeRange.call(this, date);
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
                this.timeline.setSelection([]);

                if (!event)
                    return;
                AJS.InlineDialog(target, "eventDialog", function(content, trigger, showPopup) {
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
                }).show();
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