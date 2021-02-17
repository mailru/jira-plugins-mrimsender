define('calendar/calendar-gadget-config', ['calendar/calendar-view', 'underscore', 'mailrucal/moment'], function(CalendarView, _, moment) {
    function genArray(size, object) {
        return _.map(_.range(size), function(i) {
            return object;
        });
    }

    function getContextPath() {
        if (AJS.gadget) {
            return AJS.gadget.getBaseUrl();
        } else {
            return AJS.contextPath();
        }
    }

    function getView(oldName) {
        if (oldName === undefined) {
            return 'dayGridMonth';
        } else {
            var views = {
                month: 'dayGridMonth',
                agendaWeek: 'timeGridWeek',
                agendaDay: 'timeGridDay'
            }
            if (views.hasOwnProperty(oldName)) {
                return views[oldName]
            } else {
                return oldName;
            }
        }
    }

    //todo: store timeline scale in gadget settings
    return {
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
                                var selectedCalendarMap = _.object(selectedCalendars, genArray(selectedCalendars.length, true));
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
                                parentDiv.closest('.field-group').append('<div class="description">' + AJS.format(gadget.getMsg('ru.mail.jira.plugins.calendar.gadget.config.calendars.description'), '<a href="' + getContextPath() + '/secure/MailRuCalendar.jspa">', '</a>') + '</div>');
                                $calendarSelect.auiSelect2({
                                    multiple: true,
                                    data: data,
                                    formatResult: format,
                                    formatSelection: format
                                });
                                $calendarSelect.on('change', function(data) {
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
                            id: 'calendarView',
                            userpref: 'calendarView',
                            label: gadget.getMsg('ru.mail.jira.plugins.calendar.gadget.config.view'),
                            type: 'select',
                            selected: getView(gadget.getPref('calendarView') || gadget.getPref('view')),
                            options: [
                                {
                                    label: gadget.getMsg('ru.mail.jira.plugins.calendar.quarter'),
                                    value: 'quarter'
                                },
                                {
                                    label: gadget.getMsg('ru.mail.jira.plugins.calendar.month'),
                                    value: 'dayGridMonth'
                                },
                                {
                                    label: gadget.getMsg('ru.mail.jira.plugins.calendar.week'),
                                    value: 'timeGridWeek'
                                },
                                {
                                    label: gadget.getMsg('ru.mail.jira.plugins.calendar.day'),
                                    value: 'timeGridDay'
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
                AJS.$(document).unbind('ajaxStart');
                AJS.$(document).unbind('ajaxStop');

                moment.tz.setDefault(args.props.timezone);

                var gadget = this;

                console.log(gadget);

                var licenseStatus = args.props.license;
                if (!args.props.licenseValid) {
                    gadgets.window.adjustHeight();
                    gadget.getView().empty().html(
                        '<div class="aui-message aui-message-error" style="margin: 15px;">' +
                            '<p class="title">' +
                                '<span class="aui-icon icon-error"></span>' +
                                '<strong>' + AJS.I18n.getText('ru.mail.jira.plugins.license.gadgetUnavailable') + '</strong>' +
                            '</p>' +
                            '<p>' + args.props.licenseError + '</p>' +
                        '</div>'
                    );
                    return;
                }

                var selectedCalendars = _.compact(gadget.getPref('calendars').split(','));
                var selectedCalendarMap = _.object(selectedCalendars, genArray(selectedCalendars.length, true));
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
                    gadgets.window.setTitle(gadget.getMsg('ru.mail.jira.plugins.calendar.linkTitleShort') + ': ' + calendarsInTitle);

                gadgetCalendars = _.sortBy(gadgetCalendars, function(calendar) {
                    return calendar.hasError ? '' : calendar.name;
                });

                var view = getView(gadget.getPref('calendarView') || gadget.getPref('view'));
                var hideWeekends = gadget.getPref('hideWeekends') === 'hideWeekends';
                gadget.showView();
                if (!AJS.$('#mailru-calendar-gadget-full-calendar').length) {
                    gadget.getView().empty().html('' +
                        '<div class="mailru-calendar-calendar-gadget">' +
                        '   <div id="mailru-calendar-gadget-full-calendar"/>' +
                        '   <div id="mailru-calendar-gadget-legend"/>' +
                        '</div>');
                    gadget.calendarView = new CalendarView({
                        contextPath: getContextPath(),
                        el: '#mailru-calendar-gadget-full-calendar',
                        timeFormat: args.props.timeFormat,
                        dateFormat: args.props.dateFormat,
                        dateTimeFormat: args.props.dateTimeFormat,
                        customsButtonOptions: {
                            weekend: {
                                visible: false
                            }
                        },
                        disableCustomEventEditing: true
                    });
                    gadget.calendarView.on('render', function() {
                        gadget.showLoading();
                    });
                    gadget.calendarView.on('renderComplete', function() {
                        gadgets.window.adjustHeight();
                        gadget.hideLoading();
                    });
                    gadget.calendarView.setTimezone(args.props.timezone);
                    gadget.calendarView.init(view, hideWeekends, args.props.workingDays, null, null);
                    var $calendarEl = AJS.$("#mailru-calendar-gadget-full-calendar");
                    $calendarEl.find('.fc-toolbar .fc-button').removeClass('fc-button-primary fc-button').addClass('aui-button');
                    $calendarEl.find('.fc-button-group').addClass('aui-buttons');
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
                    calendar.sourceId = encodeURIComponent(calendar.source.split('_')[1]);
                });
                AJS.$('#mailru-calendar-gadget-legend').html(JIRA.Templates.Plugins.MailRuCalendar.calendarLegend({
                    calendars: gadgetCalendars,
                    contextPath: getContextPath()
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
    };
});