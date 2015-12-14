(function ($) {
    AJS.toInit(function () {
        Backbone.View.CalendarFeedView = Backbone.View.extend({
            el: '#calendar-feed-dialog',
            events: {
                'click #calendar-feed-dialog-ok': 'hide',
                'click #calendar-feed-dialog-delete': 'resetLink',
                'change #calendar-feed-dialog-calendars': 'updateCalendarFeedUrl'
            },
            initialize: function() {
                this.clipboard = new Clipboard('#url-field-copy');
                this.dialog = AJS.dialog2('#calendar-feed-dialog');
                this.$calendarSelect = this.$('#calendar-feed-dialog-calendars');
                this.$urlField = this.$('.url-field');

                this.listenTo(this.model, 'change:icalUid', this.updateCalendarFeedUrl);
                this.listenTo(this.collection, 'change', this.initCalendarsSelect);
                this.dialog.on('hide', $.proxy(this.destroy, this));
            },
            destroy: function() {
                this.clipboard.destroy();
                this.stopListening();
                this.undelegateEvents();
                this.dialog.off();
            },
            /* Public methods */
            hide: function() {
                this.dialog.hide();
            },
            show: function() {
                this.dialog.show();
                this.initCalendarsSelect();
            },
            /* Private methods */
            resetLink: function() {
                if (confirm(AJS.I18n.getText("ru.mail.jira.plugins.calendar.feed.dialog.reset.confirm")))
                    $.ajax({
                        type: 'POST',
                        url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/userPreference/ics/feed',
                        success: $.proxy(function() {this.model.unset('icalUid');}, this),
                        error: function(request) {
                            alert(request.responseText);
                        }
                    });
            },
            initCalendarsSelect: function() {
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

                this.updateCalendarFeedUrl();
            },
            updateCalendarFeedUrl: function() {
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
})(AJS.$);