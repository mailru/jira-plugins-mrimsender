define('calendar/calendar-dialog', ['jquery', 'underscore', 'backbone', 'jira/util/forms', 'jira/jql/jql-parser', 'jira/autocomplete/jql-autocomplete'], function($, _, Backbone, Forms, JQLParser, JQLAutoComplete) {
    var jqlAutoComplete;
    AJS.toInit(function() {
        $.ajax({
            type: 'GET',
            url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/config/displayedFields',
            success: function(result) {
                $('#calendar-dialog-displayed-fields').auiSelect2({
                    allowClear: true,
                    multiple: true,
                    data: Object.keys(result).map(function(key) {
                        return {
                            id: key,
                            text: result[key]
                        }
                    })
                });
            },
            error: function(xhr) {
                var msg = "Error while trying to load displayed fields.";
                if (xhr.responseText)
                    msg += xhr.responseText;
                alert(msg);
            }
        });
        $.ajax({
            type: 'GET',
            url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/config/dateFields',
            success: function(result) {
                $('#calendar-dialog-event-start').auiSelect2({data: result});
                $('#calendar-dialog-event-end').auiSelect2({
                    allowClear: true,
                    data: [{id: '', text: ' '}].concat(result)
                });
            },
            error: function(xhr) {
                var msg = "Error while trying to load date fields.";
                if (xhr.responseText)
                    msg += xhr.responseText;
                alert(msg);
            }
        });
        $.ajax({
            type: 'GET',
            url: AJS.contextPath() + '/rest/api/2/jql/autocompletedata',
            success: $.proxy(function(autoCompleteData) {
                jqlAutoComplete = JQLAutoComplete({
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
                jqlField = $('#advanced-search');
                jqlField.unbind("keypress", Forms.submitOnEnter).keypress(function(e) {
                    if (jqlAutoComplete.dropdownController === null || !jqlAutoComplete.dropdownController.displayed || jqlAutoComplete.selectedIndex < 0)
                        return true;
                });

                jqlAutoComplete.buildResponseContainer();
                jqlAutoComplete.parse(jqlField.val());

                $('.atlassian-autocomplete .suggestions').css('top', '68px');

                jqlField.click(function() {
                    jqlAutoComplete.dropdownController.hideDropdown();
                });
            }, this)
        });
    });

    return Backbone.View.extend({
        el: '#calendar-dialog',
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
            'change #calendar-dialog-permission-table-subject': '_onChangeSubjectSelect',
            'select2-open #calendar-dialog-permission-table-subject': '_onOpenSubjectSelect',
            'change input[type=radio][name=calendar-dialog-source]': '_onSourceTypeChange'
        },
        initialize: function(options) {
            this.dialog = AJS.dialog2('#calendar-dialog');
            this.$okButton = this.$('#calendar-dialog-ok');
            this.$cancelButton = this.$('#calendar-dialog-cancel');
            this.permissionIds = {};
            this.userData = options.userData;

            this.$('#calendar-dialog-source').addClass('hidden');
            this.$('.search-field-container').addClass('hidden');

            var canAdmin = !this.model.id || this.model.get('canAdmin');
            this.$('#calendar-dialog-name').attr('disabled', !canAdmin);
            this.$('#calendar-dialog-color').attr('disabled', !canAdmin);
            this.$('#calendar-dialog-color').auiSelect2('enable', canAdmin);
            this.$('#calendar-dialog-event-start').attr('disabled', !canAdmin);
            this.$('#calendar-dialog-event-start').auiSelect2('enable', canAdmin);
            this.$('#calendar-dialog-event-end').attr('disabled', !canAdmin);
            this.$('#calendar-dialog-event-end').auiSelect2('enable', canAdmin);
            this.$('.radio').attr('disabled', !canAdmin);
            this.$('.calendar-dialog-source-field').attr('disabled', !canAdmin);
            this.$('.calendar-dialog-source-field').auiSelect2('enable', canAdmin);
            this.$('#advanced-search').attr('disabled', !canAdmin);
            this.$('#calendar-dialog-displayed-fields').attr('disabled', !canAdmin);
            this.$('#calendar-dialog-displayed-fields').auiSelect2('enable', canAdmin);
            this.$('#calendar-dialog-permission-table-subject').attr('disabled', !canAdmin);
            this.$('#calendar-dialog-permission-table-subject').auiSelect2('enable', canAdmin);
            this.$('#calendar-dialog-permission-table-add').attr('disabled', !canAdmin);
            this.$okButton.attr('disabled', !canAdmin);
            this.$('.aui-dialog2-footer-hint').text(canAdmin ? '' : AJS.I18n.getText('ru.mail.jira.plugins.calendar.dialog.no.edit.permission'));

            this._initColorField();
            this._initSourceFields();
            this._initPermissionSubjectField();

            this._fillForm();

            AJS.tabs.setup();
            this.dialog.on('hide', $.proxy(this.destroy, this));
            this.$('form').submit($.proxy(this._onFormSubmit, this));
            this._keypressHandler = $.proxy(this._keypressHandler, this);
            $(document.body).on('keyup', this._keypressHandler);
        },
        destroy: function() {
            this.stopListening();
            this.undelegateEvents();
            this.$('form').off();
            this.dialog.off();
            $(document.body).off('keyup', this._keypressHandler);

            this.$okButton.removeAttr('disabled');
            this.$cancelButton.removeAttr('disabled');

            this.$('.calendar-dialog-select').auiSelect2('val', '');
            this.$('.calendar-dialog-source-dropdown').removeClass(
                'calendar-dialog-source-show-unavailable-project ' +
                'calendar-dialog-source-show-unavailable-filter ' +
                'calendar-dialog-source-show-deleted-project ' +
                'calendar-dialog-source-show-deleted-filter');

            this.$('.calendar-dialog-source-field').each(function() {
                var self = $(this);
                self.auiSelect2('val', self.find('option:eq(2)').val()); // todo:xxx bad index
            });
            this.$('#advanced-search').val('');
            this.$('.search-field-container .description').text('').hide();

            this.$('div.error').text('').addClass('hidden');
            this.$('input:not([type=radio])').val('');
            this.$('#calendar-dialog-error-panel').text('').addClass('hidden');
            this.$('#calendar-dialog-permission-table tbody tr').remove();
        },
        /* Public methods */
        show: function() {
            this.$('.calendar-dialog-permission-table-error').addClass('hidden');
            this.dialog.show();
            this._selectCommonTab();
        },
        hide: function() {
            this.dialog.hide();
        },
        /* Private methods */
        _keypressHandler: function(e) {
            switch (e.which) {
                // esc
                case 27 :
                    if (confirm(AJS.I18n.getText('ru.mail.jira.plugins.calendar.dialog.hideConfirm')))
                        this.hide();
                    break;
            }
        },
        _onJqlChange: function() {
            var currentJql = this.$('#advanced-search').val().trim();
            if (this.oldJql === currentJql)
                return;
            jqlAutoComplete.parse(currentJql);
            $.ajax({
                type: 'GET',
                url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/config/jql/count',
                data: {
                    jql: currentJql
                },
                success: $.proxy(function(result) {
                    if (_.has(result, 'issueCount'))
                        this.$('.search-field-container .description').html(AJS.I18n.getText('ru.mail.jira.plugins.calendar.dialog.source.jql.description', '<a href="' + AJS.format('{0}/issues/?jql={1}', AJS.contextPath(), currentJql.replace("\"", "'")) + '" target="_blank">', result.issueCount, '</a>')).fadeIn(250);
                    else
                        this.$('.search-field-container .description').fadeOut(250);
                    this.oldJql = currentJql;
                }, this),
                error: function() {
                    $('.search-field-container .description').fadeOut(250);
                }
            });
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
                                text: AJS.I18n.getText('ru.mail.jira.plugins.calendar.dialog.itemOfAllMatching', AJS.I18n.getText('admin.common.words.users'), data.users.length, data.usersCount),
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
                    var replacement = '<b>$1</b>';
                    var highlight = {};
                    if (item.type == 'PROJECT_ROLE') {
                        highlight.projectHighlight = regexp ? item.project.replace(regexp, replacement) : item.project;
                        highlight.projectRoleHighlight = regexp ? item.projectRole.replace(regexp, replacement) : item.projectRole;
                    } else if (item.type == 'USER') {
                        highlight.userDisplayNameHighlight = regexp ? item.userDisplayName.replace(regexp, replacement) : item.userDisplayName;
                        highlight.userEmailHighlight = regexp ? item.userEmail.replace(regexp, replacement) : item.userEmail;
                        highlight.userNameHighlight = regexp ? item.userName.replace(regexp, replacement) : item.userName;
                    } else if (item.type == 'GROUP')
                        highlight.textHighlight = regexp ? item.text.replace(regexp, replacement) : item.text;
                    return JIRA.Templates.Plugins.MailRuCalendar.permissionField(_.defaults(highlight, item));
                },
                formatSelection: JIRA.Templates.Plugins.MailRuCalendar.permissionFieldSelection
            });
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
                formatResult: function format(item) {
                    return JIRA.Templates.Plugins.MailRuCalendar.sourceField($.extend({
                        projectId: item.id,
                        sourceType: 'project'
                    }, item));
                },
                formatSelection: function format(item) {
                    return JIRA.Templates.Plugins.MailRuCalendar.sourceField($.extend({
                        projectId: item.id,
                        sourceType: 'project'
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
                formatResult: function format(item) {
                    return JIRA.Templates.Plugins.MailRuCalendar.sourceField(item);
                },
                formatSelection: function format(item) {
                    return JIRA.Templates.Plugins.MailRuCalendar.sourceField(item);
                },
                initSelection: function(element, callback) {
                }
            });
        },
        _showSourceField: function(sourceType) {
            this.sourceType = sourceType;
            this.$('#calendar-dialog-source-' + sourceType).attr('checked', true);
            if (sourceType == 'project' || sourceType == 'filter') {
                this.$('.search-field-container').addClass('hidden');
                this.$('.calendar-dialog-source-field').addClass('hidden');
                this.$('#calendar-dialog-source-' + this.sourceType + '-field').removeClass('hidden');
            } else if (sourceType == 'jql') {
                this.$('.calendar-dialog-source-field').addClass('hidden');
                this.$('.search-field-container').removeClass('hidden');
                this._onJqlChange();
            }
            this.$('.calendar-dialog-source-label').addClass('hidden');
            this.$('.calendar-dialog-source-' + sourceType + '-label').removeClass('hidden');
        },
        _initColorField: function() {
            this.$('#calendar-dialog-color').auiSelect2({
                minimumResultsForSearch: Infinity,
                formatResult: format,
                formatSelection: format
            });

            function format(data) {
                return JIRA.Templates.Plugins.MailRuCalendar.colorField(data);
            }
        },
        _fillForm: function() {
            if (this.model.id) {
                this.$('.aui-dialog2-header-main').text(AJS.I18n.getText('ru.mail.jira.plugins.calendar.editCalendar'));
                this.$okButton.text(AJS.I18n.getText('common.forms.update'));

                this.$('#calendar-dialog-name').val(this.model.get('selectedName'));
                this.$('#calendar-dialog-color').auiSelect2('val', this.model.get('selectedColor'));
                this.$('#calendar-dialog-event-start').auiSelect2('val', this.model.get('selectedEventStartId'));
                this.$('#calendar-dialog-event-end').auiSelect2('val', this.model.get('selectedEventEndId'));

                this._showSourceField(this.model.get('selectedSourceType'));
                if (this.sourceType == 'project' || this.sourceType == 'filter')
                    this.$('#calendar-dialog-source-' + this.sourceType + '-field').auiSelect2('data', {
                        id: this.model.get('selectedSourceValue'),
                        text: this.model.get('selectedSourceName'),
                        avatarId: this.model.get('selectedSourceAvatarId'),
                        unavailable: this.model.get('selectedSourceIsUnavailable')
                    });
                else if (this.sourceType == 'jql') {
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
                this.$('.aui-dialog2-header-main').text(AJS.I18n.getText('ru.mail.jira.plugins.calendar.createCalendar'));
                this.$okButton.text(AJS.I18n.getText('common.words.create'));

                this._showSourceField('project');
                this.$('#calendar-dialog-displayed-fields').auiSelect2('val', ['common.words.status', 'issue.field.assignee', 'issue.field.created', 'issue.field.duedate']);
                this._addPermissionRow({
                    id: this.userData.id,
                    type: 'USER',
                    userName: this.userData.get('name'),
                    userDisplayName: this.userData.get('displayName'),
                    accessType: 'ADMIN',
                    avatarUrl: this.userData.get('avatarUrl')
                });
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

            return {
                selectedName: name,
                selectedColor: color,
                selectedSourceType: this.sourceType,
                selectedSourceValue: source,
                selectedEventStartId: eventStart,
                selectedEventEndId: eventEnd,
                selectedDisplayedFields: displayedFields ? displayedFields.split(',') : [],
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
        _onSourceTypeChange: function() {
            var sourceType = this.$('input[type=radio][name=calendar-dialog-source]:checked').val();
            this._showSourceField(sourceType);
            this.$('#calendar-dialog-source-error').addClass('hidden');
        },
        _addPermissionRow: function(subjectData) {
            this.$('#calendar-dialog-permission-table tbody').prepend(JIRA.Templates.Plugins.MailRuCalendar.permissionTableRow(subjectData));
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
            $('.calendar-dialog-permission-table-subject-dropdown input.select2-input').addClass('ajs-dirty-warning-exempt');
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
                this.$('.calendar-dialog-permission-table-error').removeClass('hidden').find('th').html(AJS.format(AJS.I18n.getText('ru.mail.jira.plugins.calendar.dialog.alreadyHasAccess'), text));
            } else {
                this._addPermissionRow($.extend({accessType: 'USE'}, subjectData));
                $subjectSelect.auiSelect2('val', '');
            }
            $subjectSelect.auiSelect2('focus');
        },
        _removePermission: function(e) {
            var $row = $(e.target).closest('tr');
            var subjectId = $row.data('id');
            this.permissionIds[subjectId] = undefined;
            $row.remove();
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
        }
    });
});
