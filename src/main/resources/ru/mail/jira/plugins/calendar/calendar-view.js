define('calendar/calendar-view', ['jquery', 'underscore', 'backbone', 'calendar/timeline-view'], function($, _, Backbone) {

    return Backbone.View.extend({
        el: '#calendar-full-calendar',
        initialize: function(options) {
            this.eventSources = {};
            this.contextPath = options && _.has(options, 'contextPath') ? options.contextPath : AJS.contextPath();
            this.customsButtonOptions = options && _.has(options, 'contextPath') ? options.customsButtonOptions : {};
            this.timeFormat = options && _.has(options, 'timeFormat') ? options.timeFormat : AJS.Meta.get('date-time');
            this.popupWidth = options && _.has(options, 'popupWidth') ? options.popupWidth : 400;
            this.enableFullscreen = options && _.has(options, 'enableFullscreen') ? options.enableFullscreen : false;
        },
        _eventSource: function(id) {
            return this.contextPath + '/rest/mailrucalendar/1.0/calendar/events/' + id;
        },
        _getCalendarHeaderButton: function(buttonName) {
            return this.$el.find('.fc-' + buttonName + '-button');
        },
        _onClickWeekendsVisibility: function(e) {
            e.preventDefault();
            this.trigger('changeWeekendsVisibility');
        },
        _toggleFullscreen: function() {
            this.$('.fc-fullscreen-button span.fc-icon').toggleClass('fc-icon-mailrucalendar-icon-fullscreen fc-icon-mailrucalendar-icon-exit-fullscreen');
            this.fullscreenMode = !this.fullscreenMode;
            if (this.fullscreenMode) {
                $('#header,#timezoneDiffBanner,#announcement-banner,.aui-page-header,#studio-header,#footer').slideUp(400);
                $('.aui-page-panel-nav').animate({width: 'toggle', 'padding': 'toggle'}, 400, $.proxy(function() {
                    if (this.getViewType() == 'timeline')
                        this.$el.fullCalendar('getView').timeline.setOptions({height: $(window).height() - 93 + 'px'});
                }, this));
            } else {
                $('#header,#timezoneDiffBanner,#announcement-banner,.aui-page-header,#studio-header,#footer,.aui-page-panel-nav').fadeIn(400);
                $(window).trigger('resize');
                if (this.getViewType() == 'timeline')
                    this.$el.fullCalendar('getView').timeline.setOptions({height: '450px'});
            }
        },
        _canButtonVisible: function(name) {
            return this.customsButtonOptions[name] == undefined || this.customsButtonOptions[name].visible !== false;
        },
        updateButtonsVisibility: function(view) {
            if (this._canButtonVisible('zoom-out') && this._canButtonVisible('zoom-in') && view.name === 'timeline')
                this._getCalendarHeaderButton('zoom-out').parent('.fc-button-group').show();
            else
                this._getCalendarHeaderButton('zoom-out').parent('.fc-button-group').hide();
            if (this._canButtonVisible('weekend') && (view.name === 'quarter' || view.name === 'month'))
                this._getCalendarHeaderButton('weekend').show();
            else
                this._getCalendarHeaderButton('weekend').hide();
        },
        zoomOutTimeline: function() {
            var view = this.$el.fullCalendar('getView');
            var canZoomOut = view.zoomOut();
            !canZoomOut && view.calendar.header.disableButton('zoom-out');
            view.calendar.header.enableButton('zoom-in');
        },
        zoomInTimeline: function() {
            var view = this.$el.fullCalendar('getView');
            var canZoomIn = view.zoomIn();
            !canZoomIn && view.calendar.header.disableButton('zoom-in');
            view.calendar.header.enableButton('zoom-out');
        },
        init: function(view, hideWeekends) {
            var viewRenderFirstTime = true;
            var contextPath = this.contextPath;
            var self = this;
            this.$el.fullCalendar({
                contentHeight: 'auto',
                defaultView: view,
                header: {
                    left: 'prev,next today',
                    center: 'title',
                    right: 'weekend zoom-out,zoom-in' + (this.enableFullscreen ? ' fullscreen' : '')
                },
                views: {
                    quarter: {
                        type: 'basic',
                        duration: {months: 3}
                    },
                    timeline: {
                        contextPath: contextPath,
                        calendarView: self
                    }
                },
                customButtons: {
                    weekend: {
                        text: hideWeekends ? AJS.I18n.getText('ru.mail.jira.plugins.calendar.showWeekends') : AJS.I18n.getText('ru.mail.jira.plugins.calendar.hideWeekends'),
                        click: $.proxy(this._onClickWeekendsVisibility, this)
                    },
                    'zoom-out': {
                        icon: 'zoom-out',
                        click: $.proxy(this.zoomOutTimeline, this)
                    },
                    'zoom-in': {
                        icon: 'zoom-in',
                        click: $.proxy(this.zoomInTimeline, this)
                    },
                    fullscreen: {
                        icon: 'mailrucalendar-icon-fullscreen',
                        click: $.proxy(this._toggleFullscreen, this)
                    }
                },
                businessHours: {
                    start: '10:00',
                    end: '19:00'
                },
                weekNumberTitle: '',
                weekNumbers: true,
                weekNumberCalculation: 'ISO',
                timezone: 'local',
                timeFormat: this.timeFormat,
                slotLabelFormat: this.timeFormat,
                lazyFetching: true,
                editable: true,
                draggable: true,
                firstDay: 1,
                allDayText: AJS.I18n.getText('ru.mail.jira.plugins.calendar.allDay'),
                monthNames: [AJS.I18n.getText('ru.mail.jira.plugins.calendar.January'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.February'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.March'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.April'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.May'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.June'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.July'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.August'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.September'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.October'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.November'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.December')],
                monthNamesShort: [AJS.I18n.getText('ru.mail.jira.plugins.calendar.Jan'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Feb'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Mar'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Apr'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.May'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Jun'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Jul'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Aug'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Sep'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Oct'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Nov'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Dec')],
                dayNames: [AJS.I18n.getText('ru.mail.jira.plugins.calendar.Sunday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Monday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Tuesday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Wednesday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Thursday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Friday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Saturday')],
                dayNamesShort: [AJS.I18n.getText('ru.mail.jira.plugins.calendar.Sun'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Mon'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Tue'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Wed'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Thu'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Fri'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Sat')],
                buttonText: {
                    today: AJS.I18n.getText('ru.mail.jira.plugins.calendar.today')
                },
                weekends: !hideWeekends,
                weekMode: 'liquid',
                slotWidth: 100,
                slotDuration: '01:00',
                eventRender: function(event, $element) {
                    $element.addClass('calendar-event-object');
                    $element.find('.fc-title').prepend(event.id + ' ');
                    this.eventDialog = AJS.InlineDialog($element, 'eventDialog', function(content, trigger, showPopup) {
                        $.ajax({
                            type: 'GET',
                            url: AJS.format('{0}/rest/mailrucalendar/1.0/calendar/events/{1}/event/{2}/info', contextPath, event.calendarId, event.id),
                            success: function(issue) {
                                content.html(JIRA.Templates.Plugins.MailRuCalendar.issueInfo({
                                    issue: issue,
                                    contextPath: AJS.contextPath()
                                })).addClass('calendar-event-info-popup');
                                showPopup();
                            },
                            error: function(xhr) {
                                var msg = "Error while trying to view info about issue => " + event.id;
                                if (xhr.responseText)
                                    msg += xhr.responseText;
                                alert(msg);
                            }
                        });
                        return false;
                    }, {
                        width: self.popupWidth,
                        hideDelay: null,
                        onTop: true,
                        closeOnTriggerClick: true,
                        userLiveEvents: true
                    });
                },
                loading: $.proxy(function(isLoading, view) {
                    viewRenderFirstTime = false;
                    this.trigger(isLoading ? 'startLoading' : 'stopLoading', view.name);
                }, this),
                viewRender: $.proxy(function(view) {
                    if (!viewRenderFirstTime)
                        this.trigger('render', view.name);
                    this.updateButtonsVisibility(view);
                    $('.calendar-visible').find('a.calendar-name').addClass('not-active');
                }, this),
                eventAfterAllRender: $.proxy(function() {
                    this.$el.fullCalendar('unfreezeContentHeight');
                    if (viewRenderFirstTime)
                        viewRenderFirstTime = false;
                    else
                        this.trigger('renderComplete');

                    var view = this.$el.fullCalendar('getView');
                    var start = view.start.clone().startOf('month');
                    var end = view.end.clone();
                    for (; start.isBefore(end); start.add(1, 'M')) {
                        this.$('.fc-day.fc-widget-content[data-date=' + start.format("YYYY-MM-DD") + ']').addClass('fc-first-day-of-month');
                    }
                }, this),
                eventDragStart: function(event) {
                    event.eventDialog && event.eventDialog.hide();
                    event.eventDialog = undefined;
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
                    url: contextPath + '/rest/mailrucalendar/1.0/calendar/events/' + event.calendarId + '/event/' + event.id + '?millisDelta=' + duration.asMilliseconds() + '&isDrag=' + isDrag,
                    error: function(xhr) {
                        var msg = "Error while trying to drag event. Issue key => " + event.id;
                        if (xhr.responseText)
                            msg += xhr.responseText;
                        alert(msg);
                    }
                });
            }
        },
        addEventSource: function(calendarId, silent) {
            !silent && this.trigger('addSource', calendarId);
            this.$el.fullCalendar('addEventSource', {
                url: this._eventSource(calendarId),
                success: $.proxy(function() {
                    !silent && this.trigger('addSourceSuccess', calendarId, true);
                }, this)
            });
            this.eventSources['' + calendarId] = this._eventSource(calendarId);
        },
        isCalendarInSources: function(calendarId) {
            return this.eventSources['' + calendarId];
        },
        removeEventSource: function(calendarId) {
            if (this.eventSources['' + calendarId]) {
                this.$el.fullCalendar('removeEventSource', this._eventSource(calendarId));
                this.eventSources = _.omit(this.eventSources, '' + calendarId);
            }
        },
        removeAllEventSource: function() {
            _.each(this.eventSources, function(sourceUrl) {
                this.$el.fullCalendar('removeEventSource', sourceUrl);
            }, this);
            this.eventSources = {};
        },
        setView: function(viewName) {
            this.$el.fullCalendar('changeView', viewName);
        },
        getViewType: function() {
            return this.$el.fullCalendar('getView').type;
        },
        toggleWeekends: function(hideWeekends) {
            var view = this.getViewType();
            if (view === 'quarter' || view === 'month') {
                this._getCalendarHeaderButton('weekend').text(hideWeekends ? AJS.I18n.getText('ru.mail.jira.plugins.calendar.showWeekends') : AJS.I18n.getText('ru.mail.jira.plugins.calendar.hideWeekends'));
                this.$el.fullCalendar('option', 'weekends', !hideWeekends);
            }
        }
    });
});
