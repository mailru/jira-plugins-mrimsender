(function(factory) {
    factory(moment);
})(function(moment) {
    function getContextPath() {
        if (AJS.gadget) {
            return AJS.gadget.getBaseUrl();
        } else {
            return AJS.contextPath();
        }
    }

    var DEFAULT_GROUP = 'zzz-default';

    var groupFieldNames = {
        "assignee": AJS.I18n.getText('issue.field.assignee'),
        "reporter": AJS.I18n.getText('issue.field.reporter'),
        "issueType": AJS.I18n.getText('issue.field.issuetype'),
        "priority": AJS.I18n.getText('issue.field.priority'),
        "project": AJS.I18n.getText('issue.field.project'),
        "component": AJS.I18n.getText('issue.field.component'),
        "fixVersion": AJS.I18n.getText('issue.field.fixversion'),
        "affectsVersion": AJS.I18n.getText('issue.field.affectsversions'),
        "labels": AJS.I18n.getText('issue.field.labels'),
        "epicLink": AJS.I18n.getText('gh.epic.link.name'),
        "resolution": AJS.I18n.getText('issue.field.resolution')
    };

    var groupAvatarClass = {
        'assignee': 'timeline-group-avatar',
        'reporter': 'timeline-group-avatar',
        'project': 'timeline-group-avatar',
        'issueType': 'timeline-group-avatar-sm',
        'priority': 'timeline-group-avatar-sm'
    };

    define('calendar/timeline-helper', ['jquery', 'underscore', 'calendar/edit-type-dialog', 'calendar/preferences'], function($, _, EditTypeDialog, Preferences) {
        // Localize moment.js from JIRA i18n properties
        moment.locale('mailru', {
            months: [AJS.I18n.getText('ru.mail.jira.plugins.calendar.January'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.February'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.March'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.April'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.May'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.June'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.July'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.August'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.September'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.October'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.November'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.December')],
            monthsShort: [AJS.I18n.getText('ru.mail.jira.plugins.calendar.Jan'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Feb'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Mar'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Apr'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.May'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Jun'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Jul'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Aug'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Sep'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Oct'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Nov'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Dec')],
            weekdays: [AJS.I18n.getText('ru.mail.jira.plugins.calendar.Sunday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Monday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Tuesday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Wednesday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Thursday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Friday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Saturday')],
            weekdaysShort: [AJS.I18n.getText('ru.mail.jira.plugins.calendar.Sun'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Mon'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Tue'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Wed'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Thu'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Fri'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Sat')],
            weekdaysMin: [AJS.I18n.getText('ru.mail.jira.plugins.calendar.Sun'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Mon'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Tue'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Wed'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Thu'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Fri'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Sat')],
        });

        // Display today red-line for scale == 'day' and step = 1
        /*var oldTimeStepGetClassName = vis.TimeStep.prototype.getClassName;
        vis.TimeStep.prototype.getClassName = function() {
            var m = this.moment(this.current);
            var current = m.locale ? m.locale('en') : m.lang('en'); // old versions of moment have .lang() function
            function today(date) {
                return date.isSame(new Date(), 'day') ? ' vis-today' : '';
            }

            var className = oldTimeStepGetClassName.call(this);
            return this.scale == 'day' && this.step == 1 ? className + today(current) + ' vis-' + current.format('dddd').toLowerCase() : className;
        };*/ //todo


        var TimelineHelper = function() {
            this.timeline = undefined;
            this.defaultInterval = moment.duration(14, 'd');
            this.timelineOptions = {
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
                },
                format: {
                    minorLabels: {
                        millisecond:'SSS',
                        second:     's',
                        minute:     'HH:mm',
                        hour:       'HH:mm',
                        weekday:    'ddd D',
                        day:        'D MMM',
                        week:       'w',
                        month:      'MMM YY',
                        year:       'YYYY'
                    }
                },
                verticalScroll: true,
                zoomKey: 'ctrlKey',
                groupOrder: 'id',
                showMajorLabels: false,
            };
        }

        TimelineHelper.prototype.setTimeline = function(timeline) {
            this.timeline = timeline;
        }

        TimelineHelper.prototype.destroy = function() {
            $('#inline-dialog-eventTimelineDialog').remove();
            this.timeline.off('rangechanged');
            this.timeline.off('rangechange');
            this.timeline.off('select');
            this.timeline = undefined;
            $("#calendar-full-calendar, #mailru-calendar-gadget-full-calendar").removeClass('no-tooltips');
        }

        TimelineHelper.prototype.render = function(start, end, events) {
            if (!this.timeline) {
                this.timeline = new vis.Timeline($('.timeline-view')[0], null, $.extend(this.timelineOptions, {
                    start: start,
                    end: end,
                }));
                if (events.length > 0) {
                    this.renderEvents(events);
                }
                // this.timeline.on('rangechanged', $.proxy(this._onRangeChanged, this));
                // this.timeline.on('rangechange', $.proxy(this._onRangeChange, this));
                // this.setRange();
            }
        }

        TimelineHelper.prototype.renderEvents = function(_events) {
            var self = this;
            var groupBy = Preferences.getItem('groupBy');

            var _eventsWithoutHolidays = _.filter(_events, function(event) {
                return event.def.extendedProps.type !== 'HOLIDAY';
            });
            var events = _.flatten(_.map(_eventsWithoutHolidays, function(event) {
                var result = self._transformEvent(event, false);
                if (!groupBy || groupBy === 'none') {
                    return result;
                }

                if (result.groups) {
                    return _.map(result.groups, function(group) {
                        return _.extend({}, result, {
                            id: group.id + '-' + result.id,
                            group: group.id,
                            groupName: AJS.escapeHtml(group.name),
                            groupAvatar: group.avatar
                        });
                    });
                } else {
                    result.group = DEFAULT_GROUP;
                    result.groupName = AJS.I18n.getText('ru.mail.jira.plugins.calendar.timeline.defaultGroup');
                    return result
                }
            }));

            var groups =
                _.uniq(
                    _.filter(
                        _.map(events, function(e) {
                            var avatarPrefix = '';

                            if (e.groupAvatar) {
                                avatarPrefix = '<img src="' + AJS.escapeHtml(e.groupAvatar) + '" class="' + groupAvatarClass[e.groupField] + '"/>';
                            }

                            return {
                                id: e.group,
                                content: '<div class="flex flex-row">' + avatarPrefix + '<div class="flex flex-column flex-center">' + e.groupName + '</div></div>',
                                field: e.groupField
                            }
                        }),
                        function(e) {return e && e.id}
                    ),
                    function(e) {return e.id}
                );

            var parentGroups =
                _.map(
                    _.filter(
                        _.uniq(
                            _.map(groups, function(e) {
                                return e.field
                            })
                        ),
                        function(e) { return e; }
                    ),
                    function(e) {
                        return {
                            id: e,
                            content: groupFieldNames[e],
                            subgroupOrder: 'id'
                        }
                    }
                );

            if (groupBy && groupBy !== 'none') {
                if (parentGroups.length > 1) {
                    this.timeline.setGroups(new vis.DataSet(parentGroups.concat(groups)));
                } else {
                    this.timeline.setGroups(new vis.DataSet(groups));
                }
            } else {
                this.timeline.setGroups(null);
            }

            this.timeline.setData({items: events});
        }

        TimelineHelper.prototype.getRangeInterval = function() {
            var diff;
            if (this.timeline) {
                var visibleRange = this.timeline.getWindow();
                diff = visibleRange.end.getTime() - visibleRange.start.getTime();
            }
            return diff || this.defaultInterval.asMilliseconds();
        }

        TimelineHelper.prototype.getVisibleRange = function() {
            if (this.timeline)
                return this.timeline.getWindow();
            return {
                start: moment().subtract(this.defaultInterval).toDate(),
                end: moment().add(this.defaultInterval).toDate()
            };
        }

        TimelineHelper.prototype.computeRange = function(date) {
            var intervalStart, intervalEnd, start, end;
            var visibleRange = this.getVisibleRange();
            var diff = this.getRangeInterval();
            if (date) {
                intervalStart = date.clone().subtract(diff / 2, 'millisecond');
                intervalEnd = date.clone().add(diff / 2, 'millisecond');
                start = date.clone().subtract(diff / 2, 'millisecond');
                end = date.clone().add(diff / 2, 'millisecond');
            } else {
                intervalStart = moment(visibleRange.start);
                intervalEnd = moment(visibleRange.end);
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
        }

        TimelineHelper.prototype.setDate = function(date) {
            if (this.timeline)
                this.timeline.moveTo(date);
            else
                FullCalendar.CalendarApi.prototype.gotoDate().call(this, date);
        }

        TimelineHelper.prototype.setRange = function(range) {
            range = range || this.computeRange();
            this.intervalDuration = moment.duration(range.intervalEnd.toDate().getTime() - range.intervalStart.toDate().getTime());
            View.prototype.setRange.call(this, range);
        }

        TimelineHelper.prototype.zoomOut = function() {
            this.timeline.range.zoom(1.5);
            return this.timelineOptions.zoomMax > this.getRangeInterval();
        }

        TimelineHelper.prototype.zoomIn = function() {
            this.timeline.range.zoom(1 / 1.5);
            return this.timelineOptions.zoomMin < this.getRangeInterval();
        }

        // TimelineHelper.prototype._onRangeChange = function(eventDialog) {
        //     eventDialog && eventDialog.hide();
        // }

        // TimelineHelper.prototype._onRangeChanged = function() {
        //     var _start = this.start;
        //     var _end = this.end;
        //     this.setRange();
        //     this.calendar.date = this.massageCurrentDate(this.calendar.date);
        //     this.calendar.updateHeaderTitle();
        //     this.calendar.updateTodayButton();
        //     if (_start !== this.start || _end !== this.end) {
        //         this.display(this.calendar.date);
        //         this.calendar.unfreezeContentHeight();
        //         this.calendar.getAndRenderEvents();
        //     }
        // }

        TimelineHelper.prototype._transformEvent = function(event, raw) {
            var start = event.hasOwnProperty('range') ? moment(event.range.start.toISOString(), 'YYYY-MM-DDTHH:mm:ss') : moment(event.start);
            var end;
            var options = event.def || event._def;

            if (raw) {
                if (options.allDay) {
                    end = start.add(1, 'days');
                }
                if (options.hasEnd) {
                    end = event.hasOwnProperty('range') ? moment(event.range.end.toISOString(), 'YYYY-MM-DDTHH:mm:ss') : moment(event.end);
                }
            } else {
                start = start.clone().local();
                if (options.allDay) {
                    end = start.clone().add(1, 'days').local();
                }
                if (options.hasEnd) {
                    end = event.hasOwnProperty('range') ? moment(event.range.end.toISOString(), 'YYYY-MM-DDTHH:mm:ss').clone().local() : moment(event.end).clone().local();
                }
            }

            return {
                id: options.extendedProps.calendarId + options.defId,
                eventId: options.publicId,
                calendarId: options.extendedProps.calendarId,
                start: start,
                end: end,
                content: this._buildContent(event),
                title: this._buildTitle(event),
                className: this._getClassForColor(event.backgroundColor || event.ui.backgroundColor),
                style: options.extendedProps.datesError ? 'opacity: 0.4;border-color:#d04437;' : '',
                startEditable: event.startEditable || event.ui.startEditable,
                durationEditable: event.durationEditable || event.ui.durationEditable,
                editable: (event.startEditable || event.ui.startEditable) || (event.durationEditable || event.ui.durationEditable),
                eventType: options.extendedProps.type,
                parentId: options.extendedProps.parentId,
                allDay: options.allDay,
                recurring: options.extendedProps.recurring,
                recurrenceNumber: options.extendedProps.recurrenceNumber,
                originalId: options.extendedProps.originalId || event.def.publicId,
                originalStart: options.extendedProps.originalStart,
                originalEnd: options.extendedProps.originalEnd,
                originalAllDay: options.extendedProps.originalAllDay,
                groups: options.extendedProps.groups,
                groupField: options.extendedProps.groupField,
                type: end ? 'range' : 'box'
            };
        }

        TimelineHelper.prototype._buildContent = function(event) {
            var content = null;
            var options = event.def || event._def;

            if (options.extendedProps.type === 'ISSUE') {
                var typeIcon =
                    '<span class="aui-avatar aui-avatar-xsmall">' +
                    '<span class="aui-avatar-inner">' +
                    '<img src="' + getContextPath() + options.extendedProps.issueTypeImgUrl +'" />' +
                    '</span>' +
                    '</span>';
                content = typeIcon;
                if (options.extendedProps.status)
                    content += ' <span class="jira-issue-status-lozenge aui-lozenge jira-issue-status-lozenge-' + options.extendedProps.statusColor + '">' + options.extendedProps.status + '</span>';
                content += '<span> ' + options.publicId + ' ' + AJS.escapeHTML(options.title) + '</span>';
                if (options.extendedProps.hasOwnProperty('timeSpent'))
                    content += '<aui-badge class="time-spent" title="' + AJS.I18n.getText('timetracking.time.spent') + '">' + options.extendedProps.timeSpent + '</aui-badge>';
                if (options.extendedProps.hasOwnProperty('originalEstimate'))
                    content += '<aui-badge class="original-estimate" title="' + AJS.I18n.getText('timetracking.original.estimate') + '">' + options.extendedProps.originalEstimate + '</aui-badge>';
            } else if (options.extendedProps.type === 'CUSTOM') {
                content = '';
                if (options.extendedProps.participants) {
                    var formattedParticipants;
                    if (options.extendedProps.participants.length === 1) {
                        formattedParticipants = options.extendedProps.participants[0].displayName
                    } else {
                        formattedParticipants = $.map(options.extendedProps.participants, function(e) {
                            return e.displayName.split(/\s+/)[0];
                        }).join(', ');
                    }
                    content = AJS.escapeHTML(formattedParticipants) + ': ';
                }

                var iconContent = '<span class="calendar-event-issue-type custom-type-icon custom-type-icon-' + options.extendedProps.issueTypeImgUrl + '-cal" /></span>';

                if (options.extendedProps.recurring) {
                    var editedIcon = '';

                    if (options.extendedProps.parentId) {
                        editedIcon =
                            '<span class="recurring-edited-icon-bg" style="background-color: ' + (event.backgroundColor || event.ui.backgroundColor) + '"></span>' +
                            '<span class="aui-icon aui-icon-small aui-iconfont-edit recurring-edited-icon"></span>';
                    }

                    iconContent =
                        '<span>' +
                        iconContent + editedIcon +
                        '<span class="recurring-icon-bg" style="background-color: ' + (event.backgroundColor || event.ui.backgroundColor) + '"></span>' +
                        '<span class="aui-icon aui-icon-small aui-iconfont-build recurring-icon"></span>' +
                        '</span>';
                }

                content = iconContent + ' ' + content + AJS.escapeHTML(options.title);
            }

            return content
        }

        TimelineHelper.prototype._buildTitle = function(event) {
            var options = event.def || event._def;
            if (options.extendedProps.type === 'ISSUE') {
                return options.publicId + ' ' + AJS.escapeHTML(options.title);
            } else if (options.extendedProps.type === 'CUSTOM') {
                return AJS.escapeHTML(options.title);
            }
        }

        TimelineHelper.prototype._doInitGroupPicker = function(jswAvailable) {
            var $calendar = $("#calendar-full-calendar, #mailru-calendar-gadget-full-calendar");
            $calendar.find('.vis-timeline').prepend(JIRA.Templates.Plugins.MailRuCalendar.groupSelect());

            var $groupByParent = $('#calendar-group-by-parent');
            var $top = $calendar.find('.vis-top');
            $top.mouseover(function() {
                var groupBy = Preferences.getItem("groupBy");
                if (!groupBy || groupBy === "none") {
                    $groupByParent.hide();
                }
            });
            $top.mouseout(function() {
                $groupByParent.show();
            });

            var $groupByField = $('#calendar-group-by-field');

            if (!jswAvailable) {
                $groupByField.find('option[value=epicLink]').remove();
            }

            $groupByField.auiSelect2({
                minimumInputLength: 0,
                minimumResultsForSearch: -1,
                placeholder: AJS.I18n.getText('ru.mail.jira.plugins.calendar.groupBy'),
                allowClear: true
            });
            $('#s2id_calendar-group-by-field').click(function() {
                $groupByField.auiSelect2('open');
            });
            $groupByField.auiSelect2('val', Preferences.getItem('groupBy'));
        }

        // todo migrate color from hex to classes
        TimelineHelper.prototype._getClassForColor = function(color) {
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

        return new TimelineHelper();
    });
})