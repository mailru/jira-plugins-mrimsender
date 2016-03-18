var gadget = AJS.Gadget({
    baseUrl: atlassian.util.getRendererBaseUrl(),
    useOauth: '/rest/gadget/1.0/currentUser',
    config: {
        descriptor: function(args) {
            var gadget = this;
            gadgets.window.setTitle(gadget.getMsg('ru.mail.jira.plugins.calendar.linkTitle'));

            return {
                theme: gadgets.window.getViewportDimensions().width < 450 ? 'gdt top-label' : 'gdt',
                fields: [
                    AJS.gadget.fields.nowConfigured(),
                    {
                        id: 'calendars-field',
                        userpref: 'calendars',
                        label: gadget.getMsg('ru.mail.jira.plugins.calendar.gadget.config.calendars'),
                        type: 'callbackBuilder',
                        callback: function(parentDiv) {
                            var selectedCalendars = gadget.getPref('calendars').split('|');
                            var selectedCalendarMap = _.object(selectedCalendars, selectedCalendars.slice(0).fill(true));
                            var html = '<select multiple="multiple" class="select multi-select" id="calendars-field" name="calendars">';
                            _.each(args.calendars, function(calendar) {
                                html += AJS.format('<option value="{0}" {2}>{1}</option>', calendar.id, calendar.name, !!selectedCalendarMap[calendar.id] ? 'selected' : '');
                            });
                            html += '</select>';
                            var $calendarSelect = AJS.$(html);
                            parentDiv.append($calendarSelect);
                            $calendarSelect.auiSelect2();
                            $calendarSelect.on("change", function() {
                                gadgets.window.adjustHeight();
                            });
                        }
                    },
                    {
                        id: 'view-field',
                        userpref: 'view',
                        label: gadget.getMsg('ru.mail.jira.plugins.calendar.gadget.config.view'),
                        type: 'select',
                        selected: gadget.getPref('view'),
                        options: [
                            {
                                label: gadget.getMsg('ru.mail.jira.plugins.calendar.quarter'),
                                value: 'quarter'
                            },
                            {
                                label: gadget.getMsg('ru.mail.jira.plugins.calendar.month'),
                                value: 'month'
                            },
                            {
                                label: gadget.getMsg('ru.mail.jira.plugins.calendar.week'),
                                value: 'basicWeek'
                            },
                            {
                                label: gadget.getMsg('ru.mail.jira.plugins.calendar.day'),
                                value: 'agendaDay'
                            },
                            {
                                label: gadget.getMsg('ru.mail.jira.plugins.calendar.timeline'),
                                value: 'timeline'
                            }
                        ]
                    },
                    {
                        id: 'hideWeekends-select',
                        userpref: 'hideWeekends',
                        label: gadget.getMsg('ru.mail.jira.plugins.calendar.hideWeekends'),
                        type: 'checkbox',
                        options: [
                            {
                                value: 'hideWeekends',
                                selected: gadget.getPref('hideWeekends') == 'hideWeekends'
                            }
                        ]
                    }
                ]
            };
        },
        args: function() {
            return [
                {
                    key: 'calendars',
                    ajaxOptions: '/rest/mailrucalendar/1.0/calendar/all'
                }
            ];
        }()
    },
    view: {
        onResizeAdjustHeight: true,
        enableReload: true,
        onResizeReload: true,
        template: function(args) {
            var gadget = this;
            var view = gadget.getPref('view') || 'week';
            var hideWeekends = gadget.getPref('hideWeekends') == 'hideWeekends';
            require(['calendar/calendar-view'], function(CalendarView) {
                if (!gadget.calendarView) {
                    gadget.getView().empty().html('<div id="mailru-calendar-gadget-full-calendar" />');
                    gadget.calendarView = new CalendarView({
                        contextPath: '',
                        el: '#mailru-calendar-gadget-full-calendar'
                    });
                    gadget.calendarView.init(view, hideWeekends);
                } else {
                    gadget.calendarView.removeAllEventSource();
                    gadget.calendarView.setView(view);
                    gadget.calendarView.toggleWeekends(hideWeekends);
                }
                AJS.$('#mailru-calendar-gadget-full-calendar').width(gadgets.window.getViewportDimensions().width - 30);
                var calendars = gadget.getPref('calendars').split('|');
                _.each(calendars, function(calendarId) {
                    calendarId && gadget.calendarView.addEventSource(calendarId, false);
                });
                gadget.calendarView.on('changeWeekendsVisibility', function() {
                    hideWeekends = !hideWeekends;
                    gadget.savePref('hideWeekends', hideWeekends ? 'hideWeekends' : 'false');
                    gadget.calendarView.toggleWeekends(hideWeekends);
                    gadgets.window.adjustHeight();
                });
                gadget.calendarView.on('renderComplete', function() {
                    gadgets.window.adjustHeight();
                });
            });
        }
    }
});