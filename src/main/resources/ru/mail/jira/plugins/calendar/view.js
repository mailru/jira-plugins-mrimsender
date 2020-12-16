require(['jquery',
    'underscore',
    'mailrucal/moment',
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
], function($, _, moment, Backbone, LikeFlag, CalendarView, CalendarDialog, ConfirmDialog, CalendarFeedDialog, CalendarImportDialog, QuickFilterDialog, CustomEventDialog, Preferences) {
    // Override default texts for auiSelect2 messages
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

                this.calendarView.on('addSource', this.startLoadingCalendarsCallback, this);
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
                var view = model.get('calendarView') || 'dayGridMonth';
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
                var start = Preferences.getItem('mailrucalendar.start');
                var end = Preferences.getItem('mailrucalendar.end');
                this.calendarView.init(view, hideWeekends, workingDays, start, end);
                var $calendarEl = $("#calendar-full-calendar");
                $calendarEl.find('.fc-toolbar .fc-button').removeClass('fc-button-primary fc-button').addClass('aui-button');
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
                this.calendarView.reload();
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
            setUrlCalendars: function(calendars) {
                if (calendars !== undefined)
                    this.urlCalendars = calendars;
                if (mainView.calendarsLoaded && this.urlCalendars !== undefined) {
                    if (calendars !== undefined)
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
            updatePeriodButton: function(viewName) {
                var $periodItem = this.$('#calendar-period-dropdown a[data-view-type=' + viewName + ']');

                this.$('#calendar-period-dropdown a').removeClass('aui-dropdown2-checked');
                this.$('#calendar-period-dropdown a').removeClass('checked');
                $periodItem.addClass('aui-dropdown2-checked checked');
                this.$('#calendar-period-btn .trigger-label').text($periodItem.text());
            },
            updateViewInterval: function() {
                var view = this.calendarView.getView();
                var todayRange;
                if (view.type === 'timeline') {
                    todayRange = view.getCurrentData().viewSpec.optionDefaults.getTimelineHelper().computeRange(moment(this.calendarView.getNow()));
                } else {
                    todayRange = this.calendarView.computeRange(this.calendarView.getNow());
                }
                Preferences.setItem('mailrucalendar.start', !moment(todayRange.start).isSame(moment(view.activeStart)) ? moment(view.activeStart).format() : '');
                Preferences.setItem('mailrucalendar.end', !moment(todayRange.end).isSame(moment(view.activeEnd)) ? moment(view.activeEnd).format() : '');
            },
            configureQuickFilters: function(e) {
                e.preventDefault();
                e.stopPropagation();
                this.$('.aui-page-panel-nav').click();

                var calendarId = $(e.currentTarget).closest('div.aui-dropdown2').data('id');
                var calendar = this.collection.get(calendarId);

                var quickFilterCollection = new QuickFilterCollection([], {calendarId: calendarId});
                quickFilterCollection.fetch({
                    success: function(collection) {
                        var quickFilterDialog = new QuickFilterDialog({
                            collection: collection,
                            calendar: calendar,
                            calendars: mainView.collection
                        });
                        quickFilterDialog.show();
                    },
                    error: function(request) {
                        alert(request.responseText);
                    }
                });
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
                    type: 'DELETE',
                    error: $.proxy(function(xhr) {
                        this.finishLoadingCalendarsCallback();
                        alert(xhr.responseText || 'Internal error');
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

                $.getJSON(AJS.contextPath() + '/rest/mailrucalendar/1.0/customEvent/eventCount?calendarId=' + calendar.id, $.proxy(function(data) {
                    var confirmText = AJS.format('<p>{0}</p><p>{1}</p>',
                        AJS.I18n.getText('ru.mail.jira.plugins.calendar.confirmDelete1', '<b>' + AJS.escapeHtml(calendar.get('name')) + '</b>'),
                        AJS.I18n.getText('ru.mail.jira.plugins.calendar.confirmDelete2', calendar.get('usersCount') || 0));

                    if (data.count > 0) {
                        confirmText += '<p>' + AJS.I18n.getText('ru.mail.jira.plugins.calendar.confirmDelete3', data.count) + '</p>'
                    }

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
                }, this));
            },
            _createCustomEventFromMenu: function(e) {
                e.preventDefault();
                e.stopPropagation();

                var calendarId = $(e.currentTarget).closest('div.aui-dropdown2').data('id');

                this._createCustomEvent({allDay: true, calendarId: calendarId, startDate: moment().format('YYYY-MM-DD')});
            },
            _createCustomEvent: function(model) {
                var customEventDialogView = new CustomEventDialog({
                    model: new CustomEvent(model),
                    jsonModel: model,
                    userData: this.model,
                    calendars: this.collection.toJSON(),
                    successHandler: $.proxy(function() {
                        this.calendarView.reload();
                    }, this)
                });
                customEventDialogView.show();
            },
            _editCustomEvent: function(model, jsonModel) {
                var customEventDialogView = new CustomEventDialog({
                    model: model,
                    jsonModel: jsonModel,
                    calendar: null,
                    calendars: this.collection.toJSON(),
                    successHandler: $.proxy(function() {
                        this.calendarView.reload();
                    }, this)
                });
                customEventDialogView.show();
            },
            _deleteCustomEvent: function(model) {
                var text = AJS.I18n.getText('ru.mail.jira.plguins.calendar.customEvents.confirmDelete', '<b>' + AJS.escapeHtml(model.get('title')) + '</b>');

                if (model.get('parentId')) {
                    text = AJS.I18n.getText('ru.mail.jira.plugins.calendar.customEvents.recurring.deleteInstance') + '<br/>' + text;
                }

                var confirmDialog = new ConfirmDialog({
                    okText: AJS.I18n.getText('common.words.delete'),
                    header: AJS.I18n.getText('ru.mail.jira.plguins.calendar.customEvents.confirmDeleteHeader'),
                    text: text,
                    okHandler: $.proxy(function() {
                        $.ajax({
                            url: AJS.contextPath() + '/rest/mailrucalendar/1.0/customEvent/' + model.get('id'),
                            type: 'DELETE',
                            error: $.proxy(function(xhr) {
                                alert(xhr.responseText || 'Internal error');
                            }, this),
                            success: $.proxy(function() {
                                this.calendarView.reload();
                            }, this)
                        });
                    }, this)
                });

                confirmDialog.show();
            },
            toggleQuickFilter: function(e) {
                var self = this;
                var $element = $(e.currentTarget);
                var calendarId = $element.data('calendar-id');
                var filterId = $element.data('filter-id');
                var selected = !$element.data('selected');

                $.ajax({
                    type: 'PUT',
                    context: this,
                    url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/' + calendarId + '/selectQuickFilter/' + filterId,
                    data: {
                        selected: selected
                    },
                    success: $.proxy(function() {
                        self.collection.fetch({
                            success: function() {
                                self.setUrlCalendars();
                            }
                        });
                    }, this),
                    error: $.proxy(function(xhr) {
                        this.finishLoadingCalendarsCallback();
                        alert(xhr.responseText || 'Internal error');
                    }, this)
                });
            },
            buildCalendarList: function() {
                var htmlFavoriteCalendars = '';
                var sorted = this.collection.sortBy(function(calendar) {
                    return calendar.get('hasError') ? '' : calendar.get('name');
                });
                _.each(sorted, function(calendar) {
                    var json = calendar.toJSON();
                    if (calendar.get('visible') || calendar.get('favorite'))
                        htmlFavoriteCalendars += JIRA.Templates.Plugins.MailRuCalendar.calendarEntry({
                            calendar: json,
                            enableButton: true
                        });
                });

                if (htmlFavoriteCalendars) {
                    this.$('#mailrucalendar-empty-calendar-list').css('display', 'none');
                    this.$('#calendar-favorite-calendar-list').empty().css('display', 'block').append(htmlFavoriteCalendars);
                } else {
                    this.$('#calendar-favorite-calendar-list').empty().css('display', 'none');
                    this.$('#mailrucalendar-empty-calendar-list').css('display', 'block');
                }

                this.collection.each(function(calendar) {
                    if (calendar.get('visible') || calendar.get('favorite'))
                        this.$('#calendar-buttons-dropdown-' + calendar.id).on({
                            'aui-dropdown2-show': this._onCalendarDropdownShow,
                            'aui-dropdown2-hide': this._onCalendarDropdownHide
                        });
                }, this);
            },
            buildQuickFilterList: function() {
                this.$('.calendar-quick-filters dd').remove();
                var htmlQuickFilters = '';
                this.collection.each(function(calendar) {
                    if (calendar.get('visible') && !calendar.get('hasError')) {
                        htmlQuickFilters += JIRA.Templates.Plugins.MailRuCalendar.quickFilters({
                            calendar: calendar.toJSON()
                        });
                    }
                }, this);
                if (htmlQuickFilters.length === 0)
                    htmlQuickFilters = '<dd>' + AJS.I18n.getText('ru.mail.jira.plugins.calendar.quick.filter.empty') + '</dd>';
                this.$('#calendar-quick-filters dt').after(htmlQuickFilters);
            }
        });

        /* Router */
        var ViewRouter = Backbone.Router.extend({
            routes: {
                'calendars=:calendars': 'setCalendars'
            },
            setCalendars: function(calendars) {
                mainView.setUrlCalendars(calendars.split(','));
            }
        });

        var mainView = new MainView({collection: new UserCalendarCollection(), model: new UserData()});
        var router = new ViewRouter();

        /* Fetch data */
        mainView.model.fetch({
            silent: true,
            async: false,
            success: function(model) {
                var hideWeekends = Preferences.getItem('mailrucalendar.hideWeekends') === 'true';
                var calendarView = Preferences.getItem('mailrucalendar.calendarView') || 'dayGridMonth';
                var view = calendarView || 'dayGridMonth';
                if (view === 'dayGridWeek')
                    view = 'timeGridWeek';
                moment.tz.setDefault(model.get('timezone'));
                mainView.loadFullCalendar(view, hideWeekends, model.get('timezone'), model.get('workingDays'));

                model.set({
                    hideWeekends: hideWeekends,
                    calendarView: calendarView
                });

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
                mainView.loadFullCalendar('dayGridMonth', false, 'local', [1, 2, 3, 4, 5]);

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
