(function(factory) {
    factory(moment);
})(function(moment) {
    define('calendar/timeline-view-plugin', ['jquery', 'underscore', 'calendar/edit-type-dialog', 'calendar/timeline-helper'], function($, _, EditTypeDialog, TimelineHelper) {
        var extendStatics = function(d, b) {
            extendStatics = Object.setPrototypeOf ||
                ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
                function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
            return extendStatics(d, b);
        };

        function __extends(d, b) {
            extendStatics(d, b);
            function __() { this.constructor = d; }
            d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
        }

        var TimelineDateProfileGenerator = /** @class */ (function (_super) {
            __extends(TimelineDateProfileGenerator, _super);
            function TimelineDateProfileGenerator() {
                return _super !== null && _super.apply(this, arguments) || this;
            }

            TimelineDateProfileGenerator.prototype.buildRangeFromDayCount = function (date, direction, dayCount) {
                var _a = this.props, dateEnv = _a.dateEnv, dateAlignment = _a.dateAlignment;
                var runningCount = 0;
                var start = date;
                var end;
                if (dateAlignment) {
                    start = dateEnv.startOf(start, dateAlignment);
                }
                start = FullCalendar.startOfDay(start);
                start = this.skipHiddenDays(start, direction);
                end = start;
                while (runningCount <= dayCount) {
                    end = FullCalendar.addDays(end, 1);
                    if (!this.isHiddenDay(end)) {
                        runningCount++;
                    }
                }
                runningCount = 1;
                while (runningCount < dayCount) {
                    start = FullCalendar.addDays(start, -1);
                    if (!this.isHiddenDay(start)) {
                        runningCount++;
                    }
                }
                return { start: start, end: end };
            };
            // Computes the date range that will be rendered.
            TimelineDateProfileGenerator.prototype.buildRenderRange = function (currentRange, currentRangeUnit, isRangeAllDay) {
                var renderRange = _super.prototype.buildRenderRange.call(this, currentRange, currentRangeUnit, isRangeAllDay);
                var start = renderRange.start;
                return this.buildRangeFromDayCount(start, 1, 7);
            };

            TimelineDateProfileGenerator.prototype.setRange = function(currentDateProfile, range) {
                return $.extend(_super.prototype.setRange.call(this, currentDateProfile, range), {
                    changedTimelineRange: true,
                });
            }

            return TimelineDateProfileGenerator;
        }(FullCalendar.DateProfileGenerator));

        var TimelineViewConfig = {
            classNames: ['timeline-view'],
            dateProfileGeneratorClass: TimelineDateProfileGenerator,
            content: function(props) {
                if (TimelineHelper.timeline) {
                    if (!props.dateProfile.hasOwnProperty('changedTimelineRange')) {
                        var start = moment(props.dateProfile.renderRange.start.toISOString(), 'YYYY-MM-DDTHH:mm:ss');
                        var end = moment(props.dateProfile.renderRange.end.toISOString(), 'YYYY-MM-DDTHH:mm:ss');
                        TimelineHelper.timeline.setWindow(start, end);
                    }
                    var events = FullCalendar.sliceEvents(props);
                    TimelineHelper.renderEvents(events);
                }
            },
            didMount: function(props) {
                var start = moment(props.dateProfile.renderRange.start.toISOString(), 'YYYY-MM-DDTHH:mm:ss');
                var end = moment(props.dateProfile.renderRange.end.toISOString(), 'YYYY-MM-DDTHH:mm:ss');
                var events = FullCalendar.sliceEvents(props);
                TimelineHelper.render(start, end, events);
            },
            willUnmount: function() {
                TimelineHelper.destroy();
            },
            getTimelineHelper: function() {
                return TimelineHelper;
            },
        };

        return FullCalendar.createPlugin({
            views: {
                timeline: TimelineViewConfig
            }
        })
    });
});