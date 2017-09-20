define('calendar/calendar-dialog', [
    'jquery',
    'underscore',
    'backbone',
    'jira/util/forms',
    'jira/jql/jql-parser',
    'jira/autocomplete/jql-autocomplete',
    'calendar/confirm-dialog',
    'calendar/reminder'
], function($, _, Backbone, Forms, JQLParser, JQLAutoComplete, ConfirmDialog, Reminder) {
    var AVATARS = ['event', 'travel', 'leave', 'birthday'];

    var displayedFieldsData, dateFieldsData, autoCompleteData;
    AJS.toInit(function() {
        $.ajax({
            type: 'GET',
            url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/config/displayedFields',
            success: function(result) {
                displayedFieldsData = Object.keys(result).map(function(key) {
                    return {
                        id: key,
                        text: result[key]
                    }
                });
            },
            error: function(xhr) {
                var msg = 'Error while trying to load displayed fields.';
                if (xhr.responseText)
                    msg += xhr.responseText;
                alert(msg);
            }
        });
        $.ajax({
            type: 'GET',
            url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/config/dateFields',
            success: function(result) {
                dateFieldsData = result;
            },
            error: function(xhr) {
                var msg = 'Error while trying to load date fields.';
                if (xhr.responseText)
                    msg += xhr.responseText;
                alert(msg);
            }
        });
        $.ajax({
            type: 'GET',
            url: AJS.contextPath() + '/rest/api/2/jql/autocompletedata',
            success: function(data) {
                autoCompleteData = data;
            }
        });
    });

    return Backbone.View.extend({
        events: {
            'click #calendar-dialog-ok:not([disabled])': '_submit',
            'click #calendar-dialog-cancel': 'hide',
            'change #advanced-search': '_onJqlChange',
            'keyup #advanced-search': '_onJqlChange',
            'paste #advanced-search': '_onJqlChange',
            'click #calendar-dialog-permission-table-add:not([disabled])': '_addPermission',
            'click .calendar-dialog-permission-table-action .aui-icon:not([disabled])': '_removePermission',
            'click .calendar-dialog-permission-table-access-type .aui-lozenge:not([disabled])': '_toggleAccessType',
            'click a[href=#calendar-dialog-common-tab]': '_selectCommonTab',
            'click a[href=#calendar-dialog-source-tab]': '_selectSourceTab',
            'click a[href=#calendar-dialog-permissions-tab]': '_selectPermissionTab',
            'click a[href=#calendar-dialog-eventTypes-tab]': '_selectEventTypesTab',
            'change #calendar-dialog-permission-table-subject': '_onChangeSubjectSelect',
            'focus .calendar-dialog-permission-table-action a': '_onPermissionTableActionFocus',
            'blur .calendar-dialog-permission-table-action a': '_onPermissionTableActionBlur',
            'select2-open #calendar-dialog-permission-table-subject': '_onOpenSubjectSelect',
            'change input[type=radio][name=calendar-dialog-source]': '_onSourceTypeChange',
            'click #calendar-dialog-eventTypes-content .edit-button': '_editEventTypeRow',
            'click #calendar-dialog-eventTypes-content .delete-button': '_deleteEventType',
            'click #calendar-dialog-eventTypes-create-button': '_addNewEventTypeRow',
            'click #calendar-dialog-eventTypes-content .cancel-button': '_resetEventTypeEditing'
        },
        render: function() {
            this.$el.html(JIRA.Templates.Plugins.MailRuCalendar.CalendarDialog.dialog({
                model: this.model.toJSON(),
                canAdmin: !this.model.id || this.model.get('canAdmin')
            }));
            $(document.body).append(this.$el);
            this.setElement($('#calendar-dialog').unwrap());
            AJS.tabs.setup();
            return this;
        },
        initialize: function(options) {
            this.render();
            this.$okButton = this.$('#calendar-dialog-ok');
            this.$cancelButton = this.$('#calendar-dialog-cancel');
            this.dialog = AJS.dialog2('#calendar-dialog');

            this.permissionIds = {};
            this.userData = options.userData;

            this._initColorField();
            this._initSourceFields();
            this._initJqlField();
            this._initDateFields();
            this._initDisplayedFieldsField();
            this._initPermissionSubjectField();
            this._fillForm();

            this.$('form').submit($.proxy(this._onFormSubmit, this));
            this._keypressHandler = $.proxy(this._keypressHandler, this);
            $(document.body).on('keyup', this._keypressHandler);
            this.dialog.on('hide', $.proxy(this._destroy, this));
        },
        _initColorField: function() {
            this.$('#calendar-dialog-color').auiSelect2({
                minimumResultsForSearch: Infinity,
                formatResult: format,
                formatSelection: format
            });

            function format(data) {
                return JIRA.Templates.Plugins.MailRuCalendar.CalendarDialog.colorField(data);
            }
        },
        _initSourceFields: function() {
            this.$('#calendar-dialog-source-project-field').auiSelect2({
                minimumInputLength: 0,
                ajax: {
                    url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/config/eventSources/project',
                    dataType: 'json',
                    quietMillis: 100,
                    data: function(term) {
                        return {filter: term};
                    },
                    results: function(data) {
                        var results = [];
                        if (data.projects && data.projects.length > 0)
                            results.push({
                                text: AJS.I18n.getText('ru.mail.jira.plugins.calendar.dialog.itemOfAllMatchingShort', data.projects.length, data.totalProjectsCount),
                                children: data.projects
                            });
                        return {results: results};
                    },
                    cache: true
                },
                dropdownCssClass: 'calendar-dialog-source-dropdown',
                formatResult: function format(item, label, query) {
                    var text = item.text;
                    if (query.term)
                        text = text.replace(new RegExp('(' + query.term + ')', 'gi'), '{b}$1{/b}');
                    text = AJS.escapeHTML(text).replace(new RegExp('{b}', 'gi'), '<b>').replace(new RegExp('{/b}', 'gi'), '</b>');
                    return JIRA.Templates.Plugins.MailRuCalendar.CalendarDialog.sourceField(_.defaults({
                        projectId: item.id,
                        sourceType: 'project',
                        text: text
                    }, item));
                },
                formatSelection: function format(item) {
                    return JIRA.Templates.Plugins.MailRuCalendar.CalendarDialog.sourceField(_.defaults({
                        projectId: item.id,
                        sourceType: 'project',
                        text: item.text ? AJS.escapeHTML(item.text) : ''
                    }, item));
                },
                initSelection: function(element, callback) {
                }
            });

            var filterSourceTerm;
            this.$('#calendar-dialog-source-filter-field').auiSelect2({
                minimumInputLength: 0,
                ajax: {
                    url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/config/eventSources/filter',
                    dataType: 'json',
                    quietMillis: 100,
                    data: function(term) {
                        filterSourceTerm = term;
                        return {filter: term};
                    },
                    results: function(data) {
                        var results = [];
                        if (data.filters && data.filters.length > 0)
                            results.push({
                                text: AJS.I18n.getText('ru.mail.jira.plugins.calendar.dialog.source.foundFilters', data.filters.length),
                                children: data.filters
                            });
                        if (data.myFilters && data.myFilters.length > 0)
                            results.push({
                                text: AJS.I18n.getText('ru.mail.jira.plugins.calendar.dialog.source.myFilters'),
                                children: data.myFilters
                            });
                        if (data.favouriteFilters && data.favouriteFilters.length > 0)
                            results.push({
                                text: AJS.I18n.getText('ru.mail.jira.plugins.calendar.dialog.source.favouriteFilters'),
                                children: data.favouriteFilters
                            });
                        return {results: results};
                    },
                    cache: true
                },
                dropdownCssClass: 'calendar-dialog-source-dropdown',
                formatResult: function format(item, label, query) {
                    var text = item.text;
                    if (query.term)
                        text = text.replace(new RegExp('(' + query.term + ')', 'gi'), '{b}$1{/b}');
                    text = AJS.escapeHTML(text).replace(new RegExp('{b}', 'gi'), '<b>').replace(new RegExp('{/b}', 'gi'), '</b>');
                    return JIRA.Templates.Plugins.MailRuCalendar.CalendarDialog.sourceField(_.defaults({text: text}, item));
                },
                formatSelection: function format(item) {
                    var text = item.text ? AJS.escapeHTML(item.text) : '';
                    return JIRA.Templates.Plugins.MailRuCalendar.CalendarDialog.sourceField(_.defaults({text: text}, item));
                },
                initSelection: function(element, callback) {
                }
            });
        },
        _initDateFields: function() {
            this.$('#calendar-dialog-event-start').auiSelect2({data: dateFieldsData});
            this.$('#calendar-dialog-event-end').auiSelect2({
                allowClear: true,
                data: [{id: '', text: ' '}].concat(dateFieldsData)
            });
        },
        _initDisplayedFieldsField: function() {
            this.$('#calendar-dialog-displayed-fields').auiSelect2({
                allowClear: true,
                multiple: true,
                data: displayedFieldsData
            });
        },
        _initJqlField: function() {
            var jqlAutoComplete = JQLAutoComplete({
                fieldID: 'advanced-search',
                errorID: 'jqlerrormsg',
                parser: JQLParser(autoCompleteData.jqlReservedWords || []),
                queryDelay: 0.65,
                jqlFieldNames: autoCompleteData.visibleFieldNames || [],
                jqlFunctionNames: autoCompleteData.visibleFunctionNames || [],
                minQueryLength: 0,
                //allowArrowCarousel: true,
                autoSelectFirst: false,
                maxHeight: '195'
            });
            var jqlField = this.$('#advanced-search');
            jqlField.unbind('keypress', Forms.submitOnEnter).keypress(function(e) {
                if (jqlAutoComplete.dropdownController === null || !jqlAutoComplete.dropdownController.displayed || jqlAutoComplete.selectedIndex < 0)
                    return true;
            });

            jqlAutoComplete.buildResponseContainer();
            jqlAutoComplete.parse(jqlField.val());

            this.$('.atlassian-autocomplete .suggestions').css('top', '68px');

            jqlField.click(function() {
                jqlAutoComplete.dropdownController.hideDropdown();
            });
            this.jqlAutoComplete = jqlAutoComplete;
        },
        _initPermissionSubjectField: function() {
            this.$('#calendar-dialog-permission-table-subject').auiSelect2({
                placeholder: AJS.I18n.getText('ru.mail.jira.plugins.calendar.dialog.users.groups.roles'),
                allowClear: true,
                ajax: {
                    url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/config/permission/subjects',
                    dataType: 'json',
                    delay: 250,
                    quietMillis: 100,
                    data: function(term) {
                        return {filter: term};
                    },
                    results: function(data, params) {
                        var results = [];
                        if (data.users && data.users.length)
                            results.push({
                                text: AJS.I18n.getText('admin.common.words.users'),
                                children: data.users
                            });
                        if (data.groups && data.groups.length)
                            results.push({
                                text: AJS.I18n.getText('ru.mail.jira.plugins.calendar.dialog.itemOfAllMatching', AJS.I18n.getText('ru.mail.jira.plugins.calendar.common.groups'), data.groups.length, data.groupsCount),
                                children: data.groups
                            });
                        if (data.projectRoles && data.projectRoles.length)
                            results.push({
                                text: AJS.I18n.getText('ru.mail.jira.plugins.calendar.dialog.itemOfAllMatching', AJS.I18n.getText('common.words.project.roles'), data.projectRoles.length, data.projectRolesCount),
                                children: data.projectRoles
                            });
                        return {
                            results: results
                        };
                    },
                    cache: true
                },
                dropdownCssClass: 'calendar-dialog-permission-table-subject-dropdown',
                formatResult: function(item, label, query) {
                    var regexp = query.term ? new RegExp('(' + query.term + ')', 'gi') : undefined;
                    var replacement = '{b}$1{/b}';
                    var highlight = {};
                    if (item.type == 'PROJECT_ROLE') {
                        highlight.projectHighlight = regexp ? item.project.replace(regexp, replacement) : item.project;
                        highlight.projectHighlight = AJS.escapeHTML(highlight.projectHighlight).replace(new RegExp('{b}', 'gi'), '<b>').replace(new RegExp('{/b}', 'gi'), '</b>');
                        highlight.projectRoleHighlight = regexp ? item.projectRole.replace(regexp, replacement) : item.projectRole;
                        highlight.projectRoleHighlight = AJS.escapeHTML(highlight.projectRoleHighlight).replace(new RegExp('{b}', 'gi'), '<b>').replace(new RegExp('{/b}', 'gi'), '</b>');
                    } else if (item.type == 'USER') {
                        highlight.userDisplayNameHighlight = regexp ? item.userDisplayName.replace(regexp, replacement) : item.userDisplayName;
                        highlight.userDisplayNameHighlight = AJS.escapeHTML(highlight.userDisplayNameHighlight).replace(new RegExp('{b}', 'gi'), '<b>').replace(new RegExp('{/b}', 'gi'), '</b>');
                        highlight.userEmailHighlight = regexp ? item.userEmail.replace(regexp, replacement) : item.userEmail;
                        highlight.userEmailHighlight = AJS.escapeHTML(highlight.userEmailHighlight).replace(new RegExp('{b}', 'gi'), '<b>').replace(new RegExp('{/b}', 'gi'), '</b>');
                        highlight.userNameHighlight = regexp ? item.userName.replace(regexp, replacement) : item.userName;
                        highlight.userNameHighlight = AJS.escapeHTML(highlight.userNameHighlight).replace(new RegExp('{b}', 'gi'), '<b>').replace(new RegExp('{/b}', 'gi'), '</b>');
                    } else if (item.type == 'GROUP') {
                        highlight.textHighlight = regexp ? item.text.replace(regexp, replacement) : item.text;
                        highlight.textHighlight = AJS.escapeHTML(highlight.textHighlight).replace(new RegExp('{b}', 'gi'), '<b>').replace(new RegExp('{/b}', 'gi'), '</b>');
                    }
                    return JIRA.Templates.Plugins.MailRuCalendar.CalendarDialog.permissionField(_.defaults(highlight, item));
                },
                formatSelection: JIRA.Templates.Plugins.MailRuCalendar.CalendarDialog.permissionFieldSelection
            });
        },
        _destroy: function() {
            this.remove();
            $(document.body).off('keyup', this._keypressHandler);
        },
        /* Public methods */
        show: function() {
            this.dialog.show();
            this._selectCommonTab();
        },
        hide: function() {
            this.dialog.hide();
        },
        isDirty: function() {
            var orig = this.model.toJSON();
            var changed = this._serialize();
            orig.selectedEventEndId = orig.selectedEventEndId || undefined;
            changed.selectedEventEndId = changed.selectedEventEndId || undefined;

            return orig.selectedName != changed.selectedName || orig.selectedColor != changed.selectedColor
                || orig.selectedSourceType != changed.selectedSourceType || orig.selectedSourceValue != changed.selectedSourceValue
                || orig.selectedEventStartId != changed.selectedEventStartId || orig.selectedEventEndId != changed.selectedEventEndId
                || !_.isEqual(orig.selectedDisplayedFields, changed.selectedDisplayedFields)
                || this.isPermissionTableChanged;
        },
        /* Private methods */
        _keypressHandler: function(e) {
            switch (e.which) {
                // esc
                case 27 :
                    if (!this.isDirty() || confirm(AJS.I18n.getText('ru.mail.jira.plugins.calendar.dialog.hideConfirm')))
                        this.hide();
                    break;
            }
        },
        _onJqlChange: function() {
            var currentJql = this.$('#advanced-search').val().trim();
            if (this.oldJql === currentJql)
                return;
            this.jqlAutoComplete.parse(currentJql);
            $.ajax({
                type: 'GET',
                url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/config/jql/count',
                data: {
                    jql: currentJql
                },
                success: $.proxy(function(result) {
                    if (_.has(result, 'issueCount'))
                        this.$('.search-field-container .description').html(AJS.I18n.getText('ru.mail.jira.plugins.calendar.dialog.source.jql.description', '<a href="' + AJS.format('{0}/issues/?jql={1}', AJS.contextPath(), currentJql.replace(/"/g, '\'')) + '" target="_blank">', result.issueCount, '</a>')).fadeIn(250);
                    else
                        this.$('.search-field-container .description').fadeOut(250);
                    this.oldJql = currentJql;
                }, this),
                error: $.proxy(function() {
                    this.$('.search-field-container .description').fadeOut(250);
                }, this)
            });
        },
        _showSourceField: function(sourceType) {
            this.sourceType = sourceType;
            this.$('#calendar-dialog-source-' + sourceType).attr('checked', true);
            if (sourceType == 'project' || sourceType == 'filter') {
                this.$('.common-field').removeClass('hidden');
                this.$('.search-field-container').addClass('hidden');
                this.$('.calendar-dialog-source-field').addClass('hidden');
                this.$('#calendar-dialog-source-' + this.sourceType + '-field').removeClass('hidden');
            } else if (sourceType == 'jql') {
                this.$('.common-field').removeClass('hidden');
                this.$('.calendar-dialog-source-field').addClass('hidden');
                this.$('.search-field-container').removeClass('hidden');
                this._onJqlChange();
            } else if (sourceType == 'basic') {
                this.$('.search-field-container').addClass('hidden');
                this.$('.calendar-dialog-source-field').addClass('hidden');
                this.$('.common-field').addClass('hidden');
            }
            this.$('.calendar-dialog-source-label').addClass('hidden');
            this.$('.calendar-dialog-source-' + sourceType + '-label').removeClass('hidden');
        },
        _fillForm: function() {
            if (this.model.id) {
                this.$('#calendar-dialog-name').val(this.model.get('selectedName'));
                this.$('#calendar-dialog-color').auiSelect2('val', this.model.get('selectedColor'));
                this.$('#calendar-dialog-event-start').auiSelect2('val', this.model.get('selectedEventStartId'));
                this.$('#calendar-dialog-event-end').auiSelect2('val', this.model.get('selectedEventEndId'));
                this.$('#calendar-dialog-timelineGroup').val(this.model.get('timelineGroup'));

                this._showSourceField(this.model.get('selectedSourceType'));
                if (this.sourceType == 'project' || this.sourceType == 'filter') {
                    this.$('#calendar-dialog-source-' + this.sourceType + '-field').auiSelect2('data', {
                        id: this.model.get('selectedSourceValue'),
                        text: this.model.get('selectedSourceName'),
                        avatarId: this.model.get('selectedSourceAvatarId'),
                        unavailable: this.model.get('selectedSourceIsUnavailable')
                    });
                } else if (this.sourceType == 'jql') {
                    this.$('#advanced-search').val(this.model.get('selectedSourceValue'));
                    this._onJqlChange();
                }

                if (this.model.has('selectedDisplayedFields'))
                    this.$('#calendar-dialog-displayed-fields').auiSelect2('val', this.model.get('selectedDisplayedFields'));

                if (this.model.has('permissions')) {
                    var sortOrder = {'USER': 1, 'GROUP': 2, 'PROJECT_ROLE': 3};
                    var permissions = _.sortBy(this.model.get('permissions'), function(a) {
                        if (a.type == 'USER')
                            return sortOrder[a.type] + a.userDisplayName;
                        else if (a.type == 'GROUP')
                            return sortOrder[a.type] + a.text;
                        else if (a.type == 'PROJECT_ROLE')
                            return sortOrder[a.type] + a.project + a.projectRole;
                    }).reverse();
                    _.each(permissions, $.proxy(function(permission) {
                        this._addPermissionRow(permission);
                    }, this));
                }
            } else {
                this._showSourceField('basic');
                this.$('#calendar-dialog-displayed-fields').auiSelect2('val', ['common.words.status', 'issue.field.assignee', 'issue.field.created', 'issue.field.duedate']);
                this._addPermissionRow({
                    id: this.userData.id,
                    type: 'USER',
                    userName: this.userData.get('name'),
                    userDisplayName: this.userData.get('displayName'),
                    accessType: 'ADMIN',
                    avatarUrl: this.userData.get('avatarUrl')
                });
                this.model.set(this._serialize());
            }

            var canAdmin = !this.model.id || this.model.get('canAdmin');
            this.$('tbody .calendar-dialog-permission-table-action .aui-icon').attr('disabled', !canAdmin);
            this.$('tbody .calendar-dialog-permission-table-access-type .aui-lozenge').attr('disabled', !canAdmin);
        },
        _onFormSubmit: function(e) {
            e.preventDefault();
            this.$okButton.click();
        },
        _submit: function(e) {
            e.preventDefault();

            this.$okButton.attr('disabled', 'disabled');
            this.$cancelButton.attr('disabled', 'disabled');
            this.$('#calendar-dialog-error-panel').addClass('hidden').text('');
            this.$('div.error').addClass('hidden').text('');
            this.$('.calendar-dialog-permission-table-error').addClass('hidden');

            this.model.save(this._serialize(), {
                success: $.proxy(this._ajaxSuccessHandler, this),
                error: $.proxy(this._ajaxErrorHandler, this)
            });
        },
        _serialize: function() {
            var name = this.$('#calendar-dialog-name').val();
            var color = this.$('#calendar-dialog-color').val();
            var source;
            if (this.sourceType == 'project' || this.sourceType == 'filter') {
                var sourceData = this.$('#calendar-dialog-source-' + this.sourceType + '-field').auiSelect2('data');
                if (sourceData)
                    source = sourceData.id;
            } else if (this.sourceType == 'jql') {
                source = this.$('#advanced-search').val();
            }
            var eventStart = this.$('#calendar-dialog-event-start').val();
            var eventEnd = this.$('#calendar-dialog-event-end').val();
            var displayedFields = this.$('#calendar-dialog-displayed-fields').val();
            var permissions = _.filter(_.values(this.permissionIds), function(obj) {
                return !!obj;
            });
            var timelineGroup = this.$("#calendar-dialog-timelineGroup").val();

            return {
                selectedName: name,
                selectedColor: color,
                selectedSourceType: this.sourceType,
                selectedSourceValue: source,
                selectedEventStartId: eventStart,
                selectedEventEndId: eventEnd,
                selectedDisplayedFields: displayedFields ? displayedFields.split(',') : [],
                timelineGroup: timelineGroup,
                permissions: permissions && permissions.length ? permissions : []
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
            this._selectCommonTab();
            var field = response.getResponseHeader('X-Atlassian-Rest-Exception-Field');
            if (field) {
                if (field == 'name' || field == 'color')
                    this._selectCommonTab();
                else
                    this._selectSourceTab();
                this.$('#calendar-dialog-' + field + '-error').removeClass('hidden').text(response.responseText);
                var $field = this.$('#calendar-dialog-' + field);
                if (field == 'source') {
                    if (this.sourceType == 'project')
                        this.$('#calendar-dialog-source-project-field').auiSelect2('focus');
                    else if (this.sourceType == 'filter')
                        this.$('#calendar-dialog-source-filter-field').auiSelect2('focus');
                    else if (this.sourceType == 'jql')
                        this.$('#advanced-search').focus();
                } else if ($field.hasClass('select') || $field.hasClass('multi-select'))
                    this.$('#calendar-dialog-' + field).auiSelect2('focus');
                else
                    this.$('#calendar-dialog-' + field).focus();
            } else
                this.$('#calendar-dialog-error-panel').removeClass('hidden').text(response.responseText);
            this.$okButton.removeAttr('disabled');
            this.$cancelButton.removeAttr('disabled');
        },
        _selectCommonTab: function(e) {
            e && e.preventDefault();

            this.$('.aui-navgroup li.aui-nav-selected').removeClass('aui-nav-selected');
            this.$('.calendar-dialog-tab').addClass('hidden');

            this.$('#calendar-dialog-common-tab').removeClass('hidden');
            this.$('a[href=#calendar-dialog-common-tab]').closest('li').addClass('aui-nav-selected');

            this.$('#calendar-dialog-name').focus();
        },
        _selectSourceTab: function(e) {
            e && e.preventDefault();
            this.$('.aui-navgroup li.aui-nav-selected').removeClass('aui-nav-selected');
            this.$('.calendar-dialog-tab').addClass('hidden');

            this.$('#calendar-dialog-source-tab').removeClass('hidden');
            this.$('a[href=#calendar-dialog-source-tab]').closest('li').addClass('aui-nav-selected');

            this.$('#calendar-dialog-source-' + this.sourceType).focus();
        },
        _selectPermissionTab: function(e) {
            e && e.preventDefault();
            this.$('.aui-navgroup li.aui-nav-selected').removeClass('aui-nav-selected');
            this.$('.calendar-dialog-tab').addClass('hidden');

            this.$('#calendar-dialog-permissions-tab').removeClass('hidden');
            this.$('a[href=#calendar-dialog-permissions-tab]').closest('li').addClass('aui-nav-selected');

            this.$('#calendar-dialog-permission-table-subject').auiSelect2('focus');
        },
        _selectEventTypesTab: function(e) {
            e && e.preventDefault();
            this.$('.aui-navgroup li.aui-nav-selected').removeClass('aui-nav-selected');
            this.$('.calendar-dialog-tab').addClass('hidden');

            this.$('#calendar-dialog-eventTypes-tab').removeClass('hidden');
            this.$('a[href=#calendar-dialog-eventTypes-tab]').closest('li').addClass('aui-nav-selected');

            $.ajax({
                type: 'GET',
                url: AJS.contextPath() + '/rest/mailrucalendar/1.0/customEvent/type/list',
                data: {
                    calendarId: this.model.id
                },
                success: $.proxy(function(result) {
                    this.eventTypes = result;
                    _.each(this.eventTypes, function(e) {
                        if (e.reminder) {
                            e.reminderDisplayName = Reminder.names[e.reminder];
                        }
                    });
                    this._renderEventTypeTable();
                }, this)
            });
        },
        _renderEventTypeTable: function() {
            this.$('#calendar-dialog-ok, #calendar-dialog-cancel, #calendar-dialog .aui-nav > li > a').attr('disabled', null);
            this.$('#calendar-dialog-eventTypes-content').html(JIRA.Templates.Plugins.MailRuCalendar.CalendarDialog.eventTypesTable({
                systemTypes: $.grep(this.eventTypes, function(e) {
                    return e.system;
                }),
                customTypes: $.grep(this.eventTypes, function(e) {
                    return !e.system;
                }),
                canAdmin: this.model.get('canAdmin')
            }));
        },
        _resetEventTypeEditing: function(e) {
            if (e) {
                e.preventDefault();
            }

            this.$('#calendar-dialog-ok, #calendar-dialog-cancel, #calendar-dialog .aui-nav > li > a').removeAttr('disabled');
            this.$('#calendar-dialog-eventTypes-content').find('tr.edit').each($.proxy(function(i, e) {
                var $e = $(e);
                var id = $e.data('id');
                if (id) {
                    $e.empty();
                    $e.append(JIRA.Templates.Plugins.MailRuCalendar.CalendarDialog.eventRow({
                        model: this._findEventTypeModel(id),
                        canAdmin: this.model.get('canAdmin')
                    }));
                    $e.removeClass('edit');
                } else {
                    $e.remove();
                }
            }, this));
        },
        _addNewEventTypeRow: function() {
            this._resetEventTypeEditing();

            var $row = $('<tr id="calendar-dialog-custom-type-row-new" class="edit"></tr>');
            $row.append(this._renderEventEdit({}));
            this.$('#calendar-dialog-custom-type-tbody').append($row);
            this._afterEditStarted($row, null);
        },
        _editEventTypeRow: function(event) {
            this._resetEventTypeEditing();

            var $row = $(event.currentTarget).closest('tr');
            var typeId = $row.data('id');

            $row.empty();
            $row.append(this._renderEventEdit(this._findEventTypeModel(typeId)));
            $row.addClass('edit');
            this._afterEditStarted($row, typeId);
        },
        _renderEventEdit: function(model) {
            return JIRA.Templates.Plugins.MailRuCalendar.CalendarDialog.eventEdit({
                model: model,
                availableAvatars: AVATARS,
                reminderOptions: Reminder.options
            });
        },
        _deleteEventType: function(event) {
            var $row = $(event.currentTarget).closest('tr');
            var typeId = $row.data('id');

            var confirmDialog = new ConfirmDialog({
                okText: AJS.I18n.getText('common.words.delete'),
                header: AJS.I18n.getText('ru.mail.jira.plugins.calendar.dialog.confirmDeleteTypeHeader'),
                text: AJS.I18n.getText('ru.mail.jira.plugins.calendar.dialog.confirmDeleteType', AJS.escapeHtml(this._findEventTypeModel(typeId).name)),
                okHandler: $.proxy(function() {
                    $.ajax({
                        type: 'DELETE',
                        url: AJS.contextPath() + '/rest/mailrucalendar/1.0/customEvent/type/' + typeId,
                        success: $.proxy(function() {
                            this.eventTypes = _.filter(this.eventTypes, function(e) {
                                return e.id != typeId;
                            });
                            this._renderEventTypeTable();
                        }, this),
                        error: function (xhr) {
                            alert(xhr.responseText);
                        }
                    });
                }, this)
            });

            confirmDialog.show();
        },
        _findEventTypeModel: function(typeId) {
            return _.find(this.eventTypes, function(e) {
                return e.id == typeId;
            });
        },
        _afterEditStarted: function($row, typeId) {
            this.$('#calendar-dialog-ok, #calendar-dialog-cancel, #calendar-dialog .aui-nav > li > a').attr('disabled', 'disabled');

            var saveCallback = $.proxy(function(e) {
                e.preventDefault();

                var name = this.$('#calendar-dialog-customEvent-name').val();
                var avatar = this.$('#calendar-dialog-customEvent-avatar').val();
                var reminder = this.$('#calendar-dialog-customEvent-reminder').val();

                $row.find('.save-button, .cancel-button').attr('disabled', 'disabled');

                if (typeId) {
                    $.ajax({
                        type: 'PUT',
                        url: AJS.contextPath() + '/rest/mailrucalendar/1.0/customEvent/type/' + typeId,
                        contentType: 'application/json; charset=utf-8',
                        data: JSON.stringify({
                            calendarId: this.model.get('id'),
                            name: name,
                            avatar: avatar,
                            reminder: reminder
                        }),
                        success: $.proxy(function(data) {
                            if (data.reminder) {
                                data.reminderDisplayName = Reminder.names[data.reminder];
                            }
                            this.eventTypes = _.map(this.eventTypes, function(e) {
                                if (e.id == data.id) {
                                    return data;
                                } else {
                                    return e;
                                }
                            });
                            this._renderEventTypeTable();
                        }, this),
                        error: $.proxy(function(xhr) {
                            this._handleEditError($row, xhr);
                        }, this),
                        complete: function() {
                            $row.find('.save-button, .cancel-button').removeAttr('disabled');
                        }
                    });
                } else {
                    $.ajax({
                        type: 'POST',
                        url: AJS.contextPath() + '/rest/mailrucalendar/1.0/customEvent/type',
                        contentType: 'application/json; charset=utf-8',
                        data: JSON.stringify({
                            calendarId: this.model.get('id'),
                            name: name,
                            avatar: avatar,
                            reminder: reminder
                        }),
                        success: $.proxy(function(data) {
                            if (data.reminder) {
                                data.reminderDisplayName = Reminder.names[data.reminder];
                            }
                            this.eventTypes.push(data);
                            this._renderEventTypeTable();
                        }, this),
                        error: $.proxy(function(xhr) {
                            this._handleEditError($row, xhr);
                        }, this),
                        complete: function() {
                            $row.find('.save-button, .cancel-button').removeAttr('disabled');
                        }
                    });
                }
            }, this);
            $row.find('save-button').click(saveCallback);
            $row.find('form').submit(saveCallback);

            this._initAvatarPicker($row);

            this.$('#calendar-dialog-customEvent-name').focus();
        },
        _handleEditError: function($row, xhr) {
            $row.find('.error').addClass('hidden');

            var field = xhr.getResponseHeader('X-Atlassian-Rest-Exception-Field');
            if (field) {
                this.$('#calendar-dialog-customEvent-' + field + '-error').removeClass('hidden').text(xhr.responseText);
            } else
                this.$('#calendar-dialog-customEvent-error-panel').removeClass('hidden').text(xhr.responseText);
        },
        _initAvatarPicker: function($container) {
            $container.find('.avatar-picker .avatar').each(function(i, e) {
                var $e = $(e);
                $e.click(function() {
                    $container.find('.avatar-picker .avatar.selected').removeClass('selected');
                    $e.addClass('selected');
                    this.$('#calendar-dialog-customEvent-avatar').val($e.data('id'));
                });
            });
        },
        _onSourceTypeChange: function() {
            var sourceType = this.$('input[type=radio][name=calendar-dialog-source]:checked').val();
            this._showSourceField(sourceType);
            this.$('#calendar-dialog-source-error').addClass('hidden');
        },
        _onPermissionTableActionFocus: function(e) {
            $(e.target).closest('tr').addClass('calendar-dialog-permission-table-row-hover');
        },
        _onPermissionTableActionBlur: function(e) {
            $(e.target).closest('tr').removeClass('calendar-dialog-permission-table-row-hover');
        },
        _addPermissionRow: function(subjectData) {
            this.$('#calendar-dialog-permission-table tbody').prepend(JIRA.Templates.Plugins.MailRuCalendar.CalendarDialog.permissionTableRow(subjectData));
            this.permissionIds[subjectData.id] = {
                id: subjectData.id,
                type: subjectData.type,
                accessType: subjectData.accessType
            };
        },
        _onChangeSubjectSelect: function() {
            this.$('.calendar-dialog-permission-table-error').addClass('hidden');
        },
        _onOpenSubjectSelect: function() {
            this.$('.calendar-dialog-permission-table-subject-dropdown input.select2-input').addClass('ajs-dirty-warning-exempt');
        },
        _addPermission: function(e) {
            e.preventDefault();
            var $subjectSelect = this.$('#calendar-dialog-permission-table-subject');
            var subjectData = $subjectSelect.auiSelect2('data');
            if (!subjectData)
                return;
            this.$('.calendar-dialog-permission-table-error').addClass('hidden');
            if (this.permissionIds[subjectData.id]) {
                var text = subjectData.text;
                if (subjectData.type == 'PROJECT_ROLE')
                    text = AJS.format('{0}<span class="calendar-dialog-permission-project-role-separator">/</span>{1}', subjectData.project, subjectData.projectRole);
                else if (subjectData.type == 'USER')
                    text = subjectData.userDisplayName;
                this.$('.calendar-dialog-permission-table-error').removeClass('hidden').html(AJS.format(AJS.I18n.getText('ru.mail.jira.plugins.calendar.dialog.alreadyHasAccess'), AJS.escapeHtml(text)));
            } else {
                this._addPermissionRow($.extend({accessType: 'USE'}, subjectData));
                $subjectSelect.auiSelect2('val', '');
                this.isPermissionTableChanged = true;
            }
            $subjectSelect.auiSelect2('focus');
        },
        _removePermission: function(e) {
            var $row = $(e.target).closest('tr');
            var subjectId = $row.data('id');
            this.permissionIds[subjectId] = undefined;
            $row.remove();
            this.isPermissionTableChanged = true;
        },
        _toggleAccessType: function(e) {
            e.preventDefault();
            var $accessType = $(e.target);
            var accessType = $accessType.data('access-type');
            var $row = $accessType.closest('tr');
            var permission = this.permissionIds[$row.data('id')];
            if (!permission || permission.accessType == accessType)
                return;
            var selected = !$accessType.hasClass('selected');
            $accessType.toggleClass('selected', selected);
            $accessType.toggleClass('aui-lozenge-complete', selected);
            $accessType.toggleClass('aui-lozenge-subtle', !selected);
            if (selected) {
                var $otherAccessType = $row.find('.aui-lozenge[data-access-type="' + (accessType == 'ADMIN' ? 'USE' : 'ADMIN') + '"]');
                $otherAccessType.toggleClass('selected', false);
                $otherAccessType.toggleClass('aui-lozenge-complete', false);
                $otherAccessType.toggleClass('aui-lozenge-subtle', true);
            }
            permission.accessType = selected ? accessType : undefined;
            this.isPermissionTableChanged = true;
        }
    });
});
