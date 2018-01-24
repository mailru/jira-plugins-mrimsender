require(['jquery',
    'underscore',
    'backbone'
], function($, _, Backbone) {
    gantt.config.xml_date = '%Y-%m-%dT%H:%i:%s';
    gantt.templates.xml_date = function(date) {
        return moment(date).toDate();
    };
    // gantt.config.autosize = 'y';
    gantt.templates.scale_cell_class = function(date) {
        if (date.getDay() === 0 || date.getDay() === 6)
            return 'weekend';
    };
    gantt.config.columns = [
        { name: 'text', label: ' ', tree: true, width: '*' },
        // { name: 'start_date', label: 'Начало', align: 'center' },
        { name: 'duration', label: 'Продолжительность', align: 'center', width: '*' }
    ];

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
            },
            initialize: function() {
            },
            loadGantt: function() {
                gantt.init('gantt-diagram-calendar');
                gantt.load(AJS.contextPath() + '/rest/mailrucalendar/1.0/gantt/' + this.calendar.id);

                var dp = new gantt.dataProcessor(AJS.contextPath() + '/rest/mailrucalendar/1.0/gantt/' + this.calendar.id);
                dp.init(gantt);
                dp.setTransactionMode("REST");

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
