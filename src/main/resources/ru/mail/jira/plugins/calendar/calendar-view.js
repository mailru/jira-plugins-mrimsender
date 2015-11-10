(function ($) {
    AJS.toInit(function () {
        runTopMailScript();

        var $calendar = $('#calendar-full-calendar');
        var fullCalendarInstance;
        var sharedCounter;
        var SHARED_ELEMENT_INDEX_PREFIX = 'calendar-dialog-shared-';

        /* START OF CREATING FIELDS IN DIALOG */

        createColorField();
        createSourceField();
        createDateFields();
        createDisplayedFields();

        function createColorField() {
            $('#calendar-dialog-color').auiSelect2({
                minimumResultsForSearch: Infinity,
                formatResult: format,
                formatSelection: format
            });

            function format(data) {
                return '<div class="calendar-dialog-color-option" style="background:' + data.id + '"></div>';
            }
        }

        function createSourceField() {
            $('#calendar-dialog-source').auiSelect2({
                minimumInputLength: 0,
                ajax: {
                    url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/eventSources',
                    dataType: 'json',
                    quietMillis: 100,
                    data: function (term) {
                        return { filter: term };
                    },
                    results: function (data) {
                        var results = [];
                        if (data.projects && data.projects.length > 0)
                            results.push({ text: AJS.I18n.getText('common.concepts.projects'), children: data.projects });
                        if (data.filters && data.filters.length > 0)
                            results.push({ text: AJS.I18n.getText('common.concepts.filters'), children: data.filters });
                        return { results: results };
                    },
                    cache: true
                },
                dropdownCssClass: 'calendar-dialog-source-dropdown',
                formatResult: format,
                formatSelection: format,
                initSelection: function(element, callback) { }
            });

            function format(item) {
                if (!item.id) {
                    return item.text;
                } else if (item.unavailable) {
                    return '<span class="calendar-dialog-source-unavailable-source">' + item.text + '</span>'
                } else if (item.id && item.id.lastIndexOf('project_') == 0) {
                    var projectId = item.id.substring('project_'.length);
                    var projectItemHtml = '';
                    projectItemHtml += '<span class="aui-avatar aui-avatar-project aui-avatar-xsmall">';
                    projectItemHtml += '    <span class="aui-avatar-inner">';
                    projectItemHtml += '        <img src="projectavatar?pid=' + projectId + '&avatarId=' + item.avatarId + '&size=xxmall" />';
                    projectItemHtml += '    </span>';
                    projectItemHtml += '</span>';
                    projectItemHtml += '&nbsp;' + item.text;
                    return projectItemHtml;
                } else
                    return item.text;
            }
        }

        function createDateFields() {
            $('#calendar-dialog-event-start').auiSelect2();
            $('#calendar-dialog-event-end').auiSelect2({ allowClear: true });
        }

        function createDisplayedFields() {
            $.ajax({
                type: 'GET',
                url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/displayedFields',
                success: function (result) {
                    $('#calendar-dialog-displayed-fields').auiSelect2({
                        allowClear: true,
                        multiple: true,
                        data: Object.keys(result).map(function (key) {
                            return {
                                id: key,
                                text: result[key]
                            }})
                    });
                },
                error: function (xhr) {
                    var msg = "Error while trying to load displayed fields.";
                    if (xhr.responseText)
                        msg += xhr.responseText;
                    alert(msg);
                }
            });
        }

        /* END OF CREATING FIELDS IN DIALOG */

        $.ajax({
            type: 'GET',
            url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/userPreference',
            success: function (result) {
                var view = result.calendarView ? result.calendarView : "month";
                loadFullCalendar(view, result.hideWeekends);
                loadCalendars();
            },
            error: function (xhr) {
                var msg = "Error while trying to load user preferences.";
                if (xhr.responseText)
                    msg += xhr.responseText;
                alert(msg);
                loadFullCalendar("month", false);
                loadCalendars();
            }
        });

        function loadFullCalendar(view, hideWeekends) {
            var viewRenderFirstTime = true;
            fullCalendarInstance = $calendar.fullCalendar({
                contentHeight: 'auto',
                defaultView: view,
                header: {
                    left: 'prev,next today',
                    center: 'title',
                    right: 'quarter,month,basicWeek,basicDay'
                },
                views: {
                    quarter: {
                        type: 'basic',
                        duration: { months: 3 },
                        buttonText: AJS.I18n.getText('ru.mail.jira.plugins.calendar.quarter')
                    }
                },
                timeFormat: 'HH:mm',
                lazyFetching: false,
                editable: true,
                draggable: true,
                firstDay: 1,
                monthNames: [ AJS.I18n.getText('ru.mail.jira.plugins.calendar.January'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.February'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.March'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.April'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.May'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.June'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.July'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.August'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.September'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.October'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.November'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.December')],
                monthNamesShort: [ AJS.I18n.getText('ru.mail.jira.plugins.calendar.Jan'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Feb'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Mar'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Apr'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.May'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Jun'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Jul'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Aug'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Sep'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Oct'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Nov'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Dec')],
                dayNames: [ AJS.I18n.getText('ru.mail.jira.plugins.calendar.Sunday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Monday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Tuesday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Wednesday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Thursday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Friday'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Saturday')],
                dayNamesShort: [ AJS.I18n.getText('ru.mail.jira.plugins.calendar.Sun'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Mon'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Tue'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Wed'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Thu'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Fri'), AJS.I18n.getText('ru.mail.jira.plugins.calendar.Sat')],
                buttonText: {
                    today: AJS.I18n.getText('ru.mail.jira.plugins.calendar.today'),
                    month: AJS.I18n.getText('ru.mail.jira.plugins.calendar.month'),
                    week: AJS.I18n.getText('ru.mail.jira.plugins.calendar.week'),
                    day: AJS.I18n.getText('ru.mail.jira.plugins.calendar.day')
                },
                weekends: !hideWeekends,
                weekMode: 'liquid',
                eventRender: function(event, $element) {
                    $element.addClass('calendar-event-object');
                    AJS.InlineDialog($element, "eventDialog", function(content, trigger, showPopup) {
                        $.ajax({
                            type: 'GET',
                            url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/' + event.calendarId + '/event/' + event.id + '/info',
                            success: function (issue) {
                                content.html(buildMainInfoPopup(issue)).addClass('calendar-event-info-popup');
                                showPopup();
                            },
                            error: function (xhr) {
                                var msg = "Error while trying to view info about issue => " + event.id;
                                if (xhr.responseText)
                                    msg += xhr.responseText;
                                alert(msg);
                            }
                        });
                        return false;
                    }, {
                        hideDelay: null,
                        onTop: true,
                        closeOnTriggerClick: true,
                        userLiveEvents: true
                    });
                },
                viewRender: function(view) {
                    if (viewRenderFirstTime)
                        viewRenderFirstTime = false;
                    else {
                        var $visibleCalendars = $('.calendar-visible');
                        startLoadingCalendarsCallback();
                        $visibleCalendars.find('a.calendar-name').addClass('not-active');
                        $.ajax({
                            type: 'PUT',
                            url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/userPreference/view?value=' + view.name,
                            error: function (xhr) {
                                var msg = "Error while trying to update user default view to => " + view;
                                if (xhr.responseText)
                                    msg += xhr.responseText;
                                alert(msg);
                            }
                        });
                    }
                },
                eventAfterAllRender: function () {
                    finishLoadingCalendarsCallback();
                },
                eventDrop: function(event, duration) {
                    eventMove(event, duration, true);
                },
                eventResize: function(event, duration) {
                    eventMove(event, duration, false);
                }
            });

            function eventMove(event, duration, isDrag) {
                $.ajax({
                    type: 'PUT',
                    url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/' + event.calendarId + '/event/' + event.id + '?dayDelta=' + duration._days + '&millisDelta=' + duration._milliseconds + '&isDrag=' + isDrag,
                    error: function (xhr) {
                        var msg = "Error while trying to drag event. Issue key => " + event.id;
                        if (xhr.responseText)
                            msg += xhr.responseText;
                        alert(msg);
                    }
                });
            }
        }

        function loadCalendars () {
            $.ajax({
                type: 'GET',
                url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/all',
                success: function (result) {
                    var calendarsCount = result.length;
                    var htmlSharedCalendars = '';
                    var htmlMyCalendars = '';
                    var htmlOtherCalendars = '';
                    var eventSources = [];

                    if (hasVisibleCalendars(result))
                        startLoadingCalendarsCallback();

                    for (var i = 0 ; i < calendarsCount; i++) {
                        var calendar = result[i];
                        var visible = calendar.visible;

                        if (calendar.isMy)
                            htmlMyCalendars += buildCalendarLink(calendar);
                        else if (calendar.fromOthers)
                            htmlOtherCalendars += buildCalendarLink(calendar);
                        else
                            htmlSharedCalendars += buildCalendarLink(calendar);

                        if (visible)
                            eventSources.push(AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/' + calendar.id + '/events');
                    }

                    if (htmlMyCalendars != '') {
                        $('#calendar-my-calendar-list-header').css('display', 'block');
                        $('#calendar-my-calendar-list').append(htmlMyCalendars);
                    }

                    if (htmlSharedCalendars != '') {
                        $('#calendar-shared-calendar-list-header').css('display', 'block');
                        $('#calendar-shared-calendar-list').append(htmlSharedCalendars);
                    }

                    if (htmlOtherCalendars != '') {
                        $('#calendar-other-calendar-list-header').css('display', 'block');
                        $('#calendar-other-calendar-list').append(htmlOtherCalendars);
                    }

                    for (var a = 0; a < eventSources.length; a++)
                        $calendar.fullCalendar('addEventSource', eventSources[a]);
                },
                error: function (request) {
                    alert(request.responseText);
                }
            });
        }

        function buildCalendarLink(calendar) {
            var html = '';
            html += '<div class="calendar-list-item-block ';
            if (calendar.visible)
                html += 'calendar-visible';
            html += '" data-id="' + calendar.id + '" data-color="' + calendar.color + '" id="calendar-list-item-block-' + calendar.id + '">';
            if (calendar.hasError) {
                html += '<a href="#" class="calendar-name not-working">';
                html += '<span class="aui-icon aui-icon-small aui-iconfont-error" title="' + calendar.error + '"></span>';
            } else if (calendar.visible) {
                html += '<a href="#" class="calendar-name not-active">';
                html += '<div class="calendar-view-color-box" style="background-color: ' + calendar.color + ';"></div>';
            } else {
                html += '<a href="#" class="calendar-name">';
                html += '<div class="calendar-view-color-box" style="border: 2px solid ' + calendar.color + ';"></div>';
            }
            html += '<span class="aui-nav-item-label" title="' + calendar.name + '">';
            html += calendar.name;
            html += '</span>';
            if (calendar.changable) {
                html += '      <div class="calendar-vews-buttons-block">';
                html += '          <button class="aui-button aui-button-subtle aui-button-compact calendar-edit">';
                html += '              <span class="aui-icon aui-icon-small aui-iconfont-edit"></span>';
                html += '          </button>';
                html += '          <button class="aui-button aui-button-subtle aui-button-compact calendar-delete">';
                html += '              <span class="aui-icon aui-icon-small aui-iconfont-delete calendar-view-delete-button"></span>';
                html += '          </button>';
                html += '      </div>';
            }
            html += '   </a>';
            html += '</div>';
            return html;
        }

        function hasVisibleCalendars(calendar) {
            for (var i = 0 ; i < calendar.length; i++)
                if (calendar[i].visible)
                    return true;
            return false;
        }

        $(document).on('click', '.calendar-name', function (e) {
            e.preventDefault();

            var $calendarNameLink = $(this);

            if ($calendarNameLink.hasClass('not-working'))
                return;

            var $calendarBlock = $calendarNameLink.closest('.calendar-list-item-block');
            var calendarId = $calendarNameLink.closest('div.calendar-list-item-block').data('id');

            startLoadingCalendarsCallback();
            $calendarNameLink.addClass('not-active');

            $.ajax({
                type: 'PUT',
                url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/' + calendarId + '/visibility',
                success: function (visible) {
                    $calendarBlock.toggleClass('calendar-visible', visible);

                    if (visible) {
                        $calendar.fullCalendar('addEventSource', {
                            url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/' + calendarId + '/events',
                            success: function () {
                                changeEventSourceCallback(calendarId, visible);
                            }
                        })
                    } else {
                        $calendar.fullCalendar('removeEventSource', AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/' + calendarId + '/events');
                        changeEventSourceCallback(calendarId, false);
                    }
                },
                error: function (request) {
                    alert(request.responseText);
                }
            });
        });

        $('#calendar-weekends-visibility').click(function (e) {
            e.preventDefault();
            $.ajax({
                type: 'PUT',
                url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/userPreference/hideWeekends',
                success: function () {
                    window.location.reload();
                },
                error: function (xhr) {
                    var msg = "Error while trying to update user hide weekends option. ";
                    if (xhr.responseText)
                        msg += xhr.responseText;
                    alert(msg);
                }
            });
        });

        $('#calendar-add').click(function (e) {
            e.preventDefault();

            sharedCounter = 0;

            $('#calendar-dialog-owner-block').addClass('hidden');
            $('#calendar-dialog-header').text(AJS.I18n.getText('ru.mail.jira.plugins.calendar.addCalendar'));
            $('#calendar-dialog-ok').text(AJS.I18n.getText('common.words.create'));

            AJS.dialog2('#calendar-dialog').show();
        });

        $(document).on('click', '.calendar-edit', function (e) {
            e.preventDefault();
            e.stopPropagation();
            sharedCounter = 0;

            var calendarId = $(this).closest('div.calendar-list-item-block').data('id');
            $('#calendar-dialog-header').text(AJS.I18n.getText('ru.mail.jira.plugins.calendar.editCalendar'));
            $('#calendar-dialog-ok').text(AJS.I18n.getText('ru.mail.jira.plugins.calendar.common.edit'));
            $('#calendar-dialog-id').val(calendarId);
            $.ajax({
                type: 'GET',
                url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/' + calendarId,
                success: function (result) {
                    var ownerLink = '';

                    if (result.owner) {
                        ownerLink += '<span rel="' + result.owner + '" class="user-hover user-avatar calendar-dialog-owner">';
                        ownerLink += '<span class="aui-avatar aui-avatar-xsmall"><span class="aui-avatar-inner"><img src="' + result.ownerAvatarUrl + '" /></span></span>';
                        ownerLink += result.ownerFullName;
                        ownerLink += '</span>';
                    } else {
                        ownerLink += '<span class="calendar-dialog-owner">' + result.ownerFullName + '</span>';
                    }

                    $('#calendar-dialog-owner-block').removeClass('hidden').append(ownerLink);
                    $('#calendar-dialog-name').val(result.selectedName);
                    $('#calendar-dialog-color').auiSelect2('val', result.selectedColor);
                    $('#calendar-dialog-event-start').auiSelect2('val', result.selectedEventStartId);
                    $('#calendar-dialog-event-end').auiSelect2('val', result.selectedEventEndId);

                    var $sourceSelect = $('#calendar-dialog-source');
                    $sourceSelect.auiSelect2('data', {
                        id: result.selectedSourceId,
                        text: result.selectedSourceName,
                        avatarId: result.selectedSourceAvatarId,
                        unavailable: result.selectedSourceIsUnavailable
                    });

                    if (result.selectedDisplayedFields)
                        $('#calendar-dialog-displayed-fields').auiSelect2('val', result.selectedDisplayedFields);

                    if (result.selectedShares) {
                        for (var i = 0; i < result.selectedShares.length; i++) {
                            var selectedShare = result.selectedShares[i];
                            if (selectedShare.group) {
                                createShareToGroupSelect(result.groups, selectedShare.group, selectedShare.error);
                            } else {
                                var projectRoleText = selectedShare.roleId == 0 ? AJS.I18n.getText('common.words.all') : selectedShare.roleName;
                                var roles = result.projectRolesForShare[selectedShare.projectId];
                                createShareToProjectRole(result.projectsForShare, true, roles,
                                    selectedShare.projectId, selectedShare.projectName, selectedShare.roleId, projectRoleText, selectedShare.error);
                            }
                        }
                    }
                    AJS.dialog2('#calendar-dialog').show();
                },
                error: function (request) {
                    alert(request.responseText);
                }
            });
        });

        $(document).on('click', '.calendar-delete', function (e) {
            e.preventDefault();
            e.stopPropagation();

            var calendarId = $(this).closest('div.calendar-list-item-block').data('id');
            if (confirm(AJS.I18n.getText("ru.mail.jira.plugins.calendar.confirmDelete"))) {
                startLoadingCalendarsCallback();
                $.ajax({
                    url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/' + calendarId,
                    type: "DELETE",
                    error: function(xhr) {
                        finishLoadingCalendarsCallback();
                        if (xhr.responseText) {
                            alert(xhr.responseText);
                        } else {
                            alert("Internal error");
                        }
                    },
                    success: function() {
                        $('#calendar-list-item-block-' + calendarId).remove();
                        $('#calendar-full-calendar').fullCalendar('removeEventSource', AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/' + calendarId + '/events');
                    }
                });
            }
        });

        function changeEventSourceCallback(calendarId, visible, error) {
            var $calendarBlock = $('#calendar-list-item-block-' + calendarId);
            var $calendarLink = $calendarBlock.find('a.calendar-name');
            $calendarLink.removeClass('not-active');

            var calendarColor = $calendarBlock.data('color');
            $calendarLink.find('.calendar-view-color-box').remove();

            if (error) {
                if (!$calendarLink.hasClass('not-working')) {
                    $calendarLink.addClass('not-working');
                    $calendarLink.find('.calendar-view-color-box').remove();
                    $calendarLink.prepend('<span class="aui-icon aui-icon-small aui-iconfont-error" title="' + error + '"></span>')
                }
            } else {
                if ($calendarLink.hasClass('not-working'))
                    $calendarLink.removeClass('not-working').find('span.aui-iconfont-error').remove();

                if (visible)
                    $calendarLink.prepend('<div class="calendar-view-color-box" style="background-color: ' + calendarColor + ';"></div>');
                else
                    $calendarLink.prepend('<div class="calendar-view-color-box" style="border: 2px solid ' + calendarColor + ';"></div>');
            }
        }

        function startLoadingCalendarsCallback() {
            AJS.dim();
            JIRA.Loading.showLoadingIndicator();
        }

        function finishLoadingCalendarsCallback() {
            $('.calendar-name').removeClass('not-active');
            AJS.undim();
            JIRA.Loading.hideLoadingIndicator();
        }

        /* Issue Info Popup */

        function buildMainInfoPopup(issue) {
            var html = '';
            html += '<div>';
            html += '<div class="calenar-issue-popup-key-header">';
            html += '<a target="_blank" href="' + AJS.contextPath() + '/browse/' + issue.key + '"><strong>' + issue.summary + '</strong>';
            html += '<span class="aui-icon aui-icon-small aui-iconfont-share"></span>';
            html += '</a>';
            html += '</div>';
            html += buildField(issue.assignee, AJS.I18n.getText('issue.field.assignee'));
            html += buildField(issue.reporter, AJS.I18n.getText('issue.field.reporter'));
            html += buildStatusField(issue.status, issue.statusColor, AJS.I18n.getText('common.words.status'));
            html += buildField(issue.labels, AJS.I18n.getText('common.concepts.labels'));
            html += buildField(issue.components, AJS.I18n.getText('common.concepts.components'));
            html += buildField(issue.dueDate, AJS.I18n.getText('issue.field.duedate'));
            html += buildField(issue.environment, AJS.I18n.getText('common.words.env'));
            html += buildPriorityField(issue.priority, issue.priorityIconUrl, AJS.I18n.getText('issue.field.priority'));
            html += buildField(issue.resolution, AJS.I18n.getText('issue.field.resolution'));
            html += buildField(issue.affect, AJS.I18n.getText('issue.field.version'));
            html += buildField(issue.created, AJS.I18n.getText('issue.field.created'));
            html += buildField(issue.updated, AJS.I18n.getText('issue.field.updated'));
            html += buildCustomFieldsBlock(issue);
            html += buildDescription(issue.description);

            html += '</div>';
            return html;
        }

        function buildField(field, label) {
            var html = '';
            if (field) {
                html += '<dl>';
                html += '<dt>' + label + ':</dt>';
                html += '<dd>' + field + '</dd>';
                html += '</dl>';
            }
            return html;
        }

        function buildDescription(description) {
            var html = '';
            if (description && description.trim())
                html = '<div class="calendar-event-info-popup-description">' + description + '</div>';
            return html;
        }

        function buildStatusField(status, statusColor, label) {
            var html = '';
            if (status) {
                html += '<dl>';
                html += '<dt>' + label + ':</dt>';
                html += '<dd>';
                html += '<span class="jira-issue-status-lozenge aui-lozenge jira-issue-status-lozenge-' + statusColor + '">' + status + '</span>';
                html += '</dd>';
                html += '</dl>';
            }
            return html;
        }

        function buildPriorityField(priority, imageUrl, label) {
            var html = '';
            if (priority) {
                html += '<dl>';
                html += '<dt>' + label + ':</dt>';
                html += '<dd>';
                html += '<img class="calendar-priority-image-url" height="16" src="' + AJS.contextPath() + imageUrl + '">' + priority;
                html += '</dd>';
                html += '</dl>';
            }
            return html;
        }

        function buildCustomFieldsBlock(issue) {
            var html = '';
            if (issue.customFields) {
                Object.keys(issue.customFields).map(function (item) {
                    html += '<dl>';
                    html += '<dt>' + item + ':</dt>';
                    html += '<dd>';
                    html += issue.customFields[item];
                    html += '</dd>';
                    html += '</dl>';
                });
            }
            return html;
        }

        /* Dialog */

        $('#calendar-dialog').find('form').submit(function (e) {
            e.preventDefault();
            $('#calendar-dialog-ok').click();
        });

        AJS.dialog2('#calendar-dialog').on('hide', function() {
            var $this = $(this);
            $('.calendar-dialog-select').auiSelect2('val', '');
            $('.calendar-dialog-source-dropdown').removeClass(
                'calendar-dialog-source-show-unavailable-project ' +
                'calendar-dialog-source-show-unavailable-filter ' +
                'calendar-dialog-source-show-deleted-project ' +
                'calendar-dialog-source-show-deleted-filter');

            var $sourceSelect = $('#calendar-dialog-source');
            $sourceSelect.select2('val', $sourceSelect.find('option:eq(2)').val()); // todo:xxx bad index

            sharedCounter = 0;
            $this.find('div.error').text('');
            $('#calendar-dialog-error-panel').text('').addClass('hidden');
            $this.find('input').val('');

            $this.find('#calendar-dialog-owner-block').find('.calendar-dialog-owner').remove();

            $this.find('#calendar-dialog-shares').find('div.error').remove();
            $this.find('.calendar-shared-block, .calendar-shared-label').remove();
        });

        $('#calendar-dialog-ok').click(function (e) {
            e.preventDefault();
            var $okButton = $(this);
            var $cancelButton = $('#calendar-dialog-cancel');

            $okButton.attr('disabled', 'disabled');
            $cancelButton.attr('disabled', 'disabled');

            $('#calendar-dialog-error-panel').text('').addClass('hidden');

            $('#calendar-dialog').find('div.error').addClass('hidden').text('');
            var calendarId = $('#calendar-dialog-id').val();

            var shares = '';
            $('.calendar-shared-block').each(function (index, el) {
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

            var name = $('#calendar-dialog-name').val();
            var color = $('#calendar-dialog-color').val();

            var sourceData = $('#calendar-dialog-source').auiSelect2('data');
            var source;
            if (sourceData)
                source = sourceData.id;
            var eventStart = $('#calendar-dialog-event-start').val();
            var eventEnd = $('#calendar-dialog-event-end').val();
            var displayedFields = $('#calendar-dialog-displayed-fields').val();

            var ajaxData = {
                name: name,
                color: color,
                source: source,
                eventStart: eventStart,
                eventEnd: eventEnd,
                displayedFields: displayedFields,
                shares: shares
            };

            if (calendarId) {
                $.ajax({
                    type: 'PUT',
                    url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/' + calendarId,
                    data: ajaxData,
                    success: function (result) {
                        var $calendarBlock = $('#calendar-list-item-block-' + calendarId);
                        $calendarBlock.find('span.aui-nav-item-label').text(name);
                        $calendarBlock.data('color', color);

                        $okButton.removeAttr('disabled');
                        $cancelButton.removeAttr('disabled');
                        AJS.dialog2('#calendar-dialog').hide();

                        if (result.visible) {
                            startLoadingCalendarsCallback();

                            var $calendar = $('#calendar-full-calendar');
                            var eventSource = AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/' + calendarId + '/events';

                            $calendar.fullCalendar('removeEventSource', eventSource);
                            startLoadingCalendarsCallback(); //todo:bad

                            $calendar.fullCalendar('addEventSource', {
                                url: eventSource,
                                success: function () {
                                    changeEventSourceCallback(calendarId, true, result.error);
                                }
                            });
                        } else {
                            changeEventSourceCallback(calendarId, false);
                        }
                    },
                    error: handleAjaxError
                });
            } else {
                $.ajax({
                    type: 'POST',
                    url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar',
                    data: ajaxData,
                    success: function (calendar) {
                        $('#calendar-my-calendar-list-header').css('display', 'block');
                        $('#calendar-my-calendar-list').append(buildCalendarLink(calendar));

                        $okButton.removeAttr('disabled');
                        $cancelButton.removeAttr('disabled');
                        AJS.dialog2('#calendar-dialog').hide();
                        startLoadingCalendarsCallback();

                        $('#calendar-full-calendar').fullCalendar('addEventSource', {
                            url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/' + calendar.id + '/events',
                            success: function () {
                                changeEventSourceCallback(calendar.id, true);
                            }
                        });
                    },
                    error: handleAjaxError
                });
            }

            function handleAjaxError (request) {
                var field = request.getResponseHeader('X-Atlassian-Rest-Exception-Field');
                if (field) {
                    if (field.indexOf('group_') == 0) {
                        var group = field.substr('group_'.length);
                        $('#calendar-dialog-shares').find('input.calendar-dialog-shared-group').each(function (index, el) {
                            var $el = $(el);
                            if ($el.val() === group) {
                                $('#' + $el.closest('.calendar-shared-block').attr('id') + '-error').removeClass('hidden').text(request.responseText);
                            }
                        });
                    } else if (field.indexOf('project_role_') == 0) {
                        var projectRoleId = field.substr('project_role_'.length);
                        $('#calendar-dialog-shares').find('input.calendar-dialog-shared-role').each(function (index, el) {
                            var $el = $(el);
                            if ($el.val() === projectRoleId) {
                                $('#' + $el.closest('.calendar-shared-block').attr('id') + '-error').removeClass('hidden').text(request.responseText);
                            }
                        });
                    } else if (field.indexOf('project_') == 0) {
                        var projectId = field.substr('project_'.length);
                        $('#calendar-dialog-shares').find('input.calendar-dialog-shared-project').each(function (index, el) {
                            var $el = $(el);
                            if ($el.val() === projectId) {
                                $('#' + $el.closest('.calendar-shared-block').attr('id') + '-error').removeClass('hidden').text(request.responseText);
                            }
                        });
                    } else {
                        $('#calendar-dialog-' + field + '-error').removeClass('hidden').text(request.responseText);
                    }
                } else
                    $('#calendar-dialog-error-panel').removeClass('hidden').text(request.responseText);

                $okButton.removeAttr('disabled');
                $cancelButton.removeAttr('disabled');
            }
        });

        $('#calendar-dialog-cancel').click(function () {
            AJS.dialog2('#calendar-dialog').hide();
        });

        $('#calendar-dialog-shares-group').click(function (e) {
            e.preventDefault();
            $.ajax({
                type: 'GET',
                url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/groups',
                success: function (groups) {
                    createShareToGroupSelect(groups);
                },
                error: function (request) {
                    alert(request.responseText);
                }
            });
        });

        function createShareToGroupSelect(groups, selectedGroup, error) {
            var $shareToBtn = $('#calendar-dialog-shared-to');

            sharedCounter++;
            var $sharesContainer = $('#calendar-dialog-shares');
            var blockIndex = SHARED_ELEMENT_INDEX_PREFIX + 'block-' + sharedCounter;
            var groupInputId = SHARED_ELEMENT_INDEX_PREFIX + 'group-' + sharedCounter;
            var html = '';
            html += '<div id="' + blockIndex +'" class="calendar-shared-block">';
            html += '<input type="hidden" class="long-field calendar-dialog-shared-group" id="' + groupInputId + '" value="' + (selectedGroup ? selectedGroup : "") + '">';
            html += '<a data-share-index="' + sharedCounter + '" class="aui-button aui-button-subtle calendar-dialog-share-delete" tabindex="-1">';
            html += '<span class="aui-icon aui-icon-small aui-iconfont-delete"></span>';
            html += '</a>';
            html += '</div>';

            $shareToBtn.before(html);

            var errorBlock = error
                ? '<div id="' + blockIndex + '-error" class="error">' + error + '</div>'
                : '<div id="' + blockIndex + '-error" class="error hidden"></div>';
            $shareToBtn.before(errorBlock);

            var data = groups.map(function (group) {
                return { id: group, text: group };
            });

            var $sharedGroup = $sharesContainer.find('#' + blockIndex).find('.calendar-dialog-shared-group');

            $sharedGroup.auiSelect2({
                allowClear: true,
                data: data,
                initSelection: function (element, callback) {
                    if (selectedGroup) {
                        callback({
                            id: selectedGroup,
                            text: selectedGroup
                        });
                    }
                }
            });

            $sharedGroup.on('change', function (event) {
                $('#' + blockIndex + '-error').empty().addClass('hidden');
            });

            return $sharedGroup;
        }

        $(document).on('click', '.calendar-test-button', function (e) {
            console.log("test button was clicked");
        });

        $('#calendar-dialog-shares-project-role').click(function (e) {
            e.preventDefault();
            $.ajax({
                type: 'GET',
                url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/projects',
                success: function (projects) {
                    createShareToProjectRole(projects);
                },
                error: function (request) {
                    alert(request.responseText);
                }
            });
        });

        function createShareToProjectRole(projects, fillProjectRoles, roles, selectedProjectId, selectedProjectName, selectedRoleId, selectedRoleText, error) {
            var $shareTo = $('#calendar-dialog-shared-to');

            sharedCounter++;
            var $sharesContainer = $('#calendar-dialog-shares');
            var blockIndex = SHARED_ELEMENT_INDEX_PREFIX + 'block-' + sharedCounter;
            var projectInputId = SHARED_ELEMENT_INDEX_PREFIX + 'project-' + sharedCounter;
            var html = '';
            html += '<div id="' + blockIndex + '" class="calendar-shared-block">';
            html += '<input type="hidden" class="calendar-dialog-shared-project" id="' + projectInputId + '" value="' + (selectedProjectId ? selectedProjectId : "") + '" >';
            html += '<input type="hidden" class="calendar-dialog-shared-role" value="' + (selectedRoleId != null ? selectedRoleId : "") + '">';
            html += '<a data-share-index="' + sharedCounter + '" class="aui-button aui-button-subtle calendar-dialog-share-delete">';
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
                data: Object.keys(projects).map(function (key) {
                    return { id: key, text: projects[key] };
                }),
                initSelection: function (element, callback) {
                    if (selectedProjectId) {
                        callback({
                            id: selectedProjectId,
                            text: selectedProjectName
                        });
                    }
                }
            });

            var $projectRoleSelect = $sharesContainer.find('#' + blockIndex).find('.calendar-dialog-shared-role');

            $projectSelect.on("change", function (e) {
                var projectid = e.val;
                if (projectid == -1 || projectid == '') {
                    $projectRoleSelect.auiSelect2({
                        data: [],
                        allowClear: true
                    });
                } else {
                    $.ajax({
                        type: 'GET',
                        url: AJS.contextPath() + '/rest/mailrucalendar/1.0/calendar/project/' + projectid + '/roles',
                        success: function (roles) {
                            $projectRoleSelect.auiSelect2({
                                data: getProjectRoleData(roles),
                                allowClear: true
                            });
                        },
                        error: function (request) {
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
                    initSelection: function (element, callback) {
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

        $(document).on('click', '.calendar-dialog-share-delete', function (e) {
            e.preventDefault();
            var index = $(this).data('shareIndex');
            $('#' + SHARED_ELEMENT_INDEX_PREFIX + 'block-' + index).remove();
            $('#' + SHARED_ELEMENT_INDEX_PREFIX + 'block-' + index + '-error').remove();
            $('#' + SHARED_ELEMENT_INDEX_PREFIX + 'label-' + index).remove();
        });

        function getProjectRoleData(roles) {
            var keys = Object.keys(roles);
            if (keys.length == 0)
                return [];

            var allProjectRoleEl = { id: 0, text: AJS.I18n.getText('common.words.all')};
            return [allProjectRoleEl].concat(keys.map(function (key) {
                return { id: key, text: roles[key] };
            }));
        }
    });
})(AJS.$);

function runTopMailScript() {
    var _tmr = window._tmr || (window._tmr = []);
    _tmr.push({id: "2706504", type: "pageView", start: (new Date()).getTime()});
    (function (d, w, id) {
        if (d.getElementById(id)) return;
        var ts = d.createElement("script"); ts.type = "text/javascript"; ts.async = true; ts.id = id;
        ts.src = (d.location.protocol == "https:" ? "https:" : "http:") + "//top-fwz1.mail.ru/js/code.js";
        var f = function () {var s = d.getElementsByTagName("script")[0]; s.parentNode.insertBefore(ts, s);};
        if (w.opera == "[object Opera]") { d.addEventListener("DOMContentLoaded", f, false); } else { f(); }
    })(document, window, "topmailru-code");
}