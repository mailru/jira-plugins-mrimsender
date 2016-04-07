require(['jquery',
    'underscore',
    'backbone',
    'calendar/feedback-flag',
    'calendar/calendar-view',
    'calendar/calendar-dialog',
    'calendar/confirm-dialog',
    'calendar/feed-dialog',
    'calendar/import-dialog',
    'calendar/timeline-view'], function($, _, Backbone, LikeFlag, CalendarView,
                                        CalendarDialog, ConfirmDialog, CalendarFeedDialog, CalendarImportDialog) {
    $.fn.select2.defaults = $.extend($.fn.select2.defaults, {
        formatNoMatches: function() {
            return AJS.I18n.getText('ru.mail.jira.plugins.calendar.select2.formatNoMatches');
        },
        //formatInputTooShort: function(input, min) {
        //    var n = min - input.length;
        //    return "Please enter " + n + " more character" + (n == 1 ? "" : "s");
        //},
        //formatInputTooLong: function(input, max) {
        //    var n = input.length - max;
        //    return "Please delete " + n + " character" + (n == 1 ? "" : "s");
        //},
        //formatSelectionTooBig: function(limit) {
        //    return "You can only select " + limit + " item" + (limit == 1 ? "" : "s");
        //},
        formatLoadMore: function(pageNumber) {
            return AJS.I18n.getText('ru.mail.jira.plugins.calendar.select2.formatLoadMore');
        },
        formatSearching: function() {
            return AJS.I18n.getText('ru.mail.jira.plugins.calendar.select2.formatSearching');
        }
    });

    AJS.toInit(function() {
        collectTopMailCounterScript();

        /* Models and Collections*/
        var UserData = Backbone.Model.extend({url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/userPreference'});
        var Calendar = Backbone.Model.extend();
        var CalendarDetail = Backbone.Model.extend({urlRoot: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/'});
        var CalendarCollection = Backbone.Collection.extend({
            model: Calendar,
            url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/all',
            comparator: function(a, b) {
                var aCount = a.get('usersCount'),
                    bCount = b.get('usersCount'),
                    aName = a.get('name'),
                    bName = b.get('name');
                if (!aCount && !bCount || aCount == bCount)
                    if (aName < bName)
                        return -1;
                    else if (aName > bName)
                        return 1;
                    else
                        return 0;
                else if (aCount < bCount)
                    return 1;
                else
                    return -1;
            }
        });

        var MainView = Backbone.View.extend({
            el: 'body',
            events: {
                'click #calendar-create-feed': 'showCalendarFeedView',
                'click #calendar-create': 'createCalendar',
                'click #calendar-import': 'importCalendar',
                'click .calendar-name': 'toggleCalendarVisibility',
                'click .calendar-issue-navigator': 'openIssueNavigator',
                'click .calendar-delete': 'deleteCalendar',
                'click .calendar-edit': 'editCalendar',
                'click .calendar-remove': 'removeFavoriteCalendar'
            },
            initialize: function() {
                this.calendarView = new CalendarView({enableFullscreen: true});

                this.calendarView.on('addSource render', this.startLoadingCalendarsCallback, this);
                this.calendarView.on('renderComplete', this.finishLoadingCalendarsCallback, this);
                this.calendarView.on('render', this.updatePeriodButton, this);
                this.calendarView.on('changeWeekendsVisibility', this.toggleWeekendsVisibility, this);

                this.collection.on('remove', this._onDeleteCalendar, this);
                this.collection.on('change', this._onChangeCalendar, this);
                this.collection.on('add', this._onAddCalendar, this);

                this.model.on('change:hideWeekends', this.onHideWeekendsHandler, this);
                this.model.on('change:calendarView', this._onUserDataViewChange, this);
            },
            _onUserDataViewChange: function(model) {
                var view = model.get('calendarView') || "month";
                this.setCalendarView(view);
            },
            _onChangeCalendar: function(calendar) {
                this.buildCalendarList();

                this.calendarView.removeEventSource(calendar.id);
                if (calendar.get('favorite') && calendar.get('visible') && !calendar.get('hasError'))
                    this.calendarView.addEventSource(calendar.id);
            },
            _onAddCalendar: function(calendar) {
                this.buildCalendarList();
                this.calendarView.removeEventSource(calendar.id);
                if (calendar.get('visible') && !calendar.get('hasError'))
                    this.calendarView.addEventSource(calendar.id);
            },
            _onDeleteCalendar: function(calendar) {
                this.buildCalendarList();
                this.calendarView.removeEventSource(calendar.id);
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
                console.log('startLoadingCalendarsCallback');
            },
            finishLoadingCalendarsCallback: function() {
                this.$('.calendar-name').removeClass('not-active');
                JIRA.Loading.hideLoadingIndicator();
                if (!$('.aui-dialog2[aria-hidden=false]').length)
                    AJS.undim();
                console.log('finishLoadingCalendarsCallback');
            },
            loadFullCalendar: function(view, hideWeekends) {
                this.updatePeriodButton(view);
                this.calendarView.init(view, hideWeekends);
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
                    collection: this.collection
                });
                importView.show();
            },
            openIssueNavigator: function(e) {
                e.preventDefault();
                e.stopPropagation();
                this.$('.aui-page-panel-nav').click();

                var calendar = this.collection.get($(e.currentTarget).closest('div.aui-dropdown2').data('id'));
                window.open(AJS.format("{0}/issues/?{1}", AJS.contextPath(), calendar.get('source').split('_')[0] == 'project' ? 'jql=project%3D' + calendar.get('source').split('_')[1] : 'filter=' + calendar.get('source').split('_')[1]));
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
                            collection: this.collection,
                            userData: this.model
                        });
                        calendarDialogView.show();
                    }, this),
                    error: function(request) {
                        alert(request.responseText);
                    }
                });
            },
            toggleWeekendsVisibility: function() {
                this.model.save({hideWeekends: !this.model.get('hideWeekends')}, {
                    error: $.proxy(function(xhr) {
                        var msg = "Error while trying to update user hide weekends option. ";
                        if (xhr.responseText)
                            msg += xhr.responseText;
                        alert(msg);
                    }, this)
                });
            },
            onHideWeekendsHandler: function(model) {
                this.calendarView.toggleWeekends(model.get('hideWeekends'));
            },
            toggleCalendarVisibility: function(e) {
                e.preventDefault();
                var $calendarNameLink = this.$(e.currentTarget);
                if ($calendarNameLink.hasClass('not-working'))
                    return;

                var calendarId = $calendarNameLink.closest('div.calendar-list-item-block').data('id');
                var calendar = this.collection.get(calendarId);

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
                        alert(request.responseText);
                    }
                });
            },
            setCalendarView: function(viewName) {
                if (this.calendarView.getViewType() != viewName)
                    this.calendarView.setView(viewName);
                if (this.model.get('calendarView') != viewName)
                    this.model.save({calendarView: viewName}, {
                        error: function(xhr) {
                            var msg = "Error while trying to update user default view to => " + viewName;
                            if (xhr.responseText)
                                msg += xhr.responseText;
                            alert(msg);
                        }
                    });
            },
            updatePeriodButton: function(viewName) {
                var $periodItem = this.$('#calendar-period-dropdown a[href$="' + viewName + '"]');

                this.$('#calendar-period-dropdown a').removeClass('aui-dropdown2-checked');
                this.$('#calendar-period-dropdown a').removeClass('checked');
                $periodItem.addClass('aui-dropdown2-checked');
                this.$('#calendar-period-btn .trigger-label').text($periodItem.text());
            },
            removeFavoriteCalendar: function(e) {
                e.preventDefault();
                e.stopPropagation();
                this.$('.aui-page-panel-nav').click();

                var calendar = this.collection.get($(e.currentTarget).closest('div.aui-dropdown2').data('id'));

                if (this.calendarView.isCalendarInSources(calendar.id))
                    this.startLoadingCalendarsCallback();
                $.ajax({
                    url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/userPreference/favorite/' + calendar.id,
                    type: "DELETE",
                    error: $.proxy(function(xhr) {
                        this.finishLoadingCalendarsCallback();
                        alert(xhr.responseText || "Internal error");
                    }, this),
                    success: $.proxy(function() {
                        calendar.set({favorite: false, visible: false, usersCount: calendar.get('usersCount') - 1});
                    }, this)
                });
            },
            deleteCalendar: function(e) {
                e.preventDefault();
                e.stopPropagation();
                this.$('.aui-page-panel-nav').click();

                var calendar = this.collection.get($(e.currentTarget).closest('div.aui-dropdown2').data('id'));
                var confirmText = AJS.format('<p>{0}</p><p>{1}</p>',
                    AJS.format(AJS.I18n.getText('ru.mail.jira.plugins.calendar.confirmDelete1'), '<b>' + calendar.get('name') + '</b>'),
                    AJS.format(AJS.I18n.getText('ru.mail.jira.plugins.calendar.confirmDelete2'), calendar.get('usersCount') || 0));
                var confirmDialog = new ConfirmDialog({
                    okText: AJS.I18n.getText('common.words.delete'),
                    header: AJS.I18n.getText('ru.mail.jira.plugins.calendar.confirmDeleteHeader'),
                    text: confirmText,
                    okHandler: $.proxy(function() {
                        if (this.calendarView.isCalendarInSources(calendar.id))
                            this.startLoadingCalendarsCallback();
                        $.ajax({
                            url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/' + calendar.id,
                            type: 'DELETE',
                            error: $.proxy(function(xhr) {
                                this.finishLoadingCalendarsCallback();
                                alert(xhr.responseText || 'Internal error');
                            }, this),
                            success: $.proxy(function() {
                                this.collection.remove(calendar);
                            }, this)
                        });
                    }, this)
                });

                confirmDialog.show();
            },
            buildCalendarList: function() {
                var htmlFavoriteCalendars = '';
                var sorted = this.collection.sortBy(function(calendar) {
                    return calendar.get('hasError') ? '' : calendar.get('name');
                });
                _.each(sorted, function(calendar) {
                    var json = calendar.toJSON();
                    if (calendar.get('visible') || calendar.get('favorite'))
                        htmlFavoriteCalendars += JIRA.Templates.Plugins.MailRuCalendar.calendarEntry({calendar: json});
                });

                if (htmlFavoriteCalendars) {
                    $('#mailrucalendar-empty-calendar-list').css('display', 'none');
                    $('#calendar-favorite-calendar-list').empty().css('display', 'block').append(htmlFavoriteCalendars);
                } else {
                    $('#calendar-favorite-calendar-list').empty().css('display', 'none');
                    $('#mailrucalendar-empty-calendar-list').css('display', 'block');
                }

                this.collection.each($.proxy(function(calendar) {
                    if (calendar.get('visible') || calendar.get('favorite'))
                        AJS.$('#calendar-buttons-dropdown-' + calendar.id).on({
                            'aui-dropdown2-show': this._onCalendarDropdownShow,
                            'aui-dropdown2-hide': this._onCalendarDropdownHide
                        });
                }, this));
            }
        });

        /* Router */
        var ViewRouter = Backbone.Router.extend({
            routes: {
                '': 'defaultHandler',
                'period/:view': 'switchPeriod'
            },
            availableOptions: {
                view: {
                    quarter: true,
                    month: true,
                    agendaWeek: true,
                    agendaDay: true,
                    timeline: true
                }
            },
            defaultHandler: function() {
                this.navigate('period/' + mainView.calendarView.getViewType());
            },
            switchPeriod: function(view) {
                view = this.validateViewType(view);
                this.navigate('period/' + view);
                mainView.setCalendarView(view);
            },
            validateViewType: function(view) {

                return this.availableOptions.view[view] ? view : 'month';
            }
        });

        var mainView = new MainView({collection: new CalendarCollection(), model: new UserData()});
        var router = new ViewRouter();

        /* Fetch data */
        mainView.model.fetch({
            success: function(model) {
                var view = model.get('calendarView') || 'month';
                if (view == 'basicWeek')
                    view = 'agendaWeek';
                mainView.loadFullCalendar(view, model.get('hideWeekends'));

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
                mainView.loadFullCalendar('month', false);

                Backbone.history.start();
            }
        });
        mainView.startLoadingCalendarsCallback();
        mainView.collection.fetch({
            silent: true,
            success: function(collection) {
                var hasEnabledCalendar = false;
                mainView.buildCalendarList();
                collection.each(function(calendar) {
                    if (calendar.get('visible') && !calendar.get('hasError')) {
                        mainView.calendarView.addEventSource(calendar.id, true);
                        hasEnabledCalendar = true;
                    }
                });
                hasEnabledCalendar || mainView.finishLoadingCalendarsCallback();
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
