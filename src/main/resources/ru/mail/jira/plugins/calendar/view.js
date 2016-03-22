require(['jquery',
    'underscore',
    'backbone',
    'calendar/like-flag',
    'calendar/calendar-view',
    'calendar/calendar-dialog',
    'calendar/confirm-dialog',
    'calendar/feed-dialog',
    'calendar/import-dialog',
    'calendar/timeline-view'], function($, _, Backbone, LikeFlag, CalendarView,
                                        CalendarDialog, ConfirmDialog, CalendarFeedDialog, CalendarImportDialog) {
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
            loadingCounter: 0,
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
                this.calendarView = new CalendarView();

                this.calendarView.on('addSource render', this.startLoadingCalendarsCallback, this);
                this.calendarView.on('removeSource renderComplete', this.finishLoadingCalendarsCallback, this);
                this.calendarView.on('addSourceSuccess', this.changeEventSourceCallback, this);
                this.calendarView.on('render', this.updatePeriodButton, this);
                this.calendarView.on('changeWeekendsVisibility', this.toggleWeekendsVisibility, this);

                this.collection.on('request', this.startLoadingCalendarsCallback, this);
                this.collection.on('sync', this.finishLoadingCalendarsCallback, this);
                this.collection.on('remove', this.removeCalendarFromList, this);
                this.collection.on('change:favorite', this._onCalendarFavoriteChange, this);
                this.collection.on('change', this._onCalendarChange, this);
                this.collection.on('add', this._onCalendarAdd, this);

                this.model.on('change:hideWeekends', this.onHideWeekendsHandler, this);
                this.model.on('change:calendarView', this._onUserDataViewChange, this);
            },
            _onUserDataViewChange: function(model) {
                var view = model.get('calendarView') || "month";
                this.setCalendarView(view);
            },
            _onCalendarFavoriteChange: function(calendar) {
                if (calendar.get('favorite'))
                    this.addCalendarToList(calendar);
                else
                    this.removeCalendarFromList(calendar);
            },
            _onCalendarChange: function(calendar) {
                var $calendarBlock = $('#calendar-list-item-block-' + calendar.id);
                $calendarBlock.find('span.aui-nav-item-label').text(calendar.get('name'));
                $calendarBlock.data('color', calendar.get('color'));
                $calendarBlock.toggleClass('calendar-visible', calendar.get('visible'));

                this.calendarView.removeEventSource(calendar.id);
                if (calendar.get('favorite') && calendar.get('visible') && !calendar.get('hasError'))
                    this.calendarView.addEventSource(calendar.id);
                else {
                    this.changeEventSourceCallback(calendar.id, false, calendar.get('error'));

                    if (!calendar.get('favorite'))
                        this.removeCalendarFromList(calendar);
                }
            },
            _onCalendarAdd: function(calendar) {
                this.addCalendarToList(calendar);
                this.calendarView.removeEventSource(calendar.id);
                if (calendar.get('visible') && !calendar.get('hasError'))
                    this.calendarView.addEventSource(calendar.id);
            },
            _onCalendarDropdownShow: function() {
                var calendarId = $(this).data('id');
                $('div.calendar-list-item-block[data-id=' + calendarId + ']').toggleClass('calendar-list-item-selected', true);
            },
            _onCalendarDropdownHide: function() {
                var calendarId = $(this).data('id');
                $('div.calendar-list-item-block[data-id=' + calendarId + ']').toggleClass('calendar-list-item-selected', false);
            },
            changeEventSourceCallback: function(calendarId, visible, error) {
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
            startLoadingCalendarsCallback: function() {
                this.loadingCounter++;
                AJS.dim();
                JIRA.Loading.showLoadingIndicator();
            },
            finishLoadingCalendarsCallback: function() {
                this.loadingCounter--;
                if (this.loadingCounter <= 0) {
                    $('.calendar-name').removeClass('not-active');
                    AJS.undim();
                    JIRA.Loading.hideLoadingIndicator();
                    this.loadingCounter = 0;
                }
            },
            addCalendarToList: function(calendar) {
                this.$('#mailrucalendar-empty-calendar-list').css('display', 'none');
                var listBlock = this.$('#calendar-favorite-calendar-list');
                listBlock.css('display', 'block');

                var sorted = _.sortBy(this.collection.filter(function(cal) {
                    return cal.get('favorite');
                }), function(cal) {
                    return cal.get('name');
                });
                var index = _.indexOf(sorted, calendar);
                if (sorted.length - 1 == index)
                    listBlock.append(JIRA.Templates.Plugins.MailRuCalendar.calendarEntry({calendar: calendar.toJSON()}));
                else {
                    var calendarNext = sorted[index + 1];
                    this.$('.calendar-list-item-block[data-id="' + calendarNext.id + '"]').before(JIRA.Templates.Plugins.MailRuCalendar.calendarEntry({calendar: calendar.toJSON()}));
                }

                AJS.$("#calendar-buttons-dropdown-" + calendar.id).on({
                    "aui-dropdown2-show": this._onCalendarDropdownShow,
                    "aui-dropdown2-hide": this._onCalendarDropdownHide
                });
            },
            removeCalendarFromList: function(calendar) {
                var calendarBlock = this.$('#calendar-list-item-block-' + calendar.id);
                var blockContainer = calendarBlock.parent('.aui-navgroup-inner');
                calendarBlock.remove();
                this.calendarView.removeEventSource(calendar);

                if (!blockContainer.find('.calendar-list-item-block').length)
                    blockContainer.css('display', 'none');

                var calendarListEmpty = !this.collection.find(function(calendar) {
                    return calendar.get('favorite');
                });
                calendarListEmpty && $('#mailrucalendar-empty-calendar-list').css('display', 'block');
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
                this.startLoadingCalendarsCallback();
                this.model.save({hideWeekends: !this.model.get('hideWeekends')}, {
                    success: $.proxy(this.finishLoadingCalendarsCallback, this),
                    error: $.proxy(function(xhr) {
                        this.finishLoadingCalendarsCallback();
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
                this.calendarView.setView(viewName);
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
                $.ajax({
                    url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/userPreference/favorite/' + calendar.id,
                    type: "DELETE",
                    error: $.proxy(function(xhr) {
                        this.finishLoadingCalendarsCallback();
                        alert(xhr.responseText || "Internal error");
                    }, this),
                    success: function() {
                        calendar.set({favorite: false, visible: false, usersCount: calendar.get('usersCount') - 1});
                    }
                });
            },
            deleteCalendar: function(e) {
                e.preventDefault();
                e.stopPropagation();
                this.$('.aui-page-panel-nav').click();

                var calendar = this.collection.get($(e.currentTarget).closest('div.aui-dropdown2').data('id'));
                var confirmText = '<p>' + AJS.I18n.getText('ru.mail.jira.plugins.calendar.confirmDelete1').replace('%s', '<b>' + calendar.get('name') + '</b>') + '</p>';
                if (calendar.get('usersCount') > 1)
                    confirmText += '<p>' + AJS.I18n.getText('ru.mail.jira.plugins.calendar.confirmDelete2').replace('%s', calendar.get('usersCount')) + '</p>';
                var confirmDialog = new ConfirmDialog({
                    okText: AJS.I18n.getText('common.words.delete'),
                    header: AJS.I18n.getText('ru.mail.jira.plugins.calendar.confirmDeleteHeader'),
                    text: confirmText,
                    okHandler: $.proxy(function() {
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
                    basicWeek: true,
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
                mainView.loadFullCalendar(view, model.get('hideWeekends'));

                Backbone.history.start();

                if (model.has('lastLikeFlagShown') && model.get('likeShowCount') == 0)
                    $.ajax({
                        type: 'PUT',
                        url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/userPreference/likeFlagShown/false'
                    });
                else if (model.has('lastLikeFlagShown') && !model.get('pluginRated') &&
                    (model.get('likeShowCount') == 1 && moment(model.get('lastLikeFlagShown')).add(2, 'w').isBefore(moment())
                    || model.get('likeShowCount') == 2 && moment(model.get('lastLikeFlagShown')).add(2, 'w').isBefore(moment())
                    || model.get('likeShowCount') == 3 && moment(model.get('lastLikeFlagShown')).add(1, 'M').isBefore(moment())
                    || model.get('likeShowCount') == 4 && moment(model.get('lastLikeFlagShown')).add(2, 'M').isBefore(moment())
                    || model.get('likeShowCount') == 5 && moment(model.get('lastLikeFlagShown')).add(4, 'M').isBefore(moment())
                    || model.get('likeShowCount') >= 6 && moment(model.get('lastLikeFlagShown')).add(8, 'M').isBefore(moment())))
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
        mainView.collection.fetch({
            silent: true,
            success: function(collection) {
                var htmlFavoriteCalendars = '';
                var eventSources = [];

                if (!!collection.findWhere({visible: true}))
                    mainView.startLoadingCalendarsCallback();

                var sorted = collection.sortBy(function(calendar) {
                    return calendar.get('name')
                });
                _.each(sorted, function(calendar) {
                    var json = calendar.toJSON();
                    if (calendar.get('visible') || calendar.get('favorite'))
                        htmlFavoriteCalendars += JIRA.Templates.Plugins.MailRuCalendar.calendarEntry({calendar: json});
                    if (calendar.get('visible') && !calendar.get('hasError'))
                        eventSources.push(calendar.id);
                });

                if (htmlFavoriteCalendars)
                    $('#calendar-favorite-calendar-list').css('display', 'block').append(htmlFavoriteCalendars);
                else
                    $('#mailrucalendar-empty-calendar-list').css('display', 'block');

                collection.each(function(calendar) {
                    if (calendar.get('visible') || calendar.get('favorite'))
                        AJS.$('#calendar-buttons-dropdown-' + calendar.id).on({
                            'aui-dropdown2-show': mainView._onCalendarDropdownShow,
                            'aui-dropdown2-hide': mainView._onCalendarDropdownHide
                        });
                });

                for (var a = 0; a < eventSources.length; a++)
                    mainView.calendarView.addEventSource(eventSources[a], true);
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
