var gadget = AJS.Gadget({
    baseUrl: atlassian.util.getRendererBaseUrl(),
    useOauth: '/rest/gadget/1.0/currentUser',
    config: {
        descriptor: function(args) {
            var gadget = this;
            return {
                theme: gadgets.window.getViewportDimensions().width < 450 ? 'gdt top-label' : 'gdt',
                fields: [
                    AJS.gadget.fields.nowConfigured(),
                    {
                        id: 'calendars-field',
                        userpref: 'calendars',
                        label: gadget.getMsg('ru.mail.jira.plugins.calendar.gadget.config.calendars'),
                        description: gadget.getMsg('ru.mail.jira.plugins.calendar.gadget.config.calendars.description'),
                        type: 'callbackBuilder',
                        callback: function(parentDiv) {
                            var selectedCalendars = gadget.getPref('calendars').split(',');
                            var selectedCalendarMap = _.object(selectedCalendars, selectedCalendars.slice(0).fill(true));
                            var data = [];
                            _.each(args.calendars, function(calendar) {
                                data.push({
                                    id: calendar.id,
                                    text: calendar.name,
                                    color: calendar.color,
                                    selected: !!selectedCalendarMap[calendar.id]
                                });
                            });
                            var $calendarSelect = AJS.$('<input type="hidden" class="select multi-select" id="calendars-field" name="calendars"/>');
                            parentDiv.append($calendarSelect);
                            $calendarSelect.auiSelect2({
                                multiple: true,
                                data: data,
                                formatResult: format,
                                formatSelection: format
                            });
                            $calendarSelect.on("change", function() {
                                gadgets.window.adjustHeight();
                            });
                            $calendarSelect.auiSelect2('val', selectedCalendars);

                            function format(item) {
                                return JIRA.Templates.Plugins.MailRuCalendar.calendarField({
                                    name: item.text,
                                    color: item.color
                                });
                            }
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

            var selectedCalendars = _.compact(gadget.getPref('calendars').split(','));
            var selectedCalendarMap = _.object(selectedCalendars, selectedCalendars.slice(0).fill(true));
            var gadgetCalendars = _.filter(args.calendars, function(calendar) {
                return !!selectedCalendarMap[calendar.id];
            });
            var calendarsInTitle = _.pluck(gadgetCalendars, 'name').join(', ');
            gadgets.window.setTitle(gadget.getMsg('ru.mail.jira.plugins.calendar.linkTitle') + ': ' + calendarsInTitle);

            if (selectedCalendars.length > gadgetCalendars.length) {
                gadget.getView().empty().html('<div class="mailru-calendar-calendar-gadget" />');
                AJS.$('.mailru-calendar-calendar-gadget').html(JIRA.Templates.Plugins.MailRuCalendar.gadgetCalendarError({calendars: []}));
                AJS.$('.mailru-calendar-calendar-gadget').width(gadgets.window.getViewportDimensions().width - 30);
                return;
            }

            var view = gadget.getPref('view') || 'week';
            var hideWeekends = gadget.getPref('hideWeekends') == 'hideWeekends';
            require(['calendar/calendar-view'], function(CalendarView) {
                if (!gadget.calendarView) {
                    gadget.getView().empty().html('' +
                        '<div class="mailru-calendar-calendar-gadget">' +
                        '   <div id="mailru-calendar-gadget-full-calendar"/>' +
                        '   <div id="mailru-calendar-gadget-legend"/>' +
                        '</div>');
                    gadget.calendarView = new CalendarView({
                        contextPath: '',
                        el: '#mailru-calendar-gadget-full-calendar',
                        customsButtonOptions: {
                            weekend: {
                                visible: false
                            }
                        }
                    });
                    gadget.calendarView.init(view, hideWeekends);
                    AJS.$('.mailru-calendar-calendar-gadget').width(gadgets.window.getViewportDimensions().width - 30);
                } else {
                    gadget.calendarView.removeAllEventSource();
                    gadget.calendarView.setView(view);
                    gadget.calendarView.toggleWeekends(hideWeekends);
                }
                _.each(gadgetCalendars, function(calendar) {
                    gadget.calendarView.addEventSource(calendar.id, false);
                });
                AJS.$('#mailru-calendar-gadget-legend').html(JIRA.Templates.Plugins.MailRuCalendar.calendarLegend({calendars: gadgetCalendars}));
                gadgets.window.adjustHeight();
                gadget.calendarView.on('changeWeekendsVisibility', function() {
                    hideWeekends = !hideWeekends;
                    gadget.savePref('hideWeekends', hideWeekends ? 'hideWeekends' : 'false');
                    gadget.calendarView.toggleWeekends(hideWeekends);
                    gadgets.window.adjustHeight();
                });
                gadget.calendarView.on('render', function() {
                    gadget.showLoading();
                });
                gadget.calendarView.on('renderComplete', function() {
                    gadgets.window.adjustHeight();
                });
            });
        },
        args: function() {
            return [
                {
                    key: 'calendars',
                    ajaxOptions: '/rest/mailrucalendar/1.0/calendar/all'
                }
            ];
        }()
    }
});