(function(factory) {
    factory(moment);
})(function(moment) {
    define('calendar/timeline-view', ['jquery', 'underscore'], function($, _) {
        var FC = $.fullCalendar;
        var View = FC.View;
        var TimelineView;

        moment.locale('mailru', {
            months: [AJS.I18n.getText('ru.mail.jira.plugins.calendar.January'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.February'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.March'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.April'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.May'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.June'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.July'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.August'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.September'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.October'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.November'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.December')],
            monthsShort: [AJS.I18n.getText('ru.mail.jira.plugins.calendar.Jan'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Feb'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Mar'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Apr'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.May'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Jun'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Jul'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Aug'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Sep'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Oct'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Nov'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Dec')],
            weekdays: [AJS.I18n.getText('ru.mail.jira.plugins.calendar.Sunday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Monday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Tuesday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Wednesday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Thursday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Friday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Saturday')],
            weekdaysShort: [AJS.I18n.getText('ru.mail.jira.plugins.calendar.Sun'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Mon'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Tue'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Wed'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Thu'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Fri'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Sat')],
            weekdaysMin: [AJS.I18n.getText('ru.mail.jira.plugins.calendar.Sun'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Mon'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Tue'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Wed'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Thu'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Fri'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Sat')],
        });

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

        TimelineView = View.extend({
            defaultInterval: moment.duration(7, 'd'),
            timelineOptions: {
                //autoResize: false,
                height: 450,
                multiselect: false,
                zoomable: false,
                zoomMin: moment.duration(4, 'h').asMilliseconds(),
                zoomMax: moment.duration(1.5, 'y').asMilliseconds()
            },
            initialize: function() {
            },
            _destroy: function() {
                $('#inline-dialog-eventTimelineDialog').remove();
                this.eventDialog = undefined;
                this.timeline.off('select');
                this.timeline.off('rangechanged');
                this.timeline.off('rangechange');
                this.timeline = undefined;
            },
            render: function() {
                if (!this.timeline) {
                    this.timeline = new vis.Timeline(this.el[0], null, $.extend(this.timelineOptions, {
                        start: this.start,
                        end: this.end
                    }));
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

            _onRangeChange: function() {
                if (this.eventDialog) {
                    this.eventDialog.hide();
                    this.eventDialog = undefined
                }
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
            _onEventSelect: function(e) {
                var event = this.timeline.itemsData.get(e.items[0]);
                if (!event || !e.items.length || event.id == this.currentSelectedId) {
                    this.eventDialog && this.eventDialog.hide();
                    this.eventDialog = undefined;
                    this.currentSelectedId = undefined;
                    return;
                }
                var target = e.event.target;
                var contextPath = this.options.contextPath;
                var popupWidth = this.options.calendarView.popupWidth;
                this.currentSelectedId = event.id;
                $('#inline-dialog-eventTimelineDialog').remove();

                this.eventDialog = AJS.InlineDialog(target, "eventTimelineDialog", function(content, trigger, showPopup) {
                    $.ajax({
                        type: 'GET',
                        url: AJS.format('{0}/rest/mailrucalendar/1.0/calendar/events/{1}/event/{2}/info', contextPath, event.calendarId, event.eventId),
                        success: function(issue) {
                            content.html(JIRA.Templates.Plugins.MailRuCalendar.issueInfo({
                                issue: issue,
                                contextPath: AJS.contextPath()
                            })).addClass('calendar-event-info-popup');
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
                    width: popupWidth,
                    isRelativeToMouse: true,
                    hideDelay: null,
                    onTop: true,
                    closeOnTriggerClick: true,
                    userLiveEvents: true
                });
                this.eventDialog.show();
            },
            _transformToTimelineFormat: function(events) {
                return _.map(events, $.proxy(function(event) {
                    return {
                        id: event.calendarId + event.id,
                        eventId: event.id,
                        calendarId: event.calendarId,
                        start: event.start.clone().local().toDate(),
                        end: event.end && event.end.clone().local().toDate(),
                        content: event.id + ' ' + event.title,
                        className: this._getClassForColor(event.color)
                    };
                }, this));
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