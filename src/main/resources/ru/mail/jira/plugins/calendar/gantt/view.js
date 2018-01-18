require(['jquery',
    'underscore',
    'backbone',
    'calendar/feedback-flag',
    'calendar/calendar-view',
    'calendar/calendar-dialog',
    'calendar/confirm-dialog',
    'calendar/feed-dialog',
    'calendar/import-dialog',
    'calendar/quick-filter-dialog',
    'calendar/custom-event-dialog',
    'calendar/preferences',
    'calendar/timeline-view'
], function($, _, Backbone, LikeFlag, CalendarView, CalendarDialog, ConfirmDialog, CalendarFeedDialog, CalendarImportDialog, QuickFilterDialog, CustomEventDialog, Preferences) {

    AJS.toInit(function() {
        collectTopMailCounterScript();

        /* Models and Collections*/
        var UserData = Backbone.Model.extend({url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/userPreference'});
        var Calendar = Backbone.Model.extend();
        var CustomEvent = Backbone.Model.extend({urlRoot: AJS.contextPath() + '/rest/mailrucalendar/1.0/customEvent/'});
        var CalendarDetail = Backbone.Model.extend({urlRoot: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/'});
        var UserCalendarCollection = Backbone.Collection.extend({
            model: Calendar,
            url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/forUser'
        });

        var QuickFilter = Backbone.Model.extend();
        var QuickFilterCollection = Backbone.Collection.extend({
            model: QuickFilter,
            initialize: function(models, options) {
                this.calendarId = options.calendarId;
            },
            url: function() {
                return AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/' + this.calendarId + '/quickFilter/all';
            }
        });

        var MainView = Backbone.View.extend({
            el: 'body',
            events: {
                'click #calendar-create-feed': 'showCalendarFeedView',
                'click #calendar-create': 'createCalendar',
                'click #calendar-import': 'importCalendar',
                'click #mailrucalendar-add-url-calendars': 'addUrlCalendars',
                'click #calendar-period-dropdown a': 'changeCalendarView',
                'click .calendar-name': 'toggleCalendarVisibility',
                'click .calendar-issue-navigator': 'openIssueNavigator',
                'click .calendar-open-gantt': 'openGanttDiagram',
                'click .calendar-delete': 'deleteCalendar',
                'click .calendar-edit': 'editCalendar',
                'click .calendar-remove': 'removeFavoriteCalendar',
                'click .calendar-create-custom-event': '_createCustomEventFromMenu',
                'click .calendar-configure-quick-filters': 'configureQuickFilters',
                'click .calendar-quickfilter-button': 'toggleQuickFilter'
            },
            initialize: function() {
                this.calendarView = new CalendarView({enableFullscreen: true});
                this.initializeTooltips();

                this.calendarView.on('addSource render', this.startLoadingCalendarsCallback, this);
                this.calendarView.on('renderComplete', this.finishLoadingCalendarsCallback, this);
                this.calendarView.on('render', this.updatePeriodButton, this);
                this.calendarView.on('render', this.updateViewInterval, this);
                this.calendarView.on('changeWeekendsVisibility', this.toggleWeekendsVisibility, this);
                this.calendarView.on('eventCreateTriggered', $.proxy(this._createCustomEvent, this));
                this.calendarView.on('eventEditTriggered', $.proxy(this._editCustomEvent, this));
                this.calendarView.on('eventDeleteTriggered', $.proxy(this._deleteCustomEvent, this));

                this.collection.on('remove', this._onDeleteCalendar, this);
                this.collection.on('change', this._onChangeCalendar, this);
                this.collection.on('add', this._onAddCalendar, this);

                this.model.on('change:hideWeekends', this.onHideWeekendsHandler, this);
                this.model.on('change:calendarView', this._onUserDataViewChange, this);
            },
            initializeTooltips: function() {
                this.$('.calendar-quick-filters-label').tooltip({gravity: 'w'});
            },
            _onUserDataViewChange: function(model) {
                var view = model.get('calendarView') || 'month';
                this.setCalendarView(view);
            },
            fillCalendarsInUrl: function() {
                var calendars = _.pluck(this.collection.where({visible: true}), 'id');
                router.navigate('calendars=' + calendars.join(','), {
                    replace: true,
                    trigger: false
                });
            },
            _onChangeCalendar: function(calendar) {
                this.buildCalendarList();
                this.buildQuickFilterList();

                this.calendarView.removeEventSource(calendar.id);
                if (calendar.get('favorite') && calendar.get('visible') && !calendar.get('hasError'))
                    this.calendarView.addEventSource(calendar.id);
                this.fillCalendarsInUrl();
            },
            _onAddCalendar: function(calendar) {
                this.buildCalendarList();
                this.calendarView.removeEventSource(calendar.id);
                if (calendar.get('visible') && !calendar.get('hasError'))
                    this.calendarView.addEventSource(calendar.id);
                this.fillCalendarsInUrl();
            },
            _onDeleteCalendar: function(calendar) {
                this.buildCalendarList();
                this.calendarView.removeEventSource(calendar.id);
                this.fillCalendarsInUrl();
            },
            _onCalendarDropdownShow: function() {
                var calendarId = $(this).data('id');
                $('div.calendar-list-item-block[data-id=' + calendarId + ']').toggleClass('calendar-list-item-selected', true);
            },
            _onCalendarDropdownHide: function() {
                var calendarId = $(this).data('id');
                $('div.calendar-list-item-block[data-id=' + calendarId + ']').toggleClass('calendar-list-item-selected', false);
            },
            startLoadingCalendarsCallback: function() {
                AJS.dim();
                JIRA.Loading.showLoadingIndicator();
            },
            finishLoadingCalendarsCallback: function() {
                this.$('.calendar-name').removeClass('not-active');
                JIRA.Loading.hideLoadingIndicator();
                if (!$('.aui-dialog2[aria-hidden=false]').length)
                    AJS.undim();
            },
            loadFullCalendar: function(view, hideWeekends, timezone, workingDays) {
                this.updatePeriodButton(view);
                this.calendarView.setTimezone(timezone);
                this.calendarView.init(view, hideWeekends, workingDays);
                var $calendarEl = $("#calendar-full-calendar");
                $calendarEl.find('.fc-toolbar .fc-button').removeClass('fc-state-default fc-button').addClass('aui-button');
                $calendarEl.find('.fc-button-group').addClass('aui-buttons');
            },
            showCalendarFeedView: function(e) {
                e.preventDefault();
                new CalendarFeedDialog({model: this.model, collection: this.collection}).show();
            },
            createCalendar: function(e) {
                e.preventDefault();
                this.$('.aui-page-panel-nav').click();

                var calendarDialogView = new CalendarDialog({
                    model: new CalendarDetail(),
                    collection: this.collection,
                    userData: this.model
                });
                calendarDialogView.show();
            },
            importCalendar: function(e) {
                e.preventDefault();
                this.$('.aui-page-panel-nav').click();

                var importView = new CalendarImportDialog({
                    model: this.model,
                    collection: this.collection//user calendars
                });
                importView.show();
            },
            openIssueNavigator: function(e) {
                e.preventDefault();
                e.stopPropagation();
                this.$('.aui-page-panel-nav').click();

                var calendar = this.collection.get($(e.currentTarget).closest('div.aui-dropdown2').data('id'));
                var sourceType = calendar.get('source').split('_')[0];
                var url;
                if (sourceType == 'project')
                    url = 'jql=project%3D' + calendar.get('source').split('_')[1];
                else if (sourceType == 'filter')
                    url = 'filter=' + calendar.get('source').split('_')[1];
                else if (sourceType == 'jql')
                    url = 'jql=' + calendar.get('source').split('_')[1].replace(/"/g, '\'');
                window.open(AJS.format('{0}/issues/?{1}', AJS.contextPath(), url));
            },
            openGanttDiagram: function(e) {
                e.preventDefault();
                e.stopPropagation();
                this.$('.aui-page-panel-nav').click();

                var calendarId = $(e.currentTarget).closest('div.aui-dropdown2').data('id');
                window.open(AJS.format('{0}/secure/MailRuGanttDiagram.jspa#calendar={1}', AJS.contextPath(), calendarId));
            },
            editCalendar: function(e) {
                e.preventDefault();
                e.stopPropagation();
                this.$('.aui-page-panel-nav').click();

                var calendarId = $(e.currentTarget).closest('div.aui-dropdown2').data('id');
                var calendarDetail = new CalendarDetail({id: calendarId});
                calendarDetail.fetch({
                    success: $.proxy(function(model) {
                        var calendarDialogView = new CalendarDialog({
                            model: model,
                            collection: this.collection
                        });
                        calendarDialogView.show();
                    }, this),
                    error: function(request) {
                        alert(request.responseText);
                    }
                });
            },
            toggleWeekendsVisibility: function() {
                Preferences.setItem('mailrucalendar.hideWeekends', !this.model.get('hideWeekends'));
                this.model.set({hideWeekends: !this.model.get('hideWeekends')});
            },
            onHideWeekendsHandler: function(model) {
                this.calendarView.toggleWeekends(model.get('hideWeekends'));
            },
            changeCalendarView: function(e) {
                e.preventDefault();
                this.setCalendarView(this.$(e.currentTarget).data('view-type'));
            },
            toggleCalendarVisibility: function(e) {
                e.preventDefault();
                var self = this;
                var $calendarNameLink = this.$(e.currentTarget);
                if ($calendarNameLink.hasClass('not-working'))
                    return;

                var calendarId = $calendarNameLink.closest('div.calendar-list-item-block').data('id');
                var calendar = this.collection.get(calendarId);
                if (!calendar || !calendar.get('favorite')) {
                    this.model.save({calendars: [calendarId]}, {
                        success: function() {
                            self.collection.fetch({
                                success: function() {
                                    self.setUrlCalendars();
                                }
                            });
                        },
                        error: function(model, response) {
                            alert(response.responseText);
                        }
                    });
                    return;
                }

                if (calendar.get('visible'))
                    this.startLoadingCalendarsCallback();
                $calendarNameLink.addClass('not-active');

                $.ajax({
                    type: 'PUT',
                    url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/' + calendarId + '/visibility/' + !calendar.get('visible'),
                    success: function() {
                        calendar.set('visible', !calendar.get('visible'));
                    },
                    error: function(request) {
                        self.finishLoadingCalendarsCallback();
                        alert(request.responseText);
                    }
                });
            },
            setCalendarView: function(viewName) {
                if (this.calendarView.getViewType() != viewName)
                    this.calendarView.setView(viewName);
                if (this.model.get('calendarView') != viewName) {
                    Preferences.setItem('mailrucalendar.calendarView', viewName);
                    this.model.set({calendarView: viewName});
                }
            },
            setUrlCalendar: function(calendar) {
                if (calendar !== undefined)
                    this.urlCalendars = calendar;
                if (mainView.calendarsLoaded && this.urlCalendars !== undefined) {
                    if (calendar !== undefined)
                        this.collection.each(function(calendar) {
                            calendar.set({visible: false}, {silent: !!this.urlCalendars.length});
                        }, this);

                    var notInCollection = [];
                    _.each(this.urlCalendars, function(calendarId) {
                        var calendar = this.collection.get(calendarId);
                        if (calendar && calendar.get('favorite'))
                            $.ajax({
                                context: this,
                                type: 'PUT',
                                url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/' + calendarId + '/visibility/true',
                                success: function() {
                                    calendar.set({visible: true});
                                }
                            });
                        else
                            notInCollection.push(calendarId);
                    }, this);

                    if (notInCollection.length)
                        $.ajax({
                            context: this,
                            type: 'GET',
                            url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/find',
                            data: {
                                id: notInCollection
                            },
                            success: function(response) {
                                var html = AJS.I18n.getText('ru.mail.jira.plugins.calendar.addSharedCalendars');
                                _.each(response, function(calendar) {
                                    html += JIRA.Templates.Plugins.MailRuCalendar.calendarEntry({
                                        calendar: calendar,
                                        enableButton: false
                                    });
                                });
                                this.$('#mailrucalendar-message-calendar-list').html(html).removeClass('hidden');
                            }
                        });
                    else
                        this.$('#mailrucalendar-message-calendar-list').empty().addClass('hidden');
                }
            },
        });

        /* Router */
        var ViewRouter = Backbone.Router.extend({
            routes: {
                'calendar=:calendar': 'setCalendar'
            },
            setCalendar: function(calendar) {
                mainView.setUrlCalendar(calendar);
            }
        });

        var mainView = new MainView({collection: new UserCalendarCollection(), model: new UserData()});
        var router = new ViewRouter();

        /* Fetch data */
        mainView.model.fetch({
            success: function(model) {
                model.set({
                    hideWeekends: Preferences.getItem('mailrucalendar.hideWeekends') === 'true',
                    calendarView: Preferences.getItem('mailrucalendar.calendarView') || 'month'
                });
                var view = model.get('calendarView') || 'month';
                if (view == 'basicWeek')
                    view = 'agendaWeek';
                moment.tz.setDefault(model.get('timezone'));
                mainView.loadFullCalendar(view, model.get('hideWeekends'), model.get('timezone'), model.get('workingDays'));

                Backbone.history.start();

                if (model.has('nextFeedbackShow') && model.get('feedbackShowCount') == 0)
                    $.ajax({
                        type: 'PUT',
                        url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/userPreference/likeFlagShown/false'
                    });
                else if (model.has('nextFeedbackShow') && !model.get('pluginRated')
                    && moment(model.get('nextFeedbackShow')).isBefore(moment()))
                    new LikeFlag();
            },
            error: function(model, response) {
                var msg = 'Error while trying to load user preferences. ';
                if (response.responseText)
                    msg += response.responseText;
                alert(msg);
                mainView.loadFullCalendar('month', false, 'local', [1, 2, 3, 4, 5]);

                Backbone.history.start();
            }
        });
        mainView.startLoadingCalendarsCallback();
        mainView.collection.fetch({
            silent: true,
            success: function(collection) {
                var hasEnabledCalendar = false;
                mainView.buildCalendarList();
                mainView.buildQuickFilterList();
                collection.each(function(calendar) {
                    if (calendar.get('visible') && !calendar.get('hasError')) {
                        mainView.calendarView.addEventSource(calendar.id, true);
                        hasEnabledCalendar = true;
                    }
                });
                mainView.fillCalendarsInUrl();
                hasEnabledCalendar || mainView.finishLoadingCalendarsCallback();
                mainView.calendarsLoaded = true;
                mainView.setUrlCalendars();
            },
            error: function(request) {
                alert(request.responseText);
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
    _tmr.push({id: "2706504", type: "pageView", start: (new Date()).getTime()});
    (function(d, w, id) {
        if (d.getElementById(id)) return;
        var ts = d.createElement("script");
        ts.type = "text/javascript";
        ts.async = true;
        ts.id = id;
        ts.src = (d.location.protocol == "https:" ? "https:" : "http:") + "//top-fwz1.mail.ru/js/code.js";
        var f = function() {
            var s = d.getElementsByTagName("script")[0];
            s.parentNode.insertBefore(ts, s);
        };
        if (w.opera == "[object Opera]") {
            d.addEventListener("DOMContentLoaded", f, false);
        } else {
            f();
        }
    })(document, window, "topmailru-code");
}
