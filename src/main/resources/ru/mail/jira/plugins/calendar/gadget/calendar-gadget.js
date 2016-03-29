require(['calendar/calendar-view'], function(CalendarView) {
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
                            type: 'callbackBuilder',
                            callback: function(parentDiv) {
                                var selectedCalendars = _.compact(gadget.getPref('calendars').split(','));
                                var selectedCalendarMap = _.object(selectedCalendars, selectedCalendars.slice(0).fill(true));
                                var data = [];
                                _.each(args.calendars, function(calendar) {
                                    data.push({
                                        id: calendar.id,
                                        text: calendar.name,
                                        color: calendar.color,
                                        selected: !!selectedCalendarMap[calendar.id],
                                        hasError: calendar.hasError
                                    });
                                    selectedCalendarMap[calendar.id] = false;
                                });
                                _.each(selectedCalendarMap, function(notAdded, id) {
                                    if (notAdded)
                                        data.push({
                                            id: id,
                                            selected: true,
                                            hasError: true
                                        });
                                });
                                selectedCalendars = _.pluck(_.sortBy(_.filter(data, function(calendar) {
                                    return calendar.selected;
                                }), function(calendar) {
                                    return calendar.hasError ? calendar.text ? '!' + calendar.text : '' : calendar.text;
                                }), 'id');

                                var $calendarSelect = AJS.$('<input type="hidden" class="select multi-select" id="calendars" name="calendars"/>');
                                parentDiv.append($calendarSelect);
                                parentDiv.closest('.field-group').append('<div class="description">' + AJS.format(gadget.getMsg('ru.mail.jira.plugins.calendar.gadget.config.calendars.description'), '<a href="' + AJS.contextPath() + '/secure/MailRuCalendar.jspa">', '</a>') + '</div>');
                                $calendarSelect.auiSelect2({
                                    multiple: true,
                                    data: data,
                                    formatResult: format,
                                    formatSelection: format
                                });
                                $calendarSelect.on("change", function(data) {
                                    gadgets.window.adjustHeight();
                                });
                                $calendarSelect.auiSelect2('val', selectedCalendars);

                                function format(item) {
                                    return JIRA.Templates.Plugins.MailRuCalendar.calendarField({
                                        id: item.id,
                                        name: item.text,
                                        color: item.color,
                                        hasError: item.hasError
                                    });
                                }
                            }
                        },
                        {
                            id: 'view',
                            userpref: 'view',
                            label: gadget.getMsg('ru.mail.jira.plugins.calendar.gadget.config.view'),
                            type: 'select',
                            selected: gadget.getPref('view') || 'month',
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
                                    value: 'agendaWeek'
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
                            id: 'hideWeekends',
                            userpref: 'hideWeekends',
                            label: gadget.getMsg('ru.mail.jira.plugins.calendar.gadget.config.weekends'),
                            description: gadget.getMsg('ru.mail.jira.plugins.calendar.gadget.config.weekends.description'),
                            type: 'select',
                            selected: gadget.getPref('hideWeekends') || 'showWeekends',
                            options: [
                                {
                                    label: gadget.getMsg('ru.mail.jira.plugins.calendar.hideWeekends'),
                                    value: 'hideWeekends'
                                },
                                {
                                    label: gadget.getMsg('ru.mail.jira.plugins.calendar.showWeekends'),
                                    value: 'showWeekends'
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
                AJS.$(document).unbind("ajaxStart");
                AJS.$(document).unbind("ajaxStop");
                var gadget = this;

                var selectedCalendars = _.compact(gadget.getPref('calendars').split(','));
                var selectedCalendarMap = _.object(selectedCalendars, selectedCalendars.slice(0).fill(true));
                var gadgetCalendars = _.filter(args.calendars, function(calendar) {
                    return !!selectedCalendarMap[calendar.id];
                });

                if (selectedCalendars.length > gadgetCalendars.length) {
                    gadget.getView().empty().html('<div class="mailru-calendar-calendar-gadget" />');
                    AJS.$('.mailru-calendar-calendar-gadget').html(JIRA.Templates.Plugins.MailRuCalendar.gadgetCalendarError());
                    AJS.$('.mailru-calendar-calendar-gadget').width(gadgets.window.getViewportDimensions().width - 40);
                    return;
                }
                var calendarsInTitle = _.pluck(gadgetCalendars, 'name').join(', ');
                if (calendarsInTitle)
                    gadgets.window.setTitle(gadget.getMsg('ru.mail.jira.plugins.calendar.linkTitle') + ': ' + calendarsInTitle);

                gadgetCalendars = _.sortBy(gadgetCalendars, function(calendar) {
                    return calendar.hasError ? '' : calendar.name;
                });

                var view = gadget.getPref('view') || 'month';
                var hideWeekends = gadget.getPref('hideWeekends') == 'hideWeekends';
                gadget.showView();
                if (!AJS.$('#mailru-calendar-gadget-full-calendar').length) {
                    gadget.getView().empty().html('' +
                        '<div class="mailru-calendar-calendar-gadget">' +
                        '   <div id="mailru-calendar-gadget-full-calendar"/>' +
                        '   <div id="mailru-calendar-gadget-legend"/>' +
                        '</div>');
                    gadget.calendarView = new CalendarView({
                        contextPath: '',
                        el: '#mailru-calendar-gadget-full-calendar',
                        timeFormat: args.props.timeFormat,
                        customsButtonOptions: {
                            weekend: {
                                visible: false
                            }
                        }
                    });
                    gadget.calendarView.on('render', function() {
                        gadget.showLoading();
                    });
                    gadget.calendarView.on('renderComplete', function() {
                        gadgets.window.adjustHeight();
                        gadget.hideLoading();
                    });
                    gadget.calendarView.init(view, hideWeekends);
                } else {
                    gadget.calendarView.removeAllEventSource();
                    gadget.calendarView.setView(view);
                    gadget.calendarView.toggleWeekends(hideWeekends);
                }
                gadget.calendarView.popupWidth = gadgets.window.getViewportDimensions().width - 60;
                AJS.$('.mailru-calendar-calendar-gadget').width(gadgets.window.getViewportDimensions().width - 40);
                _.each(gadgetCalendars, function(calendar) {
                    gadget.calendarView.addEventSource(calendar.id, false);
                    calendar.sourceType = calendar.source.split('_')[0];
                    calendar.sourceId = calendar.source.split('_')[1];
                });
                AJS.$('#mailru-calendar-gadget-legend').html(JIRA.Templates.Plugins.MailRuCalendar.calendarLegend({
                    calendars: gadgetCalendars,
                    contextPath: AJS.contextPath()
                }));
                gadgets.window.adjustHeight();
            },
            args: function() {
                return [
                    {
                        key: 'calendars',
                        ajaxOptions: '/rest/mailrucalendar/1.0/calendar/all'
                    },
                    {
                        key: 'props',
                        ajaxOptions: '/rest/mailrucalendar/1.0/calendar/config/props'
                    }
                ];
            }()
        }
    });
});