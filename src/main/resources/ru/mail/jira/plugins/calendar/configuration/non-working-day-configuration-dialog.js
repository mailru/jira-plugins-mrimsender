define(
    'calendar/non-working-day-configuration-dialog',
    ['jquery', 'underscore', 'backbone', 'aui/dialog2'],
    function($, _, Backbone, dialog2) {
        return Backbone.View.extend({
            events: {
                'click #calendar-non-working-day-add:not([disabled])': '_submit',
                'click #calendar-non-working-day-cancel': 'hide'
            },
            render: function() {
                this.$el.html(JIRA.Templates.Plugins.MailRuCalendar.Configuration.WorkingDays.dialog);
                $(document.body).append(this.$el);
                this.setElement($('#calendar-non-working-day-configuration-dialog').unwrap());
                this.initDateTimePicker('calendar-non-working-date');
                return this;
            },
            initialize: function() {
                this.render();
                this.$okButton = this.$('#calendar-non-working-day-add');
                this.$cancelButton = this.$('#calendar-non-working-day-cancel');
                this.dialog = dialog2('#calendar-non-working-day-configuration-dialog');

                this.$('form').submit($.proxy(this._onFormSubmit, this));
                this.dialog.on('hide', $.proxy(this._destroy, this));
            },
            initDateTimePicker: function(fieldId) {
                $.ajax({
                    type: 'GET',
                    url: AJS.contextPath() + '/rest/mailrucalendar/1.0/configuration/workingDays/calendar/params',
                    success: function(result) {
                        Calendar.setup({
                            firstDay : 1,
                            inputField : fieldId,
                            button : fieldId + '_trigger',
                            align : 'Br',
                            singleClick : true,
                            showsTime : false,
                            useISO8601WeekNumbers : false,
                            ifFormat : result.dateFormat,
                            timeFormat: 24
                        })
                    }
                });
            },
            _destroy: function() {
                this.remove();
            },
            /* Public methods */
            show: function() {
                this.dialog.show();
            },
            hide: function() {
                this.dialog.hide();
            },
            /* Private methods */
            _onFormSubmit: function(e) {
                e.preventDefault();
                this.$okButton.click();
            },
            _submit: function(e) {
                e.preventDefault();
                this.$okButton.attr('disabled', 'disabled');
                this.$cancelButton.attr('disabled', 'disabled');
                this.$('#calendar-non-working-day-configuration-dialog-error-panel').addClass('hidden').text('');
                this.$('div.error').addClass('hidden').text('');

                this.model.save(this._serialize(), {
                    success: $.proxy(this._ajaxSuccessHandler, this),
                    error: $.proxy(this._ajaxErrorHandler, this)
                });
            },
            _serialize: function() {
                return {
                    date: this.$('#calendar-non-working-date').val(),
                    description: this.$('#calendar-non-working-day-description').val()
                };
            },
            _ajaxSuccessHandler: function(model, response) {
                this.collection.add(response, {merge: true});
                this.model.trigger('change', this.model);
                this.$okButton.removeAttr('disabled');
                this.$cancelButton.removeAttr('disabled');
                this.hide();
            },
            _ajaxErrorHandler: function(model, response) {
                var field = response.getResponseHeader('X-Atlassian-Rest-Exception-Field');
                if (field) {
                    this.$('#calendar-' + field + '-error').removeClass('hidden').text(response.responseText);
                    this.$('#calendar-' + field).focus();
                } else
                    this.$('#calendar-non-working-day-configuration-dialog-error-panel').removeClass('hidden').text(response.responseText);
                this.$okButton.removeAttr('disabled');
                this.$cancelButton.removeAttr('disabled');
            }
        });
    }
);
