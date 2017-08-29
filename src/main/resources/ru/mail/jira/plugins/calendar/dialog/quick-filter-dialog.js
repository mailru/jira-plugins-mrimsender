define('calendar/quick-filter-dialog', ['jquery', 'underscore', 'backbone', 'calendar/jql-auto-complete-utils'], function($, _, Backbone, CalendarJQLAutoComplete) {
    return Backbone.View.extend({
        events: {
            'click .calendar-quick-filters-join' : 'changeQuickFiltersJoin',
            'click #calendar-quick-filter-add': 'addQuickFilter',
            'click .calendar-quick-filter-edit': 'editQuickFilter',
            'click .calendar-quick-filter-update': 'updateQuickFilter',
            'click .calendar-quick-filter-edit-cancel': 'cancelEditQuickFilter',
            'click .calendar-quick-filter-delete': 'deleteQuickFilter',
            'click .calendar-quick-filter-favourite': 'addToFavourite',
            'click #mailrucalendar-quick-filter-dialog-close': 'close'
        },
        render: function() {
            this.$el.html(JIRA.Templates.Plugins.MailRuCalendar.QuickFilterDialog.dialog({calendar: this.calendar.toJSON()}));
            $(document.body).append(this.$el);
            CalendarJQLAutoComplete.initialize('mailrucalendar-quick-filter-jql-create', 'mailrucalendar-quick-filter-jql-error-create');
            this.setElement($('#mailrucalendar-quick-filter-dialog').unwrap());
            this.initializeTooltips();
            var htmlQuickFilters = '';
            this.collection.each(function(quickFilter) {
                htmlQuickFilters += JIRA.Templates.Plugins.MailRuCalendar.QuickFilterDialog.renderQuickFilterEntry({
                    quickFilter: quickFilter.toJSON()
                });
            });
            if (htmlQuickFilters.length)
                $('#mailrucalendar-quick-filter-create-row').after(htmlQuickFilters);
            return this;
        },
        initialize: function(options) {
            this.calendar = options.calendar;
            this.calendars = options.calendars;
            this.render();
            this.dialog = AJS.dialog2('#mailrucalendar-quick-filter-dialog');

            this._keypressHandler = $.proxy(this._keypressHandler, this);
            $(document.body).on('keyup', this._keypressHandler);
            this.dialog.on('hide', $.proxy(this.destroy, this));

            this.collection.on('add', this._onAddQuickFilter, this);
            this.collection.on('change', this._onChangeQuickFilter, this);
            this.collection.on('remove', this._onDeleteQuickFilter, this);
        },
        initializeTooltips: function() {
            this.$('#calendar-quick-filters-join .aui-iconfont-help').tooltip({gravity: 'w'});
        },
        _keypressHandler: function(e) {
            switch (e.which) {
                // esc
                case 27 :
                    this.dialog.hide();
                    break;
            }
        },
        _serializeQuickFilter: function(id, $row) {
            var quickFilter = {
                name:  $row.find('.mailrucalendar-quick-filter-name').val(),
                jql: $row.find('.mailrucalendar-quick-filter-jql').val(),
                description: $row.find('.mailrucalendar-quick-filter-description').val(),
                share: !!$row.find('.mailrucalendar-quick-filter-share:checked').length
            };
            if (id.length > 0)
                quickFilter.id = id;
            return quickFilter;
        },
        changeQuickFiltersJoin: function(e) {
            e.preventDefault();
            e.stopPropagation();
            var join = $(e.currentTarget).val();
            var self = this;
            $.ajax({
                type: 'PUT',
                context: this,
                data: {
                    join: join
                },
                url: AJS.format('{0}/rest/mailrucalendar/1.0/calendar/{1}/quickFilterJoin', AJS.contextPath(), this.calendar.id),
                success: function() {
                    this.$('.calendar-quick-filters-join').each(function() {
                        $(this).attr("aria-pressed", false);
                        if ($(this).val() === join)
                            $(this).attr("aria-pressed", true);

                    });
                },
                error: function(request) {
                    alert(request.responseText);
                }
            });
        },
        addQuickFilter: function(e) {
            e && e.preventDefault();
            var $row = $(e.target).parents('tr');
            $.ajax({
                type: 'POST',
                context: this,
                contentType: 'application/json',
                url: AJS.format('{0}/rest/mailrucalendar/1.0/calendar/{1}/quickFilter', AJS.contextPath(), this.calendar.id),
                data: JSON.stringify(this._serializeQuickFilter('', $row)),
                success: function(response) {
                    this._clearErrorMessages($row);
                    this.collection.add(response, {merge: true});
                    $row.find('input[type="text"], textarea').val("");
                    $row.find('input[type="checkbox"]').attr('checked', false);
                },
                error: function(xhr) {
                    this._ajaxErrorHandler(xhr, $row);
                }
            });
        },
        editQuickFilter: function(e) {
            e && e.preventDefault();
            var $row = $(e.target).parents('tr');
            var id = $row.data('id');
            $row.replaceWith(JIRA.Templates.Plugins.MailRuCalendar.QuickFilterDialog.renderEditQuickFilterEntry({
                quickFilter: this.collection.get(id).toJSON()
            }));
            CalendarJQLAutoComplete.initialize(AJS.format('mailrucalendar-quick-filter-jql-{0}', id), AJS.format('mailrucalendar-quick-filter-jql-error-{0}', id));
        },
        updateQuickFilter: function(e) {
            e && e.preventDefault();
            var $row = $(e.target).parents('tr');
            var id = $row.data('id');
            $.ajax({
                type: 'PUT',
                context: this,
                contentType: 'application/json',
                url: AJS.format('{0}/rest/mailrucalendar/1.0/calendar/{1}/quickFilter/{2}', AJS.contextPath(), this.calendar.id, id),
                data: JSON.stringify(this._serializeQuickFilter(id, $row)),
                success: function(response) {
                    this._clearErrorMessages($row);
                    this.collection.add(response, {merge: true});
                },
                error: function(xhr) {
                    this._ajaxErrorHandler(xhr, $row);
                }
            });
        },
        cancelEditQuickFilter: function(e) {
            e && e.preventDefault();
            var $row = $(e.target).parents('tr');
            $row.replaceWith(JIRA.Templates.Plugins.MailRuCalendar.QuickFilterDialog.renderQuickFilterEntry({
                quickFilter: this.collection.get($row.data('id')).toJSON()
            }));
        },
        deleteQuickFilter: function(e) {
            e && e.preventDefault();
            var $row = $(e.target).parents('tr');
            var id = $row.data('id');
            var quickFilter = this.collection.get(id);
            $.ajax({
                type: 'DELETE',
                context: this,
                contentType: 'application/json',
                url: AJS.format('{0}/rest/mailrucalendar/1.0/calendar/{1}/quickFilter/{2}', AJS.contextPath(), this.calendar.id, id),
                success: function() {
                    this._clearErrorMessages($row);
                    this.collection.remove(quickFilter);
                },
                error: function(xhr) {
                    this._ajaxErrorHandler(xhr, $row);
                }
            });
        },
        _clearErrorMessages: function($row) {
            $row.find('.error').val('').addClass('hidden');
            $('#mailrucalendar-quick-filter-dialog-error-panel').val('').addClass('hidden');
        },
        _ajaxErrorHandler: function(xhr, $row) {
            var responseText = xhr.responseText;
            if (responseText) {
                try {
                    var response = JSON.parse(responseText);
                    if (response.hasOwnProperty('errors')) {
                        $.map(response.errors, function(error, field) {
                            $row.find('#mailrucalendar-quick-filter-' + field + '-error').removeClass('hidden').text(error);
                            $row.find('#mailrucalendar-quick-filter-' + field).focus();
                        })
                    }
                    if (response.hasOwnProperty('errorMessages'))
                        this.$('#mailrucalendar-quick-filter-dialog-error-panel').removeClass('hidden').text(response.errorMessages);
                    if (!response.hasOwnProperty('errors') && !response.hasOwnProperty('errorMessages'))
                        this.$('#mailrucalendar-quick-filter-dialog-error-panel').removeClass('hidden').text(responseText);
                } catch (e) {
                    this.$('#mailrucalendar-quick-filter-dialog-error-panel').removeClass('hidden').text(responseText);
                }
            }
        },
        destroy: function() {
            this.remove();
            this.calendars.fetch();
        },
        show: function() {
            this.dialog.show();
        },
        _onAddQuickFilter: function(quickFilter) {
            $('#mailrucalendar-quick-filter-table').find('tbody').append(JIRA.Templates.Plugins.MailRuCalendar.QuickFilterDialog.renderQuickFilterEntry({
                quickFilter: quickFilter.toJSON()
            }));
        },
        _onChangeQuickFilter: function(quickFilter) {
            $('.mailrucalendar-quick-filter-row[data-id="' + quickFilter.id + '"]').replaceWith(JIRA.Templates.Plugins.MailRuCalendar.QuickFilterDialog.renderQuickFilterEntry({
                quickFilter: quickFilter.toJSON()
            }));
        },
        _onDeleteQuickFilter: function(quickFilter) {
            $('.mailrucalendar-quick-filter-row[data-id="' + quickFilter.id + '"]').remove();
        },
        addToFavourite: function(e) {
            e && e.preventDefault();
            var $row = $(e.target).parents('tr');
            var $icon = $(e.target);
            var addToFavourite = $icon.hasClass('aui-iconfont-star');
            $.ajax({
                type: 'PUT',
                context: this,
                data: {
                    addToFavourite: !addToFavourite
                },
                url: AJS.format('{0}/rest/mailrucalendar/1.0/calendar/{1}/addToFavouriteQuickFilter/{2}', AJS.contextPath(), this.calendar.id, $(e.target).parents('tr').data('id')),
                success: function() {
                    this._clearErrorMessages($row);
                    if (!addToFavourite)
                        $icon.removeClass('aui-iconfont-unstar').addClass('aui-iconfont-star');
                    else
                        $icon.removeClass('aui-iconfont-star').addClass('aui-iconfont-unstar');
                },
                error: function(xhr) {
                    this._ajaxErrorHandler(xhr, $row);
                }
            });
        },
        close: function() {
            this.dialog.hide();
        }
    });
});