define('calendar/import-dialog', ['jquery', 'underscore', 'backbone'], function($, _, Backbone) {
    var tableRowTpl;
    AJS.toInit(function() {
        tableRowTpl = _.template($('#import-calendars-row-template').html());
    });

    return Backbone.View.extend({
        el: '#mailrucalendar-calendar-import-dialog',
        events: {
            'click #calendar-import-dialog-ok': 'save',
            'click #calendar-import-dialog-cancel': 'hide',
            'click #mailrucalendar-calendar-import-dialog tbody tr': 'selectRow',
            'keyup #mailrucalendar-query': '_fillSearchResultTable'
        },
        initialize: function() {
            this.dialog = AJS.dialog2('#mailrucalendar-calendar-import-dialog');
            this.queryField = this.$('#mailrucalendar-query');
            this.queryFieldContainer = this.$('.mailrucalendar-search-field');
            this.table = this.$('#mailrucalendar-import-calendars-table');
            this.tableContent = this.table.find('tbody');
            this.adminMessage = this.$('#mailrucalendar-admin-message');
            this.noResult = this.$('#mailrucalendar-query-no-result');
            this.noCalendars = this.$('#mailrucalendar-no-calendars-for-import');
            this.addBtn = this.$('#calendar-import-dialog-ok');
            this.selectedIds = [];

            this.dialog.on('hide', $.proxy(this.destroy, this));
            this.$('form').submit($.proxy(this._onFormSubmit, this));
        },
        destroy: function() {
            this.stopListening();
            this.undelegateEvents();
            this.dialog.off();
            this.$('form').off();

            this.queryField.val('');
            this.tableContent.empty();
        },
        hide: function() {
            this.dialog.hide();
        },
        show: function() {
            this.noCalendars.hide();
            this.adminMessage.show();
            this.queryFieldContainer.show();
            this.table.hide();
            this.noResult.hide();
            this.addBtn.attr('disabled', 'disabled');
            this.dialog.show();
            if (this.collection.findWhere({favorite: false}))
                this._fillSearchResultTable();
            else {
                this.adminMessage.hide();
                this.queryFieldContainer.hide();
                this.noCalendars.show();
            }
        },
        save: function() {
            this.model.save({calendars: this.selectedIds}, {
                success: $.proxy(function() {
                    this.dialog.hide();
                    this.collection.fetch();
                }, this),
                error: function(model, response) {
                    alert(response.responseText);
                }
            });
        },
        selectRow: function(e) {
            var row = $(e.currentTarget);
            var checkbox = row.find('input');
            var selected = checkbox.prop("checked");
            checkbox.prop('checked', !selected);
            if (!selected)
                this.selectedIds.push(checkbox.data('id'));
            else
                this.selectedIds = _.without(this.selectedIds, checkbox.data('id'));

            if (this.selectedIds.length)
                this.addBtn.removeAttr('disabled');
            else
                this.addBtn.attr('disabled', 'disabled');
        },
        _onFormSubmit: function(e) {
            e.preventDefault();
            this._fillSearchResultTable();
        },
        _onStartSearch: function() {
            this.table.hide();
            this.tableContent.empty();
            this.noResult.hide();
            this.selectedIds = [];
        },
        _fillSearchResultTable: function() {
            var query = this.queryField.val();
            var result = this.collection.filter(function(calendar) {
                var name = calendar.get('name');
                var owner = calendar.get('owner');
                var ownerFullName = calendar.get('ownerFullName');
                var q = query.toLowerCase();

                return !calendar.get('hasError') && !calendar.get('favorite') &&
                    (name && name.toLowerCase().indexOf(q) != -1
                    || owner && owner.toLowerCase().indexOf(q) != -1
                    || ownerFullName && ownerFullName.toLowerCase().indexOf(q) != -1);
            });
            if (result.length && this._equalsDataArray(this.tableData, result))
                return;

            this._onStartSearch();
            var rows = '';
            _.each(result, function(calendar) {
                rows += tableRowTpl({calendar: calendar.toJSON()});
            });
            if (rows) {
                this.tableContent.append(rows);
                this.table.fadeIn(250);
            } else {
                this.noResult.show();
                this.noResult.find('span').text(query);
            }
            this.tableData = result;
        },
        _equalsDataArray: function(a, b) {
            if (!a || !b || a.length != b.length)
                return false;
            for (var i = 0; i < a.length; i++)
                if (a[i].id != b[i].id)
                    return false;
            return true;
        }
    });
});