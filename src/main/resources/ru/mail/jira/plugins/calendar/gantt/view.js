require(['jquery',
    'underscore',
    'backbone'
], function($, _, Backbone) {
    gantt.config.min_duration = 24 * 60 * 60 * 1000; // minimum task duration = 1 day
    gantt.config.work_time = true;
    gantt.config.skip_off_time = true;
    gantt.config.fit_tasks = true;
    gantt.config.details_on_dblclick = false;
    gantt.config.columns = [
        {
            name: 'id',
            label: 'Key',
            width: '100px'
        },
        {
            align: 'left',
            name: 'summary',
            label: 'Task',
            tree: true,
            width: '*'
        },
        {
            align: 'left',
            name: 'assignee',
            label: 'Assignee',
            width: '*',
            /*template: function(item) {
                if (item.assignee) {

                }
            }*/
        }
        // { name: 'duration', label: 'Продолжительность', align: 'right', width: '*' }
    ];

    gantt.templates.xml_date = function(date) {
        return moment(date).toDate();
    };
    gantt.templates.task_cell_class = function(item, date) {
        if (!gantt.isWorkTime(date)) {
            return 'gantt-diagram-weekend-day';
        }
    };
    gantt.templates.task_class = function(start, end, task) {
        return 'gantt-event-object';
    };

    // todo обработка попадания на выходные конца или начала таска
    // gantt.attachEvent('onTaskDrag', function(id, mode, task, original) {
    //     var modes = gantt.config.drag_mode;
    //     if (mode === modes.move || mode === modes.resize) {
    //         var diff = original.duration * gantt.config.min_duration;
    //
    //         if (!gantt.isWorkTime(task.end_date)) {
    //             task.end_date = new Date(task.end_date + diff);
    //             return;
    //         }
    //         if (!gantt.isWorkTime(task.start_date)) {
    //             task.start_date = new Date(task.start_date - diff);
    //             return;
    //         }
    //     }
    // });

    //Setting available scales
    var scaleConfigs = [
        // minutes
        {
            unit: 'minute', step: 1, scale_unit: 'hour', date_scale: '%H',
            subscales: [
                { unit: 'minute', step: 1, date: '%H:%i' }
            ]
        },
        // hours
        {
            unit: 'hour', step: 1, scale_unit: 'day', date_scale: '%j %M',
            subscales: [
                { unit: 'hour', step: 1, date: '%H:%i' }
            ]
        },
        // days
        {
            unit: 'day', step: 1, scale_unit: 'month', date_scale: '%F',
            subscales: [
                { unit: 'day', step: 1, date: '%j' }
            ]
        },
        // weeks
        {
            unit: 'week', step: 1, scale_unit: 'month', date_scale: '%F',
            subscales: [
                {
                    unit: 'week', step: 1,
                    template: function(date) {
                        var dateToStr = gantt.date.date_to_str('%d %M');
                        var endDate = gantt.date.add(gantt.date.add(date, 1, 'week'), -1, 'day');
                        return dateToStr(date) + ' - ' + dateToStr(endDate);
                    }
                }
            ]
        },
        // months
        {
            unit: 'month', step: 1, scale_unit: 'year', date_scale: '%Y',
            subscales: [
                { unit: 'month', step: 1, date: '%M' }
            ]
        },
        // quarters
        {
            unit: 'month', step: 3, scale_unit: 'year', date_scale: '%Y',
            subscales: [
                {
                    unit: 'month', step: 3,
                    template: function(date) {
                        var dateToStr = gantt.date.date_to_str('%M');
                        var endDate = gantt.date.add(gantt.date.add(date, 3, 'month'), -1, 'day');
                        return dateToStr(date) + ' - ' + dateToStr(endDate);
                    }
                }
            ]
        },
        // years
        {
            unit: 'year', step: 1, scale_unit: 'year', date_scale: '%Y',
            subscales: [
                {
                    unit: 'year', step: 5,
                    template: function(date) {
                        var dateToStr = gantt.date.date_to_str('%Y');
                        var endDate = gantt.date.add(gantt.date.add(date, 5, 'year'), -1, 'day');
                        return dateToStr(date) + ' - ' + dateToStr(endDate);
                    }
                }
            ]
        },
        // decades
        {
            unit: 'year', step: 10, scale_unit: 'year',
            template: function(date) {
                var dateToStr = gantt.date.date_to_str('%Y');
                var endDate = gantt.date.add(gantt.date.add(date, 10, 'year'), -1, 'day');
                return dateToStr(date) + ' - ' + dateToStr(endDate);
            },
            subscales: [
                {
                    unit: 'year', step: 100,
                    template: function(date) {
                        var dateToStr = gantt.date.date_to_str('%Y');
                        var endDate = gantt.date.add(gantt.date.add(date, 100, 'year'), -1, 'day');
                        return dateToStr(date) + ' - ' + dateToStr(endDate);
                    }
                }
            ]
        }
    ];

    AJS.toInit(function() {
        collectTopMailCounterScript();

        /* Models and Collections*/
        var UserData = Backbone.Model.extend({ url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/userPreference' });
        var Calendar = Backbone.Model.extend({ urlRoot: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/' });

        var MainView = Backbone.View.extend({
            el: 'body',
            events: {
                'click #gantt-diagram-zoom-in': 'zoomIn',
                'click #gantt-diagram-zoom-out': 'zoomOut',
                'click #gantt-diagram-zoom-fit': 'zoomToFit',
                'change #gantt-diagram-period-startDate': 'loadGantt',
                'change #gantt-diagram-period-endDate': 'loadGantt',
                'submit form': 'preventFormSubmit'
            },
            initialize: function() {
                AJS.$('#gantt-diagram-period-startDate').datePicker({ 'overrideBrowserDefault': true });
                AJS.$('#gantt-diagram-period-endDate').datePicker({ 'overrideBrowserDefault': true });

                gantt.init('gantt-diagram-calendar');

                var self = this;
                this.eventDialog = AJS.InlineDialog('.gantt-event-object', 'eventDialog', function(content, trigger, showPopup) {
                    var eventId = $(trigger).attr('task_id');

                    // Atlassian bug workaround
                    content.click(function(e) {
                        e.stopPropagation();
                    });

                    if (!event) {
                        content.html('');
                        showPopup();
                        self.eventDialog.hide();
                        return;
                    }

                    content.html('<span class="aui-icon aui-icon-wait">Loading...</span>');
                    showPopup();

                    $.ajax({
                        type: 'GET',
                        url: AJS.format('{0}/rest/mailrucalendar/1.0/calendar/events/{1}/event/{2}/info', contextPath, self.calendar.id, eventId),
                        success: function(issue) {
                            content.html(JIRA.Templates.Plugins.MailRuCalendar.issueInfo({
                                issue: issue,
                                contextPath: AJS.contextPath()
                            })).addClass('calendar-event-info-popup');
                            self.eventDialog.refresh();
                        },
                        error: function(xhr) {
                            var msg = 'Error while trying to view info about issue => ' + eventId;
                            if (xhr.responseText)
                                msg += xhr.responseText;
                            alert(msg);
                        }
                    });
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
            preventFormSubmit: function() {
                return false;
            },
            loadGantt: function() {
                var startDate = AJS.$('#gantt-diagram-period-startDate').val();
                var endDate = AJS.$('#gantt-diagram-period-endDate').val();
                gantt.clearAll();
                gantt.load(AJS.format('{0}/rest/mailrucalendar/1.0/gantt/{1}?start={2}&end={3}', AJS.contextPath(), this.calendar.id, startDate, endDate));

                this.isActive = true;
                this.current = 1;
            },
            recalculateDiagramPeriod: function() {
                var minStartDate, maxEndDate;
                gantt.eachTask(function(task) {
                    if (!minStartDate || moment(task.start_date).isBefore(minStartDate))//todo date format
                        minStartDate = moment(task.start_date);
                    if (!maxEndDate || moment(task.end_date).isAfter(maxEndDate))//todo date format
                        maxEndDate = moment(task.end_date);
                });
                if (minStartDate && maxEndDate)
                    this.setDiagramPeriod(minStartDate.toDate(), maxEndDate.toDate());
            },
            setDiagramPeriod: function(startDate, endDate) {
                gantt.config.start_date = startDate;
                gantt.config.end_date = endDate;

                gantt.render();
            },
            setCalendar: function(calendarId) {
                this.calendar = new Calendar({ id: calendarId });
                this.calendar.fetch({
                    success: function(model) {
                        mainView.loadGantt();
                        $('#gantt-diagram-title').text(AJS.I18n.getText('ru.mail.jira.plugins.calendar.gantt.calendarTitle', model.get('selectedName')))
                    },
                    error: function(model, response) {
                        var msg = 'Error while trying to load calendar. ';
                        if (response.responseText)
                            msg += response.responseText;
                        alert(msg);
                    }
                });

                var dp = new gantt.dataProcessor(AJS.contextPath() + '/rest/mailrucalendar/1.0/gantt/' + calendarId);
                dp.init(gantt);
                dp.setTransactionMode('REST');
            },
            zoomToFit: function() {
                var project = gantt.getSubtaskDates(),
                    areaWidth = gantt.$task.offsetWidth;

                for (var i = 0; i < scaleConfigs.length; i++) {
                    var columnCount = this._getUnitsBetween(project.start_date, project.end_date, scaleConfigs[i].unit, scaleConfigs[i].step);
                    if ((columnCount + 2) * gantt.config.min_column_width <= areaWidth) {
                        break;
                    }
                }

                if (i === scaleConfigs.length) {
                    i--;
                }

                this._applyConfig(scaleConfigs[i], project);
                this.refresh();
                this.current = i;
                this.$('#gantt-diagram-zoom-out').prop('disabled', !this.canZoomOut());
                this.$('#gantt-diagram-zoom-in').prop('disabled', !this.canZoomIn());
            },
            zoomOut: function(e) {
                e.preventDefault();
                this.$('#gantt-diagram-zoom-in').prop('disabled', false);
                if (this.canZoomOut()) {
                    this.isActive = true;
                    this.current = (this.current + 1);
                    if (!scaleConfigs[this.current])
                        this.current = 6;

                    this._setScaleConfig(this.current);
                    this.refresh();
                }
                this.$('#gantt-diagram-zoom-out').prop('disabled', !this.canZoomOut());
                this.$('#gantt-diagram-zoom-in').prop('disabled', !this.canZoomIn());
            },
            zoomIn: function(e) {
                e.preventDefault();
                this.$('#gantt-diagram-zoom-out').prop('disabled', false);
                if (this.canZoomIn()) {
                    this.isActive = true;
                    this.current = (this.current - 1);
                    if (!scaleConfigs[this.current])
                        this.current = 1;
                    this._setScaleConfig(this.current);
                    this.refresh();
                }
                this.$('#gantt-diagram-zoom-out').prop('disabled', !this.canZoomOut());
                this.$('#gantt-diagram-zoom-in').prop('disabled', !this.canZoomIn());
            },
            canZoomOut: function() {
                return !this.isActive || scaleConfigs.length > this.current + 1;
            },
            canZoomIn: function() {
                return !this.isActive || 0 < this.current - 1;
            },
            _setScaleConfig: function(config) {
                var project = gantt.getSubtaskDates();
                this._applyConfig(scaleConfigs[config], project);
                this.current = config;
            },
            refresh: function() {
                gantt.render();
            },
            _applyConfig: function(config, dates) {
                gantt.config.scale_unit = config.scale_unit;
                if (config.date_scale) {
                    gantt.config.date_scale = config.date_scale;
                    gantt.templates.date_scale = null;
                }
                else {
                    gantt.templates.date_scale = config.template;
                }

                gantt.config.step = config.step;
                gantt.config.subscales = config.subscales;

                if (dates) {
                    gantt.config.start_date = gantt.date.add(dates.start_date, -1, config.unit);
                    gantt.config.end_date = gantt.date.add(gantt.date[config.unit + '_start'](dates.end_date), 2, config.unit);
                } else {
                    gantt.config.start_date = gantt.config.end_date = null;
                }
            },
            // get number of columns in timeline
            _getUnitsBetween: function(from, to, unit, step) {
                var start = new Date(from),
                    end = new Date(to);
                var units = 0;
                while (start.valueOf() < end.valueOf()) {
                    units++;
                    start = gantt.date.add(start, step, unit);
                }
                return units;
            }
        });

        /* Router */
        var ViewRouter = Backbone.Router.extend({
            routes: {
                'calendar=:calendar': 'setCalendar'
            },
            setCalendar: function(calendar) {
                mainView.setCalendar(calendar);
            }
        });

        var mainView = new MainView({ model: new UserData() });
        var router = new ViewRouter();

        /* Fetch data */
        mainView.model.fetch({
            success: function(model) {
                moment.tz.setDefault(model.get('timezone'));
                var workingDays = model.get('workingDays');
                var workingHours = [10, 18];// todo make a config for it
                for (var i = 0; i <= 6; i++) {
                    gantt.setWorkTime({ day: i, hours: _.contains(workingDays, i) ? workingHours : false });
                }

                Backbone.history.start();
            },
            error: function(model, response) {
                var msg = 'Error while trying to load user preferences. ';
                if (response.responseText)
                    msg += response.responseText;
                alert(msg);

                Backbone.history.start();
            }
        });
    });
});

/**
 * Run statistic counter - like Google Analytics.
 * Surely, it doesn't collect any personal data or private information.
 * All information you can check on top.mail.ru.
 */
function collectTopMailCounterScript() {
    var _tmr = window._tmr || (window._tmr = []);
    _tmr.push({ id: '2706504', type: 'pageView', start: (new Date()).getTime() });
    (function(d, w, id) {
        if (d.getElementById(id)) return;
        var ts = d.createElement('script');
        ts.type = 'text/javascript';
        ts.async = true;
        ts.id = id;
        ts.src = (d.location.protocol == 'https:' ? 'https:' : 'http:') + '//top-fwz1.mail.ru/js/code.js';
        var f = function() {
            var s = d.getElementsByTagName('script')[0];
            s.parentNode.insertBefore(ts, s);
        };
        if (w.opera == '[object Opera]') {
            d.addEventListener('DOMContentLoaded', f, false);
        } else {
            f();
        }
    })(document, window, 'topmailru-code');
}
