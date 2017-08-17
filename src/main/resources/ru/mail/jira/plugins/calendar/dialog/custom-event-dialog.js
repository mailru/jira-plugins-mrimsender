define('calendar/custom-event-dialog', [
    'require',
    'jquery',
    'underscore',
    'backbone',
    'jira/ajs/select/multi-select',
    'calendar/reminder'
], function(require, $, _, Backbone, MultiSelect, Reminder) {
    var UserPickerUtil = JIRA ? JIRA.UserPickerUtil : null; //workaround for jira 7.0
    require(['jira/field/user-picker-util'], function(object) {
        UserPickerUtil = object;
    });

    return Backbone.View.extend({
        events: {
            'click #calendar-custom-event-dialog-ok:not([disabled])': '_submit',
            'click #calendar-custom-event-dialog-cancel': 'hide',
            'change #custom-event-dialog-allDay': '_initTimeFields',
            'change #custom-event-dialog-calendar': '_handleCalendarChange',
            'change #custom-event-dialog-type': '_handleTypeChange'
        },
        render: function() {
            var defaultTime = moment({hour: 12, minute: 0}).format(this.timeFormat);
            this.$el.html(JIRA.Templates.Plugins.MailRuCalendar.CustomEventDialog.dialog({
                model: this.jsonModel,
                calendarId: this.model.get('calendarId'),
                calendars: this.calendars,
                formattedStartDate: moment(this.jsonModel.startDate).format('YYYY-MM-DD'),
                formattedEndDate: moment(this.jsonModel.endDate).format('YYYY-MM-DD'),
                formattedStartTime: !this.jsonModel.allDay ? moment(this.jsonModel.startDate).format(this.timeFormat) : defaultTime,
                formattedEndTime: !this.jsonModel.allDay ? moment(this.jsonModel.endDate).format(this.timeFormat) : null,
                reminderName: this.jsonModel.reminder ? Reminder.names[this.jsonModel.reminder] : Reminder.none
            }));
            $(document.body).append(this.$el);
            this.setElement($('#calendar-custom-event-dialog').unwrap());
            return this;
        },
        initialize: function(options) {
            this.timeFormat = AJS.Meta.get('date-time');
            this.calendars = options.calendars;
            this.jsonModel = this.model.toJSON();

            this.render();
            this.$okButton = this.$('#calendar-custom-event-dialog-ok');
            this.$cancelButton = this.$('#calendar-custom-event-dialog-cancel');
            this.dialog = AJS.dialog2('#calendar-custom-event-dialog');

            if (this.calendars.length > 0) {
                this._initDateFields();
                this._initTypeField();
                this._initFieldsFromModel();
                this._initTimeFields();
                this._initParticipantsPicker();
            }

            this.$('form').submit($.proxy(this._onFormSubmit, this));
            this._keypressHandler = $.proxy(this._keypressHandler, this);
            $(document.body).on('keyup', this._keypressHandler);
            this.dialog.on('hide', $.proxy(this._destroy, this));
        },
        _initFieldsFromModel: function() {
            var model = this.jsonModel;
            if (model.id) {
                var defaultType = {
                    'text': model.eventTypeName,
                    'id': model.eventTypeId,
                    'avatar': model.eventTypeAvatar
                };
                this.$('#custom-event-dialog-type').auiSelect2('data', defaultType);
            }
        },
        _initDateFields: function() {
            var $startDateField = $('#custom-event-dialog-startDate');
            var $endDateField = $('#custom-event-dialog-endDate');

            $startDateField.datePicker({
                'overrideBrowserDefault': true,
                'dateFormat': 'yy-mm-dd'
            });
            $endDateField.datePicker({
                'overrideBrowserDefault': true,
                'dateFormat': 'yy-mm-dd'
            });

            $startDateField.attr('placeholder', AJS.I18n.getText('ru.mail.jira.plugins.calendar.customEvents.dialog.datePlaceholder'));
            $endDateField.attr('placeholder', AJS.I18n.getText('ru.mail.jira.plugins.calendar.customEvents.dialog.datePlaceholder'))
        },
        _initTimeFields: function() {
            if ($('#custom-event-dialog-allDay').prop('checked')) {
                $('#custom-event-dialog-startTime').hide();
                $('#custom-event-dialog-endTime').hide();
            } else {
                $('#custom-event-dialog-startTime').show();
                $('#custom-event-dialog-endTime').show();
            }
        },
        _initParticipantsPicker: function() {
            var $el = $('#custom-event-dialog-participantNames');
            this.participantsSelect = new MultiSelect({
                element: $el,
                showDropdownButton: false,
                itemAttrDisplayed: 'label',
                removeOnUnSelect: true,
                removeDuplicates: true,
                submitInputVal: false,
                ajaxOptions: {
                    url: AJS.contextPath() + '/rest/api/1.0/users/picker',
                    query: true, // keep going back to the sever for each keystroke
                    data: function (query) {
                        return {
                            showAvatar: true,
                            query: query,
                            exclude: $el.val()
                        };
                    },
                    formatResponse: UserPickerUtil.formatResponse
                }
            });
        },
        _initTypeField: function() {
            this.$('#custom-event-dialog-type').auiSelect2({
                minimumInputLength: 0,
                minimumResultsForSearch: -1,
                ajax: {
                    url: AJS.contextPath() + '/rest/mailrucalendar/1.0/customEvent/type/list',
                    dataType: 'json',
                    quietMillis: 100,
                    data: function(term) {
                        return {calendarId: $('#custom-event-dialog-calendar').val()};
                    },
                    results: function(data) {
                        return {
                            results: $.map(data, function(e) {
                                return {
                                    text: e.name,
                                    id: e.id,
                                    avatar: e.avatar,
                                    system: e.system,
                                    reminder: e.reminder
                                }
                            })
                        };
                    },
                    cache: false
                },
                formatResult: $.proxy(this._formatTypeField, this),
                formatSelection: $.proxy(this._formatTypeField, this),
                escapeMarkup: function(m) { return m; },
                dropdownCssClass: 'calendar-dialog-source-dropdown'
            });
        },
        _handleCalendarChange: function() {
            $('#custom-event-dialog-type').auiSelect2('data', null);
            $('#custom-event-dialog-reminder').text(Reminder.none);
        },
        _handleTypeChange: function() {
            var reminder = $('#custom-event-dialog-type').auiSelect2('data').reminder;
            $('#custom-event-dialog-reminder').text(reminder ? Reminder.names[reminder] : Reminder.none);
        },
        _formatTypeField: function(type) {
            return '<span class="custom-event-type-avatar custom-type-icon custom-type-icon-' + type.avatar + '"></span>' +
                    AJS.escapeHtml(type.text)
        },
        _destroy: function() {
            this.remove();
            $(document.body).off('keyup', this._keypressHandler);
        },

        _onFormSubmit: function(e) {
            e.preventDefault();
            this.$okButton.click();
        },
        _submit: function(e) {
            e.preventDefault();

            this.$okButton.attr('disabled', 'disabled');
            this.$cancelButton.attr('disabled', 'disabled');

            var serializedData = this._serialize();

            if (serializedData.startDate === 'Invalid date') {
                this.$('#custom-event-dialog-startDate-error')
                    .removeClass('hidden')
                    .text(AJS.I18n.getText('ru.mail.jira.plugins.calendar.customEvents.dialog.error.invalidDate'));
                this.$('#custom-event-dialog-startDate').focus();
                this.$okButton.removeAttr('disabled');
                this.$cancelButton.removeAttr('disabled');
                return;
            }

            if (serializedData.endDate === 'Invalid date') {
                this.$('#custom-event-dialog-endDate-error')
                    .removeClass('hidden')
                    .text(AJS.I18n.getText('ru.mail.jira.plugins.calendar.customEvents.dialog.error.invalidDate'));
                this.$('#custom-event-dialog-endDate').focus();
                this.$okButton.removeAttr('disabled');
                this.$cancelButton.removeAttr('disabled');
                return;
            }

            this.model.save(serializedData, {
                success: $.proxy(this._ajaxSuccessHandler, this),
                error: $.proxy(this._ajaxErrorHandler, this)
            });
        },
        _ajaxSuccessHandler: function(model, response) {
            this.model.trigger('change', this.model);
            this.$okButton.removeAttr('disabled');
            this.$cancelButton.removeAttr('disabled');
            this.hide();

            if (this.options.successHandler) {
                this.options.successHandler();
            }
        },
        _ajaxErrorHandler: function(model, response) {
            this.$el.find('.error').addClass('hidden');
            var field = response.getResponseHeader('X-Atlassian-Rest-Exception-Field');
            if (field) {
                this.$('#custom-event-dialog-' + field + '-error').removeClass('hidden').text(response.responseText);
                var $field = this.$('#custom-event-dialog-' + field);
                if ($field.hasClass('select') || $field.hasClass('multi-select'))
                    this.$('#custom-event-dialog-' + field).auiSelect2('focus');
                else
                    this.$('#custom-event-dialog-' + field).focus();
            } else
                this.$('#custom-event-dialog-error-panel').removeClass('hidden').text(response.responseText);
            this.$okButton.removeAttr('disabled');
            this.$cancelButton.removeAttr('disabled');
        },
        _keypressHandler: function(e) {
            switch (e.which) {
                // esc
                case 27 :
                    if (!this.isDirty() || confirm(AJS.I18n.getText('ru.mail.jira.plugins.calendar.dialog.hideConfirm')))
                        this.hide();
                    break;
            }
        },
        _serialize: function(isComparison) {
            var title = $('#custom-event-dialog-title').val();
            var startDate = $('#custom-event-dialog-startDate').val();
            var startTime = $('#custom-event-dialog-startTime').val();
            var endDate = $('#custom-event-dialog-endDate').val();
            var endTime = $('#custom-event-dialog-endTime').val();
            var calendarId = $('#custom-event-dialog-calendar').val();
            var eventTypeId = $('#custom-event-dialog-type').val();
            var participants = $('#custom-event-dialog-participantNames').val();
            var allDay = $('#custom-event-dialog-allDay').prop('checked');

            var start = null;
            if (startDate) {
                if (allDay) {
                    start = moment.utc(startDate).format('x');
                } else {
                    var parsedStartTime = moment(startTime, this.timeFormat);
                    start = moment(startDate).hours(parsedStartTime.hours()).minutes(parsedStartTime.minutes()).format('x');
                }
            }

            var end = null;
            if (endDate) {
                if (allDay) {
                    end = moment.utc(endDate).format('x');
                } else {
                    var parsedEndTime = moment(endTime, this.timeFormat);
                    end = moment(endDate).hours(parsedEndTime.hours()).minutes(parsedEndTime.minutes()).format('x');
                }
            }

            return {
                title: title,
                calendarId: calendarId,
                startDate: start,
                endDate: end,
                eventTypeId: eventTypeId,
                participantNames: participants ? participants.join(', ') : null,
                allDay: allDay
            };
        },
        /* Public methods */
        show: function() {
            this.dialog.show();
            if (this.participantsSelect) {
                this.participantsSelect.updateItemsIndent();
            }
        },
        hide: function() {
            this.dialog.hide();
        },
        isDirty: function() {
            var orig = this.model.toJSON();
            var changed = this._serialize(true);
            orig.selectedEventEndId = orig.selectedEventEndId || undefined;
            changed.selectedEventEndId = changed.selectedEventEndId || undefined;

            return orig.title != changed.title || orig.eventId != changed.eventId || orig.participantNames != changed.participantNames ||
                orig.startDate != changed.startDate || orig.endDate != changed.endDate || orig.allDay != changed.allDay;
        }
    });
});
