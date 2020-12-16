define(
    'calendar/confirm-dialog',
    ['jquery', 'underscore', 'backbone', 'aui/dialog2'],
    function($, _, Backbone, dialog2) {
        return Backbone.View.extend({
            events: {
                'click #mailrucalendar-confirm-dialog-ok': '_ok',
                'click #mailrucalendar-confirm-dialog-cancel': '_cancel'
            },
            render: function(options) {
                this.$el.html(JIRA.Templates.Plugins.MailRuCalendar.ConfirmDialog.dialog(_.defaults(options, {okText: AJS.I18n.getText("admin.common.words.confirm")})));
                $(document.body).append(this.$el);
                this.setElement($('#mailrucalendar-confirm-dialog').unwrap());
                return this;
            },
            initialize: function(options) {
                this.render(options);
                this.dialog = dialog2('#mailrucalendar-confirm-dialog');

                this._okHandler = options.okHandler;
                this._cancelHandler = options.cancelHandler;

                this.dialog.on('hide', $.proxy(this.destroy, this));
            },
            destroy: function() {
                this.remove();
            },
            show: function() {
                this.dialog.show();
            },
            _ok: function() {
                this.dialog.hide();
                this._okHandler && this._okHandler.call();
            },
            _cancel: function() {
                this.dialog.hide();
                this._cancelHandler && this._cancelHandler.call();
            }
        });
    }
);
