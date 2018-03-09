define('calendar/import-dialog', ['jquery', 'underscore', 'backbone'], function($, _, Backbone) {
    var CalendarCollection = Backbone.Collection.extend({
        model: Backbone.Model.extend(),
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
    return Backbone.View.extend({
        events: {
            'click #calendar-import-dialog-ok': 'save',
            'click #calendar-import-dialog-cancel': 'hide',
            'click #mailrucalendar-calendar-import-dialog tbody tr': 'selectRow',
            'keyup #mailrucalendar-query': '_fillSearchResultTable'
        },
        render: function() {
            this.$el.html(JIRA.Templates.Plugins.MailRuCalendar.ImportDialog.dialog());
            $(document.body).append(this.$el);
            this.setElement($('#mailrucalendar-calendar-import-dialog').unwrap());
            return this;
        },
        initialize: function() {
            this.render();

            this.dialog = AJS.dialog2('#mailrucalendar-calendar-import-dialog');
            this.queryField = this.$('#mailrucalendar-query');
            this.table = this.$('#mailrucalendar-import-calendars-table');
            this.tableContent = this.table.find('tbody');
            this.noResult = this.$('#mailrucalendar-query-no-result');
            this.addBtn = this.$('#calendar-import-dialog-ok');

            this.selectedIds = [];
            this.allCalendarCollection = new CalendarCollection();

            this.dialog.on('hide', $.proxy(this.destroy, this));
            this.$('form').submit($.proxy(this._onFormSubmit, this));

            this.allCalendarCollection.fetch({
                success: $.proxy(function() {
                    if (this.allCalendarCollection.findWhere({favorite: false})) {
                        this._fillSearchResultTable();
                        this.queryField.select();
                    } else {
                        this.$('form').addClass('hidden');
                        this.$('#mailrucalendar-no-calendars-for-import').removeClass('hidden');
                    }
                }, this)
            });
        },
        destroy: function() {
            this.remove();
        },
        hide: function() {
            this.dialog.hide();
        },
        show: function() {
            this.dialog.show();
        },
        save: function() {
            this.model.save({calendars: this.selectedIds}, {
                patch: true,
                type : 'PUT',
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
            this.noResult.addClass('hidden');
            this.selectedIds = [];
        },
        _fillSearchResultTable: function() {
            var query = this.queryField.val();
            var result = this.allCalendarCollection.filter(function(calendar) {
                var name = calendar.get('name');
                var q = query.toLowerCase();

                return !calendar.get('favorite') && name && name.toLowerCase().indexOf(q) != -1;
            });
            if (result.length && this._equalsDataArray(this.tableData, result))
                return;

            this._onStartSearch();
            var rows = '';
            _.each(result, function(calendar) {
                rows += JIRA.Templates.Plugins.MailRuCalendar.ImportDialog.importTableRow(calendar.toJSON());
            });
            if (rows) {
                this.tableContent.append(rows);
                this.table.fadeIn(250);
            } else {
                this.noResult.removeClass('hidden');
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