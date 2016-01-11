(function($) {
    AJS.toInit(function() {
        Backbone.View.ConfirmDialog = Backbone.View.extend({
            el: '#mailrucalendar-confirm-dialog',
            events: {
                'click #mailrucalendar-confirm-dialog-ok': '_ok',
                'click #mailrucalendar-confirm-dialog-cancel': '_cancel'
            },
            initialize: function(options) {
                this.dialog = AJS.dialog2('#mailrucalendar-confirm-dialog');
                this.header = this.$('.aui-dialog2-header-main');
                this.text = this.$('.aui-dialog2-content');
                this.okBtn = this.$('#mailrucalendar-confirm-dialog-ok');

                this.header.empty();
                this.text.empty();

                options.okText && this.okBtn.text(options.okText);
                this.header.append(options.header);
                this.text.append(options.text);
                this._okHandler = options.okHandler;
                this._cancelHandler = options.cancelHandler;

                this.dialog.on('hide', $.proxy(this.destroy, this));
            },
            destroy: function() {
                this.stopListening();
                this.undelegateEvents();
                this.dialog.off();
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
    });
})(AJS.$);