define('calendar/calendar-dialog', ['jquery', 'underscore', 'backbone'], function($, _, Backbone) {
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
    });

    return Backbone.View.extend({
        el: '#calendar-dialog',
        events: {
            'click #calendar-dialog-ok:not([disabled])': '_submit',
            'click #calendar-dialog-cancel': 'hide',
            'click #calendar-dialog-permission-table-add:not([disabled])': '_addPermission',
            'click .calendar-dialog-permission-table-action .aui-icon:not([disabled])': '_removePermission',
            'click .calendar-dialog-permission-table-access-type .aui-lozenge:not([disabled])': '_toggleAccessType',
            'click a[href=#calendar-dialog-common-tab]': '_selectCommonTab',
            'click a[href=#calendar-dialog-permissions-tab]': '_selectPermissionTab',
            'change #calendar-dialog-permission-table-subject': '_onChangeSubjectSelect',
            'select2-open #calendar-dialog-permission-table-subject': '_onOpenSubjectSelect'
        },
        initialize: function(options) {
            this.dialog = AJS.dialog2('#calendar-dialog');
            this.$okButton = this.$('#calendar-dialog-ok');
            this.$cancelButton = this.$('#calendar-dialog-cancel');
            this.permissionIds = {};
            this.userData = options.userData;

            var canAdmin = !this.model.id || this.model.get('canAdmin');
            this.$('#calendar-dialog-name').attr('disabled', !canAdmin);
            this.$('#calendar-dialog-color').attr('disabled', !canAdmin);
            this.$('#calendar-dialog-color').auiSelect2('enable', canAdmin);
            this.$('#calendar-dialog-event-start').attr('disabled', !canAdmin);
            this.$('#calendar-dialog-event-start').auiSelect2('enable', canAdmin);
            this.$('#calendar-dialog-event-end').attr('disabled', !canAdmin);
            this.$('#calendar-dialog-event-end').auiSelect2('enable', canAdmin);
            this.$('#calendar-dialog-source').attr('disabled', !canAdmin);
            this.$('#calendar-dialog-source').auiSelect2('enable', canAdmin);
            this.$('#calendar-dialog-displayed-fields').attr('disabled', !canAdmin);
            this.$('#calendar-dialog-displayed-fields').auiSelect2('enable', canAdmin);
            this.$('#calendar-dialog-permission-table-subject').attr('disabled', !canAdmin);
            this.$('#calendar-dialog-permission-table-subject').auiSelect2('enable', canAdmin);
            this.$('#calendar-dialog-permission-table-add').attr('disabled', !canAdmin);
            this.$okButton.attr('disabled', !canAdmin);
            this.$('.aui-dialog2-footer-hint').text(canAdmin ? '' : AJS.I18n.getText('ru.mail.jira.plugins.calendar.dialog.no.edit.permission'));

            this._initSourceField();
            this._initColorField();
            this._initPermissionSubjectField();

            this._fillForm();

            AJS.tabs.setup();
            this.dialog.on('hide', $.proxy(this.destroy, this));
            this.$('form').submit($.proxy(this._onFormSubmit, this));
        },
        destroy: function() {
            this.stopListening();
            this.undelegateEvents();
            this.$('form').off();
            this.dialog.off();

            this.$okButton.removeAttr('disabled');
            this.$cancelButton.removeAttr('disabled');

            this.$('.calendar-dialog-select').auiSelect2('val', '');
            this.$('.calendar-dialog-source-dropdown').removeClass(
                'calendar-dialog-source-show-unavailable-project ' +
                'calendar-dialog-source-show-unavailable-filter ' +
                'calendar-dialog-source-show-deleted-project ' +
                'calendar-dialog-source-show-deleted-filter');

            var $sourceSelect = this.$('#calendar-dialog-source');
            $sourceSelect.select2('val', $sourceSelect.find('option:eq(2)').val()); // todo:xxx bad index

            this.$('div.error').text('').addClass('hidden');
            this.$('input').val('');
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
        _initSourceField: function() {
            this.$('#calendar-dialog-source').auiSelect2({
                minimumInputLength: 0,
                ajax: {
                    url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/config/eventSources',
                    dataType: 'json',
                    quietMillis: 100,
                    data: function(term) {
                        return {filter: term};
                    },
                    results: function(data) {
                        var results = [];
                        if (data.projects && data.projects.length > 0)
                            results.push({
                                text: AJS.I18n.getText('ru.mail.jira.plugins.calendar.dialog.itemOfAllMatching', AJS.I18n.getText('common.concepts.projects'), data.projects.length, data.totalProjectsCount),
                                children: data.projects
                            });
                        if (data.filters && data.filters.length > 0)
                            results.push({
                                text: AJS.I18n.getText('ru.mail.jira.plugins.calendar.dialog.itemOfAllMatching', AJS.I18n.getText('common.concepts.filters'), data.filters.length, data.totalFiltersCount),
                                children: data.filters
                            });
                        return {results: results};
                    },
                    cache: true
                },
                dropdownCssClass: 'calendar-dialog-source-dropdown',
                formatResult: format,
                formatSelection: format,
                initSelection: function(element, callback) {
                }
            });

            function format(item) {
                return JIRA.Templates.Plugins.MailRuCalendar.sourceField($.extend({projectId: item.id && item.id.indexOf('project_') > -1 ? item.id.substring('project_'.length) : ''}, item));
            }
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

                this.$('#calendar-dialog-source').auiSelect2('data', {
                    id: this.model.get('selectedSourceId'),
                    text: this.model.get('selectedSourceName'),
                    avatarId: this.model.get('selectedSourceAvatarId'),
                    unavailable: this.model.get('selectedSourceIsUnavailable')
                });

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
            var sourceData = this.$('#calendar-dialog-source').auiSelect2('data');
            var source;
            if (sourceData)
                source = sourceData.id;
            var eventStart = this.$('#calendar-dialog-event-start').val();
            var eventEnd = this.$('#calendar-dialog-event-end').val();
            var displayedFields = this.$('#calendar-dialog-displayed-fields').val();
            var permissions = _.filter(_.values(this.permissionIds), function(obj) {
                return !!obj;
            });

            return {
                selectedName: name,
                selectedColor: color,
                selectedSourceId: source,
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
                this.$('#calendar-dialog-' + field + '-error').removeClass('hidden').text(response.responseText);
                var $field = this.$('#calendar-dialog-' + field);
                if ($field.hasClass('select') || $field.hasClass('multi-select'))
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
            this.$('#calendar-dialog-permissions-tab').toggleClass('hidden', true);
            this.$('a[href=#calendar-dialog-permissions-tab]').closest('li').toggleClass('aui-nav-selected', false);
            this.$('#calendar-dialog-common-tab').toggleClass('hidden', false);
            this.$('a[href=#calendar-dialog-common-tab]').closest('li').toggleClass('aui-nav-selected', true);
            this.$('#calendar-dialog-name').focus();
        },
        _selectPermissionTab: function(e) {
            e && e.preventDefault();
            this.$('#calendar-dialog-permissions-tab').toggleClass('hidden', false);
            this.$('a[href=#calendar-dialog-permissions-tab]').closest('li').toggleClass('aui-nav-selected', true);
            this.$('#calendar-dialog-common-tab').toggleClass('hidden', true);
            this.$('a[href=#calendar-dialog-common-tab]').closest('li').toggleClass('aui-nav-selected', false);
            this.$('#calendar-dialog-permission-table-subject').auiSelect2('focus');
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
                var $otherAccessType = $row.find('li[data-access-type="' + (accessType == 'ADMIN' ? 'USE' : 'ADMIN') + '"]');
                $otherAccessType.toggleClass('selected', false);
                $otherAccessType.toggleClass('aui-lozenge-complete', false);
                $otherAccessType.toggleClass('aui-lozenge-subtle', true);
            }
            permission.accessType = selected ? accessType : undefined;
        }
    });
});
