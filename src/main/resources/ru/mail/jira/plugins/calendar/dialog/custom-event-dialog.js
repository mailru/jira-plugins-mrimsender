define('calendar/custom-event-dialog', [
    'require',
    'jquery',
    'underscore',
    'backbone',
    'jira/ajs/select/multi-select',
    'calendar/reminder',
    'calendar/recurrence'
], function(require, $, _, Backbone, MultiSelect, Reminder, Recurrence) {
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
            'change #custom-event-dialog-type': '_handleTypeChange',
            'change #recurrence-type': '_handleRecurrenceTypeChange',
            'change input[name=editMode]': '_onEditModeChange',
            'focus #recurrence-end-date-value, #recurrence-end-time-value, #recurrence-end-count-value': '_focusRecurrenceEndOption'
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
                reminderName: this.jsonModel.reminder ? Reminder.names[this.jsonModel.reminder] : Reminder.none,
                recurrenceOptions: Recurrence.options,
                formattedRecurrenceEndDate: moment(this.jsonModel.recurrenceEndDate).format('YYYY-MM-DD'),
                formattedRecurrenceEndTime: !this.jsonModel.allDay ? moment(this.jsonModel.recurrenceEndDate).format(this.timeFormat) : null
            }));
            $(document.body).append(this.$el);
            this.setElement($('#calendar-custom-event-dialog').unwrap());
            return this;
        },
        initialize: function(options) {
            this.timeFormat = AJS.Meta.get('date-time');
            this.calendars = options.calendars;
            this.jsonModel = options.jsonModel || {};
            this.jsonModel.days = {};

            if (this.jsonModel.recurrenceType === 'DAYS_OF_WEEK') {
                _.each(this.jsonModel.recurrenceExpression.split(","), $.proxy(function(e) {
                    this.jsonModel.days[e] = true;
                }, this));
            }

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
                this._handleRecurrenceTypeChange();
                this._onEditModeChange();
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
            this._initDateField(this.$('#custom-event-dialog-startDate'));
            this._initDateField(this.$('#custom-event-dialog-endDate'));
            this._initDateField(this.$('#recurrence-end-date-value'));
        },
        _initDateField: function($field) {
            $field.datePicker({
                overrideBrowserDefault: true,
                dateFormat: 'yy-mm-dd',
                firstDay: AJS.Meta.get('mailrucal-use-iso8601') ? 1 : 0
            });
            $field.attr('placeholder', AJS.I18n.getText('ru.mail.jira.plugins.calendar.customEvents.dialog.datePlaceholder'));
        },
        _initTimeFields: function() {
            if (this.$('#custom-event-dialog-allDay').prop('checked')) {
                this.$('#custom-event-dialog-startTime').hide();
                this.$('#custom-event-dialog-endTime').hide();
                this.$('#recurrence-end-time-value').hide();
            } else {
                this.$('#custom-event-dialog-startTime').show();
                this.$('#custom-event-dialog-endTime').show();
                this.$('#recurrence-end-time-value').show();
            }
        },
        _initParticipantsPicker: function() {
            var $el = this.$('#custom-event-dialog-participantNames');
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
                        return {calendarId: this.$('#custom-event-dialog-calendar').val()};
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
        _onEditModeChange: function() {
            if (!this.jsonModel.recurrenceType) {
                return;
            }

            var editMode = this.$('input[name=editMode]:checked').val();

            var allDay = false;
            var startMoment = null;
            var endMoment = null;
            if (editMode === 'SINGLE_EVENT' || editMode === 'FOLLOWING_EVENTS') {
                this.$('.recurrence-type-field, .recurrence-field').hide();
                startMoment = moment(this.jsonModel.startDate);
                endMoment = this.jsonModel.endDate || null;
                if (endMoment) {
                    endMoment = moment(endMoment);
                }
                allDay = this.jsonModel.allDay;
            } else if (editMode === 'ALL_EVENTS') {
                this.$('.recurrence-type-field').show();
                startMoment = moment(this.jsonModel.originalStartDate);
                endMoment = this.jsonModel.originalEndDate || null;
                if (endMoment) {
                    endMoment = moment(endMoment);
                }
                allDay = this.jsonModel.originalAllDay;
                this._handleRecurrenceTypeChange();
            }

            if (editMode === 'FOLLOWING_EVENTS') {
                this.$('.recurrence-type-field').show();
                this._handleRecurrenceTypeChange();
            }

            this.$('#custom-event-dialog-startDate').val(startMoment.format('YYYY-MM-DD'));
            if (endMoment) {
                this.$('#custom-event-dialog-endDate').val(endMoment.format('YYYY-MM-DD'));
            }

            if (allDay) {
                this.$('#custom-event-dialog-allDay').prop('checked', 'checked');
            } else {
                this.$('#custom-event-dialog-allDay').prop('checked', null);
                this.$('#custom-event-dialog-startTime').val(startMoment.format(this.timeFormat));
                if (endMoment) {
                    this.$('#custom-event-dialog-endTime').val(endMoment.format(this.timeFormat));
                }
            }
            this._initTimeFields();
        },
        _focusRecurrenceEndOption: function(event) {
            this.$('#' + this.$(event.target).closest('label').attr('for')).attr('checked', 'checked');
        },
        _handleRecurrenceTypeChange: function() {
            this.$('.recurrence-field').hide();
            var value = this.$('#recurrence-type').val();
            var periodTypeName = Recurrence.periodNames[Recurrence.periods[value]];

            switch (value) {
                case 'DAYS_OF_WEEK':
                    this.$('#recurrence-days-of-week-group').show();
                case 'WEEKDAYS':
                case 'MON_WED_FRI':
                case 'TUE_THU':
                case 'DAILY':
                case 'MONTHLY':
                case 'YEARLY': {
                    this.$('#recurrence-period-group').show();
                    this.$('#recurrence-end-group').show();
                    if (periodTypeName) {
                        this.$("#recurrence-period-name").text(periodTypeName);
                    }
                    break;
                }
                case 'CRON': {
                    this.$('#recurrence-cron-group').show();
                    this.$('#recurrence-end-group').show();
                    break;
                }
            }
        },
        _handleCalendarChange: function() {
            this.$('#custom-event-dialog-type').auiSelect2('data', null);
            this.$('#custom-event-dialog-reminder').text(Reminder.none);
        },
        _handleTypeChange: function() {
            var reminder = this.$('#custom-event-dialog-type').auiSelect2('data').reminder;
            this.$('#custom-event-dialog-reminder').text(reminder ? Reminder.names[reminder] : Reminder.none);
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

            if (serializedData.recurrenceEndDate === 'Invalid date') {
                this.$('#custom-event-dialog-recurrenceEndDate-error')
                    .removeClass('hidden')
                    .text(AJS.I18n.getText('ru.mail.jira.plugins.calendar.customEvents.dialog.error.invalidDate'));
                this.$('#recurrence-end-date-value').focus();
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
                this.options.successHandler(model.toJSON());
            }
        },
        _ajaxErrorHandler: function(model, response) {
            var $recurrenceError = this.$("#custom-event-dialog-recurrence-error");
            var $error = this.$('#custom-event-dialog-error-panel');

            this.$el.find('.error').addClass('hidden');
            $error.addClass('hidden');
            $recurrenceError.addClass('hidden');

            var field = response.getResponseHeader('X-Atlassian-Rest-Exception-Field');
            if (field) {
                if (field.startsWith("recurrence")) { //todo: show field errors
                    $recurrenceError.removeClass('hidden');
                    $recurrenceError.text(response.responseText);
                } else {
                    this.$('#custom-event-dialog-' + field + '-error').removeClass('hidden').text(response.responseText);
                    var $field = this.$('#custom-event-dialog-' + field);
                    if ($field.hasClass('select') || $field.hasClass('multi-select'))
                        this.$('#custom-event-dialog-' + field).auiSelect2('focus');
                    else
                        this.$('#custom-event-dialog-' + field).focus();
                }
            } else {
                $error.removeClass('hidden').text(response.responseText);
            }
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
            var title = this.$('#custom-event-dialog-title').val();
            var startDate = this.$('#custom-event-dialog-startDate').val();
            var startTime = this.$('#custom-event-dialog-startTime').val();
            var endDate = this.$('#custom-event-dialog-endDate').val();
            var endTime = this.$('#custom-event-dialog-endTime').val();
            var calendarId = this.$('#custom-event-dialog-calendar').val();
            var eventTypeId = this.$('#custom-event-dialog-type').val();
            var participants = this.$('#custom-event-dialog-participantNames').val();
            var allDay = this.$('#custom-event-dialog-allDay').prop('checked');
            var editMode = this.$('input[name=editMode]:checked').val();

            var id = this.jsonModel.id;
            var parentId = null;
            var recurrenceType = this.$('#recurrence-type').val();
            var recurrenceNumber = null;
            var recurrencePeriod = null;
            var recurrenceExpression = null;
            var recurrenceEndDate = null;
            var recurrenceCount = null;

            if (editMode === 'SINGLE_EVENT' && !this.jsonModel.parentId) {
                recurrenceType = null;
                recurrenceNumber = this.jsonModel.recurrenceNumber;
                id = null;
                parentId = this.jsonModel.id;
            }

            if (recurrenceType) {
                if (recurrenceType === 'DAYS_OF_WEEK') {
                    recurrenceExpression = this.$('.dayOfWeek-checkbox > input:checked')
                        .map(function () {
                            return $(this).attr('value');
                        })
                        .get()
                        .join(",");
                }
                if (recurrenceType === 'CRON') {
                    recurrenceExpression = this.$('#recurrence-cron').val();
                } else {
                    recurrencePeriod = this.$('#recurrence-period').val();
                }

                var recurrenceEndType = this.$('#recurrence-end-group').find('input[name=recurrenceEnd]:checked').val();
                if (recurrenceEndType === 'count') {
                    recurrenceCount = this.$('#recurrence-end-count-value').val();
                }
                if (recurrenceEndType === 'date') {
                    var date = this.$('#recurrence-end-date-value').val();
                    var time = this.$('#recurrence-end-time-value').val();

                    if (allDay) {
                        recurrenceEndDate = moment.utc(date).format('x');
                    } else {
                        var parsedTime = moment(time, this.timeFormat);
                        recurrenceEndDate = moment(date).hours(parsedTime.hours()).minutes(parsedTime.minutes()).format('x');
                    }
                }
            }

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
                id: id,
                parentId: parentId,
                title: title,
                calendarId: calendarId,
                startDate: start,
                endDate: end,
                eventTypeId: eventTypeId,
                participantNames: participants ? participants.join(', ') : null,
                allDay: allDay,
                recurrenceType: recurrenceType,
                recurrencePeriod: recurrencePeriod,
                recurrenceExpression: recurrenceExpression,
                recurrenceEndDate: recurrenceEndDate,
                recurrenceCount: recurrenceCount,
                recurrenceNumber: recurrenceNumber,
                editMode: editMode
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
