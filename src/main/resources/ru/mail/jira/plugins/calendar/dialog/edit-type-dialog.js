define(
    'calendar/edit-type-dialog',
    ['jquery', 'underscore', 'backbone', 'aui/dialog2'],
    function($, _, Backbone, dialog2) {
        return Backbone.View.extend({
            events: {
                'click #mailrucalendar-edit-type-dialog-cancel': '_cancel',
                'click .type-button': '_ok'
            },
            render: function(options) {
                this.$el.html(JIRA.Templates.Plugins.MailRuCalendar.EditTypeDialog.dialog(_.defaults(options)));
                $(document.body).append(this.$el);
                this.setElement($('#mailrucalendar-edit-type-dialog').unwrap());
                return this;
            },
            initialize: function(options) {
                this.render(options);
                this.dialog = dialog2('#mailrucalendar-edit-type-dialog');

                this._okHandler = options.okHandler;
                this._cancelHandler = options.cancelHandler;

                this.dialog.on('hide', $.proxy(this.destroy, this));
            },
            destroy: function() {
                this._cancel();
                this.remove();
            },
            show: function() {
                this.dialog.show();
            },
            _ok: function(event) {
                this.dialog.hide();
                this._okHandler && this._okHandler($(event.target).data('type'));
            },
            _cancel: function() {
                this.dialog.hide();
                this._cancelHandler && this._cancelHandler.call();
            }
        });
    }
);
