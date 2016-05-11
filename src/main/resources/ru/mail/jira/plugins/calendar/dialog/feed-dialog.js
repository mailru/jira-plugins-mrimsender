define('calendar/feed-dialog', ['jquery', 'underscore', 'backbone', 'calendar/confirm-dialog'], function($, _, Backbone, ConfirmDialog) {
    return Backbone.View.extend({
        events: {
            'click #calendar-feed-dialog-ok': 'hide',
            'click #calendar-feed-dialog-delete': '_resetLink',
            'change #calendar-feed-dialog-calendars': '_updateCalendarFeedUrl'
        },
        render: function() {
            this.$el.html(JIRA.Templates.Plugins.MailRuCalendar.FeedDialog.dialog());
            $(document.body).append(this.$el);
            this.setElement($('#calendar-feed-dialog').unwrap());
            return this;
        },
        initialize: function() {
            this.render();

            new Clipboard('#url-field-copy');
            this.dialog = AJS.dialog2('#calendar-feed-dialog');
            this.$calendarSelect = this.$('#calendar-feed-dialog-calendars');
            this.$urlField = this.$('.url-field');

            this.listenTo(this.model, 'change:icalUid', this._updateCalendarFeedUrl);
            this.listenTo(this.collection, 'change', this._initCalendarsSelect);
            this.dialog.on('hide', $.proxy(this.destroy, this));
            this.$('form').submit($.proxy(this._onFormSubmit, this));
        },
        destroy: function() {
            this.remove();
        },
        /* Public methods */
        hide: function() {
            this.dialog.hide();
        },
        show: function() {
            this.dialog.show();
            this._initCalendarsSelect();
            this.$calendarSelect.focus();
        },
        /* Private methods */
        _onFormSubmit: function(e) {
            e.preventDefault();
        },
        _resetLink: function() {
            var confirmText = '<p>' + AJS.I18n.getText("ru.mail.jira.plugins.calendar.feed.dialog.reset.confirm1") + '</p>' +
                '<p>' + AJS.I18n.getText("ru.mail.jira.plugins.calendar.feed.dialog.reset.confirm2") + '</p>';
            var confirmDialog = new ConfirmDialog({
                okText: AJS.I18n.getText("admin.common.words.reset"),
                header: AJS.I18n.getText("ru.mail.jira.plugins.calendar.feed.dialog.reset.confirmHeader"),
                text: confirmText,
                okHandler: $.proxy(function() {
                    $.ajax({
                        type: 'POST',
                        url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/userPreference/ics/feed',
                        success: $.proxy(function() {
                            this.model.unset('icalUid');
                        }, this),
                        error: function(request) {
                            alert(request.responseText);
                        }
                    });
                }, this)
            });

            confirmDialog.show();
        },
        _initCalendarsSelect: function() {
            var data = this.collection.map(function(calendar) {
                return {id: calendar.get('id'), text: calendar.get('name')};
            });
            this.$calendarSelect.auiSelect2({
                multiple: true,
                allowClear: true,
                data: data
            });

            var selectedIds = this.collection.pluck.call({
                models: this.collection.filter(function(calendar) {
                    return !!calendar.get('visible');
                })
            }, 'id');
            this.$calendarSelect.auiSelect2('val', selectedIds);

            this._updateCalendarFeedUrl();
        },
        _updateCalendarFeedUrl: function() {
            this.$urlField.val('');
            if (!this.model.has('icalUid')) {
                this.model.fetch();
                return;
            }
            var calendars = this.$calendarSelect.val();
            if (calendars) {
                var calUrl = calendars.split(',').join('-');
                this.$urlField.val(window.location.origin + AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/' + this.model.get('icalUid') + '/' + calUrl + '.ics');
            }
        }
    });
});