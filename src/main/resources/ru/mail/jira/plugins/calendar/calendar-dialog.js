(function ($) {
    AJS.toInit(function () {
        var SHARED_ELEMENT_INDEX_PREFIX = 'calendar-dialog-shared-';

        var ownerLinkTpl = _.template($('#owner-link-template').html());
        var sourceFieldTpl = _.template($('#source-field-template').html());
        var colorFieldTpl = _.template($('#color-field-template').html());

        (function() {
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
        })();
        (function() {
            $.ajax({
                type: 'GET',
                url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/config/dateFields',
                success: function(result) {
                    $('#calendar-dialog-event-start').auiSelect2({data: result});
                    $('#calendar-dialog-event-end').auiSelect2({allowClear: true, data: [{id: '', text: ' '}].concat(result)});
                },
                error: function(xhr) {
                    var msg = "Error while trying to load date fields.";
                    if (xhr.responseText)
                        msg += xhr.responseText;
                    alert(msg);
                }
            });
        })();

        function getProjectRoleData(roles) {
            var keys = Object.keys(roles);
            if (keys.length == 0)
                return [];

            var allProjectRoleEl = { id: 0, text: AJS.I18n.getText('common.words.all')};
            return [allProjectRoleEl].concat(keys.map(function (key) {
                return { id: key, text: roles[key] };
            }));
        }

        Backbone.View.CalendarDialogView = Backbone.View.extend({
            el: '#calendar-dialog',
            events: {
                'click #calendar-dialog-ok': '_submit',
                'click #calendar-dialog-cancel': 'hide',
                'click .calendar-dialog-share-delete': '_onShareDeleteClick'
            },
            initialize: function() {
                this.dialog = AJS.dialog2('#calendar-dialog');
                this.$okButton = this.$('#calendar-dialog-ok');
                this.$cancelButton = this.$('#calendar-dialog-cancel');
                this.sharedCounter = 0;

                this._initSourceField();
                this._initColorField();
                this._fillForm();

                this.dialog.on('hide', $.proxy(this.destroy, this));
                this.$('form').submit($.proxy(this._onFormSubmit, this));
                this.$('#calendar-dialog-shares-group').click($.proxy(this._onSharesGroupClick, this));
                this.$('#calendar-dialog-shares-project-role').click($.proxy(this._onSharesProjectRoleClick, this));
            },
            destroy: function() {
                this.stopListening();
                this.undelegateEvents();
                this.$('form').off();
                this.$('#calendar-dialog-shares-group').off();
                this.$('#calendar-dialog-shares-project-role').off();
                this.dialog.off();

                this.$('.calendar-dialog-select').auiSelect2('val', '');
                this.$('.calendar-dialog-source-dropdown').removeClass(
                    'calendar-dialog-source-show-unavailable-project ' +
                    'calendar-dialog-source-show-unavailable-filter ' +
                    'calendar-dialog-source-show-deleted-project ' +
                    'calendar-dialog-source-show-deleted-filter');

                var $sourceSelect = this.$('#calendar-dialog-source');
                $sourceSelect.select2('val', $sourceSelect.find('option:eq(2)').val()); // todo:xxx bad index

                this.$('div.error').text('');
                this.$('#calendar-dialog-error-panel').text('').addClass('hidden');
                this.$('input').val('');

                this.$('#calendar-dialog-owner-block').find('.calendar-dialog-owner').remove();

                this.$('#calendar-dialog-shares').find('div.error').remove();
                this.$('.calendar-shared-block, .calendar-shared-label').remove();
            },
            /* Public methods */
            show: function() {
                this.dialog.show();
            },
            hide: function() {
                this.dialog.hide();
            },
            /* Private methods */
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
                                    text: AJS.I18n.getText('common.concepts.projects'),
                                    children: data.projects
                                });
                            if (data.filters && data.filters.length > 0)
                                results.push({
                                    text: AJS.I18n.getText('common.concepts.filters'),
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
                    return sourceFieldTpl({item : item});
                }
            },
            _initColorField: function() {
                this.$('#calendar-dialog-color').auiSelect2({
                    minimumResultsForSearch: Infinity,
                    formatResult: format,
                    formatSelection: format
                });

                function format(data) {
                    return colorFieldTpl({data: data});
                }
            },
            _fillForm: function() {
                if (this.model.id) {
                    this.$('.aui-dialog2-header-main').text(AJS.I18n.getText('ru.mail.jira.plugins.calendar.editCalendar'));
                    this.$okButton.text(AJS.I18n.getText('ru.mail.jira.plugins.calendar.common.edit'));

                    var ownerLink = ownerLinkTpl({
                        owner: this.model.get('owner'),
                        ownerFullName: this.model.get('ownerFullName'),
                        ownerAvatarUrl: this.model.get('ownerAvatarUrl')
                    });
                    this.$('#calendar-dialog-owner-block').removeClass('hidden').append(ownerLink);

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

                    if (this.model.has('selectedShares')) {
                        var selectedShares = this.model.get('selectedShares');
                        for (var i = 0; i < selectedShares.length; i++) {
                            var selectedShare = selectedShares[i];
                            if (selectedShare.group) {
                                this._createShareToGroupSelect(this.model.get('groups'), selectedShare.group, selectedShare.error);
                            } else {
                                var projectRoleText = selectedShare.roleId == 0 ? AJS.I18n.getText('common.words.all') : selectedShare.roleName;
                                var roles = this.model.get('projectRolesForShare')[selectedShare.projectId];
                                this._createShareToProjectRole(this.model.get('projectsForShare'), true, roles,
                                    selectedShare.projectId, selectedShare.projectName, selectedShare.roleId, projectRoleText, selectedShare.error);
                            }
                        }
                    }
                } else {
                    this.$('#calendar-dialog-owner-block').addClass('hidden');
                    this.$('.aui-dialog2-header-main').text(AJS.I18n.getText('ru.mail.jira.plugins.calendar.addCalendar'));
                    this.$okButton.text(AJS.I18n.getText('common.words.create'));
                }
            },
            _onSharesGroupClick: function(e) {
                e.preventDefault();
                $.ajax({
                    type: 'GET',
                    url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/userPreference/groups',
                    success: $.proxy(function(groups) { this._createShareToGroupSelect(groups); }, this),
                    error: this._defaultAjaxErrorHandler
                });
            },
            _onSharesProjectRoleClick: function(e) {
                e.preventDefault();
                $.ajax({
                    type: 'GET',
                    url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/userPreference/projects',
                    success: $.proxy(function(projects) { this._createShareToProjectRole(projects); }, this),
                    error: this._defaultAjaxErrorHandler
                });
            },
            _onShareDeleteClick: function(e) {
                e.preventDefault();
                var index = $(e.currentTarget).data('shareIndex');
                this.$('#' + SHARED_ELEMENT_INDEX_PREFIX + 'block-' + index).remove();
                this.$('#' + SHARED_ELEMENT_INDEX_PREFIX + 'block-' + index + '-error').remove();
                this.$('#' + SHARED_ELEMENT_INDEX_PREFIX + 'label-' + index).remove();
            },
            _onFormSubmit: function (e) {
                e.preventDefault();
                this.$okButton.click();
            },
            _serialize: function() {
                var shares = '';
                this.$('.calendar-shared-block').each(function (index, el) {
                    var $el = $(el);
                    var groupVal = $el.find('.calendar-dialog-shared-group').auiSelect2('val');
                    if (groupVal.length) {
                        shares += 'group=' + groupVal + ';';
                    } else {
                        var projectVal = $el.find('.calendar-dialog-shared-project').auiSelect2('val');
                        if (projectVal.length) {
                            var roleId = $el.find('.calendar-dialog-shared-role').auiSelect2('val');
                            if (roleId.length)  {
                                shares += "project=" + projectVal;
                                if (roleId != 0)
                                    shares += ' role=' + roleId;
                                shares += ';'
                            }
                        }
                    }
                });

                var name = this.$('#calendar-dialog-name').val();
                var color = this.$('#calendar-dialog-color').val();

                var sourceData = this.$('#calendar-dialog-source').auiSelect2('data');
                var source;
                if (sourceData)
                    source = sourceData.id;
                var eventStart = this.$('#calendar-dialog-event-start').val();
                var eventEnd = this.$('#calendar-dialog-event-end').val();
                var displayedFields = this.$('#calendar-dialog-displayed-fields').val();

                return {
                    name: name,
                    color: color,
                    source: source,
                    eventStart: eventStart,
                    eventEnd: eventEnd,
                    displayedFields: displayedFields,
                    shares: shares
                };
            },
            _submit: function(e) {
                e.preventDefault();

                this.$okButton.attr('disabled', 'disabled');
                this.$cancelButton.attr('disabled', 'disabled');
                this.$('#calendar-dialog-error-panel').addClass('hidden').text('');
                this.$('div.error').addClass('hidden').text('');

                var ajaxData = this._serialize();
                if (this.model.id) {
                    $.ajax({
                        type: 'PUT',
                        url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/' + this.model.id,
                        data: ajaxData,
                        success: $.proxy(this._ajaxSuccessHandler, this),
                        error: $.proxy(this._ajaxErrorHandler, this)
                    });
                } else {
                    $.ajax({
                        type: 'POST',
                        url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar',
                        data: ajaxData,
                        success: $.proxy(this._ajaxSuccessHandler, this),
                        error: $.proxy(this._ajaxErrorHandler, this)
                    });
                }
            },
            _ajaxSuccessHandler: function(calendar) {
                this.collection.add(calendar, {merge: true});
                this.$okButton.removeAttr('disabled');
                this.$cancelButton.removeAttr('disabled');
                this.hide();
            },
            _defaultAjaxErrorHandler: function(response) {
                alert(response.responseText);
            },
            _ajaxErrorHandler: function(request) {
                var field = request.getResponseHeader('X-Atlassian-Rest-Exception-Field');
                if (field) {
                    if (field.indexOf('group_') == 0) {
                        var group = field.substr('group_'.length);
                        this.$('#calendar-dialog-shares').find('input.calendar-dialog-shared-group').each(function (index, el) {
                            var $el = $(el);
                            if ($el.val() === group) {
                                $('#' + $el.closest('.calendar-shared-block').attr('id') + '-error').removeClass('hidden').text(request.responseText);
                            }
                        });
                    } else if (field.indexOf('project_role_') == 0) {
                        var projectRoleId = field.substr('project_role_'.length);
                        this.$('#calendar-dialog-shares').find('input.calendar-dialog-shared-role').each(function (index, el) {
                            var $el = $(el);
                            if ($el.val() === projectRoleId) {
                                $('#' + $el.closest('.calendar-shared-block').attr('id') + '-error').removeClass('hidden').text(request.responseText);
                            }
                        });
                    } else if (field.indexOf('project_') == 0) {
                        var projectId = field.substr('project_'.length);
                        this.$('#calendar-dialog-shares').find('input.calendar-dialog-shared-project').each(function (index, el) {
                            var $el = $(el);
                            if ($el.val() === projectId) {
                                $('#' + $el.closest('.calendar-shared-block').attr('id') + '-error').removeClass('hidden').text(request.responseText);
                            }
                        });
                    } else {
                        this.$('#calendar-dialog-' + field + '-error').removeClass('hidden').text(request.responseText);
                    }
                } else
                    this.$('#calendar-dialog-error-panel').removeClass('hidden').text(request.responseText);

                this.$okButton.removeAttr('disabled');
                this.$cancelButton.removeAttr('disabled');
            },
            _createShareToGroupSelect: function(groups, selectedGroup, error) {
                var $shareToBtn = this.$('#calendar-dialog-shared-to');

                this.sharedCounter++;
                var $sharesContainer = this.$('#calendar-dialog-shares');
                var blockIndex = SHARED_ELEMENT_INDEX_PREFIX + 'block-' + this.sharedCounter;
                var groupInputId = SHARED_ELEMENT_INDEX_PREFIX + 'group-' + this.sharedCounter;
                var html = '';
                html += '<div id="' + blockIndex + '" class="calendar-shared-block">';
                html += '<input type="hidden" class="long-field calendar-dialog-shared-group" id="' + groupInputId + '" value="' + (selectedGroup ? selectedGroup : "") + '">';
                html += '<a data-share-index="' + this.sharedCounter + '" class="aui-button aui-button-subtle calendar-dialog-share-delete" tabindex="-1">';
                html += '<span class="aui-icon aui-icon-small aui-iconfont-delete"></span>';
                html += '</a>';
                html += '</div>';

                $shareToBtn.before(html);

                var errorBlock = error
                    ? '<div id="' + blockIndex + '-error" class="error">' + error + '</div>'
                    : '<div id="' + blockIndex + '-error" class="error hidden"></div>';
                $shareToBtn.before(errorBlock);

                var data = groups.map(function(group) {
                    return {id: group, text: group};
                });

                var $sharedGroup = $sharesContainer.find('#' + blockIndex).find('.calendar-dialog-shared-group');

                $sharedGroup.auiSelect2({
                    allowClear: true,
                    data: data,
                    initSelection: function(element, callback) {
                        if (selectedGroup) {
                            callback({
                                id: selectedGroup,
                                text: selectedGroup
                            });
                        }
                    }
                });

                $sharedGroup.on('change', function(event) {
                    $('#' + blockIndex + '-error').empty().addClass('hidden');
                });

                return $sharedGroup;
            },
            _createShareToProjectRole: function(projects, fillProjectRoles, roles, selectedProjectId, selectedProjectName, selectedRoleId, selectedRoleText, error) {
                var $shareTo = this.$('#calendar-dialog-shared-to');

                this.sharedCounter++;
                var $sharesContainer = this.$('#calendar-dialog-shares');
                var blockIndex = SHARED_ELEMENT_INDEX_PREFIX + 'block-' + this.sharedCounter;
                var projectInputId = SHARED_ELEMENT_INDEX_PREFIX + 'project-' + this.sharedCounter;
                var html = '';
                html += '<div id="' + blockIndex + '" class="calendar-shared-block">';
                html += '<input type="hidden" class="calendar-dialog-shared-project" id="' + projectInputId + '" value="' + (selectedProjectId ? selectedProjectId : "") + '" >';
                html += '<input type="hidden" class="calendar-dialog-shared-role" value="' + (selectedRoleId != null ? selectedRoleId : "") + '">';
                html += '<a data-share-index="' + this.sharedCounter + '" class="aui-button aui-button-subtle calendar-dialog-share-delete">';
                html += '<span class="aui-icon aui-icon-small aui-iconfont-delete"></span>';
                html += '</a>';
                html += '</div>';

                $shareTo.before(html);

                var errorBlock = error
                    ? '<div id="' + blockIndex + '-error" class="error">' + error + '</div>'
                    : '<div id="' + blockIndex + '-error" class="error hidden"></div>';
                $shareTo.before(errorBlock);

                var $projectSelect = $sharesContainer.find('#' + blockIndex).find('.calendar-dialog-shared-project').auiSelect2({
                    allowClear: true,
                    data: Object.keys(projects).map(function(key) {
                        return {id: key, text: projects[key]};
                    }),
                    initSelection: function(element, callback) {
                        if (selectedProjectId) {
                            callback({
                                id: selectedProjectId,
                                text: selectedProjectName
                            });
                        }
                    }
                });

                var $projectRoleSelect = $sharesContainer.find('#' + blockIndex).find('.calendar-dialog-shared-role');

                $projectSelect.on("change", function(e) {
                    var projectid = e.val;
                    if (projectid == -1 || projectid == '') {
                        $projectRoleSelect.auiSelect2({
                            data: [],
                            allowClear: true
                        });
                    } else {
                        $.ajax({
                            type: 'GET',
                            url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/userPreference/project/' + projectid + '/roles',
                            success: function(roles) {
                                $projectRoleSelect.auiSelect2({
                                    data: getProjectRoleData(roles),
                                    allowClear: true
                                });
                            },
                            error: function(request) {
                                alert(request.responseText);
                            }
                        });
                    }
                });

                var projectRolesData = [];
                if (fillProjectRoles)
                    projectRolesData = getProjectRoleData(roles);

                return {
                    projectSelect: $projectSelect,
                    roleSelect: $projectRoleSelect.auiSelect2({
                        data: projectRolesData,
                        allowClear: true,
                        initSelection: function(element, callback) {
                            if (selectedRoleId != null) {
                                callback({
                                    id: selectedRoleId,
                                    text: selectedRoleText
                                })
                            }
                        }
                    })
                }
            }
        });
    });
})(AJS.$);