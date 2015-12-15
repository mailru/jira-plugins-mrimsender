(function ($) {
    AJS.toInit(function () {
        collectTopMailCounterScript();
        /* Models */
        var UserData = Backbone.Model.extend({url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/userPreference'});
        var Calendar = Backbone.Model.extend();
        var CalendarDetail = Backbone.Model.extend({urlRoot: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/'});

        /* Collections */
        var CalendarCollection = Backbone.Collection.extend({
            model: Calendar,
            url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/all'
        });

        /* Templates */
        var calendarLinkTpl = _.template($('#calendar-link-template').html());
        var issueInfoTpl = _.template($('#issue-info-template').html());

        /* Instances */
        var userData = new UserData();
        var calendarCollection = new CalendarCollection();
        /* Bind events */
        userData.on('change:calendarView', function(model) {
            var view = model.get('calendarView') || "month";
            $('#calendar-full-calendar').fullCalendar('changeView', view);
        });

        calendarCollection.on("add", function(calendar) {
            $('#calendar-my-calendar-list-header').css('display', 'block');
            $('#calendar-my-calendar-list').append(calendarLinkTpl({calendar: calendar.toJSON()}));

            mainView.addEventSource(calendar);
        });
        calendarCollection.on('remove', function(calendar) {
            $('#calendar-list-item-block-' + calendar.id).remove();
            mainView.removeEventSource(calendar);
        });
        calendarCollection.on("change", function(calendar) {
            var $calendarBlock = $('#calendar-list-item-block-' + calendar.id);

            $calendarBlock.find('span.aui-nav-item-label').text(calendar.get('name'));
            $calendarBlock.data('color', calendar.get('color'));
            $calendarBlock.toggleClass('calendar-visible', calendar.get('visible'));

            mainView.removeEventSource(calendar);
            if (calendar.get('visible')) {
                mainView.addEventSource(calendar);
            } else {
                mainView._changeEventSourceCallback(calendar.id, false);
            }
        });

        /* Fetch data */
        userData.fetch({
            success: function (model) {
                var view = model.get('calendarView') || "month";
                mainView.loadFullCalendar(view, model.get('hideWeekends'));
            },
            error: function (model, response) {
                var msg = "Error while trying to load user preferences.";
                if (response.responseText)
                    msg += response.responseText;
                alert(msg);
                mainView.loadFullCalendar("month", false);
            }
        });
        calendarCollection.fetch({
            silent: true,
            success: function (collection) {
                var htmlSharedCalendars = '';
                var htmlMyCalendars = '';
                var htmlOtherCalendars = '';
                var eventSources = [];

                if (!!collection.findWhere({visible: true}))
                    mainView.startLoadingCalendarsCallback();

                collection.each(function(calendar) {
                    if (calendar.get('isMy'))
                        htmlMyCalendars += calendarLinkTpl({calendar: calendar.toJSON()});
                    else if (calendar.get('fromOthers'))
                        htmlOtherCalendars += calendarLinkTpl({calendar: calendar.toJSON()});
                    else
                        htmlSharedCalendars += calendarLinkTpl({calendar: calendar.toJSON()});

                    if (calendar.get('visible'))
                        eventSources.push(AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/events/' + calendar.id);
                });

                if (htmlMyCalendars != '') {
                    $('#calendar-my-calendar-list-header').css('display', 'block');
                    $('#calendar-my-calendar-list').append(htmlMyCalendars);
                }

                if (htmlSharedCalendars != '') {
                    $('#calendar-shared-calendar-list-header').css('display', 'block');
                    $('#calendar-shared-calendar-list').append(htmlSharedCalendars);
                }

                if (htmlOtherCalendars != '') {
                    $('#calendar-other-calendar-list-header').css('display', 'block');
                    $('#calendar-other-calendar-list').append(htmlOtherCalendars);
                }

                for (var a = 0; a < eventSources.length; a++)
                    $('#calendar-full-calendar').fullCalendar('addEventSource', eventSources[a]);
            },
            error: function (request) {
                alert(request.responseText);
            }
        });

        var MainView = Backbone.View.extend({
            el: 'body',
            events: {
                'click #calendar-create-feed': 'showCalendarFeedView',
                'click #calendar-weekends-visibility': 'toggleWeekendsVisibility',
                'click .calendar-name': 'toggleCalendarVisibility',
                'click .calendar-delete': 'deleteCalendar',
                'click .calendar-edit': 'editCalendar',
                'click #calendar-add': 'addCalendar'
            },
            initialize: function() {
                this.$fullCalendar = $('#calendar-full-calendar');
            },
            eventSource: function(id) {
                return AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/events/' + id;
            },
            addEventSource: function(calendar) {
                this.startLoadingCalendarsCallback();
                this.$fullCalendar.fullCalendar('addEventSource', {
                    url: this.eventSource(calendar.id),
                    success: $.proxy(function () { this._changeEventSourceCallback(calendar.id, true, calendar.get('error')); }, this)
                });
            },
            startLoadingCalendarsCallback: function() {
                AJS.dim();
                JIRA.Loading.showLoadingIndicator();
            },
            finishLoadingCalendarsCallback: function() {
                $('.calendar-name').removeClass('not-active');
                AJS.undim();
                JIRA.Loading.hideLoadingIndicator();
            },
            removeEventSource: function(calendar) {
                this.$fullCalendar.fullCalendar('removeEventSource', this.eventSource(calendar.id));
                this.finishLoadingCalendarsCallback();
            },
            showCalendarFeedView: function(e) {
                e.preventDefault();
                new Backbone.View.CalendarFeedView({model: userData, collection: calendarCollection}).show();
            },
            loadFullCalendar: function(view, hideWeekends) {
                var viewRenderFirstTime = true;
                this.$fullCalendar.fullCalendar({
                    schedulerLicenseKey: 'GPL-My-Project-Is-Open-Source',
                    contentHeight: 'auto',
                    defaultView: view,
                    header: {
                        left: 'prev,next today',
                        center: 'title',
                        right: 'quarter,month,basicWeek,agendaDay timelineWeek'
                    },
                    views: {
                        quarter: {
                            type: 'basic',
                            duration: { months: 3 },
                            buttonText: AJS.I18n.getText('ru.mail.jira.plugins.calendar.quarter')
                        }
                    },
                    timeFormat: 'HH:mm',
                    lazyFetching: false,
                    editable: true,
                    draggable: true,
                    firstDay: 1,
                    monthNames: [ AJS.I18n.getText('ru.mail.jira.plugins.calendar.January'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.February'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.March'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.April'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.May'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.June'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.July'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.August'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.September'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.October'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.November'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.December')],
                    monthNamesShort: [ AJS.I18n.getText('ru.mail.jira.plugins.calendar.Jan'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Feb'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Mar'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Apr'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.May'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Jun'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Jul'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Aug'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Sep'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Oct'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Nov'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Dec')],
                    dayNames: [ AJS.I18n.getText('ru.mail.jira.plugins.calendar.Sunday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Monday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Tuesday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Wednesday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Thursday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Friday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Saturday')],
                    dayNamesShort: [ AJS.I18n.getText('ru.mail.jira.plugins.calendar.Sun'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Mon'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Tue'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Wed'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Thu'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Fri'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Sat')],
                    buttonText: {
                        today: AJS.I18n.getText('ru.mail.jira.plugins.calendar.today'),
                        month: AJS.I18n.getText('ru.mail.jira.plugins.calendar.month'),
                        week: AJS.I18n.getText('ru.mail.jira.plugins.calendar.week'),
                        day: AJS.I18n.getText('ru.mail.jira.plugins.calendar.day'),
                        timelineWeek: AJS.I18n.getText('ru.mail.jira.plugins.calendar.timeline')
                    },
                    weekends: !hideWeekends,
                    weekMode: 'liquid',
                    slotWidth: 100,
                    slotDuration: '01:00',
                    eventRender: function(event, $element) {
                        $element.addClass('calendar-event-object');
                        AJS.InlineDialog($element, "eventDialog", function(content, trigger, showPopup) {
                            $.ajax({
                                type: 'GET',
                                url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/events/' + event.calendarId + '/event/' + event.id + '/info',
                                success: function (issue) {
                                    content.html(issueInfoTpl({issue: issue})).addClass('calendar-event-info-popup');
                                    showPopup();
                                },
                                error: function (xhr) {
                                    var msg = "Error while trying to view info about issue => " + event.id;
                                    if (xhr.responseText)
                                        msg += xhr.responseText;
                                    alert(msg);
                                }
                            });
                            return false;
                        }, {
                            hideDelay: null,
                            onTop: true,
                            closeOnTriggerClick: true,
                            userLiveEvents: true
                        });
                    },
                    viewRender: function(view) {
                        if (viewRenderFirstTime)
                            viewRenderFirstTime = false;
                        else {
                            var $visibleCalendars = $('.calendar-visible');
                            mainView.startLoadingCalendarsCallback();
                            $visibleCalendars.find('a.calendar-name').addClass('not-active');
                            $.ajax({
                                type: 'PUT',
                                url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/userPreference/view?value=' + view.name,
                                error: function (xhr) {
                                    var msg = "Error while trying to update user default view to => " + view;
                                    if (xhr.responseText)
                                        msg += xhr.responseText;
                                    alert(msg);
                                }
                            });
                        }
                    },
                    eventAfterAllRender: function () {
                        mainView.finishLoadingCalendarsCallback();
                    },
                    eventDrop: function(event, duration) {
                        eventMove(event, duration, true);
                    },
                    eventResize: function(event, duration) {
                        eventMove(event, duration, false);
                    }
                });

                function eventMove(event, duration, isDrag) {
                    $.ajax({
                        type: 'PUT',
                        url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/events/' + event.calendarId + '/event/' + event.id + '?dayDelta=' + duration._days + '&millisDelta=' + duration._milliseconds + '&isDrag=' + isDrag,
                        error: function (xhr) {
                            var msg = "Error while trying to drag event. Issue key => " + event.id;
                            if (xhr.responseText)
                                msg += xhr.responseText;
                            alert(msg);
                        }
                    });
                }
            },
            toggleWeekendsVisibility: function (e) {
                e.preventDefault();
                $.ajax({
                    type: 'PUT',
                    url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/userPreference/hideWeekends',
                    success: function () {
                        window.location.reload();
                    },
                    error: function (xhr) {
                        var msg = "Error while trying to update user hide weekends option. ";
                        if (xhr.responseText)
                            msg += xhr.responseText;
                        alert(msg);
                    }
                });
            },
            toggleCalendarVisibility: function (e) {
                e.preventDefault();
                var $calendarNameLink = this.$(e.currentTarget);
                if ($calendarNameLink.hasClass('not-working'))
                    return;

                var calendarId = $calendarNameLink.closest('div.calendar-list-item-block').data('id');

                this.startLoadingCalendarsCallback();
                $calendarNameLink.addClass('not-active');

                $.ajax({
                    type: 'PUT',
                    url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/' + calendarId + '/visibility',
                    success: function () {
                        calendarCollection.fetch();
                    },
                    error: function (request) {
                        alert(request.responseText);
                    }
                });
            },
            addCalendar: function (e) {
                e.preventDefault();
                var calendarDialogView = new Backbone.View.CalendarDialogView({model: new CalendarDetail(), collection: calendarCollection});
                calendarDialogView.show();
            },
            editCalendar: function (e) {
                e.preventDefault();
                e.stopPropagation();

                var calendarId = $(e.currentTarget).closest('div.calendar-list-item-block').data('id');
                var calendarDetail = new CalendarDetail({id: calendarId});
                calendarDetail.fetch({
                    success: function(model) {
                        var calendarDialogView = new Backbone.View.CalendarDialogView({model: model, collection: calendarCollection});
                        calendarDialogView.show();
                    },
                    error: function (request) {
                        alert(request.responseText);
                    }
                });
            },
            deleteCalendar: function (e) {
                e.preventDefault();
                e.stopPropagation();

                var calendarId = $(e.currentTarget).closest('div.calendar-list-item-block').data('id');
                if (confirm(AJS.I18n.getText("ru.mail.jira.plugins.calendar.confirmDelete"))) {
                    this.startLoadingCalendarsCallback();
                    $.ajax({
                        url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/' + calendarId,
                        type: "DELETE",
                        error: $.proxy(function(xhr) {
                            this.finishLoadingCalendarsCallback();
                            alert(xhr.responseText || "Internal error");
                        }, this),
                        success: function() {
                            calendarCollection.fetch();
                        }
                    });
                }
            },
            _changeEventSourceCallback: function(calendarId, visible, error) {
                var $calendarBlock = this.$('#calendar-list-item-block-' + calendarId);
                var $calendarLink = $calendarBlock.find('a.calendar-name');
                $calendarLink.removeClass('not-active');

                var calendarColor = $calendarBlock.data('color');
                $calendarLink.find('.calendar-view-color-box').remove();

                if (error) {
                    if (!$calendarLink.hasClass('not-working')) {
                        $calendarLink.addClass('not-working');
                        $calendarLink.find('.calendar-view-color-box').remove();
                        $calendarLink.prepend('<span class="aui-icon aui-icon-small aui-iconfont-error" title="' + error + '"></span>')
                    }
                } else {
                    if ($calendarLink.hasClass('not-working'))
                        $calendarLink.removeClass('not-working').find('span.aui-iconfont-error').remove();

                    if (visible)
                        $calendarLink.prepend('<div class="calendar-view-color-box" style="background-color: ' + calendarColor + ';"></div>');
                    else
                        $calendarLink.prepend('<div class="calendar-view-color-box" style="border: 2px solid ' + calendarColor + ';"></div>');
                }
            },
        });

        var mainView = new MainView();
    });
})(AJS.$);

/**
 * Run statistic counter - like Google Analytics.
 * Surely, it doesn't collect any personal data or private information.
 * All information you can check on top.mail.ru.
 */
function collectTopMailCounterScript() {
    var _tmr = window._tmr || (window._tmr = []);
    _tmr.push({id: "2706504", type: "pageView", start: (new Date()).getTime()});
    (function (d, w, id) {
        if (d.getElementById(id)) return;
        var ts = d.createElement("script"); ts.type = "text/javascript"; ts.async = true; ts.id = id;
        ts.src = (d.location.protocol == "https:" ? "https:" : "http:") + "//top-fwz1.mail.ru/js/code.js";
        var f = function () {var s = d.getElementsByTagName("script")[0]; s.parentNode.insertBefore(ts, s);};
        if (w.opera == "[object Opera]") { d.addEventListener("DOMContentLoaded", f, false); } else { f(); }
    })(document, window, "topmailru-code");
}